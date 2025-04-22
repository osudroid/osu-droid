package com.reco1l.andengine

import com.osudroid.BuildSettings
import com.osudroid.debug.EntityDescriptor
import com.osudroid.debug.EntityInspector
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.engine.options.EngineOptions
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*

class ExtendedEngine(options: EngineOptions) : Engine(options) {

    private val overlayEntities = mutableListOf<ExtendedEntity>()

    private var boundEntity: ExtendedEntity? = null


    init {
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

}