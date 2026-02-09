package com.reco1l.andengine.ui

import com.reco1l.andengine.container.UIContainer
import org.anddev.andengine.input.touch.TouchEvent

open class UIClickableContainer : UIContainer() {

    /**
     * Whether the button is being pressed or not.
     */
    var isPressed = false
        private set


    /**
     * The action to perform when the button is pressed.
     */
    var onActionDown: (() -> Unit)? = null

    /**
     * The action to perform when the button is released.
     */
    var onActionUp: (() -> Unit)? = null

    /**
     * The action to perform when the button is cancelled.
     */
    var onActionCancel: (() -> Unit)? = null

    /**
     * The action to perform when the button is long pressed.
     */
    var onActionLongPress: (() -> Unit)? = null


    private var pressStartTime = 0L


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (super.onAreaTouched(event, localX, localY)) {
            return true
        }

        when {
            event.isActionDown -> {
                isPressed = true
                onActionDown?.invoke()
                pressStartTime = System.currentTimeMillis()
            }

            event.isActionUp -> {
                if (localX <= width && localY <= height && isPressed) {
                    onActionUp?.invoke()
                } else {
                    onActionCancel?.invoke()
                }
                isPressed = false
            }

            event.isActionOutside || event.isActionCancel -> {
                onActionCancel?.invoke()
                isPressed = false
            }

            !event.isActionMove -> isPressed = false
        }
        return true
    }

    //endregion

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (onActionLongPress != null) {
            if (isPressed && System.currentTimeMillis() - pressStartTime >= 500L) {
                onActionLongPress?.invoke()
                propagateTouchEvent(TouchEvent.ACTION_CANCEL)
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }


}