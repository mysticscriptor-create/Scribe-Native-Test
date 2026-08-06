package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.primaloptima.scribe.MainActivity
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.ui.theme.*
import com.primaloptima.scribe.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun DashboardTabContent(
    vm: HomeViewModel,
    allBooks: List<Book>,
    onOpenNote: (noteId: String, bookId: String) -> Unit,
    onOpenBook: (Book) -> Unit,
    onGoToStats: () -> Unit,
    onGoToBooks: () -> Unit,
    onOpenSheets: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as ScribeApp).prefs }

    val ongoingBookId by vm.ongoingProjectBookId.collectAsState()
    val chapters by vm.ongoingProjectChapters.collectAsState()
    val projectAllNotes by vm.ongoingProjectAllNotes.collectAsState()
    val currentStreak by vm.currentStreak.collectAsState()

    val ongoingBook = remember(ongoingBookId, allBooks) {
        ongoingBookId?.let { id -> allBooks.firstOrNull { it.id == id } }
    }

    // Stats derived from real data
    val totalProjectWords = remember(projectAllNotes) {
        projectAllNotes.sumOf { it.content.split("\\s+".toRegex()).count { w -> w.isNotBlank() } }
    }
    val dailyGoal = prefs.dailyGoal
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayWords = remember(todayStr) { prefs.getTodayWords(todayStr) }

    // 7-day history for the week bar chart
    val weekData = remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateStr = fmt.format(cal.time)
            val label = dayFmt.format(cal.time).take(1) // M, T, W…
            Pair(label, prefs.getTodayWords(dateStr))
        }
    }

    // Book picker bottom sheet state
    var showBookPicker by remember { mutableStateOf(false) }

    val hazeState = LocalHazeState.current
    val accentColor = LocalAccentColor.current

    // Supply LocalBarBlurBitmap as the one-shot source for all frosted cards on
    // pre-API-31 devices. Cards are always visible (no screen-capture needed), so
    // the static pre-blurred wallpaper bitmap is the correct placeholder.
    CompositionLocalProvider(LocalOneShotBitmap provides LocalBarBlurBitmap.current) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Greeting header ───────────────────────────────────────────────────
        item {
            DashboardGreeting(
                streak = currentStreak,
                accentColor = accentColor
            )
        }

        // ── Current Project card ──────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (ongoingBook == null) {
                NoProjectCard(
                    accentColor = accentColor,
                    hazeState = hazeState,
                    onSetProject = { showBookPicker = true }
                )
            } else {
                CurrentProjectCard(
                    book = ongoingBook,
                    chapters = chapters,
                    totalWords = totalProjectWords,
                    accentColor = accentColor,
                    hazeState = hazeState,
                    onContinueWriting = {
                        val latest = chapters.firstOrNull()
                        if (latest != null) onOpenNote(latest.id, latest.bookId)
                    },
                    onNewChapter = {
                        vm.createChapter { note ->
                            onOpenNote(note.id, note.bookId)
                        }
                    },
                    onOpenBook = { onOpenBook(ongoingBook) }
                )
            }
        }

        // ── Quick Actions ─────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            DashboardSectionLabel("Quick Actions")
            Spacer(modifier = Modifier.height(10.dp))
            QuickActionsRow(
                ongoingBook = ongoingBook,
                chapters = chapters,
                accentColor = accentColor,
                hazeState = hazeState,
                onOpenNote = onOpenNote,
                onOpenBook = { ongoingBook?.let { onOpenBook(it) } },
                onGoToStats = onGoToStats,
                onOpenSheets = onOpenSheets,
                onGoToBooks = onGoToBooks
            )
        }

        // ── Progress cards ────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(20.dp))
            DashboardSectionLabel("Progress")
            Spacer(modifier = Modifier.height(10.dp))
            ProgressCardsRow(
                todayWords = todayWords,
                dailyGoal = dailyGoal,
                weekData = weekData,
                accentColor = accentColor,
                hazeState = hazeState
            )
        }

        // ── Recent Chapters ───────────────────────────────────────────────────
        if (ongoingBook != null && chapters.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DashboardSectionLabel("Recent Chapters", inline = true)
                    TextButton(onClick = { ongoingBook?.let { onOpenBook(it) } }) {
                        Text("See All", fontSize = 12.sp, color = accentColor)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(chapters.take(3), key = { it.id }) { chapter ->
                RecentChapterRow(
                    note = chapter,
                    accentColor = accentColor,
                    hazeState = hazeState,
                    onClick = { onOpenNote(chapter.id, chapter.bookId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    } // end CompositionLocalProvider(LocalOneShotBitmap)

    // ── Book picker bottom sheet ──────────────────────────────────────────────
    if (showBookPicker) {
        BookPickerSheet(
            books = allBooks,
            currentOngoingId = ongoingBookId,
            accentColor = accentColor,
            onSelect = { book ->
                vm.setOngoingProject(book.id)
                showBookPicker = false
            },
            onDismiss = { showBookPicker = false }
        )
    }
}

// ── Greeting ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardGreeting(
    streak: Int,
    accentColor: Color
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }

    val (textColor, textMod) = rememberAdaptiveTextColor(
        fallback = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greeting,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = textMod
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
            }
            Text(
                text = "Let's bring your stories to life.",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.6f),
                modifier = textMod
            )
        }

        if (streak > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Text(
                        text = "$streak ${if (streak == 1) "Day" else "Days"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

// ── No project empty state ────────────────────────────────────────────────────

@Composable
private fun NoProjectCard(
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onSetProject: () -> Unit
) {
    val hasBgImage = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape = RoundedCornerShape(20.dp)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = frostedContainerColor(
                fallback = if (hasBgImage) solidSurface.copy(alpha = 0.85f)
                           else MaterialTheme.colorScheme.surface
            )
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        FrostedCardContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Book,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = accentColor
                    )
                }
                Text(
                    "No ongoing project",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Set a book as your ongoing project to track chapters, word count, and daily progress here.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onSetProject,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Ongoing Project")
                }
            }
        }
    }
}

