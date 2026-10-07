package com.primaloptima.scribe.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import com.primaloptima.scribe.ui.theme.ScribeTheme
import com.primaloptima.scribe.ui.theme.LocalBarBlurBitmap
import com.primaloptima.scribe.ui.theme.LocalHazeState
import com.primaloptima.scribe.ui.theme.LocalOneShotBitmap
import com.primaloptima.scribe.ui.theme.LocalSolidSurface
import com.primaloptima.scribe.ui.theme.frostedBar
import com.primaloptima.scribe.ui.components.ScribeSingleFab
import com.primaloptima.scribe.ui.components.ScribeEditorTopBar
import com.primaloptima.scribe.ui.components.ScribeBarAction
import com.primaloptima.scribe.ui.components.ScribeBarIconButton
import com.primaloptima.scribe.ui.components.FrostedBottomSheet
import com.primaloptima.scribe.ui.components.FrostedSheetDragHandle
import com.primaloptima.scribe.ui.components.EditorLeftDrawer
import com.primaloptima.scribe.ui.components.EditorRightPanel
import com.primaloptima.scribe.ui.theme.frostedFab
import com.primaloptima.scribe.ui.theme.frostedPanel
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.ui.theme.FrostedDropdownMenu
import com.primaloptima.scribe.ui.theme.frostedContainerColor
import com.primaloptima.scribe.ui.theme.LocalAppTheme
import dev.chrisbanes.haze.hazeSource
import com.primaloptima.scribe.ui.theme.ScribeColorScheme
import com.primaloptima.scribe.engine.ProseDiagnosticProvider
import com.primaloptima.scribe.engine.ProseInlayHintProvider
import com.primaloptima.scribe.util.BitmapBlur
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.Help
import coil3.compose.AsyncImage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.data.Folder
import com.primaloptima.scribe.data.Note
import com.primaloptima.scribe.data.WorldEntry
import com.primaloptima.scribe.ui.components.DualTitleNoteDialog
import com.primaloptima.scribe.ui.components.FloatingWindowOverlay
import com.primaloptima.scribe.util.ExportHelper
import com.primaloptima.scribe.viewmodel.BookViewModel
import com.primaloptima.scribe.viewmodel.EditorViewModel
import com.primaloptima.scribe.viewmodel.NoteListViewModel
import com.primaloptima.scribe.viewmodel.ShortcutsViewModel
import com.primaloptima.scribe.util.model.ShortcutAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.window.core.layout.WindowWidthSizeClass
import dev.chrisbanes.haze.HazeState
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.util.ScribeDataStore
import com.primaloptima.scribe.ui.ornaments.OrnamentRegistry
import com.primaloptima.scribe.ui.ornaments.OrnamentPickerSheet

