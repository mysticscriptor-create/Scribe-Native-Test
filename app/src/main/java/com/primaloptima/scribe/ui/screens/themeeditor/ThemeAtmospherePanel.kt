package com.primaloptima.scribe.ui.screens.themeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.ui.theme.parseComposeColor

/**
 * Inspector panel for Atmosphere, Background Images, Blur, and Glassmorphic effects.
 */
@Composable
fun ThemeAtmospherePanel(
    bgMode: String,
    bgUri: String?,
    bgOriginalUri: String?,
    bgOpacity: Float,
    overlayEnabled: Boolean,
    overlayColor: String?,
    bgHex: String,
    blurIntensity: Float,
    frostedGlassEnabled: Boolean,
    frostedTintEnabled: Boolean,
    frostedBlurRadius: Float,
    onPickImage: () -> Unit,
    onCropImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onBgModeChange: (String) -> Unit,
    onOverlayEnabledChange: (Boolean) -> Unit,
    onOverlayColorClick: () -> Unit,
    onBgOpacityChange: (Float) -> Unit,
    onBlurIntensityChange: (Float) -> Unit,
    onFrostedGlassEnabledChange: (Boolean) -> Unit,
    onFrostedTintEnabledChange: (Boolean) -> Unit,
    onFrostedBlurRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Informational notice establishing clear mental boundary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ScribeTheme.shapes.cardSmall,
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ScribeTheme.colors.borders.subtle)
        ) {
            Text(
                text = "Atmosphere customizes background artwork and frosted glass translucency without altering your theme colors.",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 12.sp,
                color = ScribeTheme.colors.content.secondary,
                lineHeight = 16.sp
            )
        }

        // Background Image Card
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
                        text = "Background Artwork & Canvas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = ScribeTheme.shapes.themeEditorControl
                    ) {
                        Text(
                            text = if (bgUri.isNullOrEmpty()) "Solid Base" else "Artwork Active",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Apply immersive editorial cover photography or textured patterns under the writing canvas.",
                    fontSize = 12.sp,
                    color = ScribeTheme.colors.content.secondary,
                    lineHeight = 16.sp
                )

                if (bgUri.isNullOrEmpty()) {
                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ScribeTheme.shapes.button
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Artwork Image", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickImage,
                            modifier = Modifier.weight(1f),
                            shape = ScribeTheme.shapes.button
                        ) {
                            Text("Change Image", fontWeight = FontWeight.SemiBold)
                        }

                        if (!bgOriginalUri.isNullOrEmpty() || !bgUri.isNullOrEmpty()) {
                            OutlinedButton(
                                onClick = onCropImage,
                                shape = ScribeTheme.shapes.button
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = "Crop", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crop")
                            }
                        }

                        IconButton(
                            onClick = onRemoveImage,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }

                    // Background Mode (Clear Image vs Blurred Image)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Soft Blur Artwork Canvas", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = bgMode == "blurred",
                            onCheckedChange = { isBlurred ->
                                onBgModeChange(if (isBlurred) "blurred" else "image")
                            }
                        )
                    }

                    if (bgMode == "blurred") {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Artwork Blur Radius", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = ScribeTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "${blurIntensity.toInt()} dp",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Slider(
                                value = blurIntensity,
                                onValueChange = onBlurIntensityChange,
                                valueRange = 0f..30f
                            )
                        }
                    }

                    // Contrast & Tint Overlay Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Contrast Darkening Overlay", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Tints background artwork to ensure optimal reading contrast",
                                fontSize = 11.sp,
                                color = ScribeTheme.colors.content.secondary
                            )
                        }
                        Switch(
                            checked = overlayEnabled,
                            onCheckedChange = onOverlayEnabledChange
                        )
                    }

                    if (overlayEnabled) {
                        val activeOverlayHex = overlayColor ?: bgHex
                        val activeOverlayColor = parseComposeColor(activeOverlayHex, MaterialTheme.colorScheme.surface)

                        // Color Picker Swatch Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ScribeTheme.shapes.cardSmall)
                                .clickable { onOverlayColorClick() },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = ScribeTheme.shapes.cardSmall,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ScribeTheme.colors.borders.subtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(activeOverlayColor)
                                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                    Column {
                                        Text(
                                            text = "Overlay Tint Color",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ScribeTheme.colors.content.primary
                                        )
                                        Text(
                                            text = if (overlayColor != null) "Custom Tint" else "Theme Background Default",
                                            fontSize = 11.sp,
                                            color = ScribeTheme.colors.content.secondary
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    shape = ScribeTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = activeOverlayHex.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Overlay Opacity Slider
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Overlay Opacity", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = ScribeTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "${(bgOpacity * 100).toInt()}%",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Slider(
                                value = bgOpacity,
                                onValueChange = onBgOpacityChange,
                                valueRange = 0f..0.95f
                            )
                        }
                    }
                }
            }
        }

        // Frosted Glass Card
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
                        text = "Frosted Glass & Translucency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        shape = ScribeTheme.shapes.themeEditorControl
                    ) {
                        Text(
                            text = if (frostedGlassEnabled) "Glass On" else "Opaque Surface",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = "Refined hardware-accelerated frosted glass diffusion across toolbars and elevated cards.",
                    fontSize = 12.sp,
                    color = ScribeTheme.colors.content.secondary,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Glassmorphic Diffusion", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = frostedGlassEnabled,
                        onCheckedChange = onFrostedGlassEnabledChange
                    )
                }

                if (frostedGlassEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tint with Theme Surface Base", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = frostedTintEnabled,
                            onCheckedChange = onFrostedTintEnabledChange
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Glass Blur Strength", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = ScribeTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "${frostedBlurRadius.toInt()} dp",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Slider(
                            value = frostedBlurRadius,
                            onValueChange = onFrostedBlurRadiusChange,
                            valueRange = 0f..40f
                        )
                    }
                }
            }
        }
    }
}

