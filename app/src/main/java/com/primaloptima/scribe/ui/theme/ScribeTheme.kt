package com.primaloptima.scribe.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import coil3.BitmapImage

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.ColumnScope

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.primaloptima.scribe.ui.components.InWindowMenuHostState
import com.primaloptima.scribe.ui.components.InWindowMenuHost
import com.primaloptima.scribe.ui.components.LocalInWindowMenuHost
import com.primaloptima.scribe.ui.components.FrostedInWindowDropdownMenu
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.primaloptima.scribe.util.DefaultThemes
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.ScribeDataStore
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.ProvideTextStyle
import androidx.activity.compose.BackHandler
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.material3.LocalContentColor

val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalAppTheme = compositionLocalOf<AppTheme?> { null }
val LocalBgAnalysisBitmap = compositionLocalOf<Bitmap?> { null }
val LocalBgZonalColors = compositionLocalOf<List<Color>> { emptyList() }
val LocalBgImageDominantColor = compositionLocalOf<Color?> { null }
val LocalScreenSize = compositionLocalOf { Pair(1080f, 1920f) }
val LocalRootGeometry = compositionLocalOf { Pair(0f, 0f) }
/**
 * True when the user has enabled the frosted glass effect for this theme.
 * Controls whether bars, panels, cards, and FABs use hazeEffect / one-shot blur.
 * When false all frosted modifiers fall back to a solid surface background.
 */
val LocalFrostedGlass = compositionLocalOf { true }

/** Whether the frosted glass tint overlay is enabled. false = pure blur, no colour wash. */
val LocalFrostedTint = compositionLocalOf { true }

/** Blur radius (dp) for Haze on API 31+. Pre-API-31: applied at bitmap-load time. */
val LocalFrostedBlurRadius = compositionLocalOf { 15f }

/**
 * Holds the one-shot blurred screenshot bitmap captured just before a panel/dialog
 * opens on pre-API-31 devices. Set by the screen that owns the drawer/dialog trigger,
 * consumed by [frostedPanel], [frostedCard], [frostedFab], [FrostedDialog].
 * Null when no capture has been taken or when running on API 31+.
 */
val LocalOneShotBitmap = compositionLocalOf<Bitmap?> { null }

/**
 * Pre-blurred version of the background image, derived from the already-loaded
 * Coil bitmap inside ScribeComposeTheme. Used by bars and FABs on API < 31 so
 * they get a frosted look without needing a live screen capture.
 *
 * Null on API 31+ (Haze handles blurring natively) and when no background image
 * is active. Never used by dialogs or drawers — those use [LocalOneShotBitmap].
 */
val LocalBarBlurBitmap = compositionLocalOf<Bitmap?> { null }

/**
 * Always holds the fully-opaque theme surface color, even when a background image
 * is active and the color scheme's surface is set to alpha=0 for glass effects.
 * Use this for Dropdowns, Dialogs, and any popup that must never be see-through.
 */
val LocalSolidSurface = compositionLocalOf { Color.White }

fun autoTextColor(bg: Color): Color {
    return ContrastResolver.autoTextColor(bg)
}

/**
 * Returns an accent colour that is always visually prominent against the actual
 * background image.
 *
 * When no background image is active the accent is returned unchanged.
 *
 * When a background image IS active:
 *  - If [savedBgLuminance] ≥ 0 (computed at crop-confirm time), contrast is checked
 *    against the *real* background luminance — not the theme surface colour. This fixes
 *    the core bug where baby-blue on a white background was invisible.
 *  - If [savedBgLuminance] == -1f (old theme), falls back to checking against
 *    [solidSurface] as before.
 *
 * Contrast target is 3.0:1 (WCAG minimum for UI components / large text). If the
 * accent already meets this threshold it is returned unchanged, preserving the user's
 * chosen colour exactly.
 *
 * When contrast is insufficient the accent is adjusted by shifting its HSL Lightness
 * using AndroidX [ColorUtils] — no custom math helpers needed. The hue and saturation
 * are preserved so the colour is always recognisably the same accent.  Only Lightness
 * moves: darker for a light background, lighter for a dark background.  This avoids
 * the jarring hue-rotation fallback that the previous approach used.
 */
fun adaptiveAccentColor(
    accent: Color,
    solidSurface: Color,
    hasBgImage: Boolean,
    savedBgLuminance: Float = -1f
): Color {
    if (!hasBgImage) return accent

    val bgRef = if (savedBgLuminance >= 0f) {
        val sRgbVal = ContrastResolver.linearToSRgb(savedBgLuminance.toDouble()).toFloat().coerceIn(0f, 1f)
        Color(sRgbVal, sRgbVal, sRgbVal)
    } else {
        solidSurface
    }

    return ContrastResolver.resolveContrast(
        background = bgRef,
        preferredForeground = accent,
        minRatio = 3.0,
        role = ContrastResolver.ContrastRole.UI_CONTROL
    ).color
}

@Composable
fun localHasBgImage(): Boolean {
    val theme = LocalAppTheme.current
    val frostedGlass = LocalFrostedGlass.current
    return theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred") &&
            frostedGlass
}

private data class FourCorners(val tl: Float, val tr: Float, val br: Float, val bl: Float)

/**
 * Draws an environmental specular glass border around the shape using 4 straight-edge linear
 * gradients and 4 solid corner arcs.
 * Implements a 3-tier concentric optical edge spread with shared centerline:
 * - Tier 2 (Distal Halo): 2.20dp soft outer dispersion (alpha <= 0.04)
 * - Tier 1 (Proximal Skirt): 1.50dp intermediate caustic spread (alpha <= 0.14)
 * - Tier 0 (Core Specular): 1.00dp crisp primary glass boundary (rendered on top)
 *
 * Straight edge spans:
 * - Top: (R_tl, 0) -> (W - R_tr, 0) with stops TL -> TC -> TR
 * - Right: (W, R_tr) -> (W, H - R_br) with stops TR -> MR -> BR
 * - Bottom: (W - R_br, H) -> (R_bl, H) with stops BR -> BC -> BL
 * - Left: (0, H - R_bl) -> (0, R_tl) with stops BL -> ML -> TL
 *
 * Corner arcs:
 * - Top-Right arc (270° -> 360°): solid color TR
 * - Bottom-Right arc (0° -> 90°): solid color BR
 * - Bottom-Left arc (90° -> 180°): solid color BL
 * - Top-Left arc (180° -> 270°): solid color TL
 *
 * Rendered in order: Halo -> Skirt -> Core with Butt caps and inset centerlines for seamless tangent continuity.
 */
