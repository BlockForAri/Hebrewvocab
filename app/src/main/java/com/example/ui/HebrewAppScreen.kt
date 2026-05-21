package com.example.ui

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HebrewWord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HebrewAppScreen(viewModel: HebrewViewModel) {
    val context = LocalContext.current
    
    // TTS Engine Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        try {
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val activeEngine = engine ?: return@TextToSpeech
                    // Try Hebrew locale
                    val result = activeEngine.setLanguage(Locale("he"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Try alternative locale code
                        activeEngine.language = Locale("iw")
                    }
                }
            }
            tts = engine
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            try {
                engine?.stop()
                engine?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Speak helper that strips Nikud (pointing vowels) for cleaner TTS voice play
    val onSpeak = { text: String ->
        tts?.let { engine ->
            val cleanText = text.replace("[\\u05B0-\\u05C7]".toRegex(), "")
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        Unit
    }

    // Active state from ViewModel
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Dashboard, 1 = Word Bank, 2 = Pronunciation Guide

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "מִשְׁכָּן", // Mishkan (Sanctuary / Home of Study)
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Hebrew Vocab Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (activeMode == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Mishkan") },
                        modifier = Modifier.testTag("tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Filled.Book, contentDescription = "Word Bank") },
                        label = { Text("Word Bank") },
                        modifier = Modifier.testTag("tab_word_bank")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Filled.VolumeUp, contentDescription = "Pronunciation Guide") },
                        label = { Text("Alphabet") },
                        modifier = Modifier.testTag("tab_pronunciation")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeMode != null) {
                // Active flashcard study session overlay
                StudySessionScreen(viewModel = viewModel, onSpeak = onSpeak)
            } else {
                // Regular tabs
                when (currentTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        onStartSession = { mode -> viewModel.startSession(mode) }
                    )
                    1 -> WordBankScreen(
                        viewModel = viewModel,
                        onSpeak = onSpeak
                    )
                    2 -> AlphabetGuideScreen()
                }
            }
        }
    }
}

// Row helper to set modifiers easily
private fun RowScope.horizontalModifier(modifier: Modifier): RowScope = this

