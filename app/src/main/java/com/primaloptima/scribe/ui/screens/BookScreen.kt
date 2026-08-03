package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.os.Build
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.LocalOneShotBitmap
import com.primaloptima.scribe.ui.theme.LocalSolidSurface
import com.primaloptima.scribe.ui.theme.frostedBar
import com.primaloptima.scribe.ui.theme.frostedFab
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.frostedContainerColor
import com.primaloptima.scribe.ui.theme.rememberAdaptiveTextColor
import com.primaloptima.scribe.util.BitmapBlur
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.MainActivity
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.CoverUtils
import com.primaloptima.scribe.util.MarkdownUtil
import com.primaloptima.scribe.viewmodel.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    vm: BookViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val view = LocalView.current
    val blurRadiusPx = com.primaloptima.scribe.ui.theme.LocalFrostedBlurRadius.current.toInt().coerceIn(1, 25)
    var oneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var captured by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var noteToRename by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    var dialogOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val anyDialogOpen = showCreateNoteDialog || showCreateFolderDialog ||
            noteToRename != null || noteToDelete != null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(isFabExpanded) {
            if (isFabExpanded && !captured) {
                captured = true
                val raw = BitmapBlur.captureOnly(view)
                oneShotBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    raw?.let { BitmapBlur.blurBitmap(it, radius = blurRadiusPx) }
                }
            } else if (!isFabExpanded) {
                captured = false
                oneShotBitmap = null
            }
        }
        LaunchedEffect(anyDialogOpen) {
            if (!anyDialogOpen) dialogOneShotBitmap = null
        }
    }

    val captureForDialog: suspend (() -> Unit) -> Unit = { openDialog ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val raw = BitmapBlur.captureOnly(view)
            dialogOneShotBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                raw?.let { BitmapBlur.blurBitmap(it, radius = blurRadiusPx) }
            }
        }
        openDialog()
    }

    val book by vm.book.observeAsState()
    val notes by vm.notes.observeAsState(emptyList())
    val folders by vm.folders.observeAsState(emptyList())
    val viewMode by vm.viewMode.observeAsState(BookViewModel.ViewMode.LIST)
    val sortMode by vm.sortMode.observeAsState(BookViewModel.SortMode.DATE_UPDATED)

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFolderPath by remember { mutableStateOf("/") }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val b = book ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val savedUri = CoverUtils.saveCoverImage(context.applicationContext, b.id, uri)
            val db = (context.applicationContext as ScribeApp).database
            db.bookDao().updateCover(b.id, savedUri, System.currentTimeMillis())
            vm.reload()
        }
    }

    val swipeGestureModifier = Modifier.pointerInput(drawerState) {
        var startX = 0f
        var totalX = 0f
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startX = offset.x
                totalX = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                totalX += dragAmount
                if (drawerState.isClosed && startX < size.width * 0.5f && totalX > 36.dp.toPx()) {
                    change.consume()
                    scope.launch { drawerState.open() }
                }
            }
        )
    }

    val hazeState = LocalHazeState.current

    CompositionLocalProvider(LocalOneShotBitmap provides oneShotBitmap) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = book?.title ?: "Book Folders",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Main", fontWeight = FontWeight.Medium) },
                    selected = selectedFolderPath == "/",
                    onClick = {
                        selectedFolderPath = "/"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                folders.filter { it.path != "/" }.forEach { folder ->
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        label = { Text(folder.path, fontWeight = FontWeight.Medium) },
                        selected = selectedFolderPath == folder.path,
                        onClick = {
                            selectedFolderPath = folder.path
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                ElevatedButton(
                    onClick = { scope.launch { captureForDialog { showCreateFolderDialog = true } } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Folder", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.then(swipeGestureModifier),
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.frostedBar(hazeState),
                    title = {
                        Column {
                            val (titleColor, titleModifier) = rememberAdaptiveTextColor(
                                fallback = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                book?.title ?: "Book",
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                modifier = titleModifier,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (selectedFolderPath != "/") {
                                Text("Folder: $selectedFolderPath", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Folder, contentDescription = "Folders")
                        }
                        IconButton(onClick = { vm.toggleViewMode() }) {
                            Icon(
                                if (viewMode == BookViewModel.ViewMode.LIST) Icons.Default.ViewStream else Icons.Default.AccountTree,
                                contentDescription = "Toggle Mode"
                            )
                        }
                        var showSortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = LocalSolidSurface.current,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Book Cover") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = {
                                    showSortMenu = false
                                    coverPickerLauncher.launch("image/*")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Date Updated") },
                                leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.DATE_UPDATED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Created") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.DATE_CREATED)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (A-Z)") },
                                leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                onClick = {
                                    vm.setSortMode(BookViewModel.SortMode.TITLE_AZ)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.frostedBar(hazeState)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = "Write") },
                        label = { Text("Write", fontSize = 10.sp) },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistics") },
                        label = { Text("Statistics", fontSize = 10.sp) },
                        alwaysShowLabel = false
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = fadeIn(tween(150)) + expandVertically(expandFrom = Alignment.Bottom),
                            exit = fadeOut(tween(150)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiniFabItem(
                                    label = "Text Note",
                                    icon = Icons.Default.Description,
                                    onClick = {
                                        isFabExpanded = false
                                        scope.launch { captureForDialog { showCreateNoteDialog = true } }
                                    }
                                )
                                MiniFabItem(
                                    label = "Folder",
                                    icon = Icons.Default.CreateNewFolder,
                                    onClick = {
                                        isFabExpanded = false
                                        scope.launch { captureForDialog { showCreateFolderDialog = true } }
                                    }
                                )
                            }
                        }

                        val rotation by animateFloatAsState(
                            targetValue = if (isFabExpanded) 45f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "fabRotate"
                        )

                        FloatingActionButton(
                            onClick = { isFabExpanded = !isFabExpanded },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = frostedContainerColor(
                                fallback = MaterialTheme.colorScheme.primary
                            ),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier.frostedFab(LocalHazeState.current, shape = RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New Item",
                                modifier = Modifier.rotate(rotation)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isFabExpanded) isFabExpanded = false
                    }
            ) {
                if (selectedTab == 0) {
                    val allFolderPaths = remember(folders) {
                        (listOf("/") + folders.map { it.path }).distinct()
                    }

                    if (viewMode == BookViewModel.ViewMode.LIST) {
                        val pagerState = rememberPagerState(pageCount = { allFolderPaths.size })

                        LaunchedEffect(pagerState.currentPage) {
                            selectedFolderPath = allFolderPaths[pagerState.currentPage]
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                edgePadding = 20.dp,
                                containerColor = Color.Transparent,
                                indicator = { tabPositions ->
                                    if (tabPositions.isNotEmpty()) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                            height = 3.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            ) {
                                allFolderPaths.forEachIndexed { index, path ->
                                    val label = if (path == "/") "Main" else path.removePrefix("/")
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        text = {
                                            Text(
                                                label,
                                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                        }
                                    )
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                val currentPath = allFolderPaths[page]
                                val pageNotes = notes.filter { it.folderPath == currentPath }

                                if (pageNotes.isEmpty()) {
                                    val displayPathName = if (currentPath == "/") "Main" else currentPath
                                    EmptyState(
                                        icon = Icons.Outlined.Description,
                                        title = "No notes in $displayPathName",
                                        subtitle = "Tap + to create a new note"
                                    )
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(pageNotes, key = { note -> "${sortMode}_${note.id}" }) { note ->
                                            NoteListRow(
                                                note = note,
                                                onClick = {
                                                    context.startActivity(
                                                        Intent(context, MainActivity::class.java)
                                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                                            .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                                    )
                                                },
                                                onOpenFloat = {
                                                    context.startActivity(
                                                        Intent(context, MainActivity::class.java)
                                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                                            .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                                            .putExtra("openInFloat", true)
                                                    )
                                                },
                                                onRename = { scope.launch { captureForDialog { noteToRename = note } } },
                                                onDuplicate = { vm.duplicateNote(note.id) },
                                                onDelete = { scope.launch { captureForDialog { noteToDelete = note } } }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        TreeModeView(
                            notes = notes,
                            folders = folders,
                            onNoteClick = { note ->
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                        .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                        .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                )
                            },
                            onOpenFloat = { note ->
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                        .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                        .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                        .putExtra("openInFloat", true)
                                )
                            },
                            onRename = { note -> scope.launch { captureForDialog { noteToRename = note } } },
                            onDuplicate = { note -> vm.duplicateNote(note.id) },
                            onDelete = { note -> scope.launch { captureForDialog { noteToDelete = note } } }
                        )
                    }
                } else {
                    BookStatisticsTab(notes = notes, bookTitle = book?.title ?: "Book")
                }
            }
        }
    }

    CompositionLocalProvider(LocalOneShotBitmap provides dialogOneShotBitmap) {
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        FrostedDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            title = { Text("New Note", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = noteTitle.trim()
                        if (t.isNotEmpty()) {
                            vm.createNote(t, selectedFolderPath) { id ->
                                showCreateNoteDialog = false
                                context.startActivity(
                                    Intent(context, MainActivity::class.java)
                                        .putExtra(MainActivity.EXTRA_NOTE_ID, id)
                                        .putExtra(MainActivity.EXTRA_BOOK_ID, vm.bookId)
                                )
                            }
                        }
                    }
                ) { Text("Create", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        FrostedDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name (e.g. Chapter 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val f = folderName.trim()
                        if (f.isNotEmpty()) {
                            val path = if (selectedFolderPath == "/") "/$f" else "$selectedFolderPath/$f"
                            vm.createFolder(path)
                            showCreateFolderDialog = false
                        }
                    }
                ) { Text("Create", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    noteToRename?.let { note ->
        var renameText by remember { mutableStateOf(note.name) }
        FrostedDialog(
            onDismissRequest = { noteToRename = null },
            title = { Text("Rename Note", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = renameText.trim()
                        if (t.isNotEmpty()) {
                            vm.renameNote(note.id, t)
                        }
                        noteToRename = null
                    }
                ) { Text("Rename", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { noteToRename = null }) { Text("Cancel") }
            }
        )
    }

    noteToDelete?.let { note ->
        FrostedDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${note.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteNote(note.id)
                        noteToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }
    }
    }
}

@Composable
private fun MiniFabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            tonalElevation = 2.dp
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = frostedContainerColor(fallback = MaterialTheme.colorScheme.primary),
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            modifier = Modifier.frostedFab(LocalHazeState.current)
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun TreeModeView(
    notes: List<Note>,
    folders: List<Folder>,
    onNoteClick: (Note) -> Unit,
    onOpenFloat: (Note) -> Unit,
    onRename: (Note) -> Unit,
    onDuplicate: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val expandedFolders = remember { mutableStateMapOf<String, Boolean>() }

    val folderPaths = remember(folders) {
        folders.map { it.path }.filter { it != "/" }.sorted()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val rootNotes = notes.filter { it.folderPath == "/" }
        if (rootNotes.isNotEmpty()) {
            item {
                Text(
                    "ROOT NOTES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            items(rootNotes, key = { "root_${it.id}" }) { note ->
                NoteListRow(
                    note = note,
                    onClick = { onNoteClick(note) },
                    onOpenFloat = { onOpenFloat(note) },
                    onRename = { onRename(note) },
                    onDuplicate = { onDuplicate(note) },
                    onDelete = { onDelete(note) }
                )
            }
        }

        folderPaths.forEach { fPath ->
            val isExpanded = expandedFolders[fPath] ?: true
            item(key = "folder_$fPath") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expandedFolders[fPath] = !isExpanded }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        fPath.removePrefix("/"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isExpanded) {
                val fNotes = notes.filter { it.folderPath == fPath }
                items(fNotes, key = { "fn_${it.id}" }) { note ->
                    NoteListRow(
                        note = note,
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { onNoteClick(note) },
                        onOpenFloat = { onOpenFloat(note) },
                        onRename = { onRename(note) },
                        onDuplicate = { onDuplicate(note) },
                        onDelete = { onDelete(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookStatisticsTab(notes: List<Note>, bookTitle: String) {
    val totalWords = remember(notes) {
        notes.sumOf { n -> MarkdownUtil.countWords(n.content) }
    }
    val scoredNotes = remember(notes) {
        notes.map { n ->
            val count = MarkdownUtil.countWords(n.content)
            n to count
        }.sortedByDescending { it.second }
    }
    val maxWords = (scoredNotes.firstOrNull()?.second ?: 1).coerceAtLeast(1).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Statistics for \"$bookTitle\"", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${notes.size}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Total Files", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalWords", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Total Words", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Text("Word Count Ranking", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (scoredNotes.isEmpty()) {
                    Text("No files in this book", color = MaterialTheme.colorScheme.outline)
                } else {
                    scoredNotes.forEach { (note, count) ->
                        val ratio = (count / maxWords).coerceIn(0.05f, 1.0f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(note.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("$count words", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "Folder: ${note.folderPath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListRow(
    note: Note,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onOpenFloat: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val wordCount = remember(note.content) {
        MarkdownUtil.countWords(note.content)
    }

    val previewText = remember(note.content) {
        val lines = note.content.lineSequence().filter { it.isNotBlank() }.take(3).toList()
        if (lines.isEmpty()) "No text content" else lines.joinToString("\n")
    }

    val createdStr = remember(note.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.createdAt))
    }
    val modifiedStr = remember(note.updatedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
    }

    ElevatedCard(
        onClick = { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "$wordCount words · ${note.folderPath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = LocalSolidSurface.current,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onClick() })
                        DropdownMenuItem(text = { Text("Open in Floating Window") }, onClick = { showMenu = false; onOpenFloat() })
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = previewText,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created $createdStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Modified $modifiedStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