fun Modifier.drawEnvironmentalSpecularBorder(
    shape: Shape,
    edges: EnvironmentalSpecularEdges,
    strokeWidth: Dp = 1.dp
): Modifier = this.drawWithCache {
    val strokeWidthPx = strokeWidth.toPx()
    val halfStroke = strokeWidthPx / 2f
    val w = size.width
    val h = size.height

    if (w <= 0f || h <= 0f) {
        return@drawWithCache onDrawWithContent {
            drawContent()
        }
    }

    val left = halfStroke
    val top = halfStroke
    val right = w - halfStroke
    val bottom = h - halfStroke

    val (rTL_outer, rTR_outer, rBR_outer, rBL_outer) = when (shape) {
        RectangleShape -> FourCorners(0f, 0f, 0f, 0f)
        is CornerBasedShape -> {
            val maxR = minOf(w, h) / 2f
            val tl = shape.topStart.toPx(size, this).coerceIn(0f, maxR)
            val tr = shape.topEnd.toPx(size, this).coerceIn(0f, maxR)
            val br = shape.bottomEnd.toPx(size, this).coerceIn(0f, maxR)
            val bl = shape.bottomStart.toPx(size, this).coerceIn(0f, maxR)
            FourCorners(tl, tr, br, bl)
        }
        else -> {
            val outline = shape.createOutline(size, layoutDirection, this)
            when (outline) {
                is Outline.Rounded -> {
                    val maxR = minOf(w, h) / 2f
                    val rr = outline.roundRect
                    FourCorners(
                        rr.topLeftCornerRadius.x.coerceIn(0f, maxR),
                        rr.topRightCornerRadius.x.coerceIn(0f, maxR),
                        rr.bottomRightCornerRadius.x.coerceIn(0f, maxR),
                        rr.bottomLeftCornerRadius.x.coerceIn(0f, maxR)
                    )
                }
                is Outline.Rectangle -> FourCorners(0f, 0f, 0f, 0f)
                is Outline.Generic -> {
                    val maxR = minOf(w, h) / 2f
                    FourCorners(maxR, maxR, maxR, maxR)
                }
            }
        }
    }

    val rTL = if (rTL_outer > halfStroke) rTL_outer - halfStroke else 0f
    val rTR = if (rTR_outer > halfStroke) rTR_outer - halfStroke else 0f
    val rBR = if (rBR_outer > halfStroke) rBR_outer - halfStroke else 0f
    val rBL = if (rBL_outer > halfStroke) rBL_outer - halfStroke else 0f

    val topXStart = left + rTL
    val topXEnd = right - rTR
    val rightYStart = top + rTR
    val rightYEnd = bottom - rBR
    val bottomXStart = right - rBR
    val bottomXEnd = left + rBL
    val leftYStart = bottom - rBL
    val leftYEnd = top + rTL

    // Tier 0 - Core Specular Brushes (1.00dp)
    val topBrush = if (topXEnd > topXStart) {
        val xCenter = w / 2f
        val tCenter = ((xCenter - topXStart) / (topXEnd - topXStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.topLeft,
                tCenter to edges.topCenter,
                1.0f to edges.topRight
            ),
            start = Offset(topXStart, top),
            end = Offset(topXEnd, top)
        )
    } else null

    val rightBrush = if (rightYEnd > rightYStart) {
        val yCenter = h / 2f
        val tCenter = ((yCenter - rightYStart) / (rightYEnd - rightYStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.topRight,
                tCenter to edges.midRight,
                1.0f to edges.bottomRight
            ),
            start = Offset(right, rightYStart),
            end = Offset(right, rightYEnd)
        )
    } else null

    val bottomBrush = if (bottomXStart > bottomXEnd) {
        val xCenter = w / 2f
        val tCenter = ((bottomXStart - xCenter) / (bottomXStart - bottomXEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.bottomRight,
                tCenter to edges.bottomCenter,
                1.0f to edges.bottomLeft
            ),
            start = Offset(bottomXStart, bottom),
            end = Offset(bottomXEnd, bottom)
        )
    } else null

    val leftBrush = if (leftYStart > leftYEnd) {
        val yCenter = h / 2f
        val tCenter = ((leftYStart - yCenter) / (leftYStart - leftYEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.bottomLeft,
                tCenter to edges.midLeft,
                1.0f to edges.topLeft
            ),
            start = Offset(left, leftYStart),
            end = Offset(left, leftYEnd)
        )
    } else null

    // Tier 1 - Proximal Skirt Brushes (1.50dp)
    val topSkirtBrush = if (topXEnd > topXStart) {
        val xCenter = w / 2f
        val tCenter = ((xCenter - topXStart) / (topXEnd - topXStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.skirtTopLeft,
                tCenter to edges.skirtTopCenter,
                1.0f to edges.skirtTopRight
            ),
            start = Offset(topXStart, top),
            end = Offset(topXEnd, top)
        )
    } else null

    val rightSkirtBrush = if (rightYEnd > rightYStart) {
        val yCenter = h / 2f
        val tCenter = ((yCenter - rightYStart) / (rightYEnd - rightYStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.skirtTopRight,
                tCenter to edges.skirtMidRight,
                1.0f to edges.skirtBottomRight
            ),
            start = Offset(right, rightYStart),
            end = Offset(right, rightYEnd)
        )
    } else null

    val bottomSkirtBrush = if (bottomXStart > bottomXEnd) {
        val xCenter = w / 2f
        val tCenter = ((bottomXStart - xCenter) / (bottomXStart - bottomXEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.skirtBottomRight,
                tCenter to edges.skirtBottomCenter,
                1.0f to edges.skirtBottomLeft
            ),
            start = Offset(bottomXStart, bottom),
            end = Offset(bottomXEnd, bottom)
        )
    } else null

    val leftSkirtBrush = if (leftYStart > leftYEnd) {
        val yCenter = h / 2f
        val tCenter = ((leftYStart - yCenter) / (leftYStart - leftYEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.skirtBottomLeft,
                tCenter to edges.skirtMidLeft,
                1.0f to edges.skirtTopLeft
            ),
            start = Offset(left, leftYStart),
            end = Offset(left, leftYEnd)
        )
    } else null

    // Tier 2 - Distal Halo Brushes (2.20dp)
    val topHaloBrush = if (topXEnd > topXStart) {
        val xCenter = w / 2f
        val tCenter = ((xCenter - topXStart) / (topXEnd - topXStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.haloTopLeft,
                tCenter to edges.haloTopCenter,
                1.0f to edges.haloTopRight
            ),
            start = Offset(topXStart, top),
            end = Offset(topXEnd, top)
        )
    } else null

    val rightHaloBrush = if (rightYEnd > rightYStart) {
        val yCenter = h / 2f
        val tCenter = ((yCenter - rightYStart) / (rightYEnd - rightYStart)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.haloTopRight,
                tCenter to edges.haloMidRight,
                1.0f to edges.haloBottomRight
            ),
            start = Offset(right, rightYStart),
            end = Offset(right, rightYEnd)
        )
    } else null

    val bottomHaloBrush = if (bottomXStart > bottomXEnd) {
        val xCenter = w / 2f
        val tCenter = ((bottomXStart - xCenter) / (bottomXStart - bottomXEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.haloBottomRight,
                tCenter to edges.haloBottomCenter,
                1.0f to edges.haloBottomLeft
            ),
            start = Offset(bottomXStart, bottom),
            end = Offset(bottomXEnd, bottom)
        )
    } else null

    val leftHaloBrush = if (leftYStart > leftYEnd) {
        val yCenter = h / 2f
        val tCenter = ((leftYStart - yCenter) / (leftYStart - leftYEnd)).coerceIn(0.01f, 0.99f)
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to edges.haloBottomLeft,
                tCenter to edges.haloMidLeft,
                1.0f to edges.haloTopLeft
            ),
            start = Offset(left, leftYStart),
            end = Offset(left, leftYEnd)
        )
    } else null

    val haloStrokePx = 2.20.dp.toPx()
    val skirtStrokePx = 1.50.dp.toPx()
    val coreStrokePx = strokeWidthPx

    val haloStrokeStyle = Stroke(width = haloStrokePx, cap = StrokeCap.Butt)
    val skirtStrokeStyle = Stroke(width = skirtStrokePx, cap = StrokeCap.Butt)
    val coreStrokeStyle = Stroke(width = coreStrokePx, cap = StrokeCap.Butt)

    onDrawWithContent {
        drawContent()

        // ─────────────────────────────────────────────────────────────────
        // 1. Tier 2: Distal Radiance Halo (2.20dp, soft outer dispersion)
        // ─────────────────────────────────────────────────────────────────
        if (topHaloBrush != null && topXEnd > topXStart) {
            drawLine(
                brush = topHaloBrush,
                start = Offset(topXStart, top),
                end = Offset(topXEnd, top),
                strokeWidth = haloStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rightHaloBrush != null && rightYEnd > rightYStart) {
            drawLine(
                brush = rightHaloBrush,
                start = Offset(right, rightYStart),
                end = Offset(right, rightYEnd),
                strokeWidth = haloStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (bottomHaloBrush != null && bottomXStart > bottomXEnd) {
            drawLine(
                brush = bottomHaloBrush,
                start = Offset(bottomXStart, bottom),
                end = Offset(bottomXEnd, bottom),
                strokeWidth = haloStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (leftHaloBrush != null && leftYStart > leftYEnd) {
            drawLine(
                brush = leftHaloBrush,
                start = Offset(left, leftYStart),
                end = Offset(left, leftYEnd),
                strokeWidth = haloStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rTR > 0f) {
            drawArc(
                color = edges.haloTopRight,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rTR, top),
                size = Size(2 * rTR, 2 * rTR),
                style = haloStrokeStyle
            )
        }
        if (rBR > 0f) {
            drawArc(
                color = edges.haloBottomRight,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rBR, bottom - 2 * rBR),
                size = Size(2 * rBR, 2 * rBR),
                style = haloStrokeStyle
            )
        }
        if (rBL > 0f) {
            drawArc(
                color = edges.haloBottomLeft,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottom - 2 * rBL),
                size = Size(2 * rBL, 2 * rBL),
                style = haloStrokeStyle
            )
        }
        if (rTL > 0f) {
            drawArc(
                color = edges.haloTopLeft,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(2 * rTL, 2 * rTL),
                style = haloStrokeStyle
            )
        }

        // ─────────────────────────────────────────────────────────────────
        // 2. Tier 1: Proximal Radiance Skirt (1.50dp, intermediate spread)
        // ─────────────────────────────────────────────────────────────────
        if (topSkirtBrush != null && topXEnd > topXStart) {
            drawLine(
                brush = topSkirtBrush,
                start = Offset(topXStart, top),
                end = Offset(topXEnd, top),
                strokeWidth = skirtStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rightSkirtBrush != null && rightYEnd > rightYStart) {
            drawLine(
                brush = rightSkirtBrush,
                start = Offset(right, rightYStart),
                end = Offset(right, rightYEnd),
                strokeWidth = skirtStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (bottomSkirtBrush != null && bottomXStart > bottomXEnd) {
            drawLine(
                brush = bottomSkirtBrush,
                start = Offset(bottomXStart, bottom),
                end = Offset(bottomXEnd, bottom),
                strokeWidth = skirtStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (leftSkirtBrush != null && leftYStart > leftYEnd) {
            drawLine(
                brush = leftSkirtBrush,
                start = Offset(left, leftYStart),
                end = Offset(left, leftYEnd),
                strokeWidth = skirtStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rTR > 0f) {
            drawArc(
                color = edges.skirtTopRight,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rTR, top),
                size = Size(2 * rTR, 2 * rTR),
                style = skirtStrokeStyle
            )
        }
        if (rBR > 0f) {
            drawArc(
                color = edges.skirtBottomRight,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rBR, bottom - 2 * rBR),
                size = Size(2 * rBR, 2 * rBR),
                style = skirtStrokeStyle
            )
        }
        if (rBL > 0f) {
            drawArc(
                color = edges.skirtBottomLeft,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottom - 2 * rBL),
                size = Size(2 * rBL, 2 * rBL),
                style = skirtStrokeStyle
            )
        }
        if (rTL > 0f) {
            drawArc(
                color = edges.skirtTopLeft,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(2 * rTL, 2 * rTL),
                style = skirtStrokeStyle
            )
        }

        // ─────────────────────────────────────────────────────────────────
        // 3. Tier 0: Core Specular Rim (1.00dp, crisp optical apex on top)
        // ─────────────────────────────────────────────────────────────────
        if (topBrush != null && topXEnd > topXStart) {
            drawLine(
                brush = topBrush,
                start = Offset(topXStart, top),
                end = Offset(topXEnd, top),
                strokeWidth = coreStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rightBrush != null && rightYEnd > rightYStart) {
            drawLine(
                brush = rightBrush,
                start = Offset(right, rightYStart),
                end = Offset(right, rightYEnd),
                strokeWidth = coreStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (bottomBrush != null && bottomXStart > bottomXEnd) {
            drawLine(
                brush = bottomBrush,
                start = Offset(bottomXStart, bottom),
                end = Offset(bottomXEnd, bottom),
                strokeWidth = coreStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (leftBrush != null && leftYStart > leftYEnd) {
            drawLine(
                brush = leftBrush,
                start = Offset(left, leftYStart),
                end = Offset(left, leftYEnd),
                strokeWidth = coreStrokePx,
                cap = StrokeCap.Butt
            )
        }
        if (rTR > 0f) {
            drawArc(
                color = edges.topRight,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rTR, top),
                size = Size(2 * rTR, 2 * rTR),
                style = coreStrokeStyle
            )
        }
        if (rBR > 0f) {
            drawArc(
                color = edges.bottomRight,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(right - 2 * rBR, bottom - 2 * rBR),
                size = Size(2 * rBR, 2 * rBR),
                style = coreStrokeStyle
            )
        }
        if (rBL > 0f) {
            drawArc(
                color = edges.bottomLeft,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, bottom - 2 * rBL),
                size = Size(2 * rBL, 2 * rBL),
                style = coreStrokeStyle
            )
        }
        if (rTL > 0f) {
            drawArc(
                color = edges.topLeft,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(2 * rTL, 2 * rTL),
                style = coreStrokeStyle
            )
        }
    }
}

/**
 * Directional specular rim lighting for frosted surfaces.
 * Simulates physical overhead light catch via a vertical linear gradient.
 * When [edgeLighting] is passed, it modulates the specular rim with the background's
 * environmental luminance field across all edges via [drawEnvironmentalSpecularBorder].
 */
fun Modifier.specularGlassBorder(
    shape: Shape,
    isDark: Boolean,
    strokeWidth: Dp = 1.dp,
    topColor: Color? = null,
    bottomColor: Color? = null,
    edgeLighting: EnvironmentalSpecularEdges? = null
): Modifier {
    if (edgeLighting != null) {
        return this.drawEnvironmentalSpecularBorder(
            shape = shape,
            edges = edgeLighting,
            strokeWidth = strokeWidth
        )
    }

    val brush = when {
        topColor != null && bottomColor != null -> Brush.verticalGradient(
            colors = listOf(
                topColor,
                topColor.copy(alpha = (topColor.alpha + bottomColor.alpha) * 0.35f),
                bottomColor
            )
        )
        isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f), // Overhead light reflection
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)  // Ambient bottom falloff
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.70f), // Crisp light sheen
                Color.White.copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.06f)
            )
        )
    }
    return this.border(
        width = strokeWidth,
        brush = brush,
        shape = shape
    )
}

/**
 * Specular Micro-Borders & Directional Rim Lighting for solid elevated cards (surfaceRaised)
 * and floating overlays (surfaceOverlay).
 *
 * Simulates physical overhead light catch via a subtle 1px vertical gradient:
 * - Overhead light catch: subtle white/accent reflection on top edge
 * - Ambient falloff: softened opacity on bottom edges
 *
 * Creates tactile physical separation between cards and background without heavy muddy shadows.
 */
fun Modifier.specularRimBorder(
    shape: Shape = ScribeShapeTokens.Medium,
    isDark: Boolean = true,
    strokeWidth: Dp = 1.dp,
    topAlpha: Float = if (isDark) 0.16f else 0.45f,
    bottomAlpha: Float = if (isDark) 0.03f else 0.08f,
    tintColor: Color? = null
): Modifier = this.border(
    width = strokeWidth,
    brush = Brush.verticalGradient(
        colors = if (tintColor != null) {
            listOf(
                tintColor.copy(alpha = topAlpha),
                tintColor.copy(alpha = (topAlpha + bottomAlpha) * 0.45f),
                tintColor.copy(alpha = bottomAlpha)
            )
        } else if (isDark) {
            listOf(
                Color.White.copy(alpha = topAlpha),
                Color.White.copy(alpha = (topAlpha + bottomAlpha) * 0.45f),
                Color.White.copy(alpha = bottomAlpha)
            )
        } else {
            listOf(
                Color.White.copy(alpha = topAlpha),
                Color.Black.copy(alpha = (topAlpha + bottomAlpha) * 0.2f),
                Color.Black.copy(alpha = bottomAlpha)
            )
        }
    ),
    shape = shape
)

/**
 * High-performance coordinate-mapped wallpaper blur renderer for pre-API 31 devices.
 * Samples the exact screen rectangle behind this composable from [bitmap] (pre-blurred on Dispatchers.IO),
 * overlays [tint], and paints the specular glass border. Zero main-thread CPU capture overhead.
 * Uses true global coordinate sampling so popups, menus, and dialogs in separate windows/layers
 * sample their exact localized slice of the wallpaper without squashing or distortion.
 */
@Composable
fun Modifier.drawWithBackdropBitmap(
    bitmap: Bitmap?,
    tint: Color,
    shape: Shape = RectangleShape,
    isDark: Boolean = LocalAppTheme.current?.isDark == true,
    fallbackColor: Color = ScribeTheme.colors.surfaces.surface,
    topColor: Color? = null,
    bottomColor: Color? = null,
    edgeLighting: EnvironmentalSpecularEdges? = null
): Modifier {
    if (bitmap == null) {
        return this
            .clip(shape)
            .background(fallbackColor, shape = shape)
            .specularGlassBorder(shape, isDark, topColor = topColor, bottomColor = bottomColor, edgeLighting = edgeLighting)
    }

    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    var screenOffset by remember { mutableStateOf(IntOffset.Zero) }

    return this
        .clip(shape)
        .onGloballyPositioned { coords ->
            val viewLocation = IntArray(2)
            view.getLocationOnScreen(viewLocation)
            val windowPos = coords.positionInWindow()

            // True physical screen position regardless of Popup, Window, or Dialog layer
            val globalX = viewLocation[0] + windowPos.x
            val globalY = viewLocation[1] + windowPos.y

            screenOffset = IntOffset(globalX.toInt(), globalY.toInt())
        }
        .drawWithContent {
            val bitmapW = bitmap.width.toFloat()
            val bitmapH = bitmap.height.toFloat()
            val displayMetrics = view.resources.displayMetrics
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                displayMetrics.widthPixels > 0 -> displayMetrics.widthPixels.toFloat()
                else -> bitmapW
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                displayMetrics.heightPixels > 0 -> displayMetrics.heightPixels.toFloat()
                else -> bitmapH
            }

            val srcLeft = (screenOffset.x.toFloat() / screenW * bitmapW).coerceIn(0f, bitmapW)
            val srcTop = (screenOffset.y.toFloat() / screenH * bitmapH).coerceIn(0f, bitmapH)
            val srcRight = ((screenOffset.x + size.width) / screenW * bitmapW).coerceIn(0f, bitmapW)
            val srcBottom = ((screenOffset.y + size.height) / screenH * bitmapH).coerceIn(0f, bitmapH)

            if (srcRight > srcLeft && srcBottom > srcTop) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
                    srcSize = IntSize(
                        (srcRight - srcLeft).toInt().coerceAtLeast(1),
                        (srcBottom - srcTop).toInt().coerceAtLeast(1)
                    ),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            } else {
                drawRect(fallbackColor)
            }

            if (tint != Color.Transparent) {
                drawRect(tint)
            }

            drawContent()
        }
        .specularGlassBorder(shape, isDark, topColor = topColor, bottomColor = bottomColor, edgeLighting = edgeLighting)
}