// ── Sora Editor imports ───────────────────────────────────────────────────────
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.primaloptima.scribe.ui.components.UnifiedCanvasLayout
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import com.primaloptima.scribe.util.ScribeProseLanguage
import com.primaloptima.scribe.util.ThemeManager


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun MainEditorScreen(
    editorVm: EditorViewModel,
    bookVm: BookViewModel,
    noteListVm: NoteListViewModel,
    shortcutsVm: ShortcutsViewModel,
    initialNoteId: String?,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenShortcuts: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSheets: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // ── Adaptive Window Size Class ────────────────────────────────────────────
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isCompact = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

    // ── Frosted-glass blur bitmaps (pre-API-31 fallback) ─────────────────────
    val view         = LocalView.current
    val blurRadiusPx = com.primaloptima.scribe.ui.theme.LocalFrostedBlurRadius.current
        .toInt().coerceIn(1, 25)
    var dialogOneShotBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val barBlurBitmap = LocalBarBlurBitmap.current

    val editorTheme  = LocalAppTheme.current
    val editorBgUri  = editorTheme?.backgroundImageUri

    // ── ViewModel state ───────────────────────────────────────────────────────
    val activeNote     by editorVm.activeNote.collectAsStateWithLifecycle()
    val wordCount      by editorVm.wordCount.collectAsStateWithLifecycle()
    val charCount      by editorVm.charCount.collectAsStateWithLifecycle()
    val outline        by editorVm.outline.collectAsStateWithLifecycle()
    val proseAnalysis  by editorVm.proseAnalysis.collectAsStateWithLifecycle()
    val zenMode        by editorVm.zenMode.collectAsStateWithLifecycle()
    val activeTheme    by editorVm.theme.collectAsStateWithLifecycle()
    val goalProgress   by editorVm.goalProgress.collectAsStateWithLifecycle()

    val bgUri        = activeTheme?.backgroundImageUri
    val bgMode       = activeTheme?.bgMode ?: "color"
    val themeScope   = activeTheme?.themeScope ?: "editor_only"
    val bgOpacity    = activeTheme?.backgroundImageOpacity ?: 0.35f
    val blurIntensity = activeTheme?.blurIntensity ?: 0f
    val hasBgImage   = !bgUri.isNullOrEmpty() && bgMode != "color"
    val isEditorOnlyBg = hasBgImage && themeScope == "editor_only"

    val currentBookNotes   by bookVm.notes.collectAsStateWithLifecycle()
    val currentBookFolders by bookVm.folders.collectAsStateWithLifecycle()
    val worldEntries       by bookVm.worldEntries.collectAsStateWithLifecycle()
    val allBooks           by bookVm.allBooks.collectAsStateWithLifecycle()

    val allNotes   by noteListVm.notes.collectAsStateWithLifecycle()
    val allFolders by noteListVm.folders.collectAsStateWithLifecycle()
    val shortcuts  by shortcutsVm.shortcuts.collectAsStateWithLifecycle()

    val floatingWindows    by editorVm.floatingWindows.collectAsStateWithLifecycle()
    val workbenchState     by editorVm.workbenchState.collectAsStateWithLifecycle()
    val companionTabBarBottom   by editorVm.companionTabBarBottom.collectAsStateWithLifecycle()
    val companionSplitHorizontal by editorVm.companionSplitHorizontal.collectAsStateWithLifecycle()

    // ── Local UI state ────────────────────────────────────────────────────────
    var rightPanelTab   by remember { mutableIntStateOf(0) }
    var leftDrawerMode  by remember { mutableStateOf("Current") }

    var showFindBar    by remember { mutableStateOf(false) }
    var findQuery      by remember { mutableStateOf("") }
    var replaceQuery   by remember { mutableStateOf("") }

    var showRenameDialog     by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showEditorTray       by remember { mutableStateOf(false) }

    var activeDrawerMode     by remember { mutableStateOf<EditorDrawerMode?>(null) }
    val activeBarShortcuts   = remember(shortcuts) { shortcuts.filter { it.itemType == "shortcut" } }
    val snippets             = remember(shortcuts) { shortcuts.filter { it.itemType == "snippet" } }
    val templates            = remember(shortcuts) { shortcuts.filter { it.itemType == "template" } }

    BackHandler(enabled = activeDrawerMode != null) {
        activeDrawerMode = null
    }

    val dataStore = remember { (context.applicationContext as? ScribeApp)?.dataStore ?: ScribeDataStore(context) }
    val selectedOrnamentId by dataStore.manuscriptOrnamentIdFlow.collectAsStateWithLifecycle("classic_diamond")
    var showOrnamentPicker by remember { mutableStateOf(false) }

    val anyDialogOpen = showRenameDialog || showCreateNoteDialog || showEditorTray
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        LaunchedEffect(anyDialogOpen) { if (!anyDialogOpen) dialogOneShotBitmap = null }
    }

    val captureForDialog: suspend (() -> Unit) -> Unit = { openDialog ->
        openDialog()
    }

    // ── Sora CodeEditor & Unified Canvas state ──────────────────────────────────
    var unifiedCanvasRef   by remember { mutableStateOf<UnifiedCanvasLayout?>(null) }
    var soraEditorRef      by remember { mutableStateOf<CodeEditor?>(null) }
    var isHandleDragging   by remember { mutableStateOf(false) }
    var loadedNoteId       by rememberSaveable { mutableStateOf<String?>(null) }

    // ── Floating Pills Scroll Animation & Dual-Title State ──────────────────────
    var floatingPillsVisible by rememberSaveable { mutableStateOf(true) }
    var scrollDistanceSinceDirectionChange by remember { mutableFloatStateOf(0f) }
    var lastScrollDirection by remember { mutableIntStateOf(0) }

    val hideOnScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val dy = available.y
                if (dy < -2f) {
                    // Scrolling DOWN / reading forward into document (dy is negative, content moves up)
                    if (lastScrollDirection != 1) {
                        lastScrollDirection = 1
                        scrollDistanceSinceDirectionChange = 0f
                    }
                    scrollDistanceSinceDirectionChange += (-dy)
                    // Delay before hiding: allow ~60px of scroll before animating away
                    if (scrollDistanceSinceDirectionChange > 60f && floatingPillsVisible) {
                        floatingPillsVisible = false
                    }
                } else if (dy > 2f) {
                    // Scrolling UP / navigating back to top (dy is positive, content moves down)
                    if (lastScrollDirection != -1) {
                        lastScrollDirection = -1
                        scrollDistanceSinceDirectionChange = 0f
                    }
                    scrollDistanceSinceDirectionChange += dy
                    // Immediate animate in when scrolling back
                    if (!floatingPillsVisible) {
                        floatingPillsVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Dual-title inline editing state & persistence
    var primaryTitleText by remember(activeNote?.id) {
        val raw = activeNote?.name ?: ""
        mutableStateOf(raw.substringBefore('\n'))
    }
    var secondaryTitleText by remember(activeNote?.id) {
        val raw = activeNote?.name ?: ""
        mutableStateOf(if (raw.contains('\n')) raw.substringAfter('\n') else "")
    }
    var showSecondaryTitle by remember(activeNote?.id) {
        val raw = activeNote?.name ?: ""
        mutableStateOf(raw.contains('\n'))
    }

    fun persistDualTitle(primary: String, secondary: String) {
        val targetNote = activeNote ?: return
        val p = primary.trim()
        val s = secondary.trim()
        val combined = if (s.isNotEmpty()) {
            if (p.isNotEmpty()) "$p\n$s" else s
        } else {
            p
        }
        if (combined.isNotEmpty()) {
            bookVm.renameNote(targetNote.id, combined)
        }
    }

    var pillMode     by remember { mutableIntStateOf(0) }
    var pillOffsetX  by remember { mutableFloatStateOf(0f) }
    var pillOffsetY  by remember { mutableFloatStateOf(0f) }

    // FIX 2: prevWordCount now uses rememberSaveable so it survives rotation.
    // Previously a plain remember meant the delta indicator reset to 0 after config change.
    var prevWordCount   by rememberSaveable { mutableIntStateOf(wordCount) }
    var deltaText       by remember { mutableStateOf<String?>(null) }
    var isPositiveDelta by remember { mutableStateOf(true) }
    var goalNotified    by remember { mutableStateOf(false) }

    LaunchedEffect(wordCount) {
        val diff = wordCount - prevWordCount
        if (diff != 0) {
            deltaText       = if (diff > 0) "+$diff" else "$diff"
            isPositiveDelta = diff > 0
            prevWordCount   = wordCount
            delay(800)
            deltaText = null
        }
    }
    LaunchedEffect(goalProgress) {
        if (goalProgress >= 1f && !goalNotified && wordCount > 0) {
            goalNotified = true
            Toast.makeText(context, "Daily writing goal reached!", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(initialNoteId) {
        if (!initialNoteId.isNullOrEmpty()) editorVm.loadNote(initialNoteId)
        else if (currentBookNotes.isNotEmpty()) editorVm.loadNote(currentBookNotes.first().id)
    }

    // FIX 3: Removed activeNote?.content from the LaunchedEffect key.
    // Previously keying on content meant this effect was cancelled and re-launched on
    // every keystroke (content changes → ViewModel emits → new content value → effect restarts).
    // The guard condition `editor.text.length == 0 && note.content.isNotEmpty()` already
    // handles the edge case of an editor that exists but hasn't been filled yet.
    // Keying only on id + soraEditorRef is sufficient and far cheaper.
    LaunchedEffect(activeNote?.id, soraEditorRef) {
        val note   = activeNote ?: return@LaunchedEffect
        val editor = soraEditorRef ?: return@LaunchedEffect
        if (loadedNoteId != note.id || (editor.text.length == 0 && note.content.isNotEmpty())) {
            loadedNoteId = note.id
            unifiedCanvasRef?.resetScroll()
            floatingPillsVisible = true
            editor.setText(note.content)
            ProseDiagnosticProvider.attachEditor(editor)
            val (hints, diagnostics) = withContext(Dispatchers.Default) {
                val h = ProseInlayHintProvider.computeInlayHints(note.content, worldEntries)
                val d = ProseDiagnosticProvider.analyzeDiagnostics(note.content)
                h to d
            }
            editor.setInlayHints(hints)
            editor.setDiagnostics(diagnostics)
        }
    }

    // Debounced analysis for Inlay Hints (Scene word counts & POV tags) and Diagnostics (Passives, Adverbs, Repetitions)
    var editorCurrentText by remember { mutableStateOf("") }
    LaunchedEffect(editorCurrentText, worldEntries) {
        if (editorCurrentText.isEmpty()) return@LaunchedEffect
        val editor = soraEditorRef ?: return@LaunchedEffect
        delay(400) // Debounce 400ms to keep editing fluid
        val (hints, diagnostics) = withContext(Dispatchers.Default) {
            val h = ProseInlayHintProvider.computeInlayHints(editorCurrentText, worldEntries)
            val d = ProseDiagnosticProvider.analyzeDiagnostics(editorCurrentText)
            h to d
        }
        editor.setInlayHints(hints)
        editor.setDiagnostics(diagnostics)
    }

    val soraEditorForDispose = soraEditorRef
    DisposableEffect(activeNote?.id) {
        onDispose {
            activeNote?.let {
                editorVm.saveVersionSnapshotOnLeave(soraEditorForDispose?.text?.toString() ?: "")
            }
        }
    }

    // FIX 4: The launcher result was being discarded (variable not stored, never launched).
    // Now stored so it can be called. If you have a UI entry point for connecting an external
    // folder, call externalFolderLauncher.launch(null) from that button/menu item.
    val externalFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast(':') ?: "External Folder"
        noteListVm.connectExternalFolder(uri, name)
    }

    val isKeyboardVisible = WindowInsets.isImeVisible
    val hazeState = LocalHazeState.current ?: dev.chrisbanes.haze.HazeState()
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Editor-only background image ──────────────────────────────────────
        if (isEditorOnlyBg) {
            AsyncImage(
                model              = bgUri,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .then(
                        if (bgMode == "blurred" &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            blurIntensity > 0f
                        ) Modifier.graphicsLayer {
                            val r = blurIntensity * density.density
                            if (r > 0f) renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        } else Modifier
                    )
            )
            val themeBgColor = parseComposeColor(
                activeTheme?.colors?.background ?: "#FAFAF7", Color(0xFFFAFAF7)
            )
            Box(Modifier.fillMaxSize().background(themeBgColor.copy(alpha = bgOpacity)))
        }

        // ── Reusable Component Renderers ──────────────────────────────────────
        val renderEditorScaffold: @Composable (
            onNavClick: () -> Unit,
            onOpenRightPanel: () -> Unit,
            isLeftDrawerOpen: Boolean
        ) -> Unit = { onNavClick, onOpenRightPanel, isLeftDrawerOpen ->
            Scaffold(
                containerColor      = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    CompositionLocalProvider(LocalOneShotBitmap provides barBlurBitmap) {
                        val registerBounds = LocalInteractiveBoundsRegistry.current
                        DisposableEffect(isKeyboardVisible, activeDrawerMode) {
                            onDispose { registerBounds("shortcut_bar", null) }
                        }

                        AnimatedVisibility(
                            visible = isKeyboardVisible || activeDrawerMode != null,
                            enter   = slideInVertically(initialOffsetY = { it }),
                            exit    = slideOutVertically(targetOffsetY = { it })
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .frostedBar(hazeState)
                                        .onGloballyPositioned { coords ->
                                            if (isKeyboardVisible || activeDrawerMode != null) {
                                                registerBounds("shortcut_bar", coords.boundsInRoot())
                                            }
                                        }
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    // Tmpl Pill
                                    DrawerPillButton(
                                        label = "Tmpl",
                                        icon = Icons.Default.Article,
                                        isActive = activeDrawerMode == EditorDrawerMode.TEMPLATES,
                                        onClick = {
                                            activeDrawerMode = if (activeDrawerMode == EditorDrawerMode.TEMPLATES) null else EditorDrawerMode.TEMPLATES
                                        }
                                    )

                                    // Snip Pill
                                    DrawerPillButton(
                                        label = "Snip",
                                        icon = Icons.Default.ContentPaste,
                                        isActive = activeDrawerMode == EditorDrawerMode.SNIPPETS,
                                        onClick = {
                                            activeDrawerMode = if (activeDrawerMode == EditorDrawerMode.SNIPPETS) null else EditorDrawerMode.SNIPPETS
                                        }
                                    )

                                    VerticalDivider(
                                        modifier = Modifier
                                            .height(18.dp)
                                            .padding(horizontal = 2.dp),
                                        color = ScribeTheme.colors.content.secondary.copy(alpha = 0.25f)
                                    )

                                    activeBarShortcuts.forEach { shortcut ->
                                        FormatButton(label = shortcut.label) {
                                            when (shortcut.kind) {
                                                "wrap" -> soraEditorRef?.applyFormat(shortcut.payload, shortcut.closing ?: shortcut.payload)
                                                "pair" -> soraEditorRef?.applyFormat(shortcut.payload, shortcut.closing ?: "")
                                                else   -> soraEditorRef?.insertAtCursor(shortcut.payload)
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = activeDrawerMode != null,
                                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    val currentMode = activeDrawerMode
                                    if (currentMode != null) {
                                        EditorAccessoryDrawer(
                                            mode = currentMode,
                                            items = if (currentMode == EditorDrawerMode.SNIPPETS) snippets else templates,
                                            onSelectItem = { item ->
                                                when (item.kind) {
                                                    "wrap" -> soraEditorRef?.applyFormat(item.payload, item.closing ?: item.payload)
                                                    "pair" -> soraEditorRef?.applyFormat(item.payload, item.closing ?: "")
                                                    else   -> soraEditorRef?.insertAtCursor(item.payload)
                                                }
                                                soraEditorRef?.requestFocus()
                                            },
                                            onOpenSettings = {
                                                activeDrawerMode = null
                                                onOpenShortcuts()
                                            },
                                            onToggleKeyboard = {
                                                activeDrawerMode = null
                                                soraEditorRef?.requestFocus()
                                            },
                                            hazeState = hazeState
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (isEditorOnlyBg && !bgUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = bgUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (hazeState != null) Modifier.hazeSource(state = hazeState) else Modifier)
                        )
                        val isOverlayActive = (activeTheme?.overlayEnabled == true || (activeTheme?.overlayColor != null && bgOpacity > 0f)) && bgOpacity > 0f
                        if (isOverlayActive) {
                            val tintBaseColor = activeTheme?.overlayColor?.let {
                                runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                            } ?: ScribeTheme.colors.surfaces.background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(tintBaseColor.copy(alpha = bgOpacity))
                            )
                        }
                    }

                    // Drive Sora's searcher from find state
                    LaunchedEffect(findQuery, showFindBar) {
                        val editor = soraEditorRef ?: return@LaunchedEffect
                        if (showFindBar && findQuery.isNotEmpty()) {
                            editor.searcher.search(findQuery, EditorSearcher.SearchOptions(true, false))
                        } else {
                            editor.searcher.stopSearch()
                        }
                    }

                    // ── Sora CodeEditor & Background Theme Setup ───────────────────────
                    val hasBgImageLocal     = !activeTheme?.backgroundImageUri.isNullOrEmpty()
                    val currentThemeBg      = MaterialTheme.colorScheme.background
                    val editorTextSizeSp    = remember(activeTheme?.fontSize) {
                        (activeTheme?.fontSize ?: 18).toFloat()
                    }
                    val editorTypeface      = remember(activeTheme?.fontFamily) {
                        activeTheme?.fontFamily?.let { ThemeManager.resolveTypeface(context, it) }
                    }
                    val bgArgb              = remember(hasBgImageLocal, currentThemeBg) {
                        if (hasBgImageLocal) android.graphics.Color.TRANSPARENT
                        else currentThemeBg.toArgb()
                    }
                    val popupBgDrawable = remember(activeTheme?.colors?.accent, activeTheme?.colors?.surface) {
                        val density    = context.resources.displayMetrics.density
                        val cornerPx   = 24f * density
                        val accentHex  = activeTheme?.colors?.accent ?: "#000000"
                        val surfaceHex = activeTheme?.colors?.surface ?: "#FFFFFF"
                        val accentArgb  = runCatching { android.graphics.Color.parseColor(accentHex) }.getOrDefault(android.graphics.Color.BLACK)
                        val surfaceArgb = runCatching { android.graphics.Color.parseColor(surfaceHex) }.getOrDefault(android.graphics.Color.WHITE)
                        val fill = android.graphics.drawable.GradientDrawable().apply {
                            setColor(surfaceArgb); cornerRadius = cornerPx
                        }
                        val overlay = android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(
                                android.graphics.Color.argb(
                                    71,
                                    android.graphics.Color.red(accentArgb),
                                    android.graphics.Color.green(accentArgb),
                                    android.graphics.Color.blue(accentArgb)
                                ),
                                android.graphics.Color.TRANSPARENT
                            )
                        ).apply { cornerRadius = cornerPx }
                        android.graphics.drawable.LayerDrawable(arrayOf(fill, overlay))
                    }

                    // ── Unified Continuous Document Canvas ─────────────────────────
                    val docTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val docBottomNavInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val docBottomPadding = if (isKeyboardVisible) 0.dp else docBottomNavInset

                    AndroidView(
                        factory = { ctx ->
                            UnifiedCanvasLayout(ctx).apply {
                                onScrollDelta = { dy ->
                                    if (dy > 2f) {
                                        if (lastScrollDirection != 1) {
                                            lastScrollDirection = 1
                                            scrollDistanceSinceDirectionChange = 0f
                                        }
                                        scrollDistanceSinceDirectionChange += dy
                                        if (scrollDistanceSinceDirectionChange > 60f && floatingPillsVisible) {
                                            floatingPillsVisible = false
                                        }
                                    } else if (dy < -2f) {
                                        if (lastScrollDirection != -1) {
                                            lastScrollDirection = -1
                                            scrollDistanceSinceDirectionChange = 0f
                                        }
                                        scrollDistanceSinceDirectionChange += (-dy)
                                        if (!floatingPillsVisible) {
                                            floatingPillsVisible = true
                                        }
                                    }
                                }
                                onUnifiedScrollChanged = { scrollD, _ ->
                                    if (scrollD <= 5 && editor.offsetY <= 10) {
                                        floatingPillsVisible = true
                                    }
                                }
                                headerView.setViewCompositionStrategy(
                                    ViewCompositionStrategy.DisposeOnDetachedFromWindow
                                )
                                editor.apply {
                                    isLineNumberEnabled    = false
                                    isHighlightCurrentLine = false
                                    isWordwrap             = true
                                    registerInlayHintRenderer(
                                        io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer()
                                    )
                                    setEditorLanguage(ScribeProseLanguage())
                                    isNestedScrollingEnabled = true
                                    try {
                                        getComponent(
                                            io.github.rosemoe.sora.widget.component.EditorTextActionWindow::class.java
                                        ).isEnabled = true
                                    } catch (_: Exception) { }

                                    try {
                                        getComponent(
                                            io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow::class.java
                                        ).isEnabled = true
                                    } catch (_: Exception) { }

                                    subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                                        val current = text.toString()
                                        editorCurrentText = current
                                        if (loadedNoteId != null)
                                            editorVm.onContentChanged(current)
                                        unifiedCanvasRef?.ensureCursorVisibleAboveKeyboard()
                                    }
                                    setOnFocusChangeListener { _, hasFocus ->
                                        if (hasFocus) {
                                            unifiedCanvasRef?.ensureCursorVisibleAboveKeyboard()
                                        }
                                    }
                                    try {
                                        setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                                            val dy = scrollY - oldScrollY
                                            if (scrollY <= 10 && scrollD == 0) {
                                                floatingPillsVisible = true
                                            } else if (dy > 20 && floatingPillsVisible) {
                                                floatingPillsVisible = false
                                            } else if (dy < -10 && !floatingPillsVisible) {
                                                floatingPillsVisible = true
                                            }
                                        }
                                    } catch (_: Throwable) { }
                                    try {
                                        subscribeEvent(io.github.rosemoe.sora.event.HandleStateChangeEvent::class.java) { event, _ ->
                                            isHandleDragging = event.isHeld
                                        }
                                    } catch (_: Throwable) { }
                                    subscribeEvent(EditorKeyEvent::class.java) { event, _ ->
                                        if (event.action != android.view.KeyEvent.ACTION_DOWN) return@subscribeEvent
                                        if (event.keyCode != android.view.KeyEvent.KEYCODE_ENTER) return@subscribeEvent
                                        val cur = this.cursor
                                        if (cur.isSelected) return@subscribeEvent
                                        val line = this.text.getLine(cur.leftLine)
                                        val col  = cur.leftColumn
                                        val closeChars = setOf(')', ']', '}', '`', '"', '\'', '\u201D', '\u2019', '\u00BB')
                                        if (col < line.length && line[col] in closeChars) {
                                            setSelection(cur.leftLine, col + 1)
                                            event.intercept()
                                        }
                                    }
                                }
                            }.also {
                                unifiedCanvasRef = it
                                soraEditorRef = it.editor
                            }
                        },
                        update = { layout ->
                            val editor = layout.editor
                            editor.setTextSize(editorTextSizeSp)
                            editorTypeface?.let { editor.typefaceText = it }
                            editor.setBackgroundColor(bgArgb)
                            activeTheme?.let { theme ->
                                val scheme = ScribeColorScheme(theme)
                                scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND,       bgArgb)
                                scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, bgArgb)
                                scheme.setColor(EditorColorScheme.LINE_NUMBER,            bgArgb)
                                editor.colorScheme = scheme
                                try {
                                    val aw = editor.getComponent(
                                        io.github.rosemoe.sora.widget.component.EditorTextActionWindow::class.java
                                    )
                                    var popup: android.widget.PopupWindow? = null
                                    var cls: Class<*>? = aw.javaClass
                                    outer@ while (cls != null && cls != Any::class.java) {
                                        for (f in cls.declaredFields) {
                                            if (android.widget.PopupWindow::class.java.isAssignableFrom(f.type)) {
                                                f.isAccessible = true
                                                popup = f.get(aw) as? android.widget.PopupWindow
                                                break@outer
                                            }
                                        }
                                        cls = cls.superclass
                                    }
                                    popup?.setBackgroundDrawable(popupBgDrawable)
                                } catch (_: Exception) { }
                            }

                            // Update Header inside ComposeView
                            layout.headerView.setContent {
                                if (!zenMode && activeNote != null) {
                                    ManuscriptHeader(
                                        primaryTitleText = primaryTitleText,
                                        secondaryTitleText = secondaryTitleText,
                                        selectedOrnamentId = selectedOrnamentId,
                                        showSecondaryTitle = showSecondaryTitle,
                                        onPrimaryTitleChange = { sanitized ->
                                            primaryTitleText = sanitized
                                            persistDualTitle(sanitized, secondaryTitleText)
                                        },
                                        onSecondaryTitleChange = { sanitized ->
                                            secondaryTitleText = sanitized
                                            persistDualTitle(primaryTitleText, sanitized)
                                        },
                                        onOrnamentClick = { showOrnamentPicker = true },
                                        onMoveToSecondaryTitle = {
                                            showSecondaryTitle = true
                                        },
                                        onDone = {
                                            soraEditorRef?.let { ed ->
                                                ed.setSelection(0, 0)
                                                unifiedCanvasRef?.resetScroll()
                                                ed.requestFocus()
                                            }
                                        },
                                        onBackspaceEmptySecondary = {
                                            showSecondaryTitle = false
                                            persistDualTitle(primaryTitleText, "")
                                        },
                                        onEnterInSecondary = {
                                            soraEditorRef?.let { ed ->
                                                ed.setSelection(0, 0)
                                                unifiedCanvasRef?.resetScroll()
                                                ed.requestFocus()
                                            }
                                        },
                                        isEditable = true
                                    )
                                }
                            }
                        },
                        onRelease = { layout ->
                            soraEditorRef = null
                            unifiedCanvasRef = null
                            ProseDiagnosticProvider.attachEditor(null)
                            layout.editor.release()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = docTopInset, bottom = docBottomPadding)
                    )

                    // ── Zen Mode Exit FAB ──────────────────────────────────────────
                    if (zenMode) {
                        CompositionLocalProvider(LocalOneShotBitmap provides barBlurBitmap) {
                            ScribeSingleFab(
                                icon               = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Zen",
                                modifier           = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                onClick            = { editorVm.setZen(false) }
                            )
                        }
                    }

                    // ── Find/Replace bar (Fixed overlay at top) ────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                    ) {
                        FindReplaceBar(
                            visible       = showFindBar,
                            findQuery     = findQuery,
                            replaceQuery  = replaceQuery,
                            onFindChange  = { findQuery = it },
                            onReplaceChange = { replaceQuery = it },
                            onPrevious    = { soraEditorRef?.searcher?.gotoPrevious() },
                            onNext        = { soraEditorRef?.searcher?.gotoNext() },
                            onReplaceAll  = {
                                val editor = soraEditorRef ?: return@FindReplaceBar
                                if (findQuery.isNotEmpty()) {
                                    editor.searcher.replaceAll(replaceQuery)
                                    editorVm.onContentChanged(editor.text.toString())
                                }
                            },
                            onClose       = { showFindBar = false }
                        )
                    }

                    // ── Floating Pills Layer (Top-Left and Top-Right) ─────────
                    if (!zenMode) {
                        AnimatedVisibility(
                            visible = floatingPillsVisible,
                            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    slideInVertically(
                                        initialOffsetY = { -it },
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                                    ),
                            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                   slideOutVertically(
                                       targetOffsetY = { -it },
                                       animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                   ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Top-Left: Search Floating Pill
                                FloatingPillButton(
                                    icon = Icons.Default.Search,
                                    contentDescription = "Find & Replace",
                                    hazeState = hazeState,
                                    onClick = { showFindBar = !showFindBar }
                                )

                                // Top-Right: Actions Group (Save Checkpoint, Menu)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Save Checkpoint Floating Pill
                                    FloatingPillButton(
                                        icon = Icons.Default.BookmarkAdd,
                                        contentDescription = "Save Checkpoint",
                                        hazeState = hazeState,
                                        onClick = {
                                            editorVm.saveManualSnapshot(soraEditorRef?.text?.toString() ?: "")
                                            Toast.makeText(context, "Checkpoint saved", Toast.LENGTH_SHORT).show()
                                        }
                                    )

                                    // Options Menu Floating Pill
                                    FloatingPillButton(
                                        icon = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        hazeState = hazeState,
                                        onClick = { showEditorTray = true }
                                    )
                                }
                            }
                        }

                        // ── Draggable Floating Word Counter Pill (Always Visible) ──
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 54.dp, end = 16.dp)
                        ) {
                            CompositionLocalProvider(LocalOneShotBitmap provides barBlurBitmap) {
                                WordCountPill(
                                    modifier        = Modifier,
                                    pillOffsetX     = pillOffsetX,
                                    pillOffsetY     = pillOffsetY,
                                    onOffsetChange  = { dx, dy ->
                                        pillOffsetX += dx
                                        pillOffsetY += dy
                                    },
                                    pillMode        = pillMode,
                                    onModeClick     = { pillMode = (pillMode + 1) % 3 },
                                    wordCount       = wordCount,
                                    charCount       = charCount,
                                    deltaText       = deltaText,
                                    isPositiveDelta = isPositiveDelta,
                                    hazeState       = hazeState,
                                )
                            }
                        }
                    }
                }
            } // end Scaffold
        }

        val renderLeftDrawer: @Composable (onClose: () -> Unit) -> Unit = { onClose ->
            EditorLeftDrawer(
                leftDrawerMode   = leftDrawerMode,
                onModeChange     = { leftDrawerMode = it },
                currentBookNotes = currentBookNotes,
                allNotes         = allNotes,
                activeNoteId     = activeNote?.id,
                onNoteClick      = { id ->
                    editorVm.loadNote(id)
                    onClose()
                },
                onAddNote        = { scope.launch { captureForDialog { showCreateNoteDialog = true } } },
                hazeState        = hazeState,
                barBlurBitmap    = barBlurBitmap,
            )
        }

        val renderRightPanel: @Composable (onClose: () -> Unit) -> Unit = { onClose ->
            EditorRightPanel(
                rightPanelTab         = rightPanelTab,
                onTabChange           = { rightPanelTab = it },
                workbenchState        = workbenchState,
                allNotes              = allNotes,
                worldEntries          = worldEntries,
                books                 = allBooks,
                outline               = outline,
                activeTheme           = activeTheme,
                activeNote            = activeNote,
                proseAnalysis         = proseAnalysis,
                soraEditorRef         = soraEditorRef,
                tabBarAtBottom        = companionTabBarBottom,
                onToggleTabBarPos     = { editorVm.setCompanionTabBarBottom(!companionTabBarBottom) },
                onUpdatePane          = { id, transform -> editorVm.updatePane(id, transform) },
                onUpdateWorkbench     = { transform -> editorVm.updateWorkbench(transform) },
                onAddPane             = { scope -> editorVm.addPane(scope) },
                onRemovePane          = { id -> editorVm.removePane(id) },
                onDuplicatePane       = { id -> editorVm.duplicatePane(id) },
                onMinimizePane        = { id, by -> editorVm.minimizePane(id, by) },
                onRestorePane         = { id -> editorVm.restorePane(id) },
                onPinNote             = { paneId, noteId -> editorVm.pinNoteToPane(paneId, noteId) },
                onUnpinNote           = { paneId, noteId -> editorVm.unpinNote(paneId, noteId) },
                onReorderNote         = { paneId, from, to -> editorVm.reorderPinnedNote(paneId, from, to) },
                onCreateNote          = { paneId, title, content -> editorVm.createNoteForPane(paneId, title, content, activeNote?.bookId ?: Note.DEFAULT_BOOK_ID) },
                onSaveNoteContent     = { noteId, content -> editorVm.updateNoteContent(noteId, content) },
                onLoadNote            = { id -> editorVm.loadNote(id) },
                onClose               = onClose,
                barBlurBitmap         = barBlurBitmap,
                hazeState             = hazeState,
            )
        }

        // ── Adaptive Layout Branching ─────────────────────────────────────────
        if (isCompact) {
            CompactEditorLayout(
                hazeState          = hazeState,
                barBlurBitmap      = barBlurBitmap,
                isKeyboardVisible  = isKeyboardVisible,
                soraEditorRef      = soraEditorRef,
                isHandleDragging   = isHandleDragging,
                focusManager       = focusManager,
                editorContent      = renderEditorScaffold,
                leftDrawerContent  = renderLeftDrawer,
                rightPanelContent  = renderRightPanel,
            )
        } else {
            ExpandedEditorLayout(
                hazeState          = hazeState,
                barBlurBitmap      = barBlurBitmap,
                soraEditorRef      = soraEditorRef,
                editorContent      = renderEditorScaffold,
                leftDrawerContent  = renderLeftDrawer,
                rightPanelContent  = renderRightPanel,
            )
        }

        // ── Floating Windows Overlay ──────────────────────────────────────────
        val mappedNotes = remember(currentBookNotes, worldEntries) {
            buildList {
                addAll(currentBookNotes)
                worldEntries.forEach { w ->
                    if (none { it.id == w.id }) add(
                        Note(id = w.id, name = w.name,
                             content = "${w.type.uppercase()}: ${w.summary}\n\n${w.fieldsJson}")
                    )
                }
            }
        }
        FloatingWindowOverlay(
            floatingWindows  = floatingWindows,
            notes            = mappedNotes,
            activeTheme      = activeTheme,
            onCloseWindow    = { id -> editorVm.closeFloatingWindow(id) },
            onToggleCollapse = { id -> editorVm.toggleCollapseFloatingWindow(id) },
            onMoveWindow     = { id, x, y -> editorVm.moveFloatingWindow(id, x, y) }
        )

        // ── Dialogs & Bottom Sheets ───────────────────────────────────────────
        if (showEditorTray) {
            EditorOptionsBottomSheet(
                noteTitle        = activeNote?.name ?: "Untitled Note",
                onDismiss        = { showEditorTray = false },
                onEnterZen       = {
                    showEditorTray = false
                    editorVm.setZen(true)
                },
                onOpenFloating   = {
                    showEditorTray = false
                    activeNote?.let { editorVm.openFloatingWindow(it.id) }
                },
                onExport         = { fmt ->
                    showEditorTray = false
                    activeNote?.let { ExportHelper.shareNote(context, it, fmt) }
                },
                onVersionHistory = {
                    showEditorTray = false
                    editorVm.flushContent(soraEditorRef?.text?.toString() ?: "")
                    onOpenHistory()
                },
                onShortcuts      = {
                    showEditorTray = false
                    onOpenShortcuts()
                },
                onGuide          = {
                    showEditorTray = false
                    onOpenGuide()
                },
                onSettings       = {
                    showEditorTray = false
                    onOpenSettings()
                },
            )
        }

        CompositionLocalProvider(LocalOneShotBitmap provides dialogOneShotBitmap) {
            if (showRenameDialog && activeNote != null) {
                val noteToRename = activeNote
                val p = noteToRename?.name?.substringBefore('\n') ?: ""
                val s = if (noteToRename?.name?.contains('\n') == true) noteToRename.name.substringAfter('\n') else ""
                DualTitleNoteDialog(
                    dialogTitle = "Rename Note",
                    confirmButtonText = "Rename",
                    initialPrimary = p,
                    initialSecondary = s,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { updatedName ->
                        if (noteToRename != null) bookVm.renameNote(noteToRename.id, updatedName)
                        showRenameDialog = false
                    }
                )
            }

            if (showCreateNoteDialog) {
                DualTitleNoteDialog(
                    dialogTitle = "New Note",
                    confirmButtonText = "Create",
                    onDismiss = { showCreateNoteDialog = false },
                    onConfirm = { fullName ->
                        bookVm.createNote(fullName) { id ->
                            showCreateNoteDialog = false
                            editorVm.loadNote(id)
                        }
                    }
                )
            }

            if (showOrnamentPicker) {
                OrnamentPickerSheet(
                    selectedId = selectedOrnamentId,
                    onSelect = { newId ->
                        scope.launch {
                            dataStore.setManuscriptOrnamentId(newId)
                        }
                    },
                    onDismiss = { showOrnamentPicker = false }
                )
            }
        }
    } // end outer Box
}

