package com.v2ray.ang.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.RealPingResult
import com.v2ray.ang.dto.TestServiceMessage
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.extension.delay
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.handler.ConfigFetcher
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import com.v2ray.ang.handler.SpeedtestManager
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.IspChoice
import com.v2ray.ang.handler.ConfigSources
import com.v2ray.ang.extension.matchesPattern
import com.v2ray.ang.extension.moveItem
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.PatternSyntaxException

private fun applyTestDelayResults(
    servers: List<ServersCache>,
    updates: Map<String, Long>,
): List<ServersCache> = servers.map { server ->
    val delayMillis = updates[server.guid]
    if (delayMillis == null || delayMillis == server.testDelayMillis) {
        server
    } else {
        server.copy(testDelayMillis = delayMillis)
    }
}

private fun applyTestDelayResultsToRows(
    rows: List<ServerRowUiModel>,
    updates: Map<String, Long>,
): List<ServerRowUiModel> = rows.map { row ->
    val delayMillis = updates[row.guid]
    if (delayMillis == null || delayMillis == row.testDelayMillis) {
        row
    } else {
        row.copy(testDelayMillis = delayMillis)
    }
}

class MainViewModel(
    application: Application,
    private val dataSource: MainDataSource
) : BaseViewModel(application) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val preloadDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    // ---------- UI state ----------
    private val _uiState = MutableStateFlow(
        MainUiState(
            selectedGroupId = dataSource.getSelectedSubscriptionId(),
            selectedGuid = dataSource.getSelectServer(),
            confirmRemove = dataSource.getConfirmRemove(),
            doubleColumnDisplay = dataSource.getDoubleColumnDisplay()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Whether the running tunnel is a profile that uses Psiphon (an Aether profile with Psiphon on): what the
     * "connect to Psiphon" button shows. Read off the main thread.
     */
    val psiphonActive: StateFlow<Boolean> = uiState
        .map { state ->
            state.isRunning && state.selectedGuid?.let { MmkvManager.decodeServerConfig(it)?.aetherPsiphon } != null
        }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Matixray: download and upload speed of the app while the service runs, from the system's per-app byte counters.
     * The core's own counters are not used here: reading them resets them, and the speed notification owns that.
     * Read off the main thread, once a second, and only while the service runs.
     */
    val trafficSpeed: StateFlow<TrafficSpeed> = uiState
        .map { it.isRunning }
        .distinctUntilChanged()
        .flatMapLatest { running ->
            if (!running) {
                flowOf(TrafficSpeed())
            } else {
                flow {
                    val uid = android.os.Process.myUid()
                    var lastRx = android.net.TrafficStats.getUidRxBytes(uid)
                    var lastTx = android.net.TrafficStats.getUidTxBytes(uid)
                    var lastTime = android.os.SystemClock.elapsedRealtime()
                    while (true) {
                        kotlinx.coroutines.delay(1000L)
                        val rx = android.net.TrafficStats.getUidRxBytes(uid)
                        val tx = android.net.TrafficStats.getUidTxBytes(uid)
                        val now = android.os.SystemClock.elapsedRealtime()
                        val seconds = ((now - lastTime) / 1000.0).coerceAtLeast(0.001)
                        // UNSUPPORTED (-1) on a device without per-app counters: show zero instead of a bogus speed
                        val down = if (rx < 0 || lastRx < 0) 0L else ((rx - lastRx) / seconds).toLong().coerceAtLeast(0L)
                        val up = if (tx < 0 || lastTx < 0) 0L else ((tx - lastTx) / seconds).toLong().coerceAtLeast(0L)
                        emit(TrafficSpeed(downBytesPerSec = down, upBytesPerSec = up))
                        lastRx = rx
                        lastTx = tx
                        lastTime = now
                    }
                }
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrafficSpeed())

    /** True from the "auto connect" tap until the real tests it started have finished. */
    private var autoConnectPending = false

    // Matixray: the "get configs" button. Declared before init, whose work off the main thread may read them.
    private val _fetchState = MutableStateFlow(ConfigFetchState())
    val fetchState: StateFlow<ConfigFetchState> = _fetchState.asStateFlow()
    private var fetchJob: Job? = null

    // Matixray: the liquid-glass look of the cards can be turned off.
    private val _glassCards = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_GLASS_CARDS, false))
    val glassCards: StateFlow<Boolean> = _glassCards.asStateFlow()

    fun setGlassCards(enabled: Boolean) {
        _glassCards.value = enabled
        com.v2ray.ang.ui.compose.GlassPrefs.enabled = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_GLASS_CARDS, enabled)
    }

    // Matixray: the internet provider the user says they use (see IspChoice); "get configs" reads the lists that fit it.
    private val _isp = MutableStateFlow(MmkvManager.decodeSettingsString(AppConfig.PREF_ISP) ?: IspChoice.AUTO)
    val isp: StateFlow<String> = _isp.asStateFlow()

    fun setIsp(id: String) {
        if (id !in IspChoice.all) return
        _isp.value = id
        MmkvManager.encodeSettings(AppConfig.PREF_ISP, id)
    }

    /** True from the end of a "get configs" download until the tests it started finish; then the list is sorted. */
    private var sortAfterTests = false

    // "Connect to the best server when the server is cut": a probe of the current server every 25 s while connected;
    // two failed probes in a row start the same real-ping search as the auto-connect button.
    private val _autoSwitch = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_SWITCH_ON_DROP, false))
    val autoSwitch: StateFlow<Boolean> = _autoSwitch.asStateFlow()

    // Matixray: "get configs" tests each config first and keeps only the working ones.
    private val _fetchOnlyWorking = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_FETCH_ONLY_WORKING, true))
    val fetchOnlyWorking: StateFlow<Boolean> = _fetchOnlyWorking.asStateFlow()

    fun setFetchOnlyWorking(enabled: Boolean) {
        _fetchOnlyWorking.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_FETCH_ONLY_WORKING, enabled)
    }

    /** True from the end of a "get configs" download until its tests finish and the dead configs are removed. */
    private var purgeDeadAfterTests = false

    /** The group the user was looking at when a background fetch started; shown again when that fetch is done. */
    private var restoreGroupAfterFetch: String? = null

    // Matixray: the free configs are fetched and tested by themselves when the app opens, so a tested list is waiting.
    private val _autoFetch = MutableStateFlow(MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_FETCH, true))
    val autoFetch: StateFlow<Boolean> = _autoFetch.asStateFlow()

    fun setAutoFetch(enabled: Boolean) {
        _autoFetch.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_FETCH, enabled)
    }

    private fun restoreGroupIfNeeded() {
        val previous = restoreGroupAfterFetch ?: return
        restoreGroupAfterFetch = null
        if (_uiState.value.groups.any { it.id == previous }) subscriptionIdChanged(previous)
    }

    /** Starts a quiet fetch when the app opens, if the free group is empty or its last fetch is old. */
    private fun autoFetchOnStart() {
        if (!_autoFetch.value) return
        viewModelScope.launch(ioDispatcher) {
            kotlinx.coroutines.delay(AUTO_FETCH_DELAY_MS)
            val last = MmkvManager.decodeSettingsString(AppConfig.PREF_FREE_LAST_FETCH)?.toLongOrNull() ?: 0L
            val stale = System.currentTimeMillis() - last > AUTO_FETCH_MAX_AGE_MS
            val empty = dataSource.getServerGuidList(AppConfig.FREE_SUBSCRIPTION_ID).isEmpty()
            if (_fetchState.value.busy || _uiState.value.isTesting || !(stale || empty)) return@launch
            withContext(Dispatchers.Main) { fetchConfigs(silent = true) }
        }
    }
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()
    private var probeFailStreak = 0
    private var lastAutoSwitchAtMs = 0L

    // Matixray: the first-run tour is shown once; this remembers that it was finished or skipped.
    private val _showTutorial = MutableStateFlow(!MmkvManager.decodeSettingsBool(AppConfig.PREF_TUTORIAL_DONE, false))
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    fun finishTutorial() {
        _showTutorial.value = false
        MmkvManager.encodeSettings(AppConfig.PREF_TUTORIAL_DONE, true)
    }

    fun setAutoSwitch(enabled: Boolean) {
        _autoSwitch.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_SWITCH_ON_DROP, enabled)
        probeFailStreak = 0
    }

    private var pingJob: Job? = null
    private val silentProbeIds: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())

    /**
     * Shows the ping of the connection by itself: the first test starts 0.6 s after the tunnel is up and is repeated
     * every 2 s until a result arrives, then refreshed every 20 s while connected. The probes are silent (they change
     * only the ping number, not the status line), and their results also feed the "connect to the best server when the
     * server is cut" option.
     */
    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(600L)
            var quickTries = 0
            while (_uiState.value.isRunning) {
                val state = _uiState.value
                if (!state.isTesting && !autoConnectPending) testCurrentServerRealPing(silent = true)
                val waitingForFirst = _uiState.value.pingMillis == null && quickTries++ < 6
                kotlinx.coroutines.delay(if (waitingForFirst) 2_000L else 20_000L)
            }
        }
    }

    private fun onProbeResult(delayMillis: Long) {
        if (delayMillis >= 0L) {
            probeFailStreak = 0
            return
        }
        // The first 30 s after a start or a switch are not judged: the tunnel may still be coming up.
        if (System.currentTimeMillis() - lastAutoSwitchAtMs < 30_000L) return
        probeFailStreak++
        if (_autoSwitch.value && probeFailStreak >= 2 && !autoConnectPending) {
            probeFailStreak = 0
            lastAutoSwitchAtMs = System.currentTimeMillis()
            startAutoConnect()
        }
    }
    private val _autoConnectEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once the fastest working profile has been selected and the service should (re)start on it. */
    val autoConnectEvents: SharedFlow<Unit> = _autoConnectEvents.asSharedFlow()

    private fun startAutoConnect() {
        if (currentServers().isEmpty()) return
        testAllRealPing()
        autoConnectPending = true
    }

    private fun finishAutoConnect() {
        if (!autoConnectPending) return
        autoConnectPending = false
        val guids = currentServers().map { it.guid }
        viewModelScope.launch {
            val best = withContext(ioDispatcher) {
                guids.mapNotNull { guid ->
                    MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis
                        ?.takeIf { it > 0L }
                        ?.let { guid to it }
                }.minByOrNull { it.second }
            } ?: return@launch
            updateSelectedGuid(best.first)
            _autoConnectEvents.tryEmit(Unit)
        }
    }

    /** The GUID of the first stored profile that uses Psiphon, or null when there is none. */
    suspend fun firstPsiphonGuid(): String? = withContext(ioDispatcher) {
        MmkvManager.decodeAllServerList().firstOrNull { MmkvManager.decodeServerConfig(it)?.aetherPsiphon != null }
    }

    /**
     * The GUID of a profile that uses Psiphon: the first stored one, or a new Psiphon-only Aether profile that is made
     * on the spot, so the \"connect to Psiphon\" button needs no profile added by hand. Null when it could not be stored.
     */
    suspend fun ensurePsiphonGuid(): String? = withContext(ioDispatcher) {
        firstPsiphonGuid() ?: try {
            val guid = MmkvManager.encodeServerConfig(
                "",
                ProfileItem.create(com.v2ray.ang.enums.EConfigType.AETHER).copy(
                    remarks = "Psiphon",
                    server = "",
                    serverPort = "",
                    aetherProtocol = com.v2ray.ang.enums.AetherProtocol.WIREGUARD.type,
                    aetherTransport = com.v2ray.ang.enums.AetherTransport.HTTP3.type,
                    aetherScanMode = com.v2ray.ang.enums.AetherScanMode.BALANCED.type,
                    aetherObfuscation = com.v2ray.ang.enums.AetherObfuscation.AUTO.type,
                    aetherFingerprint = com.v2ray.ang.enums.AetherFingerprint.CHROME.type,
                    aetherIpVersion = com.v2ray.ang.enums.AetherIpVersion.V4.type,
                    aetherPsiphon = com.v2ray.ang.enums.AetherPsiphon.ONLY.type,
                    aetherPsiphonMode = com.v2ray.ang.enums.AetherPsiphonMode.AUTO.type,
                )
            )
            com.v2ray.ang.handler.SettingsChangeManager.makeSetupGroupTab()
            guid
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Keyword filtering ----------
    @Volatile
    private var keywordFilter: String = ""
    private var filterJob: Job? = null

    // ---------- Groups & cache ----------
    private val cacheMutex = Mutex()
    private val groupDataCache = mutableMapOf<String, List<ServersCache>>()
    private val groupUiFlows = ConcurrentHashMap<String, MutableStateFlow<ServerGroupUiState>>()
    private val groupServerFlows = ConcurrentHashMap<String, StateFlow<List<ServersCache>>>()
    private val groupLoadMutexes = ConcurrentHashMap<String, Mutex>()
    private val serverOrderPersistenceJobs = mutableMapOf<String, Job>()

    /**
     * Matixray: per group, the moves asked for and not stored yet, and the groups one of whose moves the storage refused,
     * see [moveServer]. Concurrent, as a move's job may end on the thread its store ended on, as under an unconfined
     * dispatcher.
     */
    private val unstoredMoves = ConcurrentHashMap<String, Int>()
    private val refusedMoveGroups: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Matixray: the number the last refusal of a move to be told got, see [MainUiState.moveRefusal]. */
    private val moveRefusals = AtomicInteger()

    /**
     * Matixray: per group, the moves shown, and those settled, stored or refused, see [storeMove]. A reading of a group
     * taken before every move shown was settled is held back, see [updateGroupUi], and the group is shown anew once its
     * moves are, see [moveServer]; [groupsReadEarly] names the groups waiting for that. A move is shown, and a reading
     * shown or held back, under [groupShowLock], one at a time.
     */
    private val shownMoves = ConcurrentHashMap<String, Int>()
    private val settledMoves = ConcurrentHashMap<String, Int>()
    private val groupsReadEarly: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val groupShowLock = Any()

    /**
     * Matixray: per group, the number the last reading got, in the order the readings were taken, which the cached list,
     * written with it, is too, and the number of the reading shown last: a reading older than the one shown is not
     * shown over it, see [updateGroupUi].
     */
    private val readingNumbers = ConcurrentHashMap<String, Int>()
    private val shownReadings = ConcurrentHashMap<String, Int>()

    private var setupGroupJob: Job? = null
    private var preloadJob: Job? = null
    private var selectedGroupLoadJob: Job? = null
    private var reloadJob: Job? = null
    private var testResultFlushJob: Job? = null
    private val pendingTestResults = linkedMapOf<String, Long>()

    private val testRequests = MainTestRequests()
    private var bulkTestJob: Job? = null

    private val initialPageReady = CompletableDeferred<Unit>()

    /**
     * Matixray: the names the Aether editor gives the WARP protocols, which the rows of Aether profiles show. Declared
     * before init, whose work off the main thread reads it: a delegate declared after it may not be there yet.
     */
    private val aetherProtocolLabel by lazy {
        aetherProtocolLabels(
            dataSource.getStringArray(R.array.aether_protocol_entries),
            dataSource.getStringArray(R.array.aether_protocol_values),
        )
    }

    // ---------- Service events ----------
    init {
        collectServiceEvents()
        setupGroupTab()
    }

    private fun collectServiceEvents() {
        viewModelScope.launch {
            dataSource.mainServiceEvent.collect { event ->
                handleServiceEvent(event)
            }
        }
    }

    private fun handleServiceEvent(event: MainServiceEvent) {
        when (event) {
            MainServiceEvent.StateRunning -> updateRunningState(true, clearTestingText = false)
            MainServiceEvent.StateNotRunning -> updateRunningState(false, clearTestingText = false)
            MainServiceEvent.StateStartSuccess -> {
                toastSuccess(R.string.toast_services_success)
                probeFailStreak = 0
                lastAutoSwitchAtMs = System.currentTimeMillis()
                updateRunningState(true)
            }

            is MainServiceEvent.StateStartFailure -> {
                // The daemon attaches a reason only when it is a localized resource string, e.g.
                // the Aether core stopping or missing on this ABI; the generic text is the fallback.
                if (!event.message.isNullOrBlank()) {
                    toastError(event.message)
                } else {
                    toastError(R.string.toast_services_failure)
                }
                updateRunningState(false)
            }

            is MainServiceEvent.StateConnecting -> {
                if (uiState.value.isRunning) {
                    _uiState.update { it.copy(status = MainStatus.Connecting(event.message)) }
                }
            }

            MainServiceEvent.StateStopSuccess -> updateRunningState(false)
            is MainServiceEvent.MeasureDelayResult -> {
                if (!uiState.value.isRunning || !testRequests.completeCurrent(event.requestId)) return
                onProbeResult(event.result.delayMillis)
                if (silentProbeIds.remove(event.requestId)) {
                    // A background probe: only the ping number changes.
                    _uiState.update { it.copy(pingMillis = event.result.delayMillis.takeIf { delay -> delay >= 0L }) }
                    return
                }
                _uiState.update {
                    it.copy(
                        isTesting = testRequests.isTesting,
                        status = MainStatus.ConnectionTest(event.result),
                        pingMillis = event.result.delayMillis.takeIf { delay -> delay >= 0L },
                    )
                }
            }

            is MainServiceEvent.MeasureConfigSuccess -> {
                val request = testRequests.bulk?.takeIf { it.id == event.requestId } ?: return
                queueTestResult(event.result, request)
            }

            is MainServiceEvent.MeasureConfigNotify -> {
                if (event.requestId == testRequests.bulk?.id) {
                    _uiState.update { it.copy(status = MainStatus.TestProgress(event.progress)) }
                }
            }

            is MainServiceEvent.MeasureConfigFinish -> {
                val request = testRequests.bulk?.takeIf { it.id == event.requestId } ?: return
                val scheduledFlush = testResultFlushJob
                testResultFlushJob = viewModelScope.launch {
                    scheduledFlush?.cancelAndJoin()
                    if (testRequests.bulk?.id != request.id) return@launch
                    flushPendingTestResults(request)
                    onTestsFinished(request.id)
                }
            }

            is MainServiceEvent.MeasureDelayCancelled -> {
                if (testRequests.completeCurrent(event.requestId)) resetTestStatus()
            }

            is MainServiceEvent.MeasureConfigCancelled -> {
                if (testRequests.completeBulk(event.requestId) != null) {
                    cancelPendingTestResults()
                    resetTestStatus()
                }
            }

            MainServiceEvent.ServersChanged -> setupGroupTab(forceRefresh = true)
        }
    }

    private fun queueTestResult(result: RealPingResult, request: MainTestRequests.Bulk) {
        pendingTestResults[result.guid] = result.delayMillis
        if (testResultFlushJob?.isActive == true) return

        testResultFlushJob = viewModelScope.launch {
            while (pendingTestResults.isNotEmpty()) {
                delay(TEST_RESULT_FLUSH_INTERVAL_MS)
                flushPendingTestResults(request)
            }
        }
    }

    private suspend fun flushPendingTestResults(request: MainTestRequests.Bulk) {
        if (pendingTestResults.isEmpty()) return
        if (testRequests.bulk?.id != request.id) return

        val updates = cacheMutex.withLock {
            val drained = pendingTestResults.toMap()
            pendingTestResults.clear()
            groupDataCache[request.groupId]?.let { cached ->
                groupDataCache[request.groupId] = applyTestDelayResults(cached, drained)
            }
            drained
        }
        if (updates.isEmpty()) return
        if (testRequests.bulk?.id != request.id) return
        mutableServerGroupState(request.groupId).update { current ->
            current.copy(
                servers = applyTestDelayResults(current.servers, updates),
                rows = applyTestDelayResultsToRows(current.rows, updates),
            )
        }
    }

    internal fun formatStatus(status: MainStatus): String = when (status) {
        MainStatus.Disconnected -> dataSource.getString(R.string.connection_not_connected)
        MainStatus.Connected -> dataSource.getString(R.string.connection_connected)
        is MainStatus.Connecting -> status.message
        MainStatus.Testing -> dataSource.getString(R.string.connection_test_testing)
        is MainStatus.TestProgress -> dataSource.getString(
            R.string.connection_running_task_left,
            status.progress
        )

        is MainStatus.ConnectionTest -> formatConnectionTestResult(status.result)
    }

    private fun formatConnectionTestResult(result: ConnectionTestResult): String {
        val status = if (result.delayMillis >= 0) {
            val delay = dataSource.getString(R.string.server_test_delay_value, result.delayMillis)
            dataSource.getString(R.string.connection_test_available, delay)
        } else {
            val detail = result.errorMessage.ifBlank {
                dataSource.getString(R.string.connection_test_empty_message)
            }
            dataSource.getString(R.string.connection_test_error, detail)
        }

        if (result.delayMillis < 0 || (result.country == null && result.ipAddress == null)) {
            return status
        }

        val unknown = dataSource.getString(R.string.value_unknown)
        return "$status\n(${result.country ?: unknown}) ${result.ipAddress ?: unknown}"
    }

    // ---------- Public state accessors ----------
    fun serversForGroup(groupId: String): StateFlow<List<ServersCache>> =
        groupServerFlows.computeIfAbsent(groupId) {
            val groupState = mutableServerGroupState(groupId)
            groupState
                .map { it.servers }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                    initialValue = groupState.value.servers,
                )
        }

    internal fun serverGroupState(groupId: String): StateFlow<ServerGroupUiState> =
        mutableServerGroupState(groupId).asStateFlow()

    private fun mutableServerGroupState(groupId: String): MutableStateFlow<ServerGroupUiState> =
        groupUiFlows.computeIfAbsent(groupId) { MutableStateFlow(ServerGroupUiState()) }

    private fun currentServers(): List<ServersCache> =
        mutableServerGroupState(uiState.value.selectedGroupId).value.servers

    // ---------- Action handler ----------
    fun onAction(action: MainAction) {
        when (action) {
            MainAction.Initialize -> initialize()
            MainAction.RefreshServiceState -> dataSource.queryServiceState()
            MainAction.RefreshGroups -> setupGroupTab(forceRefresh = true)
            MainAction.TestAllServers -> testAllRealPing(true)
            MainAction.TestRealAllServers -> testAllRealPing()
            MainAction.CancelTesting -> cancelAllPing()
            MainAction.RemoveAllServers -> removeAllServerAsync()
            MainAction.RemoveDuplicateServers -> removeDuplicateServerAsync()
            MainAction.RemoveInvalidServers -> removeInvalidServerAsync()
            MainAction.SortByTestResults -> sortByTestResultsAsync()
            MainAction.UpdateSubscriptions -> importConfigViaSub()
            MainAction.FetchConfigs -> toggleFetchConfigs()
            MainAction.ExportAll -> exportAllAsync()
            is MainAction.SelectGroup -> subscriptionIdChanged(action.groupId)
            is MainAction.SelectServer -> updateSelectedGuid(action.guid)
            is MainAction.RemoveServer -> removeServerAndRefresh(action.guid)
            is MainAction.MoveServer -> moveServer(action.groupId, action.fromGuid, action.toGuid)
            is MainAction.Search -> filterConfig(action.query)
            is MainAction.ImportBatchConfig -> importBatchConfig(action.configText, action.groupName, action.targetGroupId)
            is MainAction.RemoveGroup -> removeGroup(action.groupId)
            MainAction.DismissImportNaming -> _uiState.update { it.copy(pendingImport = null) }
            is MainAction.RefreshGroup -> refreshGroup(action.groupId)
            MainAction.LocateHandled -> consumeLocateTarget()
            is MainAction.MoveRefusalShown -> _uiState.update {
                if (it.moveRefusal == action.refusal) it.copy(moveRefusal = null) else it
            }
            is MainAction.ShareQRCode -> {
                val bitmap = dataSource.share2QRCode(action.guid)
                _uiState.update { it.copy(shareQRCodeBitmap = bitmap) }
            }

            MainAction.DismissUpdateNotice -> _uiState.update { it.copy(updateNotice = null) }
            MainAction.DismissQRCodeDialog -> {
                _uiState.update { it.copy(shareQRCodeBitmap = null) }
            }

            MainAction.AutoConnectBest -> startAutoConnect()

            MainAction.ToggleService,
            MainAction.TogglePsiphon,
            MainAction.TestCurrentServer,
            MainAction.ImportQRcode,
            MainAction.ImportClipboard,
            MainAction.ImportConfigLocal,
            is MainAction.ImportManually,
            MainAction.RestartService,
            MainAction.LocateSelectedServer,
            is MainAction.EditServer,
            is MainAction.ShareClipboard,
            is MainAction.ShareFullContent -> {
                // Handled by Activity via its onAction lambda
            }
        }
    }

    /**
     * Matixray: asks the release page once per process whether a newer version than the running one is out, and if so
     * announces it. A failed check (no network, rate limit) is only logged: the user can still check by hand.
     */
    private fun checkForUpdateOnStart() {
        if (updateCheckedThisProcess) return
        updateCheckedThisProcess = true
        viewModelScope.launch(ioDispatcher) {
            try {
                // Let the first screen settle before the network is used.
                kotlinx.coroutines.delay(4000L)
                val result = com.v2ray.ang.handler.UpdateCheckerManager.checkForUpdate(
                    MmkvManager.decodeSettingsBool(AppConfig.PREF_CHECK_UPDATE_PRE_RELEASE, false)
                )
                if (result.hasUpdate) {
                    _updateAvailable.value = true
                    _uiState.update { it.copy(updateNotice = result) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Update check on start failed", error)
            }
        }
    }

    // ---------- Initialization ----------
    fun initialize() {
        checkForUpdateOnStart()
        autoFetchOnStart()
        viewModelScope.launch(preloadDispatcher) {
            try {
                initialPageReady.await()
                delay(32)
                dataSource.initAssets()
                dataSource.syncSubscriptions()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Main background initialization failed", error)
            }
        }
    }

    fun refreshUiSettings() {
        _uiState.update {
            it.copy(
                confirmRemove = dataSource.getConfirmRemove(),
                doubleColumnDisplay = dataSource.getDoubleColumnDisplay()
            )
        }
    }

    // ---------- Group & server loading ----------
    private suspend fun buildServersCache(guids: List<String>): List<ServersCache> =
        guids.mapNotNull { guid ->
            currentCoroutineContext().ensureActive()
            val profile = dataSource.decodeServerConfig(guid) ?: return@mapNotNull null
            val affiliation = dataSource.decodeAffiliationInfo(guid)
            ServersCache(
                guid = guid,
                profile = profile.copy(),
                testDelayMillis = affiliation?.testDelayMillis ?: 0L
            )
        }

    /**
     * Matixray: the servers of a group as read, with how many of its moves were settled then, and the number of the reading,
     * see [updateGroupUi].
     */
    private class GroupReading(val servers: List<ServersCache>, val settledMoves: Int, val number: Int)

    private suspend fun loadGroup(
        groupId: String,
        forceRefresh: Boolean = false
    ): GroupReading {
        val loadMutex = groupLoadMutexes.computeIfAbsent(groupId) { Mutex() }
        return loadMutex.withLock {
            if (!forceRefresh) {
                cacheMutex.withLock { groupDataCache[groupId]?.let { return@withLock it } }
            }
            val servers = buildServersCache(dataSource.getServerGuidList(groupId))
            currentCoroutineContext().ensureActive()
            // Matixray: under the group's load lock, which a move is stored under too, see [storeMove], and numbered in turn.
            cacheMutex.withLock {
                groupDataCache[groupId] = servers
                val number = readingNumbers.merge(groupId, 1, Int::plus)!!
                GroupReading(servers, settledMoves.getOrDefault(groupId, 0), number)
            }
        }
    }

    private fun applyKeywordFilter(servers: List<ServersCache>, filter: String): List<ServersCache> {
        val keyword = filter.trim()
        if (keyword.isEmpty()) return servers
        val regex = try {
            Regex(keyword, RegexOption.IGNORE_CASE)
        } catch (_: PatternSyntaxException) {
            return servers
        }
        return servers.filter { cache ->
            val profile = cache.profile
            profile.remarks.matchesPattern(regex, keyword) ||
                    profile.description.orEmpty().matchesPattern(regex, keyword) ||
                    profile.server.orEmpty().matchesPattern(regex, keyword) ||
                    profile.configType.name.matchesPattern(regex, keyword)
        }
    }

    /**
     * Shows [reading] of the group [groupId]. Matixray: as [readingFate] decides: not over a newer reading shown, and not
     * when a move of the group was shown that was not settled when the group was read, which the reading would undo on
     * the screen; the group is then shown anew once its moves are settled, see [moveServer], or at once when they are
     * by now. Nor when the search changed while it was filtered: the group is then read and filtered anew.
     */
    private fun updateGroupUi(groupId: String, reading: GroupReading) {
        val filter = keywordFilter
        val filteredServers = applyKeywordFilter(reading.servers, filter)
        val state = ServerGroupUiState(
            servers = filteredServers,
            rows = buildServerRows(groupId, filteredServers)
        )
        val readAgain = synchronized(groupShowLock) {
            val fate = readingFate(
                number = reading.number,
                lastShown = shownReadings.getOrDefault(groupId, 0),
                settledWhenRead = reading.settledMoves,
                shownMoves = shownMoves.getOrDefault(groupId, 0),
                unsettledMoves = unstoredMoves.getOrDefault(groupId, 0),
                filteredAsSearched = filter == keywordFilter,
            )
            when (fate) {
                ReadingFate.SHOW -> {
                    shownReadings[groupId] = reading.number
                    mutableServerGroupState(groupId).value = state
                }
                ReadingFate.HOLD_UNTIL_SETTLED -> groupsReadEarly += groupId
                ReadingFate.OVERTAKEN, ReadingFate.READ_AGAIN -> Unit
            }
            fate == ReadingFate.READ_AGAIN
        }
        if (readAgain) viewModelScope.launch { showGroupAnew(groupId) }
    }

    /** Matixray: reads the group [groupId] anew and shows it, see [updateGroupUi]. */
    private suspend fun showGroupAnew(groupId: String) {
        val reading = withContext(ioDispatcher) { loadGroup(groupId) }
        withContext(defaultDispatcher) { updateGroupUi(groupId, reading) }
    }

    private fun buildServerRows(groupId: String, servers: List<ServersCache>): List<ServerRowUiModel> {
        val subscriptionRemarks = if (groupId.isEmpty()) {
            servers.asSequence()
                .map { it.profile.subscriptionId }
                .filter { it.isNotEmpty() }
                .distinct()
                .associateWith { subscriptionId ->
                    dataSource.getSubscriptionItem(subscriptionId)?.remarks.orEmpty()
                }
        } else {
            emptyMap()
        }
        return servers.map { server ->
            buildServerRowUiModel(
                server = server,
                subscriptionRemarks = subscriptionRemarks[server.profile.subscriptionId].orEmpty(),
                aetherProtocolLabel = aetherProtocolLabel,
            )
        }
    }

    fun getSubscriptions(): List<SubscriptionCache> = dataSource.getSubscriptions()

    private fun resolveSelectedGroup(groups: List<GroupMapItem>): String {
        val current = uiState.value.selectedGroupId
        val resolved = when {
            groups.isEmpty() -> ""
            groups.any { it.id == current } -> current
            else -> groups.first().id
        }
        if (resolved != current) {
            dataSource.setSelectedSubscriptionId(resolved)
        }
        return resolved
    }

    private fun radialPreloadOrder(groups: List<GroupMapItem>, selectedIndex: Int): List<String> {
        if (groups.isEmpty()) return emptyList()
        val result = ArrayList<String>((groups.size - 1).coerceAtLeast(0))
        for (distance in 1 until groups.size) {
            val right = selectedIndex + distance
            val left = selectedIndex - distance
            if (right in groups.indices) result += groups[right].id
            if (left in groups.indices) result += groups[left].id
        }
        return result
    }

    fun setupGroupTab(forceRefresh: Boolean = false): Job {
        setupGroupJob?.cancel()
        preloadJob?.cancel()
        selectedGroupLoadJob?.cancel()

        return viewModelScope.launch(ioDispatcher) {
            try {
                if (forceRefresh) {
                    cacheMutex.withLock { groupDataCache.clear() }
                }
                val groups = dataSource.getSubscriptions().map {
                    GroupMapItem(id = it.guid, remarks = it.subscription.remarks)
                }
                val selectedGroup = resolveSelectedGroup(groups)
                val validIds = groups.mapTo(HashSet()) { it.id }
                groupUiFlows.keys.removeAll { it !in validIds }
                groupServerFlows.keys.removeAll { it !in validIds }
                groupLoadMutexes.keys.removeAll { it !in validIds }

                _uiState.update {
                    it.copy(
                        groups = groups,
                        selectedGroupId = selectedGroup,
                        selectedGuid = dataSource.getSelectServer(),
                    )
                }
                groups.forEach { mutableServerGroupState(it.id) }

                if (groups.isEmpty()) {
                    cacheMutex.withLock { groupDataCache.clear() }
                    return@launch
                }

                val selectedServers = loadGroup(selectedGroup, forceRefresh)
                updateGroupUi(selectedGroup, selectedServers)

                if (!initialPageReady.isCompleted) {
                    initialPageReady.complete(Unit)
                }

                val selectedIndex =
                    groups.indexOfFirst { it.id == selectedGroup }.coerceAtLeast(0)
                val preloadOrder = radialPreloadOrder(groups, selectedIndex)
                preloadJob = viewModelScope.launch(preloadDispatcher) {
                    preloadOrder.forEach { groupId ->
                        ensureActive()
                        delay(32)
                        val servers = loadGroup(groupId, forceRefresh)
                        updateGroupUi(groupId, servers)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to set up group tabs", error)
            } finally {
                if (!initialPageReady.isCompleted) {
                    initialPageReady.complete(Unit)
                }
            }
        }.also { setupGroupJob = it }
    }

    // ---------- Business actions (coroutine-based) ----------
    private fun importBatchConfig(configText: String, groupName: String?, targetGroupId: String?) {
        // Matixray: the user picks the group of the configs they add (or names a new one); with no choice they go to
        // Default. Only links of subscriptions keep the way they had (a group made from the link itself).
        val onlySubscriptionLinks = configText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .let { lines -> lines.isNotEmpty() && lines.all { Utils.isValidSubUrl(it) } }
        if (groupName == null && targetGroupId == null && !onlySubscriptionLinks) {
            _uiState.update { it.copy(pendingImport = configText) }
            return
        }
        _uiState.update { it.copy(pendingImport = null) }
        launchLoading {
            withContext(ioDispatcher) {
                var madeGroup: String? = null
                try {
                    // No group chosen and no name typed: the configs go to Default, as they always did.
                    val target = when {
                        onlySubscriptionLinks -> uiState.value.selectedGroupId
                        targetGroupId != null -> targetGroupId
                        groupName.isNullOrBlank() -> AppConfig.DEFAULT_SUBSCRIPTION_ID
                        else -> dataSource.createLocalGroup(groupName.trim()).also { madeGroup = it }
                    }
                    val movesSelection = !onlySubscriptionLinks && target != uiState.value.selectedGroupId
                    val (count, countSub) = dataSource.importBatchConfig(configText, target, true)
                    when {
                        count > 0 -> {
                            toast(dataSource.getString(R.string.title_import_config_count, count))
                            if (movesSelection) {
                                dataSource.setSelectedSubscriptionId(target)
                                _uiState.update { it.copy(selectedGroupId = target) }
                            }
                            setupGroupTab(forceRefresh = true)
                        }

                        countSub > 0 -> setupGroupTab(forceRefresh = true)
                        else -> {
                            madeGroup?.let { dataSource.removeGroup(it) }
                            toastError(R.string.toast_failure)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    madeGroup?.let { dataSource.removeGroup(it) }
                    throw cancelled
                } catch (e: Exception) {
                    madeGroup?.let { dataSource.removeGroup(it) }
                    LogUtil.e(AppConfig.TAG, "Failed to import batch config", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    /** Matixray: the delete button of a group card; the Default group is never removed. */
    private fun removeGroup(groupId: String) {
        if (groupId.isEmpty() || groupId == AppConfig.DEFAULT_SUBSCRIPTION_ID) return
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    dataSource.removeGroup(groupId)
                    toast(dataSource.getString(R.string.mx_group_deleted))
                    cacheMutex.withLock { groupDataCache.clear() }
                    setupGroupTab(forceRefresh = true).join()
                    refreshSelectedGuid()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Failed to remove group", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    /**
     * Matixray: the refresh button of a group card. A group with a link is downloaded again, the free group is fetched
     * anew (and nothing else is touched), any other group gets its real ping tested.
     */
    private fun refreshGroup(groupId: String) {
        if (groupId.isEmpty()) return
        if (groupId == AppConfig.FREE_SUBSCRIPTION_ID) {
            if (!_fetchState.value.busy) fetchConfigs()
            return
        }
        val item = dataSource.getSubscriptionItem(groupId) ?: return
        if (item.url.isNotEmpty()) {
            launchLoading {
                withContext(ioDispatcher) {
                    try {
                        val result = dataSource.updateConfigViaSub(SubscriptionCache(groupId, item))
                        if (result.successCount > 0) {
                            toast(getQuantityString(R.plurals.title_update_config_count, result.configCount, result.configCount))
                            setupGroupTab(forceRefresh = true)
                        } else {
                            toastError(R.string.toast_failure)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "Group refresh failed", e)
                        toastError(R.string.toast_failure)
                    }
                }
            }
        } else {
            subscriptionIdChanged(groupId)
            testAllRealPing()
        }
    }

    private fun importConfigViaSub() {
        val subId = uiState.value.selectedGroupId
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val result = if (subId.isEmpty()) {
                        dataSource.updateConfigViaSubAll()
                    } else {
                        val item = dataSource.getSubscriptionItem(subId) ?: return@withContext
                        dataSource.updateConfigViaSub(SubscriptionCache(subId, item))
                    }
                    when {
                        result.successCount + result.failureCount + result.skipCount == 0 ->
                            toast(R.string.title_update_subscription_no_subscription)

                        result.successCount > 0 && result.failureCount + result.skipCount == 0 ->
                            toast(
                                getQuantityString(
                                    R.plurals.title_update_config_count,
                                    result.configCount,
                                    result.configCount,
                                )
                            )

                        else ->
                            toast(dataSource.getString(R.string.title_update_subscription_result, result.configCount, result.successCount, result.failureCount, result.skipCount))
                    }
                    if (result.configCount > 0) {
                        setupGroupTab(forceRefresh = true)
                        refreshSelectedGuid()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Subscription update failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    // ---------- Get configs from the public lists ----------

    private fun toggleFetchConfigs() {
        if (_fetchState.value.busy) {
            fetchJob?.cancel()
            fetchJob = null
            restoreGroupAfterFetch = null
            cancelAllPing()
            _fetchState.value = ConfigFetchState()
            return
        }
        fetchConfigs()
    }

    /**
     * Reads every source of [ConfigSources], a few at a time, and adds the new configs one by one (each as its own
     * profile, the list refreshing as they arrive), then tests the group and sorts the best first. A config the app
     * already holds, or one that came twice, is dropped by the app's own duplicate identity.
     */
    private fun fetchConfigs(silent: Boolean = false) {
        val sources = ConfigSources.forIsp(MmkvManager.decodeSettingsString(AppConfig.PREF_ISP) ?: IspChoice.AUTO)
        _fetchState.value = ConfigFetchState(FetchPhase.Downloading, sourcesTotal = sources.size)
        restoreGroupAfterFetch = if (silent) _uiState.value.selectedGroupId else null
        fetchJob = viewModelScope.launch(ioDispatcher) {
            try {
                // Matixray: the fetched configs live in the free group, and only that group is cleared (when the first
                // source answers, so a failed download keeps the old ones); the user's own groups are never touched.
                val freeId = dataSource.ensureFreeGroup(dataSource.getString(R.string.mx_group_free))
                var freeCleared = false
                var seen = HashSet<ProfileItem>()
                fun collectSeen() {
                    seen = HashSet()
                    dataSource.getServerGuidList("").forEach { guid ->
                        val profile = dataSource.decodeServerConfig(guid) ?: return@forEach
                        if (!profile.configType.isComplexType()) seen += profile.duplicateIdentity()
                    }
                }
                collectSeen()
                val importLock = Mutex()
                var added = 0
                var done = 0
                var reachable = 0
                var lastRefreshAt = 0L
                ConfigFetcher.fetchAll(sources, enough = { added >= ENOUGH_CANDIDATES }) { result ->
                    importLock.withLock {
                        done++
                        if (result.links.isNotEmpty()) reachable++
                        if (!freeCleared && result.links.isNotEmpty()) {
                            freeCleared = true
                            dataSource.clearGroup(freeId)
                            collectSeen()
                        }
                        currentCoroutineContext().ensureActive()
                        val take = minOf(MAX_FETCH_PER_SOURCE, MAX_FETCH_TOTAL - added)
                        if (take > 0) added += importFetchedBatch(result.links.take(take), seen, freeId)
                        _fetchState.value = ConfigFetchState(FetchPhase.Downloading, done, sources.size, added)
                    }
                }
                currentCoroutineContext().ensureActive()
                if (added > 0) {
                    // A quick TCP check first, many at a time: servers that do not even open a connection are dropped
                    // before the slower real test, which makes the whole run much shorter.
                    _fetchState.value = ConfigFetchState(FetchPhase.Downloading, done, sources.size, added)
                    added -= dropUnreachable(freeId)
                }
                currentCoroutineContext().ensureActive()
                if (added <= 0) {
                    _fetchState.value = ConfigFetchState()
                    restoreGroupAfterFetch = null
                    if (!silent) toast(dataSource.getString(if (reachable == 0) R.string.mx_fetch_unreachable else R.string.mx_fetch_none))
                    return@launch
                }
                if (!silent) toast(dataSource.getString(R.string.mx_fetch_result, added, reachable, sources.size))
                showFetchedGroup()
                setupGroupTab(forceRefresh = true).join()
                if (currentServers().isEmpty()) {
                    _fetchState.value = ConfigFetchState()
                    return@launch
                }
                // testAllRealPing starts with cancelAllPing, which clears the flag and the testing phase, so both
                // are set after the call.
                testAllRealPing()
                _fetchState.value = ConfigFetchState(FetchPhase.Testing, done, sources.size, added)
                sortAfterTests = true
                purgeDeadAfterTests = _fetchOnlyWorking.value
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Get configs failed", e)
                _fetchState.value = ConfigFetchState()
                toastError(R.string.toast_failure)
            }
        }
    }

    /**
     * Opens a plain TCP connection to every config of the group [groupId], 40 at a time with a short timeout, and removes
     * the ones that do not answer, then keeps only the [REAL_TEST_MAX] that answered fastest for the real test. Configs that run over UDP (hysteria, tuic, wireguard) cannot be checked this way and
     * stay for the real test. Returns how many were removed.
     */
    private suspend fun dropUnreachable(groupId: String): Int = coroutineScope {
        val gate = Semaphore(TCP_PRECHECK_PARALLEL)
        val guids = dataSource.getServerGuidList(groupId)
        // The connect time of each config that answered (a config that cannot be checked this way counts as 500 ms).
        val times = guids.map { guid ->
            async(ioDispatcher) {
                val profile = dataSource.decodeServerConfig(guid) ?: return@async guid to -1L
                val host = profile.server
                val port = profile.serverPort?.toIntOrNull()
                val udp = profile.configType == EConfigType.HYSTERIA2 || profile.configType == EConfigType.WIREGUARD
                if (udp) return@async guid to 500L
                if (host.isNullOrEmpty() || port == null) return@async guid to -1L
                guid to gate.withPermit { SpeedtestManager.socketConnectTime(host, port, TCP_PRECHECK_TIMEOUT_MS) }
            }
        }.awaitAll()
        val keep = times.filter { it.second >= 0L }.sortedBy { it.second }.take(REAL_TEST_MAX).map { it.first }.toHashSet()
        guids.filter { it !in keep }.forEach { dataSource.removeServer(it) }
        guids.size - keep.size
    }

    /**
     * Adds all the share links of one source to the free group [groupId] in one import, then removes the profiles that
     * the app already holds (the same identity as one in [seen]). Returns how many new profiles stayed.
     */
    private suspend fun importFetchedBatch(links: List<String>, seen: MutableSet<ProfileItem>, groupId: String): Int {
        if (links.isEmpty()) return 0
        val before = dataSource.getServerGuidList(groupId).toHashSet()
        try {
            dataSource.importBatchConfig(links.joinToString("\n"), groupId, true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Importing a fetched source failed", e)
        }
        var kept = 0
        dataSource.getServerGuidList(groupId).filter { it !in before }.forEach { guid ->
            val profile = dataSource.decodeServerConfig(guid)
            if (profile == null || (!profile.configType.isComplexType() && !seen.add(profile.duplicateIdentity()))) {
                dataSource.removeServer(guid)
            } else {
                kept++
            }
        }
        return kept
    }

    /**
     * Adds one share link as a profile of the group [groupId] (the free group); false when the app could not read it or already holds
     * the same profile (the copy just made is removed again).
     */
    private suspend fun importFetchedLink(link: String, seen: MutableSet<ProfileItem>, groupId: String): Boolean {
        val before = dataSource.getServerGuidList(groupId).toHashSet()
        val count = try {
            dataSource.importBatchConfig(link, groupId, true).first
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            0
        }
        if (count <= 0) return false
        val created = dataSource.getServerGuidList(groupId).firstOrNull { it !in before } ?: return false
        val profile = dataSource.decodeServerConfig(created)
        if (profile != null && !profile.configType.isComplexType() && !seen.add(profile.duplicateIdentity())) {
            dataSource.removeServer(created)
            return false
        }
        return true
    }

    /** Shows the group the fetched configs went to (the default group, or "All" when the app shows that one). */
    private fun showFetchedGroup() {
        val ids = _uiState.value.groups.map { it.id }.toSet()
        val target = listOf(AppConfig.FREE_SUBSCRIPTION_ID, AppConfig.DEFAULT_SUBSCRIPTION_ID, "").firstOrNull { it in ids } ?: return
        subscriptionIdChanged(target)
    }

    private fun exportAllAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val groupId = uiState.value.selectedGroupId
                    val list = if (groupId.isEmpty() && keywordFilter.isEmpty()) {
                        dataSource.getServerGuidList("")
                    } else {
                        currentServers().map { it.guid }
                    }
                    val ret = dataSource.shareNonCustomConfigsToClipboard(list)
                    if (ret > 0) {
                        toast(dataSource.getString(R.string.title_export_config_count, ret))
                    } else {
                        toastError(R.string.toast_failure)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Export failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeAllServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val count =
                        if (uiState.value.selectedGroupId.isEmpty() && keywordFilter.isEmpty()) {
                            dataSource.removeAllServer()
                        } else {
                            // Matixray: what the storage removed, a profile it refused to remove staying.
                            currentServers().count { dataSource.removeServer(it.guid) }
                        }
                    viewModelScope.launch(ioDispatcher) {
                        cacheMutex.withLock { groupDataCache.clear() }
                    }
                    setupGroupTab(forceRefresh = true)
                    toast(dataSource.getString(R.string.title_del_config_count, count))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete all failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeDuplicateServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val seen = HashSet<ProfileItem>()
                    val duplicates = ArrayList<String>()
                    currentServers().forEach { server ->
                        val profile = server.profile
                        if (!profile.configType.isComplexType()) {
                            val identity = profile.duplicateIdentity()
                            if (!seen.add(identity)) duplicates += server.guid
                        }
                    }
                    // Matixray: what the storage removed, a profile it refused to remove staying.
                    val removed = duplicates.count { dataSource.removeServer(it) }
                    setupGroupTab(forceRefresh = true)
                    toast(dataSource.getString(R.string.title_del_duplicate_config_count, removed))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete duplicate failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeInvalidServerAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    val count = removeInvalidServerInternal()
                    viewModelScope.launch(ioDispatcher) {
                        cacheMutex.withLock { groupDataCache.clear() }
                        setupGroupTab(forceRefresh = true)
                    }
                    toast(dataSource.getString(R.string.title_del_config_count, count))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Delete invalid failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun removeInvalidServerInternal(): Int {
        val visibleServersOnly =
            uiState.value.selectedGroupId.isNotEmpty() || keywordFilter.isNotBlank()
        return if (visibleServersOnly) {
            currentServers().sumOf { server ->
                dataSource.removeInvalidServerByGuid(server.guid)
            }
        } else {
            dataSource.removeInvalidServersInGroup("")
        }
    }

    private fun sortByTestResultsAsync() {
        launchLoading {
            withContext(ioDispatcher) {
                try {
                    sortByTestResultsInternal()
                    cacheMutex.withLock { groupDataCache.clear() }
                    setupGroupTab(forceRefresh = true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Sort by test results failed", e)
                    toastError(R.string.toast_failure)
                }
            }
        }
    }

    private fun sortByTestResultsInternal() {
        val subs = if (uiState.value.selectedGroupId.isEmpty()) {
            dataSource.getSubsList()
        } else {
            listOf(uiState.value.selectedGroupId)
        }
        subs.forEach { dataSource.sortByTestResultsForSub(it) }
    }

    fun subscriptionIdChanged(id: String) {
        if (_uiState.value.groups.none { it.id == id }) return
        mutableServerGroupState(id)
        if (uiState.value.selectedGroupId != id) {
            dataSource.setSelectedSubscriptionId(id)
            _uiState.update { it.copy(selectedGroupId = id) }
        }
        selectedGroupLoadJob?.cancel()
        selectedGroupLoadJob = viewModelScope.launch(ioDispatcher) {
            try {
                updateGroupUi(id, loadGroup(id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load selected group: $id", error)
            }
        }
    }

    fun reloadServerList() {
        val groupId = uiState.value.selectedGroupId
        selectedGroupLoadJob?.cancel()
        selectedGroupLoadJob = viewModelScope.launch(ioDispatcher) {
            updateGroupUi(groupId, loadGroup(groupId, forceRefresh = true))
        }
    }

    fun reloadAllGroups(groupIds: List<String>) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch(preloadDispatcher) {
            val selected = uiState.value.selectedGroupId
            val order = buildList {
                if (selected in groupIds) add(selected)
                addAll(groupIds.filter { it != selected })
            }
            order.forEachIndexed { index, groupId ->
                ensureActive()
                if (index > 0) delay(32)
                updateGroupUi(groupId, loadGroup(groupId, forceRefresh = true))
            }
        }
    }

    fun filterConfig(keyword: String) {
        if (keyword == keywordFilter) return
        keywordFilter = keyword
        filterJob?.cancel()
        filterJob = viewModelScope.launch(defaultDispatcher) {
            delay(300)
            // Matixray: with how many of each group's moves were settled when it was cached, and the number of the reading it
            // is, see [updateGroupUi].
            val snapshot = cacheMutex.withLock {
                groupDataCache.mapValues { (groupId, servers) ->
                    GroupReading(servers, settledMoves.getOrDefault(groupId, 0), readingNumbers.getOrDefault(groupId, 0))
                }
            }
            ensureActive()
            snapshot.forEach { (groupId, reading) ->
                ensureActive()
                updateGroupUi(groupId, reading)
            }
        }
    }

    fun updateSelectedGuid(guid: String) {
        dataSource.setSelectServer(guid)
        _uiState.update { it.copy(selectedGuid = guid) }
    }

    fun refreshSelectedGuid() {
        _uiState.update { it.copy(selectedGuid = dataSource.getSelectServer()) }
    }

    fun removeServerAndRefresh(guid: String) {
        if (guid == uiState.value.selectedGuid) {
            toast(R.string.toast_action_not_allowed)
            return
        }
        viewModelScope.launch(ioDispatcher) {
            // Matixray: a removal the storage refused is told; the list, shown anew, keeps the profile.
            if (!dataSource.removeServer(guid)) toastError(R.string.toast_failure)
            cacheMutex.withLock { groupDataCache.clear() }
            setupGroupTab(forceRefresh = true).join()
        }
    }

    /**
     * Shows the profile [fromGuid] names where the one [toGuid] names stands, and stores it there; the job storing it,
     * null when nothing moved. Matixray: by the two guids, see [storeMove], even when the screen closes right after. A
     * move the storage refused is told, see [MainUiState.moveRefusal], once, when the group's last move asked for is
     * stored, and the groups are shown anew, as stored, so that the reading comes after every move queued behind the
     * refused one. Not in the list of every group, [groupId] empty, which is no stored list.
     */
    fun moveServer(groupId: String, fromGuid: String, toGuid: String): Job? {
        if (groupId.isEmpty()) return null
        synchronized(groupShowLock) {
            val groupState = mutableServerGroupState(groupId).value
            val servers = groupState.servers.toMutableList()
            val fromPosition = servers.indexOfFirst { it.guid == fromGuid }
            val toPosition = servers.indexOfFirst { it.guid == toGuid }
            if (!servers.moveItem(fromPosition, toPosition)) return null
            val rows = groupState.rows.toMutableList()
            rows.moveItem(fromPosition, toPosition)
            mutableServerGroupState(groupId).value = ServerGroupUiState(servers, rows)
            shownMoves.merge(groupId, 1, Int::plus)
            unstoredMoves.merge(groupId, 1, Int::plus)
        }
        // A drag emits several moves; each is stored once the one before it is, in the order they were made.
        val previousPersistenceJob = serverOrderPersistenceJobs[groupId]
        // Matixray: begun at once, on the main thread the move comes on, and the store kept from cancellation, so that a
        // screen closing right after neither stops it nor keeps it from starting.
        return viewModelScope.launch {
            val moved = withContext(NonCancellable + ioDispatcher) {
                previousPersistenceJob?.join()
                storeMove(groupId, fromGuid, toGuid)
            }
            if (!moved) refusedMoveGroups += groupId
            // Matixray: once the group's last move asked for is settled: a refusal among them told, and the groups read anew;
            // or else a reading held back for them taken anew, see [updateGroupUi].
            val (refused, readEarly) = synchronized(groupShowLock) {
                if (unstoredMoves.merge(groupId, -1, Int::plus) != 0) {
                    false to false
                } else {
                    refusedMoveGroups.remove(groupId) to groupsReadEarly.remove(groupId)
                }
            }
            if (refused) {
                val refusal = moveRefusals.incrementAndGet()
                _uiState.update { it.copy(moveRefusal = refusal) }
                setupGroupTab(forceRefresh = true)
            } else if (readEarly) {
                showGroupAnew(groupId)
            }
        }.also { serverOrderPersistenceJobs[groupId] = it }
    }

    /**
     * Matixray: stores the move of the profile [fromGuid] names to where the one [toGuid] names stands, in the list of
     * [groupId] as stored then, see [MainDataSource.moveServer], so that a profile an update stored, or a removal took
     * away, meanwhile is not undone, and makes it in the group's cached list too, under the group's load lock, see
     * [loadGroup]: a reading of the group comes before the move is stored or after it is cached, so the cached list stays
     * the stored one; and counts it settled, see [settledMoves]. Whether the storage took it.
     */
    private suspend fun storeMove(groupId: String, fromGuid: String, toGuid: String): Boolean =
        groupLoadMutexes.computeIfAbsent(groupId) { Mutex() }.withLock {
            val moved = dataSource.moveServer(groupId, fromGuid, toGuid)
            cacheMutex.withLock {
                if (moved) {
                    groupDataCache[groupId]?.toMutableList()?.let { cached ->
                        if (cached.moveItem(cached.indexOfFirst { it.guid == fromGuid }, cached.indexOfFirst { it.guid == toGuid })) {
                            groupDataCache[groupId] = cached
                        }
                    }
                }
                // Matixray: settled, stored or refused: a reading from now on has it as stored, see [updateGroupUi].
                settledMoves.merge(groupId, 1, Int::plus)
            }
            moved
        }

    // ---------- Testing ----------
    fun cancelAllPing() {
        autoConnectPending = false
        sortAfterTests = false
        purgeDeadAfterTests = false
        if (_fetchState.value.phase == FetchPhase.Testing) _fetchState.value = ConfigFetchState()
        bulkTestJob?.cancel()
        bulkTestJob = null
        testRequests.cancelBulk()
        testRequests.invalidateCurrent()
        cancelPendingTestResults()
        resetTestStatus()
        dataSource.cancelAllPing()
    }

    private fun resetTestStatus() {
        _uiState.update {
            it.copy(
                isTesting = testRequests.isTesting,
                status = if (testRequests.isTesting) MainStatus.Testing
                else if (it.isRunning) MainStatus.Connected else MainStatus.Disconnected
            )
        }
    }

    fun testAllRealPing(onlyTcp: Boolean = false) {
        cancelAllPing()
        val groupId = uiState.value.selectedGroupId
        val servers = currentServers()
        if (servers.isEmpty()) {
            return
        }
        val serverGuids = servers.map { it.guid }
        mutableServerGroupState(groupId).update { current ->
            current.copy(
                servers = current.servers.map { server ->
                    if (server.testDelayMillis == 0L) server
                    else server.copy(testDelayMillis = 0L)
                },
                rows = current.rows.map { row ->
                    if (row.testDelayMillis == 0L) row
                    else row.copy(testDelayMillis = 0L)
                }
            )
        }
        val request = testRequests.beginBulk(groupId)
        val message = TestServiceMessage(
            key = AppConfig.MSG_MEASURE_CONFIG_START,
            subscriptionId = groupId,
            serverGuids = if (keywordFilter.isNotEmpty()) serverGuids else emptyList(),
            onlyTcp = onlyTcp
        )
        _uiState.update {
            it.copy(
                isTesting = true,
                status = MainStatus.Testing
            )
        }
        bulkTestJob = viewModelScope.launch {
            withContext(ioDispatcher) {
                dataSource.clearAllTestDelayResults(serverGuids)
                val resetGuids = serverGuids.toHashSet()
                cacheMutex.withLock {
                    groupDataCache[groupId]?.let { cached ->
                        groupDataCache[groupId] = cached.map { server ->
                            if (server.guid !in resetGuids || server.testDelayMillis == 0L) server
                            else server.copy(testDelayMillis = 0L)
                        }
                    }
                }
            }
            dataSource.sendMsg2TestService(message, request.id)
        }
    }

    private fun cancelPendingTestResults() {
        testResultFlushJob?.cancel()
        testResultFlushJob = null
        pendingTestResults.clear()
    }

    fun testCurrentServerRealPing(silent: Boolean = false) {
        if (!uiState.value.isRunning) return
        val requestId = testRequests.beginCurrent()
        if (silent) silentProbeIds.add(requestId)
        else _uiState.update { it.copy(isTesting = true, status = MainStatus.Testing) }
        dataSource.testCurrentServerRealPing(requestId)
    }

    private fun onTestsFinished(requestId: String) {
        if (testRequests.completeBulk(requestId) == null) return
        resetTestStatus()
        finishAutoConnect()
        if (sortAfterTests) {
            // The "get configs" run: the fresh configs are tested; the ones that did not answer are removed (when the
            // setting asks for it) before they ever show in the list, then the best go first.
            sortAfterTests = false
            val purge = purgeDeadAfterTests
            purgeDeadAfterTests = false
            viewModelScope.launch(ioDispatcher) {
                try {
                    if (purge) {
                        val freeId = AppConfig.FREE_SUBSCRIPTION_ID
                        val before = dataSource.getServerGuidList(freeId)
                        val working = before
                            .map { it to (dataSource.decodeAffiliationInfo(it)?.testDelayMillis ?: 0L) }
                            .filter { it.second > 0L }
                            .sortedBy { it.second }
                            .take(FREE_KEEP_BEST)
                            .map { it.first }
                            .toHashSet()
                        before.filter { it !in working }.forEach { dataSource.removeServer(it) }
                        val kept = working.size
                        toast(dataSource.getString(R.string.mx_fetch_kept, kept, before.size - kept))
                    }
                    MmkvManager.encodeSettings(AppConfig.PREF_FREE_LAST_FETCH, System.currentTimeMillis().toString())
                    cacheMutex.withLock { groupDataCache.clear() }
                    setupGroupTab(forceRefresh = true).join()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Removing the dead fetched configs failed", e)
                } finally {
                    // The list shows the free group again only now.
                    _fetchState.value = ConfigFetchState()
                    withContext(Dispatchers.Main) { restoreGroupIfNeeded() }
                }
                sortByTestResultsAsync()
            }
        }
        viewModelScope.launch(ioDispatcher) {
            cacheMutex.withLock { groupDataCache.clear() }
            reloadAllGroups(_uiState.value.groups.map { it.id })
        }
    }

    fun triggerLocateSelectedServer() {
        val selected = dataSource.getSelectServer() ?: return
        val profile = dataSource.decodeServerConfig(selected) ?: return
        val groupId = profile.subscriptionId
        if (_uiState.value.groups.none { it.id == groupId }) return
        viewModelScope.launch(ioDispatcher) {
            updateGroupUi(groupId, loadGroup(groupId))
            if (_uiState.value.selectedGroupId != groupId) {
                dataSource.setSelectedSubscriptionId(groupId)
            }
            val target = LocateTarget(groupId, selected)
            _uiState.update {
                it.copy(selectedGroupId = groupId, locateTarget = target)
            }
        }
    }

    private fun consumeLocateTarget() {
        _uiState.update { it.copy(locateTarget = null) }
    }

    // ---------- Running state ----------
    private fun updateRunningState(running: Boolean, clearTestingText: Boolean = true) {
        if (!running || clearTestingText) testRequests.invalidateCurrent()
        if (running) {
            if (pingJob?.isActive != true) startPingLoop()
        } else {
            pingJob?.cancel()
            pingJob = null
            silentProbeIds.clear()
        }
        _uiState.update { state ->
            state.copy(
                isRunning = running,
                isTesting = testRequests.isTesting,
                status = runningStatus(state.status, state.isRunning, running, clearTestingText),
                pingMillis = if (running) state.pingMillis else null
            )
        }
    }

    override fun onCleared() {
        setupGroupJob?.cancel()
        preloadJob?.cancel()
        selectedGroupLoadJob?.cancel()
        reloadJob?.cancel()
        filterJob?.cancel()
        cancelAllPing()
        dataSource.close()
        super.onCleared()
    }

    /** Matixray: what becomes of a reading of a group, see [readingFate]. */
    internal enum class ReadingFate { SHOW, OVERTAKEN, HOLD_UNTIL_SETTLED, READ_AGAIN }

    companion object {
        /** Matixray: limits of one "get configs" run, so the list and the tests that follow stay small. */
        private const val MAX_FETCH_TOTAL = 400
        private const val MAX_FETCH_PER_SOURCE = 25
        private const val TCP_PRECHECK_PARALLEL = 60
        /** How many configs go on to the real test after the quick check: the ones that answered fastest. */
        private const val REAL_TEST_MAX = 60
        private const val TCP_PRECHECK_TIMEOUT_MS = 1000
        /** The download stops waiting for slow sources once this many configs are in. */
        private const val ENOUGH_CANDIDATES = 140
        private const val AUTO_FETCH_DELAY_MS = 6_000L
        private const val AUTO_FETCH_MAX_AGE_MS = 3 * 60 * 60 * 1000L
        /** How many tested configs the free group keeps: few, but the fastest ones that work. */
        private const val FREE_KEEP_BEST = 20
        private const val FETCH_REFRESH_MS = 700L

        /** Matixray: the update check on start runs once per process, not once per screen. */
        @Volatile
        private var updateCheckedThisProcess = false

        private const val TEST_RESULT_FLUSH_INTERVAL_MS = 500L

        /**
         * Matixray: what becomes of a reading of a group numbered [number] when the one shown last is [lastShown], taken
         * when [settledWhenRead] of the group's moves were settled, now that [shownMoves] were shown and [unsettledMoves]
         * of them are not settled yet, and filtered as the search stands now or not, [filteredAsSearched]: one older than
         * the one shown is not shown over it; one that a move shown since it was taken overtook waits until the group's
         * moves are settled, or, when they are by now, is taken anew, as one filtered for a search changed since is;
         * else it is shown.
         */
        internal fun readingFate(
            number: Int,
            lastShown: Int,
            settledWhenRead: Int,
            shownMoves: Int,
            unsettledMoves: Int,
            filteredAsSearched: Boolean,
        ): ReadingFate =
            when {
                number < lastShown -> ReadingFate.OVERTAKEN
                settledWhenRead < shownMoves && unsettledMoves > 0 -> ReadingFate.HOLD_UNTIL_SETTLED
                settledWhenRead < shownMoves || !filteredAsSearched -> ReadingFate.READ_AGAIN
                else -> ReadingFate.SHOW
            }

        /**
         * The status after a running or stopped signal. A test text survives a repeated signal
         * that changes nothing, but a connecting status does not: the daemon repeats the
         * connecting signal right after a running one whenever the tunnel is still on its way.
         */
        internal fun runningStatus(
            current: MainStatus,
            wasRunning: Boolean,
            running: Boolean,
            clearTestingText: Boolean,
        ): MainStatus =
            if (!clearTestingText && wasRunning == running && current !is MainStatus.Connecting) current
            else if (running) MainStatus.Connected else MainStatus.Disconnected
    }

    // ---------- Factory ----------
    class Factory(private val application: Application, private val dataSource: MainDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(application, dataSource) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
