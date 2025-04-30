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


    fun onSkinChange() {
        val scene = scene ?: return

        fun IEntity.updateSkin() {
            callOnChildren { it.updateSkin() }

            if (this is ISkinnable) {
                onSkinChanged()
            }
        }

        scene.updateSkin()
    }


    /**
     * Sets the current theme for the engine's properties.
     */
    fun onThemeChange(theme: Theme) {
        val scene = scene ?: return

        fun IEntity.updateTheme() {
            callOnChildren { it.updateTheme() }

            if (this is ExtendedEntity) {
                onThemeChanged(theme)
            }
        }

        scene.updateTheme()
    }

    /**
     * Called when a key is pressed.
     */
    fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {

        fun IEntity.onKeyPress(keyCode: Int, event: KeyEvent): Boolean {

            for (i in childCount - 1 downTo 0) {
                if (getChild(i).onKeyPress(keyCode, event)) {
                    return true
                }
            }

            if (this is ExtendedEntity) {
                onKeyPress(keyCode, event)
            }
            return false
        }

        return scene?.onKeyPress(keyCode, event) ?: false
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