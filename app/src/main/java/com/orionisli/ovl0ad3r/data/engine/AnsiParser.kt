package com.orionisli.ovl0ad3r.data.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.orionisli.ovl0ad3r.ui.theme.TerminalThemePalette

object AnsiParser {

    private val NON_SGR_REGEX = Regex("\u001B\\[[0-9;]*[A-ln-zA-Z]|\u001B\\([AB012]|\u001B\\][^\u0007\u001B]*(\u0007|\u001B\\\\)")
    private val SGR_REGEX = Regex("\u001B\\[([0-9;]*)m")

    private val STANDARD_COLORS = arrayOf(
        Color(0xFF282C34), // 30: Black
        Color(0xFFE06C75), // 31: Red
        Color(0xFF98C379), // 32: Green
        Color(0xFFE5C07B), // 33: Yellow
        Color(0xFF61AFEF), // 34: Blue
        Color(0xFFC678DD), // 35: Magenta
        Color(0xFF56B6C2), // 36: Cyan
        Color(0xFFABB2BF)  // 37: White
    )

    private val BRIGHT_COLORS = arrayOf(
        Color(0xFF5C6370), // 90: Bright Black (Gray)
        Color(0xFFFF7B86), // 91: Bright Red
        Color(0xFFB5E890), // 92: Bright Green
        Color(0xFFF1D888), // 93: Bright Yellow
        Color(0xFF79C0FF), // 94: Bright Blue
        Color(0xFFD2A8FF), // 95: Bright Magenta
        Color(0xFF76E3EA), // 96: Bright Cyan
        Color(0xFFFFFFFF)  // 97: Bright White
    )

    private fun getStandardColor(index: Int, palette: TerminalThemePalette?): Color {
        if (palette == null) return STANDARD_COLORS[index]
        return when (index) {
            0 -> palette.ansiBlack
            1 -> palette.ansiRed
            2 -> palette.ansiGreen
            3 -> palette.ansiYellow
            4 -> palette.ansiBlue
            5 -> palette.ansiMagenta
            6 -> palette.ansiCyan
            7 -> palette.ansiWhite
            else -> STANDARD_COLORS[index]
        }
    }

    private fun getBrightColor(index: Int, palette: TerminalThemePalette?): Color {
        if (palette == null) return BRIGHT_COLORS[index]
        return when (index) {
            0 -> palette.ansiBrightBlack
            1 -> palette.ansiBrightRed
            2 -> palette.ansiBrightGreen
            3 -> palette.ansiBrightYellow
            4 -> palette.ansiBrightBlue
            5 -> palette.ansiBrightMagenta
            6 -> palette.ansiBrightCyan
            7 -> palette.ansiBrightWhite
            else -> BRIGHT_COLORS[index]
        }
    }

    fun stripAnsi(text: String): String =
        text.replace(NON_SGR_REGEX, "").replace(SGR_REGEX, "")

    fun parse(text: String, terminalPalette: TerminalThemePalette? = null): AnnotatedString {
        val sanitized = text.replace(NON_SGR_REGEX, "")
        val builder = AnnotatedString.Builder()
        var currentIndex = 0
        var currentFg: Color? = null
        var isBold = false

        val matches = SGR_REGEX.findAll(sanitized)
        for (match in matches) {
            if (match.range.first > currentIndex) {
                val plainText = sanitized.substring(currentIndex, match.range.first)
                appendStyled(builder, plainText, currentFg, isBold)
            }
            currentIndex = match.range.last + 1

            val codes = match.groupValues[1]
                .split(';')
                .mapNotNull { it.trim().toIntOrNull() }

            val effectiveCodes = if (codes.isEmpty()) listOf(0) else codes
            var i = 0
            while (i < effectiveCodes.size) {
                when (val code = effectiveCodes[i]) {
                    0 -> {
                        currentFg = null
                        isBold = false
                    }
                    1 -> isBold = true
                    22 -> isBold = false
                    39 -> currentFg = null
                    in 30..37 -> currentFg = getStandardColor(code - 30, terminalPalette)
                    in 90..97 -> currentFg = getBrightColor(code - 90, terminalPalette)
                    38 -> {
                        if (i + 2 < effectiveCodes.size && effectiveCodes[i + 1] == 5) {
                            val colorIdx = effectiveCodes[i + 2]
                            currentFg = parse256Color(colorIdx)
                            i += 2
                        }
                    }
                }
                i++
            }
        }

        if (currentIndex < sanitized.length) {
            val plainText = sanitized.substring(currentIndex)
            appendStyled(builder, plainText, currentFg, isBold)
        }

        return builder.toAnnotatedString()
    }

    private fun appendStyled(
        builder: AnnotatedString.Builder,
        text: String,
        fg: Color?,
        bold: Boolean
    ) {
        val start = builder.length
        builder.append(text)
        val end = builder.length
        if (fg != null || bold) {
            builder.addStyle(
                SpanStyle(
                    color = fg ?: Color.Unspecified,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
                ),
                start,
                end
            )
        }
    }

    private fun parse256Color(colorIdx: Int): Color {
        return when (colorIdx) {
            in 0..7 -> STANDARD_COLORS[colorIdx]
            in 8..15 -> BRIGHT_COLORS[colorIdx - 8]
            in 16..231 -> {
                val idx = colorIdx - 16
                val r = (idx / 36) * 51
                val g = ((idx % 36) / 6) * 51
                val b = (idx % 6) * 51
                Color(r, g, b)
            }
            in 232..255 -> {
                val gray = (colorIdx - 232) * 10 + 8
                Color(gray, gray, gray)
            }
            else -> Color.Unspecified
        }
    }
}