@Composable
fun DashboardScreen(
    viewModel: HebrewViewModel,
    onStartSession: (StudyMode) -> Unit
) {
    val allWords by viewModel.allWords.collectAsStateWithLifecycle()
    val hardWords by viewModel.hardWords.collectAsStateWithLifecycle()
    val dueWords by viewModel.dueWords.collectAsStateWithLifecycle()
    val timeOffsetMs by viewModel.timeOffsetMs.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    val totalWordsCount = allWords.size
    val hardWordsCount = hardWords.size
    val dueCount = dueWords.size

    // Statistics about Leitner Stages (0 to 6)
    val stageCounts = remember(allWords) {
        val counts = IntArray(7)
        for (w in allWords) {
            if (w.srsStage in 0..6) {
                counts[w.srsStage]++
            }
        }
        counts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card with Streak Context & Daily Goal Tracker
        item {
            val learnedCount = allWords.count { it.srsStage > 0 }
            val dailyTarget = 15
            val progressPercent = if (totalWordsCount > 0) {
                (learnedCount.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)
            } else {
                0.65f // Fallback mock starter progress if table is empty
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Daily Goal header exactly matching HTML
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY GOAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Progress Track background
                                Box(
                                    modifier = Modifier
                                        .width(96.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(Color(0xFFE2E8F0)) // slate-200
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressPercent)
                                            .clip(RoundedCornerShape(100))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Text(
                                    text = "$learnedCount/$dailyTarget",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Shalom Aleichem in elegant right-pinned style
                        Text(
                            text = "שָׁלוֹם עֲלֵיכֶם", // Shalom Aleichem
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Shalom, Student!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Expand your Hebrew vocabulary with Spaced-Repetition. Review your assigned desks or advance simulator clock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Dictionary",
                    value = totalWordsCount.toString(),
                    icon = Icons.Filled.Book,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Due Now",
                    value = dueCount.toString(),
                    icon = Icons.Filled.History,
                    color = if (dueCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f).testTag("due_stat_card")
                )
                StatCard(
                    title = "Hard Words",
                    value = hardWordsCount.toString(),
                    icon = Icons.Filled.Star,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Leitner Stages progress bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Leitner Spaced Repetition Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple representation of stages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val colors = listOf(
                            Color(0xFFB0BEC5), // Slate gray for Stage 0 (Unlearned)
                            Color(0xFFEE8E75), // Stage 1
                            Color(0xFFF3AD82), // Stage 2
                            Color(0xFFFCD385), // Stage 3
                            Color(0xFFC5E1A5), // Stage 4
                            Color(0xFFA1D99B), // Stage 5
                            Color(0xFF81C784)  // Stage 6 (Mastered)
                        )
                        
                        stageCounts.forEachIndexed { index, count ->
                            if (count > 0 && totalWordsCount > 0) {
                                val weight = count.toFloat() / totalWordsCount.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight)
                                        .background(colors[index])
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Legend Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = Color(0xFFB0BEC5), label = "L0: New (${stageCounts[0]})")
                        LegendItem(color = Color(0xFFEE8E75), label = "L1-3: Learning (${stageCounts[1] + stageCounts[2] + stageCounts[3]})")
                        LegendItem(color = Color(0xFF81C784), label = "L4-6: Saved (${stageCounts[4] + stageCounts[5] + stageCounts[6]})")
                    }
                }
            }
        }

        // Active Study Options
        item {
            Text(
                text = "Study Desks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Option 1: Study Due SRS Cards
                StudyOptionCard(
                    title = "Scheduled SRS Session",
                    description = "Review $dueCount Hebrew words scheduled for repetition now.",
                    icon = Icons.Filled.PlayArrow,
                    enabled = dueCount > 0,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onStartSession(StudyMode.SRS) },
                    badgeText = if (dueCount > 0) "$dueCount DUE" else "UP TO DATE",
                    testTag = "start_srs_quiz"
                )

                // Option 2: Extra review of HARD WORDS
                StudyOptionCard(
                    title = "Hard Words Stack",
                    description = "Take extra time on the $hardWordsCount difficult words you bookmarked.",
                    icon = Icons.Filled.Star,
                    enabled = hardWordsCount > 0,
                    tint = MaterialTheme.colorScheme.tertiary,
                    onClick = { onStartSession(StudyMode.HARD_WORDS) },
                    badgeText = "CUSTOM REVIEW"
                )

                // Option 3: All dictionary random shuffle
                StudyOptionCard(
                    title = "Casual Word Shuffle",
                    description = "Review all $totalWordsCount words in a randomly ordered deck.",
                    icon = Icons.Filled.Refresh,
                    enabled = totalWordsCount > 0,
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = { onStartSession(StudyMode.ALL) },
                    badgeText = "CASUAL PLAY"
                )
            }
        }

        // Simulated Time Adjuster (Critical for Android SRS app check and user testing!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⏰ Spaced-Repetition Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SRS works based on review intervals (20s, 90s, 5m, 15m, 2h, 1d). Use these buttons to simulate passing of time and force cards into 'Due' status instantly!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val sdf = remember { SimpleDateFormat("hh:mm:ss a, EEE dd MMM", Locale.US) }
                    val dateFormatted = sdf.format(Date(currentTime))
                    Text(
                        text = "Simulated Time: $dateFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (timeOffsetMs > 0L) {
                        val hoursPassed = timeOffsetMs / (1000 * 60 * 60)
                        val minsPassed = (timeOffsetMs / (1000 * 60)) % 60
                        Text(
                            text = "Time advanced by: ${hoursPassed}h ${minsPassed}m",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.advanceTime(1) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("+1m", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.advanceTime(10) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("+10m", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.advanceTime(60) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("+1h", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.advanceTime(1440) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("+1d", fontSize = 11.sp)
                        }
                    }

                    if (timeOffsetMs > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.resetTimeOffset() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Reset Real Time Clock ↩", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StudyOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    badgeText: String,
    testTag: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) tint else tint.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (enabled) tint.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) tint else Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ==========================================
// ACTIVE STUDY SESSION MODULE
// ==========================================
@Composable
fun StudySessionScreen(
    viewModel: HebrewViewModel,
    onSpeak: (String) -> Unit
) {
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val studyList by viewModel.studyList.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isCardFlipped.collectAsStateWithLifecycle()
    val sessionComplete by viewModel.sessionComplete.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP CONTROL AND PROGRESS RATIO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.endSession() }
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit Study Session")
            }

            Text(
                text = when (activeMode) {
                    StudyMode.SRS -> "SRS Quiz Desk"
                    StudyMode.HARD_WORDS -> "Hard Review Desk"
                    StudyMode.ALL -> "Casual Card Shuffle"
                    null -> "Study Session"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Minimal text indicator
            if (studyList.isNotEmpty() && !sessionComplete) {
                Text(
                    text = "${currentIndex + 1} of ${studyList.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (studyList.isNotEmpty() && !sessionComplete && currentIndex < studyList.size) {
            val progressPercent = (currentIndex.toFloat() / studyList.size.toFloat())
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            val word = studyList[currentIndex]

            Spacer(modifier = Modifier.weight(0.12f))

            // FLASHCARD COMPONENT (Animated flips / Switch transitions)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable { viewModel.flipCard() }
                    .testTag("flashcard_button"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Quick state items (Top Row)
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
                        // SRS Level Badge styled matching HTML
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clip(RoundedCornerShape(100))
                                .background(Color(0xFFF0FDF4)) // bg-green-50
                                .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(100))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF22C55E)) // bg-green-500
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Level ${word.srsStage} · ${if (word.srsStage >= 4) "Retained" else "Learning"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D) // text-green-700
                            )
                        }

                        // Hard Word bookmark inside Flashcard on top right
                        IconButton(
                            onClick = { viewModel.toggleHardWord(word) },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = if (word.isHardWord) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Toggle Saved Word Status",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Card Faces Content (Crossfade for a visual transition)
                    Crossfade(
                        targetState = isFlipped,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 48.dp),
                        label = "cardFlipAnimation"
                    ) { flipped ->
                        if (!flipped) {
                            // FRONT FACE (Large bold clean Hebrew characters with vowels)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = word.hebrew,
                                        fontSize = 68.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = "/${word.transliteration.lowercase(Locale.US)}/",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8), // Slate 400
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    // Part of speech category label tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = word.partOfSpeech.lowercase(Locale.US),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Interactive Listen to Audio pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF8FAFC)) // bg-slate-50
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                        .clickable { onSpeak(word.hebrew) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB), // text-blue-600
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Listen Pronunciation",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569) // text-slate-600
                                    )
                                }

                                // Tap to Flip guide footer
                                Column {
                                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tap card to flip meaning",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF94A3B8) // slate-400
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFFCBD5E1), // slate-300
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // BACK FACE (Translation, Phonetics breakdown, guidelines)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = word.english,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Text(
                                    text = "TRANSLITERATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                                )
                                Text(
                                    text = word.transliteration,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "ACCENT / PRONUNCIATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                                )
                                Text(
                                    text = word.pronunciationGuide,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SRS stage: ${word.srsStage}  •  Streak: ${word.correctStreak}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // BOTTOM RATING CONTROLS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isFlipped) {
                    Button(
                        onClick = { viewModel.flipCard() },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(56.dp)
                            .testTag("reveal_card_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("REVEAL CARD ☝", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "How well did you know this word?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AGAIN BUTTON - Styled red dual tone
                        Button(
                            onClick = { viewModel.submitAssessment(Assessment.AGAIN) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEF2F2),
                                contentColor = Color(0xFFB91C1C)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2)),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("assess_again"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Forgot ✗", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Review Again", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                            }
                        }

                        // GOOD BUTTON - Styled slate dual tone
                        Button(
                            onClick = { viewModel.submitAssessment(Assessment.GOOD) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF475569)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("assess_good"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hesitated ✓", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Keep Interval", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            }
                        }

                        // EASY BUTTON - Styled green dual tone
                        Button(
                            onClick = { viewModel.submitAssessment(Assessment.EASY) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0FDF4),
                                contentColor = Color(0xFF15803D)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCFCE7)),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("assess_easy"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Easy! ⭐", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Extend Interval", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))

        } else if (sessionComplete) {
            // END OF STUDY ROUND SCREEN
            Spacer(modifier = Modifier.weight(0.15f))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "כָּל הַכָּבוֹד!", // Kol HaKavod (Well done!)
                        fontFamily = FontFamily.Serif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Kol HaKavod! (Super job)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "You've successfully completed this study session! Your spaced repetition scores have been stored securely. Return to your dashboard to review remaining cards or fast-forward time to simulate review scheduler delivery.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.endSession() },
                        modifier = Modifier
                            .fillMaxWidth(0.81f)
                            .height(52.dp)
                            .testTag("exit_session_ok"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("BACK TO DASHBOARD ↩", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.2f))
        } else {
            // EMPTY LIST FALLBACK
            Spacer(modifier = Modifier.weight(0.15f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No cards selected.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add custom vocabulary words or select a different desk review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.endSession() }
                ) {
                    Text("Return to Dashboard")
                }
            }
            Spacer(modifier = Modifier.weight(0.2f))
        }
    }
}

