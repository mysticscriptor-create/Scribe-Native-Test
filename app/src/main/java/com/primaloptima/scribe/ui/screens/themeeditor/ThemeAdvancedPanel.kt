package com.primaloptima.scribe.ui.screens.themeeditor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.ui.theme.autoTextColor
import com.primaloptima.scribe.ui.theme.parseComposeColor
import com.primaloptima.scribe.util.model.ThemeColors

/**
 * Inspector panel for Advanced options, Layout geometry, Elevation, Accessibility, and Reset controls.
 */
@Composable
fun ThemeAdvancedPanel(
    textAlignment: String,
    sideMargins: Float,
    themeScope: String,
    resolvedColors: ThemeColors,
    hasCustomOverrides: Boolean,
    onTextAlignmentChange: (String) -> Unit,
    onSideMarginsChange: (Float) -> Unit,
    onThemeScopeChange: (String) -> Unit,
    onResetAllOverrides: () -> Unit,
    onResetToOriginal: () -> Unit,
    onOpenAccessibilityDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showElevationDetails by remember { mutableStateOf(false) }
    var showResetOverridesDialog by remember { mutableStateOf(false) }
    var showResetOriginalDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. CANVAS GEOMETRY & ALIGNMENT ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.themeEditorSection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Canvas Geometry & Margins",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ScribeTheme.colors.content.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = ScribeTheme.shapes.themeEditorControl
                    ) {
                        Text(
                            text = "Canvas Flow",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Configure reading canvas alignment and horizontal gutter margins.",
                    fontSize = 12.sp,
                    color = ScribeTheme.colors.content.secondary,
                    lineHeight = 16.sp
                )

                // Text Alignment Segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val alignOptions = listOf(
                        Triple("left", "Left", Icons.AutoMirrored.Filled.FormatAlignLeft),
                        Triple("justified", "Justified", Icons.Default.FormatAlignJustify),
                        Triple("center", "Center", Icons.Default.FormatAlignCenter)
                    )
                    alignOptions.forEach { (key, label, icon) ->
                        val isSelected = textAlignment == key
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ScribeTheme.shapes.cardSmall)
                                .clickable { onTextAlignmentChange(key) },
                            shape = ScribeTheme.shapes.cardSmall,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else ScribeTheme.colors.borders.subtle
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else ScribeTheme.colors.content.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else ScribeTheme.colors.content.primary
                                )
                            }
                        }
                    }
                }

                // Side Margins Slider
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reading Side Margins", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ScribeTheme.colors.content.primary)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = ScribeTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "${sideMargins.toInt()} dp",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Slider(
                        value = sideMargins,
                        onValueChange = onSideMarginsChange,
                        valueRange = 8f..48f,
                        steps = 9
                    )
                }
            }
        }

        // ── 2. ARTWORK DISPLAY SCOPE ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.themeEditorSection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Artwork Display Scope",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ScribeTheme.colors.content.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        shape = ScribeTheme.shapes.themeEditorControl
                    ) {
                        Text(
                            text = "Scope",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                val scopes = listOf(
                    Pair("whole_app", "Whole App"),
                    Pair("editor_only", "Editor Only")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scopes.forEach { (key, label) ->
                        val isSelected = themeScope == key
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ScribeTheme.shapes.cardSmall)
                                .clickable { onThemeScopeChange(key) },
                            shape = ScribeTheme.shapes.cardSmall,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else ScribeTheme.colors.borders.subtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onThemeScopeChange(key) }
                                )
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) ScribeTheme.colors.content.primary else ScribeTheme.colors.content.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 3. 5-TIER PERCEPTUAL ELEVATION RAMP ────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.themeEditorSection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ScribeTheme.shapes.themeEditorControl)
                        .clickable { showElevationDetails = !showElevationDetails },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "5-Tier Perceptual Elevation Ramp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ScribeTheme.colors.content.primary
                        )
                        Text(
                            text = "Auto-derived OKLCH surface hierarchy",
                            fontSize = 11.sp,
                            color = ScribeTheme.colors.content.secondary
                        )
                    }
                    Icon(
                        if (showElevationDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle elevation details",
                        tint = ScribeTheme.colors.content.secondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tiers = listOf(
                        Triple("L0", "Base", resolvedColors.background),
                        Triple("L1", "Lowest", resolvedColors.surfaceLowest),
                        Triple("L2", "Surface", resolvedColors.surface),
                        Triple("L3", "Raised", resolvedColors.surfaceRaised),
                        Triple("L4", "Overlay", resolvedColors.surfaceOverlay)
                    )
                    val borderSubtle = parseComposeColor(resolvedColors.borderSubtle, MaterialTheme.colorScheme.outlineVariant)
                    tiers.forEach { (code, name, hex) ->
                        val swatchColor = parseComposeColor(hex, MaterialTheme.colorScheme.surfaceVariant)
                        val swatchText = autoTextColor(swatchColor)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ScribeTheme.shapes.themeEditorControl)
                                .background(swatchColor)
                                .border(1.dp, borderSubtle, ScribeTheme.shapes.themeEditorControl)
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = code,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = swatchText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = name,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = swatchText.copy(alpha = 0.8f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = hex.uppercase(),
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = swatchText.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showElevationDetails) {
                    Text(
                        text = "Scribe mathematically derives this 5-tier elevation ramp using OKLCH lightness scaling from your background. This guarantees perfect optical contrast and subtle borders across cards, dialogs, and floating sheets without manual calibration.",
                        fontSize = 11.sp,
                        color = ScribeTheme.colors.content.secondary,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // ── 4. ACCESSIBILITY & CONTRAST DIAGNOSTICS ───────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.themeEditorSection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accessibility & Readability",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ScribeTheme.colors.content.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = ScribeTheme.shapes.themeEditorControl
                    ) {
                        Text(
                            text = "WCAG 2.2 & APCA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Verify contrast ratios and perceptual readability across body text, dialogue tokens, and interactive elements.",
                    fontSize = 12.sp,
                    color = ScribeTheme.colors.content.secondary,
                    lineHeight = 16.sp
                )

                OutlinedButton(
                    onClick = onOpenAccessibilityDiagnostics,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ScribeTheme.shapes.button
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Full Accessibility Diagnostics")
                }
            }
        }

        // ── 5. RESET CONTROLS ──────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.themeEditorSection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Theme Reset Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ScribeTheme.colors.content.primary
                )

                // Reset Overrides
                OutlinedButton(
                    onClick = { showResetOverridesDialog = true },
                    enabled = hasCustomOverrides,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ScribeTheme.shapes.button
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Color Overrides to Defaults")
                }

                // Reset Entire Theme
                OutlinedButton(
                    onClick = { showResetOriginalDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ScribeTheme.shapes.button,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Revert All Session Edits")
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showResetOverridesDialog) {
        FrostedDialog(
            onDismissRequest = { showResetOverridesDialog = false },
            title = { Text("Reset Custom Overrides?") },
            text = {
                Text("This will restore all customized editorial tokens (headings, dialogue, monologue, highlights) to their automatic algorithmic defaults.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllOverrides()
                        showResetOverridesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reset Overrides")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetOverridesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetOriginalDialog) {
        FrostedDialog(
            onDismissRequest = { showResetOriginalDialog = false },
            title = { Text("Revert All Edits?") },
            text = {
                Text("This will discard all changes made in this session and restore the theme to its original state when opened.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToOriginal()
                        showResetOriginalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Revert All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetOriginalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
