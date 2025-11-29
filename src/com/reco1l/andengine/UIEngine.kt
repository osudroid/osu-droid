package com.reco1l.andengine

import android.app.Activity
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.hud.*
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

class UIEngine(val context: Activity, options: EngineOptions) : Engine(options) {

    /**
     * The global HUD used for overlays (menus, dialogs, etc).
     */
    val overlay = HUD()

    /**
     * The resource manager for loading and accessing UI resources (fonts, textures, etc).
     */
    val resources = UIResourceManager(context)

    /**
     * The current focused entity.
     */
    var focusedEntity: UIComponent? = null
        set(value) {
            if (value != null && value !is IFocusable) {
                throw IllegalArgumentException("value must be an instance of IFocusable")
            }

            if (field != value) {
                (field as? IFocusable)?.onBlur()
                field = value

                if (value != null) {
                    value.onFocus()

                    val (_, sceneY) = value.convertLocalToSceneCoordinates(0f, value.height)
                    val (_, surfaceY) = camera.convertSceneToSurfaceCoordinates(0f, sceneY, invertYAxis = false)

                    focusedEntitySurfacePositionY = surfaceY
                }
            }
        }


    private var focusedEntitySurfacePositionY = 0f


    init {
        current = this
        camera.hud = overlay
    }


    override fun onDrawScene(pGL: GL10) {

        val focusedEntity = focusedEntity

        if (focusedEntity != null) {

            if (focusedEntity is UITextInput) {

                val keyboardHeight = ViewCompat.getRootWindowInsets(context.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())
                    ?.bottom
                    ?.toFloat() ?: 0f

                val offset = max(0f, focusedEntitySurfacePositionY - (surfaceHeight - keyboardHeight))

                val (_, sceneOffset) = camera.convertSurfaceToSceneCoordinates(0f, offset)

                // Current scene isn't supposed to have any other position rather than 0,0
                // so we assume it is safe to override the position.
                scene.setPosition(0f, -sceneOffset)
                scene.childScene?.setPosition(0f, -sceneOffset)
                overlay.setPosition(0f, -sceneOffset)
            }

        } else {
            scene.setPosition(0f, 0f)
            scene.childScene?.setPosition(0f, 0f)
            overlay.setPosition(0f, 0f)
        }

        super.onDrawScene(pGL)
    }


    /**
     * Called when the skin is changed.
     */
    fun onSkinChange() {
        val scene = scene ?: return

        fun IEntity.propagateSkinChange() {

            if (this is ISkinnable) {
                onSkinChanged()
            }

            forEach { it.propagateSkinChange() }
        }

        scene.propagateSkinChange()
    }

    /**
     * Called when the theme is changed.
     */
    fun onThemeChange(theme: Theme) {
        val scene = scene ?: return

        fun IEntity.propagateThemeChange() {

            if (this is UIComponent) {
                onThemeChanged(theme)
            }

            if (this is Scene) {
                childScene?.propagateThemeChange()
            }

            forEach { it.propagateThemeChange() }
        }

        scene.propagateThemeChange()
    }

    /**
     * Called when a key is pressed.
     */
    fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {

        fun IEntity.propagateKeyPress(keyCode: Int, event: KeyEvent): Boolean {

            if (this is UIComponent && onKeyPress(keyCode, event)) {
                return true
            }

            for (i in childCount - 1 downTo 0) {
                if (getChild(i).propagateKeyPress(keyCode, event)) {
                    return true
                }
            }

            return false
        }

        if (overlay.propagateKeyPress(keyCode, event)) {
            return true
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
            return true
        }

        return super.onTouchScene(scene, event)
    }


    override fun setScene(scene: Scene?) {
        mScene?.onDetached()
        super.setScene(scene)
        scene?.onAttached()
    }


    companion object {

        @JvmStatic
        lateinit var current: UIEngine
            private set

    }
}