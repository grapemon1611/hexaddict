package com.almostbrilliantideas.hexaddict.ui.components

import android.icu.util.TimeZone
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almostbrilliantideas.hexaddict.R
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

/**
 * Time of day states (4 total)
 */
enum class TimeOfDay {
    MORNING,    // 5am - 10am: warm pinks and golds, low sun, soft light
    MIDDAY,     // 10am - 4pm: bright blues, white clouds, full sun high
    EVENING,    // 4pm - 8pm: deep oranges, purples, warm horizon, sun low
    NIGHT       // 8pm - 5am: deep navy, stars, moon
}

/**
 * Season states (4 total)
 */
enum class Season {
    SPRING,     // Cherry blossom trees, soft greens, pastel sky
    SUMMER,     // Full green trees, vivid sky, warm
    AUTUMN,     // Orange and red foliage, amber tones
    WINTER      // Bare trees or snow-capped, cool blues, desaturated
}

/**
 * Get current time of day from device clock.
 */
fun getCurrentTimeOfDay(): TimeOfDay {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..9 -> TimeOfDay.MORNING
        in 10..15 -> TimeOfDay.MIDDAY
        in 16..19 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
}

/**
 * Get current season from device date, adjusting for hemisphere.
 * Northern hemisphere: Mar-May = Spring, Jun-Aug = Summer, Sep-Nov = Autumn, Dec-Feb = Winter
 * Southern hemisphere: inverted (Dec-Feb = Summer, etc.)
 */
fun getCurrentSeason(): Season {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH) // 0-indexed: Jan=0, Dec=11

    // Determine hemisphere from timezone
    val isSouthernHemisphere = isInSouthernHemisphere()

    // Northern hemisphere season mapping
    val northernSeason = when (month) {
        in 2..4 -> Season.SPRING      // Mar, Apr, May
        in 5..7 -> Season.SUMMER      // Jun, Jul, Aug
        in 8..10 -> Season.AUTUMN     // Sep, Oct, Nov
        else -> Season.WINTER          // Dec, Jan, Feb
    }

    // Invert for southern hemisphere
    return if (isSouthernHemisphere) {
        when (northernSeason) {
            Season.SPRING -> Season.AUTUMN
            Season.SUMMER -> Season.WINTER
            Season.AUTUMN -> Season.SPRING
            Season.WINTER -> Season.SUMMER
        }
    } else {
        northernSeason
    }
}

/**
 * Determine if device is in southern hemisphere based on timezone.
 * Uses timezone ID to infer location - no location permission needed.
 */
private fun isInSouthernHemisphere(): Boolean {
    val timezoneId = TimeZone.getDefault().id.lowercase()

    // Southern hemisphere regions by timezone
    val southernTimezones = listOf(
        "australia", "sydney", "melbourne", "perth", "brisbane", "adelaide", "hobart", "darwin",
        "new_zealand", "auckland", "wellington", "pacific/auckland",
        "argentina", "buenos_aires", "america/buenos_aires", "america/argentina",
        "brazil", "sao_paulo", "america/sao_paulo", "america/fortaleza", "america/recife",
        "chile", "santiago", "america/santiago",
        "south_africa", "johannesburg", "africa/johannesburg",
        "africa/harare", "africa/maputo", "africa/lusaka",
        "indonesia/east", "asia/jayapura",
        "pacific/fiji", "pacific/port_moresby", "pacific/noumea",
        "indian/madagascar", "indian/mauritius", "indian/reunion",
        "antarctica"
    )

    return southernTimezones.any { timezoneId.contains(it) }
}

// Color palettes for each time of day

object SkyColors {
    // Morning: warm pinks and golds
    val morningTop = Color(0xFFFFA07A)     // Light salmon
    val morningMid = Color(0xFFFFB6C1)     // Light pink
    val morningBottom = Color(0xFFFFE4B5)  // Moccasin/gold

    // Midday: bright blues
    val middayTop = Color(0xFF4A90D9)      // Sky blue
    val middayMid = Color(0xFF87CEEB)      // Light sky blue
    val middayBottom = Color(0xFFB0E0E6)   // Powder blue

    // Evening: deep oranges and purples
    val eveningTop = Color(0xFF4B0082)     // Indigo
    val eveningMid = Color(0xFFFF6347)     // Tomato/orange
    val eveningBottom = Color(0xFFFFD700)  // Gold

