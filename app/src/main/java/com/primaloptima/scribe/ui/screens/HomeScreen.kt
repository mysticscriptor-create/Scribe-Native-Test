package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.primaloptima.scribe.ui.theme.LocalAppTheme
import android.graphics.Bitmap
import android.os.Build
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.LocalOneShotBitmap
import com.primaloptima.scribe.ui.theme.LocalBarBlurBitmap
import com.primaloptima.scribe.ui.theme.LocalSolidSurface
import com.primaloptima.scribe.ui.theme.frostedBar
import com.primaloptima.scribe.ui.theme.frostedContainerColor
import com.primaloptima.scribe.ui.theme.frostedFab
import com.primaloptima.scribe.ui.theme.frostedPanel
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.FrostedBarContent
import com.primaloptima.scribe.ui.theme.FrostedPanelContent
import com.primaloptima.scribe.ui.theme.parseComposeColor
import com.primaloptima.scribe.ui.theme.adaptiveAccentColor
import com.primaloptima.scribe.ui.theme.localHasBgImage
import androidx.compose.material3.LocalContentColor
import com.primaloptima.scribe.util.BitmapBlur
import androidx.compose.ui.platform.LocalView
import com.primaloptima.scribe.ui.theme.rememberAdaptiveTextColor
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.primaloptima.scribe.*
import com.primaloptima.scribe.R
import com.primaloptima.scribe.data.Book
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.util.CoverUtils
import com.primaloptima.scribe.util.ThemeDataStoreRepo
import com.primaloptima.scribe.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenBook: (Book) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSheets: () -> Unit,
    onOpenThemes: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var rightPanelVisible by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val view = LocalView.current
    val blurRadiusPx = com.primaloptima.scribe.ui.theme.LocalFrostedBlurRadius.current.toInt().coerceIn(1, 25)
    var oneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var captured by remember { mutableStateOf(false) }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(drawerState.currentValue, drawerState.targetValue) {
            if (drawerState.targetValue == DrawerValue.Open && !captured) {
                captured = true
                val raw = BitmapBlur.captureOnly(view)
                oneShotBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    raw?.let { BitmapBlur.blurBitmap(it, radius = blurRadiusPx) }
                }
            } else if (drawerState.currentValue == DrawerValue.Closed &&
                       drawerState.targetValue == DrawerValue.Closed) {
                captured = false
                oneShotBitmap = null
            }
        }
    }
    var rightOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rightCaptured by remember { mutableStateOf(false) }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(rightPanelVisible) {
            if (rightPanelVisible && !rightCaptured) {
                rightCaptured = true
                val raw = BitmapBlur.captureOnly(view)
                rightOneShotBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    raw?.let { BitmapBlur.blurBitmap(it, radius = blurRadiusPx) }
                }
            } else if (!rightPanelVisible) {
                kotlinx.coroutines.delay(250)
                rightCaptured = false
                rightOneShotBitmap = null
            }
        }
    }

    val repo = remember { ThemeDataStoreRepo(context) }

    var selectedNavTab by remember { mutableIntStateOf(0) }
    var isGridMode by remember { mutableStateOf(true) }
    var gridColumns by remember { mutableIntStateOf(2) }

    LaunchedEffect(Unit) {
        repo.gridColumnsFlow.collectLatest { gridColumns = it }
    }

    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    LaunchedEffect(pagerState.currentPage) {
        selectedNavTab = pagerState.currentPage
    }
    LaunchedEffect(selectedNavTab) { fabExpanded = false }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) fabExpanded = false
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allBooks by vm.books.observeAsState(emptyList())
    val allNotes by vm.allNotes.observeAsState(emptyList())
    val allFolders by vm.allFolders.observeAsState(emptyList())

    val bookWordCounts = remember(allNotes, allBooks) {
        allBooks.associate { book ->
            book.id to allNotes.filter { it.bookId == book.id }.sumOf { n ->
                n.content.split("\\s+".toRegex()).count { it.isNotBlank() }
            }
        }
    }
    val bookFileCounts = remember(allNotes, allBooks) {
        allBooks.associate { book ->
            book.id to allNotes.count { it.bookId == book.id }
        }
    }
    val bookFolderCounts = remember(allFolders, allBooks) {
        allBooks.associate { book ->
            book.id to allFolders.count { it.bookId == book.id && it.path != "/" }
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<Book?>(null) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var bookToChangeCover by remember { mutableStateOf<Book?>(null) }

    var dialogOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val anyDialogOpen = showCreateDialog || bookToRename != null ||
            bookToDelete != null || bookToChangeCover != null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(anyDialogOpen) {
            if (!anyDialogOpen) dialogOneShotBitmap = null
        }
    }

    val captureForDialog: suspend (() -> Unit) -> Unit = { openDialog ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val raw = BitmapBlur.captureOnly(view)
            dialogOneShotBitmap = withContext(Dispatchers.IO) {
                raw?.let { BitmapBlur.blurBitmap(it, radius = blurRadiusPx) }
            }
        }
        openDialog()
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val book = bookToChangeCover ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val savedUri = CoverUtils.saveCoverImage(context.applicationContext, book.id, uri)
            vm.updateCover(book.id, savedUri)
        }
        bookToChangeCover = null
    }

    val swipeGestureModifier = Modifier.pointerInput(drawerState, rightPanelVisible) {
        var startX = 0f
        var totalX = 0f
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startX = offset.x
                totalX = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                totalX += dragAmount
                val threshold = 36.dp.toPx()
                if (drawerState.isClosed && startX < size.width * 0.3f && totalX > threshold) {
                    change.consume()
                    scope.launch { drawerState.open() }
                }
                if (!rightPanelVisible && startX > size.width * 0.72f && totalX < -threshold) {
                    change.consume()
                    rightPanelVisible = true
                }
            }
        )
    }

    val hazeState = LocalHazeState.current

    CompositionLocalProvider(LocalOneShotBitmap provides dialogOneShotBitmap) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            CompositionLocalProvider(LocalOneShotBitmap provides oneShotBitmap) {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.78f)
                    .frostedPanel(hazeState)
            ) {
                FrostedPanelContent {
                val drawerTheme = LocalAppTheme.current
                val rawAccentColor = drawerTheme?.let {
                    parseComposeColor(it.colors.accent, MaterialTheme.colorScheme.primary)
                } ?: MaterialTheme.colorScheme.primary
                val accentColor = adaptiveAccentColor(rawAccentColor, LocalSolidSurface.current, localHasBgImage())
                val (adaptiveTextColor, adaptiveTextModifier) = rememberAdaptiveTextColor(
                    fallback = MaterialTheme.colorScheme.onSurface
                )
                val currentStreak by vm.currentStreak.collectAsState()

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .wrapContentWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_scribe_text),
                            contentDescription = null,
                            tint = adaptiveTextColor,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(2048f / 922f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_scribe_s),
                            contentDescription = "Scribe",
                            tint = accentColor,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(2048f / 922f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = 0.20f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🔥 $currentStreak ${if (currentStreak == 1) "Day" else "Days"}",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val innerCardBg = if (hazeState != null) {
                    Color.White.copy(alpha = 0.07f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(innerCardBg)
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        DrawerLabel("NAVIGATION")
                        DrawerNavItem(
                            icon = Icons.Default.Map,
                            label = "World Sheets",
                            accentColor = accentColor,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenSheets()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DrawerLabel("TOOLS")
                        DrawerNavItem(
                            icon = Icons.Default.Palette,
                            label = "Themes",
                            accentColor = accentColor,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenThemes()
                            }
                        )
                        DrawerNavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            accentColor = accentColor,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onOpenSettings()
                            }
                        )
                    }
                }
                }
            }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.then(swipeGestureModifier),
            topBar = {
                CompositionLocalProvider(LocalOneShotBitmap provides LocalBarBlurBitmap.current) {
                FrostedBarContent {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.frostedBar(hazeState),
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search full text, titles...") },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotEmpty()) searchQuery = ""
                                        else isSearching = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            val (titleColor, titleModifier) = rememberAdaptiveTextColor(
                                fallback = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Scribe",
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                modifier = titleModifier,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            val (iconColor, iconModifier) = rememberAdaptiveTextColor(
                                fallback = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = iconColor,
                                modifier = iconModifier
                            )
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { rightPanelVisible = !rightPanelVisible }) {
                                Icon(Icons.Default.Info, contentDescription = "Overview")
                            }
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        if (selectedNavTab == 0 && !isSearching) {
                            if (isGridMode) {
                                IconButton(onClick = {
                                    val nextCols = if (gridColumns == 2) 3 else 2
                                    gridColumns = nextCols
                                    scope.launch { repo.setGridColumns(nextCols) }
                                }) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "${gridColumns}C",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isGridMode = !isGridMode }) {
                                Icon(
                                    if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List View"
                                )
                            }
                            var showSortMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Sort Options")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                containerColor = LocalSolidSurface.current,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Date Updated") },
                                    leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.DATE_UPDATED)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Created") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.DATE_CREATED)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Title (A-Z)") },
                                    leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                    onClick = {
                                        vm.setSortMode(HomeViewModel.SortMode.TITLE_AZ)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
                }
                }
            },
            bottomBar = {
                CompositionLocalProvider(LocalOneShotBitmap provides LocalBarBlurBitmap.current) {
                FrostedBarContent {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .frostedBar(hazeState)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    val activeTheme = LocalAppTheme.current
                    val accentColor = activeTheme?.let { parseComposeColor(it.colors.accent, MaterialTheme.colorScheme.primary) }
                        ?: MaterialTheme.colorScheme.primary
                    val navColors = NavigationBarItemDefaults.colors(
                        indicatorColor = accentColor,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 0 && !isSearching,
                        onClick = {
                            selectedNavTab = 0
                            isSearching = false
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Books") },
                        label = { Text("Books", fontSize = 10.sp) },
                        colors = navColors,
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 1 && !isSearching,
                        onClick = {
                            selectedNavTab = 1
                            isSearching = false
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { Icon(Icons.Default.StickyNote2, contentDescription = "Notes") },
                        label = { Text("Notes", fontSize = 10.sp) },
                        colors = navColors,
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 2 && !isSearching,
                        onClick = {
                            selectedNavTab = 2
                            isSearching = false
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistics") },
                        label = { Text("Stats", fontSize = 10.sp) },
                        colors = navColors,
                        alwaysShowLabel = false
                    )
                }
                }
                }
            },
            floatingActionButton = {
                CompositionLocalProvider(LocalOneShotBitmap provides LocalBarBlurBitmap.current) {
                val fabTheme = LocalAppTheme.current
                val accentClr = fabTheme?.let {
                    parseComposeColor(it.colors.accent, MaterialTheme.colorScheme.primary)
                } ?: MaterialTheme.colorScheme.primary

                AnimatedContent(
                    targetState = selectedNavTab,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) +
                                fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "fabSwitch"
                ) { tab ->
                    when (tab) {
                        0 -> {
                            AnimatedContent(
                                targetState = fabExpanded,
                                transitionSpec = {
                                    if (targetState) {
                                        (scaleIn(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            ),
                                            transformOrigin = TransformOrigin(1f, 1f)
                                        ) + fadeIn()) togetherWith
                                        (scaleOut(
                                            targetScale = 0.75f,
                                            transformOrigin = TransformOrigin(1f, 1f)
                                        ) + fadeOut(tween(100)))
                                    } else {
                                        (scaleIn(
                                            initialScale = 0.75f,
                                            transformOrigin = TransformOrigin(1f, 1f)
                                        ) + fadeIn(tween(100))) togetherWith
                                        (scaleOut(
                                            animationSpec = tween(180),
                                            transformOrigin = TransformOrigin(1f, 1f)
                                        ) + fadeOut(tween(180)))
                                    }
                                },
                                label = "fabMorph"
                            ) { expanded ->
                                if (expanded) {
                                    var showItems by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) { showItems = true }

                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = frostedContainerColor(
                                            fallback = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                                        ),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .width(210.dp)
                                            .frostedFab(LocalHazeState.current, shape = RoundedCornerShape(24.dp))
                                    ) {
                                        Column {
                                            AnimatedVisibility(
                                                visible = showItems,
                                                enter = fadeIn(tween(150)) + slideInVertically(
                                                    initialOffsetY = { 30 },
                                                    animationSpec = tween(200)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            fabExpanded = false
                                                            scope.launch {
                                                                captureForDialog { showCreateDialog = true }
                                                            }
                                                        }
                                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(accentClr.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Add,
                                                            contentDescription = "New Book",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = accentClr
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "New Book",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            AnimatedVisibility(
                                                visible = showItems,
                                                enter = fadeIn(tween(150, delayMillis = 60)) +
                                                        slideInVertically(
                                                            initialOffsetY = { 30 },
                                                            animationSpec = tween(200, delayMillis = 60)
                                                        )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            fabExpanded = false
                                                            context.startActivity(
                                                                Intent(context, SheetsActivity::class.java)
                                                                    .putExtra(SheetsActivity.EXTRA_OPEN_CREATE, true)
                                                            )
                                                        }
                                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(accentClr.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Outlined.Book,
                                                            contentDescription = "New Sheet",
                                                            modifier = Modifier.size(18.dp),
                                                            tint = accentClr
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "New Sheet",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    FloatingActionButton(
                                        onClick = { fabExpanded = true },
                                        containerColor = frostedContainerColor(fallback = accentClr),
                                        contentColor = Color.White,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.frostedFab(LocalHazeState.current, shape = RoundedCornerShape(20.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "New Book")
                                    }
                                }
                            }
                        }
                        1 -> ExtendedFloatingActionButton(
                            onClick = {
                                vm.createQuickNote { note ->
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java)
                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                            .putExtra(MainActivity.EXTRA_BOOK_ID, note.bookId)
                                    )
                                }
                            },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            text = { Text("Quick Note", fontWeight = FontWeight.SemiBold) },
                            containerColor = frostedContainerColor(fallback = accentClr),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.frostedFab(LocalHazeState.current, shape = RoundedCornerShape(20.dp))
                        )
                        else -> Box(Modifier)
                    }
                }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isSearching) {
                    SearchResultsView(
                        query = searchQuery,
                        allBooks = allBooks,
                        allNotes = allNotes,
                        onOpenNote = { note ->
                            context.startActivity(
                                Intent(context, MainActivity::class.java)
                                    .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                    .putExtra(MainActivity.EXTRA_BOOK_ID, note.bookId)
                            )
                        }
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                    ) { page ->
                        when (page) {
                            0 -> BooksTabContent(
                                books = vm.sortedBooks(allBooks),
                                isGridMode = isGridMode,
                                gridColumns = gridColumns,
                                wordCounts = bookWordCounts,
                                fileCounts = bookFileCounts,
                                folderCounts = bookFolderCounts,
                                allNotes = allNotes,
                                onOpen = onOpenBook,
                                onRename = { book -> scope.launch { captureForDialog { bookToRename = book } } },
                                onChangeCover = {
                                    bookToChangeCover = it
                                    coverPickerLauncher.launch("image/*")
                                },
                                onDelete = { book -> scope.launch { captureForDialog { bookToDelete = book } } }
                            )
                            1 -> NotesTabContent(
                                allNotes = allNotes,
                                onOpenNote = { note ->
                                    context.startActivity(
                                        Intent(context, MainActivity::class.java)
                                            .putExtra(MainActivity.EXTRA_NOTE_ID, note.id)
                                            .putExtra(MainActivity.EXTRA_BOOK_ID, note.bookId)
                                    )
                                }
                            )
                            2 -> MainStatisticsTabContent(
                                allBooks = allBooks,
                                allNotes = allNotes,
                                allFolders = allFolders
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = fabExpanded && selectedNavTab == 0,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { fabExpanded = false }
                    )
                }

                AnimatedVisibility(
                    visible = rightPanelVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(180))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { rightPanelVisible = false }
                    )
                }
                AnimatedVisibility(
                    visible = rightPanelVisible,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(200)
                    )
                ) {
                    val totalWords = remember(allNotes) {
                        allNotes.sumOf { n -> n.content.split("\\s+".toRegex()).count { it.isNotBlank() } }
                    }
                    CompositionLocalProvider(LocalOneShotBitmap provides rightOneShotBitmap) {
                    FrostedPanelContent {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.72f)
                            .frostedPanel(hazeState)
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { rightPanelVisible = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        HorizontalDivider()
                        StatPanelRow(Icons.Default.Book, "Books", "${allBooks.size}")
                        StatPanelRow(Icons.Default.StickyNote2, "Notes", "${allNotes.size}")
                        StatPanelRow(Icons.Default.TextFields, "Total words", "$totalWords")
                        StatPanelRow(Icons.Default.FolderOpen, "Folders", "${allFolders.size}")
                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider()
                        Text(
                            "Quick Actions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp
                        )
                        TextButton(
                            onClick = { rightPanelVisible = false; onOpenSettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Settings")
                        }
                        TextButton(
                            onClick = { rightPanelVisible = false; onOpenThemes() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Themes")
                        }
                    }
                    }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        FrostedDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Book", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Book Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            vm.createBook(title) { showCreateDialog = false }
                        }
                    }
                ) { Text("Create", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    bookToRename?.let { book ->
        var renameText by remember { mutableStateOf(book.title) }
        FrostedDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Rename Book", fontWeight = FontWeight.Bold) },
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
                            vm.renameBook(book.id, t)
                        }
                        bookToRename = null
                    }
                ) { Text("Rename", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) { Text("Cancel") }
            }
        )
    }

    bookToDelete?.let { book ->
        FrostedDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete "${book.title}"? All notes in it will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteBook(book.id)
                        bookToDelete = null
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            }
        )
    }
    }
}

