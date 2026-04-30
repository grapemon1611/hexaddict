# Hex Addict — Claude Code Project Instructions

## Project Identity
- **App name:** Hex Addict
- **Package:** `com.almostbrilliantideas.hexaddict`
- **Platform:** Android (Kotlin)
- **Min SDK:** API 24
- **Developer:** Almost Brilliant Ideas

## What This Game Is
A hex block puzzle game where the player drags pieces onto a hexagonal grid and clears lines on three axes simultaneously. Think Bejeweled and Tetris had a baby. The hex grid is the visual differentiator — this is an endless score-attack game with no levels, no rounds, no timers.

## Current Build Status

### COMPLETE — Do Not Refactor Without Good Reason
- **Hex board rendering** — 77-cell grid, 9 rows alternating 9/8 cells, pointy-top hexagons
- **Axial coordinate system** — verified and validated, do not change the math
- **Drag and drop placement** — piece follows finger, snaps to nearest valid hex cell
- **15-piece library** — all pieces defined in axial coordinates, no rotation
- **Weighted piece generation** — shifts from small pieces early to compact shapes late game
- **Three-axis line clearing** — horizontal, diagonal-R (q fixed), diagonal-L (q+r fixed)
- **Color palette progression** — 4 colors at 0-999, 5 at 1000-2999, 6 at 3000+
- **Color bonus scoring** — 2x single same-color line, 3x PAYDAY, JACKPOT board clear x5
- **Visual feedback** — 2x indicator implemented, PAYDAY and JACKPOT text overlays
- **Game over screen** — shows final score, best score, Play Again button
- **Best score persistence** — stored in SharedPreferences, survives app restarts

### IN PROGRESS (current session)
- Ambient line highlighting — lines with 2+ same-color cells glow subtly, intensity increases as line fills
- Score preview on drag — shows points a placement would score including color bonus multiplier

### NOT YET STARTED
- Time-of-day and seasonal backgrounds (16 combinations)
- Sound effects (6 distinct clear tiers plus placement and game over)
- Haptic feedback
- Settings screen (sound toggle, haptics toggle)
- AdMob integration (game over screen only)
- One-time ad removal purchase (Google Play Billing)

## Critical Coordinate System
Do not modify these — they are mathematically verified:

```
Axial to pixel:
  x = size * sqrt(3) * (q + r/2)
  y = size * 1.5 * r

Board cell to axial:
  q = col - floor(row/2)
  r = row

Six neighbor directions:
  (1,0), (-1,0), (0,1), (0,-1), (1,-1), (-1,1)
```

## Board Geometry
- 9 rows, alternating 9 and 8 cells (row 0 = 9 cells, row 1 = 8 cells, etc.)
- 77 total cells
- 73 of 77 cells sit on all three axes — triple-axis clears are achievable
- Shortest diagonal lines are 3 cells (board corners)
- Horizontal lines: 9 lines (five 9-cell, four 8-cell)
- Diagonal-R lines: 11 lines (five 9-cell, two 7-cell, two 5-cell, two 3-cell)
- Diagonal-L lines: 11 lines (five 9-cell, two 7-cell, two 5-cell, two 3-cell)

## Piece Library (15 total, axial coordinates, no rotation)
**1-cell:** single [(0,0)]

**2-cell (3 pieces):**
- pair flat: [(0,0),(1,0)]
- pair diag-R: [(0,0),(0,1)]
- pair diag-L: [(0,0),(-1,1)]

**3-cell straight lines (3 pieces):**
- line horiz: [(0,0),(1,0),(2,0)]
- line diag-R: [(0,0),(0,1),(0,2)]
- line diag-L: [(0,0),(-1,1),(-2,2)]

**3-cell compact shapes (8 pieces):**
- c1: [(0,0),(0,1),(1,0)]
- c2: [(0,0),(0,1),(1,1)]
- c3: [(0,0),(1,0),(1,1)]
- c4: [(0,1),(0,2),(1,0)]
- c5: [(0,1),(1,0),(1,1)]
- c6: [(0,1),(1,0),(2,0)]
- c7: [(0,1),(1,1),(2,0)]
- c8: [(0,2),(1,0),(1,1)]

## Color Palette (6 colors, gradient fills)
- Purple: #5A52B0 → #8B84D4
- Teal: #268B68 → #4DB893
- Coral: #B05530 → #D88060
- Blue: #2870A8 → #55A0CE
- Mauve: #7A52A0 → #A880C4
- Amber: #A87E10 → #D4AE45

## Scoring Rules
- 1 point per cell placed
- 10 points per cell cleared in a line
- Same-color line bonus: 2x multiplier on entire move
- Two or more same-color lines (PAYDAY): 3x multiplier on entire move
- JACKPOT (all three axes + at least one same-color line): entire board clears, all remaining cells score x5
- Multiplier applies to clear points only, not placement points
- Mixed clears still trigger highest applicable tier

## Weighted Piece Generation
- Score 0–999: heavy small pieces (single 4.0x, pair 3.5x, line 1.5x, compact 1.0x)
- Score 1000–2999: gentle transition, small pieces still dominant
- Score 3000–4999: balanced mix
- Score 5000+: aggressive pressure (single 1.0x, pair 1.0x, line 2.0x, compact 8.0x)
- Tray size: 2 pieces (reduced from 3 for increased difficulty)
- Guaranteed floor: no tray can be all compact shapes — always replace one with 1 or 2-cell piece
- Uses smoothStep interpolation between breakpoints

## Design Philosophy — Read This
- **Ship once, ship complete.** No planned updates or iterations.
- **Offline first.** Game works fully without internet. Ad load failure never interrupts gameplay.
- **No manipulation.** No energy systems, currencies, loot boxes, forced online, subscriptions.
- **Ads only at game over screen.** Never during active play, drag operations, animations, or decisions.
- **Silent progression.** Palette expansion and difficulty increase happen without announcements.
- **Restrained aesthetic.** Muted gradient blocks, frosted board surface, atmospheric backgrounds. No wood texture, no neon, no casino glow — except JACKPOT which intentionally breaks this rule.

## Monetization (not yet implemented)
- Free with ads at game over screen only
- One-time purchase removes ads forever
- If offline and ads cannot load: game continues normally, no penalty, no nagging
- No account required

## Typography
- **Logo/game title only:** Google Font — Oxanium
- **All other UI elements:** system default font

## What NOT to Build
- Piece rotation
- Multiplayer
- Accounts or cloud sync
- Daily challenges or leaderboards
- Achievements
- Alternate game modes
- Energy systems, currencies, battle pass, loot boxes
- Forced online play
- Story mode
