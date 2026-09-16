package com.primaloptima.scribe.util.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Shortcut ─────────────────────────────────────────────────────────────────

@Immutable
@Serializable
data class ShortcutAction(
    val id: String,
    val label: String,
    /** "insert" | "wrap" | "pair" */
    val kind: String,
    val payload: String,
    /** Non-null for wrap/pair */
    val closing: String? = null
)

// ── Pinned item ───────────────────────────────────────────────────────────────

@Immutable
@Serializable
data class PinnedItem(
    /** "top" | "bottom" */
    val slot: String,
    val noteId: String
)

// ── Floating window ───────────────────────────────────────────────────────────

// @Stable (not @Immutable): FloatingWindow has var fields (x, y, width, height,
// zOrder, collapsed) mutated during drag/resize. @Stable is the correct promise:
// "mutations will be reflected through Compose state channels."
@Stable
@Serializable
data class FloatingWindow(
    val id: String,
    val noteId: String,
    var x: Float,
    var y: Float,
    var width: Int,
    var height: Int,
    var zOrder: Int,
    var collapsed: Boolean = false
)

// ── External root ─────────────────────────────────────────────────────────────

@Immutable
@Serializable
data class ExternalRoot(
    val uri: String,
    val name: String
)

// ── Outline entry ─────────────────────────────────────────────────────────────

@Immutable
data class OutlineEntry(
    val level: Int,   // 1–4
    val text: String,
    val lineIndex: Int,
    val preview: String? = null
)

// ── App theme ─────────────────────────────────────────────────────────────────

/**
 * Theme schema version constants and evolution documentation.
 */
object ThemeSchema {
    /**
     * Legacy schema version prior to explicit semantic role versioning.
     * Themes without a "schemaVersion" field in JSON deserialize as VERSION_LEGACY (0).
     */
    const val VERSION_LEGACY = 0

    /**
     * Schema Version 1:
     * - Complete 5-tier elevation hierarchy (surfaceLowest, surface, surfaceRaised, surfaceOverlay).
     * - 3-tier boundary hierarchy (borderSubtle, border, borderProminent, focus).
     * - Semantic statuses (success, warning, error, info, specialHighlight).
     * - Editorial writing lexer semantics (dialogueText, monologueText, headingText, annotation, link).
     * - Analytics series semantics (analyticsPositive, analyticsNeutral, analyticsNegative, analyticsSeries1-3, analyticsTarget, analyticsWarning).
     * - Worldbuilding entity semantics (worldCharacter, worldLocation, worldFaction, worldItem, worldLore, worldEvent, worldRelationship).
     * - Explicit ThemeColorOverrides model for user modifications.
     * - Background image luminance & zonal metrics.
     */
    const val VERSION_1 = 1

    /**
     * Schema Version 2 (Session 4):
     * - Complete generation provenance metadata (recipe, image influence, writing character, relationship mode, seed candidate).
     * - Regeneration continuity preserving recipe intent across foundation edits.
     * - Authoritative user override protection during regeneration.
     * - Material 3 component bridge decoupling domain semantics.
     * - Gamut-mapped accessibility and contrast reporting persistence.
     */
    const val VERSION_2 = 2

    /** The current active schema version emitted by the application. */
    const val CURRENT_VERSION = VERSION_2
}

/**
 * Phase 1 Foundation Sources:
 * Authoritative authoring inputs that drive default color generation.
 * Supports intelligent multi-color palette interpretation with optional
 * secondary/tertiary accents and atmospheric color.
 */
@Immutable
@Serializable
data class ThemeSourcePalette(
    val background: String,
    val text: String,
    val accent: String,
    val secondaryAccent: String? = null,
    val tertiaryAccent: String? = null,
    val atmosphericColor: String? = null,
    val visualPalette: VisualThemePalette? = null
)

/**
 * Generation Provenance Metadata:
 * Captures generation intent and source parameters so intelligent themes
 * retain their generative character across session re-edits and persistence.
 */
