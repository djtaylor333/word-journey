package com.djtaylor.wordjourney.ui.levelselect

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.domain.model.Difficulty
import com.djtaylor.wordjourney.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
// Zone Themes — every 10 levels a new visual world
// ═══════════════════════════════════════════════════════════════════════════════

data class ZoneTheme(
    val name: String,
    val emoji: String,
    val bgTop: Color,
    val bgBottom: Color,
    val pathColor: Color,
    val glow: Color,
    val decor: List<String>
)

private val zones = listOf(
    ZoneTheme("Enchanted Meadow", "🌿",   Color(0xFF1A3D2E), Color(0xFF0D2118), Color(0xFF4ADE80), Color(0xFF22C55E), listOf("🌸", "🦋", "🌼", "🐝", "🍀")),
    ZoneTheme("Crystal Cavern",  "💎",    Color(0xFF1A1A3D), Color(0xFF0D0D21), Color(0xFF818CF8), Color(0xFF6366F1), listOf("💎", "✨", "🔮", "⚡", "🌟")),
    ZoneTheme("Sunset Desert",   "🏜️",   Color(0xFF3D2A1A), Color(0xFF211A0D), Color(0xFFFBBF24), Color(0xFFF59E0B), listOf("🌵", "🦎", "☀️", "🏜️", "🐪")),
    ZoneTheme("Frozen Peaks",    "🏔️",   Color(0xFF1A2D3D), Color(0xFF0D1821), Color(0xFF67E8F9), Color(0xFF06B6D4), listOf("❄️", "🏔️", "🌨️", "⛷️", "🦌")),
    ZoneTheme("Volcanic Core",   "🌋",    Color(0xFF3D1A1A), Color(0xFF210D0D), Color(0xFFF87171), Color(0xFFEF4444), listOf("🌋", "🔥", "💥", "🪨", "⚡")),
    ZoneTheme("Mystic Forest",   "🌲",    Color(0xFF0D3D2A), Color(0xFF082118), Color(0xFF34D399), Color(0xFF10B981), listOf("🌲", "🍄", "🦉", "🌿", "🦊")),
    ZoneTheme("Starlit Sky",     "🌌",    Color(0xFF1A1A4D), Color(0xFF0D0D28), Color(0xFFC084FC), Color(0xFFA855F7), listOf("⭐", "🌙", "🌌", "💫", "🪐")),
    ZoneTheme("Ocean Depths",    "🌊",    Color(0xFF0D2D3D), Color(0xFF081821), Color(0xFF38BDF8), Color(0xFF0EA5E9), listOf("🌊", "🐠", "🐙", "🐚", "🪸")),
    ZoneTheme("Ancient Ruins",   "🏛️",   Color(0xFF2D2A1A), Color(0xFF181710), Color(0xFFFCD34D), Color(0xFFEAB308), listOf("🏛️", "🗿", "📜", "🏺", "⚱️")),
    ZoneTheme("Dragon's Summit", "🐉",    Color(0xFF3D0D1A), Color(0xFF210810), Color(0xFFFB7185), Color(0xFFF43F5E), listOf("🐉", "👑", "💎", "🗡️", "🏰"))
)

private fun zoneFor(level: Int) = zones[((level - 1) / 10) % zones.size]

