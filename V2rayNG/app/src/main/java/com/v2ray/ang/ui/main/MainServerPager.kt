package com.v2ray.ang.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import com.v2ray.ang.ui.compose.XfMint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.ReorderableGridItem
import com.v2ray.ang.ui.compose.ReorderableListItem
import com.v2ray.ang.ui.compose.colorConfigType
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import com.v2ray.ang.ui.compose.liquidGlass
import com.v2ray.ang.ui.compose.verticalScrollbar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs

@Composable
fun GroupPagerPage(
    groupId: String,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    doubleColumnDisplay: Boolean,
    searchQuery: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String, String) -> Unit,
    contentPadding: PaddingValues
) {
    val groupStateFlow = remember(groupId) {
        mainViewModel.serverGroupState(groupId)
    }
    val groupState by groupStateFlow.collectAsStateWithLifecycle()
    val canReorder = groupId.isNotEmpty() && searchQuery.isEmpty()
    val actions = remember(
        onSelectServer,
        onEditServer,
        onShareServer,
        onMoreServer,
        onRemoveServer,
    ) {
        ServerRowActions(
            select = onSelectServer,
            edit = onEditServer,
            share = onShareServer,
            more = onMoreServer,
            remove = onRemoveServer,
        )
    }
    ServerListPage(
        rows = groupState.rows,
        selectedGuid = selectedGuid,
        locateTarget = locateTarget?.takeIf { it.groupId == groupId },
        canReorder = canReorder,
        doubleColumnDisplay = doubleColumnDisplay,
        groupId = groupId,
        lazyListStates = lazyListStates,
        lazyGridStates = lazyGridStates,
        actions = actions,
        onLocateHandled = { mainViewModel.onAction(MainAction.LocateHandled) },
        onMoveServer = { fromGuid, toGuid ->
            mainViewModel.onAction(MainAction.MoveServer(groupId, fromGuid, toGuid))
        },
        contentPadding = contentPadding
    )
}

private class ServerRowActions(
    val select: (String) -> Unit,
    val edit: (String, ProfileItem) -> Unit,
    val share: (String, ProfileItem) -> Unit,
    val more: (String, ProfileItem) -> Unit,
    val remove: (String, String) -> Unit,
)

@Composable
private fun ServerListPage(
    rows: List<ServerRowUiModel>,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    canReorder: Boolean,
    doubleColumnDisplay: Boolean,
    groupId: String,
    lazyListStates: MutableMap<String, LazyListState>,
    lazyGridStates: MutableMap<String, LazyGridState>,
    actions: ServerRowActions,
    onLocateHandled: () -> Unit,
    onMoveServer: (fromGuid: String, toGuid: String) -> Unit,
    contentPadding: PaddingValues
) {
    if (doubleColumnDisplay) {
        val gridState = remember(groupId) {
            lazyGridStates.getOrPut(groupId) { LazyGridState() }
        }
        val reorderableGridState = if (canReorder) {
            rememberReorderableLazyGridState(gridState) { from, to ->
                // Matixray: by the keys of the rows, their guids, not by their positions.
                onMoveServer(from.key as? String ?: return@rememberReorderableLazyGridState, to.key as? String ?: return@rememberReorderableLazyGridState)
            }
        } else null

        LocateTargetEffect(locateTarget, rows, gridState, onLocateHandled)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(gridState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = rows, key = { _, item -> item.guid }) { _, row ->
                val content: @Composable () -> Unit = {
                    ServerItemColumn(
                        row = row,
                        isSelected = row.guid == selectedGuid,
                        doubleColumnDisplay = true,
                        actions = actions
                    )
                }
                if (canReorder && reorderableGridState != null) {
                    ReorderableItem(
                        reorderableGridState,
                        key = row.guid
                    ) { isDragging ->
                        ReorderableGridItem(
                            scope = this,
                            isDragging = isDragging
                        ) { content() }
                    }
                } else {
                    content()
                }
            }
        }
    } else {
        val listState = remember(groupId) {
            lazyListStates.getOrPut(groupId) { LazyListState() }
        }
        val reorderableState = if (canReorder) {
            rememberReorderableLazyListState(listState) { from, to ->
                // Matixray: by the keys of the rows, their guids, not by their positions.
                onMoveServer(from.key as? String ?: return@rememberReorderableLazyListState, to.key as? String ?: return@rememberReorderableLazyListState)
            }
        } else null

        LocateTargetEffect(locateTarget, rows, listState, onLocateHandled)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(listState),
            contentPadding = contentPadding
        ) {
            itemsIndexed(items = rows, key = { _, item -> item.guid }) { _, row ->
                if (canReorder && reorderableState != null) {
                    ReorderableItem(
                        reorderableState,
                        key = row.guid
                    ) { isDragging ->
                        ReorderableListItem(
                            scope = this,
                            isDragging = isDragging
                        ) {
                            ServerItemRow(
                                row = row,
                                isSelected = row.guid == selectedGuid,
                                actions = actions
                            )
                        }
                    }
                } else {
                    ServerItemRow(
                        row = row,
                        isSelected = row.guid == selectedGuid,
                        actions = actions
                    )
                }
            }
        }
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyListState,
    onHandled: () -> Unit,
) {
    if (target == null) return
    LaunchedEffect(target, rows) {
        val index = rows.indexOfFirst { it.guid == target.serverGuid }
        if (index < 0) return@LaunchedEffect
        state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
        onHandled()
    }
}

