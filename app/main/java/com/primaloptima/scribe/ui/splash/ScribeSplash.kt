package com.primaloptima.scribe.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.primaloptima.scribe.R
import com.primaloptima.scribe.util.ThemeManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Animated Scribe launch screen.
 *
 * The supplied transparent mark is tinted at runtime instead of baking a
 * light/dark copy into the APK. The shadow is intentionally made from layered
 * strokes and ovals: the mark has a dense network of lines and nodes, so the
 * shadow becomes denser as the mark breathes.
 */
@Composable
fun ScribeSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val theme = remember { ThemeManager(context).activeTheme() }
    val background = remember(theme) {
        Color(ThemeManager.parseColor(theme.colors.background))
    }
    val configuredAccent = remember(theme) {
        Color(ThemeManager.parseColor(theme.colors.accent))
    }
    val markColor = remember(background, configuredAccent) {
        val contrast = kotlin.math.abs(
            background.luminance() - configuredAccent.luminance()
        )
        if (contrast >= 0.22f) {
            configuredAccent
        } else if (background.luminance() > 0.5f) {
            Color(0xFF111111)
        } else {
            Color.White
        }
    }
    val mark = painterResource(R.drawable.scribe_splash_mark)
    val fade = remember { Animatable(0f) }
    val breathing by rememberInfiniteTransition(label = "scribe-breathing")
        .animateFloat(
            initialValue = 0.965f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "mark-scale"
        )

    LaunchedEffect(Unit) {
        fade.animateTo(1f, animationSpec = tween(durationMillis = 650))
        delay(1250)
        fade.animateTo(0f, animationSpec = tween(durationMillis = 520))
        delay(90)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .graphicsLayer { alpha = fade.value },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = mark,
                contentDescription = null,
                colorFilter = ColorFilter.tint(markColor, BlendMode.SrcIn),
                modifier = Modifier
                    .size(270.dp)
                    .graphicsLayer {
                        scaleX = breathing
                        scaleY = breathing
                    }
            )

            DensityShadow(
                color = markColor,
                scale = breathing,
                modifier = Modifier.size(width = 205.dp, height = 58.dp)
            )
        }
    }

}

@Composable
private fun DensityShadow(
    color: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    // Approximate visual density of the supplied network mark. Keeping this
    // explicit makes the shadow easy to tune if the artwork is replaced.
    val lineDensity = 0.82f
    val strokeCount = (4 + lineDensity * 8).roundToInt()

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = 1f / scale
        }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.36f
        val maxWidth = size.width * 0.78f

        drawOval(
            color = color.copy(alpha = 0.16f),
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - maxWidth * 0.5f,
                centerY - 4f
            ),
            size = androidx.compose.ui.geometry.Size(maxWidth, 18f)
        )
        drawOval(
            color = color.copy(alpha = 0.12f),
            topLeft = androidx.compose.ui.geometry.Offset(
                centerX - maxWidth * 0.38f,
                centerY + 5f
            ),
            size = androidx.compose.ui.geometry.Size(maxWidth * 0.76f, 12f)
        )

        repeat(strokeCount) { index ->
            val fraction = if (strokeCount == 1) 0.5f else index / (strokeCount - 1).toFloat()
            val y = size.height * (0.18f + fraction * 0.42f)
            val width = maxWidth * (0.9f - fraction * 0.34f)
            drawLine(
                color = color.copy(alpha = 0.08f * (1f - fraction * 0.55f)),
                start = androidx.compose.ui.geometry.Offset(centerX - width / 2f, y),
                end = androidx.compose.ui.geometry.Offset(centerX + width / 2f, y),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }
    }
}