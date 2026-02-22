package com.djtaylor.wordjourney.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.djtaylor.wordjourney.domain.model.BackgroundPattern
import com.djtaylor.wordjourney.domain.model.GameTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Decorative pattern overlay matching the current theme.
 * Combines a gradient wash with drawn patterns at a visible opacity.
 */
@Composable
fun ThemeBackgroundOverlay(
    theme: GameTheme,
    modifier: Modifier = Modifier,
    alpha: Float = 0.22f
) {
    val hasGradient = theme.gradientTop != Color.Transparent ||
            theme.gradientMid != Color.Transparent ||
            theme.gradientBottom != Color.Transparent
    val hasPattern = theme.backgroundPattern != BackgroundPattern.NONE

    if (!hasGradient && !hasPattern) return

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Vertical gradient wash — amplified so it's visually present
        if (hasGradient) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scaleAlpha(theme.gradientTop, 2.5f),
                                scaleAlpha(theme.gradientMid, 2.5f),
                                scaleAlpha(theme.gradientBottom, 2.5f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scaleAlpha(theme.gradientTop, 1.2f),
                                Color.Transparent
                            ),
                            radius = Float.MAX_VALUE
                        )
                    )
            )
        }

        // Layer 2: Pattern overlay — drawn shapes representing the theme
        if (hasPattern) {
            val fillColor = theme.primaryAccent.copy(alpha = alpha)
            val strokeColor = theme.primaryAccent.copy(alpha = alpha * 0.7f)
            val positions = remember(theme.id) {
                List(60) { Offset(Random.nextFloat(), Random.nextFloat()) }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                when (theme.backgroundPattern) {
                    BackgroundPattern.DOTS       -> drawDots(fillColor, positions)
                    BackgroundPattern.WAVES      -> drawWaves(strokeColor)
                    BackgroundPattern.STARS      -> drawStars(fillColor, strokeColor, positions)
                    BackgroundPattern.SNOWFLAKES -> drawSnowflakes(strokeColor, positions)
                    BackgroundPattern.HEARTS     -> drawHearts(fillColor, strokeColor, positions)
                    BackgroundPattern.LEAVES     -> drawLeaves(fillColor, strokeColor, positions)
                    BackgroundPattern.DIAMONDS   -> drawDiamonds(fillColor, strokeColor, positions)
                    BackgroundPattern.GRID       -> drawGrid(strokeColor)
                    BackgroundPattern.NONE       -> {}
                }
            }
        }
    }
}

/** Scale a color's alpha component by [factor], clamped to [0, 1]. */
private fun scaleAlpha(color: Color, factor: Float): Color {
    val newAlpha = (color.alpha * factor).coerceIn(0f, 1f)
    return color.copy(alpha = newAlpha)
}

// ── Pattern drawing functions ─────────────────────────────────────────────────

private fun DrawScope.drawDots(color: Color, positions: List<Offset>) {
    positions.forEach { p ->
        val radius = 5f + (p.x * 10f)
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(p.x * size.width, p.y * size.height)
        )
    }
}

private fun DrawScope.drawWaves(color: Color) {
    val waveCount = 8
    val waveHeight = size.height / waveCount
    for (i in 0 until waveCount) {
        val path = Path()
        val y = waveHeight * i + waveHeight * 0.5f
        path.moveTo(0f, y)
        var x = 0f
        while (x < size.width) {
            val cp1x = x + size.width * 0.1f
            val cp2x = x + size.width * 0.2f
            val endX = x + size.width * 0.25f
            val amplitude = waveHeight * 0.25f * (1f + (i % 3) * 0.3f)
            path.cubicTo(cp1x, y - amplitude, cp2x, y + amplitude, endX, y)
            x = endX
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawStars(fillColor: Color, strokeColor: Color, positions: List<Offset>) {
    positions.take(30).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val outerR = 7f + p.x * 14f
        val innerR = outerR * 0.42f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerR else innerR
            val angle = (i * 36.0 - 90.0) * PI / 180.0
            val px = cx + radius * cos(angle).toFloat()
            val py = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color = fillColor, style = Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawSnowflakes(color: Color, positions: List<Offset>) {
    positions.take(25).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val r = 8f + p.x * 16f
        for (i in 0 until 6) {
            val angle = (i * 60.0) * PI / 180.0
            val ex = cx + r * cos(angle).toFloat()
            val ey = cy + r * sin(angle).toFloat()
            drawLine(color, Offset(cx, cy), Offset(ex, ey), strokeWidth = 2f)
            val mx = cx + r * 0.55f * cos(angle).toFloat()
            val my = cy + r * 0.55f * sin(angle).toFloat()
            val branchAngle1 = angle + PI / 5
            val branchAngle2 = angle - PI / 5
            val br = r * 0.35f
            drawLine(
                color, Offset(mx, my),
                Offset(mx + br * cos(branchAngle1).toFloat(), my + br * sin(branchAngle1).toFloat()),
                strokeWidth = 1.2f
            )
            drawLine(
                color, Offset(mx, my),
                Offset(mx + br * cos(branchAngle2).toFloat(), my + br * sin(branchAngle2).toFloat()),
                strokeWidth = 1.2f
            )
        }
        drawCircle(color, radius = 2f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawHearts(fillColor: Color, strokeColor: Color, positions: List<Offset>) {
    positions.take(25).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val s = 8f + p.x * 16f
        val path = Path()
        path.moveTo(cx, cy + s * 0.9f)
        path.cubicTo(
            cx - s * 1.8f, cy - s * 0.2f,
            cx - s * 0.6f, cy - s * 1.6f,
            cx, cy - s * 0.4f
        )
        path.cubicTo(
            cx + s * 0.6f, cy - s * 1.6f,
            cx + s * 1.8f, cy - s * 0.2f,
            cx, cy + s * 0.9f
        )
        path.close()
        drawPath(path, color = fillColor, style = Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.5f))
    }
}

private fun DrawScope.drawLeaves(fillColor: Color, strokeColor: Color, positions: List<Offset>) {
    positions.take(25).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val s = 10f + p.x * 14f
        val path = Path()
        path.moveTo(cx, cy - s)
        path.quadraticTo(cx + s * 1.2f, cy, cx, cy + s)
        path.quadraticTo(cx - s * 1.2f, cy, cx, cy - s)
        path.close()
        drawPath(path, color = fillColor, style = Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.2f))
        drawLine(strokeColor, Offset(cx, cy - s * 0.8f), Offset(cx, cy + s * 0.8f), strokeWidth = 1f)
    }
}

private fun DrawScope.drawDiamonds(fillColor: Color, strokeColor: Color, positions: List<Offset>) {
    positions.take(25).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val r = 8f + p.x * 14f
        val path = Path()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r * 0.65f, cy)
        path.lineTo(cx, cy + r)
        path.lineTo(cx - r * 0.65f, cy)
        path.close()
        drawPath(path, color = fillColor, style = Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.5f))
    }
}

private fun DrawScope.drawGrid(color: Color) {
    val spacing = 36f
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += spacing
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += spacing
    }
}