@Composable
private fun LocateTargetEffect(
    target: LocateTarget?,
    rows: List<ServerRowUiModel>,
    state: LazyGridState,
    onHandled: () -> Unit,
) {
    if (target == null) return
    LaunchedEffect(target, rows) {
        val index = rows.indexOfFirst { it.guid == target.serverGuid }
        if (index < 0) return@LaunchedEffect
        state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
        onHandled()
    }
}

@Composable
private fun ServerItemRow(
    row: ServerRowUiModel,
    isSelected: Boolean,
    actions: ServerRowActions
) {
    ServerListItem(
        row = row,
        isSelected = isSelected,
        doubleColumnDisplay = false,
        actions = actions
    )
}

@Composable
private fun ServerItemColumn(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions
) {
    Column {
        ServerListItem(
            row = row,
            isSelected = isSelected,
            doubleColumnDisplay = doubleColumnDisplay,
            actions = actions
        )
    }
}

/** Text turned a quarter circle up, for the protocol strip at the edge of a config card; it takes the room of the turned text. */
private fun Modifier.verticalText(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = Constraints.Infinity, maxHeight = Constraints.Infinity))
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width / 2 - placeable.height / 2),
                y = -(placeable.height / 2 - placeable.width / 2),
            )
        }
    }
    .rotate(-90f)

private val pingGoodText = Color(0xFF0E9F6E)
private val pingMidText = Color(0xFFB7791F)

/** The round-cornered ping mark of a config card: green with the delay, red "Timeout" when the test failed. */
@Composable
private fun PingPill(delayMillis: Long, onBlue: Boolean = false) {
    if (delayMillis == 0L) return
    val failed = delayMillis < 0L
    val color = when {
        onBlue -> Color.White
        failed -> colorPingRed
        delayMillis > 800L -> pingMidText
        else -> pingGoodText
    }
    Text(
        text = if (failed) stringResource(R.string.mx_timeout) else stringResource(R.string.server_test_delay_value, delayMillis),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (onBlue) 0.22f else 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * One config of the list, small and flat like the design: a strip with the protocol written upward on the edge, the
 * name, the address with the ping mark, and a rail with the edit and share buttons. The selected config gets a violet
 * frame and a mint strip. The card keeps the left-to-right order of the design in every language.
 */
@Composable
private fun ServerListItem(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    actions: ServerRowActions
) {
    val scheme = MaterialTheme.colorScheme
    val selectedStateDescription = if (isSelected) {
        stringResource(R.string.acc_selected_server)
    } else {
        null
    }
    val shape = RoundedCornerShape(16.dp)
    val protocol = row.profile.configType.name
    // Matixray: the selected config is blue behind, white on top, as in V2Box.
    val mainColor = if (isSelected) Color.White else scheme.onSurface
    val subColor = if (isSelected) Color.White.copy(alpha = 0.82f) else scheme.onSurfaceVariant
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(72.dp)
                .then(
                    if (isSelected) Modifier.clip(shape).background(Brush.horizontalGradient(listOf(ConfigBlue, ConfigBlueDark)))
                    else Modifier.liquidGlass(shape)
                )
                .semantics {
                    if (selectedStateDescription != null) {
                        stateDescription = selectedStateDescription
                    }
                }
                .clickable { actions.select(row.guid) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Protocol strip
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else scheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = protocol,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (isSelected) Color.White else scheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.verticalText(),
                )
            }
            // Name, address and ping
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = row.remarks,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = mainColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.profile.subscriptionId == AppConfig.FREE_SUBSCRIPTION_ID) FreeBadge(onBlue = isSelected)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = row.statistics,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    PingPill(row.testDelayMillis, onBlue = isSelected)
                }
            }
            // Rail with the buttons
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(if (isSelected) Color.Transparent else scheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!doubleColumnDisplay) {
                    RailButton(R.drawable.ic_edit_24dp, stringResource(R.string.acc_edit), onBlue = isSelected) {
                        actions.edit(row.guid, row.profile)
                    }
                }
                // Share, with the rest of the server actions (delete) in the same list.
                RailButton(
                    if (doubleColumnDisplay) R.drawable.ic_more_vert_24dp else R.drawable.ic_share_24dp,
                    stringResource(if (doubleColumnDisplay) R.string.acc_more else R.string.title_configuration_share),
                    onBlue = isSelected,
                ) { actions.more(row.guid, row.profile) }
            }
        }
    }
}

