package com.primaloptima.scribe.ui.screens.themeeditor

import androidx.compose.runtime.Immutable
import com.primaloptima.scribe.util.ThemeGenerationEngine
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ImageInfluence
import com.primaloptima.scribe.util.model.ImageUnderstanding
import com.primaloptima.scribe.util.model.ThemeColorOverrides
import com.primaloptima.scribe.util.model.ThemeColors
import com.primaloptima.scribe.util.model.ThemeGenerationRecipe
import com.primaloptima.scribe.util.model.ThemeRelationshipMode
import com.primaloptima.scribe.util.model.ThemeSchema
import com.primaloptima.scribe.util.model.ThemeSourcePalette
import com.primaloptima.scribe.util.model.VisualThemePalette
import com.primaloptima.scribe.util.model.WritingCharacter

/**
 * High-level category tabs for the decomposed Theme Editor.
 * Re-architected in Phase 19 into 4 intuitive writing-first pillars.
 */
enum class ThemeEditorCategory(val title: String) {
    APPEARANCE("Appearance"),
    WRITING("Writing"),
    ATMOSPHERE("Atmosphere"),
    ADVANCED("More");

    companion object {
        @Deprecated("Use APPEARANCE", ReplaceWith("APPEARANCE"))
        val COLORS = APPEARANCE
        @Deprecated("Use WRITING", ReplaceWith("WRITING"))
        val TYPOGRAPHY = WRITING
        @Deprecated("Use ADVANCED", ReplaceWith("ADVANCED"))
        val LAYOUT = ADVANCED
    }
}

/**
 * Identifies which color token is currently targeted by the Color Picker dialog.
 */
enum class ColorPickerTarget {
    // Foundation Sources
    BACKGROUND,
    TEXT,
    ACCENT,

    // Writing Overrides
    HEADING_TEXT,
    DIALOGUE_TEXT,
    MONOLOGUE_TEXT,
    SPECIAL_HIGHLIGHT,
    ANNOTATION,

    // Supporting Accent Overrides
    SECONDARY,
    TERTIARY,

    // System Status Indicators Overrides
    SUCCESS,
    WARNING,
    ERROR,

    // Surface Overrides
    SURFACE,

    // Overlay Tint Override
    OVERLAY
}

/**
 * Authoritative draft state representation for the Theme Editor session.
 * Encapsulates the editing lifecycle: Persisted Theme -> Editor Draft -> Resolve -> Preview -> Save.
 */
