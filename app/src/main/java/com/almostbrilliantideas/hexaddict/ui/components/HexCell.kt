package com.almostbrilliantideas.hexaddict.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draw a single hexagon (pointy-top orientation).
 */
fun DrawScope.drawHexagon(
    center: Offset,
    size: Float,
    fillColor: Color?,
    strokeColor: Color = Color(0xFF4A4A6A),
    strokeWidth: Float = 1.5f,
    gradientLight: Color? = null,
    gradientDark: Color? = null
) {
    val path = createHexagonPath(center, size)

    // Fill with gradient if colors provided, otherwise solid color
    if (fillColor != null) {
        if (gradientLight != null && gradientDark != null) {
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(gradientLight, gradientDark),
                    start = Offset(center.x - size, center.y - size),
                    end = Offset(center.x + size, center.y + size)
                ),
                style = Fill
            )
        } else {
            drawPath(
                path = path,
                color = fillColor,
                style = Fill
            )
        }
    }

    // Stroke
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Create a hexagon path (pointy-top orientation).
 * Angle starts at -90 degrees (top vertex).
 */
fun createHexagonPath(center: Offset, size: Float): Path {
    val path = Path()
    val angleOffset = -Math.PI / 2  // Start at top (pointy-top)

    for (i in 0 until 6) {
        val angle = angleOffset + i * Math.PI / 3
        val x = center.x + size * cos(angle).toFloat()
        val y = center.y + size * sin(angle).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    return path
}

/**
 * Check if a point is inside a hexagon.
 */
fun isPointInHexagon(point: Offset, center: Offset, size: Float): Boolean {
    val dx = kotlin.math.abs(point.x - center.x)
    val dy = kotlin.math.abs(point.y - center.y)

    // Quick bounding box check
    val hexWidth = size * kotlin.math.sqrt(3.0).toFloat()
    val hexHeight = size * 2

    if (dx > hexWidth / 2 || dy > hexHeight / 2) return false

    // More precise hexagon check
    return dy <= hexHeight / 2 - (hexHeight / 4) * (dx / (hexWidth / 2))
}
