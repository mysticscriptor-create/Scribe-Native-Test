package com.primaloptima.scribe.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import coil3.BitmapImage
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.primaloptima.scribe.util.DefaultThemes
import com.primaloptima.scribe.util.PrefsManager
import com.primaloptima.scribe.util.ThemeDataStoreRepo
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.graphics.luminance

val LocalHazeState = compositionLocalOf<HazeState?> { null }
val LocalAppTheme = compositionLocalOf<AppTheme?> { null }
val LocalBgAnalysisBitmap = compositionLocalOf<Bitmap?> { null }
val LocalScreenSize = compositionLocalOf { Pair(1080f, 1920f) }
/** True when the user has opted into CPU blur on pre-API-31 devices. */
val LocalLegacyBlur = compositionLocalOf { false }

/**
 * Holds the one-shot blurred screenshot bitmap captured just before a panel/dialog
 * opens on pre-API-31 devices. Set by the screen that owns the drawer/dialog trigger,
 * consumed by [frostedPanel], [frostedCard], [frostedFab], [FrostedDialog].
 * Null when no capture has been taken or when running on API 31+.
 */
val LocalOneShotBitmap = compositionLocalOf<Bitmap?> { null }

/**
 * Always holds the fully-opaque theme surface color, even when a background image
 * is active and the color scheme's surface is set to alpha=0 for glass effects.
 * Use this for Dropdowns, Dialogs, and any popup that must never be see-through.
 */
val LocalSolidSurface = compositionLocalOf { Color.White }

fun autoTextColor(bg: Color): Color {
    val luminance = bg.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun Modifier.frostedBar(hazeState: HazeState?): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = LocalSolidSurface.current
    val oneShotBitmap = LocalOneShotBitmap.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    return if (!hasBgImage) {
        this.background(solidSurface)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this.hazeEffect(state = hazeState, style = HazeMaterials.thin())
    } else if (oneShotBitmap != null) {
        // Pre-API-31: draw the one-shot blurred capture as the bar background
        this.drawWithOneShotBitmap(oneShotBitmap, solidSurface.copy(alpha = 0.3f))
    } else {
        this.background(solidSurface.copy(alpha = 0.82f))
    }
}

/**
 * Applies a frosted-glass effect to a FAB (or any component) when a whole-app
 * background image is active.  On Android 12+ this is a real GPU blur via Haze;
 * on older devices it falls back to a semi-transparent surface tint.
 * When there is no background image the modifier is a no-op.
 */
@Composable
fun Modifier.frostedFab(hazeState: HazeState?): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = LocalSolidSurface.current
    val oneShotBitmap = LocalOneShotBitmap.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    return if (!hasBgImage) {
        this
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this
            .clip(androidx.compose.foundation.shape.CircleShape)
            .hazeEffect(state = hazeState, style = HazeMaterials.regular())
    } else if (oneShotBitmap != null) {
        this
            .clip(androidx.compose.foundation.shape.CircleShape)
            .drawWithOneShotBitmap(oneShotBitmap, solidSurface.copy(alpha = 0.3f))
    } else {
        this.background(solidSurface.copy(alpha = 0.75f), shape = androidx.compose.foundation.shape.CircleShape)
    }
}

/**
 * Applies a frosted-glass effect to side panels, navigation drawers, and any
 * overlay that should feel "elevated glass" over the content behind it.
 *
 * When a background image is active:
 *   - API 31+ : real GPU blur via Haze (hazeChild with a regular material)
 *   - API < 31: high-opacity solid surface tint using the theme's actual surface
 *               colour so the fallback blends naturally on every theme
 *
 * When there is no background image the modifier is a no-op (the drawer's own
 * containerColor provides the background).
 *
 * Usage: Set `drawerContainerColor = Color.Transparent` on the drawer/sheet
 * and add `.frostedPanel(hazeState)` to its `modifier`.
 */