@Immutable
@Serializable
data class ThemeGenerationMetadata(
    val recipe: ThemeGenerationRecipe = ThemeGenerationRecipe.BALANCED,
    val imageInfluence: ImageInfluence = ImageInfluence.BALANCED,
    val writingCharacter: WritingCharacter = WritingCharacter.NEUTRAL,
    val relationshipMode: ThemeRelationshipMode = ThemeRelationshipMode.THEME_IMAGE,
    val originalAtmosphereHex: String? = null,
    val selectedCandidateHex: String? = null,
    val secondaryAccentHex: String? = null,
    val tertiaryAccentHex: String? = null,
    val highlightHex: String? = null,
    val visualPalette: VisualThemePalette? = null,
    val sourceImageFingerprint: String? = null,
    val generationVersion: Int = 3
) {
    /** True if this theme was generated with an image understanding session */
    val isImageDerived: Boolean
        get() = originalAtmosphereHex != null || selectedCandidateHex != null || sourceImageFingerprint != null || visualPalette != null
}

/**
 * Phase 1 Explicit User Overrides:
 * Explicit representation of user-customized semantic colors.
 * When null, the token resolves to its generated default.
 */
@Immutable
@Serializable
data class ThemeColorOverrides(
    val surfaceLowest: String? = null,
    val surface: String? = null,
    val surfaceRaised: String? = null,
    val surfaceOverlay: String? = null,

    val mutedText: String? = null,
    val subtleText: String? = null,

    val secondary: String? = null,
    val tertiary: String? = null,
    val accentMuted: String? = null,
    val selection: String? = null,

    val border: String? = null,
    val borderSubtle: String? = null,
    val borderProminent: String? = null,
    val focus: String? = null,

    val success: String? = null,
    val warning: String? = null,
    val error: String? = null,
    val info: String? = null,
    val specialHighlight: String? = null,

    val dialogueText: String? = null,
    val monologueText: String? = null,
    val headingText: String? = null,
    val annotation: String? = null,
    val link: String? = null,

    // ── Analytics Overrides ──
    val analyticsPositive: String? = null,
    val analyticsNeutral: String? = null,
    val analyticsNegative: String? = null,
    val analyticsSeries1: String? = null,
    val analyticsSeries2: String? = null,
    val analyticsSeries3: String? = null,
    val analyticsTarget: String? = null,
    val analyticsWarning: String? = null,

    // ── World Entity Overrides ──
    val worldCharacter: String? = null,
    val worldLocation: String? = null,
    val worldFaction: String? = null,
    val worldItem: String? = null,
    val worldLore: String? = null,
    val worldEvent: String? = null,
    val worldRelationship: String? = null
) {
    fun isEmpty(): Boolean =
        surfaceLowest == null && surface == null && surfaceRaised == null && surfaceOverlay == null &&
        mutedText == null && subtleText == null &&
        secondary == null && tertiary == null &&
        accentMuted == null && selection == null &&
        border == null && borderSubtle == null && borderProminent == null && focus == null &&
        success == null && warning == null && error == null && info == null && specialHighlight == null &&
        dialogueText == null && monologueText == null && headingText == null && annotation == null && link == null &&
        analyticsPositive == null && analyticsNeutral == null && analyticsNegative == null &&
        analyticsSeries1 == null && analyticsSeries2 == null && analyticsSeries3 == null &&
        analyticsTarget == null && analyticsWarning == null &&
        worldCharacter == null && worldLocation == null && worldFaction == null &&
        worldItem == null && worldLore == null && worldEvent == null && worldRelationship == null

    fun isNotEmpty(): Boolean = !isEmpty()
}

