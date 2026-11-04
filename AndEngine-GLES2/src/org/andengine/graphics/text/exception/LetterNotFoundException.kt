/*
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 12:19:24 - 03.11.2011
 */
package org.andengine.graphics.text.exception

open class LetterNotFoundException : FontException {

    constructor() : super()

    constructor(pMessage: String?) : super(pMessage)

    constructor(pThrowable: Throwable?) : super(pThrowable)

    constructor(pMessage: String?, pThrowable: Throwable?) : super(pMessage, pThrowable)

    private companion object {
        private const val serialVersionUID = 5260601170771253529L
    }
}