// ==========================================
// WORD BANK MODULE (DICTIONARY AND ADD WORD)
// ==========================================
@Composable
fun WordBankScreen(
    viewModel: HebrewViewModel,
    onSpeak: (String) -> Unit
) {
    val allWords by viewModel.allWords.collectAsStateWithLifecycle()
    val hardWords by viewModel.hardWords.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyHardWords by remember { mutableStateOf(false) }
    var expandedWordId by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = remember(allWords, hardWords, searchQuery, filterOnlyHardWords) {
        val baseList = if (filterOnlyHardWords) hardWords else allWords
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.hebrew.contains(searchQuery, ignoreCase = true) ||
                        it.english.contains(searchQuery, ignoreCase = true) ||
                        it.transliteration.contains(searchQuery, ignoreCase = true) ||
                        it.partOfSpeech.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Row
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Hebrew, English, Category...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vocabulary_search"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Bookmark selection switch
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (filterOnlyHardWords) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredList.size} Words Listed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filterOnlyHardWords) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { filterOnlyHardWords = !filterOnlyHardWords }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (filterOnlyHardWords) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtered Hard-Words Only",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterOnlyHardWords) "No bookmarked Hard Words yet!" else "No search results match",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (filterOnlyHardWords) {
                            Text(
                                text = "Tick the star icon next to difficult words in the vocabulary bank to save them here for custom study sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Word item List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Avoid overlap with active Floating action button
                ) {
                    items(filteredList, key = { it.id }) { word ->
                        val isExpanded = expandedWordId == word.id
                        
                        Card(
                            modifier = Modifier
                                .fillModifier(word.id)
                                .clickable { expandedWordId = if (isExpanded) null else word.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hebrew text column
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = word.hebrew,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "/${word.transliteration.lowercase(Locale.US)}/",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF94A3B8) // Slate 400
                                        )
                                    }
 
                                    // English translation
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = word.english,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = word.partOfSpeech.lowercase(Locale.US),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Bookmark and Listen triggers
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(onClick = { onSpeak(word.hebrew) }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Speak pronunciation audio",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        IconButton(onClick = { viewModel.toggleHardWord(word) }) {
                                            Icon(
                                                imageVector = if (word.isHardWord) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                contentDescription = "Toggle saved bookmark status",
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }

                                // Expanded details view
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .background(
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "Consonantal Syllable Breakdown:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = word.pronunciationGuide,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val scheduleText = if (word.nextReviewTime == 0L) {
                                                "Due now (not yet reviewed)"
                                            } else {
                                                val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.US)
                                                "Review due on: " + sdf.format(Date(word.nextReviewTime))
                                            }
                                            
                                            Text(
                                                text = "Leitner Stage: ${word.srsStage}  •  Streak: ${word.correctStreak}\n$scheduleText",
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )

                                            IconButton(
                                                onClick = { viewModel.deleteWord(word) },
                                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete custom word permanently"
                                                )
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

        // Expanded Floating action button to generate customized word
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_custom_word_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add New Custom Words")
        }

        if (showAddDialog) {
            AddWordDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { heb, trans, eng, pos, pron ->
                    viewModel.addNewWord(heb, trans, eng, pos, pron)
                    showAddDialog = false
                }
            )
        }
    }
}