@Composable
fun Modifier.frostedBar(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.None,
    isDark: Boolean = LocalAppTheme.current?.isDark == true
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surface
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        this.background(solidSurface, shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = 0.92f), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = 0.92f), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Applies a frosted-glass effect to a FAB (or any floating circular button).
 * On Android 12+ this is a real GPU blur via Haze with specular rim border;
 * on older devices it samples the pre-blurred wallpaper crop with specular rim.
 */
@Composable
fun Modifier.frostedFab(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.Full,
    isDark: Boolean = LocalAppTheme.current?.isDark == true
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surfaceRaised
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        this.clip(shape).background(solidSurface, shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = 0.90f), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = 0.90f), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Applies a frosted-glass effect to side panels, navigation drawers, and bottom sheets.
 */
@Composable
fun Modifier.frostedPanel(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.BottomSheet,
    isDark: Boolean = LocalAppTheme.current?.isDark == true
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surface
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        this.clip(shape).background(solidSurface, shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = 0.95f), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = 0.94f), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Frosted glass for contextual dropdowns, anchored popovers, and overflow menus.
 */
@Composable
fun Modifier.frostedMenu(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.Menu,
    isDark: Boolean = LocalAppTheme.current?.isDark == true
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surfaceOverlay
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        this.clip(shape).background(solidSurface, shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = 0.94f), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = 0.94f), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Frosted glass dropdown menu with cross-version support (API 24+).
 * Automatically delegates to [FrostedInWindowDropdownMenu] to render as an in-window overlay in the same tree:
 * - On API 31+: Pure GPU Haze blur with zero popup sub-window isolation.
 * - On API < 31: StackBlur bitmap slicing with accurate screen-coordinate sampling.
 * - Solid themes: Clean opaque theme surface with zero transparency defects.
 */
@Composable
fun FrostedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = ScribeShapeTokens.Menu,
    content: @Composable ColumnScope.() -> Unit
) {
    FrostedInWindowDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = shape,
        content = content
    )
}

/**
 * Frosted glass for Card composables (ElevatedCard, Card, etc).
 */
@Composable
fun Modifier.frostedCard(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.CardMedium,
    isDark: Boolean = LocalAppTheme.current?.isDark == true,
    solidAlpha: Float = 0.92f,
    applyFallbackBackground: Boolean = true
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surfaceRaised
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        if (applyFallbackBackground) {
            this.clip(shape).background(solidSurface.copy(alpha = solidAlpha), shape = shape)
        } else {
            this
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = solidAlpha), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = solidAlpha), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Frosted glass for Chips, Pill tabs, and small badges.
 */
