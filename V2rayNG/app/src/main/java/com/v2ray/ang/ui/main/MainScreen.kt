package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.AutoConnectButton
import com.v2ray.ang.ui.compose.XfOrange
import com.v2ray.ang.ui.compose.XfOrangeLight
import com.v2ray.ang.ui.compose.QRCodeDialog
import com.v2ray.ang.handler.AngConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val psiphonActive by mainViewModel.psiphonActive.collectAsStateWithLifecycle()
    val speed by mainViewModel.trafficSpeed.collectAsStateWithLifecycle()
    val autoSwitch by mainViewModel.autoSwitch.collectAsStateWithLifecycle()
    val updateAvailable by mainViewModel.updateAvailable.collectAsStateWithLifecycle()
    val fetchState by mainViewModel.fetchState.collectAsStateWithLifecycle()
    // Home has two pages side by side, Matixray and Psiphon, moved by a swipe or a tap on the titles; the bottom bar
    // switches between Home (0) and the server list (1).
    val homePager = rememberPagerState(initialPage = 0, pageCount = { 2 })
    var bottomTab by rememberSaveable { mutableStateOf(0) }
    androidx.activity.compose.BackHandler(enabled = bottomTab != MainTab.Home) { bottomTab = MainTab.Home }
    val isRunning = uiState.isRunning

    // One timer for the connection, kept here so it does not restart when the section changes. The start time is
    // saved across rotation; the tick is aligned to the second boundary so the digits never skip. The seconds are
    // read through a lambda, so only the timer text is recomposed each second, not the screen.
    var startedAt by rememberSaveable { mutableStateOf(0L) }
    val elapsedSec = remember { mutableStateOf(0L) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            // 0 = not started; a start time in the future is stale (the device rebooted), so it restarts too
            if (startedAt == 0L || startedAt > android.os.SystemClock.elapsedRealtime()) startedAt = android.os.SystemClock.elapsedRealtime()
            while (true) {
                val spent = android.os.SystemClock.elapsedRealtime() - startedAt
                elapsedSec.value = spent / 1000L
                kotlinx.coroutines.delay(1000L - spent % 1000L)
            }
        } else {
            startedAt = 0L
            elapsedSec.value = 0L
        }
    }
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    // Matixray: a move of a profile the storage refused is told once, each refusal by its number, so that one set again
    // right after the last was told is told too; the groups are shown anew, as stored.
    val context = LocalContext.current
    val failureText = stringResource(R.string.toast_failure)
    val moveRefusal = uiState.moveRefusal
    LaunchedEffect(moveRefusal) {
        if (moveRefusal != null) {
            context.toastError(failureText)
            onAction(MainAction.MoveRefusalShown(moveRefusal))
        }
    }

    val scope = rememberCoroutineScope()
    val tourState = remember { TourState() }
    val showTutorial by mainViewModel.showTutorial.collectAsStateWithLifecycle()
    val selectedName = remember(selectedGuid) {
        selectedGuid?.let { com.v2ray.ang.handler.MmkvManager.decodeServerConfig(it)?.remarks }
    }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }

    // Matixray: the box for configs added by hand. It hands the pasted text to the importer on a worker thread, into
    // the group that is open, then tells the list to read the storage again and answers with the number it added.
    var showManualAdd by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<com.v2ray.ang.dto.GroupMapItem?>(null) }
    val fetchOnlyWorking by mainViewModel.fetchOnlyWorking.collectAsStateWithLifecycle()
    val glassCards by mainViewModel.glassCards.collectAsStateWithLifecycle()
    val autoFetch by mainViewModel.autoFetch.collectAsStateWithLifecycle()
    val isp by mainViewModel.isp.collectAsStateWithLifecycle()
    // Matixray: configs added by hand never go under "Default": they get a new group named by the user; links of
    // subscriptions keep their own way (a group made from the link).
    val defaultGroupName = stringResource(R.string.mx_group_mine)
    val importManual: (String, String, String?, (Int) -> Unit) -> Unit = { text, groupName, targetGroupId, done ->
        val groupId = (uiState.selectedGroupId as Any?)?.toString().orEmpty()
        scope.launch {
            var madeGroup = false
            val added = withContext(Dispatchers.IO) {
                runCatching {
                    val onlySubscriptionLinks = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        .let { lines -> lines.isNotEmpty() && lines.all { com.v2ray.ang.util.Utils.isValidSubUrl(it) } }
                    if (onlySubscriptionLinks) {
                        val (configs, subscriptions) = AngConfigManager.importBatchConfig(text, groupId, false)
                        configs + subscriptions
                    } else if (targetGroupId != null || groupName.isBlank()) {
                        // A chosen group, or no name typed: Default (its configs are stored with an empty group id).
                        val (configs, subscriptions) = AngConfigManager.importBatchConfig(text, targetGroupId ?: "", true)
                        configs + subscriptions
                    } else {
                        val newId = com.v2ray.ang.util.Utils.getUuid()
                        com.v2ray.ang.handler.MmkvManager.encodeSubscription(
                            newId,
                            com.v2ray.ang.dto.entities.SubscriptionItem(
                                remarks = groupName.trim().ifEmpty { defaultGroupName },
                                autoUpdate = false,
                            ),
                        )
                        val (configs, subscriptions) = AngConfigManager.importBatchConfig(text, newId, true)
                        if (configs + subscriptions > 0) {
                            madeGroup = true
                        } else {
                            com.v2ray.ang.handler.MmkvManager.removeSubscription(newId)
                        }
                        configs + subscriptions
                    }
                }.getOrDefault(0)
            }
            if (added > 0) onAction(if (madeGroup) MainAction.RefreshGroups else MainAction.Search(searchQuery))
            done(added)
        }
    }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }

    uiState.updateNotice?.let { notice ->
        InAppUpdateDialog(
            version = notice.latestVersion.orEmpty(),
            url = notice.downloadUrl,
            onDismiss = { onAction(MainAction.DismissUpdateNotice) },
        )
    }

    uiState.pendingImport?.let {
        ImportNameDialog(
            groups = groups,
            onConfirm = { name, target -> onAction(MainAction.ImportBatchConfig(it, name, target)) },
            onDismiss = { onAction(MainAction.DismissImportNaming) },
        )
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    groupToDelete?.let { group ->
        val shownName = if (group.id == com.v2ray.ang.AppConfig.FREE_SUBSCRIPTION_ID) stringResource(R.string.mx_group_free) else group.remarks
        com.v2ray.ang.ui.compose.DeleteConfirmDialog(
            message = stringResource(R.string.mx_group_delete_confirm),
            itemName = shownName,
            onConfirm = { groupToDelete = null; onAction(MainAction.RemoveGroup(group.id)) },
            onDismiss = { groupToDelete = null },
        )
    }
    if (showManualAdd) {
        ManualConfigDialog(onDismiss = { showManualAdd = false }, groups = groups, onSubmit = importManual)
    }
    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            bottomBar = {
                Column {
                    // Matixray: four quick buttons above the connect bar (not on the settings tab).
                    if (bottomTab != MainTab.Settings) {
                        MainQuickActions(
                            fetch = fetchState,
                            testing = uiState.isTesting,
                            // Matixray: the fetch clears only the free group itself (in the ViewModel); the user's own groups,
                            // links and configs are never removed here. A tap while it runs only stops it.
                            onFetch = { onAction(MainAction.FetchConfigs) },
                            onTestAll = {
                                onAction(if (uiState.isTesting) MainAction.CancelTesting else MainAction.TestRealAllServers)
                            },
                            onSort = { onAction(MainAction.SortByTestResults) },
                            onRemoveDead = { showDelInvalidConfirm = true },
                            modifier = Modifier.tourTarget(tourState, TourTarget.QuickActions),
                        )
                    }
                    // The one connect button of the app: the same bar on every tab. On the Psiphon page it connects
                    // Psiphon, now that the round button of that page is gone.
                    val onPsiphonPage = bottomTab == MainTab.Home && homePager.currentPage == MainSection.Psiphon.ordinal
                    MainConnectionBar(
                        isRunning = if (onPsiphonPage) psiphonActive else isRunning,
                        onToggle = { onAction(if (onPsiphonPage) MainAction.TogglePsiphon else MainAction.ToggleService) },
                        onTest = { onAction(MainAction.TestCurrentServer) },
                        modifier = Modifier.tourTarget(tourState, TourTarget.ConnectionBar),
                        pingMillis = uiState.pingMillis,
                        pageKey = onPsiphonPage,
                    )
                    MainBottomBar(selected = bottomTab, onSelect = { bottomTab = it }, tour = tourState)
                }
            },
            floatingActionButton = {},
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    bottomTab == MainTab.Settings -> MainSettingsTab(
                        isRunning = isRunning,
                        elapsedSec = { elapsedSec.value },
                        speed = speed,
                        autoSwitch = autoSwitch,
                        onAutoSwitchChange = { mainViewModel.setAutoSwitch(it) },
                        fetchOnlyWorking = fetchOnlyWorking,
                        onFetchOnlyWorkingChange = { mainViewModel.setFetchOnlyWorking(it) },
                        autoFetch = autoFetch,
                        onAutoFetchChange = { mainViewModel.setAutoFetch(it) },
                        glassCards = glassCards,
                        onGlassCardsChange = { mainViewModel.setGlassCards(it) },
                        isp = isp,
                        onIspChange = { mainViewModel.setIsp(it) },
                        updateAvailable = updateAvailable,
                        onNavigate = onNavigate,
                        tour = tourState,
                    )

                    groups.isEmpty() -> Unit

                    bottomTab == MainTab.Home -> {
                        MainPageHeader(subtitle = stringResource(R.string.mx_nav_home))
                        val section = if (homePager.currentPage == 1) MainSection.Psiphon else MainSection.Main
                        MainSectionTabs(
                            selected = section,
                            onSelect = { target -> scope.launch { homePager.animateScrollToPage(target.ordinal) } },
                            modifier = Modifier.tourTarget(tourState, TourTarget.Sections),
                        )
                        HorizontalPager(
                            state = homePager,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                        ) { page ->
                            if (page == MainSection.Psiphon.ordinal) {
                                PsiphonSection(
                                    connected = psiphonActive,
                                    elapsedSec = { elapsedSec.value },
                                    speed = speed,
                                    pingMillis = uiState.pingMillis,
                                    onStatusClick = { onAction(MainAction.TestCurrentServer) },
                                )
                            } else {
                                MainConnectHero(
                                    isRunning = isRunning,
                                    elapsedSec = { elapsedSec.value },
                                    statusText = displayText,
                                    speed = speed,
                                    pingMillis = uiState.pingMillis,
                                    onStatusClick = { onAction(MainAction.TestCurrentServer) },
                                    bottomExtra = {
                                        SelectedServerCard(name = selectedName, onClick = { bottomTab = MainTab.Configs })
                                    },
                                )
                            }
                        }
                    }

                    else -> {
                        MainConfigsHeader(
                            onAction = onAction,
                            onManualAdd = { showManualAdd = true },
                            onMoreMenuAction = { action ->
                                when (action) {
                                    MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                                    MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                                    MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                                    MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                                    MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                                    MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                                    MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                                    MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                                    MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                                    MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                                }
                            },
                            tour = tourState,
                        )
                        if (showSearch) {
                            MainConfigsSearchField(
                                query = searchQuery,
                                onQueryChange = { query: String ->
                                    searchQuery = query
                                    onAction(MainAction.Search(query))
                                },
                                onClose = {
                                    searchQuery = ""
                                    onAction(MainAction.Search(""))
                                    showSearch = false
                                },
                            )
                        } else {
                            MainConfigsToolbar(
                                onSort = { onAction(MainAction.SortByTestResults) },
                                onSearch = { showSearch = true },
                                onPingAll = { onAction(MainAction.TestRealAllServers) },
                                tour = tourState,
                            )
                        }
                        if (isLoading || uiState.isTesting) {
                            // The thin mint line that runs while the app is busy (tests, updates).
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = com.v2ray.ang.ui.compose.XfMint,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                        }
                        GroupedConfigsList(
                            groups = groups,
                            mainViewModel = mainViewModel,
                            selectedGuid = selectedGuid,
                            selectedGroupId = uiState.selectedGroupId,
                            locateTarget = uiState.locateTarget,
                            searchQuery = searchQuery,
                            fetchBusy = fetchState.busy,
                            hideFreeRows = fetchOnlyWorking,
                            onDeleteGroup = { group -> groupToDelete = group },
                            onSelectGroup = { id -> onAction(MainAction.SelectGroup(id)) },
                            onRefreshGroup = { id -> onAction(MainAction.RefreshGroup(id)) },
                            onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                            onEditServer = { guid, profile -> onAction(MainAction.EditServer(guid, profile)) },
                            onShareServer = { guid, profile -> shareTarget = Triple(guid, profile, false) },
                            onMoreServer = { guid, profile -> shareTarget = Triple(guid, profile, true) },
                            onLocateHandled = { onAction(MainAction.LocateHandled) },
                            contentPadding = PaddingValues(start = 0.dp, top = 2.dp, end = 0.dp, bottom = 16.dp),
                        )
                    }
                }
            }
        }

        // First run only: the tour that circles the parts of the screen one by one.
        if (showTutorial) {
            MainTourOverlay(
                state = tourState,
                onTabChange = { bottomTab = it },
                onFinish = {
                    bottomTab = MainTab.Home
                    mainViewModel.finishTutorial()
                },
            )
        }
    }
}

/**
 * The Psiphon section: its own page in orange, with the same live numbers as the main section. The pill button of the
 * bar at the bottom connects through a Psiphon profile (made on the spot when there is none) or disconnects.
 */
@Composable
private fun PsiphonSection(
    connected: Boolean,
    elapsedSec: () -> Long,
    speed: TrafficSpeed,
    pingMillis: Long?,
    onStatusClick: () -> Unit,
) {
    MainConnectHero(
        isRunning = connected,
        elapsedSec = elapsedSec,
        statusText = stringResource(if (connected) R.string.mx_psiphon_connected else R.string.mx_psiphon_connect),
        speed = speed,
        pingMillis = pingMillis,
        accent = XfOrange,
        accentLight = XfOrangeLight,
        onStatusClick = onStatusClick,
        bottomExtra = {
            Text(
                text = stringResource(R.string.mx_psiphon_section_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            )
        },
    )
}