@Immutable
data class ThemeEditorDraft(
    val id: String,
    val builtIn: Boolean,
    val isDark: Boolean,
    val name: String,
    val emoji: String,

    // Foundation Sources (Authoritative driving inputs)
    val bgHex: String,
    val textHex: String,
    val accentHex: String,

    // Explicit User Overrides (null means token resolves to generated default)
    val overrides: ThemeColorOverrides? = null,

    // Typography
    val fontFamily: String,
    val fontSize: Float,
    val lineHeight: Float,
    val paragraphSpacing: Float,
    val sideMargins: Float,

    // Layout
    val textAlignment: String,
    val themeScope: String,

    // Atmosphere & Background
    val bgMode: String,
    val bgUri: String?,
    val bgOriginalUri: String?,
    val bgOpacity: Float,
    val overlayEnabled: Boolean = false,
    val overlayColor: String? = null,
    val blurIntensity: Float,
    val frostedGlassEnabled: Boolean,
    val frostedTintEnabled: Boolean,
    val frostedBlurRadius: Float,

    // Image Background Analysis
    val bgLuminance: Float,
    val zonalLuminanceMatrix: List<Float> = emptyList(),
    val zonalVarianceMatrix: List<Float> = emptyList(),
    val bgDominantColor: String? = null,
    val zonalColorsMatrix: List<String> = emptyList(),
    val luminanceFieldMatrix: List<Float> = emptyList(),

    // Phase 18: Intelligent Image Generation State
    val extractedCandidates: List<Int> = emptyList(),
    val activeCandidate: Int? = null,
    val activeRecipe: ThemeGenerationRecipe = ThemeGenerationRecipe.BALANCED,
    val activeInfluence: ImageInfluence = ImageInfluence.BALANCED,
    val activeWritingCharacter: WritingCharacter = WritingCharacter.NEUTRAL,
    val activeUnderstanding: ImageUnderstanding? = null,

    // Phase 20: Intelligent Multi-Color Palette Sources
    val secondaryHex: String? = null,
    val tertiaryHex: String? = null,
    val atmosphericHex: String? = null,
    val visualPalette: VisualThemePalette? = null
) {
    /**
     * Resolves the canonical active ThemeColors by layering overrides onto generated defaults.
     */
    fun resolveColors(): ThemeColors {
        return ThemeManager.resolveThemeColors(
            sources = ThemeSourcePalette(
                background = bgHex,
                text = textHex,
                accent = accentHex,
                secondaryAccent = secondaryHex,
                tertiaryAccent = tertiaryHex,
                atmosphericColor = atmosphericHex,
                visualPalette = visualPalette
            ),
            overrides = overrides,
            isDark = ThemeManager.isDarkColor(bgHex)
        )
    }

    /**
     * Generates pure algorithmic defaults for comparison and reset previews.
     */
    fun generateDefaults(): ThemeColors {
        return ThemeManager.generateThemeDefaults(
            sources = ThemeSourcePalette(
                background = bgHex,
                text = textHex,
                accent = accentHex,
                secondaryAccent = secondaryHex,
                tertiaryAccent = tertiaryHex,
                atmosphericColor = atmosphericHex,
                visualPalette = visualPalette
            ),
            isDark = ThemeManager.isDarkColor(bgHex)
        )
    }

    /**
     * Checks whether a specific semantic color token is currently overridden.
     */
    fun isOverridden(target: ColorPickerTarget): Boolean {
        if (overrides == null) return false
        return when (target) {
            ColorPickerTarget.HEADING_TEXT -> overrides.headingText != null
            ColorPickerTarget.DIALOGUE_TEXT -> overrides.dialogueText != null
            ColorPickerTarget.MONOLOGUE_TEXT -> overrides.monologueText != null
            ColorPickerTarget.SPECIAL_HIGHLIGHT -> overrides.specialHighlight != null
            ColorPickerTarget.ANNOTATION -> overrides.annotation != null
            ColorPickerTarget.SECONDARY -> overrides.secondary != null
            ColorPickerTarget.TERTIARY -> overrides.tertiary != null
            ColorPickerTarget.SUCCESS -> overrides.success != null
            ColorPickerTarget.WARNING -> overrides.warning != null
            ColorPickerTarget.ERROR -> overrides.error != null
            ColorPickerTarget.SURFACE -> overrides.surface != null
            else -> false
        }
    }

    /**
     * Sets an explicit override for the given semantic target.
     */
    fun withOverride(target: ColorPickerTarget, hex: String): ThemeEditorDraft {
        val current = overrides ?: ThemeColorOverrides()
        val updatedOverrides = when (target) {
            ColorPickerTarget.HEADING_TEXT -> current.copy(headingText = hex)
            ColorPickerTarget.DIALOGUE_TEXT -> current.copy(dialogueText = hex)
            ColorPickerTarget.MONOLOGUE_TEXT -> current.copy(monologueText = hex)
            ColorPickerTarget.SPECIAL_HIGHLIGHT -> current.copy(specialHighlight = hex)
            ColorPickerTarget.ANNOTATION -> current.copy(annotation = hex)
            ColorPickerTarget.SECONDARY -> current.copy(secondary = hex)
            ColorPickerTarget.TERTIARY -> current.copy(tertiary = hex)
            ColorPickerTarget.SUCCESS -> current.copy(success = hex)
            ColorPickerTarget.WARNING -> current.copy(warning = hex)
            ColorPickerTarget.ERROR -> current.copy(error = hex)
            ColorPickerTarget.SURFACE -> current.copy(surface = hex)
            else -> current
        }
        return copy(overrides = updatedOverrides)
    }

    /**
     * Clears an override for the given semantic target, returning it to the generated default.
     */
    fun withClearedOverride(target: ColorPickerTarget): ThemeEditorDraft {
        if (overrides == null) return this
        val updated = when (target) {
            ColorPickerTarget.HEADING_TEXT -> overrides.copy(headingText = null)
            ColorPickerTarget.DIALOGUE_TEXT -> overrides.copy(dialogueText = null)
            ColorPickerTarget.MONOLOGUE_TEXT -> overrides.copy(monologueText = null)
            ColorPickerTarget.SPECIAL_HIGHLIGHT -> overrides.copy(specialHighlight = null)
            ColorPickerTarget.ANNOTATION -> overrides.copy(annotation = null)
            ColorPickerTarget.SECONDARY -> overrides.copy(secondary = null)
            ColorPickerTarget.TERTIARY -> overrides.copy(tertiary = null)
            ColorPickerTarget.SUCCESS -> overrides.copy(success = null)
            ColorPickerTarget.WARNING -> overrides.copy(warning = null)
            ColorPickerTarget.ERROR -> overrides.copy(error = null)
            ColorPickerTarget.SURFACE -> overrides.copy(surface = null)
            else -> overrides
        }
        return copy(overrides = if (updated.isEmpty()) null else updated)
    }

    /**
     * Clears all explicit overrides, restoring the theme entirely to algorithmic OKLCH defaults.
     */
    fun withResetAllOverrides(): ThemeEditorDraft = copy(overrides = null)

    /**
     * Checks if the user has modified any aspect of the theme during the current editing session.
     */
    fun isDirty(original: AppTheme): Boolean {
        if (name != original.name) return true
        if (emoji != (original.emoji ?: "🖊️")) return true
        if (bgHex != original.colors.background) return true
        if (textHex != original.colors.text) return true
        if (accentHex != original.colors.accent) return true
        if (overrides != original.overrides) return true
        if (fontFamily != original.fontFamily) return true
        if (fontSize.toInt() != original.fontSize) return true
        if (lineHeight != original.lineHeight) return true
        if (paragraphSpacing.toInt() != original.paragraphSpacing) return true
        if (sideMargins.toInt() != original.paddingHorizontal) return true
        if (textAlignment != original.textAlignment) return true
        if (themeScope != original.themeScope) return true
        if (bgMode != original.bgMode) return true
        if (bgUri != original.backgroundImageUri) return true
        if (bgOpacity != (original.backgroundImageOpacity ?: 0.35f)) return true
        if (overlayEnabled != original.overlayEnabled) return true
        if (overlayColor != original.overlayColor) return true
        if (blurIntensity != original.blurIntensity) return true
        if (frostedGlassEnabled != original.frostedGlassEnabled) return true
        if (frostedTintEnabled != original.frostedTintEnabled) return true
        if (frostedBlurRadius != original.frostedBlurRadius) return true
        return false
    }

    /**
     * Updates foundation driving sources from an algorithmic [ThemeSourcePalette].
     * Preserves existing explicit overrides unless [resetOverrides] is true.
     */
    fun withFoundationPalette(palette: ThemeSourcePalette, resetOverrides: Boolean = false): ThemeEditorDraft {
        return copy(
            bgHex = palette.background,
            textHex = palette.text,
            accentHex = palette.accent,
            secondaryHex = palette.secondaryAccent,
            tertiaryHex = palette.tertiaryAccent,
            atmosphericHex = palette.atmosphericColor,
            visualPalette = palette.visualPalette,
            overrides = if (resetOverrides) null else overrides
        )
    }

    /**
     * Applies image-generated colors from ranked color ints (or dominant color) into foundation sources.
     * Preserves existing explicit user overrides, strictly respecting the unidirectional pipeline.
     */
    fun withImageGeneratedPalette(rankedColors: List<Int>, resetOverrides: Boolean = false): ThemeEditorDraft {
        val dark = ThemeManager.isDarkColor(bgHex)
        val palette = ThemeGenerationEngine.generateSourcePalette(rankedColors, dark)
        return withFoundationPalette(palette, resetOverrides).copy(extractedCandidates = rankedColors)
    }

    /**
     * Applies an [ImageUnderstanding] directly into foundation sources using the chosen [recipe],
     * [candidateColor], [influence], and [writingCharacter].
     * Changing any recipe parameter recomputes the foundation palette instantly without re-running quantization.
     */
    fun withImageUnderstanding(
        understanding: ImageUnderstanding,
        recipe: ThemeGenerationRecipe = activeRecipe,
        candidateColor: Int? = null,
        influence: ImageInfluence = activeInfluence,
        writingCharacter: WritingCharacter = activeWritingCharacter,
        resetOverrides: Boolean = false
    ): ThemeEditorDraft {
        val dark = ThemeManager.isDarkColor(bgHex)
        val chosenCandidate = candidateColor ?: activeCandidate ?: understanding.rankedCandidates.firstOrNull()
        val palette = ThemeGenerationEngine.generateSourcePalette(
            understanding = understanding,
            recipe = recipe,
            candidateColor = chosenCandidate,
            isDark = dark,
            influence = influence,
            writingCharacter = writingCharacter
        )
        return copy(
            bgHex = palette.background,
            textHex = palette.text,
            accentHex = palette.accent,
            secondaryHex = palette.secondaryAccent,
            tertiaryHex = palette.tertiaryAccent,
            atmosphericHex = palette.atmosphericColor,
            visualPalette = palette.visualPalette,
            overrides = if (resetOverrides) null else overrides,
            extractedCandidates = understanding.rankedCandidates,
            activeCandidate = chosenCandidate,
            activeRecipe = recipe,
            activeInfluence = influence,
            activeWritingCharacter = writingCharacter,
            activeUnderstanding = understanding
        )
    }

    /**
     * Switches the active seed color candidate from the existing [ImageUnderstanding] candidates list.
     * Reuses understanding and re-generates downstream tokens without re-quantizing.
     */
    fun withCandidateSelection(candidateColor: Int, resetOverrides: Boolean = false): ThemeEditorDraft {
        val understanding = activeUnderstanding
        return if (understanding != null) {
            withImageUnderstanding(
                understanding = understanding,
                recipe = activeRecipe,
                candidateColor = candidateColor,
                influence = activeInfluence,
                writingCharacter = activeWritingCharacter,
                resetOverrides = resetOverrides
            )
        } else {
            val dark = ThemeManager.isDarkColor(bgHex)
            val palette = ThemeGenerationEngine.generateSourcePaletteFromSeed(candidateColor, dark)
            withFoundationPalette(palette, resetOverrides).copy(activeCandidate = candidateColor)
        }
    }

    /**
     * Switches the active theme recipe (Balanced, Atmospheric, Ink, Expressive).
     */
    fun withRecipe(recipe: ThemeGenerationRecipe, resetOverrides: Boolean = false): ThemeEditorDraft {
        val understanding = activeUnderstanding ?: return copy(activeRecipe = recipe)
        return withImageUnderstanding(
            understanding = understanding,
            recipe = recipe,
            candidateColor = activeCandidate,
            influence = activeInfluence,
            writingCharacter = activeWritingCharacter,
            resetOverrides = resetOverrides
        )
    }

    /**
     * Switches image influence (Subtle, Balanced, Strong).
     */
    fun withInfluence(influence: ImageInfluence, resetOverrides: Boolean = false): ThemeEditorDraft {
        val understanding = activeUnderstanding ?: return copy(activeInfluence = influence)
        return withImageUnderstanding(
            understanding = understanding,
            recipe = activeRecipe,
            candidateColor = activeCandidate,
            influence = influence,
            writingCharacter = activeWritingCharacter,
            resetOverrides = resetOverrides
        )
    }

    /**
     * Switches writing character (Neutral, Warm, Cool, Dramatic).
     */
    fun withWritingCharacter(character: WritingCharacter, resetOverrides: Boolean = false): ThemeEditorDraft {
        val understanding = activeUnderstanding ?: return copy(activeWritingCharacter = character)
        return withImageUnderstanding(
            understanding = understanding,
            recipe = activeRecipe,
            candidateColor = activeCandidate,
            influence = activeInfluence,
            writingCharacter = character,
            resetOverrides = resetOverrides
        )
    }

    /**
     * Re-derives the foundation palette for light/dark mode without re-quantizing.
     */
    fun withPolarity(isDark: Boolean, resetOverrides: Boolean = false): ThemeEditorDraft {
        val understanding = activeUnderstanding
        return if (understanding != null) {
            val chosenCandidate = activeCandidate ?: understanding.rankedCandidates.firstOrNull()
            val palette = ThemeGenerationEngine.generateSourcePalette(
                understanding = understanding,
                recipe = activeRecipe,
                candidateColor = chosenCandidate,
                isDark = isDark,
                influence = activeInfluence,
                writingCharacter = activeWritingCharacter
            )
            copy(
                bgHex = palette.background,
                textHex = palette.text,
                accentHex = palette.accent,
                secondaryHex = palette.secondaryAccent,
                tertiaryHex = palette.tertiaryAccent,
                atmosphericHex = palette.atmosphericColor,
                visualPalette = palette.visualPalette,
                overrides = if (resetOverrides) null else overrides
            )
        } else {
            val currentSeed = activeCandidate ?: try { ThemeManager.parseColor(accentHex) } catch (_: Exception) { 0xFF3B82F6.toInt() }
            val palette = ThemeGenerationEngine.generateSourcePaletteFromSeed(currentSeed, isDark)
            withFoundationPalette(palette, resetOverrides)
        }
    }

    /**
     * Applies the high-level presentation relationship mode without altering color generation.
     */
    fun withRelationshipMode(mode: ThemeRelationshipMode): ThemeEditorDraft {
        return when (mode) {
            ThemeRelationshipMode.THEME_ONLY -> copy(
                bgMode = "color",
                frostedGlassEnabled = false
            )
            ThemeRelationshipMode.THEME_IMAGE -> copy(
                bgMode = if (bgMode == "color") "image" else bgMode,
                frostedGlassEnabled = false
            )
            ThemeRelationshipMode.THEME_GLASS -> copy(
                bgMode = if (bgMode == "color") "image" else bgMode,
                frostedGlassEnabled = true
            )
        }
    }

    /**
     * Inspects the current presentation settings to infer the active [ThemeRelationshipMode].
     */
    fun getRelationshipMode(): ThemeRelationshipMode {
        return when {
            frostedGlassEnabled && bgMode != "color" -> ThemeRelationshipMode.THEME_GLASS
            bgMode != "color" && !bgUri.isNullOrBlank() -> ThemeRelationshipMode.THEME_IMAGE
            else -> ThemeRelationshipMode.THEME_ONLY
        }
    }

    /**
     * Converts the editor draft into a persisted AppTheme entity upon user save.
     */
    fun toAppTheme(base: AppTheme): AppTheme {
        val resolved = resolveColors()
        return base.copy(
            schemaVersion = maxOf(base.schemaVersion, ThemeSchema.CURRENT_VERSION),
            name = name,
            isDark = ThemeManager.isDarkColor(bgHex),
            emoji = emoji,
            colors = resolved,
            overrides = if (overrides?.isEmpty() == true) null else overrides,
            fontFamily = fontFamily,
            fontSize = fontSize.toInt(),
            lineHeight = lineHeight,
            paragraphSpacing = paragraphSpacing.toInt(),
            paddingHorizontal = sideMargins.toInt(),
            textAlignment = textAlignment,
            themeScope = themeScope,
            bgMode = bgMode,
            backgroundImageUri = bgUri,
            backgroundImageOriginalUri = bgOriginalUri,
            backgroundImageOpacity = bgOpacity,
            overlayEnabled = overlayEnabled,
            overlayColor = overlayColor,
            blurIntensity = blurIntensity,
            frostedGlassEnabled = frostedGlassEnabled,
            frostedTintEnabled = frostedTintEnabled,
            frostedBlurRadius = frostedBlurRadius,
            savedBgLuminance = bgLuminance,
            savedZonalLuminance = zonalLuminanceMatrix,
            savedZonalVariance = zonalVarianceMatrix,
            savedBgDominantColor = bgDominantColor,
            savedBgZonalColors = zonalColorsMatrix,
            savedBgLuminanceField = luminanceFieldMatrix,
            generationMetadata = if (activeUnderstanding != null || secondaryHex != null || tertiaryHex != null || atmosphericHex != null || visualPalette != null) {
                com.primaloptima.scribe.util.model.ThemeGenerationMetadata(
                    recipe = activeRecipe,
                    imageInfluence = activeInfluence,
                    writingCharacter = activeWritingCharacter,
                    originalAtmosphereHex = atmosphericHex,
                    secondaryAccentHex = secondaryHex,
                    tertiaryAccentHex = tertiaryHex,
                    visualPalette = visualPalette
                )
            } else {
                base.generationMetadata
            }
        )
    }

    companion object {
        fun fromAppTheme(theme: AppTheme): ThemeEditorDraft {
            return ThemeEditorDraft(
                id = theme.id,
                builtIn = theme.builtIn,
                isDark = theme.isDark,
                name = theme.name,
                emoji = theme.emoji ?: "🖊️",
                bgHex = theme.colors.background,
                textHex = theme.colors.text,
                accentHex = theme.colors.accent,
                overrides = theme.overrides,
                fontFamily = theme.fontFamily,
                fontSize = theme.fontSize.toFloat(),
                lineHeight = theme.lineHeight,
                paragraphSpacing = theme.paragraphSpacing.toFloat(),
                sideMargins = theme.paddingHorizontal.toFloat(),
                textAlignment = theme.textAlignment,
                themeScope = theme.themeScope,
                bgMode = theme.bgMode,
                bgUri = theme.backgroundImageUri,
                bgOriginalUri = theme.backgroundImageOriginalUri,
                bgOpacity = theme.backgroundImageOpacity ?: 0.35f,
                overlayEnabled = theme.overlayEnabled,
                overlayColor = theme.overlayColor,
                blurIntensity = theme.blurIntensity,
                frostedGlassEnabled = theme.frostedGlassEnabled,
                frostedTintEnabled = theme.frostedTintEnabled,
                frostedBlurRadius = theme.frostedBlurRadius,
                bgLuminance = theme.savedBgLuminance,
                zonalLuminanceMatrix = theme.savedZonalLuminance,
                zonalVarianceMatrix = theme.savedZonalVariance,
                bgDominantColor = theme.savedBgDominantColor,
                zonalColorsMatrix = theme.savedBgZonalColors,
                luminanceFieldMatrix = theme.savedBgLuminanceField,
                secondaryHex = theme.generationMetadata?.secondaryAccentHex,
                tertiaryHex = theme.generationMetadata?.tertiaryAccentHex,
                atmosphericHex = theme.generationMetadata?.originalAtmosphereHex,
                visualPalette = theme.generationMetadata?.visualPalette ?: theme.sourcePalette().visualPalette
            )
        }
    }
}
