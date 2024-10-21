package com.reco1l.andengine

import android.util.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*


/**
 * Scene with extended functionality.
 */
open class ExtendedScene : Scene(), IShape {

    /**
     * The time multiplier for the scene.
     *
     * Setting this will affect the speed of every entity attached to this scene.
     */
    var timeMultiplier = 1f


    private var cameraWidth = 0f

    private var cameraHeight = 0f


    override fun onManagedUpdate(secElapsed: Float) {
        super.onManagedUpdate(secElapsed * timeMultiplier)
    }

    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        cameraWidth = pCamera.widthRaw
        cameraHeight = pCamera.heightRaw
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


    override fun setColor(pRed: Float, pGreen: Float, pBlue: Float) {
        Log.w("ExtendedScene", "Color is not supported for scenes.")
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

    override fun isCullingEnabled(): Boolean = false
}