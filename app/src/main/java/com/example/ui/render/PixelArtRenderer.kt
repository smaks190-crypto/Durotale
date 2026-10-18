package com.example.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Universal Undertale Pixel Art Sprite drawing extension for Compose DrawScope.
 */
fun DrawScope.drawPixelSprite(
    matrix: List<String>,
    palette: Map<Char, Color>,
    topLeft: Offset,
    pixelSize: Float,
    flipX: Boolean = false
) {
    val height = matrix.size
    for (row in 0 until height) {
        val line = matrix[row]
        val width = line.length
        for (col in 0 until width) {
            val char = line[col]
            if (char == ' ' || char == '.') continue
            val color = palette[char] ?: continue
            val actualCol = if (flipX) width - 1 - col else col
            drawRect(
                color = color,
                topLeft = Offset(topLeft.x + actualCol * pixelSize, topLeft.y + row * pixelSize),
                size = Size(pixelSize, pixelSize)
            )
        }
    }
}

object PixelSprites {

    // Palettes
    val BLACK = Color.Black
    val WHITE = Color.White
    val SKIN = Color(0xFFFFCCAA)
    val SKIN_SHADOW = Color(0xFFE5A882)
    val HAIR = Color(0xFF5D4037)
    val HAIR_DARK = Color(0xFF3E2723)
    val STRAP = Color(0xFF78909C)
    val BUCKLE = Color(0xFFFFD54F)
    val PANTS = Color(0xFF37474F)
    val PANTS_SHADOW = Color(0xFF263238)
    val BOOTS = Color(0xFF1E272C)
    val DOCTOR_COAT = Color(0xFFE0F2F1)
    val DOCTOR_TRIM = Color(0xFF26A69A)
    val RED_CROSS = Color(0xFFE53935)
    val STETHOSCOPE = Color(0xFF546E7A)
    val BRASS_KEY = Color(0xFFFFB300)
    val BLUE_STRIPE = Color(0xFF1E88E5)
    val VEGETABLE_SKIN = Color(0xFFA5D6A7)

    val PlayerPalette = mapOf(
        '#' to BLACK,
        'W' to WHITE,
        'S' to SKIN,
        's' to SKIN_SHADOW,
        'H' to HAIR,
        'h' to HAIR_DARK,
        'B' to STRAP,
        'b' to BUCKLE,
        'P' to PANTS,
        'p' to PANTS_SHADOW,
        'F' to BOOTS,
        'C' to DOCTOR_COAT,
        'T' to DOCTOR_TRIM,
        'R' to RED_CROSS,
        'M' to STETHOSCOPE,
        'G' to VEGETABLE_SKIN
    )

    // Player in Straitjacket: Facing Down (16x22)
    val PLAYER_DOWN_IDLE = listOf(
        "..########......",
        ".#HHHHHHHH#.....",
        "#HHHHHHHHHH#....",
        "#H##H##H###H#...",
        "#HSSSSSSSSSH#...",
        "#H#S#SS#S#SH#...",
        "#HSSSSSSSSSH#...",
        ".#HsSSsSSsH#....",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WWBWWWBWW#....",
        "#WWWBWBWWBWW#...",
        "#WWWWbbWWWWW#...",
        "#WWBWWWBWWWB#...",
        ".#WWWBWBWWWW#...",
        "..#WWWWWWWW#....",
        "..#WWWWWWWW#....",
        "..#PPPPPPPP#....",
        "..#PP#..#PP#....",
        "..#PP#..#PP#....",
        "..#FF#..#FF#....",
        "..####..####...."
    )