// Custom modifier extension helper to build unique tags
private fun Modifier.fillModifier(id: Int): Modifier {
    return this.fillMaxWidth().testTag("word_item_$id")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var hebrewInput by remember { mutableStateOf("") }
    var transliterationInput by remember { mutableStateOf("") }
    var englishInput by remember { mutableStateOf("") }
    var posInput by remember { mutableStateOf("Noun") }
    var pronunciationInput by remember { mutableStateOf("") }

    val posList = listOf("Noun", "Verb", "Greeting", "Adjective", "Adverb", "Pronoun", "Phrase")
    var posMenuExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .testTag("add_word_dialog_frame"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Custom Word",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Add custom words to expand your learning library. You can include Hebrew vowel mappings (Nikud) for accurate references.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Hebrew Word Input
                OutlinedTextField(
                    value = hebrewInput,
                    onValueChange = { hebrewInput = it },
                    label = { Text("Hebrew Letters (e.g. מַיִם)") },
                    placeholder = { Text("שלום / מים") },
                    modifier = Modifier.fillMaxWidth().testTag("input_hebrew"),
                    singleLine = true
                )

                // Transliteration
                OutlinedTextField(
                    value = transliterationInput,
                    onValueChange = { transliterationInput = it },
                    label = { Text("Transliteration / Syllables") },
                    placeholder = { Text("Shalom / Mayim") },
                    modifier = Modifier.fillMaxWidth().testTag("input_translit"),
                    singleLine = true
                )

                // Meaning
                OutlinedTextField(
                    value = englishInput,
                    onValueChange = { englishInput = it },
                    label = { Text("English Meaning") },
                    placeholder = { Text("Water / Hello / Peace") },
                    modifier = Modifier.fillMaxWidth().testTag("input_english"),
                    singleLine = true
                )

                // Part Of Speech
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = posInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category / Part Of Speech") },
                        trailingIcon = {
                            IconButton(onClick = { posMenuExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { posMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = posMenuExpanded,
                        onDismissRequest = { posMenuExpanded = false }
                    ) {
                        posList.forEach { pos ->
                            DropdownMenuItem(
                                text = { Text(pos) },
                                onClick = {
                                    posInput = pos
                                    posMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Pronunciation phonetic details
                OutlinedTextField(
                    value = pronunciationInput,
                    onValueChange = { pronunciationInput = it },
                    label = { Text("Pronunciation guides / accents") },
                    placeholder = { Text("MAH-yeem (Emphasis on the 'MAH')") },
                    modifier = Modifier.fillMaxWidth().testTag("input_pronunciation")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (hebrewInput.isNotBlank() && englishInput.isNotBlank()) {
                                onAdd(
                                    hebrewInput,
                                    transliterationInput,
                                    englishInput,
                                    posInput,
                                    pronunciationInput.ifBlank { transliterationInput }
                                )
                            }
                        },
                        enabled = hebrewInput.isNotBlank() && englishInput.isNotBlank(),
                        modifier = Modifier.testTag("dialog_save_btn")
                    ) {
                        Text("ADD WORD")
                    }
                }
            }
        }
    }
}

// ==========================================
// PRONUNCIATION & ALPHABET GUIDE MODULE
// ==========================================
@Composable
fun AlphabetGuideScreen() {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Letters/Consonants, 1 = Vowels (Nikud)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Sub tab switcher
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Hebrew Letters", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Vowel Signs (Nikud)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeSubTab == 0) {
            // LETTERS GRID
            val alphabetList = getAlphabetData()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(alphabetList) { item ->
                    AlphabetCard(item = item)
                }
            }
        } else {
            // VOWELS GRID
            val vowelsList = getVowelsData()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(vowelsList) { item ->
                    VowelCard(item = item)
                }
            }
        }
    }
}

