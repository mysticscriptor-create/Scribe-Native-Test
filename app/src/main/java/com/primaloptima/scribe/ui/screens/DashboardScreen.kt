package com.primaloptima.scribe.ui.screens

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.airbnb.lottie.compose.*
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.ui.theme.*
import com.primaloptima.scribe.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.basicMarquee

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
    val chapters      by vm.ongoingProjectChapters.collectAsState()
    val projectAllNotes by vm.ongoingProjectAllNotes.collectAsState()
    val currentStreak by vm.currentStreak.collectAsState()

    val ongoingBook = remember(ongoingBookId, allBooks) {
        ongoingBookId?.let { id -> allBooks.firstOrNull { it.id == id } }
    }

    // Total word count across all notes in the ongoing project
    val totalProjectWords = remember(projectAllNotes) {
        projectAllNotes.sumOf { it.content.split("\\s+".toRegex()).count { w -> w.isNotBlank() } }
    }

    val dailyGoal = prefs.dailyGoal
    val todayStr  = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayWords = remember(todayStr) { prefs.getTodayWords(todayStr) }

    // 7-day history for the week bar chart (oldest → newest)
    val weekData = remember {
        val fmt    = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        (6 downTo 0).map { daysAgo ->
            val cal     = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateStr = fmt.format(cal.time)
            val label   = dayFmt.format(cal.time).take(1)
            Pair(label, prefs.getTodayWords(dateStr))
        }
    }

    var showBookPicker by remember { mutableStateOf(false) }

    val hazeState   = LocalHazeState.current
    val accentColor = LocalAccentColor.current

    CompositionLocalProvider(LocalOneShotBitmap provides LocalBarBlurBitmap.current) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Greeting ──────────────────────────────────────────────────────
            item {
                DashboardGreeting(streak = currentStreak, accentColor = accentColor)
            }

            // ── Current Project card (or empty state) ─────────────────────────
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (ongoingBook == null) {
                    NoProjectCard(
                        accentColor  = accentColor,
                        hazeState    = hazeState,
                        onSetProject = { showBookPicker = true }
                    )
                } else {
                    CurrentProjectCard(
                        book             = ongoingBook,
                        chapters         = chapters,
                        totalWords       = totalProjectWords,
                        accentColor      = accentColor,
                        hazeState        = hazeState,
                        onContinueWriting = {
                            val latest = chapters.firstOrNull()
                            if (latest != null) onOpenNote(latest.id, latest.bookId)
                        },
                        onNewChapter = {
                            vm.createChapter { note -> onOpenNote(note.id, note.bookId) }
                        },
                        onOpenBook = { onOpenBook(ongoingBook) }
                    )
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                QuickActionsRow(
                    ongoingBook  = ongoingBook,
                    chapters     = chapters,
                    accentColor  = accentColor,
                    hazeState    = hazeState,
                    onOpenNote   = onOpenNote,
                    onOpenBook   = { ongoingBook?.let { onOpenBook(it) } },
                    onGoToStats  = onGoToStats,
                    onOpenSheets = onOpenSheets,
                    onGoToBooks  = onGoToBooks
                )
            }

            // ── Writing Progress (unified card) ───────────────────────────────
            item {
                Spacer(modifier = Modifier.height(20.dp))
                WritingProgressCard(
                    todayWords   = todayWords,
                    dailyGoal    = dailyGoal,
                    weekData     = weekData,
                    streak       = currentStreak,
                    totalWords   = totalProjectWords,
                    accentColor  = accentColor,
                    hazeState    = hazeState,
                    onGoToStats  = onGoToStats
                )
            }

            // ── Recent Chapters ───────────────────────────────────────────────
            if (ongoingBook != null && chapters.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    RecentChaptersCard(
                        chapters    = chapters.take(3),
                        accentColor = accentColor,
                        hazeState   = hazeState,
                        onSeeAll    = { ongoingBook?.let { onOpenBook(it) } },
                        onOpenNote  = onOpenNote
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // ── Book picker bottom sheet ──────────────────────────────────────────────
    if (showBookPicker) {
        BookPickerSheet(
            books            = allBooks,
            currentOngoingId = ongoingBookId,
            accentColor      = accentColor,
            onSelect = { book ->
                vm.setOngoingProject(book.id)
                showBookPicker = false
            },
            onDismiss = { showBookPicker = false }
        )
    }
}

// ── Section 1: Greeting ───────────────────────────────────────────────────────

@Composable
private fun DashboardGreeting(streak: Int, accentColor: Color) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else      -> "Good evening"
    }
    // Rotating subtitles by time of day
    val subtitle = when {
        hour < 12 -> "The blank page is waiting for you."
        hour < 17 -> "Every word shapes your story."
        else      -> "Great stories begin with one more sentence."
    }

    val (textColor, textMod) = rememberAdaptiveTextColor(
        fallback = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: greeting text
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = greeting,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textColor,
                    modifier   = textMod
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint     = accentColor
                )
            }
            Text(
                text     = subtitle,
                fontSize = 13.sp,
                color    = textColor.copy(alpha = 0.6f),
                modifier = textMod
            )
        }

        // Right: streak pill (two lines — number + label)
        if (streak > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔥", fontSize = 14.sp)
                        Text(
                            text       = "$streak ${if (streak == 1) "Day" else "Days"}",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                    Text(
                        text     = "Writing Streak",
                        fontSize = 10.sp,
                        color    = accentColor.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Section 2a: No Project empty state ───────────────────────────────────────

@Composable
private fun NoProjectCard(
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onSetProject: () -> Unit
) {
    val hasBgImage   = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape    = RoundedCornerShape(20.dp)

    // Lottie animation from assets/shelf_empty.json
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("shelf_empty.json")
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape  = cardShape,
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lottie animation instead of static icon
                LottieAnimation(
                    composition = composition,
                    iterations  = LottieConstants.IterateForever,
                    modifier    = Modifier.size(80.dp)
                )
                Text(
                    "No ongoing project",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Set a book as your ongoing project to track chapters, word count, and daily progress here.",
                    fontSize  = 13.sp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSetProject,
                    colors  = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set Ongoing Project", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Section 2b: Current Project card ─────────────────────────────────────────

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
    val hasBgImage   = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape    = RoundedCornerShape(20.dp)
    val context      = LocalContext.current

    val lastChapter   = chapters.firstOrNull()
    val lastTimestamp = lastChapter?.let { formatRelativeTime(it.updatedAt) }
    val chapterCount  = chapters.size

    // Novel progress toward 120k-word milestone
    val targetGoal       = 120_000
    val progressFraction = (totalWords.toFloat() / targetGoal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue    = progressFraction,
        animationSpec  = tween(600, easing = FastOutSlowInEasing),
        label          = "project-progress"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape  = cardShape,
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

                // ── Cover + meta row ──────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth()) {

                    // Cover: flush to card left edge, clips to card corners on left side
                    Box(
                        modifier = Modifier
                            .width(108.dp)
                            .height(152.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
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
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder when no cover image
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint     = accentColor
                                )
                            }
                        }
                    }

                    // Right column: all metadata
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(152.dp)
                            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top block
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            // "CURRENT PROJECT" overline
                            Text(
                                text          = "CURRENT PROJECT",
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp,
                                color         = accentColor
                            )
                            // Book title
                            Text(
                                text       = book.title,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 17.sp,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            // Metadata row: chapters · words
                            Row(
                                verticalAlignment      = Alignment.CenterVertically,
                                horizontalArrangement  = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text     = "$chapterCount ${if (chapterCount == 1) "Chapter" else "Chapters"}",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text     = "·",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text     = "${formatWordCount(totalWords)} / ${formatWordCount(targetGoal)} words",
                                    fontSize = 11.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Progress bar + percent
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThinProgressBar(
                                    progress    = animatedProgress,
                                    accentColor = accentColor,
                                    modifier    = Modifier.weight(1f).height(5.dp)
                                )
                                Text(
                                    text       = "${(progressFraction * 100).toInt()}%",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = accentColor
                                )
                            }
                        }

                        // Bottom block: last edited info
                        if (lastChapter != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text       = "Last edited",
                                    fontSize   = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Article,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text     = lastChapter.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text     = lastTimestamp ?: "",
                                    fontSize = 10.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }

                // ── Action buttons ────────────────────────────────────────────
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick  = onContinueWriting,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape    = RoundedCornerShape(12.dp),
                        enabled  = chapters.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = if (chapters.isEmpty()) "No chapters yet" else "Continue Writing",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1
                        )
                    }
                    OutlinedButton(
                        onClick  = onNewChapter,
                        modifier = Modifier.height(44.dp),
                        shape    = RoundedCornerShape(12.dp),
                        border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Chapter",
                            modifier = Modifier.size(16.dp),
                            tint     = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 14.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Section 3: Quick Actions row ──────────────────────────────────────────────

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
        Triple(Icons.Default.Edit,               "Write",      {
            val latest = chapters.firstOrNull()
            if (latest != null) onOpenNote(latest.id, latest.bookId) else onGoToBooks()
        }),
        Triple(Icons.Default.FormatListBulleted, "Outline",    { onOpenBook() }),
        Triple(Icons.Default.People,             "Characters", { onOpenBook() }),
        Triple(Icons.Default.Public,             "World",      { onOpenSheets() }),
        Triple(Icons.Default.BarChart,           "Stats",      { onGoToStats() })
    )

    val hasBgImage   = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape    = RoundedCornerShape(16.dp)

    val (labelColor, _) = rememberAdaptiveTextColor(
        fallback = MaterialTheme.colorScheme.onSurface
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Section header with "See All" link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text          = "Quick Actions",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color         = labelColor.copy(alpha = 0.6f)
            )
            Text(
                text       = "See All",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = accentColor,
                modifier   = Modifier.clickable { onGoToBooks() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Tiles row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEachIndexed { index, (icon, label, action) ->
                val isWrite = index == 0
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.85f)
                        .then(
                            if (isWrite) Modifier
                            else Modifier.frostedCard(hazeState, shape = cardShape)
                        )
                        .clickable { action() },
                    shape  = cardShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isWrite) accentColor
                        else frostedContainerColor(
                            fallback = if (hasBgImage) solidSurface.copy(alpha = 0.82f)
                                       else MaterialTheme.colorScheme.surface
                        )
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    if (isWrite) {
                        Column(
                            modifier              = Modifier.fillMaxSize(),
                            horizontalAlignment   = Alignment.CenterHorizontally,
                            verticalArrangement   = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = icon,
                                contentDescription = label,
                                modifier           = Modifier.size(24.dp),
                                tint               = Color.White
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text       = label,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                                maxLines   = 1
                            )
                        }
                    } else {
                        FrostedCardContent {
                            Column(
                                modifier              = Modifier.fillMaxSize(),
                                horizontalAlignment   = Alignment.CenterHorizontally,
                                verticalArrangement   = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector        = icon,
                                    contentDescription = label,
                                    modifier           = Modifier.size(24.dp),
                                    tint               = accentColor
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text       = label,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    maxLines   = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Section 4: Writing Progress — unified card ────────────────────────────────

@Composable
private fun WritingProgressCard(
    todayWords: Int,
    dailyGoal: Int,
    weekData: List<Pair<String, Int>>,
    streak: Int,
    totalWords: Int,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onGoToStats: () -> Unit
) {
    val hasBgImage   = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape    = RoundedCornerShape(20.dp)
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline

    // Derived stats
    val weekTotal      = weekData.sumOf { it.second }
    val weekGoal       = dailyGoal * 7
    val cal            = Calendar.getInstance()
    val daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthGoal      = dailyGoal * daysInMonth
    val monthWritten   = (weekTotal * (daysInMonth / 7f)).toInt()
    val monthProgress  = if (monthGoal > 0)
        (monthWritten.toFloat() / monthGoal).coerceIn(0f, 1f) else 0f
    val animatedMonthProgress by animateFloatAsState(
        targetValue   = monthProgress,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "month-progress"
    )

    // Next milestone
    val milestones    = listOf(1_000, 5_000, 10_000, 25_000, 50_000, 75_000, 100_000, 120_000)
    val nextMilestone = milestones.firstOrNull { it > totalWords } ?: (totalWords + 10_000)
    val wordsToGo     = nextMilestone - totalWords

    // Streak dot indicators from weekData
    val streakDots = weekData.map { (label, count) -> Pair(label, count > 0) }

    // Today goal met?
    val goalMet = dailyGoal > 0 && todayWords >= dailyGoal

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape  = cardShape,
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

                // ── Card header ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Your Progress",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = onSurface
                    )
                    Text(
                        text       = "Set Goal ›",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = accentColor,
                        modifier   = Modifier.clickable { onGoToStats() }
                    )
                }
                HorizontalDivider(color = outlineColor.copy(alpha = 0.1f))

                // ── Three stat columns ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Column A: Today
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Icon in accent circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint     = accentColor
                            )
                        }
                        Text(
                            text     = "Today",
                            fontSize = 10.sp,
                            color    = onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text       = formatWordCount(todayWords),
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = onSurface
                        )
                        Text(
                            text     = "/ ${formatWordCount(dailyGoal)} words",
                            fontSize = 11.sp,
                            color    = onSurface.copy(alpha = 0.45f)
                        )
                        if (goalMet) {
                            Text(
                                text       = "Goal reached! 🎉",
                                fontSize   = 10.sp,
                                color      = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(90.dp)
                            .background(outlineColor.copy(alpha = 0.12f))
                            .align(Alignment.CenterVertically)
                    )

                    // Column B: Streak
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6B00).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 13.sp)
                        }
                        Text(
                            text       = "Streak",
                            fontSize   = 10.sp,
                            color      = onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text       = "$streak",
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = onSurface
                        )
                        Text(
                            text     = "days",
                            fontSize = 11.sp,
                            color    = onSurface.copy(alpha = 0.45f)
                        )
                        // Day dots: M T W T F S S
                        StreakDots(streakDots = streakDots, accentColor = accentColor)
                    }

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(90.dp)
                            .background(outlineColor.copy(alpha = 0.12f))
                            .align(Alignment.CenterVertically)
                    )

                    // Column C: Monthly Goal
                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint     = accentColor
                            )
                        }
                        Text(
                            text       = "Monthly",
                            fontSize   = 10.sp,
                            color      = onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text       = "${(monthProgress * 100).toInt()}%",
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = onSurface
                        )
                        Text(
                            text     = "${formatWordCount(monthWritten)} written",
                            fontSize = 11.sp,
                            color    = onSurface.copy(alpha = 0.45f)
                        )
                        // Thin progress bar for monthly
                        ThinProgressBar(
                            progress    = animatedMonthProgress,
                            accentColor = accentColor,
                            modifier    = Modifier
                                .fillMaxWidth(0.75f)
                                .height(4.dp)
                                .padding(top = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = outlineColor.copy(alpha = 0.1f))

                // ── This Week bar chart ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "This Week",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text     = "${formatWordCount(weekTotal)} / ${formatWordCount(weekGoal)} words",
                            fontSize = 11.sp,
                            color    = onSurface.copy(alpha = 0.45f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MiniWeekBar(
                        weekData    = weekData,
                        accentColor = accentColor,
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }

                HorizontalDivider(color = outlineColor.copy(alpha = 0.1f))

                // ── Next Milestone row ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoToStats() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint     = onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text       = "Next Milestone",
                            fontSize   = 12.sp,
                            color      = onSurface.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = "${formatWordCount(wordsToGo)} words to ${formatWordCount(nextMilestone)}",
                            fontSize   = 12.sp,
                            color      = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint     = accentColor
                        )
                    }
                }

                // ── Motivational quote (only when 0 words written today) ──────
                if (todayWords == 0) {
                    HorizontalDivider(color = outlineColor.copy(alpha = 0.1f))
                    Text(
                        text      = "\" A page a day builds a world. \"",
                        fontSize  = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color     = onSurface.copy(alpha = 0.38f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// ── Section 5: Recent Chapters card ──────────────────────────────────────────

@Composable
private fun RecentChaptersCard(
    chapters: List<Note>,
    accentColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onSeeAll: () -> Unit,
    onOpenNote: (noteId: String, bookId: String) -> Unit
) {
    val hasBgImage   = localHasBgImage()
    val solidSurface = LocalSolidSurface.current
    val cardShape    = RoundedCornerShape(16.dp)
    val outlineColor = MaterialTheme.colorScheme.outline

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedCard(hazeState, shape = cardShape),
        shape  = cardShape,
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
                // ── Header ────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "Continue where you left off",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text       = "See All ›",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = accentColor,
                        modifier   = Modifier.clickable { onSeeAll() }
                    )
                }
                HorizontalDivider(color = outlineColor.copy(alpha = 0.12f))

                // ── Chapter rows ──────────────────────────────────────────────
                chapters.forEachIndexed { index, chapter ->
                    val wordCount = remember(chapter.content) {
                        chapter.content.split("\\s+".toRegex()).count { it.isNotBlank() }
                    }
                    // First non-blank line of content as preview
                    val previewLine = remember(chapter.content) {
                        chapter.content
                            .lines()
                            .firstOrNull { it.isNotBlank() }
                            ?.trim()
                            ?.take(70)
                            ?: ""
                    }
                    val isFirst = index == 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenNote(chapter.id, chapter.bookId) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon box
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = outlineColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Article,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint     = accentColor.copy(alpha = 0.8f)
                            )
                        }

                        // Text column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = chapter.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp,
                                color      = MaterialTheme.colorScheme.onSurface,
                                maxLines   = 1,
                                modifier   = Modifier.basicMarquee()
                            )
                            Text(
                                text     = "${formatRelativeTime(chapter.updatedAt)}  ·  $wordCount words",
                                fontSize = 11.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                maxLines = 1
                            )
                            // Preview line — first sentence of chapter content
                            if (previewLine.isNotEmpty()) {
                                Text(
                                    text     = previewLine,
                                    fontSize = 12.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                Text(
                                    text      = "No content yet",
                                    fontSize  = 12.sp,
                                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }

                        // "Continue" button on the most recent chapter only
                        if (isFirst) {
                            OutlinedButton(
                                onClick  = { onOpenNote(chapter.id, chapter.bookId) },
                                modifier = Modifier.height(32.dp),
                                shape    = RoundedCornerShape(8.dp),
                                border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text       = "Continue",
                                    fontSize   = 11.sp,
                                    color      = accentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (index < chapters.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 64.dp, end = 14.dp),
                            color    = outlineColor.copy(alpha = 0.08f)
                        )
                    }
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
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val solidSurface = LocalSolidSurface.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = solidSurface,
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
                fontSize   = 18.sp,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "A Chapters folder will be created in the selected book.",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
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
                val context = LocalContext.current
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
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mini cover
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 50.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            if (book.coverUri != null) {
                                AsyncImage(
                                    model = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                        ImageRequest.Builder(context)
                                            .data(book.coverUri)
                                            .allowHardware(false)
                                            .build()
                                    } else book.coverUri,
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier.fillMaxSize()
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
                                        tint     = accentColor
                                    )
                                }
                            }
                        }

                        Text(
                            text       = book.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize   = 15.sp,
                            color      = if (isSelected) accentColor
                                         else MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Current project",
                                modifier = Modifier.size(18.dp),
                                tint     = accentColor
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

// ── Primitive: thin animated progress bar ────────────────────────────────────

@Composable
private fun ThinProgressBar(
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(accentColor)
        )
    }
}