@Composable
fun Modifier.frostedPanel(hazeState: HazeState?): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = LocalSolidSurface.current
    val oneShotBitmap = LocalOneShotBitmap.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    return if (!hasBgImage) {
        this.background(solidSurface)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null) {
        this.hazeEffect(state = hazeState, style = HazeMaterials.regular())
    } else if (oneShotBitmap != null) {
        this.drawWithOneShotBitmap(oneShotBitmap, solidSurface.copy(alpha = 0.25f))
    } else {
        this.background(solidSurface.copy(alpha = 0.95f))
    }
}

/**
 * Frosted glass for Card composables (ElevatedCard, Card, etc).
 * When a background image is active this clips to [shape] and applies hazeEffect.
 * When no background image is active it returns the card's normal solid surface color,
 * so on plain-color themes the cards look identical to before.
 *
 * Usage:
 *   ElevatedCard(
 *       colors = CardDefaults.elevatedCardColors(
 *           containerColor = if (hasBgImage) Color.Transparent else surface.copy(alpha = 0.92f)
 *       ),
 *       modifier = Modifier.frostedCard(hazeState)
 *   )
 */
@Composable
fun Modifier.frostedCard(
    hazeState: HazeState?,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    solidAlpha: Float = 0.92f,
    /** When true, applies clip+solid background even when no bg image is active.
     *  Use for plain Box/Container callers that don't have their own background. */
    applyFallbackBackground: Boolean = false
): Modifier {
    val theme = LocalAppTheme.current
    val solidSurface = LocalSolidSurface.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    val oneShotBitmap = LocalOneShotBitmap.current
    return if (!hasBgImage || hazeState == null) {
        this
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeMaterials.regular())
    } else if (oneShotBitmap != null) {
        this
            .clip(shape)
            .drawWithOneShotBitmap(oneShotBitmap, solidSurface.copy(alpha = 0.25f))
    } else {
        this
            .clip(shape)
            .background(solidSurface.copy(alpha = solidAlpha))
    }
}

/**
 * A dialog that lives in the same window as the rest of the UI, so Haze blur works correctly.
 * Standard AlertDialog creates a separate Android window which breaks hazeEffect.
 *
 * Usage: replace AlertDialog with FrostedDialog. The API mirrors AlertDialog.
 *
 * When no background image is active the dialog looks identical to a standard M3 AlertDialog
 * because it uses the solid surface color. When a background image IS active, the dialog
 * surface itself gets the frosted blur via hazeEffect.
 */
@Composable
fun FrostedDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    val hazeState = LocalHazeState.current
    val solidSurface = LocalSolidSurface.current
    val oneShotBitmap = LocalOneShotBitmap.current
    val theme = LocalAppTheme.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        val containerModifier = when {
            hasBgImage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null ->
                Modifier.clip(shape).hazeEffect(state = hazeState, style = HazeMaterials.regular())
            hasBgImage && oneShotBitmap != null ->
                Modifier.clip(shape).drawWithOneShotBitmap(oneShotBitmap, solidSurface.copy(alpha = 0.25f))
            else ->
                Modifier.background(solidSurface, shape = shape)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .then(containerModifier)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume so taps inside don't dismiss */ }
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            title?.let {
                androidx.compose.runtime.CompositionLocalProvider {
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                    ) { it() }
                }
            }
            text?.let {
                androidx.compose.runtime.CompositionLocalProvider {
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    ) { it() }
                }
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

/**
 * Returns the correct containerColor for a Card or FAB when frosted glass is active.
 * When a background image is set and blur is allowed, returns [Color.Transparent] so
 * hazeEffect shows through. Otherwise returns [fallback].
 *
 * Usage:
 *   ElevatedCard(
 *       colors = CardDefaults.elevatedCardColors(
 *           containerColor = frostedContainerColor(fallback = surface.copy(alpha = 0.92f))
 *       ),
 *       modifier = Modifier.frostedCard(hazeState)
 *   )
 */
