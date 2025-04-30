package com.reco1l.andengine

import android.app.Activity
import android.view.*
import com.osudroid.BuildSettings
import com.osudroid.debug.EntityDescriptor
import com.osudroid.debug.EntityInspector
import com.reco1l.andengine.ui.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*

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


    private val overlayEntities = mutableListOf<ExtendedEntity>()

    private var boundEntity: ExtendedEntity? = null


    init {
        Current = this

        runOnUpdateThread {
            if (BuildSettings.ENTITY_INSPECTOR) {
                overlayEntities += EntityInspector(this).apply {
                    isExpanded = false
                }
                overlayEntities += EntityDescriptor().apply {
                    x = 400f
                    isExpanded = false
                }
            }
        }
    }

    fun setTheme(theme: Theme) {

        val scene = scene ?: return

        fun IEntity.updateTheme() {

            for (i in 0 until childCount) {
                getChild(i).updateTheme()
            }

            if (this is ExtendedEntity) {
                onThemeChanged(theme)
            }
        }

        scene.updateTheme()
    }


    override fun onDrawScene(pGL: GL10) {
        super.onDrawScene(pGL)

        if (overlayEntities.isNotEmpty()) {

            // If there's a HUD this is needed to prevent the overlay entities from being drawn wrongly.
            if (camera.hasHUD()) {
                camera.onApplySceneMatrix(pGL)
            }

            overlayEntities.fastForEach { entity ->
                entity.onDraw(pGL, camera)
            }
        }
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

    override fun onTouchHUD(camera: Camera, event: TouchEvent): Boolean {

        if (overlayEntities.isNotEmpty()) {
            camera.convertSceneToCameraSceneTouchEvent(event)

            if (boundEntity != null) {
                boundEntity?.onAreaTouched(event, event.x - boundEntity!!.absoluteX, event.y - boundEntity!!.absoluteY)

                if (event.isActionUp) {
                    boundEntity = null
                }
                return true
            }

            for (entity in overlayEntities.reversed()) {
                if (entity.contains(event.x, event.y) && entity.onAreaTouched(event, event.x - entity.absoluteX, event.y - entity.absoluteY)) {
                    boundEntity = entity
                    return true
                }
            }

            camera.convertCameraSceneToSceneTouchEvent(event)
        }

        return super.onTouchHUD(camera, event)
    }

    override fun onUpdateScene(pSecondsElapsed: Float) {

        overlayEntities.fastForEach { entity ->
            entity.onUpdate(pSecondsElapsed)
        }

        super.onUpdateScene(pSecondsElapsed)
    }


    private fun IEntity.onKeyPress(keyCode: Int, event: KeyEvent): Boolean {

        for (i in 0 until childCount) {
            if (getChild(i).onKeyPress(keyCode, event)) {
                return true
            }
        }

        if (this is ExtendedEntity) {
            onKeyPress(keyCode, event)
        }
        return false
    }

    /**
     * Called when a key is pressed.
     */
    fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {
        return scene?.onKeyPress(keyCode, event) ?: false
    }


    companion object {

        @JvmStatic
        lateinit var Current: ExtendedEngine
            private set

    }
}