data class AlphabetItem(val letter: String, val name: String, val phonetic: String, val notes: String)

fun getAlphabetData(): List<AlphabetItem> {
    return listOf(
        AlphabetItem("א", "Alef", "Silent / Glottal Stop", "Takes the sound of its associated vowel sign."),
        AlphabetItem("בּ / ב", "Bet / Vet", "B sound / V sound", "Dot inside (Dagesh) makes it hard: 'B'. Silent dot: 'V'."),
        AlphabetItem("ג", "Gimel", "G (as in Go)", "Hard G sound."),
        AlphabetItem("ד", "Dalet", "D (as in Day)", "Standard English D sound."),
        AlphabetItem("ה", "He", "H (as in Home)", "Usually silent at the end of words."),
        AlphabetItem("ו", "Vav", "V / O / Oo", "Can serve as a consonant 'V' or vowel sounds 'Oh' / 'Oo'."),
        AlphabetItem("ז", "Zayin", "Z (as in Zebra)", "Standard Z sound."),
        AlphabetItem("ח", "Chet", "Ch (Guttural)", "Deep, friction sound in the back of the throat like German 'Bach'."),
        AlphabetItem("ט", "Tet", "T sound", "Standard English T sound."),
        AlphabetItem("י", "Yod", "Y (as in Yellow)", "Also acts as a vowel helper for 'Ee' or 'Eh' sound."),
        AlphabetItem("כּ / כ", "Kaf / Chaf", "K sound / Ch (Guttural)", "With dot: 'K'. No dot: deep throat friction 'Ch' (ך final form)."),
        AlphabetItem("ל", "Lamed", "L (as in Love)", "Standard English L sound."),
        AlphabetItem("מ", "Mem", "M sound", "Traditional M sound (ם final form used at ends of words)."),
        AlphabetItem("נ", "Nun", "N sound", "Traditional N sound (ן final form used at ends of words)."),
        AlphabetItem("ס", "Samech", "S (as in Sun)", "Standard English S hiss sound."),
        AlphabetItem("ע", "Ayin", "Guttural / Silent", "Similar to Alef, acts as a vowel carrier in modern Hebrew."),
        AlphabetItem("פּ / פ", "Pe / Fe", "P sound / F sound", "With dot: 'P'. No dot: 'F' sound (ף final form at ends)."),
        AlphabetItem("צ", "Tsade", "Ts (as in Cats)", "Affricate 'ts' sound (ץ final form at ends)."),
        AlphabetItem("ק", "Kof", "K sound", "Standard English hard K sound."),
        AlphabetItem("ר", "Resh", "R (Throat Soft)", "Soft uvular R, similar to French or German pronunciation."),
        AlphabetItem("שׁ / שׂ", "Shin / Sin", "Sh / S sound", "Point on right is 'Sh' (Shemesh). Point on left is 'S' (Sin)."),
        AlphabetItem("ת", "Tav", "T sound", "Standard English hard T sound.")
    )
}

