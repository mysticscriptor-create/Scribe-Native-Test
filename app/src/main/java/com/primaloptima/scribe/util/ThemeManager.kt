package com.primaloptima.scribe.util

import com.primaloptima.scribe.ui.theme.ContrastResolver
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.primaloptima.scribe.R
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors
import com.primaloptima.scribe.util.model.ThemeColorOverrides
import com.primaloptima.scribe.util.model.ThemeGenerationMetadata
import com.primaloptima.scribe.util.model.ThemeGenerationRecipe
import com.primaloptima.scribe.util.model.ImageInfluence
import com.primaloptima.scribe.util.model.WritingCharacter
import com.primaloptima.scribe.util.model.ThemeRelationshipMode
import com.primaloptima.scribe.util.model.ThemeSchema
import com.primaloptima.scribe.util.model.ThemeSourcePalette
import com.primaloptima.scribe.util.model.VisualThemePalette
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.math.roundToInt

/**
 * Phase 2-C: ThemeManager migrated away from direct prefs access.
 *
 * ThemeManager is called synchronously from non-coroutine contexts
 * (ThemeViewModel.reload(), applyThemeToActivity()). Rather than blocking
 * on DataStore, we keep an in-memory cache that is seeded once at startup
 * by ScribeApp.seedThemeManagerCache() and updated by ViewModels after
 * every write.
 *
 * Phase 7 complete: all prefs fallbacks removed. Cache is the sole source of truth.
 */
class ThemeManager(private val context: Context) {

    // ── In-memory cache seeded by ScribeApp.seedThemeManagerCache() ──────────
    // Volatile so reads from any thread see the latest write.
    @Volatile private var cachedCustomThemesJson: String? = null
    @Volatile private var cachedActiveThemeId: String? = null

    /**
     * Called once from ScribeApp after DataStore has emitted its first values.
     * After this, allThemes() and activeTheme() read from the cache instead of prefs.
     */
    fun onDataStoreReady(customJson: String, activeId: String) {
        cachedCustomThemesJson = customJson
        cachedActiveThemeId = activeId
    }

    /**
     * Called by ThemeViewModel after every save/delete/setActive so the cache
     * stays in sync without a round-trip through DataStore.
     */
    fun updateCache(customJson: String, activeId: String) {
        cachedCustomThemesJson = customJson
        cachedActiveThemeId = activeId
    }

    // ── Theme accessors ───────────────────────────────────────────────────────

    /** All themes = built-ins + custom themes. Reads from in-memory cache. */
    fun allThemes(): List<AppTheme> {
        val json = cachedCustomThemesJson ?: "[]"
        val custom = AppJson.decodeAppThemes(json)
        val builtInIds = DefaultThemes.all.map { it.id }.toSet()
        val customMap = custom.associateBy { it.id }
        val updatedBuiltIns = DefaultThemes.all.map { builtIn -> customMap[builtIn.id] ?: builtIn }
        val newCustoms = custom.filter { it.id !in builtInIds }
        return (updatedBuiltIns + newCustoms).distinctBy { it.id }
    }

    fun activeTheme(): AppTheme {
        val id = cachedActiveThemeId ?: "paper"
        return allThemes().firstOrNull { it.id == id } ?: DefaultThemes.all.first()
    }

    /** Write to prefs AND update the cache (called from ThemeViewModel coroutine scope). */
    fun setActiveTheme(id: String) {
        cachedActiveThemeId = id
    }

    fun saveCustomTheme(theme: AppTheme) {
        val migrated = migrateTheme(theme)
        val list = allCustomThemes().toMutableList()
        val idx = list.indexOfFirst { it.id == migrated.id }
        if (idx >= 0) list[idx] = migrated else list.add(migrated)
        val json = AppJson.encodeAppThemes(list)
        cachedCustomThemesJson = json
    }

    fun deleteCustomTheme(id: String) {
        val list = allCustomThemes().filter { it.id != id }
        val json = AppJson.encodeAppThemes(list)
        cachedCustomThemesJson = json
        if (cachedActiveThemeId == id) {
            cachedActiveThemeId = "paper"
        }
    }

    fun duplicateTheme(id: String): AppTheme? {
        val source = allThemes().firstOrNull { it.id == id } ?: return null
        val copy = source.copy(
            id = System.currentTimeMillis().toString() + (1000..9999).random().toString(),
            name = "${source.name} Copy",
            builtIn = false,
            schemaVersion = CURRENT_SCHEMA_VERSION
        )
        saveCustomTheme(copy)
        return copy
    }

    fun allCustomThemes(): List<AppTheme> {
        val json = cachedCustomThemesJson ?: "[]"
        return AppJson.decodeAppThemes(json)
    }

    // ── Activity theming (unchanged) ──────────────────────────────────────────

