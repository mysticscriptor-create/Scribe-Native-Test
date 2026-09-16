package com.primaloptima.scribe.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.primaloptima.scribe.ui.components.ScribeBarAction
import com.primaloptima.scribe.ui.components.ScribeThemeLivePreview
import com.primaloptima.scribe.ui.components.ScribeTopBar
import com.primaloptima.scribe.ui.screens.themeeditor.*
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.LocalOneShotBitmap
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.util.AppJson
import com.primaloptima.scribe.util.BitmapBlur
import com.primaloptima.scribe.util.DefaultThemes
import com.primaloptima.scribe.util.SAFHelper
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors
import com.primaloptima.scribe.viewmodel.ThemeViewModel
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditScreen(
    themeId: String,
    vm: ThemeViewModel,
    onBack: UnitCallback
) {
    val context = LocalContext.current
    val themes by vm.themes.collectAsStateWithLifecycle()

    val originalTheme = remember(themes, themeId) {
        themes.firstOrNull { it.id == themeId } ?: DefaultThemes.all.first()
    }

    // ── Centralized Draft State Holder ────────────────────────────────────────
    var draft by remember(originalTheme) {
        mutableStateOf(ThemeEditorDraft.fromAppTheme(originalTheme))
    }

    // Canonical Resolved Theme Colors (authoritative resolution layering overrides onto defaults)
    val resolvedColors = remember(draft) {
        draft.resolveColors()
    }

    // ── Active Category Navigation Tab (4-Pillar Architecture) ─────────────────
    var selectedCategory by remember { mutableStateOf(ThemeEditorCategory.APPEARANCE) }
    var isExpandedPreview by remember { mutableStateOf(true) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // ── Modal Dialog & Sheet States ───────────────────────────────────────────
    var activeColorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showAccessibilityDiagnostics by remember { mutableStateOf(false) }
    var showCropScreen by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<String?>(null) }
    var isLuminancePending by remember { mutableStateOf(false) }

    val view = LocalView.current
    val hazeState = LocalHazeState.current
    var dialogOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dialogCaptured by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val anyDialogOpen = showEmojiDialog || activeColorPickerTarget != null || showAccessibilityDiagnostics || showDiscardDialog
        LaunchedEffect(anyDialogOpen) {
            if (anyDialogOpen && !dialogCaptured) {
                dialogCaptured = true
                val raw = BitmapBlur.captureOnly(view)
                dialogOneShotBitmap = withContext(Dispatchers.IO) {
                    raw?.let { BitmapBlur.blurBitmap(it, radius = draft.frostedBlurRadius.toInt().coerceIn(1, 25)) }
                }
            } else if (!anyDialogOpen) {
                dialogCaptured = false
                dialogOneShotBitmap = null
            }
        }
    }

    val scope = rememberCoroutineScope()

    // ── Image Picker Launcher ─────────────────────────────────────────────────
    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val localUri = SAFHelper.copyBgImageToInternalStorage(context, uri, themeId)
                val stableUri = (localUri ?: uri).toString()
                pendingCropUri = stableUri
                draft = draft.copy(
                    bgOriginalUri = stableUri,
                    bgUri = stableUri,
                    bgMode = if (draft.bgMode == "color") "image" else draft.bgMode
                )
                showCropScreen = true
            }
        }
    }

    // ── Save Theme Action ─────────────────────────────────────────────────────
    val saveAction = {
        val updated = draft.toAppTheme(originalTheme)
        vm.save(updated)
        Toast.makeText(context, "Theme saved", Toast.LENGTH_SHORT).show()
        onBack()
    }

    // ── Safe Back Navigation with Unsaved Changes Guard ────────────────────────
    val handleBack = {
        if (draft.isDirty(originalTheme)) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = draft.isDirty(originalTheme)) {
        showDiscardDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
            topBar = {
                ScribeTopBar(
                    title = if (originalTheme.builtIn) "View Theme" else "Edit Theme",
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = handleBack,
                    actions = if (originalTheme.builtIn) emptyList() else listOf(
                        ScribeBarAction(Icons.Default.Check, "Save") {
                            if (!isLuminancePending) {
                                saveAction()
                            }
                        }
                    )
                )
            }
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                    .padding(paddingValues)
            ) {
                val isWide = maxWidth >= 720.dp

                if (isWide) {
                    // Two-Pane Responsive Layout for Tablets & Landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Sticky Preview Stage & Identity
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!originalTheme.builtIn) {
                                ThemeIdentityCard(
                                    emoji = draft.emoji,
                                    name = draft.name,
                                    onEmojiClick = { showEmojiDialog = true },
                                    onNameChange = { draft = draft.copy(name = it) }
                                )
                            }

                            ScribeThemeLivePreview(
                                colors = resolvedColors,
                                themeName = draft.name,
                                fontFamily = draft.fontFamily,
                                fontSize = draft.fontSize,
                                lineHeight = draft.lineHeight,
                                textAlignment = draft.textAlignment,
                                sideMargins = draft.sideMargins,
                                bgMode = draft.bgMode,
                                bgUri = draft.bgUri,
                                bgOpacity = draft.bgOpacity,
                                overlayEnabled = draft.overlayEnabled,
                                overlayColor = draft.overlayColor,
                                blurIntensity = draft.blurIntensity,
                                frostedGlassEnabled = draft.frostedGlassEnabled,
                                frostedTintEnabled = draft.frostedTintEnabled,
                                frostedBlurRadius = draft.frostedBlurRadius,
                                isDark = draft.isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Right Pane: 4-Pillar Tabs & Inspector Panel
                        LazyColumn(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                ThemeCategorySelector(
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = { selectedCategory = it }
                                )
                            }

                            item {
                                ActiveInspectorContent(
                                    selectedCategory = selectedCategory,
                                    draft = draft,
                                    originalTheme = originalTheme,
                                    resolvedColors = resolvedColors,
                                    onDraftChange = { draft = it },
                                    onSelectTarget = { activeColorPickerTarget = it },
                                    onPickImage = { bgImagePicker.launch("image/*") },
                                    onCropImage = {
                                        val uriToCrop = draft.bgOriginalUri ?: draft.bgUri
                                        if (uriToCrop != null) {
                                            pendingCropUri = uriToCrop
                                            showCropScreen = true
                                        }
                                    },
                                    onOpenAccessibilityDiagnostics = { showAccessibilityDiagnostics = true }
                                )
                            }

                            item {
                                ThemeActionButtons(
                                    isLuminancePending = isLuminancePending,
                                    isBuiltIn = originalTheme.builtIn,
                                    onSave = saveAction,
                                    onExport = {
                                        val currentThemeToExport = draft.toAppTheme(originalTheme)
                                        exportThemeJson(context, currentThemeToExport)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Single Column Flow for Standard Handheld Devices
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        // 1. Theme Name & Emoji
                        if (!originalTheme.builtIn) {
                            item {
                                ThemeIdentityCard(
                                    emoji = draft.emoji,
                                    name = draft.name,
                                    onEmojiClick = { showEmojiDialog = true },
                                    onNameChange = { draft = draft.copy(name = it) }
                                )
                            }
                        }

                        // 2. Collapsible Live Anchored Preview Stage
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ScribeTheme.shapes.themeEditorSection,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, ScribeTheme.colors.borders.subtle)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(ScribeTheme.shapes.themeEditorControl)
                                            .clickable { isExpandedPreview = !isExpandedPreview }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Theme Preview",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ScribeTheme.colors.content.primary
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                                shape = ScribeTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = if (isExpandedPreview) "Expanded" else "Compact",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Icon(
                                            if (isExpandedPreview) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isExpandedPreview) "Collapse preview" else "Expand preview",
                                            tint = ScribeTheme.colors.content.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isExpandedPreview) {
                                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            ScribeThemeLivePreview(
                                                colors = resolvedColors,
                                                themeName = draft.name,
                                                fontFamily = draft.fontFamily,
                                                fontSize = draft.fontSize,
                                                lineHeight = draft.lineHeight,
                                                textAlignment = draft.textAlignment,
                                                sideMargins = draft.sideMargins,
                                                bgMode = draft.bgMode,
                                                bgUri = draft.bgUri,
                                                bgOpacity = draft.bgOpacity,
                                                overlayEnabled = draft.overlayEnabled,
                                                overlayColor = draft.overlayColor,
                                                blurIntensity = draft.blurIntensity,
                                                frostedGlassEnabled = draft.frostedGlassEnabled,
                                                frostedTintEnabled = draft.frostedTintEnabled,
                                                frostedBlurRadius = draft.frostedBlurRadius,
                                                isDark = draft.isDark,
                                                modifier = Modifier.height(420.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. 4-Pillar Category Selector
                        item {
                            ThemeCategorySelector(
                                selectedCategory = selectedCategory,
                                onSelectCategory = { selectedCategory = it }
                            )
                        }

                        // 4. Active Inspector Panel
                        item {
                            ActiveInspectorContent(
                                selectedCategory = selectedCategory,
                                draft = draft,
                                originalTheme = originalTheme,
                                resolvedColors = resolvedColors,
                                onDraftChange = { draft = it },
                                onSelectTarget = { activeColorPickerTarget = it },
                                onPickImage = { bgImagePicker.launch("image/*") },
                                onCropImage = {
                                    val uriToCrop = draft.bgOriginalUri ?: draft.bgUri
                                    if (uriToCrop != null) {
                                        pendingCropUri = uriToCrop
                                        showCropScreen = true
                                    }
                                },
                                onOpenAccessibilityDiagnostics = { showAccessibilityDiagnostics = true }
                            )
                        }

                        // 5. Actions (Save / Export)
                        item {
                            ThemeActionButtons(
                                isLuminancePending = isLuminancePending,
                                isBuiltIn = originalTheme.builtIn,
                                onSave = saveAction,
                                onExport = {
                                    val currentThemeToExport = draft.toAppTheme(originalTheme)
                                    exportThemeJson(context, currentThemeToExport)
                                }
                            )
                        }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalOneShotBitmap provides dialogOneShotBitmap) {
            // Modal Color Picker Bottom Sheet
            activeColorPickerTarget?.let { target ->
                val title = when (target) {
                    ColorPickerTarget.BACKGROUND -> "Background Color"
                    ColorPickerTarget.TEXT -> "Reading Text Color"
                    ColorPickerTarget.ACCENT -> "Primary Accent Color"
                    ColorPickerTarget.HEADING_TEXT -> "Heading Color"
                    ColorPickerTarget.DIALOGUE_TEXT -> "Dialogue Color"
                    ColorPickerTarget.MONOLOGUE_TEXT -> "Monologue Color"
                    ColorPickerTarget.SPECIAL_HIGHLIGHT -> "Emphasis Highlight Color"
                    ColorPickerTarget.ANNOTATION -> "Annotation Color"
                    ColorPickerTarget.SECONDARY -> "Secondary Accent Color"
                    ColorPickerTarget.TERTIARY -> "Tertiary Accent Color"
                    ColorPickerTarget.SUCCESS -> "Success Status Color"
                    ColorPickerTarget.WARNING -> "Warning Status Color"
                    ColorPickerTarget.ERROR -> "Error Status Color"
                    ColorPickerTarget.SURFACE -> "Surface Color"
                    ColorPickerTarget.OVERLAY -> "Overlay Tint Color"
                }

                val currentHex = when (target) {
                    ColorPickerTarget.BACKGROUND -> draft.bgHex
                    ColorPickerTarget.TEXT -> draft.textHex
                    ColorPickerTarget.ACCENT -> draft.accentHex
                    ColorPickerTarget.HEADING_TEXT -> resolvedColors.headingText
                    ColorPickerTarget.DIALOGUE_TEXT -> resolvedColors.dialogueText
                    ColorPickerTarget.MONOLOGUE_TEXT -> resolvedColors.monologueText
                    ColorPickerTarget.SPECIAL_HIGHLIGHT -> if (resolvedColors.specialHighlight.isNotBlank()) resolvedColors.specialHighlight else resolvedColors.accent
                    ColorPickerTarget.ANNOTATION -> if (resolvedColors.annotation.isNotBlank()) resolvedColors.annotation else resolvedColors.accent
                    ColorPickerTarget.SECONDARY -> resolvedColors.secondary
                    ColorPickerTarget.TERTIARY -> resolvedColors.tertiary
                    ColorPickerTarget.SUCCESS -> resolvedColors.success
                    ColorPickerTarget.WARNING -> resolvedColors.warning
                    ColorPickerTarget.ERROR -> resolvedColors.error
                    ColorPickerTarget.SURFACE -> resolvedColors.surface
                    ColorPickerTarget.OVERLAY -> draft.overlayColor ?: draft.bgHex
                }

                ColorPickerBottomSheet(
                    title = title,
                    initialHex = currentHex,
                    onDismiss = { activeColorPickerTarget = null },
                    onColorSelected = { newHex ->
                        when (target) {
                            ColorPickerTarget.BACKGROUND -> draft = draft.copy(bgHex = newHex)
                            ColorPickerTarget.TEXT -> draft = draft.copy(textHex = newHex)
                            ColorPickerTarget.ACCENT -> draft = draft.copy(accentHex = newHex)
                            ColorPickerTarget.OVERLAY -> draft = draft.copy(overlayColor = newHex)
                            else -> draft = draft.withOverride(target, newHex)
                        }
                    }
                )
            }

            // Accessibility Diagnostics Full Dialog
            if (showAccessibilityDiagnostics) {
                AccessibilityDiagnosticsDialog(
                    colors = resolvedColors,
                    onDismiss = { showAccessibilityDiagnostics = false }
                )
            }

            // Emoji Picker Dialog
            if (showEmojiDialog) {
                val emojis = listOf(
                    "🖊️", "📖", "🌙", "⭐", "🌿", "🔥", "🌊", "🌸", "🏔️", "🌌",
                    "📜", "✨", "🎭", "🌅", "🍂", "❄️", "🪶", "🕯️", "🌺", "☕"
                )
                FrostedDialog(
                    onDismissRequest = { showEmojiDialog = false },
                    title = { Text("Theme Emoji Badge") },
                    text = {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(emojis) { em ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(ScribeTheme.shapes.cardNested)
                                        .clickable {
                                            draft = draft.copy(emoji = em)
                                            showEmojiDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(em, fontSize = 22.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEmojiDialog = false }) { Text("Close") }
                    }
                )
            }

            // Unsaved Changes Discard Dialog
            if (showDiscardDialog) {
                FrostedDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    title = { Text("Discard Unsaved Changes?") },
                    text = {
                        Text("You have unsaved changes to this theme. If you leave now, any customizations made during this session will be lost.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDiscardDialog = false
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Discard Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Keep Editing")
                        }
                    }
                )
            }
        }

        // ── Fullscreen Image Crop Screen Overlay ──────────────────────────────
        if (showCropScreen && pendingCropUri != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageCropScreen(
                    imageUri = pendingCropUri!!,
                    themeId = themeId,
                    onConfirm = { croppedUri ->
                        draft = draft.copy(
                            bgOriginalUri = pendingCropUri,
                            bgUri = croppedUri,
                            bgMode = if (draft.bgMode == "color") "image" else draft.bgMode
                        )
                        showCropScreen = false
                        pendingCropUri = null

                        isLuminancePending = true
                        scope.launch {
                            val analysis = computeBgAnalysis(context, croppedUri)
                            draft = draft.copy(
                                bgLuminance = analysis.avgLightness,
                                zonalLuminanceMatrix = analysis.zonalLuminance,
                                zonalVarianceMatrix = analysis.zonalVariance,
                                bgDominantColor = analysis.dominantColor,
                                zonalColorsMatrix = analysis.zonalColors,
                                luminanceFieldMatrix = analysis.bgLuminanceField
                            )
                            isLuminancePending = false
                        }
                    },
                    onCancel = {
                        showCropScreen = false
                        pendingCropUri = null
                    }
                )
            }
        }
    }
}

/**
 * Clean theme identity input card (Emoji button + Name field).
 */
@Composable
private fun ThemeIdentityCard(
    emoji: String,
    name: String,
    onEmojiClick: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ScribeTheme.shapes.themeEditorSection,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ScribeTheme.colors.borders.subtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onEmojiClick,
                shape = ScribeTheme.shapes.cardSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, ScribeTheme.colors.borders.subtle),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 22.sp)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Theme Name") },
                singleLine = true,
                shape = ScribeTheme.shapes.field,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 4-Pillar Category Selector (Appearance, Writing, Atmosphere, More).
 */
@Composable
private fun ThemeCategorySelector(
    selectedCategory: ThemeEditorCategory,
    onSelectCategory: (ThemeEditorCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ScribeTheme.shapes.cardSmall,
        border = BorderStroke(1.dp, ScribeTheme.colors.borders.subtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeEditorCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { onSelectCategory(cat) },
                    shape = ScribeTheme.shapes.navigationItem,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, ScribeTheme.colors.borders.subtle) else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = cat.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ScribeTheme.colors.interaction.primary else ScribeTheme.colors.content.secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Delegator rendering the active pillar's inspector panel.
 */
@Composable
private fun ActiveInspectorContent(
    selectedCategory: ThemeEditorCategory,
    draft: ThemeEditorDraft,
    originalTheme: AppTheme,
    resolvedColors: ThemeColors,
    onDraftChange: (ThemeEditorDraft) -> Unit,
    onSelectTarget: (ColorPickerTarget) -> Unit,
    onPickImage: () -> Unit,
    onCropImage: () -> Unit,
    onOpenAccessibilityDiagnostics: () -> Unit
) {
    when (selectedCategory) {
        ThemeEditorCategory.APPEARANCE -> {
            ThemeAppearancePanel(
                draft = draft,
                resolvedColors = resolvedColors,
                onSelectTarget = onSelectTarget,
                onResetOverride = { target ->
                    onDraftChange(draft.withClearedOverride(target))
                }
            )
        }
        ThemeEditorCategory.WRITING -> {
            ThemeWritingPanel(
                draft = draft,
                resolvedColors = resolvedColors,
                onSelectTarget = onSelectTarget,
                onResetOverride = { target ->
                    onDraftChange(draft.withClearedOverride(target))
                },
                onFontFamilyChange = { onDraftChange(draft.copy(fontFamily = it)) },
                onFontSizeChange = { onDraftChange(draft.copy(fontSize = it)) },
                onLineHeightChange = { onDraftChange(draft.copy(lineHeight = it)) },
                onParagraphSpacingChange = { onDraftChange(draft.copy(paragraphSpacing = it)) }
            )
        }
        ThemeEditorCategory.ATMOSPHERE -> {
            ThemeAtmospherePanel(
                bgMode = draft.bgMode,
                bgUri = draft.bgUri,
                bgOriginalUri = draft.bgOriginalUri,
                bgOpacity = draft.bgOpacity,
                overlayEnabled = draft.overlayEnabled,
                overlayColor = draft.overlayColor,
                bgHex = draft.bgHex,
                blurIntensity = draft.blurIntensity,
                frostedGlassEnabled = draft.frostedGlassEnabled,
                frostedTintEnabled = draft.frostedTintEnabled,
                frostedBlurRadius = draft.frostedBlurRadius,
                onPickImage = onPickImage,
                onCropImage = onCropImage,
                onRemoveImage = {
                    onDraftChange(
                        draft.copy(
                            bgUri = null,
                            bgOriginalUri = null,
                            bgMode = "color",
                            bgLuminance = -1f,
                            zonalLuminanceMatrix = emptyList(),
                            zonalVarianceMatrix = emptyList(),
                            bgDominantColor = null,
                            zonalColorsMatrix = emptyList(),
                            luminanceFieldMatrix = emptyList()
                        )
                    )
                },
                onBgModeChange = { onDraftChange(draft.copy(bgMode = it)) },
                onOverlayEnabledChange = { onDraftChange(draft.copy(overlayEnabled = it)) },
                onOverlayColorClick = { onSelectTarget(ColorPickerTarget.OVERLAY) },
                onBgOpacityChange = { onDraftChange(draft.copy(bgOpacity = it)) },
                onBlurIntensityChange = { onDraftChange(draft.copy(blurIntensity = it)) },
                onFrostedGlassEnabledChange = { onDraftChange(draft.copy(frostedGlassEnabled = it)) },
                onFrostedTintEnabledChange = { onDraftChange(draft.copy(frostedTintEnabled = it)) },
                onFrostedBlurRadiusChange = { onDraftChange(draft.copy(frostedBlurRadius = it)) }
            )
        }
        ThemeEditorCategory.ADVANCED -> {
            ThemeAdvancedPanel(
                textAlignment = draft.textAlignment,
                sideMargins = draft.sideMargins,
                themeScope = draft.themeScope,
                resolvedColors = resolvedColors,
                hasCustomOverrides = draft.overrides != null && !draft.overrides.isEmpty(),
                onTextAlignmentChange = { onDraftChange(draft.copy(textAlignment = it)) },
                onSideMarginsChange = { onDraftChange(draft.copy(sideMargins = it)) },
                onThemeScopeChange = { onDraftChange(draft.copy(themeScope = it)) },
                onResetAllOverrides = { onDraftChange(draft.withResetAllOverrides()) },
                onResetToOriginal = { onDraftChange(ThemeEditorDraft.fromAppTheme(originalTheme)) },
                onOpenAccessibilityDiagnostics = onOpenAccessibilityDiagnostics
            )
        }
    }
}

/**
 * Save & Export action buttons.
 */
@Composable
private fun ThemeActionButtons(
    isLuminancePending: Boolean,
    isBuiltIn: Boolean,
    onSave: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSave,
            enabled = !isLuminancePending,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = ScribeTheme.shapes.button,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLuminancePending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analysing image…", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (!isBuiltIn) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = ScribeTheme.shapes.button
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Theme JSON")
            }
        }
    }
}

private typealias UnitCallback = () -> Unit

private fun exportThemeJson(context: Context, theme: AppTheme) {
    try {
        val json = AppJson.encodeToString(theme)
        val fileName = "${theme.name.lowercase().replace(Regex("[^a-z0-9]"), "_")}_theme.json"
        val dir = File(context.cacheDir, "exported_themes").also { it.mkdirs() }
        val file = File(dir, fileName).also { it.writeText(json) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Theme: ${theme.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
