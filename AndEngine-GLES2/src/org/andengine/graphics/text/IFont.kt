/*
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 12:15:25 - 03.11.2011
 */
package org.andengine.graphics.text

import org.andengine.graphics.text.exception.LetterNotFoundException

interface IFont {

    val lineHeight: Float

    fun load()

    fun unload()

    @Throws(LetterNotFoundException::class)
    fun getLetter(pChar: Char): Letter
}
