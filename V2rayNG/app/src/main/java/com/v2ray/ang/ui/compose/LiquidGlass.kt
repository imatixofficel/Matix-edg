package com.v2ray.ang.ui.compose

import android.graphics.BlurMaskFilter
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colors of the "liquid glass" surfaces: a translucent fill, a specular edge and a soft highlight. */
@Immutable
class GlassStyle(
    val fillTop: Color,
    val fillBottom: Color,
    val edgeLight: Color,
    val edgeDim: Color,
    val highlight: Color,
)

// Matixray: the surfaces are flat now, as in the Shadow VPN kit: a solid card color and a one-pixel border.
// The old glass fields stay so every caller keeps compiling; only fillTop (the card) and edgeDim (the border) are used.
val DarkGlassStyle = GlassStyle(
    fillTop = Color(0xFF161617),
    fillBottom = Color(0xFF161617),
    edgeLight = Color(0xFF2E2E31),
    edgeDim = Color(0xFF2E2E31),
    highlight = Color.Transparent,
)

val LightGlassStyle = GlassStyle(
    fillTop = Color(0xFFFFFFFF),
    fillBottom = Color(0xFFFFFFFF),
    edgeLight = Color(0xFFE0E0E5),
    edgeDim = Color(0xFFE0E0E5),
    highlight = Color.Transparent,
)

val LocalGlassStyle = staticCompositionLocalOf { DarkGlassStyle }

/**
 * Whether the cards use the liquid-glass look. The choice is stored by the main ViewModel ([PREF_GLASS_CARDS]); this
 * holder only keeps the current value as Compose state so every card redraws when the setting changes.
 */
object GlassPrefs {
    var enabled by mutableStateOf(MmkvManager.decodeSettingsBool(AppConfig.PREF_GLASS_CARDS, false))
}

val GlassCardShape = RoundedCornerShape(14.dp)
val GlassPillShape = RoundedCornerShape(50)

/**
 * The shape of every button: fully rounded, a capsule. (The names stay from the first cut-corner design.)
 */
val MatixCutShape = RoundedCornerShape(50)
val MatixSmallCutShape = RoundedCornerShape(50)
/** Shape of the quiet buttons and chips. */
val MatixButtonShape = RoundedCornerShape(50)

/** Fill of buttons and selected tabs: the solid accent color. */
val PrimaryGradient: Brush
    @Composable get() = Brush.linearGradient(
        // Flat, like the kit's buttons: a solid accent (the two stops are the same color on purpose)
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
    )

/**
 * Card surface of the app: a solid fill and a thin border, flat like the Shadow VPN kit. It keeps the name of the old
 * glass modifier so every screen picks the new look up at once. [tint] lays a color over the card (a selected item).
 * It draws no gradient and no highlight, so a long list of cards stays cheap to scroll.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = GlassCardShape,
    tint: Color = Color.Unspecified,
): Modifier {
    val glass = LocalGlassStyle.current
    // Day: a soft gray shadow under the card, as the cards of the design float a little over the page.
    val dark = LocalDarkTheme.current
    if (GlassPrefs.enabled) {
        // Liquid glass (the user can turn it off in the settings): a see-through fill that is brighter on top, and an
        // edge that catches light on its upper-left and fades toward the lower-right.
        // iPhone-style glass: very see-through, so the colors of the page behind show through; a bright rim on the upper-left
        // and the lower-right edges (where light would catch the glass) and a soft wash of light from the top-left corner.
        val fill = if (dark) listOf(Color(0x30FFFFFF), Color(0x0FFFFFFF)) else listOf(Color(0x99FFFFFF), Color(0x5CFFFFFF))
        val rim = if (dark) Color(0xCCFFFFFF) else Color(0xFFFFFFFF)
        val rimFaint = if (dark) Color(0x1FFFFFFF) else Color(0x40FFFFFF)
        val edge = listOf(rim, rimFaint, rimFaint, rim.copy(alpha = rim.alpha * 0.7f))
        val sheen = if (dark) Color(0x26FFFFFF) else Color(0x80FFFFFF)
        var glassResult = (if (dark) this else this.shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0x2E3B4A7A),
            spotColor = Color(0x2E3B4A7A),
        )).clip(shape).background(Brush.verticalGradient(fill)).drawBehind {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(sheen, Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.7f, size.height * 1.2f),
                ),
            )
        }
        if (tint != Color.Unspecified) glassResult = glassResult.background(tint)
        return glassResult.border(1.2.dp, Brush.linearGradient(edge, start = Offset.Zero, end = Offset.Infinite), shape)
    }
    var result = (if (dark) this else this.shadow(
        elevation = 3.dp,
        shape = shape,
        clip = false,
        ambientColor = Color(0x1F000000),
        spotColor = Color(0x1F000000),
    )).clip(shape).background(glass.fillTop)
    if (tint != Color.Unspecified) result = result.background(tint)
    return result.border(1.dp, glass.edgeDim, shape)
}

/** Soft colored glow behind a shape (the violet halo of the main connect button). */
fun Modifier.softGlow(glowColor: Color, blur: Dp = 18.dp, corner: Dp = 20.dp): Modifier = drawWithCache {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = glowColor.copy(alpha = 0.55f).toArgb()
        maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val radius = corner.toPx()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
        }
    }
}

/**
 * Soft press feedback for buttons: the control shrinks a little while it is held and springs back, so taps feel
 * alive without a ripple. Pass the same [interactionSource] to the `clickable` of the control.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** A glass card with arbitrary content. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    tint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.liquidGlass(shape, tint), content = content)
}

/**
 * Full-window backdrop: the flat canvas color with one soft violet glow at the top, like the lit-from-above look of the
 * Shadow VPN kit. The brush is built once per size, so a frame costs two fills.
 */
@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    val glow = if (dark) Color(0x14FFFFFF) else Color.Transparent
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .drawWithCache {
                // Night: a violet glow from the top. Day: white that fades into a soft violet haze at the top.
                val brush = if (dark) {
                    Brush.radialGradient(
                        listOf(glow, Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 1.05f,
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(glow, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.42f,
                    )
                }
                val glassOn = GlassPrefs.enabled
                onDrawBehind {
                    drawRect(brush)
                    if (glassOn) {
                        // Soft colored light behind the page, so the glass cards have something to refract.
                        val blobs = if (dark) {
                            listOf(Color(0x663B6BFF), Color(0x5A8A4DFF), Color(0x4D19C6B8))
                        } else {
                            listOf(Color(0x553B82F6), Color(0x4DA78BFA), Color(0x4D2DD4BF))
                        }
                        val spots = listOf(
                            Offset(size.width * 0.10f, size.height * 0.12f),
                            Offset(size.width * 0.95f, size.height * 0.45f),
                            Offset(size.width * 0.25f, size.height * 0.92f),
                        )
                        blobs.forEachIndexed { i, color ->
                            drawCircle(
                                brush = Brush.radialGradient(listOf(color, Color.Transparent), center = spots[i], radius = size.width * 0.8f),
                                radius = size.width * 0.8f,
                                center = spots[i],
                            )
                        }
                    }
                }
            }
    )
}

/** Rounded-corner convenience for gradient fills inside glass (selected tab, connect button). */
fun Modifier.gradientFill(brush: Brush, shape: Shape): Modifier = this.clip(shape).background(brush)