    // Night: deep navy
    val nightTop = Color(0xFF0D1B2A)       // Very dark blue
    val nightMid = Color(0xFF1B263B)       // Dark blue
    val nightBottom = Color(0xFF2E3A59)    // Navy
}

object SeasonColors {
    // Spring: soft greens, pastels
    val springTreeLight = Color(0xFFFFB7C5)   // Cherry blossom pink
    val springTreeDark = Color(0xFFE091A3)    // Darker pink
    val springGround = Color(0xFF90EE90)      // Light green
    val springGroundDark = Color(0xFF228B22)  // Forest green

    // Summer: vivid greens
    val summerTreeLight = Color(0xFF32CD32)   // Lime green
    val summerTreeDark = Color(0xFF228B22)    // Forest green
    val summerGround = Color(0xFF7CFC00)      // Lawn green
    val summerGroundDark = Color(0xFF006400)  // Dark green

    // Autumn: oranges and reds
    val autumnTreeLight = Color(0xFFFF8C00)   // Dark orange
    val autumnTreeDark = Color(0xFFB8860B)    // Dark goldenrod
    val autumnGround = Color(0xFFD2691E)      // Chocolate
    val autumnGroundDark = Color(0xFF8B4513)  // Saddle brown

    // Winter: cool blues, snow
    val winterTreeLight = Color(0xFF696969)   // Dim gray (bare branches)
    val winterTreeDark = Color(0xFF2F4F4F)    // Dark slate gray
    val winterGround = Color(0xFFF0F8FF)      // Alice blue (snow)
    val winterGroundDark = Color(0xFFB0C4DE)  // Light steel blue
}

// Hill colors by time of day (base, will be tinted by season)
object HillColors {
    val morningFar = Color(0xFF6B8E6B)
    val morningMid = Color(0xFF5A7D5A)
    val morningNear = Color(0xFF4A6C4A)

    val middayFar = Color(0xFF5D8A5D)
    val middayMid = Color(0xFF4A7A4A)
    val middayNear = Color(0xFF3A6A3A)

    val eveningFar = Color(0xFF6B5D5D)
    val eveningMid = Color(0xFF5A4D4D)
    val eveningNear = Color(0xFF4A3D3D)

    val nightFar = Color(0xFF2A3A4A)
    val nightMid = Color(0xFF1F2F3F)
    val nightNear = Color(0xFF152535)
}

/**
 * Scene zone boundaries for constrained rendering.
 * All values are fractions of total screen height (0.0 to 1.0).
 */
data class SceneZones(
    val skyZoneEnd: Float,      // Where sky zone ends (top of score bar area)
    val groundZoneStart: Float, // Where ground zone starts (bottom of board)
    val groundZoneEnd: Float    // Where ground zone ends (top of piece tray)
)

/**
 * The main atmospheric background scene composable.
 * Renders a layered 2D illustrated landscape based on current time and season.
 * Scene elements are constrained to sky zone (above board) and ground zone (below board).
 */
@Composable
fun BackgroundScene(
    modifier: Modifier = Modifier,
    skyZoneEndFraction: Float = 0.18f,      // ~18% down from top (above score bar)
    groundZoneStartFraction: Float = 0.75f,  // ~75% down (below board)
    groundZoneEndFraction: Float = 0.88f     // ~88% down (above piece tray)
) {
    val timeOfDay = remember { getCurrentTimeOfDay() }
    val season = remember { getCurrentSeason() }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val zones = SceneZones(
                skyZoneEnd = size.height * skyZoneEndFraction,
                groundZoneStart = size.height * groundZoneStartFraction,
                groundZoneEnd = size.height * groundZoneEndFraction
            )
            drawScene(timeOfDay, season, zones)
        }
    }
}

/**
 * Logo composable to be placed separately in the layout.
 */
@Composable
fun GameLogo(
    modifier: Modifier = Modifier
) {
    val timeOfDay = remember { getCurrentTimeOfDay() }

    Text(
        text = "Hex Addict",
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        color = getLogoColor(timeOfDay),
        fontFamily = oxaniumFontFamily
    )
}

/**
 * Get appropriate logo color for time of day.
 */
