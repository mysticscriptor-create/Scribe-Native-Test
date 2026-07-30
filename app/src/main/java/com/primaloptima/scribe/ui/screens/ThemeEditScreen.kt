package com.primaloptima.scribe.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.gson.GsonBuilder
import com.primaloptima.scribe.ui.theme.FontHelper
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.LocalOneShotBitmap
import com.primaloptima.scribe.ui.theme.frostedBar
import com.primaloptima.scribe.ui.theme.parseComposeColor
import com.primaloptima.scribe.util.BitmapBlur
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.withContext
import com.primaloptima.scribe.util.DefaultThemes
import com.primaloptima.scribe.util.SAFHelper
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.viewmodel.ThemeViewModel
import java.io.File
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEditScreen(
    themeId: String,
    vm: ThemeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themes by vm.themes.observeAsState(emptyList())

    val originalTheme = remember(themes, themeId) {
        themes.firstOrNull { it.id == themeId } ?: DefaultThemes.all.first()
    }

    var name by remember(originalTheme) { mutableStateOf(originalTheme.name) }
    var bgHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.background) }
    var textHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.text) }
    var accentHex by remember(originalTheme) { mutableStateOf(originalTheme.colors.accent) }

    var bgMode by remember(originalTheme) { mutableStateOf(originalTheme.bgMode) }
    var bgUri by remember(originalTheme) { mutableStateOf(originalTheme.backgroundImageUri) }
    var bgOpacity by remember(originalTheme) { mutableFloatStateOf(originalTheme.backgroundImageOpacity ?: 0.35f) }
    var blurIntensity by remember(originalTheme) { mutableFloatStateOf(originalTheme.blurIntensity) }
    var frostedGlassEnabled by remember(originalTheme) { mutableStateOf(originalTheme.frostedGlassEnabled) }

    var fontFamily by remember(originalTheme) { mutableStateOf(originalTheme.fontFamily) }
    var fontSize by remember(originalTheme) { mutableFloatStateOf(originalTheme.fontSize.toFloat()) }
    var lineHeight by remember(originalTheme) { mutableFloatStateOf(originalTheme.lineHeight) }
    var paragraphSpacing by remember(originalTheme) { mutableFloatStateOf(originalTheme.paragraphSpacing.toFloat()) }
    var sideMargins by remember(originalTheme) { mutableFloatStateOf(originalTheme.paddingHorizontal.toFloat()) }

    var textAlignment by remember(originalTheme) { mutableStateOf(originalTheme.textAlignment) }
    var themeScope by remember(originalTheme) { mutableStateOf(originalTheme.themeScope) }
    var emoji by remember(originalTheme) { mutableStateOf(originalTheme.emoji ?: "🖊️") }

    var activeColorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    // Crop screen: shown after the user picks a new background image
    var showCropScreen by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<String?>(null) }

    val view = LocalView.current
    val hazeState = LocalHazeState.current
    var dialogOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dialogCaptured by remember { mutableStateOf(false) }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val anyDialogOpen = showEmojiDialog || activeColorPickerTarget != null
        LaunchedEffect(anyDialogOpen) {
            if (anyDialogOpen && !dialogCaptured) {
                dialogCaptured = true
                val raw = BitmapBlur.captureOnly(view)
                dialogOneShotBitmap = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    raw?.let { BitmapBlur.blurBitmap(it, radius = 15) }
                }
            } else if (!anyDialogOpen) {
                dialogCaptured = false
                dialogOneShotBitmap = null
            }
        }
    }

    val scope = rememberCoroutineScope()

    // Image picker: copies to internal storage, then opens the crop screen so
    // the user can choose which region of the image to display as background.
    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                // Copy to internal storage first so the crop screen has a stable
                // file:// URI it can read (SAF content:// may lose permission).
                val localUri = SAFHelper.copyBgImageToInternalStorage(context, uri)
                val stableUri = (localUri ?: uri).toString()
                pendingCropUri = stableUri
                showCropScreen = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.frostedBar(hazeState),
                title = {
                    Text(
                        if (originalTheme.builtIn) "View Theme" else "Edit Theme",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updated = originalTheme.copy(
                            name = name,
                            fontSize = fontSize.toInt(),
                            lineHeight = lineHeight,
                            paragraphSpacing = paragraphSpacing.toInt(),
                            paddingHorizontal = sideMargins.toInt(),
                            fontFamily = fontFamily,
                            backgroundImageUri = bgUri,
                            backgroundImageOpacity = bgOpacity,
                            bgMode = bgMode,
                            blurIntensity = blurIntensity,
                            frostedGlassEnabled = frostedGlassEnabled,
                            textAlignment = textAlignment,
                            themeScope = themeScope,
                            emoji = emoji,
                            colors = originalTheme.colors.copy(
                                background = bgHex,
                                surface = bgHex,
                                text = textHex,
                                accent = accentHex,
                                toolbar = bgHex,
                                toolbarText = textHex
                            )
                        )
                        vm.save(updated)
                        Toast.makeText(context, "Theme saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            // SECTION 1: APPEARANCE
            item {
                SectionHeader("APPEARANCE")
            }

            // Live Preview
            item {
                Text("Live Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LivePreviewCard(
                    themeName = if (emoji.isNotBlank()) "$emoji $name" else name,
                    bgHex = bgHex,
                    textHex = textHex,
                    accentHex = accentHex,
                    bgMode = bgMode,
                    bgUri = bgUri,
                    bgOpacity = bgOpacity,
                    blurIntensity = blurIntensity,
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    textAlignment = textAlignment,
                    sideMargins = sideMargins
                )
            }

            // Background options
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Background Image", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Image picker row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(onClick = { bgImagePicker.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (bgUri.isNullOrEmpty()) "Pick Image" else "Change Image")
                            }
                            if (!bgUri.isNullOrEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Re-open crop screen for the current image
                                    TextButton(onClick = {
                                        pendingCropUri = bgUri
                                        showCropScreen = true
                                    }) {
                                        Icon(
                                            Icons.Default.Crop,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Crop")
                                    }
                                    TextButton(onClick = {
                                        bgUri = null
                                        bgMode = "color"
                                    }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        // Image-specific settings (only when an image is set)
                        if (!bgUri.isNullOrEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Blur background toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Blur Background", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Soften the image with a blur effect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Switch(
                                    checked = bgMode == "blurred",
                                    onCheckedChange = { blur ->
                                        bgMode = if (blur) "blurred" else "image"
                                    }
                                )
                            }

                            // Blur intensity slider — only when blur is on
                            if (bgMode == "blurred") {
                                Text(
                                    "Blur Intensity: ${blurIntensity.toInt()} dp",
                                    fontWeight = FontWeight.Medium
                                )
                                Slider(
                                    value = blurIntensity,
                                    onValueChange = { blurIntensity = it },
                                    valueRange = 0f..30f
                                )
                            }

                            // Overlay opacity
                            Text(
                                "Overlay Opacity: ${(bgOpacity * 100).toInt()}%",
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = bgOpacity,
                                onValueChange = { bgOpacity = it },
                                valueRange = 0.0f..0.90f
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Frosted glass toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Frosted Glass", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Apply glass morphism to bars, drawers, and cards",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Switch(
                                    checked = frostedGlassEnabled,
                                    onCheckedChange = { frostedGlassEnabled = it }
                                )
                            }
                        }
                    }
                }
            }

            // Theme Colors
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Theme Colors", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ColorTile(
                                label = "Background",
                                hex = bgHex,
                                onClick = { activeColorPickerTarget = ColorPickerTarget.BACKGROUND }
                            )
                            ColorTile(
                                label = "Text",
                                hex = textHex,
                                onClick = { activeColorPickerTarget = ColorPickerTarget.TEXT }
                            )
                            ColorTile(
                                label = "Accent",
                                hex = accentHex,
                                onClick = { activeColorPickerTarget = ColorPickerTarget.ACCENT }
                            )
                        }
                    }
                }
            }

            // SECTION 2: TYPOGRAPHY
            item {
                SectionHeader("TYPOGRAPHY")
            }

            // Font family selector
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Font Family", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(FontHelper.fontOptions) { fontOpt ->
                                val isSelected = fontFamily.equals(fontOpt.key, ignoreCase = true) ||
                                        fontFamily.equals(fontOpt.name, ignoreCase = true)
                                Surface(
                                    selected = isSelected,
                                    onClick = { fontFamily = fontOpt.key },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.height(64.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Scribe",
                                            fontFamily = FontHelper.getFontFamily(fontOpt.key),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = fontOpt.name,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Typography sliders
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Typography & Margins", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Text("Font Size: ${fontSize.toInt()} sp")
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            valueRange = 12f..28f
                        )

                        Text("Line Height: ${String.format("%.2f", lineHeight)}")
                        Slider(
                            value = lineHeight,
                            onValueChange = { lineHeight = it },
                            valueRange = 1.2f..2.5f
                        )

                        Text("Paragraph Spacing: ${paragraphSpacing.toInt()} dp")
                        Slider(
                            value = paragraphSpacing,
                            onValueChange = { paragraphSpacing = it },
                            valueRange = 0f..24f
                        )

                        Text("Side Margins: ${sideMargins.toInt()} dp")
                        Slider(
                            value = sideMargins,
                            onValueChange = { sideMargins = it },
                            valueRange = 0f..32f
                        )
                    }
                }
            }

            // SECTION 3: LAYOUT
            item {
                SectionHeader("LAYOUT")
            }

            // Text alignment
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Text Alignment", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val alignOptions = listOf(
                                "left" to Icons.Default.FormatAlignLeft,
                                "justified" to Icons.Default.FormatAlignJustify,
                                "center" to Icons.Default.FormatAlignCenter
                            )
                            alignOptions.forEach { (key, icon) ->
                                OutlinedIconToggleButton(
                                    checked = textAlignment == key,
                                    onCheckedChange = { textAlignment = key }
                                ) {
                                    Icon(icon, contentDescription = key)
                                }
                            }
                        }
                    }
                }
            }

            // Theme scope toggle
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Theme Scope", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val scopes = listOf("editor_only" to "Editor Only", "whole_app" to "Whole App")
                            scopes.forEachIndexed { index, (scopeKey, label) ->
                                SegmentedButton(
                                    selected = themeScope == scopeKey,
                                    onClick = { themeScope = scopeKey },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = scopes.size)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: DETAILS
            if (!originalTheme.builtIn) {
                item {
                    SectionHeader("DETAILS")
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Name & Emoji", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showEmojiDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Theme Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Save button
            item {
                val saveAction = {
                    val updated = originalTheme.copy(
                        name = name,
                        fontSize = fontSize.toInt(),
                        lineHeight = lineHeight,
                        paragraphSpacing = paragraphSpacing.toInt(),
                        paddingHorizontal = sideMargins.toInt(),
                        fontFamily = fontFamily,
                        backgroundImageUri = bgUri,
                        backgroundImageOpacity = bgOpacity,
                        bgMode = bgMode,
                        blurIntensity = blurIntensity,
                        frostedGlassEnabled = frostedGlassEnabled,
                        textAlignment = textAlignment,
                        themeScope = themeScope,
                        emoji = emoji,
                        colors = originalTheme.colors.copy(
                            background = bgHex,
                            surface = bgHex,
                            text = textHex,
                            accent = accentHex,
                            toolbar = bgHex,
                            toolbarText = textHex
                        )
                    )
                    vm.save(updated)
                    Toast.makeText(context, "Theme saved", Toast.LENGTH_SHORT).show()
                    onBack()
                }

                Button(
                    onClick = saveAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Export button
            if (!originalTheme.builtIn) {
                item {
                    Button(
                        onClick = {
                            val currentThemeToExport = originalTheme.copy(
                                name = name,
                                fontSize = fontSize.toInt(),
                                lineHeight = lineHeight,
                                paragraphSpacing = paragraphSpacing.toInt(),
                                paddingHorizontal = sideMargins.toInt(),
                                fontFamily = fontFamily,
                                backgroundImageUri = bgUri,
                                backgroundImageOpacity = bgOpacity,
                                bgMode = bgMode,
                                blurIntensity = blurIntensity,
                                frostedGlassEnabled = frostedGlassEnabled,
                                textAlignment = textAlignment,
                                themeScope = themeScope,
                                emoji = emoji,
                                colors = originalTheme.colors.copy(
                                    background = bgHex,
                                    surface = bgHex,
                                    text = textHex,
                                    accent = accentHex
                                )
                            )
                            exportThemeJson(context, currentThemeToExport)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Theme")
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
                ColorPickerTarget.TEXT -> "Text Color"
                ColorPickerTarget.ACCENT -> "Accent Color"
            }
            val currentHex = when (target) {
                ColorPickerTarget.BACKGROUND -> bgHex
                ColorPickerTarget.TEXT -> textHex
                ColorPickerTarget.ACCENT -> accentHex
            }

            ColorPickerBottomSheet(
                title = title,
                initialHex = currentHex,
                onDismiss = { activeColorPickerTarget = null },
                onColorSelected = { newHex ->
                    when (target) {
                        ColorPickerTarget.BACKGROUND -> bgHex = newHex
                        ColorPickerTarget.TEXT -> textHex = newHex
                        ColorPickerTarget.ACCENT -> accentHex = newHex
                    }
                }
            )
        }

        // Emoji Picker Dialog
        if (showEmojiDialog) {
        val emojis = listOf("🖊️", "📖", "🌙", "⭐", "🌿", "🔥", "🌊", "🌸", "🏔️", "🌌", "📜", "✨", "🎭", "🌅", "🍂", "❄️", "🌙", "🪶", "🕯️", "🌺")
        FrostedDialog(
            onDismissRequest = { showEmojiDialog = false },
            title = { Text("Theme Badge") },
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
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    emoji = em
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
    }

    // ── Crop Screen Overlay ───────────────────────────────────────────────────
    // Shown full-screen after the user picks a new background image.
    // Lets them drag a crop window to choose which part of the image to display.
    if (showCropScreen && pendingCropUri != null) {
        ImageCropScreen(
            imageUri = pendingCropUri!!,
            onConfirm = { croppedUri ->
                bgUri = croppedUri
                if (bgMode == "color") bgMode = "image"
                showCropScreen = false
                pendingCropUri = null
            },
            onCancel = {
                showCropScreen = false
                pendingCropUri = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 1.sp
        )
    }
}

enum class ColorPickerTarget {
    BACKGROUND, TEXT, ACCENT
}

@Composable
private fun ColorTile(
    label: String,
    hex: String,
    onClick: () -> Unit
) {
    val color = parseComposeColor(hex, Color.Gray)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val squareSize = 8.dp.toPx()
                val rows = (size.height / squareSize).toInt() + 1
                val cols = (size.width / squareSize).toInt() + 1
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val isDark = (r + c) % 2 == 0
                        drawRect(
                            color = if (isDark) Color(0xFFE0E0E0) else Color.White,
                            topLeft = androidx.compose.ui.geometry.Offset(c * squareSize, r * squareSize),
                            size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        Text(hex, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun LivePreviewCard(
    themeName: String,
    bgHex: String,
    textHex: String,
    accentHex: String,
    bgMode: String,
    bgUri: String?,
    bgOpacity: Float,
    blurIntensity: Float,
    fontFamily: String,
    fontSize: Float,
    lineHeight: Float,
    textAlignment: String,
    sideMargins: Float
) {
    val bgColor = parseComposeColor(bgHex, Color.White)
    val textColor = parseComposeColor(textHex, Color.Black)
    val accentColor = parseComposeColor(accentHex, Color.Blue)
    val font = FontHelper.getFontFamily(fontFamily)

    val textAlign = when (textAlignment) {
        "justified" -> TextAlign.Justify
        "center" -> TextAlign.Center
        else -> TextAlign.Left
    }

    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }
    val cursorAlpha by animateFloatAsState(
        targetValue = if (cursorVisible) 1f else 0f,
        animationSpec = tween(150),
        label = "cursorAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image if set
            if ((bgMode == "image" || bgMode == "blurred") && !bgUri.isNullOrEmpty()) {
                AsyncImage(
                    model = bgUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
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
                // Overlay: on API 31+ the GPU blur handles it; on older devices we
                // compensate by boosting the overlay opacity when blur intensity is raised,
                // simulating the visual weight the blur would normally add.
                val overlayAlpha = if (bgMode == "blurred" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && blurIntensity > 0f) {
                    (bgOpacity + blurIntensity / 35f).coerceIn(0f, 0.90f)
                } else {
                    bgOpacity
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }

            // Preview Layout with Fake Top Bar, Content, and Fake Bottom Toolbar
            Column(modifier = Modifier.fillMaxSize()) {
                // Fake Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                    Text(themeName, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = sideMargins.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Experience seamless distraction-free writing.",
                            color = textColor,
                            fontFamily = font,
                            fontSize = (fontSize * 0.7f).sp,
                            lineHeight = (fontSize * 0.7f * lineHeight).sp,
                            textAlign = textAlign,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .graphicsLayer { alpha = cursorAlpha }
                                .background(accentColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Accent & Highlight Color",
                        color = accentColor,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSize * 0.75f).sp
                    )
                }

                // Fake Bottom Toolbar Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FormatBold, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.FormatItalic, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomColorPicker(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(currentColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), array)
        array
    }
    var hue by remember(currentColor) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(currentColor) { mutableFloatStateOf(hsv[1]) }
    var valVal by remember(currentColor) { mutableFloatStateOf(hsv[2]) }

    fun update(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        valVal = v
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        onColorChanged(Color(argb))
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            val presets = listOf("#FAFAF7", "#1E1E2E", "#0D1117", "#000000", "#3366FF", "#E11D48", "#10B981", "#F59E0B", "#8B5CF6")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { p ->
                    val c = parseComposeColor(p)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable {
                                onColorChanged(c)
                            }
                    )
                }
            }
        }

        Column {
            Text("Hue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hue,
                onValueChange = { update(it, sat, valVal) },
                valueRange = 0f..360f
            )
        }

        Column {
            Text("Saturation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = sat,
                onValueChange = { update(hue, it, valVal) },
                valueRange = 0f..1f
            )
        }

        Column {
            Text("Brightness", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = valVal,
                onValueChange = { update(hue, sat, it) },
                valueRange = 0f..1f
            )
        }
    }
}

@Composable
private fun ColorPickerBottomSheet(
    title: String,
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(parseComposeColor(initialHex, Color.Red)) }
    var hexText by remember { mutableStateOf(initialHex) }

    FrostedDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Text(
                    text = hexText.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                CustomColorPicker(
                    modifier = Modifier.fillMaxWidth(),
                    currentColor = selectedColor,
                    onColorChanged = { color ->
                        selectedColor = color
                        val argb = color.toArgb()
                        hexText = String.format("#%06X", 0xFFFFFF and argb)
                    }
                )

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        if (input.startsWith("#") && (input.length == 7 || input.length == 9)) {
                            try {
                                selectedColor = parseComposeColor(input)
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("Hex Color") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(hexText)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    )
}

private fun exportThemeJson(context: Context, theme: AppTheme) {
    try {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(theme)
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

// ─────────────────────────────────────────────────────────────────────────────
// Image Crop Screen
//
// Full-screen overlay that lets the user drag a crop window over their chosen
// background image. On confirm, the selected region is cropped from the file
// on disk and saved as a new file — the returned URI replaces bgUri in the
// theme editor.
//
// No external library required — uses pure Compose touch gestures.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ImageCropScreen(
    imageUri: String,
    onConfirm: (croppedUri: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Crop rect stored as fractions (0..1) of the displayed image size.
    // Starts at a centered 80% box.
    var cropLeft   by remember { androidx.compose.runtime.mutableFloatStateOf(0.1f) }
    var cropTop    by remember { androidx.compose.runtime.mutableFloatStateOf(0.1f) }
    var cropRight  by remember { androidx.compose.runtime.mutableFloatStateOf(0.9f) }
    var cropBottom by remember { androidx.compose.runtime.mutableFloatStateOf(0.9f) }

    // Track which handle is being dragged: "move", "tl", "tr", "bl", "br", or null
    var dragging by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var dragStartLeft  by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragStartTop   by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragStartRight by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var dragStartBottom by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    var isSaving by remember { androidx.compose.runtime.mutableStateOf(false) }

    val minCrop = 0.15f  // minimum crop dimension as fraction of image

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full-screen image preview
        var imageLayoutSize by remember {
            androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
        }

        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    imageLayoutSize = coords.size
                }
        )

        // Crop overlay — semi-transparent dimming outside the crop window
        if (imageLayoutSize.width > 0) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val iw = imageLayoutSize.width.toFloat()
                val ih = imageLayoutSize.height.toFloat()

                // Image might be letterboxed — calculate actual image rect within Fit layout
                val displayW = size.width
                val displayH = size.height
                val imageAspect = iw / ih.coerceAtLeast(1f)
                val viewAspect  = displayW / displayH.coerceAtLeast(1f)
                val (imgX, imgY, imgW, imgH) = if (imageAspect > viewAspect) {
                    // Letterbox top/bottom
                    val w = displayW
                    val h = displayW / imageAspect
                    arrayOf((displayW - w) / 2f, (displayH - h) / 2f, w, h)
                } else {
                    // Pillarbox left/right
                    val h = displayH
                    val w = displayH * imageAspect
                    arrayOf((displayW - w) / 2f, (displayH - h) / 2f, w, h)
                }

                val cL = imgX + cropLeft   * imgW
                val cT = imgY + cropTop    * imgH
                val cR = imgX + cropRight  * imgW
                val cB = imgY + cropBottom * imgH

                val dimColor = androidx.compose.ui.graphics.Color(0x99000000)
                // Top bar
                drawRect(dimColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(displayW, cT))
                // Bottom bar
                drawRect(dimColor, topLeft = androidx.compose.ui.geometry.Offset(0f, cB),
                    size = androidx.compose.ui.geometry.Size(displayW, displayH - cB))
                // Left bar (between top and bottom)
                drawRect(dimColor, topLeft = androidx.compose.ui.geometry.Offset(0f, cT),
                    size = androidx.compose.ui.geometry.Size(cL, cB - cT))
                // Right bar (between top and bottom)
                drawRect(dimColor, topLeft = androidx.compose.ui.geometry.Offset(cR, cT),
                    size = androidx.compose.ui.geometry.Size(displayW - cR, cB - cT))

                // Crop border
                val borderColor = androidx.compose.ui.graphics.Color.White
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                drawRect(borderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(cL, cT),
                    size = androidx.compose.ui.geometry.Size(cR - cL, cB - cT),
                    style = stroke)

                // Corner handles
                val handleSize = 24f
                val handleStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                // TL
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cL, cT + handleSize), end = androidx.compose.ui.geometry.Offset(cL, cT), strokeWidth = 4f)
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cL, cT), end = androidx.compose.ui.geometry.Offset(cL + handleSize, cT), strokeWidth = 4f)
                // TR
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cR - handleSize, cT), end = androidx.compose.ui.geometry.Offset(cR, cT), strokeWidth = 4f)
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cR, cT), end = androidx.compose.ui.geometry.Offset(cR, cT + handleSize), strokeWidth = 4f)
                // BL
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cL, cB - handleSize), end = androidx.compose.ui.geometry.Offset(cL, cB), strokeWidth = 4f)
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cL, cB), end = androidx.compose.ui.geometry.Offset(cL + handleSize, cB), strokeWidth = 4f)
                // BR
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cR - handleSize, cB), end = androidx.compose.ui.geometry.Offset(cR, cB), strokeWidth = 4f)
                drawLine(borderColor, start = androidx.compose.ui.geometry.Offset(cR, cB), end = androidx.compose.ui.geometry.Offset(cR, cB - handleSize), strokeWidth = 4f)

                // Rule-of-thirds grid inside crop box
                val thirdW = (cR - cL) / 3f
                val thirdH = (cB - cT) / 3f
                val gridColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f)
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(cL + thirdW, cT), end = androidx.compose.ui.geometry.Offset(cL + thirdW, cB), strokeWidth = 1f)
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(cL + thirdW * 2, cT), end = androidx.compose.ui.geometry.Offset(cL + thirdW * 2, cB), strokeWidth = 1f)
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(cL, cT + thirdH), end = androidx.compose.ui.geometry.Offset(cR, cT + thirdH), strokeWidth = 1f)
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(cL, cT + thirdH * 2), end = androidx.compose.ui.geometry.Offset(cR, cT + thirdH * 2), strokeWidth = 1f)
            }

            // Drag handler overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageLayoutSize) {
                        val displayW = size.width.toFloat()
                        val displayH = size.height.toFloat()

                        // Compute image-within-display rect
                        val iw = imageLayoutSize.width.toFloat()
                        val ih = imageLayoutSize.height.toFloat()
                        val imageAspect = iw / ih.coerceAtLeast(1f)
                        val viewAspect  = displayW / displayH.coerceAtLeast(1f)
                        val (imgX, imgY, imgW, imgH) = if (imageAspect > viewAspect) {
                            val w = displayW; val h = displayW / imageAspect
                            arrayOf((displayW - w) / 2f, (displayH - h) / 2f, w, h)
                        } else {
                            val h = displayH; val w = displayH * imageAspect
                            arrayOf((displayW - w) / 2f, (displayH - h) / 2f, w, h)
                        }

                        detectDragGestures(
                            onDragStart = { offset ->
                                val x = offset.x; val y = offset.y
                                val cL = imgX + cropLeft   * imgW
                                val cT = imgY + cropTop    * imgH
                                val cR = imgX + cropRight  * imgW
                                val cB = imgY + cropBottom * imgH
                                val handleRadius = 36f

                                dragStartLeft   = cropLeft
                                dragStartTop    = cropTop
                                dragStartRight  = cropRight
                                dragStartBottom = cropBottom

                                dragging = when {
                                    kotlin.math.sqrt((x - cL).pow(2) + (y - cT).pow(2)) < handleRadius -> "tl"
                                    kotlin.math.sqrt((x - cR).pow(2) + (y - cT).pow(2)) < handleRadius -> "tr"
                                    kotlin.math.sqrt((x - cL).pow(2) + (y - cB).pow(2)) < handleRadius -> "bl"
                                    kotlin.math.sqrt((x - cR).pow(2) + (y - cB).pow(2)) < handleRadius -> "br"
                                    x in cL..cR && y in cT..cB -> "move"
                                    else -> null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / imgW
                                val dy = dragAmount.y / imgH
                                when (dragging) {
                                    "tl" -> {
                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCrop)
                                        cropTop  = (cropTop  + dy).coerceIn(0f, cropBottom - minCrop)
                                    }
                                    "tr" -> {
                                        cropRight = (cropRight + dx).coerceIn(cropLeft + minCrop, 1f)
                                        cropTop   = (cropTop  + dy).coerceIn(0f, cropBottom - minCrop)
                                    }
                                    "bl" -> {
                                        cropLeft   = (cropLeft   + dx).coerceIn(0f, cropRight - minCrop)
                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minCrop, 1f)
                                    }
                                    "br" -> {
                                        cropRight  = (cropRight  + dx).coerceIn(cropLeft + minCrop, 1f)
                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minCrop, 1f)
                                    }
                                    "move" -> {
                                        val w = dragStartRight - dragStartLeft
                                        val h = dragStartBottom - dragStartTop
                                        val newL = (cropLeft + dx).coerceIn(0f, 1f - w)
                                        val newT = (cropTop  + dy).coerceIn(0f, 1f - h)
                                        cropLeft   = newL
                                        cropTop    = newT
                                        cropRight  = newL + w
                                        cropBottom = newT + h
                                    }
                                }
                            },
                            onDragEnd = { dragging = null }
                        )
                    }
            )
        }

        // Top bar: Cancel + title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Crop Background",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom bar: Confirm button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Drag corners or box to adjust",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        scope.launch {
                            val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val src = android.net.Uri.parse(imageUri)
                                    // Decode the full image
                                    val opts = android.graphics.BitmapFactory.Options().apply {
                                        inJustDecodeBounds = false
                                    }
                                    val inputStream = context.contentResolver.openInputStream(src)
                                        ?: java.io.File(imageUri.removePrefix("file://")).inputStream()
                                    val full = android.graphics.BitmapFactory.decodeStream(inputStream, null, opts)
                                        ?: return@withContext null
                                    inputStream.close()

                                    val fw = full.width.toFloat()
                                    val fh = full.height.toFloat()
                                    val x = (cropLeft * fw).toInt().coerceIn(0, full.width - 1)
                                    val y = (cropTop  * fh).toInt().coerceIn(0, full.height - 1)
                                    val w = ((cropRight - cropLeft) * fw).toInt().coerceIn(1, full.width - x)
                                    val h = ((cropBottom - cropTop) * fh).toInt().coerceIn(1, full.height - y)

                                    val cropped = android.graphics.Bitmap.createBitmap(full, x, y, w, h)
                                    full.recycle()

                                    // Save as new file alongside the original
                                    val dir = java.io.File(context.filesDir, "bg_images").also { it.mkdirs() }
                                    val dest = java.io.File(dir, "theme_bg_crop_${System.currentTimeMillis()}.jpg")
                                    dest.outputStream().use { out ->
                                        cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
                                    }
                                    cropped.recycle()
                                    android.net.Uri.fromFile(dest).toString()
                                } catch (e: Exception) {
                                    android.util.Log.e("ImageCrop", "Crop failed", e)
                                    null
                                }
                            }
                            isSaving = false
                            if (result != null) {
                                onConfirm(result)
                            } else {
                                // Fallback: use the image as-is if crop fails
                                onConfirm(imageUri)
                            }
                        }
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Use this crop")
                }
            }
        }
    }
}

private fun Float.pow(n: Int): Float {
    var result = 1f
    repeat(n) { result *= this }
    return result
}
