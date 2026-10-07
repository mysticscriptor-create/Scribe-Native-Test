package com.primaloptima.scribe.ui.components

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.primaloptima.scribe.ui.theme.FontHelper
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.util.font.OnlineFontItem
import com.primaloptima.scribe.util.font.OnlineFontLibrary
import com.primaloptima.scribe.util.font.ScribeFont
import com.primaloptima.scribe.util.font.ScribeFontManager
import kotlinx.coroutines.launch

/**
 * Preview sample mode presets for side-by-side font comparison.
 */
enum class FontPreviewMode(val label: String, val sampleText: String) {
    SENTENCE("Sentence", "The quick brown fox jumps over the lazy dog."),
    HEADLINE("Headline", "Chapter VII: The Obsidian Gate"),
    ALPHABET("Alphabet", "Aa Bb Cc Dd Ee Ff Gg Hh Ii Jj Kk Ll Mm Nn Oo Pp Qq Rr Ss Tt Uu Vv Ww Xx Yy Zz"),
    NUMERALS("Numerals", "0 1 2 3 4 5 6 7 8 9  •  & % $ € £ # @ ! ?")
}

/**
 * Collapsed Typography Font Section:
 * 1. Current Active Font Card (tap opens My Fonts list)
 * 2. Download Fonts Card (tap opens Online Font Store)
 * 3. Import Font Card (tap opens SAF file picker)
 */
