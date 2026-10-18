package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.v2ray.ang.ui.compose.MatixButtonBrush
import com.v2ray.ang.ui.compose.MatixButtonShape
import com.v2ray.ang.ui.compose.MatixCutShape
import com.v2ray.ang.ui.compose.XfBlue
import com.v2ray.ang.ui.compose.XfConnected
import com.v2ray.ang.ui.compose.XfConnectedLight
import com.v2ray.ang.ui.compose.onMatixButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import com.v2ray.ang.ui.compose.SlidingPillButton
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.v2ray.ang.ui.compose.XfMintDark
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import com.v2ray.ang.ui.compose.XfPurpleDark
import com.v2ray.ang.ui.compose.pressScale
import com.v2ray.ang.ui.compose.XfMint

/** The three tabs of the bottom bar. */
object MainTab {
    const val Home = 0
    const val Configs = 1
    const val Settings = 2
}

/**
 * Top of every page, as in the design: an optional row of buttons (the first at the left, the rest at the right),
 * and under it the big title, the name "Matixray" at the left (whatever the language) with the version, and the
 * name of the page in small gray under it. With [compact] the buttons and the title share one short row, so a long
 * list below gets the room (the configs page).
 */
@Composable
fun MainPageHeader(
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (compact) {
            Row(
                modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke(this)
                Column(modifier = Modifier.padding(start = if (leading != null) 6.dp else 12.dp)) {
                    BrandTitle(size = 24.sp, small = true)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                actions?.invoke(this)
            }
        } else {
            Column(modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)) {
                if (leading != null || actions != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        leading?.invoke(this)
                        Spacer(Modifier.weight(1f))
                        actions?.invoke(this)
                    }
                }
                Column(modifier = Modifier.padding(start = 8.dp, top = if (leading != null || actions != null) 0.dp else 10.dp)) {
                    BrandTitle(size = 34.sp, small = false)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "Matixray" with the second half in the accent, and the version in a small frame beside it. */
@Composable
private fun BrandTitle(size: TextUnit, small: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = scheme.onBackground)) { append("Matix") }
                withStyle(SpanStyle(color = scheme.onSurfaceVariant)) { append("ray") }
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size,
            maxLines = 1,
        )
        Text(
            text = "v" + BuildConfig.VERSION_NAME.trimStart('v', 'V'),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = if (small) 8.dp else 10.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** Bottom bar: Home, Configs and Settings, the open one on a soft gray pill. */
@Composable
fun MainBottomBar(selected: Int, onSelect: (Int) -> Unit, tour: TourState) {
    val scheme = MaterialTheme.colorScheme
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = XfBlue,
        selectedTextColor = XfBlue,
        indicatorColor = XfBlue.copy(alpha = 0.12f),
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant,
    )
    NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
        BottomItem(MainTab.Home, selected, onSelect, R.drawable.ic_home_24dp, R.string.mx_nav_home, colors,
            Modifier.tourTarget(tour, TourTarget.TabHome))
        BottomItem(MainTab.Configs, selected, onSelect, R.drawable.ic_list_24dp, R.string.mx_nav_configs, colors,
            Modifier.tourTarget(tour, TourTarget.TabConfigs))
        BottomItem(MainTab.Settings, selected, onSelect, R.drawable.ic_settings_24dp, R.string.mx_nav_settings, colors,
            Modifier.tourTarget(tour, TourTarget.TabSettings))
    }
}

@Composable
private fun RowScope.BottomItem(
    tab: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    @DrawableRes icon: Int,
    label: Int,
    colors: androidx.compose.material3.NavigationBarItemColors,
    modifier: Modifier,
) {
    NavigationBarItem(
        selected = selected == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(painterResource(icon), contentDescription = null) },
        label = { Text(stringResource(label), maxLines = 1) },
        colors = colors,
        modifier = modifier,
    )
}

/**
 * The bar above the bottom tabs: what the connection is doing in words, and the button that connects or disconnects.
 * A tap on the words tests the connection and the ping shows right there, colored by how good it is (green, yellow,
 * red); the button is graphite while off, a green bar runs around it while it connects, solid green once on.
 */