private fun getLogoColor(timeOfDay: TimeOfDay): Color {
    return when (timeOfDay) {
        TimeOfDay.MORNING -> Color(0xFFFFFFFF)
        TimeOfDay.MIDDAY -> Color(0xFFFFFFFF)
        TimeOfDay.EVENING -> Color(0xFFFFE4B5)
        TimeOfDay.NIGHT -> Color(0xFFE8E8FF)
    }
}

/**
 * Main scene drawing function with zone constraints.
 */
private fun DrawScope.drawScene(timeOfDay: TimeOfDay, season: Season, zones: SceneZones) {
    // 1. Draw full-screen sky gradient (background for everything)
    drawSkyGradient(timeOfDay, season)

    // 2. Draw sky zone elements (celestial bodies, clouds) - clipped to sky zone
    clipRect(
        left = 0f,
        top = 0f,
        right = size.width,
        bottom = zones.skyZoneEnd
    ) {
        drawCelestialBodies(timeOfDay, zones.skyZoneEnd)

        if (timeOfDay != TimeOfDay.NIGHT) {
            drawClouds(timeOfDay, zones.skyZoneEnd)
        }
    }

    // 3. Draw ground zone elements (hills, trees, ground) - clipped to ground zone
    clipRect(
        left = 0f,
        top = zones.groundZoneStart,
        right = size.width,
        bottom = zones.groundZoneEnd
    ) {
        drawDistantHills(timeOfDay, season, zones)
        drawTreeLine(timeOfDay, season, zones)
        drawGround(timeOfDay, season, zones)
    }
}

/**
 * Draw the sky gradient based on time of day and season tinting.
 */
private fun DrawScope.drawSkyGradient(timeOfDay: TimeOfDay, season: Season) {
    val (topColor, midColor, bottomColor) = when (timeOfDay) {
        TimeOfDay.MORNING -> Triple(
            tintForSeason(SkyColors.morningTop, season, 0.1f),
            tintForSeason(SkyColors.morningMid, season, 0.05f),
            tintForSeason(SkyColors.morningBottom, season, 0.05f)
        )
        TimeOfDay.MIDDAY -> Triple(
            tintForSeason(SkyColors.middayTop, season, 0.1f),
            tintForSeason(SkyColors.middayMid, season, 0.05f),
            tintForSeason(SkyColors.middayBottom, season, 0.05f)
        )
        TimeOfDay.EVENING -> Triple(
            tintForSeason(SkyColors.eveningTop, season, 0.1f),
            tintForSeason(SkyColors.eveningMid, season, 0.05f),
            tintForSeason(SkyColors.eveningBottom, season, 0.05f)
        )
        TimeOfDay.NIGHT -> Triple(
            SkyColors.nightTop,
            SkyColors.nightMid,
            SkyColors.nightBottom
        )
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(topColor, midColor, bottomColor),
        startY = 0f,
        endY = size.height
    )

    drawRect(brush = gradient, size = size)
}

/**
 * Apply subtle seasonal tinting to a color.
 */
private fun tintForSeason(base: Color, season: Season, intensity: Float): Color {
    val tint = when (season) {
        Season.SPRING -> Color(0xFFFFD0E0)  // Soft pink tint
        Season.SUMMER -> Color(0xFFFFF0A0)  // Warm yellow tint
        Season.AUTUMN -> Color(0xFFFFD080)  // Amber tint
        Season.WINTER -> Color(0xFFE0F0FF)  // Cool blue tint
    }

    return Color(
        red = base.red * (1 - intensity) + tint.red * intensity,
        green = base.green * (1 - intensity) + tint.green * intensity,
        blue = base.blue * (1 - intensity) + tint.blue * intensity,
        alpha = base.alpha
    )
}

/**
 * Draw sun, moon, or stars based on time of day - constrained to sky zone.
 */
