package com.reco1l.andengine

import android.util.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*


/**
 * Scene with extended functionality.
 */
class ExtendedScene : Scene(), IShape {

    /**
     * The time multiplier for the scene.
     *
     * Setting this will affect the speed of every entity attached to this scene.
     */
    var timeMultiplier = 1f

    /**
     * The pool of modifiers for the scene and its children.
     */
    var modifierPool: Pool<UniversalModifier>? = null
        get() {
            if (field != null) {
                return field
            }

            // Inherit the modifier pool from the parent scene.
            field = (this as IEntity).findHierarchically(IEntity::getParent) {
                (it as? ExtendedScene)?.modifierPool
            }

            return field
        }


    private var cameraWidth = 0f

    private var cameraHeight = 0f


    /**
     * Allocates a modifier pool for the scene.
     */
    @JvmOverloads
    fun allocateModifierPool(size: Int = 0) {
        modifierPool = Pool(size) { UniversalModifier(it) }
    }


    override fun onManagedUpdate(secElapsed: Float) {
        super.onManagedUpdate(secElapsed * timeMultiplier)
    }

    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        cameraWidth = pCamera.width
        cameraHeight = pCamera.height
        super.onManagedDrawChildren(pGL, pCamera)
    }


    override fun registerTouchArea(pTouchArea: ITouchArea) {

        if (pTouchArea == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(pTouchArea)
    }


    override fun onAreaTouched(pSceneTouchEvent: TouchEvent?, pTouchAreaLocalX: Float, pTouchAreaLocalY: Float): Boolean {
        return false
    }

    override fun setCullingEnabled(pCullingEnabled: Boolean) {
        Log.w("ExtendedScene", "Culling is not supported for scenes.")
    }

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        Log.w("ExtendedScene", "Blend functions are not supported for scenes.")
    }


    override fun contains(pX: Float, pY: Float): Boolean = true

    override fun getWidth(): Float = cameraWidth

    override fun getHeight(): Float = cameraHeight

    override fun collidesWith(shape: IShape): Boolean = false

    override fun getBaseWidth(): Float = cameraWidth

    override fun getBaseHeight(): Float = cameraHeight

    override fun getWidthScaled(): Float = cameraWidth * scaleX

    override fun getHeightScaled(): Float = cameraHeight * scaleY

    override fun isCullingEnabled(): Boolean = isCullingEnabled
}