@Composable
fun frostedContainerColor(fallback: Color): Color {
    val theme = LocalAppTheme.current
    val hazeState = LocalHazeState.current
    val oneShotBitmap = LocalOneShotBitmap.current
    val hasBgImage = theme?.backgroundImageUri?.isNotEmpty() == true &&
            (theme.bgMode == "image" || theme.bgMode == "blurred")
    // Transparent so the frosted modifier (hazeEffect or one-shot bitmap) shows through
    val legacyReady = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && oneShotBitmap != null
    val modernReady = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hazeState != null
    return if (hasBgImage && (modernReady || legacyReady)) Color.Transparent else fallback
}

/**
 * Draws the [bitmap] as a tiled/stretched background behind this composable,
 * then overlays [tint] on top to achieve the frosted glass look.
 * The bitmap is the one-shot blurred screen capture taken just before the
 * panel/dialog opened, so it shows the actual UI content blurred behind it.
 */
fun Modifier.drawWithOneShotBitmap(bitmap: Bitmap, tint: Color): Modifier =
    this.drawWithContent {
        // Draw the blurred capture scaled to fill this composable
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isFilterBitmap = true
            }
            canvas.nativeCanvas.drawBitmap(
                bitmap,
                null,
                android.graphics.RectF(0f, 0f, size.width, size.height),
                paint
            )
        }
        // Tint overlay — gives the surface colour bleed that makes it feel glassy
        drawRect(tint)
        // Draw the composable's own content on top (text, icons etc.)
        drawContent()
    }