private fun DrawScope.drawCelestialBodies(timeOfDay: TimeOfDay, skyZoneHeight: Float) {
    when (timeOfDay) {
        TimeOfDay.MORNING -> {
            // Low sun on the right, positioned within sky zone
            drawSun(
                center = Offset(size.width * 0.80f, skyZoneHeight * 0.65f),
                radius = skyZoneHeight * 0.35f,
                glowColor = Color(0x40FFD700)
            )
        }
        TimeOfDay.MIDDAY -> {
            // High sun in center-top
            drawSun(
                center = Offset(size.width * 0.5f, skyZoneHeight * 0.45f),
                radius = skyZoneHeight * 0.30f,
                glowColor = Color(0x30FFFFFF)
            )
        }
        TimeOfDay.EVENING -> {
            // Low sun on the left
            drawSun(
                center = Offset(size.width * 0.20f, skyZoneHeight * 0.70f),
                radius = skyZoneHeight * 0.38f,
                glowColor = Color(0x50FF6347)
            )
        }
        TimeOfDay.NIGHT -> {
            // Stars scattered in sky zone
            drawStars(skyZoneHeight)
            // Moon
            drawMoon(
                center = Offset(size.width * 0.75f, skyZoneHeight * 0.50f),
                radius = skyZoneHeight * 0.25f
            )
        }
    }
}

/**
 * Draw the sun with a soft glow.
 */
private fun DrawScope.drawSun(center: Offset, radius: Float, glowColor: Color) {
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = center,
            radius = radius * 2.5f
        ),
        radius = radius * 2.5f,
        center = center
    )

    // Sun body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFACD), Color(0xFFFFD700)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * Draw the moon with crescent shadow.
 */
private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    // Moon glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x30E8E8FF), Color.Transparent),
            center = center,
            radius = radius * 2f
        ),
        radius = radius * 2f,
        center = center
    )

    // Moon body
    drawCircle(
        color = Color(0xFFF0F0F0),
        radius = radius,
        center = center
    )

    // Crescent shadow (offset circle to create crescent effect)
    drawCircle(
        color = Color(0xFF1B263B),
        radius = radius * 0.85f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.1f)
    )
}

/**
 * Draw stars for night sky - constrained to sky zone.
 */
private fun DrawScope.drawStars(skyZoneHeight: Float) {
    // Deterministic "random" stars positioned within sky zone
    val starPositions = listOf(
        0.1f to 0.15f, 0.25f to 0.10f, 0.4f to 0.25f, 0.55f to 0.08f, 0.7f to 0.20f,
        0.85f to 0.12f, 0.95f to 0.30f, 0.05f to 0.40f, 0.2f to 0.50f, 0.35f to 0.35f,
        0.5f to 0.45f, 0.65f to 0.55f, 0.8f to 0.40f, 0.9f to 0.60f, 0.15f to 0.70f,
        0.3f to 0.65f, 0.45f to 0.75f, 0.6f to 0.68f, 0.75f to 0.80f, 0.88f to 0.72f
    )

    val starSizes = listOf(1.5f, 2f, 1f, 2.5f, 1.5f, 2f, 1f, 2f, 1.5f, 1f,
        2f, 1.5f, 2.5f, 1f, 2f, 1.5f, 1f, 2f, 1.5f, 2.5f)

    val starAlphas = listOf(0.9f, 0.7f, 0.8f, 0.6f, 0.9f, 0.7f, 0.8f, 0.6f, 0.9f, 0.7f,
        0.8f, 0.6f, 0.9f, 0.7f, 0.8f, 0.6f, 0.9f, 0.7f, 0.8f, 0.6f)

    starPositions.forEachIndexed { index, (xRatio, yRatio) ->
        drawCircle(
            color = Color.White.copy(alpha = starAlphas[index % starAlphas.size]),
            radius = starSizes[index % starSizes.size],
            center = Offset(size.width * xRatio, skyZoneHeight * yRatio)
        )
    }
}

/**
 * Draw clouds (simplified puffy shapes) - constrained to sky zone.
 */
private fun DrawScope.drawClouds(timeOfDay: TimeOfDay, skyZoneHeight: Float) {
    val cloudColor = when (timeOfDay) {
        TimeOfDay.MORNING -> Color(0xCCFFE4E1)  // Misty rose
        TimeOfDay.MIDDAY -> Color(0xCCFFFFFF)   // White
        TimeOfDay.EVENING -> Color(0xCCFFB6C1)  // Light pink
        TimeOfDay.NIGHT -> Color.Transparent    // No clouds
    }

    if (cloudColor == Color.Transparent) return

    // Cloud 1 - left side, within sky zone
    drawCloud(
        centerX = size.width * 0.15f,
        centerY = skyZoneHeight * 0.35f,
        scale = 0.6f,
        color = cloudColor,
        skyZoneHeight = skyZoneHeight
    )

    // Cloud 2 - right side
    drawCloud(
        centerX = size.width * 0.75f,
        centerY = skyZoneHeight * 0.50f,
        scale = 0.5f,
        color = cloudColor,
        skyZoneHeight = skyZoneHeight
    )

    // Cloud 3 - center-ish
    drawCloud(
        centerX = size.width * 0.45f,
        centerY = skyZoneHeight * 0.25f,
        scale = 0.4f,
        color = cloudColor,
        skyZoneHeight = skyZoneHeight
    )
}

