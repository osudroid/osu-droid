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


    init {
        super.setTouchAreaBindingEnabled(true)
        super.setOnAreaTouchTraversalFrontToBack()
    }


    // Update

    override fun onManagedUpdate(secElapsed: Float) {
        super.onManagedUpdate(secElapsed * timeMultiplier)
    }


    // Drawing

    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        cameraWidth = pCamera.widthRaw
        cameraHeight = pCamera.heightRaw
        super.onManagedDrawChildren(pGL, pCamera)
    }


    // Input

    override fun registerTouchArea(area: ITouchArea) {

        if (area == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(area)
    }

    override fun setTouchAreaBindingEnabled(pTouchAreaBindingEnabled: Boolean) {
        Log.w("ExtendedScene", "Touch area binding is always enabled for ExtendedScene.")
        super.setTouchAreaBindingEnabled(true)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        // This should never be called, as the scene itself is not a touch area.
        return true
    }


    // Unsupported methods

    override fun setColor(pRed: Float, pGreen: Float, pBlue: Float) {
        Log.w("ExtendedScene", "Color is not supported for scenes.")
    }

    override fun setCullingEnabled(pCullingEnabled: Boolean) {
        Log.w("ExtendedScene", "Culling is not supported for scenes.")
    }

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        Log.w("ExtendedScene", "Blend functions are not supported for scenes.")
    }


    // IShape interface

    override fun contains(pX: Float, pY: Float): Boolean {
        return true
    }

    override fun getWidth(): Float {
        return cameraWidth
    }

    override fun getHeight(): Float {
        return cameraHeight
    }

    override fun collidesWith(shape: IShape): Boolean {
        return false
    }

    override fun getBaseWidth(): Float {
        return cameraWidth
    }

    override fun getBaseHeight(): Float {
        return cameraHeight
    }

    override fun getWidthScaled(): Float {
        return cameraWidth * scaleX
    }

    override fun getHeightScaled(): Float {
        return cameraHeight * scaleY
    }

    override fun isCullingEnabled(): Boolean {
        return false
    }
}