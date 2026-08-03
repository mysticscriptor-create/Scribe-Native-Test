package com.primaloptima.scribe.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primaloptima.scribe.ScribeApp
import com.primaloptima.scribe.ThemeListActivity
import com.primaloptima.scribe.ui.theme.FrostedDialog
import com.primaloptima.scribe.util.WritingStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ScribeApp
    val prefs = remember { app.prefs }
    val themeManager = remember { app.themeManager }
    val writingStats = remember { WritingStats(prefs) }

    var showWordCount by remember { mutableStateOf(prefs.showWordCount) }
    var typewriterMode by remember { mutableStateOf(prefs.typewriterMode) }
    var lineSpacing by remember { mutableStateOf(prefs.lineSpacing) }
    var fontSize by remember { mutableFloatStateOf(prefs.editorFontSize.toFloat()) }
    var dailyGoal by remember { mutableIntStateOf(prefs.dailyGoal) }

    var autoHistoryEnabled by remember { mutableStateOf(prefs.autoHistoryEnabled) }
    var manualCheckpointsEnabled by remember { mutableStateOf(prefs.manualCheckpointsEnabled) }
    var autoHistorySlots by remember { mutableFloatStateOf(prefs.autoHistorySlots.toFloat()) }
    var manualCheckpointSlots by remember { mutableFloatStateOf(prefs.manualCheckpointSlots.toFloat()) }
    var autoHistoryMinWords by remember { mutableFloatStateOf(prefs.autoHistoryMinWords.toFloat()) }

    var showGoalDialog by remember { mutableStateOf(false) }

    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionHeader(icon = Icons.Default.Palette, label = "Appearance")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                onClick = {
                    context.startActivity(Intent(context, ThemeListActivity::class.java))
                }
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Active Theme",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            themeManager.activeTheme().name,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            SectionHeader(icon = Icons.Default.EditNote, label = "Writing Options")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SwitchRow(
                        title = "Show Word Count FAB",
                        checked = showWordCount,
                        onCheckedChange = {
                            showWordCount = it
                            prefs.showWordCount = it
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                    SwitchRow(
                        title = "Typewriter Mode",
                        subtitle = "Center active line while typing",
                        checked = typewriterMode,
                        onCheckedChange = {
                            typewriterMode = it
                            prefs.typewriterMode = it
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Editor Font Size", fontWeight = FontWeight.Medium)
                            Text(
                                "${fontSize.toInt()} sp",
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = fontSize,
                            onValueChange = {
                                fontSize = it
                                prefs.editorFontSize = it.toInt()
                            },
                            valueRange = 12f..28f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor
                            )
                        )
                    }
                }
            }

            SectionHeader(icon = Icons.Default.History, label = "Version History")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    SwitchRow(
                        title = "Auto-save History",
                        subtitle = "Saves a snapshot when you leave a note",
                        checked = autoHistoryEnabled,
                        onCheckedChange = { autoHistoryEnabled = it; prefs.autoHistoryEnabled = it }
                    )

                    AnimatedVisibility(
                        visible = autoHistoryEnabled,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            HorizontalDivider(thickness = 0.5.dp)
                            SliderLabeled(
                                label = "Auto-save slots",
                                value = autoHistorySlots,
                                onValueChange = { autoHistorySlots = it; prefs.autoHistorySlots = it.toInt() },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                            SliderLabeled(
                                label = "Min words to trigger",
                                value = autoHistoryMinWords,
                                onValueChange = { autoHistoryMinWords = it; prefs.autoHistoryMinWords = it.toInt() },
                                valueRange = 1f..100f,
                                steps = 98
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    SwitchRow(
                        title = "Manual Checkpoints",
                        subtitle = "Saved when you tap the bookmark button",
                        checked = manualCheckpointsEnabled,
                        onCheckedChange = { manualCheckpointsEnabled = it; prefs.manualCheckpointsEnabled = it }
                    )

                    AnimatedVisibility(
                        visible = manualCheckpointsEnabled,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            SliderLabeled(
                                label = "Checkpoint slots",
                                value = manualCheckpointSlots,
                                onValueChange = { manualCheckpointSlots = it; prefs.manualCheckpointSlots = it.toInt() },
                                valueRange = 1f..30f,
                                steps = 28
                            )
                        }
                    }
                }
            }

            SectionHeader(icon = Icons.Default.Flag, label = "Goals & Progress")
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                onClick = { showGoalDialog = true }
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Word Goal", fontWeight = FontWeight.SemiBold)
                        AnimatedGoalNumber(dailyGoal)
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                    StatRow(label = "Words today", value = "${writingStats.todayWords}")
                    StatRow(label = "Current streak", value = "${writingStats.currentStreak()} days")
                    StatRow(label = "Longest streak", value = "${writingStats.longestStreak()} days")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf("$dailyGoal") }
        FrostedDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Word Goal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("Target Words") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = goalInput.toIntOrNull()
                        if (num != null && num >= 50) {
                            dailyGoal = num
                            writingStats.setDailyGoal(num)
                            showGoalDialog = false
                            Toast.makeText(context, "Daily goal updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = accent,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SliderLabeled(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    val accent = MaterialTheme.colorScheme.primary
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(
                "${value.toInt()}",
                fontWeight = FontWeight.Bold,
                color = accent,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            )
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AnimatedGoalNumber(goal: Int) {
    val animated by animateIntAsState(
        targetValue = goal,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "goalAnim"
    )
    Text(
        "$animated words",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 15.sp
    )
}