@Composable
fun MainConnectionBar(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
    /** The last ping of the running connection, in milliseconds; null while there is none. */
    pingMillis: Long? = null,
    /** Changes when the bar starts to speak for another page (main / Psiphon), so the button does not animate then. */
    pageKey: Any = Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // A tap starts a test: "testing" shows for a moment, then the number the test left in [pingMillis].
    var testing by remember(pageKey) { mutableStateOf(false) }
    var taps by remember(pageKey) { mutableStateOf(0) }
    LaunchedEffect(taps) {
        if (taps > 0) {
            delay(2500)
            testing = false
        }
    }
    LaunchedEffect(isRunning) { if (!isRunning) testing = false }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        shadowElevation = 6.dp,
    ) {
      // Matixray: the round button is always at the right, a little in from the edge, in every language.
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 26.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isRunning, role = Role.Button) {
                        testing = true
                        taps += 1
                        onTest()
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    !isRunning -> Text(
                        text = stringResource(R.string.mx_bar_disconnected),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 2,
                    )
                    testing -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.mx_bar_testing),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    pingMillis != null -> {
                        val color = pingColor(pingMillis).let { if (it == Color.Unspecified) scheme.onSurface else it }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.mx_stat_ping),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.mx_ping_value, pingMillis),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = stringResource(R.string.mx_bar_ping_again),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    else -> Text(
                        text = stringResource(R.string.mx_bar_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 2,
                    )
                }
            }
            ConnectPillButton(isRunning = isRunning, pageKey = pageKey, onToggle = onToggle)
        }
      }
    }
}


/**
 * Matixray: the four quick buttons above the connect bar: get configs (the main button, metal fill, with a spinner and
 * the count while it runs; a second tap stops it), test all (a tap while testing stops the tests), sort and remove
 * the dead ones. Every tile is one focusable button that dips a little under the finger.
 */
