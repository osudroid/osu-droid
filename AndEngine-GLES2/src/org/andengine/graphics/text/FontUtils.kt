/*
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 15:06:32 - 25.01.2012
 */
package org.andengine.graphics.text

import org.andengine.entity.text.AutoWrap
import org.andengine.util.TextUtils
import org.andengine.util.exception.MethodNotYetImplementedException

object FontUtils {

    private const val UNSPECIFIED = -1

    /**
     * @return the width of pText.
     */
    @JvmStatic
    fun measureText(pFont: IFont, pText: CharSequence): Float {
        return measureText(pFont, pText, null)
    }

    /**
     * @param pStart the index of the first character to start measuring.
     * @param pEnd `1` beyond the index of the last character to measure.
     * @return the width of pText.
     */
    @JvmStatic
    fun measureText(pFont: IFont, pText: CharSequence, pStart: Int, pEnd: Int): Float {
        return measureText(pFont, pText, pStart, pEnd, null)
    }

    /**
     * @param pWidths (optional) If not `null`, returns the actual width measured.
     * @return the width of pText.
     */
    @JvmStatic
    fun measureText(pFont: IFont, pText: CharSequence, pWidths: FloatArray?): Float {
        return measureText(pFont, pText, 0, pText.length, pWidths)
    }

    /**
     * Does not respect linebreaks!
     *
     * @param pStart the index of the first character to start measuring.
     * @param pEnd `1` beyond the index of the last character to measure.
     * @param pWidths (optional) If not `null`, returns the actual width after each character.
     * @return the width of pText.
     */
    @JvmStatic
    fun measureText(pFont: IFont, pText: CharSequence, pStart: Int, pEnd: Int, pWidths: FloatArray?): Float {
        val textLength = pEnd - pStart
        // Early exits.
        if (pStart == pEnd) {
            return 0f
        } else if (textLength == 1) {
            return pFont.getLetter(pText[pStart]).mWidth
        }

        var previousLetter: Letter? = null
        var width = 0f
        var i = 0
        for (pos in pStart until pEnd) {
            val letter = pFont.getLetter(pText[pos])
            if (previousLetter != null) {
                width += previousLetter.getKerning(letter.mCharacter.code)
            }
            previousLetter = letter

            // Check if this is the last character.
            if (pos == pEnd - 1) {
                width += letter.mOffsetX + letter.mWidth
            } else {
                width += letter.mAdvance
            }

            if (pWidths != null) {
                pWidths[i] = width
            }
            i++
        }
        return width
    }

    /**
     * Measure the text, stopping early if the measured width exceeds pMaximumWidth.
     *
     * @param pMeasureDirection If [MeasureDirection.FORWARDS], starts with the first character in the [CharSequence]. If [MeasureDirection.BACKWARDS] starts with the last character in the [CharSequence].
     * @param pMeasuredWidth (optional) If not `null`, returns the actual width measured. Must be an Array of size `1` or bigger.
     * @return the number of chars that were measured.
     */
    @JvmStatic
    fun breakText(pFont: IFont, pText: CharSequence, pMeasureDirection: MeasureDirection, pWidthMaximum: Float, pMeasuredWidth: FloatArray?): Int {
        throw MethodNotYetImplementedException()
    }

    @JvmStatic
    fun <L : MutableList<CharSequence>> splitLines(pText: CharSequence, pResult: L): L {
        return TextUtils.split(pText, '\n', pResult)
    }

    /**
     * Does not respect linebreaks!
     */
    @JvmStatic
    fun <L : MutableList<CharSequence>> splitLines(pFont: IFont, pText: CharSequence, pResult: L, pAutoWrap: AutoWrap, pAutoWrapWidth: Float): L {
        // TODO In order to respect already existing linebreaks, {@link FontUtils#split(CharSequence, List)} could be leveraged and than the following methods could be called for each line.
        return when (pAutoWrap) {
            AutoWrap.LETTERS -> splitLinesByLetters(pFont, pText, pResult, pAutoWrapWidth)
            AutoWrap.WORDS -> splitLinesByWords(pFont, pText, pResult, pAutoWrapWidth)
            AutoWrap.CJK -> splitLinesByCJK(pFont, pText, pResult, pAutoWrapWidth)
            else -> throw IllegalArgumentException("Unexpected " + AutoWrap::class.java.simpleName + ": '" + pAutoWrap + "'.")
        }
    }