fun parseComposeColor(hex: String, fallback: Color = Color.Black): Color {
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
    val themeManager = remember { ThemeManager(context) }
    val resolvedTheme = if (appTheme != null) {
        appTheme
    } else {
        val repo = remember { ThemeDataStoreRepo(context) }
        val activeThemeId by repo.activeThemeIdFlow.collectAsState(
            initial = themeManager.activeTheme().id
        )
        val customThemesJson by repo.customThemesJsonFlow.collectAsState(initial = "[]")
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
    var analysisBitmap by remember(bgUri, hasBgImage) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bgUri, hasBgImage) {
        if (!hasBgImage || bgUri.isNullOrEmpty()) {
            analysisBitmap = null
            return@LaunchedEffect
        }
        analysisBitmap = withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(bgUri)
                    .size(32, 32)
                    .allowHardware(false) // Prevents hardware bitmap; getPixel() requires software config
                    .build()
                (ImageLoader(context).execute(request).image as? BitmapImage)?.bitmap
            } catch (_: Exception) {
                null
            }
        }
    }

    val bg = parseComposeColor(resolvedTheme.colors.background, Color(0xFFFAFAF7))
    val surface = parseComposeColor(resolvedTheme.colors.surface, Color.White)
    val configuredText = parseComposeColor(resolvedTheme.colors.text, Color(0xFF1A1A1A))
    val configuredAccent = parseComposeColor(resolvedTheme.colors.accent, Color(0xFF333333))
    val border = parseComposeColor(resolvedTheme.colors.border, Color(0xFFE0E0D8))
    val surfaceVariant = parseComposeColor(resolvedTheme.colors.surface, surface)

    val isLight = !resolvedTheme.isDark
    val defaultText = if (resolvedTheme.isDark) Color.White else Color(0xFF1A1A1A)
    val defaultAccent = if (resolvedTheme.isDark) Color(0xFFE0E0E0) else Color(0xFF333333)
    val imageContrast = analysisBitmap?.let {
        contrastingTextColor(
            bitmap = it,
            screenRect = Rect(0f, 0f, screenWidthPx, screenHeightPx),
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx
        )
    }
    val text = if (hasBgImage && configuredText == defaultText && imageContrast != null) imageContrast else configuredText
    val accentIcons = if (hasBgImage && configuredAccent == defaultAccent && imageContrast != null) imageContrast else configuredAccent
    val onPrimaryColor = if (accentIcons.luminance() < 0.5f) Color.White else Color.Black

    val rawColorScheme: ColorScheme = if (isLight) {
        lightColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = surface,
            onPrimaryContainer = text,
            secondary = accentIcons,
            onSecondary = onPrimaryColor,
            // KEY: secondaryContainer was unset → M3 default is purple(#E8DEF8)
            // Setting it to surfaceVariant gives a themed, warm tint instead.
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = accentIcons,
            onTertiary = onPrimaryColor,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = text,
            surfaceContainerLowest = bg,
            surfaceContainerLow = bg,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            // KEY: surfaceContainerHighest was unset → M3 default is lavender(#E6E0E9)
            // Card() in BOM 2026.06.00 uses this slot by default.
            surfaceContainerHighest = surfaceVariant,
            // Keep tonal surface tint on-theme (prevents extra purple tinting)
            surfaceTint = accentIcons,
            inverseSurface = text,
            inverseOnSurface = bg,
            inversePrimary = accentIcons,
            outline = border,
            outlineVariant = border,
            scrim = Color.Black.copy(alpha = 0.32f)
        )
    } else {
        darkColorScheme(
            primary = accentIcons,
            onPrimary = onPrimaryColor,
            primaryContainer = surface,
            onPrimaryContainer = text,
            secondary = accentIcons,
            onSecondary = onPrimaryColor,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = text,
            tertiary = accentIcons,
            onTertiary = onPrimaryColor,
            tertiaryContainer = surfaceVariant,
            onTertiaryContainer = text,
            background = bg,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = text,
            surfaceContainerLowest = bg,
            surfaceContainerLow = bg,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surfaceVariant,
            surfaceTint = accentIcons,
            inverseSurface = text,
            inverseOnSurface = bg,
            inversePrimary = accentIcons,
            outline = border,
            outlineVariant = border,
            scrim = Color.Black.copy(alpha = 0.32f)
        )
    }

    val duration = 400
    val animSpec = tween<Color>(durationMillis = duration)

    val animPrimary by animateColorAsState(rawColorScheme.primary, animSpec, label = "primary")
    val animOnPrimary by animateColorAsState(rawColorScheme.onPrimary, animSpec, label = "onPrimary")
    val animBg by animateColorAsState(rawColorScheme.background, animSpec, label = "bg")
    val animOnBg by animateColorAsState(rawColorScheme.onBackground, animSpec, label = "onBg")
    val animSurface by animateColorAsState(rawColorScheme.surface, animSpec, label = "surface")
    val animOnSurface by animateColorAsState(rawColorScheme.onSurface, animSpec, label = "onSurface")
    val animSurfaceVariant by animateColorAsState(rawColorScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val animOnSurfaceVariant by animateColorAsState(rawColorScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
    val animOutline by animateColorAsState(rawColorScheme.outline, animSpec, label = "outline")

    val showWholeAppBg = resolvedTheme.themeScope == "whole_app" && hasBgImage

    // When a whole-app background image is active, surfaces must be transparent so
    // the image shows through and the Haze blur effect works. However, we must NOT
    // use Color.Transparent (= ARGB 0,0,0,0 — transparent BLACK) because any
    // downstream call like surface.copy(alpha = 0.95f) would produce a near-opaque
    // BLACK instead of the theme colour. Instead, we zero only the alpha channel
    // while keeping the RGB channels intact, so copy(alpha = X) restores the
    // correct colour at the requested opacity.
    val glassySurface        = if (showWholeAppBg) animSurface.copy(alpha = 0f)        else animSurface
    val glassySurfaceVariant = if (showWholeAppBg) animSurfaceVariant.copy(alpha = 0f) else animSurfaceVariant
    val glassyBg             = if (showWholeAppBg) animBg.copy(alpha = 0f)             else animBg

    val animatedColorScheme = rawColorScheme.copy(
        primary = animPrimary,
        onPrimary = animOnPrimary,
        primaryContainer = glassySurface,
        onPrimaryContainer = animOnSurface,
        secondary = animPrimary,
        onSecondary = animOnPrimary,
        secondaryContainer = glassySurfaceVariant,
        onSecondaryContainer = animOnSurface,
        tertiary = animPrimary,
        onTertiary = animOnPrimary,
        tertiaryContainer = glassySurfaceVariant,
        onTertiaryContainer = animOnSurface,
        background = glassyBg,
        onBackground = animOnBg,
        surface = glassySurface,
        onSurface = animOnSurface,
        surfaceVariant = glassySurfaceVariant,
        onSurfaceVariant = animOnSurfaceVariant,
        surfaceContainerLowest = glassyBg,
        surfaceContainerLow = glassyBg,
        surfaceContainer = glassySurface,
        surfaceContainerHigh = glassySurface,
        surfaceContainerHighest = glassySurfaceVariant,
        outline = animOutline,
        outlineVariant = animOutline
    )

    val window = (LocalContext.current as? Activity)?.window
    SideEffect {
        window?.let { win ->
            val barColor = (if (showWholeAppBg) Color.Transparent else animSurface).toArgb()
            win.statusBarColor = barColor
            win.navigationBarColor = barColor
            WindowCompat.getInsetsController(win, win.decorView).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    val hazeState = rememberHazeState(blurEnabled = true)
    val prefsManager = remember { PrefsManager(context) }
    var legacyBlurEnabled by remember { mutableStateOf(prefsManager.legacyBlurEnabled) }

    // Re-read the pref every time this composable re-enters the composition
    // (e.g. returning from ThemeEditScreen where the toggle lives).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                legacyBlurEnabled = prefsManager.legacyBlurEnabled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        content = {
            CompositionLocalProvider(
                LocalHazeState provides hazeState,
                LocalAppTheme provides resolvedTheme,
                LocalBgAnalysisBitmap provides analysisBitmap,
                LocalScreenSize provides Pair(screenWidthPx, screenHeightPx),
                LocalLegacyBlur provides legacyBlurEnabled,
                LocalSolidSurface provides animSurface,
                // One-shot bitmap starts null; screens set it via their own
                // CompositionLocalProvider wrapping the drawer/dialog content.
                LocalOneShotBitmap provides null
            ) {
                val bgOpacity = resolvedTheme.backgroundImageOpacity ?: 0.35f
                val bgMode = resolvedTheme.bgMode
                val blurIntensity = resolvedTheme.blurIntensity

                // On API < 31 we can't use RenderEffect on a live composable, so we
                // On API < 31 we pre-blur the source bitmap once using pure Kotlin
                // stack blur and display that pre-blurred bitmap instead.
                val needsSoftwareBlur = bgMode == "blurred" &&
                        blurIntensity > 0f &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                        bgUri != null

                val softwareBlurredModel by produceState<android.graphics.Bitmap?>(
                    initialValue = null,
                    key1 = bgUri,
                    key2 = blurIntensity,
                    key3 = needsSoftwareBlur
                ) {
                    if (!needsSoftwareBlur || bgUri == null) {
                        value = null
                        return@produceState
                    }
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val loader = coil3.ImageLoader(context)
                            val req = coil3.request.ImageRequest.Builder(context)
                                .data(bgUri)
                                .size(coil3.size.Size(800, 800))
                                .allowHardware(false)
                                .build()
                            val result = loader.execute(req)
                            val bmp = (result as? coil3.request.SuccessResult)
                                ?.image
                                ?.let { (it as? coil3.BitmapImage)?.bitmap }
                            bmp?.let {
                                val radiusPx = (blurIntensity * 0.8f).toInt().coerceIn(1, 25)
                                com.primaloptima.scribe.util.BitmapBlur.blurBitmap(it, radiusPx)
                            }
                        } catch (_: Exception) { null }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (showWholeAppBg) Color.Transparent else animBg)
                ) {
                    if (showWholeAppBg) {
                        // Display either the pre-blurred bitmap (API < 31) or the
                        // live image with RenderEffect (API 31+)
                        val imageModel = if (needsSoftwareBlur && softwareBlurredModel != null) {
                            softwareBlurredModel
                        } else {
                            bgUri
                        }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState)
                                .then(
                                    if (bgMode == "blurred" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurIntensity > 0f) {
                                        Modifier.graphicsLayer {
                                            val radiusPx = blurIntensity * density
                                            if (radiusPx > 0f) {
                                                renderEffect = AndroidRenderEffect
                                                    .createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                    } else Modifier
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bg.copy(alpha = bgOpacity))
                        )
                    }

                    content()
                }
            }
        }
    )
}