@Composable
fun Modifier.frostedChip(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.Chip,
    isDark: Boolean = LocalAppTheme.current?.isDark == true,
    isSelected: Boolean = false,
    selectedAlpha: Float = 0.25f,
    unselectedAlpha: Float = 0.12f,
    solidAlpha: Float = 0.90f
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surface
    val accentColor = ScribeTheme.colors.interaction.primary
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }

    val baseTint = if (isSelected) accentColor else (if (hasBgImage) adaptiveTokens.glassTint else solidSurface)
    val tintAlpha = if (isSelected) selectedAlpha else unselectedAlpha
    val tintColor = if (tintEnabled) baseTint.copy(alpha = tintAlpha) else (if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent)

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    val selectedSurface = ScribeTheme.colors.surfaces.surfaceSelected
    return if (!hasBgImage) {
        val fallbackBg = if (isSelected) selectedSurface else solidSurface.copy(alpha = solidAlpha)
        this.clip(shape).background(fallbackBg, shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        val fallbackBg = if (isSelected) selectedSurface else solidSurface.copy(alpha = solidAlpha)
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, fallbackBg, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        val fallbackBg = if (isSelected) selectedSurface else solidSurface.copy(alpha = solidAlpha)
        this
            .then(positionModifier)
            .clip(shape)
            .background(fallbackBg, shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

/**
 * Renders a canonical accessibility focus ring using [ScribeColors.interaction.focus]
 * when the component receives keyboard, switch, or accessibility focus.
 *
 * Preserves semantic independence:
 * - Does not override persistent selection fills (e.g. [SurfaceColors.surfaceSelected]).
 * - Does not conflict with transient press scale or touch ripples.
 * - Conforms to WCAG 2.4.7 (Focus Visible) and WCAG 2.4.11 (Focus Appearance).
 */
@Composable
fun Modifier.scribeFocusRing(
    interactionSource: InteractionSource,
    shape: Shape = ScribeShapeTokens.CardSmall,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return if (isFocused) {
        this.border(strokeWidth, ScribeTheme.colors.interaction.focus, shape)
    } else {
        this
    }
}

/**
 * Frosted glass for Search Bars / TextFields.
 */
@Composable
fun Modifier.frostedSearchBox(
    hazeState: HazeState?,
    shape: Shape = ScribeShapeTokens.SearchBar,
    isDark: Boolean = LocalAppTheme.current?.isDark == true,
    solidAlpha: Float = 0.92f
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = ScribeTheme.colors.surfaces.surfaceLowest
    val hasBgImage = localHasBgImage()
    val barBlurBitmap = LocalBarBlurBitmap.current
    val tintEnabled = LocalFrostedTint.current
    val blurRadius = LocalFrostedBlurRadius.current
    val zonalColors = LocalBgZonalColors.current
    val view = LocalView.current
    val (screenWFromLocal, screenHFromLocal) = LocalScreenSize.current
    val (rootWFromLocal, rootHFromLocal) = LocalRootGeometry.current

    val defaultLum = if (isDark) 0.15f else 0.90f
    var currentLuminance by remember { mutableFloatStateOf(defaultLum) }
    var currentAmbientColor by remember { mutableStateOf<Color?>(null) }
    var componentBounds by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }

    val positionModifier = Modifier.onGloballyPositioned { coords ->
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val windowPos = coords.positionInWindow()
        val globalX = viewLocation[0] + windowPos.x
        val globalY = viewLocation[1] + windowPos.y

        val newBounds = Rect(globalX, globalY, coords.size.width.toFloat(), coords.size.height.toFloat())
        if (abs(newBounds.left - componentBounds.left) > 1f ||
            abs(newBounds.top - componentBounds.top) > 1f ||
            abs(newBounds.right - componentBounds.right) > 1f ||
            abs(newBounds.bottom - componentBounds.bottom) > 1f
        ) {
            componentBounds = newBounds
        }

        val screenW = when {
            rootWFromLocal > 0f -> rootWFromLocal
            screenWFromLocal > 0f -> screenWFromLocal
            view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
            else -> 1080f
        }
        val screenH = when {
            rootHFromLocal > 0f -> rootHFromLocal
            screenHFromLocal > 0f -> screenHFromLocal
            view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
            else -> 1920f
        }
        val lum = interpolateSpatialLuminance(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalLuminance = theme?.savedZonalLuminance,
            fallbackLuminance = defaultLum
        )
        if (abs(lum - currentLuminance) > 0.005f) {
            currentLuminance = lum
        }
        val color = interpolateSpatialColor(
            screenOffsetX = globalX,
            screenOffsetY = globalY,
            componentWidth = coords.size.width.toFloat(),
            componentHeight = coords.size.height.toFloat(),
            screenW = screenW,
            screenH = screenH,
            zonalColors = zonalColors,
            fallback = null
        )
        if (color != currentAmbientColor) {
            currentAmbientColor = color
        }
    }

    val fallbackColor = remember(theme?.savedBgDominantColor) {
        theme?.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    val zoneSourceColor = currentAmbientColor ?: fallbackColor
    val adaptiveTokens = remember(currentLuminance, theme?.colors, zoneSourceColor, isDark) {
        deriveAdaptiveTokens(
            backgroundLightness = currentLuminance,
            baseColors = theme?.colors,
            sourceImageColor = zoneSourceColor,
            isThemeDark = isDark
        )
    }
    val tintColor = if (tintEnabled) {
        if (hasBgImage) adaptiveTokens.glassTint else solidSurface.copy(alpha = 0.22f)
    } else {
        Color.Transparent
    }

    val specularEdges = remember(
        componentBounds,
        adaptiveTokens.glassSpecularTop,
        adaptiveTokens.glassSpecularBottom,
        theme?.savedBgLuminanceField,
        theme?.savedZonalLuminance,
        screenWFromLocal,
        screenHFromLocal
    ) {
        if (componentBounds.right > 0f && componentBounds.bottom > 0f) {
            val screenW = when {
                rootWFromLocal > 0f -> rootWFromLocal
                screenWFromLocal > 0f -> screenWFromLocal
                view.resources.displayMetrics.widthPixels > 0 -> view.resources.displayMetrics.widthPixels.toFloat()
                else -> 1080f
            }
            val screenH = when {
                rootHFromLocal > 0f -> rootHFromLocal
                screenHFromLocal > 0f -> screenHFromLocal
                view.resources.displayMetrics.heightPixels > 0 -> view.resources.displayMetrics.heightPixels.toFloat()
                else -> 1920f
            }
            computeEnvironmentalSpecularEdges(
                screenOffsetX = componentBounds.left,
                screenOffsetY = componentBounds.top,
                componentWidth = componentBounds.right,
                componentHeight = componentBounds.bottom,
                screenW = screenW,
                screenH = screenH,
                luminanceField = theme?.savedBgLuminanceField,
                zonalLuminance = theme?.savedZonalLuminance,
                baseTopColor = adaptiveTokens.glassSpecularTop,
                baseBottomColor = adaptiveTokens.glassSpecularBottom,
                fallbackLuminance = defaultLum
            )
        } else {
            null
        }
    }

    return if (!hasBgImage) {
        this.clip(shape).background(solidSurface.copy(alpha = solidAlpha), shape = shape)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(blurRadius = blurRadius.dp, tint = HazeTint(tintColor), noiseFactor = 0f)
            )
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom)
    } else if (barBlurBitmap != null) {
        this
            .then(positionModifier)
            .clip(shape)
            .drawWithBackdropBitmap(barBlurBitmap, tintColor, shape, isDark, solidSurface.copy(alpha = solidAlpha), topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    } else {
        this
            .then(positionModifier)
            .clip(shape)
            .background(solidSurface.copy(alpha = solidAlpha), shape = shape)
            .specularGlassBorder(shape, isDark, topColor = adaptiveTokens.glassSpecularTop, bottomColor = adaptiveTokens.glassSpecularBottom, edgeLighting = specularEdges)
    }
}

// ── Composable wrappers that combine frosted modifiers with LocalContentColor ──
//
// The frosted*() modifier functions are @Composable but return a Modifier, so they
// cannot host CompositionLocalProvider themselves. These wrapper composables apply
// both the frosted modifier AND set LocalContentColor to the correct contrasting
// colour so every Text and Icon inside inherits the right colour automatically,
// with no per-element colour arguments needed.

/**
 * Wraps [content] with LocalContentColor set to contrast against the frosted bar surface.
 *
 * When a background image is active, uses [savedBgLuminance] (precomputed at crop time)
 * to determine whether white or dark text is needed against the real visual background.
 * Falls back to [autoTextColor] against the solid surface for old themes without this field.
 * Use this around TopAppBar / NavigationBar / BottomAppBar content lambdas.
 */
@Composable
fun FrostedBarContent(content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current
    val savedLum = theme?.zonalLuminance(AmbientZone.TOP_APP_BAR) ?: (theme?.savedBgLuminance ?: -1f)
    val hasBgImage = localHasBgImage()
    val contentColor = when {
        hasBgImage && savedLum >= 0f -> if (savedLum < 0.45f) Color(0xFFFAF9F8) else Color(0xFF141416)
        else -> ScribeTheme.colors.content.primary
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}

/**
 * Wraps [content] with LocalContentColor set to contrast against the frosted panel surface.
 *
 * When a background image is active, uses [savedBgLuminance] (precomputed at crop time)
 * to determine whether white or dark text is needed against the real visual background.
 * Falls back to [autoTextColor] against the solid surface for old themes without this field.
 * Use this around drawer / side-panel content.
 */
@Composable
fun FrostedPanelContent(content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current
    val savedLum = theme?.zonalLuminance(AmbientZone.GLOBAL) ?: (theme?.savedBgLuminance ?: -1f)
    val hasBgImage = localHasBgImage()
    val contentColor = when {
        hasBgImage && savedLum >= 0f -> if (savedLum < 0.45f) Color(0xFFFAF9F8) else Color(0xFF141416)
        else -> ScribeTheme.colors.content.primary
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}

/**
 * Wraps [content] with LocalContentColor set to contrast against the frosted card surface.
 * Use this around card body content.
 */
@Composable
fun FrostedCardContent(content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current
    val hasBgImage = localHasBgImage()
    val savedLum = theme?.zonalLuminance(AmbientZone.MAIN_CONTENT) ?: (theme?.savedBgLuminance ?: -1f)
    val contentColor = if (hasBgImage && savedLum >= 0f) {
        if (savedLum < 0.45f) Color(0xFFFAF9F8) else Color(0xFF141416)
    } else {
        ScribeTheme.colors.content.primary
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}

/**
 * A dialog that lives in the same window as the rest of the UI, so Haze blur works correctly.
 * Standard AlertDialog creates a separate Android window which breaks hazeEffect.
 *
 * Usage: replace AlertDialog with FrostedDialog. The API mirrors AlertDialog.
 *
 * When no background image is active the dialog uses the solid surface color (guaranteed opaque).
 * When a background image IS active, the dialog surface gets the frosted blur via hazeEffect (API 31+)
 * or coordinate-mapped StackBlur (pre-API 31) with directional specular rim lighting.
 */
@Composable
fun FrostedDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ScribeShapeTokens.Dialog,
) {
    val hazeState = LocalHazeState.current
    val isDark = LocalAppTheme.current?.isDark == true

    BackHandler { onDismissRequest() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        val dialogContentColor = ScribeTheme.colors.content.primary
        CompositionLocalProvider(LocalContentColor provides dialogContentColor) {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .clip(shape)
                    .frostedCard(
                        hazeState = hazeState,
                        shape = shape,
                        isDark = isDark,
                        solidAlpha = 0.98f,
                        applyFallbackBackground = true
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { /* consume so taps inside don't dismiss */ }
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                icon?.let {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        it()
                    }
                }
                title?.let {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.headlineSmall
                    ) { it() }
                }
                text?.let {
                    ProvideTextStyle(
                        value = MaterialTheme.typography.bodyMedium
                    ) { it() }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/**
 * Returns the correct containerColor for a Card or FAB when frosted glass is active.
 * When a background image is set and blur is allowed, returns [Color.Transparent] so
 * hazeEffect / drawWithBackdropBitmap shows through. Otherwise returns [fallback].
 */
@Composable
fun frostedContainerColor(fallback: Color): Color {
    val hazeState = LocalHazeState.current
    val barBlurBitmap = LocalBarBlurBitmap.current
    val hasBgImage = localHasBgImage()
    val legacyReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && barBlurBitmap != null
    val modernReady = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null
    return if (hasBgImage && (modernReady || legacyReady)) Color.Transparent else fallback
}

/**
 * Backward-compatible bridge for legacy drawWithOneShotBitmap.
 * Automatically delegates to modern drawWithBackdropBitmap or frostedCard.
 */
@Composable
fun Modifier.drawWithOneShotBitmap(bitmap: Bitmap?, tint: Color): Modifier {
    val hazeState = LocalHazeState.current
    val isDark = LocalAppTheme.current?.isDark == true
    val barBlurBitmap = LocalBarBlurBitmap.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.frostedCard(hazeState = hazeState, isDark = isDark)
    } else {
        this.drawWithBackdropBitmap(bitmap ?: barBlurBitmap, tint, isDark = isDark)
    }
}

fun parseComposeColor(hex: String, fallback: Color = Color.Black): Color {
    // Guard blank/empty strings before hitting ThemeManager — avoids returning
    // the fallback colour when a theme has an uninitialised hex field, which
    // would accidentally match lightDefault or darkDefault in isDefaultText.
    if (hex.isBlank()) return fallback
    return try {
        Color(ThemeManager.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun ScribeComposeTheme(
    appTheme: AppTheme? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Use the app-singleton ThemeManager so its in-memory cache (seeded by ScribeApp
    // and kept up-to-date by ThemeViewModel.save/delete) is always current.
    // Creating a new ThemeManager here would give an instance whose cache is never
    // seeded, causing allThemes() to return only built-ins (no backgroundImageUri).
    val themeManager = remember { (context.applicationContext as ScribeApp).themeManager }
    val resolvedTheme = if (appTheme != null) {
        appTheme
    } else {
        val dataStore = remember { (context.applicationContext as ScribeApp).dataStore }
        val activeThemeId by dataStore.activeThemeIdFlow.collectAsState(
            initial = themeManager.activeTheme().id
        )
        val customThemesJson by dataStore.customThemesJsonFlow.collectAsState(initial = "[]")
        remember(activeThemeId, customThemesJson) {
            themeManager.allThemes().firstOrNull { it.id == activeThemeId }
                ?: DefaultThemes.all.first()
        }
    }

    val bgUri = resolvedTheme.backgroundImageUri
    val hasBgImage = !bgUri.isNullOrEmpty() && resolvedTheme.bgMode != "color"
    val view = LocalView.current
    val screenWidthPx = remember(view) { view.resources.displayMetrics.widthPixels.toFloat() }
    val screenHeightPx = remember(view) { view.resources.displayMetrics.heightPixels.toFloat() }
    var analysisBitmap by remember(resolvedTheme.id, bgUri, hasBgImage) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(resolvedTheme.id, bgUri, hasBgImage, screenWidthPx, screenHeightPx) {
        if (!hasBgImage || bgUri.isNullOrEmpty()) {
            analysisBitmap = null
            return@LaunchedEffect
        }
        analysisBitmap = withContext(Dispatchers.IO) {
            try {
                val targetW = 200
                val targetH = ((200f * screenHeightPx) / screenWidthPx.coerceAtLeast(1f)).toInt().coerceAtLeast(200)
                val request = ImageRequest.Builder(context)
                    .data(bgUri)
                    .size(coil3.size.Size(targetW, targetH))
                    .allowHardware(false) // Prevents hardware bitmap; getPixel() requires software config
                    .build()
                (ImageLoader(context).execute(request).image as? BitmapImage)?.bitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    // Fast path: precomputed 9 zonal dominant colors from saved theme
    val savedZonalColors = remember(resolvedTheme.id, resolvedTheme.savedBgZonalColors) {
        if (resolvedTheme.savedBgZonalColors.size >= 9) {
            resolvedTheme.savedBgZonalColors.map { parseComposeColor(it) }
        } else {
            emptyList()
        }
    }

    // Live fallback if savedBgZonalColors is empty but analysisBitmap is loaded
    var liveZonalColors by remember(resolvedTheme.id, bgUri, hasBgImage) { mutableStateOf<List<Color>>(emptyList()) }
    LaunchedEffect(resolvedTheme.id, analysisBitmap, savedZonalColors.size, hasBgImage) {
        if (hasBgImage && savedZonalColors.isEmpty() && analysisBitmap != null) {
            val ints = withContext(Dispatchers.IO) {
                computeZonalDominantColorMatrix(analysisBitmap!!)
            }
            if (ints.size >= 9) {
                liveZonalColors = ints.map { Color(it) }
            }
        } else if (savedZonalColors.isNotEmpty() || !hasBgImage) {
            liveZonalColors = emptyList()
        }
    }

    val activeZonalColors = if (savedZonalColors.isNotEmpty()) savedZonalColors else liveZonalColors

    val savedDominantColor = remember(resolvedTheme.id, resolvedTheme.savedBgDominantColor) {
        resolvedTheme.savedBgDominantColor?.let { parseComposeColor(it) }
    }
    var liveDominantColor by remember(resolvedTheme.id, bgUri, hasBgImage) { mutableStateOf<Color?>(null) }
    LaunchedEffect(resolvedTheme.id, analysisBitmap, savedDominantColor, hasBgImage) {
        if (hasBgImage && savedDominantColor == null && analysisBitmap != null) {
            val domInt = withContext(Dispatchers.IO) {
                computeGlobalDominantColor(analysisBitmap!!)
            }
            liveDominantColor = Color(domInt)
        } else if (savedDominantColor != null || !hasBgImage) {
            liveDominantColor = null
        }
    }
    val activeDominantColor = savedDominantColor ?: liveDominantColor

    val bg = parseComposeColor(resolvedTheme.colors.background, Color(0xFFFAFAF7))
    val surfaceLowest = parseComposeColor(resolvedTheme.colors.surfaceLowest, bg)
    val surface = parseComposeColor(resolvedTheme.colors.surface, Color.White)
    val surfaceRaised = parseComposeColor(resolvedTheme.colors.surfaceRaised, surface)
    val surfaceOverlay = parseComposeColor(resolvedTheme.colors.surfaceOverlay, surfaceRaised)
    // Fallback is Color.Black (0xFF000000), NOT Color(0xFF1A1A1A) which equals
    // darkDefault.  If parsing fails on an empty/malformed hex, a fallback that
    // equals darkDefault would set isDefaultText = true and silently enable the
    // auto-luminance override — the secondary cause of the text-revert bug.
    val configuredText = parseComposeColor(resolvedTheme.colors.text, Color.Black)
    val configuredAccent = parseComposeColor(resolvedTheme.colors.accent, Color(0xFF333333))
    val border = parseComposeColor(
        resolvedTheme.colors.border,
        if (resolvedTheme.isDark) Color(0xFF2C2C32) else Color(0xFFD4D4D8)
    )
    val borderSubtle = parseComposeColor(
        resolvedTheme.colors.borderSubtle,
        if (resolvedTheme.isDark) Color(0xFF232328) else Color(0xFFE4E4E7)
    )
    val surfaceVariant = surfaceRaised

    val bgLum = resolvedTheme.savedBgLuminance

    // ── Text colour resolution ────────────────────────────────────────────────
    // Priority order:
    // 1. savedBgLuminance ≥ 0: use the precomputed real background luminance.
    //    Only override if the user hasn't manually set a custom text colour.
    //    We detect "custom" by checking whether configuredText differs from BOTH
    //    the light default and the dark default — if it matches neither, the user
    //    picked something intentional and we leave it alone.
    // 2. analysisBitmap available (old theme, no savedBgLuminance): live analysis.
    // 3. Neither: use the stored configuredText as-is.
    val lightDefault = Color.White
    val darkDefault = Color(0xFF1A1A1A)
    val isDefaultText = configuredText == lightDefault || configuredText == darkDefault
    val text: Color = when {
        hasBgImage && bgLum >= 0f && isDefaultText ->
            if (bgLum < 0.45f) Color.White else Color(0xFF1A1A1A)
        hasBgImage && bgLum < 0f && analysisBitmap != null -> {
            // Fallback: live analysis for old themes
            contrastingTextColor(
                bitmap = analysisBitmap,
                screenRect = Rect(0f, 0f, screenWidthPx, screenHeightPx),
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx
            )
        }
        else -> configuredText
    }

    val subtleTextResolved = parseComposeColor(resolvedTheme.colors.subtleText, text.copy(alpha = 0.6f))
    val mutedTextResolved = parseComposeColor(resolvedTheme.colors.mutedText, text.copy(alpha = 0.75f))
    val disabledTextResolved = text.copy(alpha = 0.38f)

    val secondaryColor = parseComposeColor(
        resolvedTheme.colors.secondary,
        if (resolvedTheme.isDark) Color(0xFFA58BEA) else Color(0xFF6366F1)
    )
    val tertiaryColor = parseComposeColor(
        resolvedTheme.colors.tertiary,
        if (resolvedTheme.isDark) Color(0xFF63D5D0) else Color(0xFF0D9488)
    )

    val successResolved = parseComposeColor(
        resolvedTheme.colors.success,
        if (resolvedTheme.isDark) Color(0xFF55D18A) else Color(0xFF2E7D32)
    )
    val warningResolved = parseComposeColor(
        resolvedTheme.colors.warning,
        if (resolvedTheme.isDark) Color(0xFFFFC857) else Color(0xFFD97706)
    )
    val errorResolved = parseComposeColor(
        resolvedTheme.colors.error,
        if (resolvedTheme.isDark) Color(0xFFFF6B7A) else Color(0xFFDC2626)
    )
    val infoResolved = parseComposeColor(
        resolvedTheme.colors.info,
        if (resolvedTheme.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    )

    val accentMutedResolved = parseComposeColor(resolvedTheme.colors.accentMuted, surface)

    // ── Accent colour resolution ──────────────────────────────────────────────
    // Use adaptiveAccentColor with savedBgLuminance so it contrasts the real image,
    // not the theme surface. This is now used for the full color scheme (accentIcons),
    // replacing the old hard-coded white/black override.
    val accentIcons = adaptiveAccentColor(
        accent = configuredAccent,
        solidSurface = surface,
        hasBgImage = hasBgImage,
        savedBgLuminance = bgLum
    )

    val borderProminentResolved = parseComposeColor(resolvedTheme.colors.borderProminent, accentIcons)
    val focusResolved = if (resolvedTheme.colors.focus.isNotBlank()) {
        parseComposeColor(resolvedTheme.colors.focus, borderProminentResolved)
    } else {
        borderProminentResolved
    }

    val onPrimaryColor = ContrastResolver.resolveOnColor(
        container = accentIcons,
        preferredForeground = if (resolvedTheme.isDark) bg else text,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val onPrimaryContainerColor = ContrastResolver.resolveOnColor(
        container = accentMutedResolved,
        preferredForeground = text,
        minRatio = 3.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val onSecondaryColor = ContrastResolver.resolveOnColor(
        container = secondaryColor,
        preferredForeground = if (resolvedTheme.isDark) bg else text,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val onTertiaryColor = ContrastResolver.resolveOnColor(
        container = tertiaryColor,
        preferredForeground = if (resolvedTheme.isDark) bg else text,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )

    val onSuccessColor = ContrastResolver.resolveOnColor(
        container = successResolved,
        preferredForeground = if (resolvedTheme.isDark) bg else Color.White,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val successContainerResolved = successResolved.copy(alpha = if (resolvedTheme.isDark) 0.16f else 0.12f)
    val onSuccessContainerColor = ContrastResolver.resolveContrast(
        background = surface,
        preferredForeground = if (resolvedTheme.isDark) successResolved else Color(0xFF1B5E20),
        minRatio = 3.5
    ).color

    val onWarningColor = ContrastResolver.resolveOnColor(
        container = warningResolved,
        preferredForeground = if (resolvedTheme.isDark) Color(0xFF141416) else Color.Black,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val warningContainerResolved = warningResolved.copy(alpha = if (resolvedTheme.isDark) 0.16f else 0.12f)
    val onWarningContainerColor = ContrastResolver.resolveContrast(
        background = surface,
        preferredForeground = if (resolvedTheme.isDark) warningResolved else Color(0xFF92400E),
        minRatio = 3.5
    ).color

    val onErrorColor = ContrastResolver.resolveOnColor(
        container = errorResolved,
        preferredForeground = if (resolvedTheme.isDark) bg else Color.White,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val errorContainerResolved = errorResolved.copy(alpha = if (resolvedTheme.isDark) 0.16f else 0.12f)
    val onErrorContainerColor = ContrastResolver.resolveContrast(
        background = surface,
        preferredForeground = if (resolvedTheme.isDark) errorResolved else Color(0xFF991B1B),
        minRatio = 3.5
    ).color

    val onInfoColor = ContrastResolver.resolveOnColor(
        container = infoResolved,
        preferredForeground = if (resolvedTheme.isDark) bg else Color.White,
        minRatio = 4.5,
        isDarkTheme = resolvedTheme.isDark
    )
    val infoContainerResolved = infoResolved.copy(alpha = if (resolvedTheme.isDark) 0.16f else 0.12f)
    val onInfoContainerColor = ContrastResolver.resolveContrast(
        background = surface,
        preferredForeground = if (resolvedTheme.isDark) infoResolved else Color(0xFF075985),
        minRatio = 3.5
    ).color

    val isLight = !resolvedTheme.isDark

    val rawColorScheme: ColorScheme = if (isLight) {
        lightColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = accentMutedResolved,
            onPrimaryContainer = onPrimaryContainerColor,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = tertiaryColor,
            onTertiary = onTertiaryColor,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = mutedTextResolved,
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = bg,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceOverlay,
            surfaceTint = accentIcons,
            inverseSurface = text,
            inverseOnSurface = bg,
            inversePrimary = accentIcons,
            outline = border,
            outlineVariant = borderSubtle,
            scrim = Color.Black.copy(alpha = 0.32f),
            error = errorResolved,
            onError = onErrorColor,
            errorContainer = errorContainerResolved,
            onErrorContainer = onErrorContainerColor
        )
    } else {
        darkColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = accentMutedResolved,
            onPrimaryContainer = onPrimaryContainerColor,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = tertiaryColor,
            onTertiary = onTertiaryColor,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = mutedTextResolved,
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = bg,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceOverlay,
            surfaceTint = accentIcons,
            inverseSurface = text,
            inverseOnSurface = bg,
            inversePrimary = accentIcons,
            outline = border,
            outlineVariant = borderSubtle,
            scrim = Color.Black.copy(alpha = 0.32f),
            error = errorResolved,
            onError = onErrorColor,
            errorContainer = errorContainerResolved,
            onErrorContainer = onErrorContainerColor
        )
    }

    // Use snap() on first composition and when the theme ID hasn't actually changed
    // (e.g. a color tweak inside the same theme triggers recomposition but should not
    // animate). Only animate when the user deliberately switches to a different theme.
    var prevThemeId by remember { mutableStateOf<String?>(null) }
    val isThemeChanging = prevThemeId != null && prevThemeId != resolvedTheme.id
    SideEffect { prevThemeId = resolvedTheme.id }

    val animSpec = if (isThemeChanging) tween<Color>(durationMillis = 400) else snap()

    val animPrimary by animateColorAsState(rawColorScheme.primary, animSpec, label = "primary")
    val animOnPrimary by animateColorAsState(rawColorScheme.onPrimary, animSpec, label = "onPrimary")
    val animBg by animateColorAsState(rawColorScheme.background, animSpec, label = "bg")
    val animOnBg by animateColorAsState(rawColorScheme.onBackground, animSpec, label = "onBg")
    val animSurfaceLowest by animateColorAsState(rawColorScheme.surfaceContainerLowest, animSpec, label = "surfaceLowest")
    val animSurface by animateColorAsState(rawColorScheme.surface, animSpec, label = "surface")
    val animSurfaceRaised by animateColorAsState(rawColorScheme.surfaceContainerHigh, animSpec, label = "surfaceRaised")
    val animSurfaceOverlay by animateColorAsState(rawColorScheme.surfaceContainerHighest, animSpec, label = "surfaceOverlay")
    val animOnSurface by animateColorAsState(rawColorScheme.onSurface, animSpec, label = "onSurface")
    val animSurfaceVariant by animateColorAsState(rawColorScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val animOnSurfaceVariant by animateColorAsState(rawColorScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
    val animOutline by animateColorAsState(rawColorScheme.outline, animSpec, label = "outline")
    val animOutlineVariant by animateColorAsState(rawColorScheme.outlineVariant, animSpec, label = "outlineVariant")

    val showWholeAppBg = resolvedTheme.themeScope == "whole_app" && hasBgImage

    // When a whole-app background image is active, surfaces must be transparent so
    // the image shows through and the Haze blur effect works. However, we must NOT
    // use Color.Transparent (= ARGB 0,0,0,0 — transparent BLACK) because any
    // downstream call like surface.copy(alpha = 0.95f) would produce a near-opaque
    // BLACK instead of the theme colour. Instead, we zero only the alpha channel
    // while keeping the RGB channels intact, so copy(alpha = X) restores the
    // correct colour at the requested opacity.
    val glassySurfaceLowest  = if (showWholeAppBg) animSurfaceLowest.copy(alpha = 0f)  else animSurfaceLowest
    val glassySurface        = if (showWholeAppBg) animSurface.copy(alpha = 0f)        else animSurface
    val glassySurfaceRaised  = if (showWholeAppBg) animSurfaceRaised.copy(alpha = 0f)  else animSurfaceRaised
    val glassySurfaceOverlay = if (showWholeAppBg) animSurfaceOverlay.copy(alpha = 0f) else animSurfaceOverlay
    val glassySurfaceVariant = if (showWholeAppBg) animSurfaceVariant.copy(alpha = 0f) else animSurfaceVariant
    val glassyBg             = if (showWholeAppBg) animBg.copy(alpha = 0f)             else animBg

    val animatedColorScheme = rawColorScheme.copy(
        primary = animPrimary,
        onPrimary = animOnPrimary,
        primaryContainer = accentMutedResolved,
        onPrimaryContainer = onPrimaryContainerColor,
        secondary = secondaryColor,
        onSecondary = animOnPrimary,
        secondaryContainer = glassySurfaceVariant,
        onSecondaryContainer = animOnSurface,
        tertiary = tertiaryColor,
        onTertiary = animOnPrimary,
        tertiaryContainer = glassySurfaceVariant,
        onTertiaryContainer = animOnSurface,
        background = glassyBg,
        onBackground = animOnBg,
        surface = glassySurface,
        onSurface = animOnSurface,
        surfaceVariant = glassySurfaceVariant,
        onSurfaceVariant = animOnSurfaceVariant,
        surfaceContainerLowest = glassySurfaceLowest,
        surfaceContainerLow = glassyBg,
        surfaceContainer = glassySurface,
        surfaceContainerHigh = glassySurfaceRaised,
        surfaceContainerHighest = glassySurfaceOverlay,
        outline = animOutline,
        outlineVariant = animOutlineVariant
    )

    // enableEdgeToEdge() (called in each Activity) owns bar transparency.
    // Here we only update icon/caret appearance to match the active theme.
    val window = (LocalContext.current as? Activity)?.window
    SideEffect {
        window?.let { win ->
            WindowCompat.getInsetsController(win, win.decorView).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    // Only activate Haze when there is actually a background image in blurred mode.
    // blurEnabled = false makes Haze fully dormant — no GPU overhead, no blur passes.
    // image mode (sharp wallpaper) and color mode (no image) both get blurEnabled = false.
    val hazeState = rememberHazeState(blurEnabled = hasBgImage && resolvedTheme.bgMode == "blurred")

    // ── Pre-compute blur inputs before CompositionLocalProvider ──────────────
    // These vals must live here (not inside the provider's content lambda) because
    // barBlurBitmap is referenced in the provider list itself.

    val bgOpacity = resolvedTheme.backgroundImageOpacity ?: 0.35f
    val bgMode = resolvedTheme.bgMode
    val blurIntensity = resolvedTheme.blurIntensity
    val frostedTintEnabled = resolvedTheme.frostedTintEnabled
    val frostedBlurRadius = resolvedTheme.frostedBlurRadius

    // On API < 31 we can't use RenderEffect on a live composable, so we
    // pre-blur the source bitmap once using pure Kotlin stack blur and
    // display that pre-blurred bitmap instead.
    //
    // Even for bgMode == "image" (no blur), we must load with
    // allowHardware(false) on API < 31 so that BitmapBlur.captureOnly
    // can draw the view onto a software Canvas without crashing.
    // Hardware bitmaps throw an exception when drawn onto a software Canvas,
    // which captureOnly silently catches → returns null → frosted glass falls
    // back to solid. By keeping the background image as a software bitmap
    // the capture succeeds and one-shot blur works correctly.
    val needsSoftwareBlur = bgMode == "blurred" &&
            blurIntensity > 0f &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            bgUri != null

    // For bgMode == "image" on API < 31 we still need a software bitmap
    // (no blur) so captureOnly can read the view hierarchy.
    val needsSoftwareImage = bgMode == "image" &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            bgUri != null

    // ── Aspect-ratio-aware bitmap sizes ──────────────────────────────────────
    // The background AsyncImage now uses ContentScale.FillBounds — the saved
    // cropped JPEG is already exactly the screen's aspect ratio, so FillBounds
    // stretches it to fill without any re-cropping or distortion.
    // Blur bitmaps are still loaded at screen aspect ratio so that the blur
    // and the visible background stay in sync across API levels.
    val blurLoadW = (screenWidthPx * 0.5f).toInt().coerceAtLeast(1)
    val blurLoadH = (screenHeightPx * 0.5f).toInt().coerceAtLeast(1)

    val softwareBlurredModel by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = bgUri,
        key2 = blurIntensity,
        key3 = needsSoftwareBlur
    ) {
        if (!needsSoftwareBlur) {
            value = null
            return@produceState
        }
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loader = coil3.ImageLoader(context)
                val req = coil3.request.ImageRequest.Builder(context)
                    .data(bgUri)
                    .size(coil3.size.Size(blurLoadW, blurLoadH))
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req)
                val bmp = (result as? coil3.request.SuccessResult)
                    ?.image
                    ?.let { (it as? coil3.BitmapImage)?.bitmap }
                bmp?.let {
                    val radiusPx = (blurIntensity * 0.8f).toInt().coerceIn(1, 25)
                    com.primaloptima.scribe.util.BitmapBlur.blurBitmap(it, radiusPx)
                    // applyFrostedGlassLook is now called inside blurBitmap — no chain needed here.
                }
            } catch (_: Exception) { null }
        }
    }

    // Software (non-blurred) image for bgMode == "image" on API < 31.
    // Load at full screen resolution so captureOnly has accurate pixel data.
    val softwareImageModel by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = bgUri,
        key2 = needsSoftwareImage
    ) {
        if (!needsSoftwareImage) {
            value = null
            return@produceState
        }
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val loader = coil3.ImageLoader(context)
                val req = coil3.request.ImageRequest.Builder(context)
                    .data(bgUri)
                    .size(coil3.size.Size(screenWidthPx.toInt(), screenHeightPx.toInt()))
                    .allowHardware(false)
                    .build()
                val result = loader.execute(req)
                (result as? coil3.request.SuccessResult)
                    ?.image
                    ?.let { (it as? coil3.BitmapImage)?.bitmap }
            } catch (_: Exception) { null }
        }
    }

    // ── Bar blur bitmap (API < 31 only) ──────────────────────────────────────
    // Derived from bitmaps Coil already loaded above — no extra request.
    // Provided as LocalBarBlurBitmap for bars and FABs only.
    // Dialogs and drawers are unaffected (they use LocalOneShotBitmap).
    val barBlurBitmap by produceState<Bitmap?>(
        initialValue = null,
        keys = arrayOf(bgUri, bgMode, softwareBlurredModel, softwareImageModel, frostedBlurRadius)
    ) {
        value = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || !hasBgImage) {
            return@produceState  // Haze handles it natively on API 31+
        }
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            when {
                // blurred mode: softwareBlurredModel is already blurred
                bgMode == "blurred" && softwareBlurredModel != null ->
                    softwareBlurredModel

                // image mode: blur the sharp image now
                // applyFrostedGlassLook is now called inside blurBitmap — no chain needed.
                bgMode == "image" && softwareImageModel != null ->
                    com.primaloptima.scribe.util.BitmapBlur.blurBitmap(softwareImageModel!!, radius = frostedBlurRadius.toInt().coerceIn(1, 25))

                // fallback: load fresh at screen aspect ratio (rare cold-start race)
                bgUri != null -> {
                    try {
                        val loader = coil3.ImageLoader(context)
                        val req = coil3.request.ImageRequest.Builder(context)
                            .data(bgUri)
                            .size(coil3.size.Size(blurLoadW, blurLoadH))
                            .allowHardware(false)
                            .build()
                        val bmp = (loader.execute(req) as? coil3.request.SuccessResult)
                            ?.image
                            ?.let { (it as? coil3.BitmapImage)?.bitmap }
                        // applyFrostedGlassLook is now called inside blurBitmap — no chain needed.
                        bmp?.let { com.primaloptima.scribe.util.BitmapBlur.blurBitmap(it, radius = frostedBlurRadius.toInt().coerceIn(1, 25)) }
                    } catch (_: Exception) { null }
                }

                else -> null
            }
        }
    }

    var rootDimensions by remember { mutableStateOf(Pair(screenWidthPx, screenHeightPx)) }
    val menuHostState = remember { InWindowMenuHostState() }

    val dialogueResolved = parseComposeColor(resolvedTheme.colors.dialogueText, accentIcons)
    val monologueResolved = parseComposeColor(resolvedTheme.colors.monologueText, text)
    val headingResolved = parseComposeColor(resolvedTheme.colors.headingText, accentIcons)
    val annotationResolved = parseComposeColor(
        resolvedTheme.colors.annotation,
        if (resolvedTheme.isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
    )
    val highlightResolved = parseComposeColor(
        resolvedTheme.colors.specialHighlight,
        if (resolvedTheme.isDark) Color(0xFFE7B85A) else Color(0xFFB45309)
    )
    val linkResolved = parseComposeColor(
        resolvedTheme.colors.link,
        accentIcons
    )

    // Analytics & Metrics Semantics (Independent role resolution)
    val analyticsPositiveResolved = parseComposeColor(
        resolvedTheme.colors.analyticsPositive,
        if (resolvedTheme.isDark) Color(0xFF10B981) else Color(0xFF059669)
    )
    val analyticsNeutralResolved = parseComposeColor(
        resolvedTheme.colors.analyticsNeutral,
        mutedTextResolved
    )
    val analyticsNegativeResolved = parseComposeColor(
        resolvedTheme.colors.analyticsNegative,
        if (resolvedTheme.isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    )
    val analyticsSeries1Resolved = parseComposeColor(
        resolvedTheme.colors.analyticsSeries1,
        accentIcons
    )
    val analyticsSeries2Resolved = parseComposeColor(
        resolvedTheme.colors.analyticsSeries2,
        if (resolvedTheme.isDark) Color(0xFF8B5CF6) else Color(0xFF6D28D9)
    )
    val analyticsSeries3Resolved = parseComposeColor(
        resolvedTheme.colors.analyticsSeries3,
        if (resolvedTheme.isDark) Color(0xFF06B6D4) else Color(0xFF0E7490)
    )
    val analyticsTargetResolved = parseComposeColor(
        resolvedTheme.colors.analyticsTarget,
        if (resolvedTheme.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    )
    val analyticsWarningResolved = parseComposeColor(
        resolvedTheme.colors.analyticsWarning,
        if (resolvedTheme.isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    )

    // Worldbuilding Lore Entity Semantics (Independent role resolution)
    val worldCharacterResolved = parseComposeColor(
        resolvedTheme.colors.worldCharacter,
        accentIcons
    )
    val worldLocationResolved = parseComposeColor(
        resolvedTheme.colors.worldLocation,
        if (resolvedTheme.isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    )
    val worldFactionResolved = parseComposeColor(
        resolvedTheme.colors.worldFaction,
        if (resolvedTheme.isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    )
    val worldItemResolved = parseComposeColor(
        resolvedTheme.colors.worldItem,
        if (resolvedTheme.isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    )
    val worldLoreResolved = parseComposeColor(
        resolvedTheme.colors.worldLore,
        if (resolvedTheme.isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    )
    val worldEventResolved = parseComposeColor(
        resolvedTheme.colors.worldEvent,
        if (resolvedTheme.isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    )
    val worldRelationshipResolved = parseComposeColor(
        resolvedTheme.colors.worldRelationship,
        if (resolvedTheme.isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
    )

    val selectionResolved = parseComposeColor(resolvedTheme.colors.selection, accentIcons.copy(alpha = 0.3f))

    val surfaceSelectedResolved = if (resolvedTheme.isDark) surfaceRaised.copy(alpha = 0.6f) else accentMutedResolved
    val surfacePressedResolved = surface.copy(alpha = 0.8f)

    val scribeColors = remember(
        resolvedTheme, animBg, animSurfaceLowest, animSurface, animSurfaceRaised,
        animSurfaceOverlay, text, accentIcons, onPrimaryColor, animOutline, animOutlineVariant
    ) {
        ScribeColors(
            surfaces = SurfaceColors(
                background = animBg,
                surfaceLowest = animSurfaceLowest,
                surface = animSurface,
                surfaceRaised = animSurfaceRaised,
                surfaceOverlay = animSurfaceOverlay,
                surfaceSelected = surfaceSelectedResolved,
                surfacePressed = surfacePressedResolved
            ),
            content = ContentColors(
                primary = text,
                secondary = mutedTextResolved,
                tertiary = subtleTextResolved,
                disabled = disabledTextResolved,
                onAccent = onPrimaryColor
            ),
            interaction = InteractionColors(
                primary = accentIcons,
                primaryContainer = accentMutedResolved,
                onPrimary = onPrimaryColor,
                onPrimaryContainer = onPrimaryContainerColor,
                secondary = secondaryColor,
                tertiary = tertiaryColor,
                selection = selectionResolved,
                focus = focusResolved,
                link = linkResolved
            ),
            semantic = SemanticStatusColors(
                success = successResolved,
                onSuccess = onSuccessColor,
                successContainer = successContainerResolved,
                onSuccessContainer = onSuccessContainerColor,

                warning = warningResolved,
                onWarning = onWarningColor,
                warningContainer = warningContainerResolved,
                onWarningContainer = onWarningContainerColor,

                error = errorResolved,
                onError = onErrorColor,
                errorContainer = errorContainerResolved,
                onErrorContainer = onErrorContainerColor,

                info = infoResolved,
                onInfo = onInfoColor,
                infoContainer = infoContainerResolved,
                onInfoContainer = onInfoContainerColor
            ),
            writing = WritingColors(
                prose = text,
                dialogue = dialogueResolved,
                monologue = monologueResolved,
                heading = headingResolved,
                annotation = annotationResolved,
                highlight = highlightResolved
            ),
            analytics = AnalyticsColors(
                positive = analyticsPositiveResolved,
                neutral = analyticsNeutralResolved,
                negative = analyticsNegativeResolved,
                series1 = analyticsSeries1Resolved,
                series2 = analyticsSeries2Resolved,
                series3 = analyticsSeries3Resolved,
                target = analyticsTargetResolved,
                warning = analyticsWarningResolved
            ),
            borders = BorderColors(
                subtle = animOutlineVariant,
                normal = animOutline,
                prominent = borderProminentResolved
            ),
            world = WorldEntityColors(
                character = worldCharacterResolved,
                location = worldLocationResolved,
                faction = worldFactionResolved,
                item = worldItemResolved,
                lore = worldLoreResolved,
                event = worldEventResolved,
                relationship = worldRelationshipResolved
            ),
            isDark = resolvedTheme.isDark
        )
    }

    val scribeShapes = remember { ScribeShapes() }
    val scribeMetrics = remember { ScribeMetrics() }
    val scribeTypography = remember(
        resolvedTheme.fontFamily, resolvedTheme.fontSize, resolvedTheme.lineHeight,
        resolvedTheme.letterSpacing, resolvedTheme.paragraphSpacing, resolvedTheme.textAlignment,
        scribeColors
    ) {
        val resolvedFontFamily = FontHelper.getFontFamily(resolvedTheme.fontFamily)
        val app = ScribeAppTypography(
            display = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.5).sp,
                color = scribeColors.content.primary
            ),
            headline = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.25).sp,
                color = scribeColors.content.primary
            ),
            title = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                color = scribeColors.content.primary
            ),
            sectionTitle = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.15.sp,
                color = scribeColors.content.primary
            ),
            body = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.2.sp,
                color = scribeColors.content.primary
            ),
            bodySecondary = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp,
                color = scribeColors.content.secondary
            ),
            label = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.3.sp,
                color = scribeColors.content.primary
            ),
            caption = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.4.sp,
                color = scribeColors.content.tertiary
            ),
            statValue = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.5).sp,
                color = scribeColors.content.primary
            ),
            statLabel = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.6.sp,
                color = scribeColors.content.secondary
            )
        )
        val editor = ScribeEditorTypography(
            prose = TextStyle(
                fontFamily = resolvedFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = resolvedTheme.fontSize.sp,
                lineHeight = (resolvedTheme.fontSize * resolvedTheme.lineHeight).sp,
                letterSpacing = resolvedTheme.letterSpacing.sp,
                color = scribeColors.writing.prose
            ),
            dialogue = TextStyle(
                fontFamily = resolvedFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = resolvedTheme.fontSize.sp,
                lineHeight = (resolvedTheme.fontSize * resolvedTheme.lineHeight).sp,
                letterSpacing = resolvedTheme.letterSpacing.sp,
                color = scribeColors.writing.dialogue
            ),
            monologue = TextStyle(
                fontFamily = resolvedFontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                fontSize = resolvedTheme.fontSize.sp,
                lineHeight = (resolvedTheme.fontSize * resolvedTheme.lineHeight).sp,
                letterSpacing = resolvedTheme.letterSpacing.sp,
                color = scribeColors.writing.monologue
            ),
            heading = TextStyle(
                fontFamily = resolvedFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (resolvedTheme.fontSize * 1.25f).sp,
                lineHeight = (resolvedTheme.fontSize * 1.25f * resolvedTheme.lineHeight).sp,
                letterSpacing = (resolvedTheme.letterSpacing * 1.1f).sp,
                color = scribeColors.writing.heading
            ),
            fontFamily = resolvedFontFamily,
            fontSize = resolvedTheme.fontSize,
            lineHeight = resolvedTheme.lineHeight,
            letterSpacing = resolvedTheme.letterSpacing,
            paragraphSpacing = resolvedTheme.paragraphSpacing,
            textAlignment = resolvedTheme.textAlignment
        )
        ScribeTypography(
            app = app,
            editor = editor,
            display = app.display,
            headline = app.headline,
            title = app.title,
            sectionTitle = app.sectionTitle,
            body = app.body,
            bodySecondary = app.bodySecondary,
            label = app.label,
            caption = app.caption,
            statValue = app.statValue,
            statLabel = app.statLabel,
            prose = editor.prose,
            dialogue = editor.dialogue,
            monologue = editor.monologue,
            heading = editor.heading
        )
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        shapes = scribeShapes.toMaterialShapes(),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        val w = coords.size.width.toFloat()
                        val h = coords.size.height.toFloat()
                        if (w > 0f && h > 0f && (w != rootDimensions.first || h != rootDimensions.second)) {
                            rootDimensions = Pair(w, h)
                        }
                    }
            ) {
                CompositionLocalProvider(
                    LocalScribeColors provides scribeColors,
                    LocalScribeShapes provides scribeShapes,
                    LocalScribeTypography provides scribeTypography,
                    LocalScribeMetrics provides scribeMetrics,
                    LocalScribeSpacing provides scribeMetrics.spacing,
                    LocalHazeState provides hazeState,
                    LocalAppTheme provides resolvedTheme,
                    LocalBgAnalysisBitmap provides analysisBitmap,
                    LocalBgZonalColors provides activeZonalColors,
                    LocalBgImageDominantColor provides activeDominantColor,
                    LocalScreenSize provides Pair(screenWidthPx, screenHeightPx),
                    LocalRootGeometry provides rootDimensions,
                    LocalInWindowMenuHost provides menuHostState,
                    LocalFrostedGlass provides resolvedTheme.frostedGlassEnabled,
                    LocalFrostedTint provides frostedTintEnabled,
                    LocalFrostedBlurRadius provides frostedBlurRadius,
                    LocalSolidSurface provides animSurface,
                    LocalBarBlurBitmap provides barBlurBitmap,
                    // One-shot bitmap starts null; screens set it via their own
                    // CompositionLocalProvider wrapping the drawer/dialog content.
                    LocalOneShotBitmap provides null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (showWholeAppBg) Color.Transparent else animBg)
                    ) {
                        if (showWholeAppBg) {
                            // Pick the right image model:
                            // • API < 31 + blurred mode → pre-blurred software bitmap
                            // • API < 31 + image mode  → software bitmap (no blur, but
                            //   allowHardware=false so captureOnly can draw the view)
                            // • API 31+ or no pre-processing needed → raw URI (Haze handles blur)
                            val imageModel = when {
                                needsSoftwareBlur && softwareBlurredModel != null -> softwareBlurredModel
                                needsSoftwareImage && softwareImageModel != null -> softwareImageModel
                                else -> bgUri
                            }
                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    // hazeSource registers this image as the layer Haze blurs from.
                                    // On API 31+, hazeEffect on child composables (bars, cards, FABs)
                                    // does all the blurring — the source must be the raw unprocessed
                                    // image. Applying renderEffect here creates an isolated offscreen
                                    // GPU render node that Haze cannot see through, which is why blur
                                    // was broken on API 31+. Pre-API-31 uses a pre-blurred software
                                    // bitmap with blurEnabled = false, so it never needs renderEffect
                                    // here either.
                                    .hazeSource(state = hazeState)
                            )
                            // Apply contrast darkening overlay when overlayEnabled is true and opacity > 0
                            val isOverlayActive = (resolvedTheme.overlayEnabled || (resolvedTheme.overlayColor != null && bgOpacity > 0f)) && bgOpacity > 0f
                            if (isOverlayActive) {
                                val tintBaseColor = resolvedTheme.overlayColor?.let { parseComposeColor(it, bg) } ?: bg
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(tintBaseColor.copy(alpha = bgOpacity))
                                )
                            }
                        }

                        content()

                        // In-Window Anchored Dropdown Menu overlay layer
                        InWindowMenuHost(hostState = menuHostState)
                    }
                }
            }
        }
    )
}
