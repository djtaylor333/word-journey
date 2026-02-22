package com.djtaylor.wordjourney.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.djtaylor.wordjourney.domain.model.BackgroundPattern
import com.djtaylor.wordjourney.domain.model.GameTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Subtle decorative pattern overlay matching the current theme.
 * Drawn at very low opacity so it doesn't distract from gameplay.
 */
@Composable
fun ThemeBackgroundOverlay(
    theme: GameTheme,
    modifier: Modifier = Modifier,
    alpha: Float = 0.06f
) {
    if (theme.backgroundPattern == BackgroundPattern.NONE) return

    val color = theme.primaryAccent.copy(alpha = alpha)
    // Pre-compute stable random positions per theme id
    val positions = remember(theme.id) {
        List(40) { Offset(Random.nextFloat(), Random.nextFloat()) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        when (theme.backgroundPattern) {
            BackgroundPattern.DOTS -> drawDots(color, positions)
            BackgroundPattern.WAVES -> drawWaves(color)
            BackgroundPattern.STARS -> drawStars(color, positions)
            BackgroundPattern.SNOWFLAKES -> drawSnowflakes(color, positions)
            BackgroundPattern.HEARTS -> drawHearts(color, positions)
            BackgroundPattern.LEAVES -> drawLeaves(color, positions)
            BackgroundPattern.DIAMONDS -> drawDiamonds(color, positions)
            BackgroundPattern.GRID -> drawGrid(color)
            BackgroundPattern.NONE -> {}
        }
    }
}

private fun DrawScope.drawDots(color: Color, positions: List<Offset>) {
    positions.forEach { p ->
        val radius = 3f + (p.x * 6f)
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(p.x * size.width, p.y * size.height)
        )
    }
}

private fun DrawScope.drawWaves(color: Color) {
    val waveCount = 6
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
            val amplitude = waveHeight * 0.15f * (1f + (i % 3) * 0.3f)
            path.cubicTo(cp1x, y - amplitude, cp2x, y + amplitude, endX, y)
            x = endX
        }
        drawPath(path, color = color, style = Stroke(width = 1.5f))
    }
}

private fun DrawScope.drawStars(color: Color, positions: List<Offset>) {
    positions.take(25).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val r = 4f + p.x * 8f
        val path = Path()
        for (i in 0 until 5) {
            val angle = (i * 144.0 - 90.0) * PI / 180.0
            val px = cx + r * cos(angle).toFloat()
            val py = cy + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color = color, style = Stroke(width = 1.2f))
    }
}

private fun DrawScope.drawSnowflakes(color: Color, positions: List<Offset>) {
    positions.take(20).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val r = 5f + p.x * 10f
        for (i in 0 until 6) {
            val angle = (i * 60.0) * PI / 180.0
            val ex = cx + r * cos(angle).toFloat()
            val ey = cy + r * sin(angle).toFloat()
            drawLine(color, Offset(cx, cy), Offset(ex, ey), strokeWidth = 1f)
            // Small branches
            val mx = cx + r * 0.6f * cos(angle).toFloat()
            val my = cy + r * 0.6f * sin(angle).toFloat()
            val branchAngle1 = angle + PI / 6
            val branchAngle2 = angle - PI / 6
            val br = r * 0.3f
            drawLine(color, Offset(mx, my),
                Offset(mx + br * cos(branchAngle1).toFloat(), my + br * sin(branchAngle1).toFloat()),
                strokeWidth = 0.8f)
            drawLine(color, Offset(mx, my),
                Offset(mx + br * cos(branchAngle2).toFloat(), my + br * sin(branchAngle2).toFloat()),
                strokeWidth = 0.8f)
        }
    }
}

private fun DrawScope.drawHearts(color: Color, positions: List<Offset>) {
    positions.take(20).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val s = 4f + p.x * 8f
        val path = Path()
        path.moveTo(cx, cy + s)
        path.cubicTo(cx - s * 1.5f, cy - s * 0.5f, cx - s * 0.5f, cy - s * 1.5f, cx, cy - s * 0.5f)
        path.cubicTo(cx + s * 0.5f, cy - s * 1.5f, cx + s * 1.5f, cy - s * 0.5f, cx, cy + s)
        drawPath(path, color = color, style = Stroke(width = 1f))
    }
}

private fun DrawScope.drawLeaves(color: Color, positions: List<Offset>) {
    positions.take(20).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val s = 6f + p.x * 10f
        val path = Path()
        path.moveTo(cx, cy - s)
        path.quadraticTo(cx + s, cy, cx, cy + s)
        path.quadraticTo(cx - s, cy, cx, cy - s)
        drawPath(path, color = color, style = Stroke(width = 1f))
        // Vein line
        drawLine(color, Offset(cx, cy - s * 0.8f), Offset(cx, cy + s * 0.8f), strokeWidth = 0.6f)
    }
}

private fun DrawScope.drawDiamonds(color: Color, positions: List<Offset>) {
    positions.take(20).forEach { p ->
        val cx = p.x * size.width
        val cy = p.y * size.height
        val r = 5f + p.x * 8f
        val path = Path()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r * 0.7f, cy)
        path.lineTo(cx, cy + r)
        path.lineTo(cx - r * 0.7f, cy)
        path.close()
        drawPath(path, color = color, style = Stroke(width = 1f))
    }
}

private fun DrawScope.drawGrid(color: Color) {
    val spacing = 40f
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
        x += spacing
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
        y += spacing
    }
}