// ── Primitive: streak day dots ────────────────────────────────────────────────

@Composable
private fun StreakDots(
    streakDots: List<Pair<String, Boolean>>,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        streakDots.forEach { (_, hasWords) ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasWords) accentColor
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

// ── Primitive: week bar chart (Canvas) ────────────────────────────────────────

@Composable
private fun MiniWeekBar(
    weekData: List<Pair<String, Int>>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val maxVal      = remember(weekData) { (weekData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1) }
    val trackColor  = MaterialTheme.colorScheme.outlineVariant
    val labelColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val animatedBars = weekData.mapIndexed { i, (label, count) ->
        val anim by animateFloatAsState(
            targetValue   = count.toFloat() / maxVal,
            animationSpec = tween(400 + i * 40, easing = FastOutSlowInEasing),
            label         = "bar-$i"
        )
        Triple(label, count, anim)
    }

    val labelHeightDp = 14.dp
    BoxWithConstraints(modifier = modifier) {
        val labelHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { labelHeightDp.toPx() }
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount     = animatedBars.size
            val spacing      = size.width * 0.04f
            val totalSpacing = spacing * (barCount - 1)
            val barW         = (size.width - totalSpacing) / barCount
            val chartH       = size.height - labelHeightPx
            val radius       = barW / 2f

            animatedBars.forEachIndexed { i, (label, _, fraction) ->
                val left = i * (barW + spacing)
                val barH = (fraction * chartH).coerceAtLeast(4f)
                val top  = chartH - barH

                drawRoundRect(
                    color        = trackColor,
                    topLeft      = Offset(left, 0f),
                    size         = Size(barW, chartH),
                    cornerRadius = CornerRadius(radius)
                )
                drawRoundRect(
                    color        = accentColor,
                    topLeft      = Offset(left, top),
                    size         = Size(barW, barH),
                    cornerRadius = CornerRadius(radius)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    left + barW / 2f,
                    size.height - labelHeightPx * 0.1f,
                    android.graphics.Paint().apply {
                        color     = labelColor.toArgb()
                        textSize  = labelHeightPx * 0.72f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

// ── Primitive: DonutChart (kept for potential use in StatsScreen) ─────────────

@Composable
private fun DonutChart(
    progress: Float,
    centerLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue   = progress * 360f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "donut-sweep"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface  = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.14f
            val inset  = stroke / 2f
            val oval   = androidx.compose.ui.geometry.Rect(
                left   = inset, top   = inset,
                right  = size.width - inset, bottom = size.height - inset
            )
            drawArc(
                color       = trackColor,
                startAngle  = -90f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = Offset(oval.left, oval.top),
                size        = Size(oval.width, oval.height),
                style       = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0f) {
                drawArc(
                    color       = accentColor,
                    startAngle  = -90f,
                    sweepAngle  = animatedSweep,
                    useCenter   = false,
                    topLeft     = Offset(oval.left, oval.top),
                    size        = Size(oval.width, oval.height),
                    style       = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text       = centerLabel,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = onSurface,
            maxLines   = 1
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun DashboardSectionLabel(text: String, inline: Boolean = false) {
    val (color, mod) = rememberAdaptiveTextColor(
        fallback = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text          = text,
        fontSize      = 13.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color         = color.copy(alpha = 0.6f),
        modifier      = Modifier
            .then(if (inline) Modifier else Modifier.padding(horizontal = 16.dp))
            .then(mod)
    )
}

private fun formatWordCount(count: Int): String = when {
    count >= 1_000 -> "${count / 1_000}k"
    else           -> "$count"
}

private fun formatRelativeTime(timestamp: Long): String {
    val now      = System.currentTimeMillis()
    val diffMs   = now - timestamp
    val diffMin  = diffMs / 60_000
    val diffHours = diffMs / 3_600_000
    val diffDays  = diffMs / 86_400_000

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