@Immutable
@Serializable
data class ThemeColors(
    // ── Surfaces & Elevation (5-Tier Perceptual Hierarchy) ──
    val background: String = "#121214",      // L0: Canvas base background
    val surfaceLowest: String = background,  // L1: Recessed gutters, split rails
    val surface: String = background,        // L2: App bars, drawers, primary panels
    val surfaceRaised: String = surface,     // L3: Floating cards, workbench cards
    val surfaceOverlay: String = surface,    // L4: Popovers, elevated menus, dialogs

    // ── Text & Typography Hierarchy ──
    val text: String = "#F4F4F6",            // Primary foreground prose & headers
    val mutedText: String = text,            // Secondary metadata, word counts, subtitles
    val subtleText: String = mutedText,      // Inactive hints, timestamps, subtle counters

    // ── Brand & Interactive Accents (Source Hues) ──
    val accent: String = text,               // Primary interactive controls & carets (Primary source hue)
    val secondary: String = accent,          // Secondary source hue (cool violet/slate)
    val tertiary: String = accent,           // Tertiary source hue (teal/cyan)
    val accentMuted: String = surface,       // Subtle badge & chip background fill
    val selection: String = accent,          // Selection highlight tint

    // ── Semantic Status Source Hues ──
    val success: String = "",                // Semantic success source hue
    val warning: String = "",                // Semantic warning source hue
    val error: String = "",                  // Semantic error source hue
    val info: String = "",                   // Semantic info source hue (neutral notice)
    val specialHighlight: String = "",       // Literary gold / emphasis hue

    // ── Boundaries & Dividers ──
    val border: String = text,               // Standard component boundary (outline)
    val borderSubtle: String = border,       // Subtle 1px structural hairline (outlineVariant)
    val borderProminent: String = accent,    // High-emphasis boundary (active keylines, selected states)
    val focus: String = "",                  // Keyboard navigation focus rings & accessibility outlines (defaults to borderProminent if blank)

    // ── Editorial & Prose Lexer Semantics ──
    val dialogueText: String = accent,       // Spoken dialogue highlighting
    val monologueText: String = text,        // Internal thoughts & reflections
    val headingText: String = accent,        // Chapter & scene headings
    val annotation: String = "",             // Editorial margin notes & commentary
    val link: String = "",                   // Interactive hyperlinks & citations

    // ── Data & Analytics Semantics ──
    val analyticsPositive: String = "",
    val analyticsNeutral: String = "",
    val analyticsNegative: String = "",
    val analyticsSeries1: String = "",
    val analyticsSeries2: String = "",
    val analyticsSeries3: String = "",
    val analyticsTarget: String = "",
    val analyticsWarning: String = "",

    // ── World / Entity Semantics ──
    val worldCharacter: String = "",
    val worldLocation: String = "",
    val worldFaction: String = "",
    val worldItem: String = "",
    val worldLore: String = "",
    val worldEvent: String = "",
    val worldRelationship: String = "",

    // ── Toolbars & Action Bars ──
    val toolbar: String = surface,           // Action bar background
    val toolbarText: String = text           // Action bar foreground
) {
    val focusRing: String get() = focus.ifBlank { borderProminent }
}

