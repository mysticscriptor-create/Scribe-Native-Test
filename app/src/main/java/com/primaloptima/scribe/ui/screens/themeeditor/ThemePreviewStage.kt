package com.primaloptima.scribe.ui.screens.themeeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.primaloptima.scribe.ui.components.ScribeThemeLivePreview
import com.primaloptima.scribe.util.model.ThemeColors

/**
 * Anchored live preview stage demonstrating canonical resolved theme colors and typography in real-time.
 *
 * Delegates directly to the canonical [ScribeThemeLivePreview] system, rendering real Scribe UI
 * composables (Dashboard + Editor) within simulated device frames.
 */
@Composable
fun ThemePreviewStage(
    colors: ThemeColors,
    themeName: String,
    fontFamily: String,
    fontSize: Float,
    lineHeight: Float,
    textAlignment: String,
    sideMargins: Float,
    bgMode: String,
    bgUri: String?,
    bgOpacity: Float,
    overlayEnabled: Boolean = false,
    overlayColor: String? = null,
    blurIntensity: Float,
    isDark: Boolean = false,
    selectedOrnamentId: String = "classic_flourish",
    modifier: Modifier = Modifier
) {
    ScribeThemeLivePreview(
        colors = colors,
        themeName = themeName,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        textAlignment = textAlignment,
        sideMargins = sideMargins,
        bgMode = bgMode,
        bgUri = bgUri,
        bgOpacity = bgOpacity,
        overlayEnabled = overlayEnabled,
        overlayColor = overlayColor,
        blurIntensity = blurIntensity,
        isDark = isDark,
        selectedOrnamentId = selectedOrnamentId,
        modifier = modifier
    )
}
