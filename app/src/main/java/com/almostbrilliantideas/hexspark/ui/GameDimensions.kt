package com.almostbrilliantideas.hexspark.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

/**
 * Holds all scaled dimensions for the game UI.
 * Calculated based on available screen real estate to ensure the game
 * looks good on devices from 5" phones to 10" tablets.
 *
 * The key insight: calculate the optimal hex size FIRST based on the limiting
 * constraint (width or height), then derive all other dimensions from that.
 */
data class GameDimensions(
    // The calculated optimal hex size for the board (in dp)
    val boardHexSize: Dp,

    // Screen padding
    val screenPaddingHorizontal: Dp,
    val screenPaddingTop: Dp,
    val screenPaddingBottom: Dp,

    // Logo
    val logoFontSize: TextUnit,
    val logoPaddingVertical: Dp,
    val logoHeight: Dp,  // Approximate total height including padding

    // Score display
    val scoreBarPaddingHorizontal: Dp,
    val scoreBarPaddingVertical: Dp,
    val scoreLabelFontSize: TextUnit,
    val scoreValueFontSize: TextUnit,
    val scoreBarCornerRadius: Dp,
    val scoreBarHeight: Dp,  // Approximate total height including padding

    // Spacers
    val spacerAfterScoreBar: Dp,
    val spacerAfterBoard: Dp,

    // Board
    val boardPadding: Dp,
    val boardCornerRadius: Dp,
    val boardWidth: Dp,   // Calculated from hex size
    val boardHeight: Dp,  // Calculated from hex size

    // Piece tray - scales proportionally to board hex size
    val pieceTrayHeight: Dp,
    val pieceTrayPaddingHorizontal: Dp,
    val pieceSlotPadding: Dp,
    val pieceSlotCornerRadius: Dp,
    val piecePreviewCanvasSize: Dp,
    val piecePreviewHexSize: Float,  // Proportional to board hex size

    // Dragged piece - slightly larger than preview
    val draggedPieceCanvasSize: Dp,
    val draggedPieceHexSize: Float,

    // Buttons and overlays
    val buttonPaddingHorizontal: Dp,
    val buttonPaddingVertical: Dp,
    val buttonFontSize: TextUnit,
    val overlayTitleFontSize: TextUnit,
    val overlaySubtitleFontSize: TextUnit,
    val overlayPadding: Dp,

    // Settings gear icon
    val settingsIconSize: Dp,
    val settingsIconFontSize: TextUnit,

    // Bonus text overlay
    val bonusTextFontSize: TextUnit,

    // Score preview
    val scorePreviewFontSize: TextUnit,
    val scorePreviewPadding: Dp
)

/**
 * Calculate game dimensions based on current screen configuration.
 */
@Composable
fun rememberGameDimensions(): GameDimensions {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        calculateGameDimensions(
            screenWidthDp = configuration.screenWidthDp.toFloat(),
            screenHeightDp = configuration.screenHeightDp.toFloat()
        )
    }
}

/**
 * Core calculation function for game dimensions.
 *
 * Algorithm:
 * 1. Estimate fixed UI element heights (logo, score bar, spacers, piece tray)
 * 2. Calculate available space for the board
 * 3. Determine optimal hex size from BOTH width and height constraints
 * 4. Use the smaller hex size (the limiting constraint)
 * 5. Scale piece tray proportionally to the board hex size
 */