@Composable
private fun DrawerLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = LocalContentColor.current.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 4.dp)
    )
}

@Composable
private fun DrawerNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = accentColor
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BooksTabContent(
    books: List<Book>,
    isGridMode: Boolean,
    gridColumns: Int,
    wordCounts: Map<String, Int>,
    fileCounts: Map<String, Int>,
    folderCounts: Map<String, Int>,
    allNotes: List<Note>,
    onOpen: (Book) -> Unit,
    onRename: (Book) -> Unit,
    onChangeCover: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (books.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.MenuBook,
                title = "No books yet",
                subtitle = "Tap + to create your first book"
            )
        } else if (isGridMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(books, key = { it.id }) { book ->
                    BookGridCard(
                        book = book,
                        words = wordCounts[book.id] ?: 0,
                        files = fileCounts[book.id] ?: 0,
                        onOpen = { onOpen(book) },
                        onRename = { onRename(book) },
                        onChangeCover = { onChangeCover(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(books, key = { it.id }) { book ->
                    val firstNote = allNotes.firstOrNull { it.bookId == book.id && it.content.isNotBlank() }
                    val introSnippet = firstNote?.content?.take(100)?.replace("\n", " ") ?: "No book intro"
                    BookListRow(
                        book = book,
                        words = wordCounts[book.id] ?: 0,
                        files = fileCounts[book.id] ?: 0,
                        folders = folderCounts[book.id] ?: 0,
                        introSnippet = introSnippet,
                        onOpen = { onOpen(book) },
                        onRename = { onRename(book) },
                        onChangeCover = { onChangeCover(book) },
                        onDelete = { onDelete(book) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyFloatY"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { translationY = floatY },
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun BookGridCard(
    book: Book,
    words: Int,
    files: Int,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    var isPressed by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }

    val targetRotX = if (isPressed) (pressOffset.y - 100f) * 0.04f else 0f
    val targetRotY = if (isPressed) -(pressOffset.x - 80f) * 0.04f else 0f
    val targetScale = if (isPressed) 0.96f else 1f
    val targetElev = if (isPressed) 2f else 16f

    val rotX by animateFloatAsState(targetRotX, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rotX")
    val rotY by animateFloatAsState(targetRotY, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rotY")
    val scale by animateFloatAsState(targetScale, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val elev by animateFloatAsState(targetElev, tween(200), label = "elev")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressed = true
                        pressOffset = offset
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onOpen() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = floatY
                    scaleX = scale
                    scaleY = scale
                    rotationX = rotX
                    rotationY = rotY
                    cameraDistance = 10f * density
                    shadowElevation = elev.dp.toPx()
                }
                .aspectRatio(0.72f)
                .shadow(elev.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (book.coverUri != null) {
                val context = LocalContext.current
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                startY = 200f
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = if (book.coverUri != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = LocalSolidSurface.current,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename() })
                    DropdownMenuItem(text = { Text("Change Cover") }, onClick = { showMenu = false; onChangeCover() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = book.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "$words words · $files files",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BookListRow(
    book: Book,
    words: Int,
    files: Int,
    folders: Int,
    introSnippet: String,
    onOpen: () -> Unit,
    onRename: (Book) -> Unit,
    onChangeCover: (Book) -> Unit,
    onDelete: (Book) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { onOpen() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shadow(4.dp, RoundedCornerShape(10.dp))
            ) {
                if (book.coverUri != null) {
                    val context = LocalContext.current
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
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    introSnippet,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(Icons.Default.TextFields, "$words", MaterialTheme.colorScheme.primary)
                    StatChip(Icons.Default.Article, "$files", MaterialTheme.colorScheme.onSurfaceVariant)
                    StatChip(Icons.Default.FolderOpen, "$folders", MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                    DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRename(book) })
                    DropdownMenuItem(text = { Text("Change Cover") }, onClick = { showMenu = false; onChangeCover(book) })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NotesTabContent(
    allNotes: List<Note>,
    onOpenNote: (Note) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (allNotes.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.StickyNote2,
                title = "No notes yet",
                subtitle = "Tap + Quick Note to create one instantly"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allNotes, key = { "notes_tab_${it.id}" }) { note ->
                    val wordCount = note.content.split("\\s+".toRegex()).count { it.isNotBlank() }
                    ElevatedCard(
                        onClick = { onOpenNote(note) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "$wordCount words",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.content.ifBlank { "Empty quick note..." },
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    allBooks: List<Book>,
    allNotes: List<Note>,
    onOpenNote: (Note) -> Unit
) {
    if (query.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Type above to search across all notes", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        val matches = remember(query, allNotes) {
            allNotes.filter { n ->
                n.name.contains(query, ignoreCase = true) || n.content.contains(query, ignoreCase = true) || n.folderPath.contains(query, ignoreCase = true)
            }
        }

        if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notes found matching "$query"", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(matches, key = { "sr_${it.id}" }) { note ->
                    val bookTitle = allBooks.firstOrNull { it.id == note.bookId }?.title ?: "Vault"
                    ElevatedCard(
                        onClick = { onOpenNote(note) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = highlightMatch(note.name, query),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "$bookTitle / ${note.folderPath}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = highlightMatch(note.content.take(150).replace("\n", " "), query),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun highlightMatch(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return buildAnnotatedString { append(text) }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            if (index > start) {
                append(text.substring(start, index))
            }
            withStyle(style = SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
}

@Composable
private fun StatPanelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 15.sp)
        }
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