@Composable
fun AlphabetCard(item: AlphabetItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Text(
                    text = item.letter,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sound: ${item.phonetic}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.notes,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class VowelItem(val symbol: String, val name: String, val sound: String, val sample: String)

fun getVowelsData(): List<VowelItem> {
    return listOf(
        VowelItem("  ָ   ", "Kamatz", "Ah (as in Father)", "דָּבָר = Da-var"),
        VowelItem("  ַ   ", "Patach", "Ah (as in Father)", "בַּיִת = Ba-yit"),
        VowelItem("  ֵ   ", "Tzere", "Eh (as in Shed)", "סֵפֶר = Se-fer"),
        VowelItem("  ֶ   ", "Segol", "Eh (as in Shed)", "יֶלֶד = Ye-led"),
        VowelItem("  ִ   ", "Hiriq", "Ee (as in Meet)", "אִישׁ = Eesh"),
        VowelItem(" וֹ   ", "Holam", "Oh (as in Boat)", "יוֹם = Yohm"),
        VowelItem(" וּ   ", "Shuruk", "Oo (as in Root)", "בַּקָּשָׁה = Be-va-ka-sha"),
        VowelItem("  ְ   ", "Sheva", "Short vocal break or Silent", "בְּבַקָּשָׁה = Be-va-ka-sha")
    )
}

@Composable
fun VowelCard(item: VowelItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.symbol,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sound: ${item.sound}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Example: ${item.sample}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