    private fun <L : MutableList<CharSequence>> splitLinesByLetters(pFont: IFont, pText: CharSequence, pResult: L, pAutoWrapWidth: Float): L {
        val textLength = pText.length

        var lineStart = 0
        var lineEnd = 0
        var lastNonWhitespace = 0
        var charsAvailable = false

        var i = 0
        while (i < textLength) {
            val character = pText[i]
            if (character != ' ') {
                if (charsAvailable) {
                    lastNonWhitespace = i + 1
                } else {
                    charsAvailable = true
                    lineStart = i
                    lastNonWhitespace = lineStart + 1
                    lineEnd = lastNonWhitespace
                }
            }

            if (charsAvailable) {
//				/* Just for debugging. */
//				final CharSequence line = pText.subSequence(lineStart, lineEnd);
//				final float lineWidth = FontUtils.measureText(pFont, pText, lineStart, lineEnd);
//
//				final CharSequence lookaheadLine = pText.subSequence(lineStart, lastNonWhitespace);
                val lookaheadLineWidth = measureText(pFont, pText, lineStart, lastNonWhitespace)

                val isEndReached = i == textLength - 1
                if (isEndReached) {
                    // When the end of the string is reached, add remainder to result.
                    if (lookaheadLineWidth <= pAutoWrapWidth) {
                        pResult.add(pText.subSequence(lineStart, lastNonWhitespace))
                    } else {
                        pResult.add(pText.subSequence(lineStart, lineEnd))
                        // Avoid special case where last line is added twice.
                        if (lineStart != i) {
                            pResult.add(pText.subSequence(i, lastNonWhitespace))
                        }
                    }
                } else {
                    if (lookaheadLineWidth <= pAutoWrapWidth) {
                        lineEnd = lastNonWhitespace
                    } else {
                        pResult.add(pText.subSequence(lineStart, lineEnd))
                        i = lineEnd - 1
                        charsAvailable = false
                    }
                }
            }
            i++
        }

        return pResult
    }