@Composable
private fun RailButton(
    @androidx.annotation.DrawableRes iconRes: Int,
    description: String,
    onBlue: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (onBlue) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (onBlue) Color.White else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

private val ConfigBlue = Color(0xFF2F7BF6)
private val ConfigBlueDark = Color(0xFF1D5FD8)

/** One line of the grouped list: the card of a group, or one config under an open card. */
private sealed interface ConfigListEntry {
    val key: String

    class Header(val group: GroupMapItem, val count: Int, val open: Boolean) : ConfigListEntry {
        override val key: String get() = "group:" + group.id
    }

    class Config(val groupId: String, val row: ServerRowUiModel) : ConfigListEntry {
        override val key: String get() = row.guid
    }
}

/**
 * Matixray: the configs page as V2Box shows it: one card per group (Default, the free group, the groups the user
 * named, every subscription), each with a small triangle that opens or closes it and a refresh button, and the configs
 * of the open cards under it. The selected config is blue. Which cards are open is saved across rotation (by group id).
 */
@Composable
internal fun GroupedConfigsList(
    groups: List<GroupMapItem>,
    mainViewModel: MainViewModel,
    selectedGuid: String?,
    selectedGroupId: String,
    locateTarget: LocateTarget?,
    searchQuery: String,
    fetchBusy: Boolean,
    /** While "get configs" tests its configs, the free group shows none: only the working ones are shown, after the tests. */
    hideFreeRows: Boolean,
    onDeleteGroup: (GroupMapItem) -> Unit,
    onSelectGroup: (String) -> Unit,
    onRefreshGroup: (String) -> Unit,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onLocateHandled: () -> Unit,
    contentPadding: PaddingValues,
) {
    val cards = remember(groups) { groups.filter { it.id.isNotEmpty() } }
    val rowsByGroup = HashMap<String, List<ServerRowUiModel>>()
    cards.forEach { group ->
        key(group.id) {
            val flow = remember(group.id, mainViewModel) { mainViewModel.serverGroupState(group.id) }
            val state by flow.collectAsStateWithLifecycle()
            rowsByGroup[group.id] = state.rows
        }
    }

    // The ids of the open cards, joined by commas (ids never hold one); the selected card is open at first.
    var openIds by rememberSaveable { mutableStateOf("") }
    var openInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(cards) {
        if (!openInitialized && cards.isNotEmpty()) {
            openInitialized = true
            openIds = cards.firstOrNull { it.id == selectedGroupId }?.id ?: cards.first().id
        }
    }
    val open = openIds.split(',').filter { it.isNotEmpty() }.toSet()
    val searching = searchQuery.isNotEmpty()

    val entries = ArrayList<ConfigListEntry>()
    cards.forEach { group ->
        val hidden = hideFreeRows && fetchBusy && group.id == AppConfig.FREE_SUBSCRIPTION_ID
        val rows = if (hidden) emptyList() else rowsByGroup[group.id].orEmpty()
        if (searching && rows.isEmpty()) return@forEach
        val isOpen = searching || group.id in open
        entries += ConfigListEntry.Header(group, rows.size, isOpen)
        if (isOpen) rows.forEach { entries += ConfigListEntry.Config(group.id, it) }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(locateTarget, open, entries.size) {
        val target = locateTarget ?: return@LaunchedEffect
        if (target.groupId.isNotEmpty() && target.groupId !in open) {
            openIds = (open + target.groupId).joinToString(",")
            return@LaunchedEffect
        }
        val index = entries.indexOfFirst { it is ConfigListEntry.Config && it.row.guid == target.serverGuid }
        if (index < 0) return@LaunchedEffect
        listState.scrollToItem(index, -listState.layoutInfo.viewportSize.height / 3)
        onLocateHandled()
    }

    // A tap on a config also makes its group the selected one (the tests and the sort work on the selected group).
    val groupOfGuid = HashMap<String, String>()
    entries.forEach { if (it is ConfigListEntry.Config) groupOfGuid[it.row.guid] = it.groupId }
    val actions = ServerRowActions(
        select = { guid ->
            groupOfGuid[guid]?.let { if (it != selectedGroupId) onSelectGroup(it) }
            onSelectServer(guid)
        },
        edit = onEditServer,
        share = onShareServer,
        more = onMoreServer,
        remove = { _, _ -> },
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(listState),
        contentPadding = contentPadding,
    ) {
        items(items = entries, key = { it.key }) { entry ->
            when (entry) {
                is ConfigListEntry.Header -> GroupHeaderCard(
                    group = entry.group,
                    count = entry.count,
                    open = entry.open,
                    selected = entry.group.id == selectedGroupId,
                    refreshing = fetchBusy && entry.group.id == AppConfig.FREE_SUBSCRIPTION_ID,
                    onDelete = if (entry.group.id == AppConfig.DEFAULT_SUBSCRIPTION_ID) null else ({ onDeleteGroup(entry.group) }),
                    onToggle = {
                        val id = entry.group.id
                        openIds = if (id in open) (open - id).joinToString(",") else (open + id).joinToString(",")
                        onSelectGroup(id)
                    },
                    onRefresh = { onRefreshGroup(entry.group.id) },
                )

                is ConfigListEntry.Config -> Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                    ServerListItem(
                        row = entry.row,
                        isSelected = entry.row.guid == selectedGuid,
                        doubleColumnDisplay = false,
                        actions = actions,
                    )
                }
            }
        }
    }
}

/** The card of one group: a small triangle that opens or closes it, the name with the number of configs, and a refresh button. */
@Composable
private fun GroupHeaderCard(
    group: GroupMapItem,
    count: Int,
    open: Boolean,
    selected: Boolean,
    refreshing: Boolean,
    onDelete: (() -> Unit)?,
    onToggle: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val angle by animateFloatAsState(if (open) 90f else 0f, label = "groupTriangle")
    val name = if (group.id == AppConfig.FREE_SUBSCRIPTION_ID) stringResource(R.string.mx_group_free) else group.remarks
    val stateText = stringResource(if (open) R.string.mx_group_expanded else R.string.mx_group_collapsed)
    val refreshLabel = stringResource(R.string.mx_group_refresh, name)
    val deleteLabel = stringResource(R.string.mx_group_delete, name)
    val triangleColor = scheme.primary
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .liquidGlass(shape, tint = if (selected) scheme.primary.copy(alpha = 0.07f) else Color.Unspecified)
                .then(if (selected) Modifier.border(1.dp, scheme.primary.copy(alpha = 0.6f), shape) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = stateText }
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = angle }) {
                    val triangle = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(triangle, triangleColor)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$name ($count)",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (group.id == AppConfig.FREE_SUBSCRIPTION_ID) {
                    Spacer(Modifier.width(8.dp))
                    FreeBadge(onBlue = false)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = deleteLabel,
                        tint = scheme.error,
                    )
                }
            }
            IconButton(onClick = onRefresh, modifier = Modifier.padding(end = 4.dp)) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = scheme.primary)
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh_24dp),
                        contentDescription = refreshLabel,
                        tint = scheme.primary,
                    )
                }
            }
        }
    }
}

internal suspend fun PagerState.navigateToPageOptimized(
    targetPage: Int,
    animateAdjacentPage: Boolean = true
) {
    if (pageCount <= 0) return
    val target = targetPage.coerceIn(0, pageCount - 1)
    val current = settledPage.coerceIn(0, pageCount - 1)
    if (target == current) return

    if (abs(target - current) == 1 && animateAdjacentPage) {
        animateScrollToPage(target)
    } else {
        scrollToPage(target)
    }
}


/** Matixray: the small green "free" tag shown next to the free group and the configs inside it. */
@Composable
private fun FreeBadge(onBlue: Boolean) {
    val green = Color(0xFF2EBD6B)
    val shape = RoundedCornerShape(50)
    Text(
        text = stringResource(R.string.mx_free_badge),
        modifier = Modifier
            .clip(shape)
            .background(if (onBlue) Color.White.copy(alpha = 0.9f) else green.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (onBlue) Color(0xFF1B7F47) else green,
        maxLines = 1,
        softWrap = false,
    )
}