// ── Current Project card ──────────────────────────────────────────────────────

@Composable
private fun CurrentProjectCard(
    book: Book,
    chapters: List<Note>,
    totalWords: Int,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onContinueWriting: () -> Unit,
    onNewChapter: () -> Unit,
    onOpenBook: () -> Unit
) {
    val hasBgImage = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape = RoundedCornerShape(20.dp)
    val context = LocalContext.current

    val lastChapter = chapters.firstOrNull()
    val lastEditedText = lastChapter?.let {
        val wc = it.content.split("\\s+".toRegex()).count { w -> w.isNotBlank() }
        "${it.name}  •  $wc words"
    }
    val lastTimestamp = lastChapter?.let { formatRelativeTime(it.updatedAt) }

    // Animated progress bar
    val targetGoal = 120_000 // classic novel length as a sensible milestone
    val progressFraction = (totalWords.toFloat() / targetGoal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "project-progress"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = frostedContainerColor(
                fallback = if (hasBgImage) solidSurface.copy(alpha = 0.85f)
                           else MaterialTheme.colorScheme.surface
            )
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        FrostedCardContent {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Cover + meta ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cover thumbnail — tall hero matching image 2 proportions
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 136.dp)
                            .shadow(8.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenBook() }
                    ) {
                        if (book.coverUri != null) {
                            AsyncImage(
                                model = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                    ImageRequest.Builder(context)
                                        .data(book.coverUri)
                                        .allowHardware(false)
                                        .build()
                                } else {
                                    book.coverUri
                                },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = accentColor
                                )
                            }
                        }
                    }

                    // Title + stats
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "CURRENT PROJECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                            color = accentColor
                        )
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Word count + percentage
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = formatWordCount(totalWords),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ ${formatWordCount(targetGoal)} words",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        // Progress bar with inline percentage label (image 2 style)
                        val barTrack = MaterialTheme.colorScheme.outlineVariant
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                            ) {
                                val radius = size.height / 2f
                                drawRoundRect(
                                    color = barTrack,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                                )
                                if (animatedProgress > 0f) {
                                    drawRoundRect(
                                        color = accentColor,
                                        size = Size(size.width * animatedProgress, size.height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
                                    )
                                }
                            }
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        }

                        // Last edited
                        if (lastChapter != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Article,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                                Column {
                                    Text(
                                        text = lastChapter.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = lastTimestamp ?: "",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Action buttons ────────────────────────────────────────────
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Continue Writing — primary CTA
                    Button(
                        onClick = onContinueWriting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = chapters.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (chapters.isEmpty()) "No chapters yet" else "Continue Writing",
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    // New chapter — secondary
                    OutlinedButton(
                        onClick = onNewChapter,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Chapter",
                            modifier = Modifier.size(16.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 13.sp, color = accentColor)
                    }
                }
            }
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    ongoingBook: Book?,
    chapters: List<Note>,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onOpenNote: (noteId: String, bookId: String) -> Unit,
    onOpenBook: () -> Unit,
    onGoToStats: () -> Unit,
    onOpenSheets: () -> Unit,
    onGoToBooks: () -> Unit
) {
    val actions = listOf(
        Triple(Icons.Default.Edit,       "Write",      {
            val latest = chapters.firstOrNull()
            if (latest != null) onOpenNote(latest.id, latest.bookId)
            else onGoToBooks()
        }),
        Triple(Icons.Default.FormatListBulleted, "Outline", { onOpenBook() }),
        Triple(Icons.Default.People,     "Characters", { onOpenBook() }),
        Triple(Icons.Default.Public,     "World",      { onOpenSheets() }),
        Triple(Icons.Default.BarChart,   "Stats",      { onGoToStats() })
    )

    val hasBgImage = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEachIndexed { index, (icon, label, action) ->
            val isWrite = index == 0  // "Write" is always the first action — primary CTA
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
                    .then(
                        // Write button: accent background (no frosted, always solid)
                        // Others: frosted card modifier as normal
                        if (isWrite) Modifier else Modifier.frostedCard(hazeState, shape = cardShape)
                    )
                    .clickable { action() },
                shape = cardShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isWrite) accentColor
                    else frostedContainerColor(
                        fallback = if (hasBgImage) solidSurface.copy(alpha = 0.82f)
                                   else MaterialTheme.colorScheme.surface
                    )
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                // Write button uses plain content (no frosted wrapper — it's a solid accent card)
                if (isWrite) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                } else {
                    FrostedCardContent {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp),
                                tint = accentColor
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Progress Cards ────────────────────────────────────────────────────────────

@Composable
private fun ProgressCardsRow(
    todayWords: Int,
    dailyGoal: Int,
    weekData: List<Pair<String, Int>>,  // (dayLabel, wordCount)
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?
) {
    val hasBgImage = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape = RoundedCornerShape(16.dp)
    val fallbackColor = if (hasBgImage) solidSurface.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.surface

    val weekTotal = weekData.sumOf { it.second }
    val weekGoal = dailyGoal * 7

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Today's Progress donut
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.85f)
                .frostedCard(hazeState, shape = cardShape),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = frostedContainerColor(fallback = fallbackColor)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            FrostedCardContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Today's Progress",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    DonutChart(
                        progress = if (dailyGoal > 0) (todayWords.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f,
                        centerLabel = formatWordCount(todayWords),
                        accentColor = accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "/ ${formatWordCount(dailyGoal)} words",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        if (dailyGoal > 0) {
                            Text(
                                "${((todayWords.toFloat() / dailyGoal) * 100).toInt()}% of daily goal",
                                fontSize = 8.sp,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // This Week mini bar chart
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.85f)
                .frostedCard(hazeState, shape = cardShape),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = frostedContainerColor(fallback = fallbackColor)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            FrostedCardContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "This Week",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    MiniWeekBar(
                        weekData = weekData,
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${formatWordCount(weekTotal)} words",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            "/ ${formatWordCount(weekGoal)} goal",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }

        // Monthly Goal donut
        ElevatedCard(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.85f)
                .frostedCard(hazeState, shape = cardShape),
            shape = cardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = frostedContainerColor(fallback = fallbackColor)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            FrostedCardContent {
                val cal = Calendar.getInstance()
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthGoal = dailyGoal * daysInMonth

                // Approximate month words from weekData * ~4.3 weeks, clamped sensibly
                val monthProgress = if (monthGoal > 0)
                    (weekTotal.toFloat() * (daysInMonth / 7f) / monthGoal).coerceIn(0f, 1f)
                else 0f

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Monthly Goal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    DonutChart(
                        progress = monthProgress,
                        centerLabel = "${(monthProgress * 100).toInt()}%",
                        accentColor = accentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "/ ${formatWordCount(monthGoal)} words",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Text(
                            "${formatWordCount((weekTotal * (daysInMonth / 7f)).toInt())} written",
                            fontSize = 8.sp,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ── Recent Chapter row ────────────────────────────────────────────────────────

@Composable
private fun RecentChapterRow(
    note: Note,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onClick: () -> Unit
) {
    val hasBgImage = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape = RoundedCornerShape(14.dp)
    val wordCount = remember(note.content) {
        note.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape)
            .clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = frostedContainerColor(
                fallback = if (hasBgImage) solidSurface.copy(alpha = 0.82f)
                           else MaterialTheme.colorScheme.surface
            )
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        FrostedCardContent {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Timestamp • word count on one line — image 2 style
                    Text(
                        text = "${formatRelativeTime(note.updatedAt)}  •  $wordCount words",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

// ── Book picker bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPickerSheet(
    books: List<Book>,
    currentOngoingId: String?,
    accentColor: Color,
    onSelect: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val solidSurface = LocalSolidSurface.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = solidSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Set Ongoing Project",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "A Chapters folder will be created in the selected book.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No books yet. Create a book first.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                books.forEach { book ->
                    val isSelected = book.id == currentOngoingId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(book) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mini cover
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 50.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            val context = LocalContext.current
                            if (book.coverUri != null) {
                                AsyncImage(
                                    model = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                        ImageRequest.Builder(context)
                                            .data(book.coverUri)
                                            .allowHardware(false)
                                            .build()
                                    } else book.coverUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = accentColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = book.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Current project",
                                modifier = Modifier.size(18.dp),
                                tint = accentColor
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

// ── Chart primitives ──────────────────────────────────────────────────────────

@Composable
private fun DonutChart(
    progress: Float,
    centerLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "donut-sweep"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.14f
            val inset = stroke / 2f
            val oval = androidx.compose.ui.geometry.Rect(
                left = inset, top = inset,
                right = size.width - inset, bottom = size.height - inset
            )

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(oval.left, oval.top),
                size = Size(oval.width, oval.height),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Fill
            if (animatedSweep > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(oval.left, oval.top),
                    size = Size(oval.width, oval.height),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = centerLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniWeekBar(
    weekData: List<Pair<String, Int>>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(weekData) {
        (weekData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val animatedBars = weekData.mapIndexed { i, (label, count) ->
        val anim by animateFloatAsState(
            targetValue = count.toFloat() / maxVal,
            animationSpec = tween(400 + i * 40, easing = FastOutSlowInEasing),
            label = "bar-$i"
        )
        Triple(label, count, anim)
    }

    Canvas(modifier = modifier) {
        val barCount = animatedBars.size
        val spacing = size.width * 0.04f
        val totalSpacing = spacing * (barCount - 1)
        val barW = (size.width - totalSpacing) / barCount
        val chartH = size.height
        val radius = barW / 2f

        animatedBars.forEachIndexed { i, (_, _, fraction) ->
            val left = i * (barW + spacing)
            val barH = (fraction * chartH).coerceAtLeast(4f)
            val top = chartH - barH

            // Track (full height, dim)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barW, chartH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
            // Fill
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun DashboardSectionLabel(text: String, inline: Boolean = false) {
    val (color, mod) = rememberAdaptiveTextColor(
        fallback = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = color.copy(alpha = 0.6f),
        modifier = Modifier
            .then(if (inline) Modifier else Modifier.padding(horizontal = 16.dp))
            .then(mod)
    )
}

private fun formatWordCount(count: Int): String {
    return when {
        count >= 1_000 -> "${count / 1_000}k"
        else           -> "$count"
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMin = diffMs / 60_000
    val diffHours = diffMs / 3_600_000
    val diffDays = diffMs / 86_400_000

    return when {
        diffMin < 1    -> "Just now"
        diffMin < 60   -> "$diffMin min ago"
        diffHours < 24 -> {
            val fmt = SimpleDateFormat("h:mm a", Locale.US)
            "Today, ${fmt.format(Date(timestamp))}"
        }
        diffDays == 1L -> {
            val fmt = SimpleDateFormat("h:mm a", Locale.US)
            "Yesterday, ${fmt.format(Date(timestamp))}"
        }
        diffDays < 7   -> {
            val fmt = SimpleDateFormat("EEE, h:mm a", Locale.US)
            fmt.format(Date(timestamp))
        }
        else -> {
            val fmt = SimpleDateFormat("MMM d", Locale.US)
            fmt.format(Date(timestamp))
        }
    }
}
