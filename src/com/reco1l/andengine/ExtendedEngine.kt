package com.reco1l.andengine

import android.app.Activity
import android.view.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.input.touch.*

class ExtendedEngine(val context: Activity, options: EngineOptions) : Engine(options) {

    /**
     * The current focused entity.
     */
    var focusedEntity: ExtendedEntity? = null
        set(value) {
            if (value != null && value !is IFocusable) {
                throw IllegalArgumentException("value must be an instance of IFocusable")
            }

            if (field != value) {
                (field as? IFocusable)?.onBlur()
                field = value
                (value as? IFocusable)?.onFocus()
            }
        }


    init {
        Current = this
    }


    /**
     * Called when the skin is changed.
     */
    fun onSkinChange() {
        val scene = scene ?: return

        fun IEntity.propagateSkinChange() {
            callOnChildren { it.propagateSkinChange() }
            if (this is ISkinnable) {
                onSkinChanged()
            }
        }

        scene.propagateSkinChange()
    }

    /**
     * Called when the theme is changed.
     */
    fun onThemeChange(theme: Theme) {
        val scene = scene ?: return

        fun IEntity.propagateThemeChange() {
            callOnChildren { it.propagateThemeChange() }
            if (this is ExtendedEntity) {
                onThemeChanged(theme)
            }
        }

        scene.propagateThemeChange()
    }

    /**
     * Called when a key is pressed.
     */
    fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {

        fun IEntity.propagateKeyPress(keyCode: Int, event: KeyEvent): Boolean {

            if (this is ExtendedEntity && onKeyPress(keyCode, event)) {
                return true
            }

            for (i in childCount - 1 downTo 0) {
                if (getChild(i).propagateKeyPress(keyCode, event)) {
                    return true
                }
            }
            return false
        }

        val scene = scene ?: return false

        if (scene.childScene?.propagateKeyPress(keyCode, event) == true) {
            return true
        }

        return scene.propagateKeyPress(keyCode, event)
    }


    override fun onTouchScene(scene: Scene, event: TouchEvent): Boolean {

        val focusedEntity = focusedEntity
        if (focusedEntity != null) {
            val (left, top) = focusedEntity.convertLocalToSceneCoordinates(0f, 0f)
            val (right, bottom) = focusedEntity.convertLocalToSceneCoordinates(focusedEntity.width, focusedEntity.height)

            if (event.x < left || event.x > right || event.y < top || event.y > bottom) {
                (focusedEntity as IFocusable).blur()
            }
        }

        return super.onTouchScene(scene, event)
    }


    companion object {

        @JvmStatic
        lateinit var Current: ExtendedEngine
            private set

    }
}