// ═══════════════════════════════════════════════════════════════════════════════
// Main Screen
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    difficultyKey: String,
    onNavigateToGame: (String, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: LevelSelectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val difficulty = state.difficulty
    val accent = when (difficulty) {
        Difficulty.EASY    -> AccentEasy
        Difficulty.REGULAR -> AccentRegular
        Difficulty.HARD    -> AccentHard
    }

    // Heart shrink animation
    var showHeartAnim by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnim) 0.5f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heartScale",
        finishedListener = { if (showHeartAnim) showHeartAnim = false }
    )

    var pendingLevel by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.lifeDeducted) {
        if (state.lifeDeducted) {
            showHeartAnim = true
            delay(500)
            viewModel.resetLifeAnimation()
            if (pendingLevel > 0) {
                onNavigateToGame(difficultyKey, pendingLevel)
                pendingLevel = 0
            }
        }
    }

    // Scroll to current level row on load
    val scrollState = rememberScrollState()
    LaunchedEffect(state.currentLevel, state.isLoading) {
        if (!state.isLoading && state.currentLevel > 3) {
            val rowPx = (state.currentLevel - 1) * 320 // approx 120dp * density
            scrollState.animateScrollTo(rowPx.coerceAtLeast(0))
        }
    }

    // Ambient float for decorations
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "floatPhase"
    )

    val curZone = zoneFor(state.currentLevel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${difficulty.displayName} Journey", fontWeight = FontWeight.Bold, fontSize = 23.sp)
                        Text("${curZone.emoji} ${curZone.name}", fontSize = 14.sp, color = curZone.glow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("❤️", fontSize = 32.sp, modifier = Modifier.scale(heartScale))
                            Text("${state.lives}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(y = 1.dp))
                        }
                        if (state.bonusLives > 0) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BonusHeartBlue, modifier = Modifier.padding(horizontal = 2.dp))
                            Box(contentAlignment = Alignment.Center) {
                                Text("💙", fontSize = 32.sp)
                                Text("${state.bonusLives}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.offset(y = 1.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = curZone.bgTop)
            )
        },
        bottomBar = {
            Surface(color = curZone.bgTop, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.timerDisplayMs > 0L && state.lives + state.bonusLives <= 0) {
                        Text("⏱ Next life in ${fmtTimer(state.timerDisplayMs)}", style = MaterialTheme.typography.bodyMedium, color = accent)
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = {
                            viewModel.playButtonClick()
                            val lvl = state.currentLevel
                            if (viewModel.canStartLevel(lvl) && viewModel.deductLifeForLevel(lvl)) pendingLevel = lvl
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = state.lives + state.bonusLives > 0
                    ) { Text("Continue — Level ${state.currentLevel}", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                val total = state.totalLevels
                val zoneCount = (total + 9) / 10
                for (zi in 0 until zoneCount) {
                    val start = zi * 10 + 1
                    val end = minOf(start + 9, total)
                    ZoneSection(
                        zone = zoneFor(start),
                        zoneIdx = zi,
                        levels = (start..end).toList(),
                        currentLevel = state.currentLevel,
                        accent = accent,
                        floatPhase = floatPhase,
                        difficultyKey = difficultyKey,
                        onLevelClick = { level ->
                            val completed = level < state.currentLevel
                            val current = level == state.currentLevel
                            if (!completed && !current) return@ZoneSection
                            viewModel.playButtonClick()
                            if (completed) {
                                onNavigateToGame(difficultyKey, level)
                            } else if (viewModel.deductLifeForLevel(level)) {
                                pendingLevel = level
                            }
                        }
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // No-lives dialog
    if (state.showNoLivesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoLivesDialog() },
            title = { Text("Out of Lives!", fontSize = 22.sp) },
            text = {
                Column {
                    Text("You need at least 1 life to start a new level.", fontSize = 17.sp)
                    if (state.timerDisplayMs > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Text("⏱ Next life in ${fmtTimer(state.timerDisplayMs)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.dismissNoLivesDialog() }) { Text("OK", fontSize = 17.sp) } }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Zone Section — one themed block of 10 levels
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ZoneSection(
    zone: ZoneTheme,
    zoneIdx: Int,
    levels: List<Int>,
    currentLevel: Int,
    accent: Color,
    floatPhase: Float,
    difficultyKey: String,
    onLevelClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(brush = Brush.verticalGradient(listOf(zone.bgTop, zone.bgBottom)))
                drawSparkles(zone, floatPhase, zoneIdx)
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            // Zone banner
            ZoneBanner(zone, levels.first(), levels.last())

            // Pathway
            Pathway(levels, currentLevel, zone, accent, floatPhase, onLevelClick)
        }
    }
}

@Composable
private fun ZoneBanner(zone: ZoneTheme, first: Int, last: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = zone.glow.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, zone.glow.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(zone.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(zone.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = zone.glow)
                Text("Levels $first – $last", fontSize = 14.sp, color = zone.glow.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(12.dp))
            Text(zone.emoji, fontSize = 28.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Winding Pathway
// ═══════════════════════════════════════════════════════════════════════════════

private val hPositions = listOf(0.2f, 0.5f, 0.8f, 0.5f) // zigzag x-fractions

@Composable
private fun Pathway(
    levels: List<Int>,
    currentLevel: Int,
    zone: ZoneTheme,
    accent: Color,
    floatPhase: Float,
    onLevelClick: (Int) -> Unit
) {
    val alignments = listOf(Arrangement.Start, Arrangement.Center, Arrangement.End, Arrangement.Center)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .drawBehind { drawCurvedPath(levels.size, zone, currentLevel, levels.first()) }
    ) {
        for ((idx, level) in levels.withIndex()) {
            val posIdx = idx % alignments.size
            val arr = alignments[posIdx]
            val completed = level < currentLevel
            val current = level == currentLevel
            val locked = level > currentLevel
            val decorEmoji = zone.decor[idx % zone.decor.size]
            val showDecor = idx % 3 == 0

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arr
            ) {
                // Left decor
                if (showDecor && posIdx != 0) {
                    val dy = sin((floatPhase + idx * 0.3f) * 2 * PI.toFloat()) * 6f
                    Text(decorEmoji, fontSize = 22.sp, modifier = Modifier.alpha(0.55f).offset(y = dy.dp))
                    Spacer(Modifier.width(8.dp))
                }
                if (posIdx == 0) Spacer(Modifier.width(24.dp))

                LevelNode(level, completed, current, locked, zone, accent, onLevelClick = { onLevelClick(level) })

                if (posIdx == 2) Spacer(Modifier.width(24.dp))
                // Right decor
                if (showDecor && posIdx != 2) {
                    Spacer(Modifier.width(8.dp))
                    val dy = sin((floatPhase + idx * 0.5f) * 2 * PI.toFloat()) * 6f
                    Text(zone.decor[(idx + 2) % zone.decor.size], fontSize = 22.sp, modifier = Modifier.alpha(0.55f).offset(y = dy.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Level Node
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LevelNode(
    level: Int,
    completed: Boolean,
    current: Boolean,
    locked: Boolean,
    zone: ZoneTheme,
    accent: Color,
    onLevelClick: () -> Unit
) {
    val pulse = if (current) {
        rememberInfiniteTransition(label = "p$level").animateFloat(
            1f, 1.12f,
            infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "ps$level"
        ).value
    } else 1f

    val nodeColor = when {
        completed -> zone.glow
        current   -> accent
        else      -> Color.Gray.copy(alpha = 0.35f)
    }
    val glowCol = when {
        current   -> accent.copy(alpha = 0.4f)
        completed -> zone.glow.copy(alpha = 0.2f)
        else      -> Color.Transparent
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(pulse)) {
        // Glow
        if (!locked) {
            Box(
                modifier = Modifier.size(76.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(glowCol, Color.Transparent)))
            )
        }
        // Node circle
        Surface(
            onClick = onLevelClick,
            enabled = !locked,
            shape = CircleShape,
            color = nodeColor,
            border = when {
                current   -> BorderStroke(3.dp, accent)
                completed -> BorderStroke(2.dp, zone.glow.copy(alpha = 0.6f))
                else      -> BorderStroke(2.dp, Color.Gray.copy(alpha = 0.3f))
            },
            shadowElevation = if (current) 8.dp else if (completed) 4.dp else 0.dp,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    when {
                        locked -> Text("🔒", fontSize = 20.sp)
                        completed -> {
                            Text("✓", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("$level", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                        else -> Text("$level", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }

        // Star for milestones
        if (level % 10 == 0 && completed) {
            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)) {
                Text("⭐", fontSize = 18.sp)
            }
        }
        // Current arrow
        if (current) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 8.dp)) {
                Text("▲", fontSize = 12.sp, color = accent)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Canvas draw helpers
// ═══════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawCurvedPath(
    count: Int,
    zone: ZoneTheme,
    currentLevel: Int,
    firstLevel: Int
) {
    if (count < 2) return
    val rowH = 120.dp.toPx()
    val pts = (0 until count).map { idx ->
        val x = size.width * hPositions[idx % hPositions.size]
        val y = idx * rowH + rowH / 2
        Offset(x, y)
    }

    for (i in 0 until pts.size - 1) {
        val from = pts[i]
        val to = pts[i + 1]
        val level = firstLevel + i
        val isDone = level < currentLevel

        val path = Path().apply {
            moveTo(from.x, from.y)
            val my = (from.y + to.y) / 2
            cubicTo(from.x, my, to.x, my, to.x, to.y)
        }

        if (isDone) {
            // Completed: solid bright + glow
            drawPath(path, zone.pathColor.copy(alpha = 0.6f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, zone.pathColor.copy(alpha = 0.12f), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            // Locked: dashed dim
            drawPath(path, zone.pathColor.copy(alpha = 0.15f), style = Stroke(
                width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f))
            ))
        }
    }
}

private fun DrawScope.drawSparkles(zone: ZoneTheme, phase: Float, zoneIdx: Int) {
    for (i in 0 until 14) {
        val seed = (zoneIdx * 100 + i * 37) % 1000
        val bx = (seed * 7 % 1000) / 1000f * size.width
        val by = (seed * 13 % 1000) / 1000f * size.height
        val p = (phase + seed / 1000f) % 1f
        val yo = sin(p * 2 * PI.toFloat()) * 20f
        val a = (sin(p * PI.toFloat()) * 0.3f).coerceIn(0.05f, 0.3f)
        drawCircle(zone.glow.copy(alpha = a), radius = (2 + seed % 3).toFloat().dp.toPx(), center = Offset(bx, by + yo))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════

private fun fmtTimer(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