// @Stable (not @Immutable): AppTheme instances are replaced wholesale via copy()
// in ThemeEditScreen — no field is mutated in-place, so the contract holds.
// We use @Stable rather than @Immutable because AppTheme instances are reconstructed
// frequently during editing; @Stable lets Compose diff by equals() and skip
// composables that receive an unchanged theme reference.
@Stable
@Serializable
data class AppTheme(
    val id: String = "",
    val name: String = "Custom Theme",
    val isDark: Boolean = false,
    val builtIn: Boolean = false,
    val colors: ThemeColors,
    val overrides: ThemeColorOverrides? = null,
    /** Font family key matching Google Fonts or system fonts */
    val fontFamily: String = "sans",
    val fontSize: Int = 17,
    val lineHeight: Float = 1.7f,
    val letterSpacing: Float = 0.1f,
    val paragraphSpacing: Int = 14,
    val paddingHorizontal: Int = 24,
    val paddingVertical: Int = 20,
    val maxWidth: Int = 720,
    /** Explicit schema version for safe forward-compatible serialization & migration */
    val schemaVersion: Int = ThemeSchema.VERSION_LEGACY,
    val backgroundImageUri: String? = null,
    /** The original full-resolution image the user picked, before cropping.
     *  Preserved so the user can re-crop later without quality loss, and so
     *  the crop can be re-run at a different screen ratio if needed. */
    val backgroundImageOriginalUri: String? = null,
    val backgroundImageOpacity: Float? = 0.35f,
    val overlayEnabled: Boolean = false,
    val overlayColor: String? = null,
    val bgMode: String = "color", // "color" | "image" | "blurred"
    val blurIntensity: Float = 15f,
    val frostedGlassEnabled: Boolean = true,
    /** When false, frosted glass shows pure blur with no surface tint overlay. */
    val frostedTintEnabled: Boolean = true,
    /** Blur radius (dp) for bars, drawers, dialogs in frosted glass mode.
     *  API 31+: live via Haze. Pre-API-31: applied at theme-load time (one-shot bitmap). */
    val frostedBlurRadius: Float = 15f,
    val textAlignment: String = "left", // "left" | "justified" | "center"
    val themeScope: String = "whole_app", // "editor_only" | "whole_app"
    val emoji: String? = null,
    /**
     * Average linear luminance of the background image, computed once at crop-confirm
     * time and stored with the theme. Range [0.0, 1.0]: 0 = black, 1 = white.
     * -1f means not yet computed (no image set, or pre-existing theme without this field).
     * Gson will deserialize old JSON without this field and default it to -1f cleanly.
     *
     * This single float drives text colour, frosted-panel content colour, and
     * accent-colour adaptation at runtime with zero bitmap processing on the device.
     */
    val savedBgLuminance: Float = -1f,
    /**
     * 3x3 Zonal Precomputation Matrix storing average perceptual lightness across:
     * [0: TL, 1: TC (AppBar), 2: TR,
     *  3: ML, 4: MC (Editor), 5: MR,
     *  6: BL, 7: BC (Toolbar), 8: BR]
     * Enables zero-allocation instant first-frame adaptation for top bars, editor, and toolbars.
     */
    val savedZonalLuminance: List<Float> = emptyList(),
    /**
     * 3x3 Zonal Spatial Variance Matrix (RMS Contrast / Standard Deviation).
     * High variance (>= 0.065) automatically activates directional contrast shadows and adaptive micro-scrims.
     */
    val savedZonalVariance: List<Float> = emptyList(),
    /** Global dominant color hex (e.g. "#4A6572") extracted from the background image via Material Color Utilities. */
    @SerialName("savedBgDominantColor")
    val savedBgDominantColor: String? = null,
    /** 3x3 Zonal Dominant Colors (9 hex strings in row-major order: TL, TC, TR, ML, MC, MR, BL, BC, BR). */
    @SerialName("savedBgZonalColors")
    val savedBgZonalColors: List<String> = emptyList(),
    /** 8x8 precomputed box-averaged luminance field for subtle environmental edge-light modulation. */
    @SerialName("savedBgLuminanceField")
    val savedBgLuminanceField: List<Float> = emptyList(),
    /** Provenance metadata for image-generated themes. Null for handcrafted or legacy themes. */
    val generationMetadata: ThemeGenerationMetadata? = null
) {
    /** Alias for generationMetadata for test and caller convenience */
    val metadata: ThemeGenerationMetadata? get() = generationMetadata

    fun sourcePalette(): ThemeSourcePalette = ThemeSourcePalette(
        background = colors.background,
        text = colors.text,
        accent = colors.accent,
        secondaryAccent = generationMetadata?.secondaryAccentHex ?: colors.secondary.takeIf { it != colors.accent && it.isNotBlank() },
        tertiaryAccent = generationMetadata?.tertiaryAccentHex ?: colors.tertiary.takeIf { it != colors.accent && it.isNotBlank() },
        atmosphericColor = generationMetadata?.originalAtmosphereHex,
        visualPalette = generationMetadata?.visualPalette
    )

    fun toFoundationPalette(): ThemeSourcePalette = sourcePalette()
}

// ── SAF scan result ───────────────────────────────────────────────────────────

@Immutable
data class SafFile(
    val uri: String,
    val name: String,
    val ext: String,
    val folderPath: String
)

@Immutable
data class SafFolder(
    val uri: String,
    val relativePath: String
)

@Immutable
data class SafCover(
    val uri: String,
    val folderPath: String,
    val ext: String
)

// Note: SafScanResult contains List<T> fields. @Immutable is correct here because
// these lists are populated once during a SAF scan and never mutated afterward.
// The List<T> stability issue for composable parameters is handled by stability_config.conf.
@Immutable
data class SafScanResult(
    val files: List<SafFile> = emptyList(),
    val folders: List<SafFolder> = emptyList(),
    val covers: List<SafCover> = emptyList()
)

// ── History snapshot (SharedPreferences-backed legacy history) ────────────────

@Immutable
data class HistorySnapshot(
    val content: String,
    val savedAt: Long
)

// ── Workbench / Right Panel ───────────────────────────────────────────────────

@Immutable
@Serializable
sealed class PaneScope {
    @SerialName("global")
    @Serializable data object Global : PaneScope()

    @SerialName("book")
    @Serializable data class Book(val id: String, val title: String) : PaneScope()

    @SerialName("folder")
    @Serializable data class Folder(val id: String, val title: String) : PaneScope()

    @SerialName("file")
    @Serializable data class File(val id: String, val title: String) : PaneScope()
}

val PaneScope.specificity: Int get() = when (this) {
    is PaneScope.Global -> 0
    is PaneScope.Book   -> 1
    is PaneScope.Folder -> 2
    is PaneScope.File   -> 3
}