    private fun <L : MutableList<CharSequence>> splitLinesByWords(pFont: IFont, pText: CharSequence, pResult: L, pAutoWrapWidth: Float): L {
        val textLength = pText.length

        if (textLength == 0) {
            return pResult
        }

        val spaceWidth = pFont.getLetter(' ').mAdvance

        var lastWordEnd = UNSPECIFIED
        var lineStart = UNSPECIFIED
        var lineEnd = UNSPECIFIED

        var lineWidthRemaining = pAutoWrapWidth
        var firstWordInLine = true
        var i = 0
        while (i < textLength) {
            var spacesSkipped = 0
            // Find next word.
            // Skip whitespaces.
            while (i < textLength && pText[i] == ' ') {
                i++
                spacesSkipped++
            }
            val wordStart = i

            // Mark beginning of a new line.
            if (lineStart == UNSPECIFIED) {
                lineStart = wordStart
            }

            // Skip non-whitespaces.
            while (i < textLength && pText[i] != ' ') {
                i++
            }
            val wordEnd = i

            // Nothing more could be read.
            if (wordStart == wordEnd) {
                if (!firstWordInLine) {
                    pResult.add(pText.subSequence(lineStart, lineEnd))
                }
                break
            }

//			/* Just for debugging. */
//			final CharSequence word = pText.subSequence(wordStart, wordEnd);

            val wordWidth = measureText(pFont, pText, wordStart, wordEnd)

            // Determine the width actually needed for the current word.
            val widthNeeded = if (firstWordInLine) {
                wordWidth
            } else {
                spacesSkipped * spaceWidth + wordWidth
            }

            // Check if the word fits into the rest of the line.
            if (widthNeeded <= lineWidthRemaining) {
                if (firstWordInLine) {
                    firstWordInLine = false
                } else {
                    lineWidthRemaining -= getAdvanceCorrection(pFont, pText, lastWordEnd - 1)
                }
                lineWidthRemaining -= widthNeeded
                lastWordEnd = wordEnd
                lineEnd = wordEnd

                // Check if the end was reached.
                if (wordEnd == textLength) {
                    pResult.add(pText.subSequence(lineStart, lineEnd))
                    // Added the last line.
                    break
                }
            } else {
                // Special case for lines with only one word.
                if (firstWordInLine) {
                    // Check for lines that are just too big.
                    if (wordWidth >= pAutoWrapWidth) {
                        pResult.add(pText.subSequence(wordStart, wordEnd))
                        lineWidthRemaining = pAutoWrapWidth
                    } else {
                        lineWidthRemaining = pAutoWrapWidth - wordWidth

                        // Check if the end was reached.
                        if (wordEnd == textLength) {
                            pResult.add(pText.subSequence(wordStart, wordEnd))
                            // Added the last line.
                            break
                        }
                    }

                    // Start a completely new line.
                    firstWordInLine = true
                    lastWordEnd = UNSPECIFIED
                    lineStart = UNSPECIFIED
                    lineEnd = UNSPECIFIED
                } else {
                    // Finish the current line.
                    pResult.add(pText.subSequence(lineStart, lineEnd))

                    // Check if the end was reached.
                    if (wordEnd == textLength) {
                        // Add the last word.
                        pResult.add(pText.subSequence(wordStart, wordEnd)) // TODO Does this cover all cases?
                        break
                    } else {
                        // Start a new line, carrying over the current word.
                        lineWidthRemaining = pAutoWrapWidth - wordWidth
                        firstWordInLine = false
                        lastWordEnd = wordEnd
                        lineStart = wordStart
                        lineEnd = wordEnd
                    }
                }
            }
        }
        return pResult
    }

    private fun <L : MutableList<CharSequence>> splitLinesByCJK(pFont: IFont, pText: CharSequence, pResult: L, pAutoWrapWidth: Float): L {
        val textLength = pText.length

        var lineStart = 0
        var lineEnd = 0

        // Skip whitespaces at the beginning of the string.
        while (lineStart < textLength && pText[lineStart] == ' ') {
            lineStart++
            lineEnd++
        }

        var i = lineEnd
        while (i < textLength) {
            lineStart = lineEnd

            // Look for a sub string.
            var charsAvailable = true
            while (i < textLength) {

                // Skip whitespaces at the end of the string.
                var j = lineEnd
                while (j < textLength) {
                    if (pText[j] == ' ') {
                        j++
                    } else {
                        break
                    }
                }
                if (j == textLength) {
                    if (lineStart == lineEnd) {
                        charsAvailable = false
                    }
                    i = textLength
                    break
                }

                lineEnd++

                val lineWidth = measureText(pFont, pText, lineStart, lineEnd)

                if (lineWidth > pAutoWrapWidth) {
                    if (lineStart < lineEnd - 1) {
                        lineEnd--
                    }

                    pResult.add(pText.subSequence(lineStart, lineEnd))
                    charsAvailable = false
                    i = lineEnd
                    break
                }
                i = lineEnd
            }

            if (charsAvailable) {
                pResult.add(pText.subSequence(lineStart, lineEnd))
            }
        }

        return pResult
    }

    private fun getAdvanceCorrection(pFont: IFont, pText: CharSequence, pIndex: Int): Float {
        val lastWordLastLetter = pFont.getLetter(pText[pIndex])
        return -(lastWordLastLetter.mOffsetX + lastWordLastLetter.mWidth) + lastWordLastLetter.mAdvance
    }

    enum class MeasureDirection {
        FORWARDS,
        BACKWARDS
    }
}