/**
 * Draw a single cloud (cluster of overlapping circles) - sized relative to sky zone.
 */
private fun DrawScope.drawCloud(
    centerX: Float,
    centerY: Float,
    scale: Float,
    color: Color,
    skyZoneHeight: Float
) {
    val baseRadius = skyZoneHeight * 0.15f * scale

    // Multiple overlapping circles create cloud shape
    val circles = listOf(
        Offset(-baseRadius * 1.2f, 0f) to baseRadius * 0.9f,
        Offset(0f, -baseRadius * 0.3f) to baseRadius * 1.1f,
        Offset(baseRadius * 1.0f, 0f) to baseRadius * 0.85f,
        Offset(-baseRadius * 0.5f, baseRadius * 0.3f) to baseRadius * 0.7f,
        Offset(baseRadius * 0.6f, baseRadius * 0.25f) to baseRadius * 0.75f
    )

    circles.forEach { (offset, radius) ->
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX + offset.x, centerY + offset.y)
        )
    }
}

/**
 * Draw distant rolling hills/mountains - constrained to ground zone.
 */
private fun DrawScope.drawDistantHills(timeOfDay: TimeOfDay, season: Season, zones: SceneZones) {
    val (farColor, midColor, nearColor) = when (timeOfDay) {
        TimeOfDay.MORNING -> Triple(HillColors.morningFar, HillColors.morningMid, HillColors.morningNear)
        TimeOfDay.MIDDAY -> Triple(HillColors.middayFar, HillColors.middayMid, HillColors.middayNear)
        TimeOfDay.EVENING -> Triple(HillColors.eveningFar, HillColors.eveningMid, HillColors.eveningNear)
        TimeOfDay.NIGHT -> Triple(HillColors.nightFar, HillColors.nightMid, HillColors.nightNear)
    }

    // Apply seasonal tinting to hills
    val farTinted = tintForSeason(farColor, season, 0.2f)
    val midTinted = tintForSeason(midColor, season, 0.15f)
    val nearTinted = tintForSeason(nearColor, season, 0.1f)

    val zoneHeight = zones.groundZoneEnd - zones.groundZoneStart

    // Far hills (highest, most faded) - at top of ground zone
    drawHillLayer(
        baseY = zones.groundZoneStart + zoneHeight * 0.15f,
        amplitude = zoneHeight * 0.12f,
        color = farTinted,
        phaseOffset = 0f,
        bottomY = zones.groundZoneEnd
    )

    // Mid hills
    drawHillLayer(
        baseY = zones.groundZoneStart + zoneHeight * 0.35f,
        amplitude = zoneHeight * 0.15f,
        color = midTinted,
        phaseOffset = 1.5f,
        bottomY = zones.groundZoneEnd
    )

    // Near hills
    drawHillLayer(
        baseY = zones.groundZoneStart + zoneHeight * 0.50f,
        amplitude = zoneHeight * 0.10f,
        color = nearTinted,
        phaseOffset = 3f,
        bottomY = zones.groundZoneEnd
    )
}

/**
 * Draw a single hill layer using sine wave curve - constrained to zone.
 */
private fun DrawScope.drawHillLayer(
    baseY: Float,
    amplitude: Float,
    color: Color,
    phaseOffset: Float,
    bottomY: Float
) {
    val path = Path().apply {
        moveTo(0f, bottomY)

        // Create smooth rolling hills using sine waves
        val steps = 100
        for (i in 0..steps) {
            val x = size.width * i / steps
            val wave1 = sin((x / size.width * 2 * PI + phaseOffset).toFloat()) * amplitude * 0.6f
            val wave2 = sin((x / size.width * 4 * PI + phaseOffset * 1.3).toFloat()) * amplitude * 0.4f
            val y = baseY - wave1 - wave2

            if (i == 0) {
                lineTo(0f, y)
            } else {
                lineTo(x, y)
            }
        }

        lineTo(size.width, bottomY)
        close()
    }

    drawPath(path, color = color, style = Fill)
}