// ── Floating Action Pills (Editor Top Bar Alternative) ───────────────────────

@Composable
private fun FloatingPillButton(
    icon: ImageVector,
    contentDescription: String,
    hazeState: dev.chrisbanes.haze.HazeState?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = frostedContainerColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        tonalElevation = 0.dp,
        shadowElevation = ScribeTheme.metrics.elevationLow,
        modifier = modifier
            .clip(CircleShape)
            .frostedFab(hazeState)
            .size(ScribeTheme.metrics.touchTargetCompact)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ScribeTheme.metrics.iconNormal)
            )
        }
    }
}

// ── Custom Frosted Bottom Tray for Editor ───────────────────────────────────────
@Composable
private fun EditorOptionsBottomSheet(
    noteTitle        : String,
    onDismiss        : () -> Unit,
    onEnterZen       : () -> Unit,
    onOpenFloating   : () -> Unit,
    onExport         : (String) -> Unit,
    onVersionHistory : () -> Unit,
    onShortcuts      : () -> Unit,
    onGuide          : () -> Unit,
    onSettings       : () -> Unit,
) {
    FrostedBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = noteTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Document & Editor Actions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Mode Actions (Zen Mode & Floating Reference)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorTrayActionCard(
                    title = "Zen Mode",
                    subtitle = "Focus distraction-free",
                    icon = Icons.Default.Fullscreen,
                    modifier = Modifier.weight(1f),
                    onClick = onEnterZen
                )
                EditorTrayActionCard(
                    title = "Floating Window",
                    subtitle = "Pin as quick reference",
                    icon = Icons.Default.PictureInPicture,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFloating
                )
            }

            // Export Options Section
            Text(
                text = "EXPORT NOTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScribeTheme.colors.content.secondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "TXT" to "txt",
                    "Markdown" to "md",
                    "HTML" to "html",
                    "PDF" to "pdf"
                ).forEach { (label, format) ->
                    Surface(
                        onClick = { onExport(format) },
                        shape = ScribeTheme.shapes.button,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (format) {
                                    "txt" -> Icons.Default.Description
                                    "md" -> Icons.Default.Code
                                    "html" -> Icons.Default.Language
                                    else -> Icons.Default.PictureAsPdf
                                },
                                contentDescription = null,
                                tint = ScribeTheme.colors.interaction.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Navigation & Preferences
            EditorTrayMenuItem(
                title = "Version History",
                subtitle = "Browse snapshots and restore edits",
                icon = Icons.Default.History,
                onClick = onVersionHistory
            )
            EditorTrayMenuItem(
                title = "Keyboard Shortcuts",
                subtitle = "Formatting keys and navigation helpers",
                icon = Icons.Default.Keyboard,
                onClick = onShortcuts
            )
            EditorTrayMenuItem(
                title = "User Guide",
                subtitle = "Quick manual and formatting tips",
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = onGuide
            )
            EditorTrayMenuItem(
                title = "Settings & Appearance",
                subtitle = "Themes, fonts, and editor preferences",
                icon = Icons.Default.Settings,
                onClick = onSettings
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EditorTrayActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ScribeTheme.shapes.cardSmall,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ScribeTheme.colors.interaction.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ScribeTheme.colors.interaction.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EditorTrayMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ScribeTheme.shapes.button,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ScribeTheme.colors.interaction.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Extracted: Find/Replace bar ───────────────────────────────────────────────
// FIX 9 (decomposition): Pulled out so the Column inside the Scaffold content
// doesn't inline 40+ lines of find/replace UI.
@Composable
private fun FindReplaceBar(
    visible         : Boolean,
    findQuery       : String,
    replaceQuery    : String,
    onFindChange    : (String) -> Unit,
    onReplaceChange : (String) -> Unit,
    onPrevious      : () -> Unit,
    onNext          : () -> Unit,
    onReplaceAll    : () -> Unit,
    onClose         : () -> Unit,
) {
    if (!visible) return
    Surface(shadowElevation = ScribeTheme.metrics.elevationMedium, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(ScribeTheme.spacing.compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = findQuery,
                onValueChange = onFindChange,
                placeholder   = { Text("Find") },
                singleLine    = true,
                modifier      = Modifier.weight(1f).height(ScribeTheme.metrics.fieldHeightCompact)
            )
            Spacer(Modifier.width(ScribeTheme.spacing.compact))
            OutlinedTextField(
                value         = replaceQuery,
                onValueChange = onReplaceChange,
                placeholder   = { Text("Replace") },
                singleLine    = true,
                modifier      = Modifier.weight(1f).height(ScribeTheme.metrics.fieldHeightCompact)
            )
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
            IconButton(onClick = onReplaceAll) {
                Icon(Icons.Default.FindReplace, contentDescription = "Replace All")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

// ── Extracted: Word-count pill ────────────────────────────────────────────────
@Composable
fun WordCountPill(
    modifier        : Modifier = Modifier,
    pillOffsetX     : Float = 0f,
    pillOffsetY     : Float = 0f,
    onOffsetChange  : (Float, Float) -> Unit = { _, _ -> },
    pillMode        : Int = 0,
    onModeClick     : () -> Unit = {},
    wordCount       : Int = 1420,
    charCount       : Int = 7850,
    deltaText       : String? = null,
    isPositiveDelta : Boolean = true,
    hazeState       : dev.chrisbanes.haze.HazeState? = LocalHazeState.current,
) {
    val registerBounds = LocalInteractiveBoundsRegistry.current
    DisposableEffect(Unit) {
        onDispose { registerBounds("word_count_pill", null) }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(pillOffsetX.roundToInt(), pillOffsetY.roundToInt()) }
            .onGloballyPositioned { coords ->
                registerBounds("word_count_pill", coords.boundsInRoot())
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(
                visible = deltaText != null,
                enter   = fadeIn() + slideInVertically { -10 },
                exit    = fadeOut() + slideOutVertically { -10 }
            ) {
                Text(
                    text       = deltaText ?: "",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isPositiveDelta) ScribeTheme.colors.analytics.positive else ScribeTheme.colors.analytics.negative,
                    modifier   = Modifier.padding(bottom = 2.dp)
                )
            }
            Surface(
                shape           = CircleShape,
                color           = frostedContainerColor(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                tonalElevation  = 0.dp,
                shadowElevation = ScribeTheme.metrics.elevationLow,
                modifier        = Modifier
                    .clip(CircleShape)
                    .frostedFab(hazeState)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onOffsetChange(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clickable { onModeClick() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(ScribeTheme.metrics.chipHeightCompact)
                        .padding(horizontal = 10.dp, vertical = ScribeTheme.spacing.hairline)
                ) {
                    AnimatedContent(
                        targetState    = pillMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label          = "word_count_transition"
                    ) { mode ->
                        Text(
                            text = when (mode) {
                                1    -> "$wordCount w · $charCount c"
                                2    -> "$wordCount w · ${maxOf(1, wordCount / 200)}m"
                                else -> "$wordCount words"
                            },
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines   = 1
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FormatButton(
    label      : String,
    isSelected : Boolean = false,
    onClick    : () -> Unit = {}
) {
    Surface(
        onClick      = onClick,
        shape        = CircleShape,
        color        = if (isSelected) ScribeTheme.colors.interaction.primary
                       else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier     = Modifier.height(ScribeTheme.metrics.chipHeight)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.padding(horizontal = ScribeTheme.spacing.medium, vertical = ScribeTheme.spacing.micro)
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Editor Accessory Drawer & Shortcuts System ───────────────────────────────────

enum class EditorDrawerMode {
    SNIPPETS,
    TEMPLATES
}

@Composable
fun DrawerPillButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) ScribeTheme.colors.interaction.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (isActive) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.height(ScribeTheme.metrics.chipHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditorAccessoryDrawer(
    mode: EditorDrawerMode,
    items: List<ShortcutAction>,
    onSelectItem: (ShortcutAction) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleKeyboard: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Scroll tracking: chip shrinks / fades out when scrolling down; smoothly reappears when scrolling back up
    var isScrollingUp by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset

        if (currentIndex == 0 && currentOffset <= 5) {
            isScrollingUp = true
        } else if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset + 8)) {
            isScrollingUp = false
        } else if (currentIndex < previousIndex || (currentIndex == previousIndex && currentOffset < previousScrollOffset - 8)) {
            isScrollingUp = true
        }
        previousIndex = currentIndex
        previousScrollOffset = currentOffset
    }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.keywords.any { k -> k.contains(searchQuery, ignoreCase = true) } ||
            it.payload.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .frostedBar(hazeState)
    ) {
        // Content Layer - takes maximum space, scrolls behind top-right floating chip
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching ${if (mode == EditorDrawerMode.SNIPPETS) "snippets" else "templates"} found"
                           else "No ${if (mode == EditorDrawerMode.SNIPPETS) "snippets" else "templates"} available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScribeTheme.colors.content.secondary
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (isSearchExpanded) 52.dp else 8.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    if (mode == EditorDrawerMode.SNIPPETS) {
                        DrawerSnippetCard(
                            snippet = item,
                            onClick = { onSelectItem(item) }
                        )
                    } else {
                        DrawerTemplateCard(
                            template = item,
                            onClick = { onSelectItem(item) }
                        )
                    }
                }
            }
        }

        // Floating Top-Right 3-Icon Chip (when not searching)
        AnimatedVisibility(
            visible = !isSearchExpanded && isScrollingUp,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.8f, animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.8f, animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.94f),
                border = BorderStroke(0.75.dp, ScribeTheme.colors.content.secondary.copy(alpha = 0.2f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { isSearchExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ScribeTheme.colors.content.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(ScribeTheme.colors.content.secondary.copy(alpha = 0.25f))
                    )

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Shortcuts Settings",
                            tint = ScribeTheme.colors.content.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(ScribeTheme.colors.content.secondary.copy(alpha = 0.25f))
                    )

                    IconButton(
                        onClick = onToggleKeyboard,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keyboard",
                            tint = ScribeTheme.colors.content.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // Morphed Search Bar (when user clicks Search icon)
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(150)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, top = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, ScribeTheme.colors.interaction.primary.copy(alpha = 0.35f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ScribeTheme.colors.interaction.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = ScribeTheme.colors.content.primary
                        ),
                        cursorBrush = SolidColor(ScribeTheme.colors.interaction.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (mode == EditorDrawerMode.SNIPPETS) "Search snippets..." else "Search templates...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ScribeTheme.colors.content.secondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ScribeTheme.colors.content.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = ScribeTheme.colors.content.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerSnippetCard(
    snippet: ShortcutAction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, ScribeTheme.colors.content.secondary.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ScribeTheme.colors.interaction.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = ScribeTheme.colors.interaction.primary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snippet.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ScribeTheme.colors.content.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (snippet.description.isNotBlank()) {
                    Text(
                        text = snippet.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ScribeTheme.colors.content.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (snippet.payload.isNotBlank()) {
                    Text(
                        text = snippet.payload.trim().replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = ScribeTheme.colors.content.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerTemplateCard(
    template: ShortcutAction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = ScribeTheme.colors.surfaces.surfaceRaised.copy(alpha = 0.72f),
        border = BorderStroke(0.5.dp, ScribeTheme.colors.content.secondary.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ScribeTheme.colors.interaction.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Article,
                    contentDescription = null,
                    tint = ScribeTheme.colors.interaction.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ScribeTheme.colors.content.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (template.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ScribeTheme.colors.content.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = template.payload.trim().lines().take(3).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ScribeTheme.colors.content.secondary.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Canonical Manuscript Header ───────────────────────────────────────────────

@Composable
fun ManuscriptHeader(
    primaryTitleText: String,
    secondaryTitleText: String,
    selectedOrnamentId: String = "classic_flourish",
    showSecondaryTitle: Boolean = secondaryTitleText.isNotEmpty(),
    onPrimaryTitleChange: ((String) -> Unit)? = null,
    onSecondaryTitleChange: ((String) -> Unit)? = null,
    onOrnamentClick: (() -> Unit)? = null,
    onMoveToSecondaryTitle: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onBackspaceEmptySecondary: (() -> Unit)? = null,
    onEnterInSecondary: (() -> Unit)? = null,
    isEditable: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isEditable) {
        val localPrimaryFocus = remember { FocusRequester() }
        val localSecondaryFocus = remember { FocusRequester() }
        var secondaryFocusTrigger by remember { mutableIntStateOf(0) }

        val moveToSecondary: () -> Unit = {
            onMoveToSecondaryTitle?.invoke()
            secondaryFocusTrigger++
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 56.dp, end = 28.dp, bottom = 12.dp)
        ) {
            // Primary Title / Kicker (e.g., CHAPTER I)
            BasicTextField(
                value = primaryTitleText,
                onValueChange = { input ->
                    if (input.contains('\n')) {
                        val sanitized = input.replace("\n", "").trimEnd()
                        onPrimaryTitleChange?.invoke(sanitized)
                        moveToSecondary()
                    } else {
                        onPrimaryTitleChange?.invoke(input)
                    }
                },
                singleLine = false,
                maxLines = 4,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.5.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onNext = { moveToSecondary() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(localPrimaryFocus)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            moveToSecondary()
                            true
                        } else false
                    },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (primaryTitleText.isEmpty()) {
                            Text(
                                text = "CHAPTER / TITLE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 2.5.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Main Title (e.g., The Starlit Archive)
            if (showSecondaryTitle || secondaryTitleText.isNotEmpty()) {
                LaunchedEffect(secondaryFocusTrigger) {
                    if (secondaryFocusTrigger > 0) {
                        withFrameNanos { }
                        try {
                            localSecondaryFocus.requestFocus()
                        } catch (_: Exception) { }
                    }
                }
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = secondaryTitleText,
                    onValueChange = { input ->
                        if (input.contains('\n')) {
                            val sanitized = input.replace("\n", "").trimEnd()
                            onSecondaryTitleChange?.invoke(sanitized)
                            onEnterInSecondary?.invoke()
                        } else {
                            onSecondaryTitleChange?.invoke(input)
                        }
                    },
                    singleLine = false,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone?.invoke() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(localSecondaryFocus)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Backspace && secondaryTitleText.isEmpty() && event.type == KeyEventType.KeyUp) {
                                onBackspaceEmptySecondary?.invoke()
                                localPrimaryFocus.requestFocus()
                                true
                            } else if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                onEnterInSecondary?.invoke()
                                true
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (secondaryTitleText.isEmpty()) {
                                Text(
                                    text = "Manuscript Title (Optional)",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Extensible Vector Manuscript Ornament Divider
            val currentOrnament = remember(selectedOrnamentId) {
                OrnamentRegistry.getById(selectedOrnamentId)
            }

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ScribeTheme.shapes.button)
                    .clickable { onOrnamentClick?.invoke() }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                currentOrnament.Render(
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier
                )
            }
        }
    } else {
        // Pure display mode for live theme preview and non-editing views
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
        ) {
            if (primaryTitleText.isNotEmpty()) {
                Text(
                    text = primaryTitleText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (secondaryTitleText.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = secondaryTitleText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val currentOrnament = remember(selectedOrnamentId) {
                OrnamentRegistry.getById(selectedOrnamentId)
            }
            Spacer(Modifier.height(8.dp))
            currentOrnament.Render(
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier
            )
        }
    }
}

// ── Shortcut Bar Composable ───────────────────────────────────────────────────

@Composable
fun EditorShortcutBar(
    shortcuts: List<String> = listOf("B", "I", "H1", "H2", "“ ”", "—", "•"),
    onShortcutClick: (String) -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = LocalHazeState.current,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .frostedBar(hazeState)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        shortcuts.forEach { label ->
            FormatButton(label = label) {
                onShortcutClick(label)
            }
        }
    }
}

// ── Canonical Prose Body Preview ──────────────────────────────────────────────

@Composable
fun EditorProsePreviewBody(
    modifier: Modifier = Modifier
) {
    val proseStyle = ScribeTheme.typography.prose
    val dialogueStyle = ScribeTheme.typography.dialogue
    val monologueStyle = ScribeTheme.typography.monologue
    val headingStyle = ScribeTheme.typography.heading
    val colors = ScribeTheme.colors.writing

    val align = when (ScribeTheme.typography.editor.textAlignment) {
        "justified" -> TextAlign.Justify
        "center" -> TextAlign.Center
        else -> TextAlign.Left
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((ScribeTheme.typography.editor.paragraphSpacing * 12).dp.coerceAtLeast(6.dp))
    ) {
        Text(
            text = "The morning mist clung to the cobblestones of Aethelgard like silver breath. Far below, the obsidian gates creaked against the rising wind.",
            style = proseStyle.copy(color = colors.prose, textAlign = align),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "“We must reach the high pass before the eclipse,” she murmured.",
            style = dialogueStyle.copy(color = colors.dialogue, textAlign = align),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "If the garrison falls, neither iron nor prayer will hold the eastern breach.",
            style = monologueStyle.copy(color = colors.monologue, textAlign = align),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "I. The Runes of the Archway",
            style = headingStyle.copy(color = colors.heading, textAlign = align),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )

        Text(
            text = "A faint amber luminescence pulsed along the ancient lintel—a warning they could no longer afford to ignore.",
            style = proseStyle.copy(color = colors.prose, textAlign = align),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Canonical Editor Content for Live Previews & Modularity ───────────────────

/**
 * Decoupled content composable representing Scribe's canonical Editor UI.
 *
 * Renders the top bar, manuscript header with extensible ornaments, typography tokens
 * (prose, dialogue, monologue, heading), floating word count pill, and shortcut bar.
 *
 * Decoupled from ViewModel or Room persistence, for direct use in live theme previews.
 */
@Composable
fun EditorContent(
    primaryTitle: String = "CHAPTER VII",
    secondaryTitle: String = "The Obsidian Gate",
    selectedOrnamentId: String = "classic_flourish",
    wordCount: Int = 1420,
    charCount: Int = 7850,
    deltaText: String? = "+340 today",
    isPositiveDelta: Boolean = true,
    showTopBar: Boolean = false,
    showWordCountPill: Boolean = true,
    showShortcutBar: Boolean = false,
    hazeState: dev.chrisbanes.haze.HazeState? = LocalHazeState.current,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                ScribeEditorTopBar(
                    title = secondaryTitle.ifEmpty { primaryTitle },
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavClick = {},
                    actions = listOf(
                        ScribeBarAction(Icons.Default.Search, "Search") {},
                        ScribeBarAction(Icons.Default.BookmarkAdd, "Bookmark") {},
                        ScribeBarAction(Icons.Default.MoreVert, "More") {}
                    )
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    ManuscriptHeader(
                        primaryTitleText = primaryTitle,
                        secondaryTitleText = secondaryTitle,
                        selectedOrnamentId = selectedOrnamentId,
                        isEditable = false
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    EditorProsePreviewBody()

                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (showWordCountPill) {
                    WordCountPill(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp),
                        pillOffsetX = 0f,
                        pillOffsetY = 0f,
                        onOffsetChange = { _, _ -> },
                        pillMode = 0,
                        onModeClick = {},
                        wordCount = wordCount,
                        charCount = charCount,
                        deltaText = deltaText,
                        isPositiveDelta = isPositiveDelta,
                        hazeState = hazeState
                    )
                }
            }

            if (showShortcutBar) {
                EditorShortcutBar(hazeState = hazeState)
            }
        }
    }
}

// ── CodeEditor extension helpers ──────────────────────────────────────────────

private fun CodeEditor.applyFormat(open: String, close: String) {
    val cur = cursor
    if (cur.isSelected) {
        val indexer  = text.indexer
        val startIdx = indexer.getCharIndex(cur.leftLine,  cur.leftColumn)
        val endIdx   = indexer.getCharIndex(cur.rightLine, cur.rightColumn)
        val selected = text.subSequence(startIdx, endIdx).toString()
        text.replace(
            cur.leftLine,  cur.leftColumn,
            cur.rightLine, cur.rightColumn,
            "$open$selected$close"
        )
    } else {
        val line = cur.leftLine
        val col  = cur.leftColumn
        text.insert(line, col, "$open$close")
        this.cursor.set(line, col + open.length)
    }
}

private fun CodeEditor.applyLinePrefix(prefix: String) {
    val line = cursor.leftLine
    text.insert(line, 0, prefix)
    cursor.set(line, cursor.leftColumn + prefix.length)
}

private fun CodeEditor.insertAtCursor(str: String) {
    commitText(str)
}

private fun parseComposeColor(hex: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { fallback }

