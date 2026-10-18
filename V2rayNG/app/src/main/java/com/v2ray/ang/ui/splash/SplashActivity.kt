package com.v2ray.ang.ui.splash

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.compose.BouncingBallsLoader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.v2ray.ang.ui.compose.LocalDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.GlassCardShape
import com.v2ray.ang.ui.compose.XfGreen
import com.v2ray.ang.ui.compose.liquidGlass
import com.v2ray.ang.ui.main.MainActivity
import java.util.Locale

/**
 * Launcher entry: the bouncing balls, with the opening words (always in Persian) above them the first time only,
 * then it hands over to [MainActivity]. The first time stays long enough for the balls to be seen bouncing.
 */
class SplashActivity : BaseComponentActivity() {

    @Composable
    override fun ScreenContent() {
        // Nothing to go back to: the user is either waiting or leaving the app.
        BackHandler { finish() }
        SplashScreen(onFinished = {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        })
    }
}

/** A new key, so the opening words ("In the name of God") are shown once to everyone after this update too. */
private const val SPLASH_SEEN_V2 = "pref_splash_seen_v2"
private const val FADE_IN_MS = 800
private const val HOLD_MS = 520L
private const val FIRST_RUN_HOLD_MS = 2800L
private val splashStatusRes = intArrayOf(
    R.string.xf_splash_status_0,
    R.string.xf_splash_status_1,
    R.string.xf_splash_status_2,
    R.string.xf_splash_status_3,
    R.string.xf_splash_status_4,
    R.string.xf_splash_status_5,
)

/** Status line for a percentage: five stages while loading, the last one only at 100 %. */
internal fun splashStatusIndex(percent: Int): Int =
    if (percent >= 100) splashStatusRes.lastIndex else (percent.coerceAtLeast(0) * 5 / 100)

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    // The opening words are Persian from the very first frame, whatever the language of the phone is: the title is read
    // from a Persian copy of the resources, so it does not wait for (or depend on) the app language being applied.
    val base = LocalContext.current
    val persianContext = remember(base) {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag("fa"))
        base.createConfigurationContext(config)
    }
    val title = remember(persianContext) { persianContext.getString(R.string.xf_splash_title) }
    // The opening words are for the first launch only (an app that was already used before this version has had them).
    val firstRun = remember {
        !MmkvManager.decodeSettingsBool(SPLASH_SEEN_V2, false)
    }
    // The words fade in from the first frame, the infinity loop runs under them, and the app opens after about
    // 0.9 s: one fade, a short hold, nothing else to wait for.
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        // The loading screen is for the very first launch only; every later start goes straight to the app.
        if (!firstRun) {
            onFinished()
            return@LaunchedEffect
        }
        MmkvManager.encodeSettings(SPLASH_SEEN_V2, true)
        fade.animateTo(1f, tween(durationMillis = FADE_IN_MS, easing = FastOutSlowInEasing))
        kotlinx.coroutines.delay(FIRST_RUN_HOLD_MS)
        onFinished()
    }

    if (!firstRun) return
    // Matixray: a halo that breathes and two rings that spread out behind the mark, and the words that rise into place.
    val transition = rememberInfiniteTransition(label = "splash")
    val breathe by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "ring",
    )
    val glowColor = scheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .graphicsLayer { this.alpha = fade.value },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center.copy(y = size.height * 0.40f)
            val radius = size.minDimension * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.28f), Color.Transparent),
                    center = c,
                    radius = radius * 1.5f * breathe,
                ),
                radius = radius * 1.5f * breathe,
                center = c,
            )
            for (i in 0..1) {
                val t = (ring + i * 0.5f) % 1f
                drawCircle(
                    color = glowColor.copy(alpha = (1f - t) * 0.35f),
                    radius = radius * (0.6f + 1.1f * t),
                    center = c,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The mark of the app. It is black with a soft edge, so the dark theme draws it in white.
            Image(
                painter = painterResource(R.drawable.xf_logo),
                contentDescription = null,
                colorFilter = if (LocalDarkTheme.current) ColorFilter.tint(Color.White) else null,
                modifier = Modifier.size(168.dp),
            )
            Spacer(Modifier.height(if (firstRun) 20.dp else 28.dp))
            if (firstRun) {
                Text(
                    text = title,
                    color = scheme.onBackground,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 46.sp, fontWeight = FontWeight.ExtraBold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        val scale = 0.8f + 0.2f * fade.value
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - fade.value) * 28.dp.toPx()
                    },
                )
                Spacer(Modifier.height(32.dp))
            }
            BouncingBallsLoader()
        }
        Text(
            text = stringResource(R.string.xf_brand),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 0.2.em,
        )
    }
}