/**
 * Draw the tree line in front of hills - constrained to ground zone.
 */
private fun DrawScope.drawTreeLine(timeOfDay: TimeOfDay, season: Season, zones: SceneZones) {
    val (lightColor, darkColor) = when (season) {
        Season.SPRING -> SeasonColors.springTreeLight to SeasonColors.springTreeDark
        Season.SUMMER -> SeasonColors.summerTreeLight to SeasonColors.summerTreeDark
        Season.AUTUMN -> SeasonColors.autumnTreeLight to SeasonColors.autumnTreeDark
        Season.WINTER -> SeasonColors.winterTreeLight to SeasonColors.winterTreeDark
    }

    // Darken trees for evening/night
    val timeDarkening = when (timeOfDay) {
        TimeOfDay.MORNING -> 0.1f
        TimeOfDay.MIDDAY -> 0f
        TimeOfDay.EVENING -> 0.25f
        TimeOfDay.NIGHT -> 0.5f
    }

    val adjustedLight = darkenColor(lightColor, timeDarkening)
    val adjustedDark = darkenColor(darkColor, timeDarkening)

    val zoneHeight = zones.groundZoneEnd - zones.groundZoneStart

    // Draw row of stylized trees
    val treeCount = 10
    val baseY = zones.groundZoneStart + zoneHeight * 0.65f

    for (i in 0 until treeCount) {
        val x = size.width * (i + 0.5f) / treeCount
        val heightVariation = ((i * 7 + 3) % 5) / 10f  // Pseudo-random height variation
        val treeHeight = zoneHeight * (0.50f + heightVariation * 0.15f)
        val treeWidth = size.width * 0.06f

        if (season == Season.WINTER) {
            // Bare winter trees
            drawBareTree(
                baseX = x,
                baseY = baseY,
                height = treeHeight,
                color = adjustedDark
            )
        } else {
            // Full trees with foliage
            drawTree(
                baseX = x,
                baseY = baseY,
                height = treeHeight,
                width = treeWidth,
                lightColor = adjustedLight,
                darkColor = adjustedDark
            )
        }
    }
}

/**
 * Draw a stylized tree with foliage.
 */
private fun DrawScope.drawTree(
    baseX: Float,
    baseY: Float,
    height: Float,
    width: Float,
    lightColor: Color,
    darkColor: Color
) {
    // Tree trunk
    val trunkWidth = width * 0.15f
    val trunkHeight = height * 0.3f

    drawRect(
        color = Color(0xFF4A3728),
        topLeft = Offset(baseX - trunkWidth / 2, baseY - trunkHeight),
        size = Size(trunkWidth, trunkHeight)
    )

    // Foliage (layered triangles or circles for rounded look)
    val foliageY = baseY - trunkHeight * 0.7f

    // Back layer (darker)
    drawOval(
        color = darkColor,
        topLeft = Offset(baseX - width * 0.45f, foliageY - height * 0.7f),
        size = Size(width * 0.9f, height * 0.75f)
    )

    // Front layer (lighter)
    drawOval(
        color = lightColor,
        topLeft = Offset(baseX - width * 0.35f, foliageY - height * 0.6f),
        size = Size(width * 0.7f, height * 0.55f)
    )
}

/**
 * Draw a bare winter tree (just trunk and branches).
 */
private fun DrawScope.drawBareTree(baseX: Float, baseY: Float, height: Float, color: Color) {
    val trunkWidth = height * 0.08f

    // Main trunk
    val path = Path().apply {
        moveTo(baseX - trunkWidth / 2, baseY)
        lineTo(baseX - trunkWidth / 4, baseY - height)
        lineTo(baseX + trunkWidth / 4, baseY - height)
        lineTo(baseX + trunkWidth / 2, baseY)
        close()
    }
    drawPath(path, color = color)

    // Simple branches
    val branchY1 = baseY - height * 0.6f
    val branchY2 = baseY - height * 0.8f

    // Left branches
    drawLine(
        color = color,
        start = Offset(baseX, branchY1),
        end = Offset(baseX - height * 0.25f, branchY1 - height * 0.15f),
        strokeWidth = trunkWidth * 0.4f
    )
    drawLine(
        color = color,
        start = Offset(baseX, branchY2),
        end = Offset(baseX - height * 0.18f, branchY2 - height * 0.12f),
        strokeWidth = trunkWidth * 0.3f
    )

    // Right branches
    drawLine(
        color = color,
        start = Offset(baseX, branchY1 + height * 0.05f),
        end = Offset(baseX + height * 0.22f, branchY1 - height * 0.1f),
        strokeWidth = trunkWidth * 0.4f
    )
    drawLine(
        color = color,
        start = Offset(baseX, branchY2 - height * 0.03f),
        end = Offset(baseX + height * 0.15f, branchY2 - height * 0.15f),
        strokeWidth = trunkWidth * 0.3f
    )
}