    val PLAYER_DOWN_STEP1 = listOf(
        "..########......",
        ".#HHHHHHHH#.....",
        "#HHHHHHHHHH#....",
        "#H##H##H###H#...",
        "#HSSSSSSSSSH#...",
        "#H#S#SS#S#SH#...",
        "#HSSSSSSSSSH#...",
        ".#HsSSsSSsH#....",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WWBWWWBWW#....",
        "#WWWBWBWWBWW#...",
        "#WWWWbbWWWWW#...",
        "#WWBWWWBWWWB#...",
        ".#WWWBWBWWWW#...",
        "..#WWWWWWWW#....",
        "..#WWWWWWWW#....",
        "..#PPPPPPPP#....",
        "..#PPP#...##....",
        "..#PPP#..#PP#...",
        "..#FFF#..#FF#...",
        "..#####..####..."
    )

    val PLAYER_DOWN_STEP2 = listOf(
        "..########......",
        ".#HHHHHHHH#.....",
        "#HHHHHHHHHH#....",
        "#H##H##H###H#...",
        "#HSSSSSSSSSH#...",
        "#H#S#SS#S#SH#...",
        "#HSSSSSSSSSH#...",
        ".#HsSSsSSsH#....",
        "..#SSSSSS#......",
        "..#WWWWWW#......",
        ".#WWBWWWBWW#....",
        "#WWWBWBWWBWW#...",
        "#WWWWbbWWWWW#...",
        "#WWBWWWBWWWB#...",
        ".#WWWBWBWWWW#...",
        "..#WWWWWWWW#....",
        "..#WWWWWWWW#....",
        "..#PPPPPPPP#....",
        "...##...#PPP#...",
        "..#PP#..#PPP#...",
        "..#FF#..#FFF#...",
        "..####..#####..."
    )

    // Player Straitjacket: Facing Up (16x22)
    val PLAYER_UP_IDLE = listOf(
        "..########......",
        ".#HHHHHHHH#.....",
        "#HHHHHHHHHH#....",
        "#HHHHHHHHHHH#...",
        "#HHHHHHHHHHH#...",
        "#HHHHHHHHHHH#...",
        "#HHHHHHHHHHH#...",
        ".#hHHHHHHHH#....",
        "..#hhhhhh#......",
        "..#WWWWWW#......",
        ".#WWBWWWBWW#....",
        "#WWWBWBWWBWW#...",
        "#WWWWbbWWWWW#...",
        "#WWBWWWBWWWB#...",
        ".#WWWBWBWWWW#...",
        "..#WWWWWWWW#....",
        "..#WWWWWWWW#....",
        "..#PPPPPPPP#....",
        "..#PP#..#PP#....",
        "..#PP#..#PP#....",
        "..#FF#..#FF#....",
        "..####..####...."
    )

    // Player Straitjacket: Facing Side (16x22)
    val PLAYER_SIDE_IDLE = listOf(
        "....########....",
        "...#HHHHHHHH#...",
        "..#HHHHHHHHHH#..",
        "..#H##H##HHHH#..",
        "..#HSSSSSSSHH#..",
        "..#H#S#SSSHHH#..",
        "..#HSSSSSSHHH#..",
        "...#HsSSSsHH#...",
        "....#SSSSSH#....",
        "....#WWWWW#.....",
        "...#WWWBWW#.....",
        "..#WWWWBWBW#....",
        "..#WWWWbbWW#....",
        "..#WWWWWWWW#....",
        "...#WWWWWW#.....",
        "....#WWWW#......",
        "....#WWWW#......",
        "....#PPPP#......",
        "...#PPPPPP#.....",
        "...#PP##PP#.....",
        "...#FF##FF#.....",
        "...########....."
    )

    val PLAYER_SIDE_STEP1 = listOf(
        "....########....",
        "...#HHHHHHHH#...",
        "..#HHHHHHHHHH#..",
        "..#H##H##HHHH#..",
        "..#HSSSSSSSHH#..",
        "..#H#S#SSSHHH#..",
        "..#HSSSSSSHHH#..",
        "...#HsSSSsHH#...",
        "....#SSSSSH#....",
        "....#WWWWW#.....",
        "...#WWWBWW#.....",
        "..#WWWWBWBW#....",
        "..#WWWWbbWW#....",
        "..#WWWWWWWW#....",
        "...#WWWWWW#.....",
        "....#WWWW#......",
        "....#WWWW#......",
        "....#PPPP#......",
        "...#PP#..##.....",
        "..#PPP#..#PP#...",
        "..#FFF#..#FF#...",
        "..#####..####..."
    )

