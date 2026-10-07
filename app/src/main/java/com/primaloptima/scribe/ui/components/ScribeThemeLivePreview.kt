package com.primaloptima.scribe.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.ui.ornaments.ManuscriptOrnament
import com.primaloptima.scribe.ui.ornaments.OrnamentRegistry
import com.primaloptima.scribe.ui.screens.DashboardContent
import com.primaloptima.scribe.ui.theme.LocalAppTheme
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.ScribeComposeTheme
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.util.ThemeManager
import com.primaloptima.scribe.util.model.AppTheme
import com.primaloptima.scribe.util.model.ThemeColors
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

private fun parseComposeColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(ThemeManager.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Preview pane presentation mode selector.
 */
enum class PreviewPaneMode(val label: String) {
    EDITOR("Editor"),
    DASHBOARD("Dashboard"),
    SPLIT("Split")
}

/**
 * ScribeThemeLivePreview
 *
 * Canonical live, real-time theme preview system that renders Scribe's actual production
 * UI composables (Dashboard + Editor) within simulated device frames.
 *
 * Any future UI evolutions made in [DashboardScreen] or [MainEditorScreen] automatically
 * propagate directly to this preview without duplicated or fake mock layouts.
 */
@Composable
fun ScribeThemeLivePreview(
    theme: AppTheme? = null,
    colors: ThemeColors? = null,
    themeName: String = "Preview Theme",
    fontFamily: String = "sans",
    fontSize: Float = 17f,
    lineHeight: Float = 1.7f,
    textAlignment: String = "left",
    sideMargins: Float = 24f,
    bgMode: String = "color",
    bgUri: String? = null,
    bgOpacity: Float = 0.35f,
    overlayEnabled: Boolean = false,
    overlayColor: String? = null,
    blurIntensity: Float = 15f,
    frostedGlassEnabled: Boolean = true,
    frostedTintEnabled: Boolean = true,
    frostedBlurRadius: Float = 15f,
    isDark: Boolean = false,
    selectedOrnamentId: String = "classic_flourish",
    modifier: Modifier = Modifier
) {
    val effectiveTheme = remember(
        theme, colors, themeName, fontFamily, fontSize, lineHeight,
        textAlignment, sideMargins, bgMode, bgUri, bgOpacity, overlayEnabled, overlayColor, blurIntensity,
        frostedGlassEnabled, frostedTintEnabled, frostedBlurRadius, isDark
    ) {
        if (theme != null) {
            theme
        } else {
            val resolvedColors = colors ?: ThemeColors()
            AppTheme(
                id = "live_preview_theme",
                name = themeName,
                isDark = isDark,
                colors = resolvedColors,
                fontFamily = fontFamily,
                fontSize = fontSize.toInt(),
                lineHeight = lineHeight,
                textAlignment = textAlignment,
                paddingHorizontal = sideMargins.toInt(),
                bgMode = bgMode,
                backgroundImageUri = bgUri,
                backgroundImageOpacity = bgOpacity,
                overlayEnabled = overlayEnabled,
                overlayColor = overlayColor,
                blurIntensity = blurIntensity,
                frostedGlassEnabled = frostedGlassEnabled,
                frostedTintEnabled = frostedTintEnabled,
                frostedBlurRadius = frostedBlurRadius
            )
        }
    }

    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    val availableModes = remember {
        listOf(PreviewPaneMode.SPLIT, PreviewPaneMode.DASHBOARD, PreviewPaneMode.EDITOR)
    }

    var selectedPaneMode by remember {
        mutableStateOf(PreviewPaneMode.SPLIT)
    }

    LaunchedEffect(availableModes) {
        if (selectedPaneMode !in availableModes) {
            selectedPaneMode = availableModes.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(10.dp)
    ) {
        // ── Header Bar: Mode Switcher ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Mode Selector Pills
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableModes.forEach { mode ->
                    val isSelected = selectedPaneMode == mode
                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        label = "pill_bg"
                    )
                    val pillTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        label = "pill_text"
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(pillBg)
                            .clickable { selectedPaneMode = mode }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = pillTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // ── Preview Body Wrapped in ScribeComposeTheme ────────────────────────
        ScribeComposeTheme(appTheme = effectiveTheme) {
            val previewHazeState = remember { HazeState() }
            CompositionLocalProvider(LocalHazeState provides previewHazeState) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                ) {
                    AnimatedContent(
                        targetState = selectedPaneMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "pane_mode_content"
                    ) { mode ->
                        when (mode) {
                            PreviewPaneMode.SPLIT -> {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        DeviceMockupFrame(
                                            title = "Dashboard",
                                            hazeState = previewHazeState,
                                            bgUri = if (effectiveTheme.themeScope == "editor_only") null else bgUri,
                                            bgOpacity = bgOpacity,
                                            overlayEnabled = overlayEnabled,
                                            overlayColor = overlayColor
                                        ) {
                                            DashboardPreviewScreen()
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        DeviceMockupFrame(
                                            title = "Editor",
                                            hazeState = previewHazeState,
                                            bgUri = bgUri,
                                            bgOpacity = bgOpacity,
                                            overlayEnabled = overlayEnabled,
                                            overlayColor = overlayColor
                                        ) {
                                            EditorPreviewScreen(selectedOrnamentId = selectedOrnamentId)
                                        }
                                    }
                                }
                            }

                            PreviewPaneMode.DASHBOARD -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .widthIn(max = 420.dp)
                                            .fillMaxWidth()
                                    ) {
                                        DeviceMockupFrame(
                                            title = "Dashboard",
                                            hazeState = previewHazeState,
                                            bgUri = if (effectiveTheme.themeScope == "editor_only") null else bgUri,
                                            bgOpacity = bgOpacity,
                                            overlayEnabled = overlayEnabled,
                                            overlayColor = overlayColor
                                        ) {
                                            DashboardPreviewScreen()
                                        }
                                    }
                                }
                            }

                            PreviewPaneMode.EDITOR -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .widthIn(max = 420.dp)
                                            .fillMaxWidth()
                                    ) {
                                        DeviceMockupFrame(
                                            title = "Editor",
                                            hazeState = previewHazeState,
                                            bgUri = bgUri,
                                            bgOpacity = bgOpacity,
                                            overlayEnabled = overlayEnabled,
                                            overlayColor = overlayColor
                                        ) {
                                            EditorPreviewScreen(selectedOrnamentId = selectedOrnamentId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Proportionally scales any full-scale mobile composable (virtual 360dp mobile canvas)
 * down to fit within compact preview frames.
 *
 * This turns cramped, overflowing screens into authentic, miniature mobile screens where
 * typography, margins, icons, cards, and decorations maintain correct visual proportions.
 */
@Composable
fun MiniaturePhoneScreen(
    virtualWidth: Dp = 360.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val containerWidthPx = constraints.maxWidth
        val containerHeightPx = constraints.maxHeight

        if (containerWidthPx <= 0 || containerHeightPx <= 0) {
            Box(modifier = Modifier.fillMaxSize())
            return@BoxWithConstraints
        }

        val virtualWidthPx = with(density) { virtualWidth.roundToPx() }.coerceAtLeast(1)
        val scale = (containerWidthPx.toFloat() / virtualWidthPx.toFloat()).coerceAtLeast(0.01f)
        val virtualHeightPx = (containerHeightPx / scale).roundToInt().coerceAtLeast(1)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, _ ->
                    val placeable = measurable.measure(
                        Constraints.fixed(virtualWidthPx, virtualHeightPx)
                    )
                    layout(containerWidthPx, containerHeightPx) {
                        placeable.place(0, 0)
                    }
                }
        ) {
            content()
        }
    }
}

/**
 * Clean simulated Android phone frame with border, rounded corners, and miniature scaling.
 */
@Composable
private fun DeviceMockupFrame(
    title: String,
    hazeState: HazeState,
    bgUri: String? = null,
    bgOpacity: Float = 0.35f,
    overlayEnabled: Boolean = false,
    overlayColor: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val frameShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(frameShape)
            .background(ScribeTheme.colors.surfaces.background)
            .border(
                width = 1.dp,
                color = ScribeTheme.colors.borders.subtle,
                shape = frameShape
            )
    ) {
        if (!bgUri.isNullOrEmpty()) {
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (overlayEnabled) 1f else bgOpacity)
            )
            if (overlayEnabled && bgOpacity > 0f) {
                val overlayComposeColor = overlayColor?.let { parseComposeColor(it, ScribeTheme.colors.surfaces.background) }
                    ?: ScribeTheme.colors.surfaces.background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayComposeColor.copy(alpha = bgOpacity))
                )
            }
        }

        // Screen Body with miniature proportional scaling
        MiniaturePhoneScreen(
            virtualWidth = 360.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

/**
 * Real Scribe Dashboard preview composition matching the goal miniature screen.
 * Directly renders DashboardContent with the sample ongoing book, quick actions, and progress.
 */
@Composable
private fun DashboardPreviewScreen() {
    DashboardContent(
        ongoingBook = Book(
            id = "preview_book_notes",
            title = "My Notes",
            summary = "Great stories begin with one more sentence.",
            tags = "Fantasy,Adventure,Romance",
            createdAt = 1700000000000L
        ),
        chapters = emptyList(),
        totalProjectWords = 2_000,
        totalTarget = 120_000,
        currentStreak = 1,
        todayWords = 2_000,
        dailyGoal = 500,
        weekData = listOf(
            Triple("M", 200, false),
            Triple("T", 450, false),
            Triple("W", 300, false),
            Triple("T", 600, false),
            Triple("F", 2000, true),
            Triple("S", 1200, true),
            Triple("S", 800, false)
        ),
        accentColor = ScribeTheme.colors.interaction.primary,
        contentPadding = PaddingValues(bottom = 24.dp)
    )
}

/**
 * Real Scribe Editor preview composition matching the goal miniature screen.
 * Renders the top search/note/word count bar, extensible ornament flourish divider,
 * and the rich manuscript prose with styled dialogue and monologue typography tokens.
 */
@Composable
private fun EditorPreviewScreen(
    selectedOrnamentId: String
) {
    val proseStyle = ScribeTheme.typography.prose
    val dialogueStyle = ScribeTheme.typography.dialogue
    val monologueStyle = ScribeTheme.typography.monologue
    val colors = ScribeTheme.colors.writing

    val align = when (ScribeTheme.typography.editor.textAlignment) {
        "justified" -> TextAlign.Justify
        "center" -> TextAlign.Center
        else -> TextAlign.Left
    }

    val paddingHorizontal = (LocalAppTheme.current?.paddingHorizontal ?: 16).dp.coerceIn(12.dp, 32.dp)
    val paragraphSpacing = ScribeTheme.typography.editor.paragraphSpacing.dp.coerceIn(6.dp, 24.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top Bar: Search, Title + Word Count Pill, Bookmark & More Actions ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search circular button
            Surface(
                shape = CircleShape,
                color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.65f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ScribeTheme.colors.content.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Note title + word count pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Note 3",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScribeTheme.colors.interaction.primary
                )
                Surface(
                    shape = CircleShape,
                    color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ScribeTheme.colors.borders.subtle)
                ) {
                    Text(
                        text = "464 words",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScribeTheme.colors.content.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Bookmark and More actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.65f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = ScribeTheme.colors.content.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.65f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = ScribeTheme.colors.content.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // ── Extensible Ornament Divider ───────────────────────────────────────
        val currentOrnament: ManuscriptOrnament = remember(selectedOrnamentId) {
            OrnamentRegistry.getById(selectedOrnamentId)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            currentOrnament.Render(
                tint = ScribeTheme.colors.interaction.primary.copy(alpha = 0.65f),
                modifier = Modifier
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Manuscript Prose Body with Dialogue & Monologue Styles ───────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(paragraphSpacing)
        ) {
            Text(
                text = "Dust settled over the ruined concourse. Shadows stretched long across the cracked concrete, swallowed by the cold silence of the hollowed-out building.",
                style = proseStyle.copy(color = colors.prose, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Ren leaned against a shattered pillar. Every breath dragged like glass through his lungs. His ribs screamed from the impact, but he kept his fingers pressed against the wound on his side to slow the slick heat spilling out.",
                style = proseStyle.copy(color = colors.prose, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "He's faster than the reports said.",
                style = monologueStyle.copy(
                    color = colors.monologue,
                    fontStyle = FontStyle.Italic,
                    textAlign = align
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Ren pulled his hand back. Crimson covered his skin.",
                style = proseStyle.copy(color = colors.prose, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "If I try to outrun him, I die in thirty seconds.",
                style = monologueStyle.copy(
                    color = colors.monologue,
                    fontStyle = FontStyle.Italic,
                    textAlign = align
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Heavy footsteps echoed from the far corridor. Slow. Deliberate. The sharp click of steel boots against stone cut through the quiet.",
                style = proseStyle.copy(color = colors.prose, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "“You can stop hiding, kid.”",
                style = dialogueStyle.copy(color = colors.dialogue, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "The voice was rough, scraping against the high walls.",
                style = proseStyle.copy(color = colors.prose, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "“Makes no difference. We both know how this ends.”",
                style = dialogueStyle.copy(color = colors.dialogue, textAlign = align),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