/**
 * Draw the ground/foreground - constrained to ground zone.
 */
private fun DrawScope.drawGround(timeOfDay: TimeOfDay, season: Season, zones: SceneZones) {
    val (groundLight, groundDark) = when (season) {
        Season.SPRING -> SeasonColors.springGround to SeasonColors.springGroundDark
        Season.SUMMER -> SeasonColors.summerGround to SeasonColors.summerGroundDark
        Season.AUTUMN -> SeasonColors.autumnGround to SeasonColors.autumnGroundDark
        Season.WINTER -> SeasonColors.winterGround to SeasonColors.winterGroundDark
    }

    // Darken for time of day
    val timeDarkening = when (timeOfDay) {
        TimeOfDay.MORNING -> 0.1f
        TimeOfDay.MIDDAY -> 0f
        TimeOfDay.EVENING -> 0.2f
        TimeOfDay.NIGHT -> 0.4f
    }

    val adjustedLight = darkenColor(groundLight, timeDarkening)
    val adjustedDark = darkenColor(groundDark, timeDarkening)

    val zoneHeight = zones.groundZoneEnd - zones.groundZoneStart
    val groundTop = zones.groundZoneStart + zoneHeight * 0.70f

    // Ground gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(adjustedDark, adjustedLight),
            startY = groundTop,
            endY = zones.groundZoneEnd
        ),
        topLeft = Offset(0f, groundTop),
        size = Size(size.width, zones.groundZoneEnd - groundTop)
    )

    // Add some grass tufts or snow drifts
    if (season == Season.WINTER) {
        drawSnowDrifts(groundTop, zones.groundZoneEnd)
    } else {
        drawGrassTufts(groundTop, zones.groundZoneEnd, adjustedDark)
    }
}

/**
 * Draw simple grass tufts - constrained to ground zone.
 */
private fun DrawScope.drawGrassTufts(groundTop: Float, groundBottom: Float, grassColor: Color) {
    val zoneHeight = groundBottom - groundTop
    val tuftCount = 15
    for (i in 0 until tuftCount) {
        val x = size.width * (i + 0.5f) / tuftCount
        val y = groundTop + ((i * 13) % 5) * (zoneHeight * 0.08f)

        // Small triangle tuft
        val path = Path().apply {
            moveTo(x - 3f, y + 6f)
            lineTo(x, y)
            lineTo(x + 3f, y + 6f)
            close()
        }
        drawPath(path, color = grassColor)
    }
}

/**
 * Draw snow drifts for winter - constrained to ground zone.
 */
private fun DrawScope.drawSnowDrifts(groundTop: Float, groundBottom: Float) {
    val driftColor = Color(0xE0FFFFFF)
    val zoneHeight = groundBottom - groundTop

    for (i in 0..4) {
        val centerX = size.width * (0.1f + i * 0.2f)
        val width = size.width * 0.12f
        val driftY = groundTop + zoneHeight * 0.15f

        drawOval(
            color = driftColor,
            topLeft = Offset(centerX - width / 2, driftY),
            size = Size(width, zoneHeight * 0.20f)
        )
    }
}

/**
 * Darken a color by a given factor.
 */
private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = color.red * (1 - factor),
        green = color.green * (1 - factor),
        blue = color.blue * (1 - factor),
        alpha = color.alpha
    )
}

// Google Fonts provider for Oxanium
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val oxaniumFont = GoogleFont("Oxanium")

// Oxanium font family - loaded from Google Fonts with Bold (700) weight
val oxaniumFontFamily = FontFamily(
    Font(
        googleFont = oxaniumFont,
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    )
)