    // Player in Doctor's Coat (After beating Mihalych): Facing Down (16x22)
    val PLAYER_DOCTOR_DOWN = listOf(
        "..#WWWWWW#......",
        ".#WWWRRWWW#.....",
        ".#WWWRRWWW#.....",
        ".#WWRRRRWW#.....",
        "#HSSSSSSSSSH#...",
        "#H#S#SS#S#SH#...",
        "#HSSSSSSSSSH#...",
        ".#HsSSsSSsH#....",
        "..#MSSSsSM#.....",
        "..#MCCCCCM#.....",
        ".#CMWTTTWMTC#...",
        "#CCMWTITWMTCC#..",
        "#CCMWWWWWWTCC#..",
        "#CCMWWWWWWTCC#..",
        ".#CMWWWWWWMC#...",
        "..#CCCCCCCCC#...",
        "..#CCCCCCCCC#...",
        "..#PPPPPPPP#....",
        "..#PP#..#PP#....",
        "..#PP#..#PP#....",
        "..#FF#..#FF#....",
        "..####..####...."
    )

    // Orderly Mihalych Overworld Sprite: Facing Up/Down/Side (18x24)
    val OrderlyPalette = mapOf(
        '#' to BLACK,
        'W' to WHITE,
        'w' to Color(0xFFD0DCDC),
        'S' to Color(0xFFF5CBA7),
        's' to Color(0xFFDCA882),
        'H' to Color(0xFF4E342E),
        'R' to RED_CROSS,
        'P' to Color(0xFF37474F),
        'p' to Color(0xFF263238),
        'F' to Color(0xFF1A1A1A),
        'K' to BRASS_KEY,
        'B' to Color(0xFF1E88E5), // Badge
        'b' to Color(0xFF90CAF9)  // Badge card
    )

    val ORDERLY_DOWN_IDLE = listOf(
        "...#WWWWWWWWWW#...",
        "..#WWWWRRWWWWWW#..",
        "..#WWWRRRRWWWWW#..",
        "..#WWWWRRWWWWWW#..",
        ".#HSSSSSSSSSSSS#..",
        ".#H#s#sSSs#s#sS#..",
        ".#HSSSSSSSSSSSS#..",
        ".#HssHHHHHHHHss#..",
        "..#HssHHHHHHss#...",
        "..#WWWWWWWWWW#....",
        ".#WWWWWWWWWWWW#...",
        "#WWWWWWWbBWWWWW#..",
        "#WWWWWWWbbWWWWW#..",
        "#WWWWWWWWWWWWWW#..",
        "#WWWWWWWWWWKKKW#..",
        ".#WWWWWWWWWKKKW#..",
        "..#WWWWWWWWWW#....",
        "..#WWWWWWWWWW#....",
        "..#PPPPPPPPPP#....",
        "..#PPP#..#PPP#....",
        "..#PPP#..#PPP#....",
        "..#PPP#..#PPP#....",
        "..#FFF#..#FFF#....",
        "..#####..#####...."
    )

    val ORDERLY_DOWN_STEP1 = listOf(
        "...#WWWWWWWWWW#...",
        "..#WWWWRRWWWWWW#..",
        "..#WWWRRRRWWWWW#..",
        "..#WWWWRRWWWWWW#..",
        ".#HSSSSSSSSSSSS#..",
        ".#H#s#sSSs#s#sS#..",
        ".#HSSSSSSSSSSSS#..",
        ".#HssHHHHHHHHss#..",
        "..#HssHHHHHHss#...",
        "..#WWWWWWWWWW#....",
        ".#WWWWWWWWWWWW#...",
        "#WWWWWWWbBWWWWW#..",
        "#WWWWWWWbbWWWWW#..",
        "#WWWWWWWWWWWWWW#..",
        "#WWWWWWWWWWKKKW#..",
        ".#WWWWWWWWWKKKW#..",
        "..#WWWWWWWWWW#....",
        "..#WWWWWWWWWW#....",
        "..#PPPPPPPPPP#....",
        "..#PPPP#..###.....",
        "..#PPPP#.#PPP#....",
        "..#PPPP#.#PPP#....",
        "..#FFFF#.#FFF#....",
        "..######.#####...."
    )