@Composable
fun TypographyFontSection(
    activeFontKey: String,
    typographyTarget: String,
    onOpenMyFonts: () -> Unit,
    onOpenDownloadFonts: () -> Unit,
    onImportFont: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allFonts = remember(activeFontKey) { ScribeFontManager.getAllFonts(context) }
    val currentFont = remember(activeFontKey, allFonts) {
        allFonts.find { it.id.equals(activeFontKey, ignoreCase = true) || it.name.equals(activeFontKey, ignoreCase = true) }
            ?: ScribeFontManager.builtInFonts.first()
    }

    val targetTitle = when (typographyTarget) {
        "title1" -> "Title 1 Font"
        "title2" -> "Title 2 Font"
        else -> "Document Font"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$targetTitle Family",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScribeTheme.colors.content.secondary
        )

        // ── 1. Current Font Card ──────────────────────────────────────────────
        Surface(
            onClick = onOpenMyFonts,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aa",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = currentFont.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentFont.isCustom) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (currentFont.isVariable) "${currentFont.category.replaceFirstChar { it.uppercase() }} • Variable" else currentFont.category.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            color = ScribeTheme.colors.content.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open fonts list",
                        tint = ScribeTheme.colors.content.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── 2. Download Online Fonts Card ─────────────────────────────────────
        Surface(
            onClick = onOpenDownloadFonts,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download online fonts",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Download Fonts",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Browse 2,000+ free editorial & book fonts",
                            fontSize = 11.sp,
                            color = ScribeTheme.colors.content.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Library",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open library",
                        tint = ScribeTheme.colors.content.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── 3. Import Custom Font Card ────────────────────────────────────────
        Surface(
            onClick = onImportFont,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Import font file",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Import Font",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Load .ttf or .otf files from device storage",
                            fontSize = 11.sp,
                            color = ScribeTheme.colors.content.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = ".ttf / .otf",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Import file",
                        tint = ScribeTheme.colors.content.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual Specimen Card Component:
 * Embeds a rich typography preview directly into the list item, showing:
 * - A distinctive large specimen glyph (e.g. "Aa")
 * - Live rendered sample sentence/headline/alphabet in the actual font
 * - Weight discrimination badges (300, 400, 700)
 * - Tap to open the full interactive Typeface Specimen Inspector
 */
@Composable
fun FontVisualPreviewCard(
    fontId: String = "",
    fontName: String,
    fontFamily: FontFamily,
    category: String,
    isVariable: Boolean,
    previewMode: FontPreviewMode,
    onOpenSpecimenModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenSpecimenModal,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Large Specimen Glyph + Sample Text
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aa",
                        fontFamily = fontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = previewMode.sampleText,
                    fontFamily = fontFamily,
                    fontSize = if (previewMode == FontPreviewMode.HEADLINE) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tactile preview eye icon button
            IconButton(
                onClick = onOpenSpecimenModal,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Inspect Specimen",
                    tint = ScribeTheme.colors.content.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Full Interactive Typeface Specimen Inspector Modal:
 * Allows user to dynamically test custom text, slider sizes (14sp to 42sp),
 * weight variations, and character maps before selecting or downloading!
 */
@Composable
fun TypefaceSpecimenModal(
    fontId: String = "",
    fontName: String,
    fontFamily: FontFamily,
    category: String,
    license: String,
    isVariable: Boolean,
    isInstalled: Boolean,
    actionButtonText: String,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var sampleMode by remember { mutableStateOf(FontPreviewMode.SENTENCE) }
    var customText by remember { mutableStateOf("") }
    var previewSizeSp by remember { mutableFloatStateOf(18f) }
    var selectedWeight by remember { mutableIntStateOf(400) }
    val effectiveKey = fontId.ifBlank { fontName }
    val dynamicFamily = remember(effectiveKey, selectedWeight) {
        FontHelper.getFontFamily(effectiveKey, selectedWeight)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = fontName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = category.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (isVariable) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Variable",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "License: $license • Interactive Typeface Specimen",
                            fontSize = 11.5.sp,
                            color = ScribeTheme.colors.content.secondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Preview mode preset chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FontPreviewMode.values().forEach { mode ->
                        FilterChip(
                            selected = sampleMode == mode && customText.isEmpty(),
                            onClick = {
                                sampleMode = mode
                                customText = ""
                            },
                            label = { Text(mode.label, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Custom text input
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    placeholder = { Text("Type custom specimen text...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 8.dp)
                )

                // Size & Weight Sliders / Selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Size: ${previewSizeSp.toInt()} sp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(300 to "300", 400 to "400", 500 to "500", 600 to "600", 700 to "700", 800 to "800").forEach { (w, label) ->
                            val isWSelected = selectedWeight == w
                            Surface(
                                onClick = { selectedWeight = w },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isWSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Slider(
                    value = previewSizeSp,
                    onValueChange = { previewSizeSp = it },
                    valueRange = 14f..40f,
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                )

                // Live Specimen Canvas
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val textToRender = if (customText.isNotEmpty()) customText else sampleMode.sampleText
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = textToRender,
                                fontFamily = dynamicFamily,
                                fontSize = previewSizeSp.sp,
                                fontWeight = FontWeight(selectedWeight),
                                lineHeight = (previewSizeSp * 1.35f).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Glyphs quick showcase
                            if (sampleMode != FontPreviewMode.ALPHABET) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Text(
                                    text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz\n0123456789  •  & ? ! @ # $",
                                    fontFamily = dynamicFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight(selectedWeight),
                                    lineHeight = 18.sp,
                                    color = ScribeTheme.colors.content.secondary
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Close", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onActionClick()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).height(44.dp)
                    ) {
                        Text(actionButtonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Sub-Sheet: "My Fonts"
 * Displays list of installed, built-in, and imported fonts with real-time typography preview.
 */
@Composable
fun MyFontsSubSheet(
    activeFontKey: String,
    typographyTarget: String,
    onSelectFont: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var fontToDelete by remember { mutableStateOf<ScribeFont?>(null) }
    var fontToInspect by remember { mutableStateOf<Pair<ScribeFont, FontFamily>?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var globalPreviewMode by remember { mutableStateOf(FontPreviewMode.SENTENCE) }

    val allFonts = remember(refreshTrigger) { ScribeFontManager.getAllFonts(context) }
    val filteredFonts = remember(searchQuery, allFonts) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allFonts
        else allFonts.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) }
    }

    val targetLabel = when (typographyTarget) {
        "title1" -> "Title 1"
        "title2" -> "Title 2"
        else -> "Document"
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
    ) {
        // Top Sub-Sheet Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to typography",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "My Fonts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Target: $targetLabel (${allFonts.size} installed)",
                        fontSize = 11.sp,
                        color = ScribeTheme.colors.content.secondary
                    )
                }
            }

            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ScribeTheme.colors.content.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter installed fonts...", fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        // Global Specimen Mode Bar: Quick sample style selector for side-by-side comparison
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Preview:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ScribeTheme.colors.content.secondary
            )
            FontPreviewMode.values().forEach { mode ->
                val isSelected = globalPreviewMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { globalPreviewMode = mode },
                    label = { Text(mode.label, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // List of fonts
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredFonts, key = { it.id }) { font ->
                val isSelected = activeFontKey.equals(font.id, ignoreCase = true) ||
                        activeFontKey.equals(font.name, ignoreCase = true) ||
                        (font.id == "default" && (activeFontKey.isEmpty() || activeFontKey == "default" || activeFontKey == "sans"))

                val resolvedFamily = remember(font.id) {
                    FontHelper.getFontFamily(font.id)
                }

                Surface(
                    onClick = {
                        onSelectFont(font.id)
                        onBack()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = font.name,
                                    fontFamily = resolvedFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (font.isCustom) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (font.isCustom) "Custom" else font.category.replaceFirstChar { it.uppercase() },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (font.isCustom) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }

                                if (font.isVariable) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "Variable",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (font.isCustom) {
                                    IconButton(
                                        onClick = { fontToDelete = font },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete font",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Embedded Visual Specimen Preview Card
                        Spacer(modifier = Modifier.height(6.dp))
                        FontVisualPreviewCard(
                            fontId = font.id,
                            fontName = font.name,
                            fontFamily = resolvedFamily,
                            category = font.category,
                            isVariable = font.isVariable,
                            previewMode = globalPreviewMode,
                            onOpenSpecimenModal = {
                                fontToInspect = font to resolvedFamily
                            }
                        )
                    }
                }
            }
        }
    }

    // Interactive Specimen Inspector Dialog
    fontToInspect?.let { (font, family) ->
        TypefaceSpecimenModal(
            fontId = font.id,
            fontName = font.name,
            fontFamily = family,
            category = font.category,
            license = if (font.isCustom) "User Imported" else "OFL-1.1",
            isVariable = font.isVariable,
            isInstalled = true,
            actionButtonText = "Select Font",
            onActionClick = {
                onSelectFont(font.id)
                onBack()
            },
            onDismiss = { fontToInspect = null }
        )
    }

    // Confirmation Dialog for Deleting Font
    fontToDelete?.let { font ->
        AlertDialog(
            onDismissRequest = { fontToDelete = null },
            title = { Text("Delete Font?") },
            text = { Text("Are you sure you want to remove '${font.name}' from your installed fonts?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ScribeFontManager.deleteCustomFont(context, font.id)
                        fontToDelete = null
                        refreshTrigger++
                        Toast.makeText(context, "Deleted ${font.name}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fontToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Sub-Sheet: "Download Online Fonts"
 * Browse, search, preview visual specimens, and 1-click download from over 2,000 free editorial and literary fonts.
 */
@Composable
fun DownloadFontsSubSheet(
    activeFontKey: String,
    typographyTarget: String,
    onApplyFont: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var fontItems by remember { mutableStateOf<List<OnlineFontItem>>(OnlineFontLibrary.curatedFonts) }
    var isSearching by remember { mutableStateOf(false) }
    var globalPreviewMode by remember { mutableStateOf(FontPreviewMode.SENTENCE) }

    // Font specimen inspector state: Pair of OnlineFontItem and resolved FontFamily
    var fontToInspect by remember { mutableStateOf<Pair<OnlineFontItem, FontFamily>?>(null) }

    // Map of fontId to download progress (0.0 to 1.0), or null if not downloading
    val downloadingMap = remember { mutableStateMapOf<String, Float>() }
    // Installed fonts refresh tracker
    var installedCheckKey by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    // Search and filter logic
    LaunchedEffect(searchQuery, selectedCategory) {
        isSearching = true
        fontItems = OnlineFontLibrary.searchOnlineFonts(searchQuery, selectedCategory)
        isSearching = false
    }

    val categories = listOf(
        "all" to "All",
        "serif" to "Serif",
        "sans" to "Sans-Serif",
        "display" to "Display",
        "mono" to "Monospace",
        "handwriting" to "Handwriting"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
    ) {
        // Top Sub-Sheet Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to typography",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Download Fonts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Free & open-source font library (OFL)",
                        fontSize = 11.sp,
                        color = ScribeTheme.colors.content.secondary
                    )
                }
            }

            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ScribeTheme.colors.content.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Input (Unconstrained height so typed text is never cut off)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 2,000+ fonts (e.g. Merriweather, Cinzel...)", fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { (catKey, catName) ->
                val isSelected = selectedCategory == catKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = catKey },
                    label = { Text(catName, fontSize = 11.5.sp) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // List of Online Fonts
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (fontItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No fonts found for \"$searchQuery\"",
                    fontSize = 13.sp,
                    color = ScribeTheme.colors.content.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fontItems, key = { it.id }) { item ->
                    val isInstalled = remember(installedCheckKey, item.id) {
                        ScribeFontManager.isFontInstalled(context, item.id)
                    }
                    val isCurrentlyActive = activeFontKey.equals(item.id, ignoreCase = true) ||
                            activeFontKey.equals(item.name, ignoreCase = true)
                    val downloadProgress = downloadingMap[item.id]

                    // Resolve visual preview font family dynamically (before or after download)
                    val previewFamily = remember(item.name, item.category, isInstalled) {
                        if (isInstalled) {
                            FontHelper.getFontFamily(item.id)
                        } else {
                            FontHelper.getOnlineFontPreviewFamily(item.name, item.category)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrentlyActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontFamily = previewFamily,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = item.category.replaceFirstChar { it.uppercase() },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = item.license,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.description,
                                        fontSize = 11.5.sp,
                                        color = ScribeTheme.colors.content.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action Button: Only download icon for non-downloaded, only apply/active icon for downloaded
                                when {
                                    downloadProgress != null -> {
                                        // Downloading state with progress spinner
                                        Box(
                                            modifier = Modifier.size(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { downloadProgress },
                                                modifier = Modifier.size(22.dp),
                                                strokeWidth = 2.2.dp
                                            )
                                        }
                                    }
                                    isInstalled -> {
                                        // Downloaded: Show active check or apply check icon (no Installed/Apply text)
                                        if (isCurrentlyActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Currently active font",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    onApplyFont(item.id)
                                                    onBack()
                                                },
                                                shape = CircleShape,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Apply ${item.name}",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // Not downloaded: Only download icon
                                        FilledTonalIconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    downloadingMap[item.id] = 0.1f
                                                    val result = OnlineFontLibrary.downloadFont(context, item) { prog ->
                                                        downloadingMap[item.id] = prog
                                                    }
                                                    downloadingMap.remove(item.id)
                                                    if (result.isSuccess) {
                                                        installedCheckKey++
                                                        Toast.makeText(context, "Downloaded ${item.name}!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to download: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            shape = CircleShape,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = "Download ${item.name}",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Only when the font is downloaded, the preview option animates in smoothly
                            AnimatedVisibility(
                                visible = isInstalled,
                                enter = expandVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(220)),
                                exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FontVisualPreviewCard(
                                        fontId = item.id,
                                        fontName = item.name,
                                        fontFamily = previewFamily,
                                        category = item.category,
                                        isVariable = item.isVariable,
                                        previewMode = globalPreviewMode,
                                        onOpenSpecimenModal = {
                                            fontToInspect = item to previewFamily
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Specimen Inspector Dialog
    fontToInspect?.let { (item, family) ->
        val isInstalled = ScribeFontManager.isFontInstalled(context, item.id)
        TypefaceSpecimenModal(
            fontId = item.id,
            fontName = item.name,
            fontFamily = family,
            category = item.category,
            license = item.license,
            isVariable = item.isVariable,
            isInstalled = isInstalled,
            actionButtonText = if (isInstalled) "Apply Font" else "Download & Apply",
            onActionClick = {
                if (isInstalled) {
                    onApplyFont(item.id)
                    onBack()
                } else {
                    coroutineScope.launch {
                        downloadingMap[item.id] = 0.1f
                        val result = OnlineFontLibrary.downloadFont(context, item) { prog ->
                            downloadingMap[item.id] = prog
                        }
                        downloadingMap.remove(item.id)
                        if (result.isSuccess) {
                            installedCheckKey++
                            onApplyFont(item.id)
                            onBack()
                            Toast.makeText(context, "Downloaded & applied ${item.name}!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to download: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { fontToInspect = null }
        )
    }
}