private fun calculateGameDimensions(
    screenWidthDp: Float,
    screenHeightDp: Float
): GameDimensions {
    val sqrt3 = sqrt(3.0).toFloat()

    // ========== STEP 1: Calculate base scale for UI elements ==========
    // Reference: standard phone ~390dp wide
    val referenceWidth = 390f
    val baseScale = (screenWidthDp / referenceWidth).coerceIn(0.85f, 1.6f)

    // ========== STEP 2: Calculate fixed UI element dimensions ==========

    // Screen padding — kept slim to maximise board area
    val screenPaddingHorizontal = (16f * baseScale).coerceIn(12f, 24f)
    val screenPaddingTop = (8f * baseScale).coerceIn(6f, 12f)
    val screenPaddingBottom = (8f * baseScale).coerceIn(6f, 12f)

    // Logo dimensions — compact header treatment
    val logoFontSize = (20f * baseScale).coerceIn(16f, 26f)
    val logoPaddingVertical = (3f * baseScale).coerceIn(2f, 5f)
    val logoHeight = logoFontSize * 1.3f + logoPaddingVertical * 2  // Approximate

    // Score bar dimensions — slim, readable
    val scoreBarPaddingVertical = (3f * baseScale).coerceIn(2f, 6f)
    val scoreValueFontSize = (22f * baseScale).coerceIn(18f, 30f)
    val scoreLabelFontSize = (11f * baseScale).coerceIn(9f, 14f)
    val scoreBarHeight = scoreValueFontSize * 1.3f + scoreLabelFontSize * 1.2f + scoreBarPaddingVertical * 2

    // Spacers — tight to push more space to the board
    val spacerAfterScoreBar = (3f * baseScale).coerceAtLeast(2f)
    val spacerAfterBoard = 8f  // Minimum 8dp between board and tray

    // Board padding
    val boardPadding = (6f * baseScale).coerceAtLeast(6f)

    // ========== STEP 3: Calculate available space for board ==========

    // Available width for board content (inside padding)
    val availableWidthForBoard = screenWidthDp - (screenPaddingHorizontal * 2) - (boardPadding * 2)

    // Estimate piece tray height (will be refined after hex size is known)
    // For now, use a reasonable estimate based on screen size
    val estimatedTrayHeight = (screenHeightDp * 0.10f).coerceIn(55f, 130f)

    // Available height for board content (inside padding)
    val availableHeightForBoard = screenHeightDp -
            screenPaddingTop -
            screenPaddingBottom -
            logoHeight -
            scoreBarHeight -
            spacerAfterScoreBar -
            spacerAfterBoard -
            estimatedTrayHeight -
            (boardPadding * 2)

    // ========== STEP 4: Calculate optimal hex size from constraints ==========

    // Board geometry:
    // - 9 columns, but offset rows mean effective width is ~9.5 hex widths
    // - Hex width = size * sqrt(3)
    // - Total board width = size * sqrt(3) * 9.5
    //
    // - 9 rows with vertical spacing
    // - Hex vertical spacing = size * 1.5 between row centers
    // - Total board height = size * 1.5 * 8 + size * 2 (for top/bottom of hexes)

    // Max hex size that fits width
    val maxHexFromWidth = availableWidthForBoard / (sqrt3 * 9.5f)

    // Max hex size that fits height
    val maxHexFromHeight = availableHeightForBoard / (1.5f * 8f + 2f)

    // Use the SMALLER of the two (the limiting constraint)
    val optimalHexSize = minOf(maxHexFromWidth, maxHexFromHeight) * 0.95f  // 5% margin

    // Ensure minimum playable hex size
    val boardHexSize = optimalHexSize.coerceAtLeast(18f)

    // ========== STEP 5: Calculate actual board dimensions ==========

    val actualBoardWidth = boardHexSize * sqrt3 * 9f + boardPadding * 2
    val actualBoardHeight = boardHexSize * (1.5f * 8f + 2f) + boardPadding * 2

    // ========== STEP 6: Scale piece tray proportionally to hex size ==========

    // Piece tray hexes should be proportional to board hexes
    // Typical ratio: tray hex is about 50-60% of board hex size
    val pieceHexRatio = 0.55f
    val piecePreviewHexSize = boardHexSize * pieceHexRatio

    // Canvas needs to fit a 3-cell piece with some margin
    // 3-cell piece spans roughly 2 hex widths
    val piecePreviewCanvasSize = piecePreviewHexSize * sqrt3 * 2.5f

    // Piece tray height based on piece preview size
    val pieceTrayHeight = piecePreviewCanvasSize * 1.3f  // Add some padding around pieces

    // Dragged piece is slightly larger for visibility
    val draggedPieceHexSize = piecePreviewHexSize * 1.3f
    val draggedPieceCanvasSize = draggedPieceHexSize * sqrt3 * 2.5f

    // ========== STEP 7: Calculate remaining UI dimensions ==========

    val scoreBarPaddingHorizontal = (16f * baseScale).coerceIn(12f, 24f)
    val scoreBarCornerRadius = (12f * baseScale).coerceIn(8f, 18f)
    val boardCornerRadius = (16f * baseScale).coerceIn(12f, 22f)

    val pieceTrayPaddingHorizontal = (16f * baseScale).coerceIn(8f, 24f)
    val pieceSlotPadding = (8f * baseScale).coerceIn(4f, 12f)
    val pieceSlotCornerRadius = (12f * baseScale).coerceIn(8f, 16f)

    // Overlay and button dimensions
    val buttonFontSize = (14f * baseScale).coerceIn(12f, 18f)
    val overlayTitleFontSize = (32f * baseScale).coerceIn(24f, 44f)
    val overlaySubtitleFontSize = (22f * baseScale).coerceIn(16f, 30f)
    val overlayPadding = (32f * baseScale).coerceIn(24f, 44f)

    val settingsIconSize = (36f * baseScale).coerceIn(28f, 48f)
    val settingsIconFontSize = (18f * baseScale).coerceIn(14f, 24f)

    val bonusTextFontSize = (64f * baseScale).coerceIn(48f, 88f)
    val scorePreviewFontSize = (16f * baseScale).coerceIn(12f, 22f)
    val scorePreviewPadding = (6f * baseScale).coerceIn(4f, 10f)

    return GameDimensions(
        boardHexSize = boardHexSize.dp,

        screenPaddingHorizontal = screenPaddingHorizontal.dp,
        screenPaddingTop = screenPaddingTop.dp,
        screenPaddingBottom = screenPaddingBottom.dp,

        logoFontSize = logoFontSize.sp,
        logoPaddingVertical = logoPaddingVertical.dp,
        logoHeight = logoHeight.dp,

        scoreBarPaddingHorizontal = scoreBarPaddingHorizontal.dp,
        scoreBarPaddingVertical = scoreBarPaddingVertical.dp,
        scoreLabelFontSize = scoreLabelFontSize.sp,
        scoreValueFontSize = scoreValueFontSize.sp,
        scoreBarCornerRadius = scoreBarCornerRadius.dp,
        scoreBarHeight = scoreBarHeight.dp,

        spacerAfterScoreBar = spacerAfterScoreBar.dp,
        spacerAfterBoard = spacerAfterBoard.dp,

        boardPadding = boardPadding.dp,
        boardCornerRadius = boardCornerRadius.dp,
        boardWidth = actualBoardWidth.dp,
        boardHeight = actualBoardHeight.dp,

        pieceTrayHeight = pieceTrayHeight.dp,
        pieceTrayPaddingHorizontal = pieceTrayPaddingHorizontal.dp,
        pieceSlotPadding = pieceSlotPadding.dp,
        pieceSlotCornerRadius = pieceSlotCornerRadius.dp,
        piecePreviewCanvasSize = piecePreviewCanvasSize.dp,
        piecePreviewHexSize = piecePreviewHexSize,

        draggedPieceCanvasSize = draggedPieceCanvasSize.dp,
        draggedPieceHexSize = draggedPieceHexSize,

        buttonPaddingHorizontal = (16f * baseScale).coerceIn(12f, 24f).dp,
        buttonPaddingVertical = (8f * baseScale).coerceIn(6f, 14f).dp,
        buttonFontSize = buttonFontSize.sp,
        overlayTitleFontSize = overlayTitleFontSize.sp,
        overlaySubtitleFontSize = overlaySubtitleFontSize.sp,
        overlayPadding = overlayPadding.dp,

        settingsIconSize = settingsIconSize.dp,
        settingsIconFontSize = settingsIconFontSize.sp,

        bonusTextFontSize = bonusTextFontSize.sp,

        scorePreviewFontSize = scorePreviewFontSize.sp,
        scorePreviewPadding = scorePreviewPadding.dp
    )
}
