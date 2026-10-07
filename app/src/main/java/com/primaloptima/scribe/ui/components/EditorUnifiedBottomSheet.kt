package com.primaloptima.scribe.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ui.theme.LocalAppTheme
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.ScribeMetricTokens
import com.primaloptima.scribe.ui.theme.ScribeShapeTokens
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.ui.theme.FontHelper
import com.primaloptima.scribe.util.font.ScribeFont
import com.primaloptima.scribe.util.font.ScribeFontManager
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Unified navigation pages supported inside the single Editor bottom-sheet shell.
 */
enum class EditorSheetPage {
    MENU,
    TYPOGRAPHY,
    COLORS,
    MY_FONTS,
    DOWNLOAD_FONTS
}

/**
 * Seven tools supported inside Typography mode.
 */
enum class TypographyTool(val key: String, val label: String, val icon: ImageVector) {
    FONT("font", "Font", Icons.Default.TextFields),
    WEIGHT("weight", "Weight", Icons.Default.FormatBold),
    MARGINS("margins", "Margins", Icons.Default.DensityMedium),
    LINE_SPACING("line", "Line Spacing", Icons.Default.FormatLineSpacing),
    SIZE("size", "Size", Icons.Default.FormatSize),
    INDENT("indent", "First-Line Indent", Icons.Default.FormatIndentIncrease),
    ALIGNMENT("align", "Alignment", Icons.Default.FormatAlignLeft);

    companion object {
        fun fromKey(key: String): TypographyTool =
            if (key.equals("para", ignoreCase = true)) INDENT
            else entries.find { it.key.equals(key, ignoreCase = true) } ?: FONT
    }
}

/**
 * Scope for typography styling: Document body, Title 1, or Title 2.
 */
enum class TypographyTarget(val key: String, val label: String) {
    DOCUMENT("document", "Document"),
    TITLE_1("title1", "Title 1"),
    TITLE_2("title2", "Title 2");

    companion object {
        fun fromKey(key: String): TypographyTarget =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: DOCUMENT
    }
}

/**
 * Semantic text color roles for live theme tuning.
 */
enum class ColorRole(val key: String, val label: String) {
    TITLE_1("title1", "Title 1"),
    TITLE_2("title2", "Title 2"),
    PROSE("prose", "Prose"),
    DIALOGUE("dialogue", "Dialogue"),
    THOUGHTS("thoughts", "Thoughts"),
    HEADINGS("headings", "Headings");

    companion object {
        fun fromKey(key: String): ColorRole =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: TITLE_1
    }
}

/**
 * Accessible color swatch definition with friendly announcement names.
 */
data class SwatchItem(val hex: String, val name: String)

val PRESET_SWATCHES = listOf(
    SwatchItem("#0F172A", "Slate 900 (Dark Charcoal)"),
    SwatchItem("#334155", "Slate 700 (Deep Slate)"),
    SwatchItem("#475569", "Slate 600 (Muted Slate)"),
    SwatchItem("#64748B", "Slate 500 (Cool Gray)"),
    SwatchItem("#F8FAFC", "Slate 50 (Soft White)"),
    SwatchItem("#F1F5F9", "Slate 100 (Off White)"),
    SwatchItem("#FFFFFF", "Pure White"),
    SwatchItem("#B45309", "Amber 700 (Warm Amber)"),
    SwatchItem("#78350F", "Amber 900 (Deep Bronze)"),
    SwatchItem("#D97706", "Amber 600 (Golden Ochre)"),
    SwatchItem("#059669", "Emerald 600 (Forest Green)"),
    SwatchItem("#047857", "Emerald 700 (Deep Pine)"),
    SwatchItem("#0D9488", "Teal 600 (Dark Teal)"),
    SwatchItem("#1D4ED8", "Blue 700 (Cobalt Blue)"),
    SwatchItem("#4F46E5", "Indigo 600 (Royal Indigo)"),
    SwatchItem("#3730A3", "Midnight 800 (Midnight Indigo)"),
    SwatchItem("#7C3AED", "Violet 600 (Deep Violet)"),
    SwatchItem("#6D28D9", "Purple 700 (Imperial Purple)"),
    SwatchItem("#8B5CF6", "Lavender 500 (Lavender Violet)"),
    SwatchItem("#DC2626", "Red 600 (Crimson Red)"),
    SwatchItem("#B91C1C", "Burgundy 700 (Deep Burgundy)"),
    SwatchItem("#EA580C", "Orange 600 (Burnt Orange)")
)

/**
 * Single, unified bottom-sheet container for the Scribe Editor.
 * Houses Document Actions (Menu), Typography Tuning, Color Tuning, and Font Sub-sheets
 * within a single coherent frosted glass shell with smooth internal transitions and
 * consistent drag/elevation/inset handling.
 */