    fun applyThemeToActivity(activity: AppCompatActivity, rootLayout: View? = null, bgImageView: ImageView? = null): AppTheme {
        val theme = activeTheme()
        val window = activity.window
        val bgColor = parseColor(theme.colors.background)
        val toolbarColor = parseColor(theme.colors.toolbar)
        val accentColor = parseColor(theme.colors.accent)
        val textColor = parseColor(theme.colors.text)
        val surfaceColor = parseColor(theme.colors.surface)

        rootLayout?.setBackgroundColor(bgColor)

        window.statusBarColor = toolbarColor
        window.navigationBarColor = surfaceColor

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val isDarkTheme = theme.isDark || isColorDark(bgColor)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        if (bgImageView != null) {
            val imageUriStr = theme.backgroundImageUri
            if (!imageUriStr.isNullOrEmpty()) {
                try {
                    bgImageView.visibility = View.VISIBLE
                    bgImageView.setImageURI(Uri.parse(imageUriStr))
                    bgImageView.alpha = theme.backgroundImageOpacity ?: 0.35f
                    bgImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (_: Exception) {
                    bgImageView.visibility = View.GONE
                }
            } else {
                bgImageView.visibility = View.GONE
            }
        }

        return theme
    }

    companion object {
        /** Current schema version for theme serialization & migration */
        const val CURRENT_SCHEMA_VERSION = ThemeSchema.CURRENT_VERSION

        /**
         * Defensive centralized migration pipeline.
         * Sequentially upgrades older theme instances to the current schema version,
         * populates missing semantic tokens, and sanitizes fields against schema invariants.
         *
         * Guarantees:
         * - Never throws an unhandled exception or discards valid theme data.
         * - Preserves existing user-defined values and explicit overrides.
         * - Synthesizes missing semantic roles deterministically.
         * - Roundtrip idempotent: migrateTheme(migrateTheme(t)) == migrateTheme(t).
         */
        fun migrateTheme(theme: AppTheme): AppTheme {
            return try {
                var current = theme

                // Step 1: Version 0 (Legacy / unversioned) -> Version 1
                if (current.schemaVersion < ThemeSchema.VERSION_1) {
                    current = migrateV0ToV1(current)
                }

                // Step 2: Version 1 -> Version 2 (Session 4 generation provenance)
                if (current.schemaVersion < ThemeSchema.VERSION_2) {
                    current = migrateV1ToV2(current)
                }

                // Step 3: Sanitize and enforce schema invariants
                sanitizeTheme(current)
            } catch (_: Exception) {
                // Safe recovery hierarchy: attempt sanitization with current version stamp
                try {
                    sanitizeTheme(theme.copy(schemaVersion = ThemeSchema.CURRENT_VERSION))
                } catch (_: Exception) {
                    // Safe canonical fallback
                    DefaultThemes.paper
                }
            }
        }

        /**
         * Migrates a Version 1 theme to Version 2:
         * - Ensures generation provenance metadata is properly preserved or synthesized.
         * - Preserves user overrides, explicit color tokens, and layout/typography without mutation.
         * - Updates schemaVersion to VERSION_2.
         */
        fun migrateV1ToV2(v1: AppTheme): AppTheme {
            val existingMeta = v1.generationMetadata
            val updatedMeta = if (existingMeta != null) {
                existingMeta.copy(generationVersion = 2)
            } else if (v1.backgroundImageUri != null) {
                ThemeGenerationMetadata(
                    recipe = ThemeGenerationRecipe.BALANCED,
                    imageInfluence = ImageInfluence.BALANCED,
                    writingCharacter = WritingCharacter.NEUTRAL,
                    relationshipMode = if (v1.bgMode == "blurred" || v1.frostedGlassEnabled) {
                        ThemeRelationshipMode.THEME_GLASS
                    } else {
                        ThemeRelationshipMode.THEME_IMAGE
                    },
                    selectedCandidateHex = v1.colors.accent,
                    originalAtmosphereHex = v1.savedBgDominantColor,
                    generationVersion = 2
                )
            } else null

            return v1.copy(
                schemaVersion = ThemeSchema.VERSION_2,
                generationMetadata = updatedMeta
            )
        }

        /**
         * Migrates a legacy (v0 or unversioned) theme to Version 1:
         * - Derives 5-tier elevation surface tokens and 3-tier boundary tokens.
         * - Derives editorial lexer writing, analytics, and worldbuilding entity roles.
         * - Preserves explicit user color customizations and overrides.
         * - Updates schemaVersion to VERSION_1.
         */
        fun migrateV0ToV1(legacy: AppTheme): AppTheme {
            val isDark = legacy.isDark
            val bg = sanitizeHexColor(legacy.colors.background, if (isDark) "#121214" else "#FAF8F5")
            val text = sanitizeHexColor(legacy.colors.text, if (isDark) "#F4F4F6" else "#1C211E")
            val accent = sanitizeHexColor(legacy.colors.accent, if (isDark) "#E4E4E7" else "#234B39")

            val generatedDefaults = generateThemeDefaults(bg, text, accent, isDark)
            val resolved = resolveThemeColors(
                sources = ThemeSourcePalette(background = bg, text = text, accent = accent),
                overrides = legacy.overrides,
                isDark = isDark
            )
            val finalColors = mergeLegacyCustomColors(legacy.colors, resolved, generatedDefaults)

            return legacy.copy(
                schemaVersion = ThemeSchema.VERSION_1,
                colors = finalColors,
                overrides = legacy.overrides
            )
        }

        /**
         * Preserves explicit user color customizations that may exist in legacy theme colors.
         */
        fun mergeLegacyCustomColors(
            legacy: ThemeColors,
            resolved: ThemeColors,
            defaults: ThemeColors
        ): ThemeColors {
            return resolved.copy(
                toolbar = legacy.toolbar.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.toolbar,
                toolbarText = legacy.toolbarText.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.toolbarText,
                surface = legacy.surface.takeIf { it.isNotBlank() && isValidHexColor(it) && it != legacy.background } ?: resolved.surface,
                success = legacy.success.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.success,
                warning = legacy.warning.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.warning,
                error = legacy.error.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.error,
                specialHighlight = legacy.specialHighlight.takeIf { it.isNotBlank() && isValidHexColor(it) } ?: resolved.specialHighlight,
                dialogueText = legacy.dialogueText.takeIf { it.isNotBlank() && isValidHexColor(it) && it != legacy.accent } ?: resolved.dialogueText,
                monologueText = legacy.monologueText.takeIf { it.isNotBlank() && isValidHexColor(it) && it != legacy.text } ?: resolved.monologueText,
                headingText = legacy.headingText.takeIf { it.isNotBlank() && isValidHexColor(it) && it != legacy.accent } ?: resolved.headingText
            )
        }

        /**
         * Validates and sanitizes all fields of an AppTheme against schema invariants.
         * Ensures non-empty identifiers, valid hex color tokens, and bounded typography/layout metrics.
         */
        fun sanitizeTheme(theme: AppTheme): AppTheme {
            val isDark = theme.isDark
            val defaultBg = if (isDark) "#121214" else "#FAF8F5"
            val defaultText = if (isDark) "#F4F4F6" else "#1C211E"
            val defaultAccent = if (isDark) "#E4E4E7" else "#234B39"

            val bg = sanitizeHexColor(theme.colors.background, defaultBg)
            val text = sanitizeHexColor(theme.colors.text, defaultText)
            val accent = sanitizeHexColor(theme.colors.accent, defaultAccent)

            val safeId = if (theme.id.isBlank()) {
                System.currentTimeMillis().toString() + (1000..9999).random().toString()
            } else theme.id

            val safeName = if (theme.name.isBlank()) "Custom Theme" else theme.name

            val defaults = generateThemeDefaults(bg, text, accent, isDark)
            val c = theme.colors
            val sanitizedColors = ThemeColors(
                background = bg,
                surfaceLowest = sanitizeHexColor(c.surfaceLowest, defaults.surfaceLowest),
                surface = sanitizeHexColor(c.surface, defaults.surface),
                surfaceRaised = sanitizeHexColor(c.surfaceRaised, defaults.surfaceRaised),
                surfaceOverlay = sanitizeHexColor(c.surfaceOverlay, defaults.surfaceOverlay),
                text = text,
                mutedText = sanitizeHexColor(c.mutedText, defaults.mutedText),
                subtleText = sanitizeHexColor(c.subtleText, defaults.subtleText),
                accent = accent,
                secondary = sanitizeHexColor(c.secondary, defaults.secondary),
                tertiary = sanitizeHexColor(c.tertiary, defaults.tertiary),
                accentMuted = sanitizeHexColor(c.accentMuted, defaults.accentMuted),
                selection = sanitizeHexColor(c.selection, defaults.selection),
                success = sanitizeHexColor(c.success, defaults.success),
                warning = sanitizeHexColor(c.warning, defaults.warning),
                error = sanitizeHexColor(c.error, defaults.error),
                info = sanitizeHexColor(c.info, defaults.info),
                specialHighlight = sanitizeHexColor(c.specialHighlight, defaults.specialHighlight),
                border = sanitizeHexColor(c.border, defaults.border),
                borderSubtle = sanitizeHexColor(c.borderSubtle, defaults.borderSubtle),
                borderProminent = sanitizeHexColor(c.borderProminent, defaults.borderProminent),
                focus = sanitizeHexColor(c.focus, defaults.focus.ifBlank { defaults.borderProminent }),
                dialogueText = sanitizeHexColor(c.dialogueText, defaults.dialogueText),
                monologueText = sanitizeHexColor(c.monologueText, defaults.monologueText),
                headingText = sanitizeHexColor(c.headingText, defaults.headingText),
                annotation = sanitizeHexColor(c.annotation, defaults.annotation),
                link = sanitizeHexColor(c.link, defaults.link),
                analyticsPositive = sanitizeHexColor(c.analyticsPositive, defaults.analyticsPositive),
                analyticsNeutral = sanitizeHexColor(c.analyticsNeutral, defaults.analyticsNeutral),
                analyticsNegative = sanitizeHexColor(c.analyticsNegative, defaults.analyticsNegative),
                analyticsSeries1 = sanitizeHexColor(c.analyticsSeries1, defaults.analyticsSeries1),
                analyticsSeries2 = sanitizeHexColor(c.analyticsSeries2, defaults.analyticsSeries2),
                analyticsSeries3 = sanitizeHexColor(c.analyticsSeries3, defaults.analyticsSeries3),
                analyticsTarget = sanitizeHexColor(c.analyticsTarget, defaults.analyticsTarget),
                analyticsWarning = sanitizeHexColor(c.analyticsWarning, defaults.analyticsWarning),
                worldCharacter = sanitizeHexColor(c.worldCharacter, defaults.worldCharacter),
                worldLocation = sanitizeHexColor(c.worldLocation, defaults.worldLocation),
                worldFaction = sanitizeHexColor(c.worldFaction, defaults.worldFaction),
                worldItem = sanitizeHexColor(c.worldItem, defaults.worldItem),
                worldLore = sanitizeHexColor(c.worldLore, defaults.worldLore),
                worldEvent = sanitizeHexColor(c.worldEvent, defaults.worldEvent),
                worldRelationship = sanitizeHexColor(c.worldRelationship, defaults.worldRelationship),
                toolbar = sanitizeHexColor(c.toolbar, defaults.toolbar),
                toolbarText = sanitizeHexColor(c.toolbarText, defaults.toolbarText)
            )

            val sanitizedOverrides = theme.overrides?.let { o ->
                fun cleanOverride(v: String?): String? = v?.takeIf { it.isNotBlank() && isValidHexColor(it) }
                val cleaned = o.copy(
                    surfaceLowest = cleanOverride(o.surfaceLowest),
                    surface = cleanOverride(o.surface),
                    surfaceRaised = cleanOverride(o.surfaceRaised),
                    surfaceOverlay = cleanOverride(o.surfaceOverlay),
                    mutedText = cleanOverride(o.mutedText),
                    subtleText = cleanOverride(o.subtleText),
                    secondary = cleanOverride(o.secondary),
                    tertiary = cleanOverride(o.tertiary),
                    accentMuted = cleanOverride(o.accentMuted),
                    selection = cleanOverride(o.selection),
                    border = cleanOverride(o.border),
                    borderSubtle = cleanOverride(o.borderSubtle),
                    borderProminent = cleanOverride(o.borderProminent),
                    focus = cleanOverride(o.focus),
                    success = cleanOverride(o.success),
                    warning = cleanOverride(o.warning),
                    error = cleanOverride(o.error),
                    info = cleanOverride(o.info),
                    specialHighlight = cleanOverride(o.specialHighlight),
                    dialogueText = cleanOverride(o.dialogueText),
                    monologueText = cleanOverride(o.monologueText),
                    headingText = cleanOverride(o.headingText),
                    annotation = cleanOverride(o.annotation),
                    link = cleanOverride(o.link),
                    analyticsPositive = cleanOverride(o.analyticsPositive),
                    analyticsNeutral = cleanOverride(o.analyticsNeutral),
                    analyticsNegative = cleanOverride(o.analyticsNegative),
                    analyticsSeries1 = cleanOverride(o.analyticsSeries1),
                    analyticsSeries2 = cleanOverride(o.analyticsSeries2),
                    analyticsSeries3 = cleanOverride(o.analyticsSeries3),
                    analyticsTarget = cleanOverride(o.analyticsTarget),
                    analyticsWarning = cleanOverride(o.analyticsWarning),
                    worldCharacter = cleanOverride(o.worldCharacter),
                    worldLocation = cleanOverride(o.worldLocation),
                    worldFaction = cleanOverride(o.worldFaction),
                    worldItem = cleanOverride(o.worldItem),
                    worldLore = cleanOverride(o.worldLore),
                    worldEvent = cleanOverride(o.worldEvent),
                    worldRelationship = cleanOverride(o.worldRelationship)
                )
                if (cleaned.isEmpty()) null else cleaned
            }

            return theme.copy(
                id = safeId,
                name = safeName,
                schemaVersion = maxOf(theme.schemaVersion, ThemeSchema.CURRENT_VERSION),
                colors = sanitizedColors,
                overrides = sanitizedOverrides,
                fontFamily = if (theme.fontFamily.isBlank()) "sans" else theme.fontFamily,
                fontSize = theme.fontSize.coerceIn(10, 48),
                lineHeight = theme.lineHeight.coerceIn(1.0f, 3.0f),
                letterSpacing = theme.letterSpacing.coerceIn(-1.0f, 2.0f),
                paragraphSpacing = theme.paragraphSpacing.coerceIn(0, 60),
                paddingHorizontal = theme.paddingHorizontal.coerceIn(0, 120),
                paddingVertical = theme.paddingVertical.coerceIn(0, 120),
                maxWidth = theme.maxWidth.coerceIn(320, 2560),
                bgMode = if (theme.bgMode in listOf("color", "image", "blurred")) theme.bgMode else "color",
                blurIntensity = theme.blurIntensity.coerceIn(0f, 100f),
                frostedBlurRadius = theme.frostedBlurRadius.coerceIn(0f, 100f),
                backgroundImageOpacity = theme.backgroundImageOpacity?.coerceIn(0f, 1f) ?: 0.35f,
                overlayEnabled = theme.overlayEnabled,
                overlayColor = theme.overlayColor?.let { if (isValidHexColor(it)) sanitizeHexColor(it, it) else null },
                textAlignment = if (theme.textAlignment in listOf("left", "justified", "center")) theme.textAlignment else "left",
                themeScope = if (theme.themeScope in listOf("whole_app", "editor_only")) theme.themeScope else "whole_app"
            )
        }

        fun isValidHexColor(hex: String?): Boolean {
            if (hex.isNullOrBlank()) return false
            val clean = hex.trim().removePrefix("#")
            if (clean.length != 6 && clean.length != 8 && clean.length != 3) return false
            return clean.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
        }

        fun sanitizeHexColor(hex: String?, fallback: String): String {
            if (hex.isNullOrBlank()) return fallback
            val trimmed = hex.trim()
            val formatted = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
            return if (isValidHexColor(formatted)) formatted else fallback
        }

        fun parseColor(hex: String): Int = try {
            if (hex.startsWith("#")) {
                val clean = hex.removePrefix("#")
                when (clean.length) {
                    6 -> (0xFF000000.toInt()) or clean.toLong(16).toInt()
                    8 -> clean.toLong(16).toInt()
                    3 -> {
                        val r = clean[0].toString().repeat(2).toInt(16)
                        val g = clean[1].toString().repeat(2).toInt(16)
                        val b = clean[2].toString().repeat(2).toInt(16)
                        (0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
                    }
                    else -> Color.parseColor(hex)
                }
            } else {
                Color.parseColor(hex)
            }
        } catch (_: Exception) { Color.BLACK }

        // ── OKLCH Perceptual Color Space Engine ──
        data class Oklch(val l: Double, val c: Double, val h: Double)

        private fun sRgbToLinear(c: Double): Double {
            return if (c >= 0.04045) {
                Math.pow((c + 0.055) / 1.055, 2.4)
            } else {
                c / 12.92
            }
        }

        private fun linearToSRgb(c: Double): Double {
            val clamped = c.coerceIn(0.0, 1.0)
            return if (clamped <= 0.0031308) {
                12.92 * clamped
            } else {
                1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055
            }
        }

        fun colorToOklch(colorInt: Int): Oklch {
            val r = sRgbToLinear(((colorInt shr 16) and 0xFF) / 255.0)
            val g = sRgbToLinear(((colorInt shr 8) and 0xFF) / 255.0)
            val b = sRgbToLinear((colorInt and 0xFF) / 255.0)

            val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
            val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
            val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

            val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
            val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
            val bVal = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s

            val C = Math.sqrt(a * a + bVal * bVal)
            var h = Math.toDegrees(Math.atan2(bVal, a))
            if (h < 0.0) h += 360.0

            return Oklch(L.coerceIn(0.0, 1.0), C.coerceAtLeast(0.0), h)
        }

        fun oklchToColor(oklch: Oklch): Int {
            val hRad = Math.toRadians(oklch.h)
            val a = oklch.c * Math.cos(hRad)
            val bVal = oklch.c * Math.sin(hRad)

            val l_ = oklch.l + 0.3963377774 * a + 0.2158037573 * bVal
            val m_ = oklch.l - 0.1055613458 * a - 0.0638541728 * bVal
            val s_ = oklch.l - 0.0894841775 * a - 1.2914855480 * bVal

            val l = l_ * l_ * l_
            val m = m_ * m_ * m_
            val s = s_ * s_ * s_

            val rLin = +4.0767439362 * l - 3.3077115913 * m + 0.2309699292 * s
            val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
            val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

            val r = (linearToSRgb(rLin) * 255.0).roundToInt().coerceIn(0, 255)
            val g = (linearToSRgb(gLin) * 255.0).roundToInt().coerceIn(0, 255)
            val b = (linearToSRgb(bLin) * 255.0).roundToInt().coerceIn(0, 255)

            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun oklchToHex(oklch: Oklch): String {
            val c = oklchToColor(oklch)
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            return String.format("#%02X%02X%02X", r, g, b)
        }

        fun shiftOklch(colorInt: Int, deltaL: Double, chromaFactor: Double = 1.0): String {
            val oklch = colorToOklch(colorInt)
            val newL = (oklch.l + deltaL).coerceIn(0.01, 0.99)
            val newC = (oklch.c * chromaFactor).coerceAtLeast(0.0)
            return oklchToHex(Oklch(newL, newC, oklch.h))
        }

        fun createOklchColor(l: Double, c: Double, h: Double): String {
            return oklchToHex(Oklch(l.coerceIn(0.01, 0.99), c.coerceAtLeast(0.0), h % 360.0))
        }

        fun isColorDark(color: Int): Boolean {
            return colorToOklch(color).l < 0.45
        }

        fun isDarkColor(hex: String): Boolean {
            return isColorDark(parseColor(hex))
        }

        /**
         * Phase 1 Architecture:
         * Generates the deterministic default semantic tokens from Foundation Sources (background, text, accent).
         * Does not apply any user overrides.
         */
        fun generateThemeDefaults(
            sources: ThemeSourcePalette,
            isDark: Boolean,
            metadata: ThemeGenerationMetadata? = null
        ): ThemeColors {
            val effectiveMeta = metadata ?: (sources.visualPalette?.let {
                ThemeGenerationMetadata(
                    originalAtmosphereHex = sources.atmosphericColor,
                    secondaryAccentHex = sources.secondaryAccent,
                    tertiaryAccentHex = sources.tertiaryAccent,
                    highlightHex = it.visualHighlight,
                    visualPalette = it
                )
            } ?: sources.atmosphericColor?.let {
                ThemeGenerationMetadata(originalAtmosphereHex = it)
            })
            return generateThemeDefaults(
                bgHex = sources.background,
                textHex = sources.text,
                accentHex = sources.accent,
                isDark = isDark,
                secondaryHex = sources.secondaryAccent,
                tertiaryHex = sources.tertiaryAccent,
                metadata = effectiveMeta
            )
        }

        fun generateThemeDefaults(
            bgHex: String,
            textHex: String,
            accentHex: String,
            isDark: Boolean,
            secondaryHex: String? = null,
            tertiaryHex: String? = null
        ): ThemeColors {
            return generateThemeDefaults(
                bgHex = bgHex,
                textHex = textHex,
                accentHex = accentHex,
                isDark = isDark,
                secondaryHex = secondaryHex,
                tertiaryHex = tertiaryHex,
                metadata = null
            )
        }

        fun generateThemeDefaults(
            bgHex: String,
            textHex: String,
            accentHex: String,
            isDark: Boolean,
            secondaryHex: String? = null,
            tertiaryHex: String? = null,
            metadata: ThemeGenerationMetadata? = null
        ): ThemeColors {
            val bgInt = parseColor(bgHex)
            var textInt = parseColor(textHex)
            val accentInt = parseColor(accentHex)

            val bgOklch = colorToOklch(bgInt)
            var textOklch = colorToOklch(textInt)
            val accentOklch = colorToOklch(accentInt)

            val recipe = metadata?.recipe ?: ThemeGenerationRecipe.BALANCED
            val influence = metadata?.imageInfluence ?: ImageInfluence.BALANCED
            val writingChar = metadata?.writingCharacter ?: WritingCharacter.NEUTRAL
            val atmoHex = metadata?.originalAtmosphereHex
            val atmoOklch = atmoHex?.let { colorToOklch(parseColor(it)) }
            val visualPalette = metadata?.visualPalette

            val influenceFactor = when (influence) {
                ImageInfluence.SUBTLE -> 0.50
                ImageInfluence.BALANCED -> 1.00
                ImageInfluence.STRONG -> 1.55
            }

            // Polarity Auto-Adjustment: Ensure text color has strong perceptual contrast relative to background
            // ΔL between background and text must be sufficient for high APCA readability
            val effectiveTextHex: String
            if (isDark) {
                // Dark background: text must be perceptually light (L >= 0.75, ideal ~0.92)
                if (textOklch.l < 0.60) {
                    textOklch = Oklch(0.92, (textOklch.c * 0.4).coerceAtMost(0.04), textOklch.h)
                    effectiveTextHex = oklchToHex(textOklch)
                    textInt = parseColor(effectiveTextHex)
                } else {
                    effectiveTextHex = textHex
                }
            } else {
                // Light or medium-bright background: text must be perceptually dark (L <= 0.30, ideal ~0.16)
                if (textOklch.l > 0.40) {
                    textOklch = Oklch(0.16, (textOklch.c * 0.4).coerceAtMost(0.04), textOklch.h)
                    effectiveTextHex = oklchToHex(textOklch)
                    textInt = parseColor(effectiveTextHex)
                } else {
                    effectiveTextHex = textHex
                }
            }

            fun blend(c1: Int, c2: Int, ratio: Float): String {
                val r1 = (c1 shr 16) and 0xFF
                val g1 = (c1 shr 8) and 0xFF
                val b1 = c1 and 0xFF

                val r2 = (c2 shr 16) and 0xFF
                val g2 = (c2 shr 8) and 0xFF
                val b2 = c2 and 0xFF

                val r = (r1 * (1f - ratio) + r2 * ratio).toInt().coerceIn(0, 255)
                val g = (g1 * (1f - ratio) + g2 * ratio).toInt().coerceIn(0, 255)
                val b = (b1 * (1f - ratio) + b2 * ratio).toInt().coerceIn(0, 255)
                return String.format("#%02X%02X%02X", r, g, b)
            }

            return if (isDark) {
                // Dark Mode Elevation Ramp (preserving subtle hue and saturation with progressive lightness lift)
                val surfaceLowest: String
                val surface: String
                val surfaceRaised: String
                val surfaceOverlay: String

                if (visualPalette != null) {
                    surfaceLowest = visualPalette.visualEditorSurface
                    surface = visualPalette.visualChrome
                    surfaceRaised = visualPalette.visualElevatedSurface
                    val raisedOklch = colorToOklch(parseColor(surfaceRaised))
                    surfaceOverlay = oklchToHex(Oklch((raisedOklch.l + 0.050).coerceIn(0.01, 0.95), (raisedOklch.c * 0.85).coerceAtLeast(0.0), raisedOklch.h))
                } else {
                    when (recipe) {
                        ThemeGenerationRecipe.ATMOSPHERIC -> {
                            val tintHue = atmoOklch?.h ?: bgOklch.h
                            val tintChroma = ((bgOklch.c + (atmoOklch?.c ?: 0.0) * 0.20) * influenceFactor).coerceIn(0.012, 0.055)
                            surfaceLowest = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.01, 0.95), tintChroma * 0.95, tintHue))
                            surface = oklchToHex(Oklch((bgOklch.l + 0.055).coerceIn(0.01, 0.95), tintChroma * 0.90, tintHue))
                            surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.095).coerceIn(0.01, 0.95), tintChroma * 0.85, tintHue))
                            surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.145).coerceIn(0.01, 0.95), tintChroma * 0.80, tintHue))
                        }
                        ThemeGenerationRecipe.INK -> {
                            val inkChroma = (bgOklch.c * 0.15).coerceAtMost(0.005)
                            surfaceLowest = oklchToHex(Oklch((bgOklch.l + 0.028).coerceIn(0.01, 0.95), inkChroma, bgOklch.h))
                            surface = oklchToHex(Oklch((bgOklch.l + 0.060).coerceIn(0.01, 0.95), inkChroma, bgOklch.h))
                            surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.100).coerceIn(0.01, 0.95), inkChroma, bgOklch.h))
                            surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.150).coerceIn(0.01, 0.95), inkChroma, bgOklch.h))
                        }
                        ThemeGenerationRecipe.EXPRESSIVE -> {
                            val exprChroma = (bgOklch.c * 1.15 * influenceFactor).coerceIn(0.015, 0.065)
                            surfaceLowest = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.01, 0.95), exprChroma * 0.95, bgOklch.h))
                            surface = oklchToHex(Oklch((bgOklch.l + 0.055).coerceIn(0.01, 0.95), exprChroma * 0.90, bgOklch.h))
                            surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.095).coerceIn(0.01, 0.95), exprChroma * 0.85, bgOklch.h))
                            surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.145).coerceIn(0.01, 0.95), exprChroma * 0.80, bgOklch.h))
                        }
                        ThemeGenerationRecipe.BALANCED -> {
                            surfaceLowest = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.01, 0.95), bgOklch.c * 0.95, bgOklch.h))
                            surface = oklchToHex(Oklch((bgOklch.l + 0.055).coerceIn(0.01, 0.95), bgOklch.c * 0.90, bgOklch.h))
                            surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.095).coerceIn(0.01, 0.95), bgOklch.c * 0.85, bgOklch.h))
                            surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.145).coerceIn(0.01, 0.95), bgOklch.c * 0.80, bgOklch.h))
                        }
                    }
                }

                // Content & Typography Hierarchy (calculated relative to text luminance)
                val mutedText = oklchToHex(Oklch((textOklch.l - 0.28).coerceIn(0.35, 0.85), (textOklch.c * 0.70).coerceAtLeast(0.0), textOklch.h))
                val subtleText = oklchToHex(Oklch((textOklch.l - 0.45).coerceIn(0.25, 0.70), (textOklch.c * 0.50).coerceAtLeast(0.0), textOklch.h))

                // Interactive & Secondary Harmonics (derived from multi-source palette or in OKLCH space from accent)
                val secondaryDefault = visualPalette?.visualSecondaryAccent ?: secondaryHex ?: oklchToHex(Oklch((accentOklch.l - 0.04).coerceIn(0.30, 0.85), (accentOklch.c * (if (recipe == ThemeGenerationRecipe.EXPRESSIVE) 1.15 else 0.85)).coerceAtLeast(0.0), (accentOklch.h + 20.0) % 360.0))
                val tertiaryDefault = visualPalette?.visualTertiaryAccent ?: tertiaryHex ?: oklchToHex(Oklch((accentOklch.l + 0.06).coerceIn(0.40, 0.90), (accentOklch.c * (if (recipe == ThemeGenerationRecipe.EXPRESSIVE) 1.05 else 0.75)).coerceAtLeast(0.0), (accentOklch.h - 30.0 + 360.0) % 360.0))

                // Perceptually tuned Semantic Feedback Roles (APCA readable on dark surfaces)
                val successDefault = createOklchColor(0.76, 0.15, 142.0)
                val warningDefault = createOklchColor(0.82, 0.16, 85.0)
                val errorDefault = createOklchColor(0.72, 0.18, 25.0)
                val infoDefault = createOklchColor(0.75, 0.14, 230.0)
                val specialHighlightDefault = visualPalette?.visualHighlight ?: (metadata?.highlightHex ?: createOklchColor(0.78, 0.16, 60.0))

                // Containers & Selection
                val accentMuted = blend(accentInt, bgInt, 0.80f)
                val selection = blend(accentInt, bgInt, 0.65f)

                // Boundaries & Focus (subtle divider vs standard component boundary vs focus)
                val borderNormal = visualPalette?.visualNeutral ?: oklchToHex(Oklch((bgOklch.l + 0.14).coerceIn(0.01, 0.95), bgOklch.c * 0.70, bgOklch.h))
                val borderSubtle = oklchToHex(Oklch((bgOklch.l + 0.08).coerceIn(0.01, 0.95), bgOklch.c * 0.75, bgOklch.h))
                val borderProminent = oklchToHex(Oklch((bgOklch.l + 0.28).coerceIn(0.12, 0.85), (accentOklch.c * 0.40).coerceIn(0.02, 0.08), accentOklch.h))
                val focusDefault = oklchToHex(Oklch(0.85, (accentOklch.c * 0.85).coerceIn(0.10, 0.22), accentOklch.h))

                // Lexer & Writing Engine Syntactical Roles (Editorial)
                val dialogueDefault = when (writingChar) {
                    WritingCharacter.WARM -> createOklchColor(0.90, 0.14, 75.0)
                    WritingCharacter.COOL -> createOklchColor(0.88, 0.13, 175.0)
                    WritingCharacter.DRAMATIC -> createOklchColor(0.96, 0.15, 86.0)
                    WritingCharacter.NEUTRAL -> createOklchColor(0.90, 0.13, 86.0)
                }
                val monologueDefault = when (writingChar) {
                    WritingCharacter.WARM -> createOklchColor(0.82, 0.08, 65.0)
                    WritingCharacter.COOL -> createOklchColor(0.80, 0.10, 275.0)
                    WritingCharacter.DRAMATIC -> createOklchColor(0.88, 0.10, 255.0)
                    WritingCharacter.NEUTRAL -> createOklchColor(0.80, 0.09, 255.0)
                }
                val headingDefault = when (writingChar) {
                    WritingCharacter.DRAMATIC -> oklchToHex(Oklch((textOklch.l + 0.06).coerceIn(0.94, 0.99), (accentOklch.c * 0.45).coerceIn(0.04, 0.10), accentOklch.h))
                    else -> oklchToHex(Oklch((textOklch.l + 0.04).coerceIn(0.92, 0.98), (accentOklch.c * 0.35).coerceIn(0.02, 0.08), accentOklch.h))
                }
                val annotationDefault = createOklchColor(0.78, 0.14, 300.0)
                val linkDefault = if (circularHueDistance(accentOklch.h, 235.0) < 30.0) {
                    oklchToHex(Oklch(0.80, 0.15, (accentOklch.h + 35.0) % 360.0))
                } else {
                    createOklchColor(0.78, 0.15, 235.0)
                }

                // Analytics & Metrics Semantics (Independent derivation)
                val analyticsPositiveDefault = createOklchColor(0.74, 0.16, 158.0)
                val analyticsNeutralDefault = mutedText
                val analyticsNegativeDefault = createOklchColor(0.74, 0.18, 12.0)
                val analyticsSeries1Default = if (circularHueDistance(accentOklch.h, 235.0) < 30.0) {
                    oklchToHex(Oklch(0.76, 0.17, (accentOklch.h + 40.0) % 360.0))
                } else {
                    oklchToHex(Oklch(0.76, 0.17, 235.0))
                }
                val analyticsSeries2Default = oklchToHex(Oklch(0.75, 0.16, 280.0))
                val analyticsSeries3Default = oklchToHex(Oklch(0.78, 0.14, 195.0))
                val analyticsTargetDefault = oklchToHex(Oklch(0.82, 0.16, 85.0))
                val analyticsWarningDefault = createOklchColor(0.80, 0.16, 68.0)

                // Worldbuilding Lore Entity Semantics (Independent derivation)
                val worldCharacterDefault = if (circularHueDistance(accentOklch.h, 350.0) < 25.0) {
                    oklchToHex(Oklch(0.76, 0.17, (accentOklch.h + 35.0) % 360.0))
                } else {
                    oklchToHex(Oklch(0.76, 0.17, 350.0))
                }
                val worldLocationDefault = oklchToHex(Oklch(0.78, 0.15, 138.0))
                val worldFactionDefault = oklchToHex(Oklch(0.72, 0.16, 250.0))
                val worldItemDefault = oklchToHex(Oklch(0.82, 0.16, 78.0))
                val worldLoreDefault = oklchToHex(Oklch(0.74, 0.15, 295.0))
                val worldEventDefault = oklchToHex(Oklch(0.75, 0.17, 325.0))
                val worldRelationshipDefault = oklchToHex(Oklch(0.78, 0.14, 178.0))

                // ── Stage 4: Structured Output Assembly ───────────────────────────────
                val rawDarkColors = ThemeColors(
                    background = bgHex,
                    surfaceLowest = surfaceLowest,
                    surface = surface,
                    surfaceRaised = surfaceRaised,
                    surfaceOverlay = surfaceOverlay,
                    text = effectiveTextHex,
                    mutedText = mutedText,
                    subtleText = subtleText,
                    accent = accentHex,
                    secondary = secondaryDefault,
                    tertiary = tertiaryDefault,
                    success = successDefault,
                    warning = warningDefault,
                    error = errorDefault,
                    info = infoDefault,
                    specialHighlight = specialHighlightDefault,
                    accentMuted = accentMuted,
                    selection = selection,
                    border = borderNormal,
                    borderSubtle = borderSubtle,
                    borderProminent = borderProminent,
                    focus = focusDefault,
                    dialogueText = dialogueDefault,
                    monologueText = monologueDefault,
                    headingText = headingDefault,
                    annotation = annotationDefault,
                    link = linkDefault,
                    analyticsPositive = analyticsPositiveDefault,
                    analyticsNeutral = analyticsNeutralDefault,
                    analyticsNegative = analyticsNegativeDefault,
                    analyticsSeries1 = analyticsSeries1Default,
                    analyticsSeries2 = analyticsSeries2Default,
                    analyticsSeries3 = analyticsSeries3Default,
                    analyticsTarget = analyticsTargetDefault,
                    analyticsWarning = analyticsWarningDefault,
                    worldCharacter = worldCharacterDefault,
                    worldLocation = worldLocationDefault,
                    worldFaction = worldFactionDefault,
                    worldItem = worldItemDefault,
                    worldLore = worldLoreDefault,
                    worldEvent = worldEventDefault,
                    worldRelationship = worldRelationshipDefault,
                    toolbar = surface,
                    toolbarText = effectiveTextHex
                )
                resolveSemanticCollisions(rawDarkColors, isDark = true)
            } else {
                // Light & Tinted Mode Elevation Ramp
                val surfaceLowest: String
                val surface: String
                val surfaceRaised: String
                val surfaceOverlay: String

                val isHighKey = bgOklch.l >= 0.90
                if (visualPalette != null) {
                    surfaceLowest = visualPalette.visualEditorSurface
                    surface = visualPalette.visualChrome
                    surfaceRaised = visualPalette.visualElevatedSurface
                    val raisedOklch = colorToOklch(parseColor(surfaceRaised))
                    surfaceOverlay = if (raisedOklch.l >= 0.96) "#FFFFFF" else oklchToHex(Oklch((raisedOklch.l + 0.025).coerceIn(0.05, 1.0), (raisedOklch.c * 0.70).coerceAtLeast(0.0), raisedOklch.h))
                } else {
                    when (recipe) {
                        ThemeGenerationRecipe.ATMOSPHERIC -> {
                            val tintHue = atmoOklch?.h ?: bgOklch.h
                            val tintChroma = ((bgOklch.c + (atmoOklch?.c ?: 0.0) * 0.18) * influenceFactor).coerceIn(0.010, 0.045)
                            if (isHighKey) {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.050).coerceIn(0.05, 0.98), tintChroma * 1.05, tintHue))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.024).coerceIn(0.05, 0.98), tintChroma * 0.95, tintHue))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + (1.0 - bgOklch.l) * 0.45).coerceIn(0.05, 0.992), tintChroma * 0.70, tintHue))
                                surfaceOverlay = oklchToHex(Oklch(1.0, 0.0, tintHue))
                            } else {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.045).coerceIn(0.05, 0.98), tintChroma * 1.05, tintHue))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.025).coerceIn(0.05, 0.98), tintChroma * 0.95, tintHue))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.05, 0.99), tintChroma * 0.80, tintHue))
                                surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.050).coerceIn(0.05, 1.0), tintChroma * 0.65, tintHue))
                            }
                        }
                        ThemeGenerationRecipe.INK -> {
                            val inkChroma = (bgOklch.c * 0.15).coerceAtMost(0.005)
                            if (isHighKey) {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.050).coerceIn(0.05, 0.98), inkChroma, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.024).coerceIn(0.05, 0.98), inkChroma, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + (1.0 - bgOklch.l) * 0.45).coerceIn(0.05, 0.992), inkChroma, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch(1.0, 0.0, bgOklch.h))
                            } else {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.045).coerceIn(0.05, 0.98), inkChroma, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.025).coerceIn(0.05, 0.98), inkChroma, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.05, 0.99), inkChroma, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.050).coerceIn(0.05, 1.0), inkChroma, bgOklch.h))
                            }
                        }
                        ThemeGenerationRecipe.EXPRESSIVE -> {
                            val exprChroma = (bgOklch.c * 1.15 * influenceFactor).coerceIn(0.012, 0.055)
                            if (isHighKey) {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.050).coerceIn(0.05, 0.98), exprChroma * 1.05, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.024).coerceIn(0.05, 0.98), exprChroma * 0.95, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + (1.0 - bgOklch.l) * 0.45).coerceIn(0.05, 0.992), exprChroma * 0.70, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch(1.0, 0.0, bgOklch.h))
                            } else {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.045).coerceIn(0.05, 0.98), exprChroma * 1.05, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.025).coerceIn(0.05, 0.98), exprChroma * 0.95, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.05, 0.99), exprChroma * 0.80, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.050).coerceIn(0.05, 1.0), exprChroma * 0.65, bgOklch.h))
                            }
                        }
                        ThemeGenerationRecipe.BALANCED -> {
                            if (isHighKey) {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.050).coerceIn(0.05, 0.98), bgOklch.c * 1.05, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.024).coerceIn(0.05, 0.98), bgOklch.c * 0.95, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + (1.0 - bgOklch.l) * 0.45).coerceIn(0.05, 0.992), bgOklch.c * 0.70, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch(1.0, 0.0, bgOklch.h))
                            } else {
                                surfaceLowest = oklchToHex(Oklch((bgOklch.l - 0.045).coerceIn(0.05, 0.98), bgOklch.c * 1.05, bgOklch.h))
                                surface = oklchToHex(Oklch((bgOklch.l - 0.025).coerceIn(0.05, 0.98), bgOklch.c * 0.95, bgOklch.h))
                                surfaceRaised = oklchToHex(Oklch((bgOklch.l + 0.025).coerceIn(0.05, 0.99), bgOklch.c * 0.80, bgOklch.h))
                                surfaceOverlay = oklchToHex(Oklch((bgOklch.l + 0.050).coerceIn(0.05, 1.0), bgOklch.c * 0.65, bgOklch.h))
                            }
                        }
                    }
                }

                // Content & Typography Hierarchy (increasing lightness in OKLCH with reduced chroma)
                val mutedText = oklchToHex(Oklch((textOklch.l + 0.28).coerceIn(0.20, 0.75), (textOklch.c * 0.65).coerceAtLeast(0.0), textOklch.h))
                val subtleText = oklchToHex(Oklch((textOklch.l + 0.44).coerceIn(0.30, 0.85), (textOklch.c * 0.50).coerceAtLeast(0.0), textOklch.h))

                // Interactive & Secondary Harmonics (derived from multi-source palette or in OKLCH space from accent)
                val secondaryDefault = visualPalette?.visualSecondaryAccent ?: secondaryHex ?: oklchToHex(Oklch((accentOklch.l + 0.08).coerceIn(0.20, 0.75), (accentOklch.c * (if (recipe == ThemeGenerationRecipe.EXPRESSIVE) 1.15 else 0.85)).coerceAtLeast(0.0), (accentOklch.h + 15.0) % 360.0))
                val tertiaryDefault = visualPalette?.visualTertiaryAccent ?: tertiaryHex ?: oklchToHex(Oklch((accentOklch.l + 0.14).coerceIn(0.25, 0.80), (accentOklch.c * (if (recipe == ThemeGenerationRecipe.EXPRESSIVE) 1.05 else 0.75)).coerceAtLeast(0.0), (accentOklch.h - 25.0 + 360.0) % 360.0))

                // Perceptually tuned Semantic Feedback Roles (APCA readable on light surfaces)
                val successDefault = createOklchColor(0.48, 0.16, 142.0)
                val warningDefault = createOklchColor(0.55, 0.16, 80.0)
                val errorDefault = createOklchColor(0.50, 0.20, 25.0)
                val infoDefault = createOklchColor(0.52, 0.15, 230.0)
                val specialHighlightDefault = visualPalette?.visualHighlight ?: (metadata?.highlightHex ?: createOklchColor(0.60, 0.16, 85.0))

                // Containers & Selection
                val accentMuted = blend(accentInt, bgInt, 0.88f)
                val selection = blend(accentInt, bgInt, 0.78f)

                // Boundaries & Focus (derived with adequate contrast against light background)
                val borderNormal = visualPalette?.visualNeutral ?: oklchToHex(Oklch((bgOklch.l - 0.14).coerceIn(0.10, 0.98), bgOklch.c * 0.65, bgOklch.h))
                val borderSubtle = oklchToHex(Oklch((bgOklch.l - 0.085).coerceIn(0.10, 0.98), bgOklch.c * 0.70, bgOklch.h))
                val borderProminent = oklchToHex(Oklch((bgOklch.l - 0.28).coerceIn(0.12, 0.75), (accentOklch.c * 0.40).coerceIn(0.02, 0.08), accentOklch.h))
                val focusDefault = oklchToHex(Oklch(0.32, (accentOklch.c * 0.85).coerceIn(0.12, 0.24), accentOklch.h))

                // Lexer & Writing Engine Syntactical Roles
                val dialogueDefault = when (writingChar) {
                    WritingCharacter.WARM -> createOklchColor(0.44, 0.16, 40.0)
                    WritingCharacter.COOL -> createOklchColor(0.42, 0.16, 205.0)
                    WritingCharacter.DRAMATIC -> createOklchColor(0.32, 0.18, 45.0)
                    WritingCharacter.NEUTRAL -> createOklchColor(0.44, 0.16, 45.0)
                }
                val monologueDefault = when (writingChar) {
                    WritingCharacter.WARM -> createOklchColor(0.40, 0.10, 55.0)
                    WritingCharacter.COOL -> createOklchColor(0.38, 0.12, 275.0)
                    WritingCharacter.DRAMATIC -> createOklchColor(0.28, 0.14, 255.0)
                    WritingCharacter.NEUTRAL -> createOklchColor(0.40, 0.12, 255.0)
                }
                val headingDefault = when (writingChar) {
                    WritingCharacter.DRAMATIC -> oklchToHex(Oklch((textOklch.l - 0.07).coerceIn(0.05, 0.15), (accentOklch.c * 0.45).coerceIn(0.04, 0.10), accentOklch.h))
                    else -> oklchToHex(Oklch((textOklch.l - 0.04).coerceIn(0.08, 0.20), (accentOklch.c * 0.35).coerceIn(0.02, 0.08), accentOklch.h))
                }
                val annotationDefault = createOklchColor(0.48, 0.16, 300.0)
                val linkDefault = if (circularHueDistance(accentOklch.h, 240.0) < 30.0) {
                    oklchToHex(Oklch(0.40, 0.17, (accentOklch.h + 35.0) % 360.0))
                } else {
                    createOklchColor(0.42, 0.17, 240.0)
                }

                // Analytics & Metrics Semantics (Independent derivation)
                val analyticsPositiveDefault = createOklchColor(0.46, 0.17, 158.0)
                val analyticsNeutralDefault = mutedText
                val analyticsNegativeDefault = createOklchColor(0.48, 0.20, 12.0)
                val analyticsSeries1Default = if (circularHueDistance(accentOklch.h, 235.0) < 30.0) {
                    oklchToHex(Oklch(0.48, 0.18, (accentOklch.h + 40.0) % 360.0))
                } else {
                    oklchToHex(Oklch(0.48, 0.18, 235.0))
                }
                val analyticsSeries2Default = oklchToHex(Oklch(0.48, 0.18, 280.0))
                val analyticsSeries3Default = oklchToHex(Oklch(0.50, 0.15, 195.0))
                val analyticsTargetDefault = oklchToHex(Oklch(0.55, 0.16, 80.0))
                val analyticsWarningDefault = createOklchColor(0.53, 0.17, 68.0)

                // Worldbuilding Lore Entity Semantics (Independent derivation)
                val worldCharacterDefault = if (circularHueDistance(accentOklch.h, 350.0) < 25.0) {
                    oklchToHex(Oklch(0.50, 0.18, (accentOklch.h + 35.0) % 360.0))
                } else {
                    oklchToHex(Oklch(0.50, 0.18, 350.0))
                }
                val worldLocationDefault = oklchToHex(Oklch(0.48, 0.16, 138.0))
                val worldFactionDefault = oklchToHex(Oklch(0.46, 0.17, 250.0))
                val worldItemDefault = oklchToHex(Oklch(0.54, 0.17, 78.0))
                val worldLoreDefault = oklchToHex(Oklch(0.48, 0.16, 295.0))
                val worldEventDefault = oklchToHex(Oklch(0.50, 0.18, 325.0))
                val worldRelationshipDefault = oklchToHex(Oklch(0.50, 0.14, 178.0))

                val rawLightColors = ThemeColors(
                    background = bgHex,
                    surfaceLowest = surfaceLowest,
                    surface = surface,
                    surfaceRaised = surfaceRaised,
                    surfaceOverlay = surfaceOverlay,
                    text = effectiveTextHex,
                    mutedText = mutedText,
                    subtleText = subtleText,
                    accent = accentHex,
                    secondary = secondaryDefault,
                    tertiary = tertiaryDefault,
                    success = successDefault,
                    warning = warningDefault,
                    error = errorDefault,
                    info = infoDefault,
                    specialHighlight = specialHighlightDefault,
                    accentMuted = accentMuted,
                    selection = selection,
                    border = borderNormal,
                    borderSubtle = borderSubtle,
                    borderProminent = borderProminent,
                    focus = focusDefault,
                    dialogueText = dialogueDefault,
                    monologueText = monologueDefault,
                    headingText = headingDefault,
                    annotation = annotationDefault,
                    link = linkDefault,
                    analyticsPositive = analyticsPositiveDefault,
                    analyticsNeutral = analyticsNeutralDefault,
                    analyticsNegative = analyticsNegativeDefault,
                    analyticsSeries1 = analyticsSeries1Default,
                    analyticsSeries2 = analyticsSeries2Default,
                    analyticsSeries3 = analyticsSeries3Default,
                    analyticsTarget = analyticsTargetDefault,
                    analyticsWarning = analyticsWarningDefault,
                    worldCharacter = worldCharacterDefault,
                    worldLocation = worldLocationDefault,
                    worldFaction = worldFactionDefault,
                    worldItem = worldItemDefault,
                    worldLore = worldLoreDefault,
                    worldEvent = worldEventDefault,
                    worldRelationship = worldRelationshipDefault,
                    toolbar = surface,
                    toolbarText = effectiveTextHex
                )
                resolveSemanticCollisions(rawLightColors, isDark = false)
            }
        }

        fun circularHueDistance(h1: Double, h2: Double): Double {
            val d = kotlin.math.abs(h1 - h2) % 360.0
            return if (d > 180.0) 360.0 - d else d
        }

        /**
         * Part 23 Semantic Collision Detection & Resolution.
         * Enforces strict priority order using iterative relaxation:
         * PROSE / CORE READABILITY > PRIMARY INTERACTION > STATUS SEMANTICS > WRITING SEMANTICS > ANALYTICS / WORLD > DECORATIVE ROLES
         */
        fun resolveSemanticCollisions(colors: ThemeColors, isDark: Boolean): ThemeColors {
            val textOklch = colorToOklch(parseColor(colors.text))
            val accentOklch = colorToOklch(parseColor(colors.accent))

            var successOklch = colorToOklch(parseColor(colors.success))
            var warningOklch = colorToOklch(parseColor(colors.warning))
            var errorOklch = colorToOklch(parseColor(colors.error))
            var infoOklch = colorToOklch(parseColor(colors.info))
            val highlightOklch = colorToOklch(parseColor(colors.specialHighlight))

            var dialogueOklch = colorToOklch(parseColor(colors.dialogueText))
            var monologueOklch = colorToOklch(parseColor(colors.monologueText))
            var headingOklch = colorToOklch(parseColor(colors.headingText))

            var s1Oklch = colorToOklch(parseColor(colors.analyticsSeries1))
            var s2Oklch = colorToOklch(parseColor(colors.analyticsSeries2))
            var s3Oklch = colorToOklch(parseColor(colors.analyticsSeries3))
            var targetOklch = colorToOklch(parseColor(colors.analyticsTarget))
            var analyticsWarnOklch = colorToOklch(parseColor(colors.analyticsWarning))

            var charOklch = colorToOklch(parseColor(colors.worldCharacter))
            var locOklch = colorToOklch(parseColor(colors.worldLocation))
            var factionOklch = colorToOklch(parseColor(colors.worldFaction))
            var itemOklch = colorToOklch(parseColor(colors.worldItem))
            var loreOklch = colorToOklch(parseColor(colors.worldLore))
            var eventOklch = colorToOklch(parseColor(colors.worldEvent))
            var relOklch = colorToOklch(parseColor(colors.worldRelationship))

            // 1. Primary Interaction vs Status Semantics (Primary has priority over Status)
            if (circularHueDistance(accentOklch.h, successOklch.h) < 32.0 && kotlin.math.abs(accentOklch.l - successOklch.l) < 0.20) {
                // Adjust success: shift hue towards mint/emerald
                successOklch = Oklch(successOklch.l, successOklch.c, (successOklch.h + 35.0) % 360.0)
            }
            if (circularHueDistance(accentOklch.h, warningOklch.h) < 32.0 && kotlin.math.abs(accentOklch.l - warningOklch.l) < 0.20) {
                // Adjust warning: shift hue towards amber/tangerine
                warningOklch = Oklch(warningOklch.l, warningOklch.c, (warningOklch.h - 32.0 + 360.0) % 360.0)
            }
            if (circularHueDistance(accentOklch.h, errorOklch.h) < 30.0 && kotlin.math.abs(accentOklch.l - errorOklch.l) < 0.20) {
                // Adjust error: shift hue towards ruby/crimson
                errorOklch = Oklch(errorOklch.l, errorOklch.c, (errorOklch.h - 30.0 + 360.0) % 360.0)
            }
            if (circularHueDistance(accentOklch.h, infoOklch.h) < 30.0) {
                // Adjust info: shift hue towards cyan/sky
                infoOklch = Oklch(infoOklch.l, infoOklch.c, (infoOklch.h - 30.0 + 360.0) % 360.0)
            }

            // 2. Writing Semantics vs Status / Prose / Interaction
            // Dialogue vs Highlight: Status has priority over writing
            if (circularHueDistance(dialogueOklch.h, highlightOklch.h) < 24.0) {
                dialogueOklch = if (isDark) {
                    Oklch(0.92, 0.14, 98.0) // Citron
                } else {
                    Oklch(0.44, 0.16, 38.0) // Terracotta
                }
            }
            if (circularHueDistance(dialogueOklch.h, infoOklch.h) < 22.0) {
                dialogueOklch = Oklch(dialogueOklch.l, dialogueOklch.c, (dialogueOklch.h + 35.0) % 360.0)
            }
            // Dialogue vs Prose: Prose has priority
            if (kotlin.math.abs(dialogueOklch.l - textOklch.l) < 0.08 && circularHueDistance(dialogueOklch.h, textOklch.h) < 25.0) {
                dialogueOklch = Oklch(
                    if (isDark) (textOklch.l - 0.08).coerceAtLeast(0.80) else (textOklch.l + 0.18).coerceAtMost(0.48),
                    maxOf(dialogueOklch.c, 0.12),
                    dialogueOklch.h
                )
            }
            // Monologue vs Prose: Prose has priority
            if (kotlin.math.abs(monologueOklch.l - textOklch.l) < 0.08 && monologueOklch.c < 0.06) {
                monologueOklch = Oklch(
                    if (isDark) (textOklch.l - 0.12).coerceAtLeast(0.72) else (textOklch.l + 0.22).coerceAtMost(0.42),
                    0.09,
                    255.0
                )
            }
            // Heading vs Prose: Prose has priority
            if (kotlin.math.abs(headingOklch.l - textOklch.l) < 0.04) {
                headingOklch = Oklch(
                    if (isDark) (textOklch.l + 0.05).coerceIn(0.92, 0.98) else (textOklch.l - 0.05).coerceIn(0.08, 0.22),
                    headingOklch.c,
                    headingOklch.h
                )
            }

            // 3. Analytics Series Channels (Pairwise distinct hues via iterative relaxation)
            val analyticsList = mutableListOf(s1Oklch, s2Oklch, s3Oklch)
            for (pass in 0 until 3) {
                for (i in 0 until analyticsList.size) {
                    for (j in (i + 1) until analyticsList.size) {
                        if (circularHueDistance(analyticsList[i].h, analyticsList[j].h) < 32.0) {
                            analyticsList[j] = Oklch(analyticsList[j].l, analyticsList[j].c, (analyticsList[j].h + 38.0) % 360.0)
                        }
                    }
                }
            }
            s1Oklch = analyticsList[0]
            s2Oklch = analyticsList[1]
            s3Oklch = analyticsList[2]

            if (circularHueDistance(targetOklch.h, analyticsWarnOklch.h) < 22.0) {
                analyticsWarnOklch = Oklch(analyticsWarnOklch.l, analyticsWarnOklch.c, (analyticsWarnOklch.h - 28.0 + 360.0) % 360.0)
            }

            // 4. World Categories Pairwise Hue Separation (>= 22.0 deg via iterative relaxation)
            val worldList = mutableListOf(locOklch, factionOklch, itemOklch, loreOklch, eventOklch, relOklch, charOklch)
            for (pass in 0 until 3) {
                for (i in 0 until worldList.size) {
                    if (circularHueDistance(accentOklch.h, worldList[i].h) < 20.0) {
                        worldList[i] = Oklch(worldList[i].l, worldList[i].c, (worldList[i].h + 26.0) % 360.0)
                    }
                    for (j in (i + 1) until worldList.size) {
                        if (circularHueDistance(worldList[i].h, worldList[j].h) < 22.0) {
                            worldList[j] = Oklch(worldList[j].l, worldList[j].c, (worldList[j].h + 30.0) % 360.0)
                        }
                    }
                }
            }
            locOklch = worldList[0]
            factionOklch = worldList[1]
            itemOklch = worldList[2]
            loreOklch = worldList[3]
            eventOklch = worldList[4]
            relOklch = worldList[5]
            charOklch = worldList[6]

            return colors.copy(
                success = oklchToHex(successOklch),
                warning = oklchToHex(warningOklch),
                error = oklchToHex(errorOklch),
                info = oklchToHex(infoOklch),
                dialogueText = oklchToHex(dialogueOklch),
                monologueText = oklchToHex(monologueOklch),
                headingText = oklchToHex(headingOklch),
                analyticsSeries1 = oklchToHex(s1Oklch),
                analyticsSeries2 = oklchToHex(s2Oklch),
                analyticsSeries3 = oklchToHex(s3Oklch),
                analyticsWarning = oklchToHex(analyticsWarnOklch),
                worldCharacter = oklchToHex(charOklch),
                worldLocation = oklchToHex(locOklch),
                worldFaction = oklchToHex(factionOklch),
                worldItem = oklchToHex(itemOklch),
                worldLore = oklchToHex(loreOklch),
                worldEvent = oklchToHex(eventOklch),
                worldRelationship = oklchToHex(relOklch)
            )
        }

        /**
         * Merges handcrafted reference colors (e.g. from built-in themes) over generated defaults,
         * ensuring handcrafted aesthetic decisions are preserved while any missing semantic roles are
         * deterministically populated.
         */
        fun mergeHandcraftedOverDefaults(handcrafted: ThemeColors, defaults: ThemeColors): ThemeColors {
            return handcrafted.copy(
                surfaceLowest = handcrafted.surfaceLowest.takeIf { it.isNotBlank() && it != handcrafted.background } ?: defaults.surfaceLowest,
                surface = handcrafted.surface.takeIf { it.isNotBlank() && it != handcrafted.background } ?: defaults.surface,
                surfaceRaised = handcrafted.surfaceRaised.takeIf { it.isNotBlank() && it != handcrafted.surface } ?: defaults.surfaceRaised,
                surfaceOverlay = handcrafted.surfaceOverlay.takeIf { it.isNotBlank() && it != handcrafted.surface } ?: defaults.surfaceOverlay,
                mutedText = handcrafted.mutedText.takeIf { it.isNotBlank() && it != handcrafted.text } ?: defaults.mutedText,
                subtleText = handcrafted.subtleText.takeIf { it.isNotBlank() && it != handcrafted.mutedText } ?: defaults.subtleText,
                secondary = handcrafted.secondary.takeIf { it.isNotBlank() && it != handcrafted.accent } ?: defaults.secondary,
                tertiary = handcrafted.tertiary.takeIf { it.isNotBlank() && it != handcrafted.accent } ?: defaults.tertiary,
                accentMuted = handcrafted.accentMuted.takeIf { it.isNotBlank() } ?: defaults.accentMuted,
                selection = handcrafted.selection.takeIf { it.isNotBlank() } ?: defaults.selection,
                border = handcrafted.border.takeIf { it.isNotBlank() } ?: defaults.border,
                borderSubtle = handcrafted.borderSubtle.takeIf { it.isNotBlank() } ?: defaults.borderSubtle,
                borderProminent = handcrafted.borderProminent.takeIf { it.isNotBlank() } ?: defaults.borderProminent,
                focus = handcrafted.focus.takeIf { it.isNotBlank() } ?: handcrafted.borderProminent.takeIf { it.isNotBlank() } ?: defaults.focus,
                success = handcrafted.success.takeIf { it.isNotBlank() } ?: defaults.success,
                warning = handcrafted.warning.takeIf { it.isNotBlank() } ?: defaults.warning,
                error = handcrafted.error.takeIf { it.isNotBlank() } ?: defaults.error,
                info = handcrafted.info.takeIf { it.isNotBlank() } ?: defaults.info,
                specialHighlight = handcrafted.specialHighlight.takeIf { it.isNotBlank() } ?: defaults.specialHighlight,
                dialogueText = handcrafted.dialogueText.takeIf { it.isNotBlank() } ?: defaults.dialogueText,
                monologueText = handcrafted.monologueText.takeIf { it.isNotBlank() } ?: defaults.monologueText,
                headingText = handcrafted.headingText.takeIf { it.isNotBlank() } ?: defaults.headingText,
                annotation = handcrafted.annotation.takeIf { it.isNotBlank() } ?: defaults.annotation,
                link = handcrafted.link.takeIf { it.isNotBlank() } ?: defaults.link,
                analyticsPositive = handcrafted.analyticsPositive.takeIf { it.isNotBlank() } ?: defaults.analyticsPositive,
                analyticsNeutral = handcrafted.analyticsNeutral.takeIf { it.isNotBlank() } ?: defaults.analyticsNeutral,
                analyticsNegative = handcrafted.analyticsNegative.takeIf { it.isNotBlank() } ?: defaults.analyticsNegative,
                analyticsSeries1 = handcrafted.analyticsSeries1.takeIf { it.isNotBlank() } ?: defaults.analyticsSeries1,
                analyticsSeries2 = handcrafted.analyticsSeries2.takeIf { it.isNotBlank() } ?: defaults.analyticsSeries2,
                analyticsSeries3 = handcrafted.analyticsSeries3.takeIf { it.isNotBlank() } ?: defaults.analyticsSeries3,
                analyticsTarget = handcrafted.analyticsTarget.takeIf { it.isNotBlank() } ?: defaults.analyticsTarget,
                analyticsWarning = handcrafted.analyticsWarning.takeIf { it.isNotBlank() } ?: defaults.analyticsWarning,
                worldCharacter = handcrafted.worldCharacter.takeIf { it.isNotBlank() } ?: defaults.worldCharacter,
                worldLocation = handcrafted.worldLocation.takeIf { it.isNotBlank() } ?: defaults.worldLocation,
                worldFaction = handcrafted.worldFaction.takeIf { it.isNotBlank() } ?: defaults.worldFaction,
                worldItem = handcrafted.worldItem.takeIf { it.isNotBlank() } ?: defaults.worldItem,
                worldLore = handcrafted.worldLore.takeIf { it.isNotBlank() } ?: defaults.worldLore,
                worldEvent = handcrafted.worldEvent.takeIf { it.isNotBlank() } ?: defaults.worldEvent,
                worldRelationship = handcrafted.worldRelationship.takeIf { it.isNotBlank() } ?: defaults.worldRelationship,
                toolbar = handcrafted.toolbar.takeIf { it.isNotBlank() } ?: defaults.toolbar,
                toolbarText = handcrafted.toolbarText.takeIf { it.isNotBlank() } ?: defaults.toolbarText
            )
        }

        /**
         * Applies explicit user overrides onto a base ThemeColors instance.
         */
        fun applyOverrides(base: ThemeColors, overrides: ThemeColorOverrides): ThemeColors {
            return base.copy(
                surfaceLowest = overrides.surfaceLowest?.takeIf { it.isNotBlank() } ?: base.surfaceLowest,
                surface = overrides.surface?.takeIf { it.isNotBlank() } ?: base.surface,
                surfaceRaised = overrides.surfaceRaised?.takeIf { it.isNotBlank() } ?: base.surfaceRaised,
                surfaceOverlay = overrides.surfaceOverlay?.takeIf { it.isNotBlank() } ?: base.surfaceOverlay,
                mutedText = overrides.mutedText?.takeIf { it.isNotBlank() } ?: base.mutedText,
                subtleText = overrides.subtleText?.takeIf { it.isNotBlank() } ?: base.subtleText,
                secondary = overrides.secondary?.takeIf { it.isNotBlank() } ?: base.secondary,
                tertiary = overrides.tertiary?.takeIf { it.isNotBlank() } ?: base.tertiary,
                accentMuted = overrides.accentMuted?.takeIf { it.isNotBlank() } ?: base.accentMuted,
                selection = overrides.selection?.takeIf { it.isNotBlank() } ?: base.selection,
                borderSubtle = overrides.borderSubtle?.takeIf { it.isNotBlank() } ?: base.borderSubtle,
                border = overrides.border?.takeIf { it.isNotBlank() } ?: base.border,
                borderProminent = overrides.borderProminent?.takeIf { it.isNotBlank() } ?: base.borderProminent,
                focus = overrides.focus?.takeIf { it.isNotBlank() } ?: base.focus.takeIf { it.isNotBlank() } ?: base.borderProminent,
                success = overrides.success?.takeIf { it.isNotBlank() } ?: base.success,
                warning = overrides.warning?.takeIf { it.isNotBlank() } ?: base.warning,
                error = overrides.error?.takeIf { it.isNotBlank() } ?: base.error,
                specialHighlight = overrides.specialHighlight?.takeIf { it.isNotBlank() } ?: base.specialHighlight,
                info = overrides.info?.takeIf { it.isNotBlank() } ?: base.info,
                dialogueText = overrides.dialogueText?.takeIf { it.isNotBlank() } ?: base.dialogueText,
                monologueText = overrides.monologueText?.takeIf { it.isNotBlank() } ?: base.monologueText,
                headingText = overrides.headingText?.takeIf { it.isNotBlank() } ?: base.headingText,
                annotation = overrides.annotation?.takeIf { it.isNotBlank() } ?: base.annotation,
                link = overrides.link?.takeIf { it.isNotBlank() } ?: base.link,
                analyticsPositive = overrides.analyticsPositive?.takeIf { it.isNotBlank() } ?: base.analyticsPositive,
                analyticsNeutral = overrides.analyticsNeutral?.takeIf { it.isNotBlank() } ?: base.analyticsNeutral,
                analyticsNegative = overrides.analyticsNegative?.takeIf { it.isNotBlank() } ?: base.analyticsNegative,
                analyticsSeries1 = overrides.analyticsSeries1?.takeIf { it.isNotBlank() } ?: base.analyticsSeries1,
                analyticsSeries2 = overrides.analyticsSeries2?.takeIf { it.isNotBlank() } ?: base.analyticsSeries2,
                analyticsSeries3 = overrides.analyticsSeries3?.takeIf { it.isNotBlank() } ?: base.analyticsSeries3,
                analyticsTarget = overrides.analyticsTarget?.takeIf { it.isNotBlank() } ?: base.analyticsTarget,
                analyticsWarning = overrides.analyticsWarning?.takeIf { it.isNotBlank() } ?: base.analyticsWarning,
                worldCharacter = overrides.worldCharacter?.takeIf { it.isNotBlank() } ?: base.worldCharacter,
                worldLocation = overrides.worldLocation?.takeIf { it.isNotBlank() } ?: base.worldLocation,
                worldFaction = overrides.worldFaction?.takeIf { it.isNotBlank() } ?: base.worldFaction,
                worldItem = overrides.worldItem?.takeIf { it.isNotBlank() } ?: base.worldItem,
                worldLore = overrides.worldLore?.takeIf { it.isNotBlank() } ?: base.worldLore,
                worldEvent = overrides.worldEvent?.takeIf { it.isNotBlank() } ?: base.worldEvent,
                worldRelationship = overrides.worldRelationship?.takeIf { it.isNotBlank() } ?: base.worldRelationship,
                toolbar = overrides.surface?.takeIf { it.isNotBlank() } ?: base.surface,
                toolbarText = base.toolbarText
            )
        }

        /**
         * Phase 1 Resolution Pipeline:
         * Resolves the full ThemeColors by layering explicit User Overrides onto Generated Defaults.
         *
         * Resolution precedence:
         * 1. User Override (if non-null and not blank)
         * 2. Generated Default (from OKLCH derivation)
         * 3. Fallback token
         */
        fun resolveThemeColors(
            sources: ThemeSourcePalette,
            overrides: ThemeColorOverrides? = null,
            isDark: Boolean,
            metadata: ThemeGenerationMetadata? = null
        ): ThemeColors {
            val defaults = generateThemeDefaults(sources, isDark, metadata)
            if (overrides == null || overrides.isEmpty()) {
                return defaults
            }
            return applyOverrides(defaults, overrides)
        }

        fun resolveThemeColors(
            bgHex: String,
            textHex: String,
            accentHex: String,
            isDark: Boolean,
            overrides: ThemeColorOverrides? = null
        ): ThemeColors {
            return resolveThemeColors(
                sources = ThemeSourcePalette(bgHex, textHex, accentHex),
                overrides = overrides,
                isDark = isDark
            )
        }

        /**
         * Resolves an entire AppTheme instance to its authoritative resolved colors.
         * For built-in themes, preserves handcrafted reference colors while populating any missing roles.
         * For custom themes, resolves deterministic OKLCH defaults from foundation sources and layers overrides.
         */
        fun resolveTheme(theme: AppTheme): AppTheme {
            val defaults = generateThemeDefaults(theme.sourcePalette(), theme.isDark, theme.generationMetadata)
            val baseColors = if (theme.builtIn) {
                mergeHandcraftedOverDefaults(theme.colors, defaults)
            } else {
                defaults
            }
            val resolvedColors = if (theme.overrides != null && theme.overrides.isNotEmpty()) {
                applyOverrides(baseColors, theme.overrides)
            } else {
                baseColors
            }
            return theme.copy(colors = resolvedColors)
        }

        /**
         * Part 28 Distinction: Editing foundation colors.
         * Foundation edits update the root sources and re-run deterministic generation
         * while strictly preserving existing manual semantic overrides!
         */
        fun updateFoundationColors(
            theme: AppTheme,
            newBg: String? = null,
            newText: String? = null,
            newAccent: String? = null
        ): AppTheme {
            val currentSources = theme.sourcePalette()
            val updatedSources = currentSources.copy(
                background = newBg ?: currentSources.background,
                text = newText ?: currentSources.text,
                accent = newAccent ?: currentSources.accent
            )
            val newIsDark = isDarkColor(updatedSources.background)
            val newDefaults = generateThemeDefaults(updatedSources, newIsDark, theme.generationMetadata)
            val resolvedColors = if (theme.overrides != null && theme.overrides.isNotEmpty()) {
                applyOverrides(newDefaults, theme.overrides)
            } else {
                newDefaults
            }
            return theme.copy(
                isDark = newIsDark,
                colors = resolvedColors
            )
        }

        /**
         * Part 28 Distinction: Editing semantic override.
         * Manual semantic overrides remain authoritative over generated defaults.
         */
        fun updateSemanticOverride(
            theme: AppTheme,
            role: String,
            hexColor: String?
        ): AppTheme {
            val cleanHex = hexColor?.let { sanitizeHexColor(it, "") }?.takeIf { it.isNotBlank() }
            val currentOverrides = theme.overrides ?: ThemeColorOverrides()
            val updatedOverrides = when (role) {
                "surfaceLowest" -> currentOverrides.copy(surfaceLowest = cleanHex)
                "surface" -> currentOverrides.copy(surface = cleanHex)
                "surfaceRaised" -> currentOverrides.copy(surfaceRaised = cleanHex)
                "surfaceOverlay" -> currentOverrides.copy(surfaceOverlay = cleanHex)
                "mutedText" -> currentOverrides.copy(mutedText = cleanHex)
                "subtleText" -> currentOverrides.copy(subtleText = cleanHex)
                "headingText" -> currentOverrides.copy(headingText = cleanHex)
                "dialogueText" -> currentOverrides.copy(dialogueText = cleanHex)
                "monologueText" -> currentOverrides.copy(monologueText = cleanHex)
                "specialHighlight" -> currentOverrides.copy(specialHighlight = cleanHex)
                "annotation" -> currentOverrides.copy(annotation = cleanHex)
                "link" -> currentOverrides.copy(link = cleanHex)
                "secondary" -> currentOverrides.copy(secondary = cleanHex)
                "tertiary" -> currentOverrides.copy(tertiary = cleanHex)
                "success" -> currentOverrides.copy(success = cleanHex)
                "warning" -> currentOverrides.copy(warning = cleanHex)
                "error" -> currentOverrides.copy(error = cleanHex)
                "info" -> currentOverrides.copy(info = cleanHex)
                "border" -> currentOverrides.copy(border = cleanHex)
                "borderSubtle" -> currentOverrides.copy(borderSubtle = cleanHex)
                "borderProminent" -> currentOverrides.copy(borderProminent = cleanHex)
                "focus" -> currentOverrides.copy(focus = cleanHex)
                else -> currentOverrides
            }
            val effectiveOverrides = if (updatedOverrides.isEmpty()) null else updatedOverrides
            return resolveTheme(theme.copy(overrides = effectiveOverrides))
        }

        /**
         * Part 28 Distinction: Changing background image.
         * Attaches or updates wallpaper without silently mutating theme colors or recipe identity.
         */
        fun updateBackgroundImage(
            theme: AppTheme,
            imageUri: String?,
            imageOriginalUri: String? = null,
            bgLuminance: Float = -1f,
            zonalLuminance: List<Float> = emptyList(),
            zonalVariance: List<Float> = emptyList(),
            dominantColor: String? = null,
            zonalColors: List<String> = emptyList(),
            luminanceField: List<Float> = emptyList()
        ): AppTheme {
            return theme.copy(
                backgroundImageUri = imageUri,
                backgroundImageOriginalUri = imageOriginalUri ?: theme.backgroundImageOriginalUri,
                savedBgLuminance = bgLuminance,
                savedZonalLuminance = zonalLuminance,
                savedZonalVariance = zonalVariance,
                savedBgDominantColor = dominantColor,
                savedBgZonalColors = zonalColors,
                savedBgLuminanceField = luminanceField,
                bgMode = if (imageUri != null && theme.bgMode == "color") "image" else theme.bgMode
            )
        }

        /**
         * Part 28 & 29: Explicit Regeneration.
         * Re-runs the generation pipeline with new sources or updated recipe/metadata,
         * while optionally preserving user manual overrides.
         */
        fun regenerateTheme(
            theme: AppTheme,
            newSources: ThemeSourcePalette? = null,
            newMetadata: ThemeGenerationMetadata? = null,
            preserveOverrides: Boolean = true
        ): AppTheme {
            val effectiveSources = newSources ?: theme.sourcePalette()
            val effectiveMetadata = newMetadata ?: theme.generationMetadata ?: ThemeGenerationMetadata()
            val isDark = isDarkColor(effectiveSources.background)
            val newDefaults = generateThemeDefaults(effectiveSources, isDark, effectiveMetadata)
            val effectiveOverrides = if (preserveOverrides) theme.overrides else null
            val finalColors = if (effectiveOverrides != null && effectiveOverrides.isNotEmpty()) {
                applyOverrides(newDefaults, effectiveOverrides)
            } else {
                newDefaults
            }
            return theme.copy(
                isDark = isDark,
                colors = finalColors,
                overrides = effectiveOverrides,
                generationMetadata = effectiveMetadata
            )
        }

        /**
         * Part 42: Material 3 ColorScheme Bridge.
         * Pure, authoritative mapping from AppTheme semantic roles into Material 3 ColorScheme.
         * Does not force domain-specific writing or analytics tokens into standard component roles.
         */
        fun toMaterialColorScheme(theme: AppTheme): ColorScheme {
            val resolved = resolveTheme(theme)
            val c = resolved.colors
            val isDark = resolved.isDark

            val primary = ComposeColor(parseColor(c.accent))
            val onPrimary = ComposeColor(if (isDarkColor(c.accent)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            val primaryContainer = ComposeColor(parseColor(c.accentMuted))
            val onPrimaryContainer = ComposeColor(parseColor(c.text))

            val secondary = ComposeColor(parseColor(c.secondary))
            val onSecondary = ComposeColor(if (isDarkColor(c.secondary)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            val secondaryContainer = ComposeColor(parseColor(c.surfaceRaised))
            val onSecondaryContainer = ComposeColor(parseColor(c.text))

            val tertiary = ComposeColor(parseColor(c.tertiary))
            val onTertiary = ComposeColor(if (isDarkColor(c.tertiary)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            val tertiaryContainer = ComposeColor(parseColor(c.surfaceOverlay))
            val onTertiaryContainer = ComposeColor(parseColor(c.text))

            val background = ComposeColor(parseColor(c.background))
            val onBackground = ComposeColor(parseColor(c.text))

            val surface = ComposeColor(parseColor(c.surface))
            val onSurface = ComposeColor(parseColor(c.text))

            val surfaceVariant = ComposeColor(parseColor(c.surfaceLowest))
            val onSurfaceVariant = ComposeColor(parseColor(c.mutedText))

            val surfaceContainerLowest = ComposeColor(parseColor(c.surfaceLowest))
            val surfaceContainerLow = ComposeColor(parseColor(c.background))
            val surfaceContainer = ComposeColor(parseColor(c.surface))
            val surfaceContainerHigh = ComposeColor(parseColor(c.surfaceRaised))
            val surfaceContainerHighest = ComposeColor(parseColor(c.surfaceOverlay))

            val outline = ComposeColor(parseColor(c.border))
            val outlineVariant = ComposeColor(parseColor(c.borderSubtle))

            val error = ComposeColor(parseColor(c.error))
            val onError = ComposeColor(if (isDarkColor(c.error)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            val errorContainer = ComposeColor(parseColor(c.error)).copy(alpha = 0.20f)
            val onErrorContainer = ComposeColor(parseColor(c.text))

            val inverseSurface = ComposeColor(parseColor(c.text))
            val inverseOnSurface = ComposeColor(parseColor(c.background))
            val inversePrimary = primary

            val scrim = ComposeColor.Black.copy(alpha = 0.32f)
            val surfaceTint = primary

            return if (isDark) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    inversePrimary = inversePrimary,
                    secondary = secondary,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = onSecondaryContainer,
                    tertiary = tertiary,
                    onTertiary = onTertiary,
                    tertiaryContainer = tertiaryContainer,
                    onTertiaryContainer = onTertiaryContainer,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    surfaceVariant = surfaceVariant,
                    onSurfaceVariant = onSurfaceVariant,
                    surfaceTint = surfaceTint,
                    inverseSurface = inverseSurface,
                    inverseOnSurface = inverseOnSurface,
                    error = error,
                    onError = onError,
                    errorContainer = errorContainer,
                    onErrorContainer = onErrorContainer,
                    outline = outline,
                    outlineVariant = outlineVariant,
                    scrim = scrim,
                    surfaceContainerLowest = surfaceContainerLowest,
                    surfaceContainerLow = surfaceContainerLow,
                    surfaceContainer = surfaceContainer,
                    surfaceContainerHigh = surfaceContainerHigh,
                    surfaceContainerHighest = surfaceContainerHighest
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    inversePrimary = inversePrimary,
                    secondary = secondary,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = onSecondaryContainer,
                    tertiary = tertiary,
                    onTertiary = onTertiary,
                    tertiaryContainer = tertiaryContainer,
                    onTertiaryContainer = onTertiaryContainer,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    surfaceVariant = surfaceVariant,
                    onSurfaceVariant = onSurfaceVariant,
                    surfaceTint = surfaceTint,
                    inverseSurface = inverseSurface,
                    inverseOnSurface = inverseOnSurface,
                    error = error,
                    onError = onError,
                    errorContainer = errorContainer,
                    onErrorContainer = onErrorContainer,
                    outline = outline,
                    outlineVariant = outlineVariant,
                    scrim = scrim,
                    surfaceContainerLowest = surfaceContainerLowest,
                    surfaceContainerLow = surfaceContainerLow,
                    surfaceContainer = surfaceContainer,
                    surfaceContainerHigh = surfaceContainerHigh,
                    surfaceContainerHighest = surfaceContainerHighest
                )
            }
        }

        /**
         * Backward compatibility wrapper for deriveThemeColors.
         */
        fun deriveThemeColors(
            bgHex: String,
            textHex: String,
            accentHex: String,
            isDark: Boolean,
            base: ThemeColors? = null
        ): ThemeColors {
            return resolveThemeColors(
                bgHex = bgHex,
                textHex = textHex,
                accentHex = accentHex,
                isDark = isDark,
                overrides = null
            )
        }

        fun resolveTypeface(context: Context, fontFamilyKey: String): Typeface {
            val fontResId = when (fontFamilyKey) {
                "serif", "serif-medium", "serif-bold" -> R.font.playfair_display
                "sans", "sans-medium", "sans-semibold", "sans-bold" -> R.font.inter
                "mono", "mono-medium" -> R.font.jetbrains_mono
                else -> 0
            }
            if (fontResId != 0) {
                try {
                    val tf = ResourcesCompat.getFont(context, fontResId)
                    if (tf != null) {
                        return when (fontFamilyKey) {
                            "serif-bold", "sans-bold" ->
                                Typeface.create(tf, Typeface.BOLD)
                            "serif-medium", "sans-medium", "sans-semibold", "mono-medium" ->
                                if (Build.VERSION.SDK_INT >= 28)
                                    Typeface.create(tf, 500, false)
                                else Typeface.create(tf, Typeface.NORMAL)
                            else -> tf
                        }
                    }
                } catch (_: Exception) {}
            }
            return when {
                fontFamilyKey.startsWith("serif") -> Typeface.SERIF
                fontFamilyKey.startsWith("mono")  -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
        }

        fun lineSpacingMultiplier(key: String): Float = when (key) {
            "compact"  -> 1.4f
            "spacious" -> 2.0f
            else       -> 1.7f  // comfortable
        }

        fun resolveToScribeColors(theme: AppTheme): com.primaloptima.scribe.ui.theme.ScribeColors {
            val resolved = resolveTheme(theme)
            return resolveToScribeColors(resolved.colors, resolved.isDark)
        }

        fun resolveToScribeColors(derived: ThemeColors, isDark: Boolean): com.primaloptima.scribe.ui.theme.ScribeColors {
            val bgCol = ComposeColor(parseColor(derived.background))
            val textCol = ComposeColor(parseColor(derived.text))
            val accentCol = ComposeColor(parseColor(derived.accent))
            val surfaceCol = ComposeColor(parseColor(derived.surface))
            val surfaceRaisedCol = ComposeColor(parseColor(derived.surfaceRaised))
            val surfaceOverlayCol = ComposeColor(parseColor(derived.surfaceOverlay))
            val mutedCol = ComposeColor(parseColor(derived.mutedText))
            val subtleCol = ComposeColor(parseColor(derived.subtleText))
            val borderSubtleCol = ComposeColor(parseColor(derived.borderSubtle))
            val normalBorderCol = ComposeColor(parseColor(derived.border))
            val focusBorderCol = ComposeColor(parseColor(derived.borderProminent))
            val focusInteractiveCol = if (derived.focus.isNotBlank()) ComposeColor(parseColor(derived.focus)) else focusBorderCol
            val selectionCol = if (derived.selection.isNotBlank()) ComposeColor(parseColor(derived.selection)) else accentCol.copy(alpha = 0.25f)

            val dialogueCol = ComposeColor(parseColor(derived.dialogueText))
            val monologueCol = ComposeColor(parseColor(derived.monologueText))
            val headingCol = ComposeColor(parseColor(derived.headingText))
            val secondaryCol = ComposeColor(parseColor(derived.secondary))
            val tertiaryCol = ComposeColor(parseColor(derived.tertiary))
            val annotationCol = if (derived.annotation.isNotBlank()) ComposeColor(parseColor(derived.annotation)) else (if (isDark) ComposeColor(0xFFC084FC) else ComposeColor(0xFF7E22CE))
            val infoCol = if (derived.info.isNotBlank()) ComposeColor(parseColor(derived.info)) else (if (isDark) ComposeColor(0xFF38BDF8) else ComposeColor(0xFF0284C7))
            val linkCol = if (derived.link.isNotBlank()) ComposeColor(parseColor(derived.link)) else accentCol
            val highlightCol = if (derived.specialHighlight.isNotBlank()) ComposeColor(parseColor(derived.specialHighlight)) else (if (isDark) ComposeColor(0xFFE7B85A) else ComposeColor(0xFFB45309))

            val accentMutedCol = if (derived.accentMuted.isNotBlank()) ComposeColor(parseColor(derived.accentMuted)) else accentCol.copy(alpha = 0.15f)

            val onPrimaryCol = ContrastResolver.resolveOnColor(
                container = accentCol,
                preferredForeground = if (isDark) bgCol else textCol,
                minRatio = 4.5,
                isDarkTheme = isDark
            )
            val onPrimaryContainerCol = ContrastResolver.resolveOnColor(
                container = accentMutedCol,
                preferredForeground = textCol,
                minRatio = 3.5,
                isDarkTheme = isDark
            )

            val successBase = if (derived.success.isNotBlank()) ComposeColor(parseColor(derived.success)) else (if (isDark) ComposeColor(0xFF55D18A) else ComposeColor(0xFF2E7D32))
            val successContainerCol = successBase.copy(alpha = if (isDark) 0.16f else 0.12f)
            val onSuccessCol = ContrastResolver.resolveOnColor(
                container = successBase,
                preferredForeground = if (isDark) bgCol else ComposeColor.White,
                minRatio = 4.5,
                isDarkTheme = isDark
            )
            val onSuccessContainerCol = ContrastResolver.resolveContrast(
                background = surfaceCol,
                preferredForeground = if (isDark) successBase else ComposeColor(0xFF1B5E20),
                minRatio = 3.5
            ).color

            val warningBase = if (derived.warning.isNotBlank()) ComposeColor(parseColor(derived.warning)) else (if (isDark) ComposeColor(0xFFFFC857) else ComposeColor(0xFFD97706))
            val warningContainerCol = warningBase.copy(alpha = if (isDark) 0.16f else 0.12f)
            val onWarningCol = ContrastResolver.resolveOnColor(
                container = warningBase,
                preferredForeground = if (isDark) ComposeColor(0xFF141416) else ComposeColor.Black,
                minRatio = 4.5,
                isDarkTheme = isDark
            )
            val onWarningContainerCol = ContrastResolver.resolveContrast(
                background = surfaceCol,
                preferredForeground = if (isDark) warningBase else ComposeColor(0xFF92400E),
                minRatio = 3.5
            ).color

            val errorBase = if (derived.error.isNotBlank()) ComposeColor(parseColor(derived.error)) else (if (isDark) ComposeColor(0xFFFF6B7A) else ComposeColor(0xFFDC2626))
            val errorContainerCol = errorBase.copy(alpha = if (isDark) 0.16f else 0.12f)
            val onErrorCol = ContrastResolver.resolveOnColor(
                container = errorBase,
                preferredForeground = if (isDark) bgCol else ComposeColor.White,
                minRatio = 4.5,
                isDarkTheme = isDark
            )
            val onErrorContainerCol = ContrastResolver.resolveContrast(
                background = surfaceCol,
                preferredForeground = if (isDark) errorBase else ComposeColor(0xFF991B1B),
                minRatio = 3.5
            ).color

            val infoBase = infoCol
            val infoContainerCol = infoBase.copy(alpha = if (isDark) 0.16f else 0.12f)
            val onInfoCol = ContrastResolver.resolveOnColor(
                container = infoBase,
                preferredForeground = if (isDark) bgCol else ComposeColor.White,
                minRatio = 4.5,
                isDarkTheme = isDark
            )
            val onInfoContainerCol = ContrastResolver.resolveContrast(
                background = surfaceCol,
                preferredForeground = if (isDark) infoBase else ComposeColor(0xFF075985),
                minRatio = 3.5
            ).color

            return com.primaloptima.scribe.ui.theme.ScribeColors(
                surfaces = com.primaloptima.scribe.ui.theme.SurfaceColors(
                    background = bgCol,
                    surfaceLowest = ComposeColor(parseColor(derived.surfaceLowest)),
                    surface = surfaceCol,
                    surfaceRaised = surfaceRaisedCol,
                    surfaceOverlay = surfaceOverlayCol,
                    surfaceSelected = if (isDark) surfaceRaisedCol.copy(alpha = 0.6f) else accentMutedCol,
                    surfacePressed = surfaceCol.copy(alpha = 0.8f)
                ),
                content = com.primaloptima.scribe.ui.theme.ContentColors(
                    primary = textCol,
                    secondary = mutedCol,
                    tertiary = subtleCol,
                    disabled = textCol.copy(alpha = 0.38f),
                    onAccent = onPrimaryCol
                ),
                interaction = com.primaloptima.scribe.ui.theme.InteractionColors(
                    primary = accentCol,
                    primaryContainer = accentMutedCol,
                    onPrimary = onPrimaryCol,
                    onPrimaryContainer = onPrimaryContainerCol,
                    secondary = secondaryCol,
                    tertiary = tertiaryCol,
                    selection = selectionCol,
                    focus = focusInteractiveCol,
                    link = linkCol
                ),
                semantic = com.primaloptima.scribe.ui.theme.SemanticStatusColors(
                    success = successBase,
                    onSuccess = onSuccessCol,
                    successContainer = successContainerCol,
                    onSuccessContainer = onSuccessContainerCol,
                    warning = warningBase,
                    onWarning = onWarningCol,
                    warningContainer = warningContainerCol,
                    onWarningContainer = onWarningContainerCol,
                    error = errorBase,
                    onError = onErrorCol,
                    errorContainer = errorContainerCol,
                    onErrorContainer = onErrorContainerCol,
                    info = infoBase,
                    onInfo = onInfoCol,
                    infoContainer = infoContainerCol,
                    onInfoContainer = onInfoContainerCol
                ),
                writing = com.primaloptima.scribe.ui.theme.WritingColors(
                    prose = textCol,
                    dialogue = dialogueCol,
                    monologue = monologueCol,
                    heading = headingCol,
                    annotation = annotationCol,
                    highlight = highlightCol
                ),
                analytics = com.primaloptima.scribe.ui.theme.AnalyticsColors(
                    positive = if (derived.analyticsPositive.isNotBlank()) ComposeColor(parseColor(derived.analyticsPositive)) else (if (isDark) ComposeColor(0xFF10B981) else ComposeColor(0xFF059669)),
                    neutral = if (derived.analyticsNeutral.isNotBlank()) ComposeColor(parseColor(derived.analyticsNeutral)) else mutedCol,
                    negative = if (derived.analyticsNegative.isNotBlank()) ComposeColor(parseColor(derived.analyticsNegative)) else (if (isDark) ComposeColor(0xFFEF4444) else ComposeColor(0xFFDC2626)),
                    series1 = if (derived.analyticsSeries1.isNotBlank()) ComposeColor(parseColor(derived.analyticsSeries1)) else accentCol,
                    series2 = if (derived.analyticsSeries2.isNotBlank()) ComposeColor(parseColor(derived.analyticsSeries2)) else (if (isDark) ComposeColor(0xFF8B5CF6) else ComposeColor(0xFF6D28D9)),
                    series3 = if (derived.analyticsSeries3.isNotBlank()) ComposeColor(parseColor(derived.analyticsSeries3)) else (if (isDark) ComposeColor(0xFF06B6D4) else ComposeColor(0xFF0E7490)),
                    target = if (derived.analyticsTarget.isNotBlank()) ComposeColor(parseColor(derived.analyticsTarget)) else (if (isDark) ComposeColor(0xFFF59E0B) else ComposeColor(0xFFD97706)),
                    warning = if (derived.analyticsWarning.isNotBlank()) ComposeColor(parseColor(derived.analyticsWarning)) else (if (isDark) ComposeColor(0xFFF59E0B) else ComposeColor(0xFFD97706))
                ),
                borders = com.primaloptima.scribe.ui.theme.BorderColors(
                    subtle = borderSubtleCol,
                    normal = normalBorderCol,
                    prominent = focusBorderCol
                ),
                world = com.primaloptima.scribe.ui.theme.WorldEntityColors(
                    character = if (derived.worldCharacter.isNotBlank()) ComposeColor(parseColor(derived.worldCharacter)) else accentCol,
                    location = if (derived.worldLocation.isNotBlank()) ComposeColor(parseColor(derived.worldLocation)) else (if (isDark) ComposeColor(0xFF4ADE80) else ComposeColor(0xFF16A34A)),
                    faction = if (derived.worldFaction.isNotBlank()) ComposeColor(parseColor(derived.worldFaction)) else (if (isDark) ComposeColor(0xFF3B82F6) else ComposeColor(0xFF2563EB)),
                    item = if (derived.worldItem.isNotBlank()) ComposeColor(parseColor(derived.worldItem)) else (if (isDark) ComposeColor(0xFFFBBF24) else ComposeColor(0xFFD97706)),
                    lore = if (derived.worldLore.isNotBlank()) ComposeColor(parseColor(derived.worldLore)) else (if (isDark) ComposeColor(0xFFA78BFA) else ComposeColor(0xFF7C3AED)),
                    event = if (derived.worldEvent.isNotBlank()) ComposeColor(parseColor(derived.worldEvent)) else (if (isDark) ComposeColor(0xFFF472B6) else ComposeColor(0xFFDB2777)),
                    relationship = if (derived.worldRelationship.isNotBlank()) ComposeColor(parseColor(derived.worldRelationship)) else (if (isDark) ComposeColor(0xFF2DD4BF) else ComposeColor(0xFF0D9488))
                ),
                isDark = isDark
            )
        }

        fun validateSemanticContrast(theme: AppTheme): com.primaloptima.scribe.ui.theme.ThemeSemanticContrastReport {
            val scribeColors = resolveToScribeColors(theme)
            return com.primaloptima.scribe.ui.theme.validateThemeSemanticContrast(scribeColors)
        }

        fun validateSemanticContrast(
            bgHex: String,
            textHex: String,
            accentHex: String,
            dialogueHex: String? = null,
            monologueHex: String? = null,
            headingHex: String? = null
        ): com.primaloptima.scribe.ui.theme.ThemeSemanticContrastReport {
            val isDark = isDarkColor(bgHex)
            val overrides = ThemeColorOverrides(
                dialogueText = dialogueHex,
                monologueText = monologueHex,
                headingText = headingHex
            )
            val derived = resolveThemeColors(bgHex, textHex, accentHex, isDark, overrides)
            val scribeColors = resolveToScribeColors(derived, isDark)
            return com.primaloptima.scribe.ui.theme.validateThemeSemanticContrast(scribeColors)
        }

        /**
         * Validates the theme against the centralized [ContrastMatrix] and returns a machine-readable [AccessibilityReport].
         */
        fun generateAccessibilityReport(theme: AppTheme): com.primaloptima.scribe.ui.theme.AccessibilityReport {
            val scribeColors = resolveToScribeColors(theme)
            return com.primaloptima.scribe.ui.theme.ContrastMatrix.validate(scribeColors, theme.id)
        }

        /**
         * Validates and repairs any semantic contrast deficiencies using the centralized [ContrastMatrix].
         */
        fun validateAndRepairTheme(theme: AppTheme): Pair<com.primaloptima.scribe.ui.theme.ScribeColors, com.primaloptima.scribe.ui.theme.AccessibilityReport> {
            val scribeColors = resolveToScribeColors(theme)
            return com.primaloptima.scribe.ui.theme.ContrastMatrix.validateAndRepair(scribeColors, theme.id)
        }
    }
}
