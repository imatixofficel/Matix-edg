package com.v2ray.ang.ui.xfinder

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.XfPurple
import com.v2ray.ang.ui.compose.XfPurpleLight
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScannerScreen(onBack: () -> Unit, viewModel: ScannerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = state.data
    var showMix by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (showMix) {
        MixDialog(
            maxCount = data?.ips?.size ?: 0,
            ownConfigs = state.ownConfigs,
            busy = state.mixBusy,
            onLoad = viewModel::loadOwnConfigs,
            onDismiss = { showMix = false },
            onMix = { count, guid, pasted -> viewModel.mix(count, guid, pasted) { added -> if (added > 0) showMix = false } },
        )
    }

    XfinderPage(
        title = stringResource(R.string.xf_nav_scanner),
        onBack = onBack,
        isLoading = state.isLoading,
        actions = {
            val refreshLabel = stringResource(R.string.xf_refresh)
            IconButton(
                onClick = viewModel::refresh,
                modifier = Modifier.semantics { contentDescription = refreshLabel },
            ) { RefreshIcon() }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.xf_scanner_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.xf_scanner_source),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item(key = "mix") {
                androidx.compose.material3.Button(
                    onClick = { showMix = true },
                    enabled = data != null && data.ips.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.xf_mix_button)) }
            }
            item(key = "stats") {
                val stats = data?.stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatBox(stringResource(R.string.xf_stat_total), stats?.total, Modifier.weight(1f))
                    StatBox(stringResource(R.string.xf_stat_online), stats?.online, Modifier.weight(1f))
                    StatBox(stringResource(R.string.xf_stat_pinned), stats?.pinned, Modifier.weight(1f))
                }
            }
            item(key = "radar") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RadarView(ips = data?.ips.orEmpty().take(RADAR_BLIPS).map { it.ip })
                }
            }
            if (data != null) {
                itemsIndexed(data.ips, key = { _, ip -> ip.ip }) { index, ip ->
                    IpRow(rank = index + 1, ip = ip, onClick = { viewModel.copyIp(ip.ip) })
                }
            } else if (state.hasError) {
                item(key = "error") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.xf_error_load),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.xf_retry)) }
                    }
                }
            }
        }
    }
}

private const val RADAR_BLIPS = 12

@Composable
private fun StatBox(label: String, value: Int?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = XfinderCardBorder,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value?.toString() ?: "—",
                color = XfPurpleLight,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Decorative radar: concentric rings, a rotating sweep and a few blinking blips placed from the IPs. */
@Composable
private fun RadarView(ips: List<String>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3500, easing = LinearEasing)),
        label = "sweep",
    )
    val blink by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "blink",
    )
    val ringColor = XfPurple.copy(alpha = 0.35f)

    Canvas(modifier = modifier.size(200.dp).clearAndSetSemantics { }) {
        val radius = size.minDimension / 2f
        val c = center
        for (i in 1..4) {
            drawCircle(color = ringColor, radius = radius * i / 4f, center = c, style = Stroke(1.dp.toPx()))
        }
        drawLine(ringColor, Offset(c.x - radius, c.y), Offset(c.x + radius, c.y), 1.dp.toPx())
        drawLine(ringColor, Offset(c.x, c.y - radius), Offset(c.x, c.y + radius), 1.dp.toPx())

        rotate(degrees = sweepAngle, pivot = c) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, XfPurple.copy(alpha = 0.55f)),
                    center = c,
                ),
                radius = radius,
                center = c,
            )
            drawLine(XfPurpleLight, c, Offset(c.x + radius, c.y), 2.dp.toPx())
        }

        drawCircle(XfPurple.copy(alpha = 0.25f + 0.2f * blink), radius = 10.dp.toPx(), center = c)
        drawCircle(XfPurpleLight, radius = 3.dp.toPx(), center = c)

        ips.forEachIndexed { index, ip ->
            val h = abs(ip.hashCode())
            val angle = Math.toRadians((h % 360).toDouble())
            val frac = 0.25f + ((h / 360) % 70) / 100f
            val p = Offset(c.x + (cos(angle) * radius * frac).toFloat(), c.y + (sin(angle) * radius * frac).toFloat())
            val alpha = (if (index % 2 == 0) blink else 1.15f - blink).coerceIn(0.15f, 1f)
            drawCircle(XfPurpleLight.copy(alpha = alpha), radius = 4.dp.toPx(), center = p)
        }
    }
}

@Composable
private fun IpRow(rank: Int, ip: ScannedIp, onClick: () -> Unit) {
    val level = pingLevelOf(ip.ms)
    val pingTint = pingColor(level, MaterialTheme.colorScheme.onSurfaceVariant)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (ip.pinned) BorderStroke(1.5.dp, XfPurple) else XfinderCardBorder,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = rank.toString(),
                color = XfPurpleLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(XfPurple.copy(alpha = 0.18f))
                    .wrapContentSize(Alignment.Center),
            )
            SourceBadge(ip.source)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MonoLtrText(
                    text = ip.ip,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14,
                )
                if (ip.location.isNotEmpty()) {
                    Text(
                        text = ip.location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (ip.ms != Int.MAX_VALUE) {
                Text(
                    text = stringResource(R.string.xf_ping_ms, ip.ms),
                    color = pingTint,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pingTint.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}


/** Matixray: asks how many IPs to mix and which config to mix them into (one of the user's own, or one pasted). */
@Composable
private fun MixDialog(
    maxCount: Int,
    ownConfigs: List<OwnConfig>,
    busy: Boolean,
    onLoad: () -> Unit,
    onDismiss: () -> Unit,
    onMix: (count: Int, ownGuid: String?, pasted: String) -> Unit,
) {
    var countText by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("10") }
    var pasted by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var pickedGuid by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoad() }
    val count = countText.toIntOrNull()?.coerceIn(1, maxOf(maxCount, 1)) ?: 0
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.xf_mix_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.xf_mix_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = countText,
                    onValueChange = { v -> countText = v.filter { it.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.xf_mix_count, maxCount)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.xf_mix_pick_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = pickedGuid == null,
                        onClick = { pickedGuid = null },
                        label = { Text(stringResource(R.string.xf_mix_paste)) },
                    )
                    ownConfigs.forEach { own ->
                        androidx.compose.material3.FilterChip(
                            selected = pickedGuid == own.guid,
                            onClick = { pickedGuid = own.guid },
                            label = { Text(own.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        )
                    }
                }
                if (pickedGuid == null) {
                    androidx.compose.material3.OutlinedTextField(
                        value = pasted,
                        onValueChange = { pasted = it },
                        label = { Text(stringResource(R.string.xf_mix_paste_hint)) },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = !busy && count > 0 && (pickedGuid != null || pasted.isNotBlank()),
                onClick = { onMix(count, pickedGuid, pasted) },
            ) { Text(stringResource(R.string.xf_mix_go)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
