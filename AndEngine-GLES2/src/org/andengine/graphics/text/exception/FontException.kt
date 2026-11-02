/*
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 17:53:31 - 01.11.2011
 */
package org.andengine.graphics.text.exception

import org.andengine.util.exception.AndEngineRuntimeException

open class FontException : AndEngineRuntimeException {

    constructor() : super()

    constructor(pMessage: String?) : super(pMessage)

    constructor(pThrowable: Throwable?) : super(pThrowable)

    constructor(pMessage: String?, pThrowable: Throwable?) : super(pMessage, pThrowable)

    private companion object {
        private const val serialVersionUID = 2766566088383545102L
    }
}