    val ORDERLY_DOWN_STEP2 = listOf(
        "...#WWWWWWWWWW#...",
        "..#WWWWRRWWWWWW#..",
        "..#WWWRRRRWWWWW#..",
        "..#WWWWRRWWWWWW#..",
        ".#HSSSSSSSSSSSS#..",
        ".#H#s#sSSs#s#sS#..",
        ".#HSSSSSSSSSSSS#..",
        ".#HssHHHHHHHHss#..",
        "..#HssHHHHHHss#...",
        "..#WWWWWWWWWW#....",
        ".#WWWWWWWWWWWW#...",
        "#WWWWWWWbBWWWWW#..",
        "#WWWWWWWbbWWWWW#..",
        "#WWWWWWWWWWWWWW#..",
        "#WWWWWWWWWWKKKW#..",
        ".#WWWWWWWWWKKKW#..",
        "..#WWWWWWWWWW#....",
        "..#WWWWWWWWWW#....",
        "..#PPPPPPPPPP#....",
        "....###..#PPPP#...",
        "..#PPP#..#PPPP#...",
        "..#PPP#..#PPPP#...",
        "..#FFF#..#FFFF#...",
        "..#####..######..."
    )

    val ORDERLY_STRIPPED_DOWN = listOf(
        "...#HHHHHHHHHH#...",
        "..#HHHHHHHHHHHH#..",
        ".#HSSSSSSSSSSSS#..",
        ".#H#s#sSSs#s#sS#..",
        ".#HSSSSSSSSSSSS#..",
        ".#HssHHHHHHHHss#..",
        "..#HssHHHHHHss#...",
        "..#WWWWWWWWWW#....",
        ".#WBBBBBBBBBBW#...",
        "#WWWWWWWWWWWWWW#..",
        "#WBBBBBBBBBBBBW#..",
        "#WWWWWWWWWWWWWW#..",
        "#WBBBBBBBBBBBBW#..",
        ".#WWWWWWWWWWWW#...",
        "..#WBBBBBBBBW#....",
        "..#WWWWWWWWWW#....",
        "..#PPPPPPPPPP#....",
        "..#PPP#..#PPP#....",
        "..#PPP#..#PPP#....",
        "..#FFF#..#FFF#....",
        "..#####..#####...."
    )

    // ==========================================
    // UNDERTALE BATTLE BOSS (MONOCHROME RETRO STYLE)
    // ==========================================
    val BossMonochromePalette = mapOf(
        '#' to Color.Black,
        'W' to Color.White,
        'G' to Color(0xFFC0C0C0),
        'D' to Color(0xFF606060),
        'R' to Color(0xFFFF2222) // Syringe needle glow/cross
    )

    // Boss Head (Normal Angry) - 24x18
    val BOSS_HEAD_NORMAL = listOf(
        "......############......",
        ".....#WWWWWWWWWWWW#.....",
        "....#WWWWWWRRWWWWWW#....",
        "....#WWWWWRRRRWWWWW#....",
        "....#WWWWWWRRWWWWWW#....",
        "...#WWWWWWWWWWWWWWWW#...",
        "..#GGGGGGGGGGGGGGGGGG#..",
        ".#G##GG##GGGGGG##GG##G#.",
        "#GG##GG##GGGGGG##GG##GG#",
        "#GGGGGGGGGGGGGGGGGGGGGG#",
        "#GG#G#GG#GGGGGG#GG#G#GG#",
        "#GGGGGGGGGGGGGGGGGGGGGG#",
        "#GG##DDDDDDDDDDDDDD##GG#",
        "#GG#DDDDDDDDDDDDDDDD#GG#",
        ".#GG#DDDDDDDDDDDDDD#GG#.",
        "..#GG#DDDDDDDDDDDD#GG#..",
        "...#GGGGGGGGGGGGGGGG#...",
        "....################...."
    )