@Composable
fun EditorUnifiedBottomSheet(
    activePage: EditorSheetPage,
    onNavigate: (EditorSheetPage) -> Unit,
    onDismiss: () -> Unit,
    noteTitle: String,
    activeTheme: AppTheme?,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit,
    onEnterZen: () -> Unit,
    onOpenFloating: () -> Unit,
    onExport: (String) -> Unit,
    onVersionHistory: () -> Unit,
    onShortcuts: () -> Unit,
    onGuide: () -> Unit,
    onOpenThemes: () -> Unit,
    onSettings: () -> Unit,
    onVisualIndentChange: ((Int) -> Unit)? = null,
    restoreCursorOnOpen: Boolean = false,
    onToggleRestoreCursor: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp.dp * 0.88f)

    var typographyTool by remember { mutableStateOf<TypographyTool?>(null) }
    var typographyTarget by remember { mutableStateOf(TypographyTarget.DOCUMENT) }
    var alignmentTarget by rememberSaveable { mutableStateOf("document") }
    var colorRole by remember { mutableStateOf(ColorRole.TITLE_1) }
    var isEditingHex by rememberSaveable { mutableStateOf(false) }
    var customHexInput by rememberSaveable { mutableStateOf("") }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }

    // External font file import launcher (.ttf / .otf)
    val fontImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = ScribeFontManager.importFontFromUri(context, uri)
                if (result.isSuccess) {
                    val font = result.getOrThrow()
                    when (typographyTarget) {
                        TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                            onUpdateTheme { it.copy(titleFontFamily = font.id) }
                        else ->
                            onUpdateTheme { it.copy(fontFamily = font.id) }
                    }
                    Toast.makeText(context, "Imported and applied ${font.name}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to import font: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Determine back destination based on current page
    val handleBack: () -> Unit = {
        if (activePage == EditorSheetPage.TYPOGRAPHY && typographyTool != null) {
            typographyTool = null
        } else {
            when (activePage) {
                EditorSheetPage.MY_FONTS, EditorSheetPage.DOWNLOAD_FONTS -> onNavigate(EditorSheetPage.TYPOGRAPHY)
                EditorSheetPage.TYPOGRAPHY, EditorSheetPage.COLORS -> onNavigate(EditorSheetPage.MENU)
                EditorSheetPage.MENU -> onDismiss()
            }
        }
    }

    FrostedBottomSheet(
        onDismissRequest = onDismiss,
        onBackRequest = handleBack,
        modifier = modifier.heightIn(max = maxSheetHeight)
    ) {
        val dismissRequester = LocalFrostedSheetDismiss.current ?: onDismiss

        AnimatedContent(
            targetState = activePage,
            transitionSpec = {
                val isForward = (initialState == EditorSheetPage.MENU && targetState != EditorSheetPage.MENU) ||
                        (initialState == EditorSheetPage.TYPOGRAPHY && (targetState == EditorSheetPage.MY_FONTS || targetState == EditorSheetPage.DOWNLOAD_FONTS))
                val isSibling = (initialState == EditorSheetPage.TYPOGRAPHY && targetState == EditorSheetPage.COLORS) ||
                        (initialState == EditorSheetPage.COLORS && targetState == EditorSheetPage.TYPOGRAPHY)

                if (isSibling) {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
                } else if (isForward) {
                    (slideInHorizontally(
                        initialOffsetX = { (it * 0.15f).toInt() },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.15f).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(180))
                        )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { (-it * 0.15f).toInt() },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { (it * 0.15f).toInt() },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(180))
                        )
                }
            },
            label = "EditorSheetInternalPageTransition"
        ) { page ->
            when (page) {
                EditorSheetPage.MENU -> {
                    EditorMenuPage(
                        noteTitle = noteTitle,
                        showExportOptions = showExportOptions,
                        onToggleExport = { showExportOptions = !showExportOptions },
                        onNavigate = onNavigate,
                        onEnterZen = onEnterZen,
                        onOpenFloating = onOpenFloating,
                        onExport = onExport,
                        onVersionHistory = onVersionHistory,
                        onShortcuts = onShortcuts,
                        onGuide = onGuide,
                        onOpenThemes = onOpenThemes,
                        onSettings = onSettings,
                        restoreCursorOnOpen = restoreCursorOnOpen,
                        onToggleRestoreCursor = onToggleRestoreCursor,
                        onClose = dismissRequester
                    )
                }

                EditorSheetPage.TYPOGRAPHY -> {
                    if (activeTheme != null) {
                        EditorTuningPage(
                        isTextMode = true,
                        activeTool = typographyTool,
                        onSelectTool = { typographyTool = it },
                        activeTarget = typographyTarget,
                        onSelectTarget = { typographyTarget = it },
                        alignmentTarget = alignmentTarget,
                        onSelectAlignmentTarget = { alignmentTarget = it },
                        activeTheme = activeTheme,
                        onVisualIndentChange = onVisualIndentChange,
                        onUpdateTheme = onUpdateTheme,
                        onOpenMyFonts = { onNavigate(EditorSheetPage.MY_FONTS) },
                        onOpenDownloadFonts = { onNavigate(EditorSheetPage.DOWNLOAD_FONTS) },
                        onImportFont = {
                            fontImportLauncher.launch(
                                arrayOf(
                                    "font/*",
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/x-font-opentype",
                                    "application/octet-stream"
                                )
                            )
                        },
                        onNavigate = onNavigate,
                        onBack = handleBack,
                        onClose = dismissRequester
                    )
                    }
                }

                EditorSheetPage.COLORS -> {
                    if (activeTheme != null) {
                        EditorColorsPage(
                        activeRole = colorRole,
                        onSelectRole = {
                            colorRole = it
                            isEditingHex = false
                        },
                        isEditingHex = isEditingHex,
                        onToggleEditingHex = { isEditingHex = !isEditingHex },
                        customHexInput = customHexInput,
                        onHexInputChange = { customHexInput = it },
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme,
                        onNavigate = onNavigate,
                        onBack = handleBack,
                        onClose = dismissRequester
                    )
                    }
                }

                EditorSheetPage.MY_FONTS -> {
                    val activeFont = when (typographyTarget) {
                        TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                            activeTheme?.titleFontFamily ?: activeTheme?.fontFamily ?: "sans"
                        else -> activeTheme?.fontFamily ?: "sans"
                    }
                    MyFontsSubSheet(
                        activeFontKey = activeFont,
                        typographyTarget = typographyTarget.key,
                        onSelectFont = { selectedId ->
                            when (typographyTarget) {
                                TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                                    onUpdateTheme { it.copy(titleFontFamily = selectedId) }
                                else ->
                                    onUpdateTheme { it.copy(fontFamily = selectedId) }
                            }
                        },
                        onBack = { onNavigate(EditorSheetPage.TYPOGRAPHY) }
                    )
                }

                EditorSheetPage.DOWNLOAD_FONTS -> {
                    val activeFont = when (typographyTarget) {
                        TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                            activeTheme?.titleFontFamily ?: activeTheme?.fontFamily ?: "sans"
                        else -> activeTheme?.fontFamily ?: "sans"
                    }
                    DownloadFontsSubSheet(
                        activeFontKey = activeFont,
                        typographyTarget = typographyTarget.key,
                        onApplyFont = { selectedId ->
                            when (typographyTarget) {
                                TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                                    onUpdateTheme { it.copy(titleFontFamily = selectedId) }
                                else ->
                                    onUpdateTheme { it.copy(fontFamily = selectedId) }
                            }
                        },
                        onBack = { onNavigate(EditorSheetPage.TYPOGRAPHY) }
                    )
                }
            }
        }
    }
}