@Serializable
enum class PaneAccentColor { NONE, SLATE, BLUE, INDIGO, TEAL, GREEN, AMBER, ROSE, PLUM }

fun PaneAccentColor.toComposeColor(isDark: Boolean): androidx.compose.ui.graphics.Color = when (this) {
    PaneAccentColor.NONE  -> androidx.compose.ui.graphics.Color.Unspecified
    PaneAccentColor.SLATE -> if (isDark) androidx.compose.ui.graphics.Color(0xFF94A3B8) else androidx.compose.ui.graphics.Color(0xFF64748B)
    PaneAccentColor.BLUE  -> if (isDark) androidx.compose.ui.graphics.Color(0xFF60A5FA) else androidx.compose.ui.graphics.Color(0xFF3B82F6)
    PaneAccentColor.INDIGO-> if (isDark) androidx.compose.ui.graphics.Color(0xFF818CF8) else androidx.compose.ui.graphics.Color(0xFF6366F1)
    PaneAccentColor.TEAL  -> if (isDark) androidx.compose.ui.graphics.Color(0xFF2DD4BF) else androidx.compose.ui.graphics.Color(0xFF14B8A6)
    PaneAccentColor.GREEN -> if (isDark) androidx.compose.ui.graphics.Color(0xFF4ADE80) else androidx.compose.ui.graphics.Color(0xFF22C55E)
    PaneAccentColor.AMBER -> if (isDark) androidx.compose.ui.graphics.Color(0xFFFBBF24) else androidx.compose.ui.graphics.Color(0xFFF59E0B)
    PaneAccentColor.ROSE  -> if (isDark) androidx.compose.ui.graphics.Color(0xFFFB7185) else androidx.compose.ui.graphics.Color(0xFFF43F5E)
    PaneAccentColor.PLUM  -> if (isDark) androidx.compose.ui.graphics.Color(0xFFC084FC) else androidx.compose.ui.graphics.Color(0xFFA855F7)
}

@Serializable
enum class MinimizedBy { USER, SYSTEM }

@Serializable
enum class OutOfScopeDefault { SESSION_ONLY, ALWAYS_ADD, ALWAYS_ASK }

@Serializable
enum class OutOfScopeBehavior { DEFAULT, MINIMIZE, HIDE, KEEP_VISIBLE }

@Immutable
@Serializable
sealed class WorkbenchLayout {
    @SerialName("single")
    @Serializable data object Single : WorkbenchLayout()

    @SerialName("vertical_split")
    @Serializable data object VerticalSplit : WorkbenchLayout()

    @SerialName("horizontal_split")
    @Serializable data object HorizontalSplit : WorkbenchLayout()

    @SerialName("three_pane")
    @Serializable data object ThreePane : WorkbenchLayout()

    @SerialName("four_pane")
    @Serializable data object FourPane : WorkbenchLayout()
}

@Immutable
@Serializable
data class PaneConfig(
    val id              : String,
    val label           : String = "Section",
    val accentColor     : PaneAccentColor = PaneAccentColor.NONE,
    val primaryScope    : PaneScope = PaneScope.Global,
    val secondaryScopes : List<PaneScope> = emptyList(),
    val outOfScopeBehavior : OutOfScopeBehavior = OutOfScopeBehavior.DEFAULT,
    val pinnedNoteIds   : List<String> = emptyList(),
    val currentIndex    : Int = 0,
    val isMinimized     : Boolean = false,
    val minimizedBy     : MinimizedBy? = null,
    val showFooterPills : Boolean = true,
    val showWordCountPill: Boolean = true,
    val showOutlinePill: Boolean = true,
    val showProsePill: Boolean = true,
    val showReferencesPill: Boolean = true,
    val showLabel       : Boolean = true,
    val splitFraction   : Float = 0.5f,
    val systemMinimizedNoticeShown : Boolean = false
)

@Immutable
@Serializable
data class WorkbenchState(
    val panes    : List<PaneConfig> = listOf(PaneConfig(id = "pane_default")),
    val maxSlots : Int = 2,
    val layout   : WorkbenchLayout = WorkbenchLayout.VerticalSplit,
    val outOfScopeDefault : OutOfScopeDefault = OutOfScopeDefault.ALWAYS_ASK,
    val splitHorizontal   : Boolean = false,
    val tabBarAtBottom    : Boolean = false,
    val activeFloatingWindowIds: List<String> = emptyList(),
)