    // Boss Head (Shocked - shoelace trick) - 24x18
    val BOSS_HEAD_SHOCKED = listOf(
        "......############......",
        ".....#WWWWWWWWWWWW#.....",
        "....#WWWWWWRRWWWWWW#....",
        "....#WWWWWRRRRWWWWW#....",
        "....#WWWWWWRRWWWWWW#....",
        "...#WWWWWWWWWWWWWWWW#...",
        "..#GGGGGGGGGGGGGGGGGG#..",
        ".#G##WWWW#GGGG#WWWW##G#.",
        "#G#WW#W#WW#GG#WW#W#WW#G#",
        "#G#WW#W#WW#GG#WW#W#WW#G#",
        "#G#WWWWWWW#GG#WWWWWWW#G#",
        "#GG##WWWW#GGGG#WWWW##GG#",
        "#GGGGGGGGGGGGGGGGGGGGGG#",
        "#GGG##DDDDDDDDDDDD##GGG#",
        ".#GG#D#WWWWWWWWWW#D#GG#.",
        "..#G#D#W########W#D#G#..",
        "...#GG#D#WWWWWW#D#GG#...",
        "....################...."
    )

    // Boss Head (Hurt/Grimace) - 24x18
    val BOSS_HEAD_HURT = listOf(
        "......############......",
        ".....#WWWWWWWWWWWW#.....",
        "....#WWWWWWRRWWWWWW#....",
        "....#WWWWWRRRRWWWWW#....",
        "....#WWWWWWRRWWWWWW#....",
        "...#WWWWWWWWWWWWWWWW#...",
        "..#GGGGGGGGGGGGGGGGGG#..",
        ".#GG##GG##GGGGGG##GG##G#",
        "#GGGG##GG##GGGG##GG##GGG#",
        "#GGGGGG##GGGGGGGG##GGGG#",
        "#GGGG##GG##GGGG##GG##GGG#",
        "#GG##GGGG##GGGG##GGGG##G#",
        "#GG#DDDDDDDDDDDDDDDD#GG#",
        "#GG#D#W#W#W#W#W#W#WD#GG#",
        ".#GG#D#W#W#W#W#W#WD#GG#.",
        "..#GG#DDDDDDDDDDDD#GG#..",
        "...#GGGGGGGGGGGGGGGG#...",
        "....################...."
    )

    // Boss Torso in Coat (26x24)
    val BOSS_TORSO = listOf(
        ".......##############.......",
        "......#WWWWWW##WWWWWW#......",
        ".....#WWWWWWW##WWWWWWW#.....",
        "....#WWWWWWWW##WWWWWWWW#....",
        "...#WWWWWWWW#DD#WWWWWWWW#...",
        "...#WWWWWWW#DDDD#WWWWWWW#...",
        "..#WWWWWWW#DDDDDD#WWWWWWW#..",
        "..#WWWWWWW#DDDDDD#WWWWWWW#..",
        ".#WWWWWWWW#DDDDDD#WWWWWWWW#.",
        ".#WWWWWWWW#DDDDDD#WWWWWWWW#.",
        "#WWWWWWWWW#DDDDDD#WWWWWWWWW#",
        "#WWWWWWWWWW#DDDD#WWWWWWWWWW#",
        "#WWWWWWWWWWW#DD#WWWWWWWWWWW#",
        "#WWWW#WWWWWW####WWWWWW#WWWW#",
        "#WWWW#WWWWWWWWWWWWWWWW#WWWW#",
        "#WWWW#WWWWWWWWWWWWWWWW#WWWW#",
        "#WWWW#WWWWWWWWWWWWWWWW#WWWW#",
        ".#WWWWWWWWWWWWWWWWWWWWWWWW#.",
        ".#WWWWWWWWWWWWWWWWWWWWWWWW#.",
        "..#WWWWWWWWWWWWWWWWWWWWWW#..",
        "..#WWWWWWWWWWWWWWWWWWWWWW#..",
        "...#WWWWWWWWWWWWWWWWWWWW#...",
        "....#WWWWWWWWWWWWWWWWWW#....",
        ".....##################....."
    )