/**
 * Menu page displaying Document & Editor Actions in clean tactile grid cards.
 */
@Composable
private fun EditorMenuPage(
    noteTitle: String,
    showExportOptions: Boolean,
    onToggleExport: () -> Unit,
    onNavigate: (EditorSheetPage) -> Unit,
    onEnterZen: () -> Unit,
    onOpenFloating: () -> Unit,
    onExport: (String) -> Unit,
    onVersionHistory: () -> Unit,
    onShortcuts: () -> Unit,
    onGuide: () -> Unit,
    onOpenThemes: () -> Unit,
    onSettings: () -> Unit,
    restoreCursorOnOpen: Boolean = false,
    onToggleRestoreCursor: ((Boolean) -> Unit)? = null,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // Pinned Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = noteTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Document & Editor Actions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Close Menu" }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Row 1: History, Shortcuts, Guide, Export
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorTactileBox(
                title = "History",
                icon = Icons.Default.History,
                modifier = Modifier.weight(1f),
                onClick = onVersionHistory
            )
            EditorTactileBox(
                title = "Shortcuts",
                icon = Icons.Default.Keyboard,
                modifier = Modifier.weight(1f),
                onClick = onShortcuts
            )
            EditorTactileBox(
                title = "Guide",
                icon = Icons.AutoMirrored.Filled.Help,
                modifier = Modifier.weight(1f),
                onClick = onGuide
            )
            EditorTactileBox(
                title = "Export",
                icon = Icons.Default.Share,
                modifier = Modifier.weight(1f),
                onClick = onToggleExport
            )
        }

        // Collapsible Export Options
        AnimatedVisibility(
            visible = showExportOptions,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "CHOOSE EXPORT FORMAT",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScribeTheme.colors.content.secondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "TXT" to "txt",
                        "Markdown" to "md",
                        "HTML" to "html",
                        "PDF" to "pdf"
                    ).forEach { (label, format) ->
                        EditorTactileBox(
                            title = label,
                            icon = when (format) {
                                "txt" -> Icons.Default.Description
                                "md" -> Icons.Default.Code
                                "html" -> Icons.Default.Language
                                else -> Icons.Default.PictureAsPdf
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f),
                            onClick = { onExport(format) }
                        )
                    }
                }
            }
        }

        // Row 2: Typography, Colors, Themes, Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorTactileBox(
                title = "Typography",
                icon = Icons.Default.TextFields,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(EditorSheetPage.TYPOGRAPHY) }
            )
            EditorTactileBox(
                title = "Colors",
                icon = Icons.Default.Palette,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(EditorSheetPage.COLORS) }
            )
            EditorTactileBox(
                title = "Themes",
                icon = Icons.Default.Style,
                modifier = Modifier.weight(1f),
                onClick = onOpenThemes
            )
            EditorTactileBox(
                title = "Settings",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f),
                onClick = onSettings
            )
        }

        // Row 3: Zen Mode & Floating Window
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorTactileBox(
                title = "Zen Mode",
                icon = Icons.Default.Fullscreen,
                modifier = Modifier.weight(1f),
                onClick = onEnterZen
            )
            EditorTactileBox(
                title = "Floating",
                icon = Icons.Default.PictureInPicture,
                modifier = Modifier.weight(1f),
                onClick = onOpenFloating
            )
        }

        // Cursor Position Restoration Card / Toggle
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = if (restoreCursorOnOpen) ScribeTheme.colors.interaction.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { onToggleRestoreCursor?.invoke(!restoreCursorOnOpen) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (restoreCursorOnOpen) ScribeTheme.colors.interaction.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditLocation,
                            contentDescription = null,
                            tint = if (restoreCursorOnOpen) ScribeTheme.colors.interaction.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Restore Cursor Position",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (restoreCursorOnOpen) "Resumes target, cursor, and focus" else "Opens at top of document",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
                androidx.compose.material3.Switch(
                    checked = restoreCursorOnOpen,
                    onCheckedChange = { onToggleRestoreCursor?.invoke(it) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = ScribeTheme.colors.interaction.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Shared Top Header row for both Typography and Colors modes.
 * Displays Back arrow, Page title, [ Text | Colors ] segmented toggle, and Close button.
 */
@Composable
private fun TuningSharedHeader(
    isTextMode: Boolean,
    onNavigate: (EditorSheetPage) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Back to Menu" }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(2.dp))
            Text(
                text = if (isTextMode) "Typography" else "Colors",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Segmented Toggle Pill [ Text | Colors ]
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = Modifier
                .height(34.dp)
                .semantics { role = Role.Tab }
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedBg = ScribeTheme.colors.interaction.primary
                Surface(
                    onClick = { onNavigate(EditorSheetPage.TYPOGRAPHY) },
                    shape = CircleShape,
                    color = if (isTextMode) selectedBg else Color.Transparent,
                    modifier = Modifier
                        .height(30.dp)
                        .semantics {
                            selected = isTextMode
                            role = Role.Tab
                            contentDescription = "Typography Mode"
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Text",
                            fontSize = 12.sp,
                            fontWeight = if (isTextMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTextMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    onClick = { onNavigate(EditorSheetPage.COLORS) },
                    shape = CircleShape,
                    color = if (!isTextMode) selectedBg else Color.Transparent,
                    modifier = Modifier
                        .height(30.dp)
                        .semantics {
                            selected = !isTextMode
                            role = Role.Tab
                            contentDescription = "Colors Mode"
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Colors",
                            fontSize = 12.sp,
                            fontWeight = if (!isTextMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isTextMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Close Sheet" }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Typography Tuning Page with pinned Header & Tool Row, and a scrollable body containing
 * Target Scope and active tool controls.
 */
@Composable
private fun EditorTuningPage(
    isTextMode: Boolean,
    activeTool: TypographyTool?,
    onSelectTool: (TypographyTool?) -> Unit,
    activeTarget: TypographyTarget,
    onSelectTarget: (TypographyTarget) -> Unit,
    alignmentTarget: String,
    onSelectAlignmentTarget: (String) -> Unit,
    activeTheme: AppTheme,
    onVisualIndentChange: ((Int) -> Unit)? = null,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit,
    onOpenMyFonts: () -> Unit,
    onOpenDownloadFonts: () -> Unit,
    onImportFont: () -> Unit,
    onNavigate: (EditorSheetPage) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedContent(
        targetState = activeTool,
        transitionSpec = {
            if (targetState != null) {
                // Forward drill-down into tool detail
                (slideInHorizontally(
                    initialOffsetX = { (it * 0.15f).toInt() },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { (-it * 0.15f).toInt() },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    )
            } else {
                // Return back to master list
                (slideInHorizontally(
                    initialOffsetX = { (-it * 0.15f).toInt() },
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { (it * 0.15f).toInt() },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    )
            }
        },
        label = "TypographyMasterDetailTransition"
    ) { tool ->
        if (tool == null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pinned Top Header (Back, Scope Title, Colors Switcher, Close)
                TuningSharedHeader(
                    isTextMode = isTextMode,
                    onNavigate = onNavigate,
                    onBack = onBack,
                    onClose = onClose
                )

                // Master List of typography tools matching the reference design
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    TypographyMasterCard(
                        activeTheme = activeTheme,
                        onSelectTool = { onSelectTool(it) }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }
        } else {
            TypographyDetailPage(
                activeTool = tool,
                activeTarget = activeTarget,
                onSelectTarget = onSelectTarget,
                alignmentTarget = alignmentTarget,
                onSelectAlignmentTarget = onSelectAlignmentTarget,
                activeTheme = activeTheme,
                onVisualIndentChange = onVisualIndentChange,
                onUpdateTheme = onUpdateTheme,
                onOpenMyFonts = onOpenMyFonts,
                onOpenDownloadFonts = onOpenDownloadFonts,
                onImportFont = onImportFont,
                onBack = { onSelectTool(null) },
                onClose = onClose
            )
        }
    }
}

/**
 * Focused tool detail sub-page with dedicated header and custom natural content height.
 */
@Composable
private fun TypographyDetailPage(
    activeTool: TypographyTool,
    activeTarget: TypographyTarget,
    onSelectTarget: (TypographyTarget) -> Unit,
    alignmentTarget: String,
    onSelectAlignmentTarget: (String) -> Unit,
    activeTheme: AppTheme,
    onVisualIndentChange: ((Int) -> Unit)? = null,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit,
    onOpenMyFonts: () -> Unit,
    onOpenDownloadFonts: () -> Unit,
    onImportFont: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Focused Detail Header with back navigation & tool title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Typography list",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = activeTool.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
        )

        // Detail controls with natural wrapping height
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Target Scope Selector (Document, Title 1, Title 2)
            if (activeTool in listOf(TypographyTool.SIZE, TypographyTool.FONT, TypographyTool.WEIGHT, TypographyTool.LINE_SPACING)) {
                TargetScopeSelector(
                    activeTarget = activeTarget,
                    onSelectTarget = onSelectTarget
                )
                Spacer(Modifier.height(10.dp))
            } else if (activeTool == TypographyTool.ALIGNMENT) {
                AlignmentScopeSelector(
                    alignmentTarget = alignmentTarget,
                    onSelectAlignmentTarget = onSelectAlignmentTarget
                )
                Spacer(Modifier.height(10.dp))
            }

            when (activeTool) {
                TypographyTool.FONT -> {
                    val activeFont = when (activeTarget) {
                        TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 ->
                            activeTheme.titleFontFamily ?: activeTheme.fontFamily
                        else -> activeTheme.fontFamily
                    }
                    TypographyFontSection(
                        activeFontKey = activeFont,
                        typographyTarget = when (activeTarget) {
                            TypographyTarget.TITLE_1 -> "title1"
                            TypographyTarget.TITLE_2 -> "title2"
                            else -> "document"
                        },
                        onOpenMyFonts = onOpenMyFonts,
                        onOpenDownloadFonts = onOpenDownloadFonts,
                        onImportFont = onImportFont
                    )
                }
                TypographyTool.WEIGHT -> {
                    TypographyWeightControl(
                        activeTarget = activeTarget,
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme
                    )
                }
                TypographyTool.MARGINS -> {
                    TypographyMarginsControl(
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme
                    )
                }
                TypographyTool.LINE_SPACING -> {
                    TypographyLineSpacingControl(
                        activeTarget = activeTarget,
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme
                    )
                }
                TypographyTool.SIZE -> {
                    TypographySizeControl(
                        activeTarget = activeTarget,
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme
                    )
                }
                TypographyTool.INDENT -> {
                    TypographyIndentControl(
                        activeTheme = activeTheme,
                        onVisualIndentChange = onVisualIndentChange,
                        onUpdateTheme = onUpdateTheme
                    )
                }
                TypographyTool.ALIGNMENT -> {
                    TypographyAlignmentControl(
                        alignmentTarget = alignmentTarget,
                        activeTheme = activeTheme,
                        onUpdateTheme = onUpdateTheme
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Resolves user-friendly font name and style classification.
 */
private fun formatFontSubtitle(fontKey: String): String {
    val option = FontHelper.fontOptions.find { it.key.equals(fontKey, ignoreCase = true) }
    if (option != null) return "${option.name} • ${option.subtitle}"
    val builtIn = ScribeFontManager.builtInFonts.find { it.id.equals(fontKey, ignoreCase = true) }
    if (builtIn != null) return "${builtIn.name} • ${builtIn.category.replaceFirstChar { it.uppercase() }}"
    return "${fontKey.replaceFirstChar { it.uppercase() }} • Custom"
}

/**
 * Master vertical list card for Typography tools matching the reference design.
 * Renders all available tools with circular icon avatar badges, bold labels,
 * live formatted value subtitles, and trailing chevrons within a rounded elevated card.
 */
@Composable
private fun TypographyMasterCard(
    activeTheme: AppTheme,
    onSelectTool: (TypographyTool) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TypographyTool.entries.forEachIndexed { index, tool ->
                val subtitle = when (tool) {
                    TypographyTool.FONT -> {
                        formatFontSubtitle(activeTheme.fontFamily)
                    }
                    TypographyTool.WEIGHT -> {
                        val w = activeTheme.documentFontWeight
                        val label = when (w) {
                            300 -> "Light"
                            400 -> "Regular"
                            500 -> "Medium"
                            600 -> "SemiBold"
                            700 -> "Bold"
                            800 -> "ExtraBold"
                            else -> "Regular"
                        }
                        "$label ($w)"
                    }
                    TypographyTool.MARGINS -> {
                        val m = activeTheme.paddingHorizontal
                        val preset = when {
                            m <= 12 -> "Compact"
                            m <= 24 -> "Normal"
                            else -> "Wide"
                        }
                        "$preset • ${m}px"
                    }
                    TypographyTool.LINE_SPACING -> {
                        "%.1f".format(activeTheme.lineHeight)
                    }
                    TypographyTool.SIZE -> {
                        val tSize = activeTheme.title1FontSize ?: 18
                        "${activeTheme.fontSize} sp • Title: ${tSize} sp"
                    }
                    TypographyTool.INDENT -> {
                        val indent = activeTheme.firstLineIndent ?: 0
                        if (indent > 0) "First Line • $indent spaces" else "First Line • Off"
                    }
                    TypographyTool.ALIGNMENT -> {
                        val align = when (activeTheme.textAlignment.lowercase()) {
                            "justified" -> "Justified"
                            "center" -> "Center Align"
                            else -> "Left Align"
                        }
                        "$align • Prose"
                    }
                }

                TypographyMasterRow(
                    tool = tool,
                    title = tool.label,
                    subtitle = subtitle,
                    onClick = { onSelectTool(tool) },
                    showDivider = index < TypographyTool.entries.size - 1
                )
            }
        }
    }
}

@Composable
private fun TypographyMasterRow(
    tool: TypographyTool,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Circular Avatar Badge with snug gap around icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ScribeTheme.colors.interaction.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                TypographyToolGlyph(
                    tool = tool,
                    color = ScribeTheme.colors.interaction.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(10.dp))

            // Title & Subtitle side-by-side after ":" on a single line to minimize vertical space
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title: ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = ScribeTheme.colors.content.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ScribeTheme.colors.content.secondary.copy(alpha = 0.50f),
                modifier = Modifier.size(18.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 14.dp),
                thickness = 1.dp,
                color = ScribeTheme.colors.borders.subtle
            )
        }
    }
}

/**
 * Typographic glyph rendered matching the icons in the reference image:
 * - Aa: bold dual-case for Font
 * - W: serif bold for Weight
 * - ↔: horizontal double arrow for Margins
 * - ↕: vertical double arrow for Line Spacing
 * - Tt: bold dual-case for Size
 * - ¶: pilcrow for Paragraph
 * - ≡: lines for Alignment
 */
@Composable
private fun TypographyToolGlyph(
    tool: TypographyTool,
    color: Color
) {
    when (tool) {
        TypographyTool.FONT -> {
            Text(
                text = "Aa",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = color
            )
        }
        TypographyTool.WEIGHT -> {
            Text(
                text = "W",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        TypographyTool.MARGINS -> {
            Text(
                text = "↔",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        TypographyTool.LINE_SPACING -> {
            Text(
                text = "↕",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        TypographyTool.SIZE -> {
            Text(
                text = "Tt",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = color
            )
        }
        TypographyTool.INDENT -> {
            Icon(
                imageVector = Icons.Default.FormatIndentIncrease,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        }
        TypographyTool.ALIGNMENT -> {
            Icon(
                imageVector = Icons.Default.FormatAlignLeft,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun TargetScopeSelector(
    activeTarget: TypographyTarget,
    onSelectTarget: (TypographyTarget) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Applies to:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScribeTheme.colors.content.secondary
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypographyTarget.entries.forEach { target ->
                    val isSelected = activeTarget == target
                    Surface(
                        onClick = { onSelectTarget(target) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .height(28.dp)
                            .semantics {
                                selected = isSelected
                                role = Role.RadioButton
                                contentDescription = "Applies to ${target.label}"
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Alignment Scope Selector (Document vs Titles).
 */
@Composable
private fun AlignmentScopeSelector(
    alignmentTarget: String,
    onSelectAlignmentTarget: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Alignment Target:",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScribeTheme.colors.content.secondary
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
            modifier = Modifier.height(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("document" to "Document", "titles" to "Titles").forEach { (targetKey, targetLabel) ->
                    val isSelected = alignmentTarget == targetKey
                    Surface(
                        onClick = { onSelectAlignmentTarget(targetKey) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .height(28.dp)
                            .semantics {
                                selected = isSelected
                                role = Role.RadioButton
                                contentDescription = "Alignment for $targetLabel"
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = targetLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Size Control: Reusable ScribeSettingSlider (10..56 sp, step 1) with live visual specimen.
 */
@Composable
private fun TypographySizeControl(
    activeTarget: TypographyTarget,
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    val themeVal = when (activeTarget) {
        TypographyTarget.TITLE_1 -> (activeTheme.title1FontSize ?: 18).toFloat()
        TypographyTarget.TITLE_2 -> (activeTheme.title2FontSize ?: 24).toFloat()
        else -> activeTheme.fontSize.toFloat()
    }
    val minRange = if (activeTarget == TypographyTarget.TITLE_2) 14f else if (activeTarget == TypographyTarget.TITLE_1) 12f else 10f
    val maxRange = 56f
    val label = when (activeTarget) {
        TypographyTarget.TITLE_1 -> "Title 1 (Chapter) Size"
        TypographyTarget.TITLE_2 -> "Title 2 (Main Title) Size"
        else -> "Document Prose Size"
    }

    var localVal by remember(activeTarget, themeVal) { mutableFloatStateOf(themeVal) }
    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun commitValue(v: Float) {
        val rounded = v.roundToInt()
        when (activeTarget) {
            TypographyTarget.TITLE_1 -> onUpdateTheme { it.copy(title1FontSize = rounded) }
            TypographyTarget.TITLE_2 -> onUpdateTheme { it.copy(title2FontSize = rounded) }
            else -> onUpdateTheme { it.copy(fontSize = rounded) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScribeSettingSlider(
            label = label,
            value = localVal.coerceIn(minRange, maxRange),
            onValueChange = { newVal ->
                localVal = newVal
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(50L)
                    commitValue(localVal)
                }
            },
            onValueChangeFinished = {
                debounceJob?.cancel()
                commitValue(localVal)
            },
            valueRange = minRange..maxRange,
            step = 1f,
            unit = "sp"
        )

        Spacer(Modifier.height(4.dp))

        // Live Specimen Preview Card
        val context = LocalContext.current
        val activeFontId = when (activeTarget) {
            TypographyTarget.TITLE_1, TypographyTarget.TITLE_2 -> activeTheme.titleFontFamily ?: activeTheme.fontFamily
            else -> activeTheme.fontFamily
        }
        val fontFamily = remember(activeFontId) {
            FontHelper.getFontFamilyWithContext(context, activeFontId)
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = "SPECIMEN PREVIEW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScribeTheme.colors.content.secondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The quick brown fox jumps over the lazy dog.",
                    fontFamily = fontFamily,
                    fontSize = minOf(localVal, 28f).sp,
                    fontWeight = FontWeight(activeTheme.documentFontWeight),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



/**
 * Weight Control: Numeric weight display and tactile weight pills rendered with corresponding weights.
 */
@Composable
private fun TypographyWeightControl(
    activeTarget: TypographyTarget,
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    val currentWeight = when (activeTarget) {
        TypographyTarget.TITLE_1 -> activeTheme.title1FontWeight ?: 600
        TypographyTarget.TITLE_2 -> activeTheme.title2FontWeight ?: 700
        else -> activeTheme.documentFontWeight
    }
    val weightLabels = mapOf(
        300 to "Light (300)",
        400 to "Regular (400)",
        500 to "Medium (500)",
        600 to "SemiBold (600)",
        700 to "Bold (700)",
        800 to "ExtraBold (800)"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (activeTarget) {
                    TypographyTarget.TITLE_1 -> "Title 1 Weight"
                    TypographyTarget.TITLE_2 -> "Title 2 Weight"
                    else -> "Document Font Weight"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.height(26.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weightLabels[currentWeight] ?: "$currentWeight",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                300 to "Light",
                400 to "Regular",
                500 to "Medium",
                600 to "SemiBold",
                700 to "Bold",
                800 to "ExtraBold"
            ).forEach { (w, name) ->
                val isSelected = currentWeight == w
                Surface(
                    onClick = {
                        when (activeTarget) {
                            TypographyTarget.TITLE_1 -> onUpdateTheme { it.copy(title1FontWeight = w) }
                            TypographyTarget.TITLE_2 -> onUpdateTheme { it.copy(title2FontWeight = w) }
                            else -> onUpdateTheme { it.copy(documentFontWeight = w) }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .semantics {
                            selected = isSelected
                            role = Role.RadioButton
                            contentDescription = "$name weight, $w"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "$name ($w)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight(w),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Margins Control: Reusable ScribeSettingSlider (8..72 dp, step 2).
 */
@Composable
private fun TypographyMarginsControl(
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    val themeVal = activeTheme.paddingHorizontal.toFloat().coerceIn(8f, 72f)
    var localVal by remember(themeVal) { mutableFloatStateOf(themeVal) }
    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun commitMargins(v: Float) {
        val rounded = v.roundToInt()
        if (rounded != activeTheme.paddingHorizontal) {
            onUpdateTheme { it.copy(paddingHorizontal = rounded) }
        }
    }

    ScribeSettingSlider(
        label = "Horizontal Page Margins",
        value = localVal,
        onValueChange = { newVal ->
            localVal = newVal
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(50L)
                commitMargins(localVal)
            }
        },
        onValueChangeFinished = {
            debounceJob?.cancel()
            commitMargins(localVal)
        },
        valueRange = 8f..72f,
        step = 2f,
        unit = "dp"
    )
}

/**
 * Line Spacing Control: Reusable ScribeSettingSlider (1.0..2.4 ×, step 0.1).
 */
@Composable
private fun TypographyLineSpacingControl(
    activeTarget: TypographyTarget,
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    val currentVal = when (activeTarget) {
        TypographyTarget.TITLE_1 -> (activeTheme.title1LineHeight ?: 1.00f).coerceIn(1.0f, 2.4f)
        TypographyTarget.TITLE_2 -> (activeTheme.title2LineHeight ?: 1.00f).coerceIn(1.0f, 2.4f)
        else -> activeTheme.lineHeight.coerceIn(1.0f, 2.4f)
    }
    val label = when (activeTarget) {
        TypographyTarget.TITLE_1 -> "Title 1 Line Spacing"
        TypographyTarget.TITLE_2 -> "Title 2 Line Spacing"
        else -> "Document Line Spacing"
    }

    ScribeSettingSlider(
        label = label,
        value = currentVal,
        onValueChange = { newVal ->
            val rounded = (newVal * 20f).roundToInt() / 20f
            when (activeTarget) {
                TypographyTarget.TITLE_1 -> onUpdateTheme { it.copy(title1LineHeight = rounded) }
                TypographyTarget.TITLE_2 -> onUpdateTheme { it.copy(title2LineHeight = rounded) }
                else -> onUpdateTheme { it.copy(lineHeight = rounded) }
            }
        },
        valueRange = 1.0f..2.4f,
        step = 0.1f,
        unit = "×",
        decimalPlaces = 2
    )
}

/**
 * Paragraph Spacing Control: Reusable ScribeSettingSlider (0..40 dp, step 2).
 */
@Composable
private fun TypographyIndentControl(
    activeTheme: AppTheme,
    onVisualIndentChange: ((Int) -> Unit)? = null,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    val themeVal = (activeTheme.firstLineIndent ?: 0).toFloat().coerceIn(0f, 8f)
    var localVal by remember(themeVal) { mutableFloatStateOf(themeVal) }

    fun commitIndent(v: Float) {
        val rounded = (v / 2f).roundToInt() * 2
        localVal = rounded.toFloat()
        if (rounded != (activeTheme.firstLineIndent ?: 0)) {
            onUpdateTheme { it.copy(firstLineIndent = rounded) }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var debounceCommitJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    ScribeSettingSlider(
        label = "First-Line Indent",
        value = localVal,
        onValueChange = { newVal ->
            val rounded = (newVal / 2f).roundToInt() * 2
            localVal = newVal
            // Instant, non-blocking visual feedback on the visible canvas
            onVisualIndentChange?.invoke(rounded)
        },
        onValueChangeFinished = {
            // Instant visual sync on touch release or +/- tap
            val rounded = (localVal / 2f).roundToInt() * 2
            onVisualIndentChange?.invoke(rounded)
            // Debounce background processing commit slightly (100ms) to allow rapid +/- taps without blocking
            debounceCommitJob?.cancel()
            debounceCommitJob = coroutineScope.launch {
                kotlinx.coroutines.delay(100L)
                commitIndent(localVal)
            }
        },
        valueRange = 0f..8f,
        step = 2f,
        unit = " spaces"
    )
}

/**
 * Alignment Control: Left, Justified, Center (or Left, Center, Right for Titles) with icons and text.
 */
@Composable
private fun TypographyAlignmentControl(
    alignmentTarget: String,
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit
) {
    if (alignmentTarget == "document") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("left", "Left", Icons.Default.FormatAlignLeft),
                Triple("justified", "Justified", Icons.Default.FormatAlignJustify),
                Triple("center", "Center", Icons.Default.FormatAlignCenter)
            ).forEach { (key, label, icon) ->
                val isSelected = (activeTheme.textAlignment.ifEmpty { "left" }).equals(key, ignoreCase = true)
                Surface(
                    onClick = { onUpdateTheme { it.copy(textAlignment = key) } },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .semantics {
                            selected = isSelected
                            role = Role.RadioButton
                            contentDescription = "Document alignment: $label"
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("left", "Left", Icons.Default.FormatAlignLeft),
                Triple("center", "Center", Icons.Default.FormatAlignCenter),
                Triple("right", "Right", Icons.Default.FormatAlignRight)
            ).forEach { (key, label, icon) ->
                val isSelected = (activeTheme.textAlignment.ifEmpty { "left" }).equals(key, ignoreCase = true)
                Surface(
                    onClick = { onUpdateTheme { it.copy(textAlignment = key) } },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .semantics {
                            selected = isSelected
                            role = Role.RadioButton
                            contentDescription = "Titles alignment: $label"
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Colors Page with pinned Header, role selector row, current color display, custom hex input,
 * and accessible 22-color swatches with clear tactile selection.
 */
@Composable
private fun EditorColorsPage(
    activeRole: ColorRole,
    onSelectRole: (ColorRole) -> Unit,
    isEditingHex: Boolean,
    onToggleEditingHex: () -> Unit,
    customHexInput: String,
    onHexInputChange: (String) -> Unit,
    activeTheme: AppTheme,
    onUpdateTheme: ((AppTheme) -> AppTheme) -> Unit,
    onNavigate: (EditorSheetPage) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val currentHex = when (activeRole) {
        ColorRole.TITLE_1 -> activeTheme.overrides?.headingText ?: activeTheme.colors.headingText
        ColorRole.TITLE_2 -> activeTheme.colors.text
        ColorRole.PROSE -> activeTheme.colors.text
        ColorRole.DIALOGUE -> activeTheme.overrides?.dialogueText ?: activeTheme.colors.dialogueText
        ColorRole.THOUGHTS -> activeTheme.overrides?.monologueText ?: activeTheme.colors.monologueText
        ColorRole.HEADINGS -> activeTheme.overrides?.headingText ?: activeTheme.colors.headingText
    }

    val applyColor: (String) -> Unit = { hex ->
        when (activeRole) {
            ColorRole.TITLE_1 -> onUpdateTheme { ThemeManager.updateSemanticOverride(it, "headingText", hex) }
            ColorRole.TITLE_2 -> onUpdateTheme { ThemeManager.updateFoundationColors(it, newText = hex) }
            ColorRole.PROSE -> onUpdateTheme { ThemeManager.updateFoundationColors(it, newText = hex) }
            ColorRole.DIALOGUE -> onUpdateTheme { ThemeManager.updateSemanticOverride(it, "dialogueText", hex) }
            ColorRole.THOUGHTS -> onUpdateTheme { ThemeManager.updateSemanticOverride(it, "monologueText", hex) }
            ColorRole.HEADINGS -> onUpdateTheme { ThemeManager.updateSemanticOverride(it, "headingText", hex) }
        }
    }

    val configuration = LocalConfiguration.current
    val maxColorsHeight = (configuration.screenHeightDp.dp * 0.70f).coerceIn(385.dp, 520.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxColorsHeight)
    ) {
        // Pinned Header
        TuningSharedHeader(
            isTextMode = false,
            onNavigate = onNavigate,
            onBack = onBack,
            onClose = onClose
        )

        // Pinned Role Selector Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val roleScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(roleScrollState)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorRole.entries.forEach { cRole ->
                    val isSelected = activeRole == cRole
                    Surface(
                        onClick = { onSelectRole(cRole) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .semantics {
                                selected = isSelected
                                this.role = Role.Tab
                                contentDescription = "${cRole.label} color role"
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cRole.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (roleScrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .height(38.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Scrollable Body Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            // Current Hex Badge & Custom Hex Action
            val parsedColor = runCatching { Color(android.graphics.Color.parseColor(currentHex)) }.getOrDefault(Color.Gray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    )
                    Text(
                        text = "Current: $currentHex",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = {
                        onHexInputChange(currentHex)
                        onToggleEditingHex()
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (isEditingHex) "Hide Custom" else "Custom Hex",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ScribeTheme.colors.interaction.primary
                    )
                }
            }

            // Collapsible Custom Hex Input
            AnimatedVisibility(
                visible = isEditingHex,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { input ->
                            onHexInputChange(input)
                            if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                                runCatching {
                                    android.graphics.Color.parseColor(input)
                                    applyColor(input)
                                }
                            }
                        },
                        singleLine = true,
                        label = { Text("Hex Color (e.g. #D97706)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScribeTheme.colors.interaction.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Button(
                        onClick = {
                            val formatted = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                            runCatching {
                                android.graphics.Color.parseColor(formatted)
                                applyColor(formatted)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScribeTheme.colors.interaction.primary
                        ),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Apply", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Swatches Label
            Text(
                text = "PRESET PALETTES",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScribeTheme.colors.content.secondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Swatches Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PRESET_SWATCHES.forEach { swatch ->
                    val hex = swatch.hex
                    val swatchColor = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    val isSelected = currentHex.equals(hex, ignoreCase = true)
                    val isLight = hex == "#FFFFFF" || hex.startsWith("#F")

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                selected = isSelected
                                role = Role.RadioButton
                                contentDescription = "${swatch.name}, $hex"
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 22.dp),
                                onClick = { applyColor(hex) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = swatchColor,
                            border = BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) ScribeTheme.colors.interaction.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.size(34.dp)
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isLight) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Tactile compact option box with an icon, concise text label, and subtle directional
 * shadow underneath and to the side for an elevated editorial aesthetic.
 */
@Composable
private fun EditorTactileBox(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
    contentColor: Color = ScribeTheme.colors.interaction.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val isDark = LocalAppTheme.current?.isDark == true
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF0F172A).copy(alpha = 0.10f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .drawBehind {
                val cornerPx = 12.dp.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(1.5.dp.toPx(), 2.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