@Composable
fun MainQuickActions(
    fetch: ConfigFetchState,
    testing: Boolean,
    onFetch: () -> Unit,
    onTestAll: () -> Unit,
    onSort: () -> Unit,
    onRemoveDead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fetchCaption = when (fetch.phase) {
        FetchPhase.Downloading -> stringResource(R.string.mx_fetch_progress, fetch.sourcesDone, fetch.sourcesTotal, fetch.added)
        FetchPhase.Testing -> stringResource(R.string.mx_fetch_testing)
        FetchPhase.Idle -> null
    }
    Surface(modifier = modifier.fillMaxWidth(), color = scheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionTile(
                label = stringResource(if (fetch.busy) R.string.mx_qa_stop else R.string.mx_qa_fetch),
                caption = fetchCaption,
                iconRes = R.drawable.ic_cloud_download_24dp,
                busy = fetch.busy,
                filled = true,
                tint = Color.Unspecified,
                onClick = onFetch,
                modifier = Modifier.weight(1.25f),
            )
            QuickActionTile(
                label = stringResource(if (testing) R.string.mx_qa_stop else R.string.mx_qa_test_all),
                caption = null,
                iconRes = R.drawable.ic_speed_24dp,
                busy = testing,
                filled = false,
                tint = XfConnected,
                onClick = onTestAll,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = stringResource(R.string.mx_qa_sort),
                caption = null,
                iconRes = R.drawable.ic_sort_24dp,
                busy = false,
                filled = false,
                tint = XfBlue,
                onClick = onSort,
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                label = stringResource(R.string.mx_qa_remove_dead),
                caption = null,
                iconRes = R.drawable.ic_delete_24dp,
                busy = false,
                filled = false,
                tint = scheme.error,
                onClick = onRemoveDead,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    caption: String?,
    @DrawableRes iconRes: Int,
    busy: Boolean,
    filled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val source = remember { MutableInteractionSource() }
    val iconColor = if (filled) onMatixButton else tint
    val textColor = if (filled) onMatixButton else scheme.onSurface
    val surface = if (filled) {
        Modifier
            .shadow(4.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(MatixButtonBrush)
    } else {
        Modifier
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .border(1.dp, scheme.outlineVariant, shape)
    }
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .pressScale(source)
            .then(surface)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = iconColor,
            )
        } else {
            Icon(painterResource(iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


private enum class ConnectPhase { Off, Connecting, On }

private const val MIN_RING_MS = 900L
private const val CONNECT_TIMEOUT_MS = 15_000L

/**
 * The connect button: graphite metal while off (silver on the dark theme). A tap makes a green bar run around its edge
 * while the connection comes up, and once the connection is on the button fills with solid green. When the connection comes up without a tap (the auto
 * connect, a restored session) the bar still runs for a moment first. A tap while the bar runs cancels.
 */
@Composable
private fun ConnectPillButton(isRunning: Boolean, pageKey: Any, onToggle: () -> Unit) {
    var phase by remember(pageKey) { mutableStateOf(if (isRunning) ConnectPhase.On else ConnectPhase.Off) }
    var startedAt by remember(pageKey) { mutableLongStateOf(0L) }
    val running by rememberUpdatedState(isRunning)

    LaunchedEffect(pageKey, isRunning) {
        if (!isRunning) {
            phase = ConnectPhase.Off
        } else if (phase != ConnectPhase.On) {
            if (phase == ConnectPhase.Off) {
                phase = ConnectPhase.Connecting
                startedAt = SystemClock.elapsedRealtime()
            }
            delay((MIN_RING_MS - (SystemClock.elapsedRealtime() - startedAt)).coerceAtLeast(0L))
            phase = ConnectPhase.On
        }
    }
    // A connection that never comes up (permission refused, no network) must not leave the bar running for ever.
    LaunchedEffect(phase) {
        if (phase == ConnectPhase.Connecting) {
            delay(CONNECT_TIMEOUT_MS)
            if (!running) phase = ConnectPhase.Off
        }
    }

    val on = phase == ConnectPhase.On
    val shape = CircleShape
    val greenAlpha by animateFloatAsState(if (on) 1f else 0f, tween(350), label = "connectFill")
    val edge by animateColorAsState(
        if (on) XfConnectedLight.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f),
        tween(350),
        label = "connectEdge",
    )
    val glow = if (on) XfConnected else Color.Black
    val description = stringResource(
        when (phase) {
            ConnectPhase.On -> R.string.mx_bar_btn_connected
            ConnectPhase.Connecting -> R.string.mx_bar_btn_connecting
            ConnectPhase.Off -> R.string.mx_bar_btn_connect
        }
    )

    // The round button sits in a larger box, so the rings that move while it is on have room around it.
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (on) ConnectOnRings(Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(if (on) 8.dp else 4.dp, shape, ambientColor = glow, spotColor = glow)
                .clip(shape)
                .background(MatixButtonBrush)
                .background(XfConnected.copy(alpha = greenAlpha))
                .border(1.dp, edge, shape)
                .semantics { contentDescription = description }
                .clickable(role = Role.Button) {
                    if (phase == ConnectPhase.Off && !isRunning) {
                        phase = ConnectPhase.Connecting
                        startedAt = SystemClock.elapsedRealtime()
                    } else if (phase == ConnectPhase.Connecting) {
                        phase = ConnectPhase.Off
                    }
                    onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_power_24dp),
                contentDescription = null,
                tint = if (on) Color.White else onMatixButton,
                modifier = Modifier.size(30.dp),
            )
            if (phase == ConnectPhase.Connecting) ConnectRing(Modifier.matchParentSize())
        }
    }
}

/** While connected: a ring that spreads out from the button and fades, again and again, and a bright arc that turns around it. */
@Composable
private fun ConnectOnRings(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "connectOn")
    val spread by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)),
        label = "connectOnSpread",
    )
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "connectOnTurn",
    )
    Spacer(
        modifier.drawBehind {
            val stroke = 3.dp.toPx()
            val inner = 62.dp.toPx() / 2f + 4.dp.toPx()
            val outer = size.minDimension / 2f - stroke
            drawCircle(
                color = XfConnected.copy(alpha = (1f - spread) * 0.55f),
                radius = inner + (outer - inner) * spread,
                style = Stroke(stroke),
            )
            drawCircle(color = XfConnected.copy(alpha = 0.22f), radius = inner, style = Stroke(stroke))
            drawArc(
                color = XfConnectedLight,
                startAngle = turn,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - inner, center.y - inner),
                size = androidx.compose.ui.geometry.Size(inner * 2f, inner * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    )
}

/** A green bar that runs around the edge of the button, over a faint track; it reads the turn only while drawing. */
@Composable
private fun ConnectRing(modifier: Modifier = Modifier) {
    val turn by rememberInfiniteTransition(label = "connectRing").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "connectRingTurn",
    )
    Spacer(
        modifier.drawBehind {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            val d = size.minDimension - inset * 2f
            if (d <= 0f) return@drawBehind
            val topLeft = androidx.compose.ui.geometry.Offset(center.x - d / 2f, center.y - d / 2f)
            val box = androidx.compose.ui.geometry.Size(d, d)
            drawArc(XfConnected.copy(alpha = 0.25f), 0f, 360f, false, topLeft, box, style = Stroke(stroke))
            drawArc(XfConnectedLight, turn * 360f, 100f, false, topLeft, box, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    )
}