    // Boss Right Arm with Giant Syringe (14x28)
    val BOSS_ARM_SYRINGE = listOf(
        "..########....",
        ".#WWWWWWWW#...",
        "#WWWWWWWWWW#..",
        "#WWWWWWWWWW#..",
        "#WWWW##WWWW#..",
        "#WWWW##WWWW#..",
        "#WWWW##WWWW#..",
        ".#WWWWWWWW#...",
        "..#GGGGGG#....",
        "..#G####G#....",
        "....#DD#......",
        "....#DD#......",
        "...#DDDD#.....",
        "..#WWWWWW#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#W#RR#W#....",
        "..#WWWWWW#....",
        "...#DDDD#.....",
        "....#DD#......",
        "....#DD#......",
        "....#DD#......",
        ".....##......."
    )

    // Boss Left Arm with Key Ring (14x24)
    val BOSS_ARM_KEYS = listOf(
        "....########..",
        "...#WWWWWWWW#.",
        "..#WWWWWWWWWW#",
        "..#WWWWWWWWWW#",
        "..#WWWW##WWWW#",
        "..#WWWW##WWWW#",
        "..#WWWW##WWWW#",
        "...#WWWWWWWW#.",
        "....#GGGGGG#..",
        "....#G####G#..",
        ".....#WWWW#...",
        "....#W####W#..",
        "...#W#....#W#.",
        "...#W#..###W#.",
        "...#W#.#####W#",
        "....#W#..###W#",
        ".....#W####W#.",
        "......#WWWW#..",
        ".......#WW#...",
        ".......#WW#...",
        ".......#WW##..",
        ".......#WW##..",
        ".......#WW#...",
        "........##...."
    )

    // Roman Numerals for Wall Hint (5x7)
    val ROMAN_I = listOf(
        "#####",
        "..#..",
        "..#..",
        "..#..",
        "..#..",
        "..#..",
        "#####"
    )

    val ROMAN_II = listOf(
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#"
    )

    val ROMAN_III = listOf(
        "#####",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#####"
    )

    val ROMAN_IV = listOf(
        "#.###",
        "#.#.#",
        "#.#.#",
        "#.#.#",
        "#..#.",
        "#..#.",
        "#.###"
    )

    val ROMAN_V = listOf(
        "#...#",
        "#...#",
        "#...#",
        ".#.#.",
        ".#.#.",
        "..#..",
        "..#.."
    )

    // Fist Icon for Bang (7x7)
    val ICON_FIST = listOf(
        ".#####.",
        "#..#..#",
        "#.###.#",
        "#######",
        "#######",
        ".#####.",
        "..###.."
    )

    // Scream / Mouth Icon for Scream (7x7)
    val ICON_SCREAM = listOf(
        "..###..",
        ".#...#.",
        "#.#.#.#",
        "#..#..#",
        ".#...#.",
        "..#.#..",
        "...#..."
    )

    val WallHintPalette = mapOf(
        '#' to Color(0xFF37474F),
        '.' to Color.Transparent
    )

    // Emote bubbles: "?" and "!"
    val EMOTE_QUESTION = listOf(
        ".#####.",
        "#.....#",
        "......#",
        "....##.",
        "...#...",
        ".......",
        "...#..."
    )

    val EMOTE_EXCLAMATION = listOf(
        "...#...",
        "...#...",
        "...#...",
        "...#...",
        "...#...",
        ".......",
        "...#..."
    )

    val EmotePalette = mapOf(
        '#' to Color(0xFFFFEB3B),
        '.' to Color.Transparent
    )
}
