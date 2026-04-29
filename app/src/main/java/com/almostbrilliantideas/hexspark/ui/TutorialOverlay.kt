package com.almostbrilliantideas.hexspark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click to prevent dismissing when tapping card */ },
            color = Color(0xFF252540),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "HOW TO PLAY",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Line clearing
                TutorialSection(
                    title = "Clear Lines",
                    description = "Fill any complete line on any axis to clear it."
                ) {
                    LineClearDiagram()
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Color bonus
                TutorialSection(
                    title = "Color Bonus",
                    description = "Same-color line = 2x points.\nTwo or more = PAYDAY 3x!"
                ) {
                    ColorBonusDiagram()
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Jackpot
                TutorialSection(
                    title = "JACKPOT",
                    description = "Clear all 3 axes at once with a color match — board clears, everything scores 5x!"
                ) {
                    JackpotDiagram()
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Got it button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B84D4)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Got it!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialSection(
    title: String,
    description: String,
    diagram: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1A1A2E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B84D4)
        )

        Spacer(modifier = Modifier.height(8.dp))

        diagram()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB8B4C4),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LineClearDiagram() {
    Canvas(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
    ) {
        val hexSize = 18f
        val spacing = hexSize * sqrt(3f)
        val startX = center.x - (spacing * 2.5f)
        val y = center.y

        // Draw 5 hexes in a row - showing a completed line
        for (i in 0 until 5) {
            val x = startX + (i * spacing)
            drawHex(
                center = Offset(x, y),
                size = hexSize,
                fillColor = Color(0xFF4DB893), // Teal - filled
                strokeColor = Color(0xFF6FCFAA)
            )
        }

        // Arrow indicating clear
        val arrowX = startX + (5.5f * spacing)
        drawLine(
            color = Color.White,
            start = Offset(arrowX - 10, y),
            end = Offset(arrowX + 15, y),
            strokeWidth = 2f
        )
        // Arrowhead
        drawLine(
            color = Color.White,
            start = Offset(arrowX + 15, y),
            end = Offset(arrowX + 8, y - 6),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.White,
            start = Offset(arrowX + 15, y),
            end = Offset(arrowX + 8, y + 6),
            strokeWidth = 2f
        )

        // Sparkle/clear indicator
        val sparkleX = arrowX + 35
        drawCircle(
            color = Color(0xFF4DB893).copy(alpha = 0.6f),
            radius = 8f,
            center = Offset(sparkleX, y)
        )
    }
}

@Composable
private fun ColorBonusDiagram() {
    Row(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 2x example - single color line
        Canvas(modifier = Modifier.size(80.dp, 50.dp)) {
            val hexSize = 14f
            val spacing = hexSize * sqrt(3f)
            val startX = 10f
            val y = center.y

            for (i in 0 until 3) {
                drawHex(
                    center = Offset(startX + (i * spacing), y),
                    size = hexSize,
                    fillColor = Color(0xFF8B84D4), // All purple
                    strokeColor = Color(0xFFA8A0E0),
                    glowColor = Color(0xFF8B84D4).copy(alpha = 0.4f)
                )
            }
        }

        Text(
            text = "2x",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B84D4)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // PAYDAY example - two color lines
        Canvas(modifier = Modifier.size(80.dp, 50.dp)) {
            val hexSize = 12f
            val spacing = hexSize * sqrt(3f)
            val startX = 10f

            // First line
            for (i in 0 until 3) {
                drawHex(
                    center = Offset(startX + (i * spacing), center.y - 12),
                    size = hexSize,
                    fillColor = Color(0xFF4DB893), // Teal
                    strokeColor = Color(0xFF6FCFAA),
                    glowColor = Color(0xFF4DB893).copy(alpha = 0.4f)
                )
            }
            // Second line
            for (i in 0 until 3) {
                drawHex(
                    center = Offset(startX + (i * spacing), center.y + 12),
                    size = hexSize,
                    fillColor = Color(0xFF4DB893), // Teal
                    strokeColor = Color(0xFF6FCFAA),
                    glowColor = Color(0xFF4DB893).copy(alpha = 0.4f)
                )
            }
        }

        Text(
            text = "3x",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4DB893)
        )
    }
}

@Composable
private fun JackpotDiagram() {
    Canvas(
        modifier = Modifier
            .height(70.dp)
            .width(120.dp)
    ) {
        val hexSize = 12f
        val sqrt3 = sqrt(3f)

        // Draw a small hex cluster showing 3-axis intersection
        val centerX = center.x
        val centerY = center.y

        // Center hex (intersection point) - gold/jackpot color
        drawHex(
            center = Offset(centerX, centerY),
            size = hexSize,
            fillColor = Color(0xFFFFD700),
            strokeColor = Color(0xFFFFE55C),
            glowColor = Color(0xFFFFD700).copy(alpha = 0.5f)
        )

        // Surrounding hexes showing 3 axes
        val directions = listOf(
            Offset(sqrt3 * hexSize, 0f),           // Right (horizontal)
            Offset(-sqrt3 * hexSize, 0f),          // Left (horizontal)
            Offset(sqrt3 * hexSize * 0.5f, hexSize * 1.5f),   // Bottom-right (diag)
            Offset(-sqrt3 * hexSize * 0.5f, -hexSize * 1.5f), // Top-left (diag)
            Offset(-sqrt3 * hexSize * 0.5f, hexSize * 1.5f),  // Bottom-left (diag)
            Offset(sqrt3 * hexSize * 0.5f, -hexSize * 1.5f),  // Top-right (diag)
        )

        val colors = listOf(
            Color(0xFFFFD700), Color(0xFFFFD700),  // Horizontal - gold
            Color(0xFF8B84D4), Color(0xFF8B84D4),  // Diag R - purple
            Color(0xFF4DB893), Color(0xFF4DB893),  // Diag L - teal
        )

        for ((i, dir) in directions.withIndex()) {
            drawHex(
                center = Offset(centerX + dir.x, centerY + dir.y),
                size = hexSize,
                fillColor = colors[i],
                strokeColor = colors[i].copy(alpha = 0.8f),
                glowColor = colors[i].copy(alpha = 0.3f)
            )
        }

        // "5x" text position indicator
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 4f,
            center = Offset(centerX + 50, centerY)
        )
    }

    Text(
        text = "5x",
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFFD700)
    )
}

private fun DrawScope.drawHex(
    center: Offset,
    size: Float,
    fillColor: Color,
    strokeColor: Color,
    glowColor: Color? = null
) {
    val path = Path()
    val angles = (0 until 6).map { i ->
        val angleDeg = 60f * i - 30f
        val angleRad = (PI / 180f) * angleDeg
        Offset(
            center.x + size * cos(angleRad).toFloat(),
            center.y + size * sin(angleRad).toFloat()
        )
    }

    path.moveTo(angles[0].x, angles[0].y)
    for (i in 1 until 6) {
        path.lineTo(angles[i].x, angles[i].y)
    }
    path.close()

    // Glow effect
    if (glowColor != null) {
        drawCircle(
            color = glowColor,
            radius = size * 1.3f,
            center = center
        )
    }

    // Fill
    drawPath(path, fillColor, style = Fill)

    // Stroke
    drawPath(path, strokeColor, style = Stroke(width = 1.5f))
}
