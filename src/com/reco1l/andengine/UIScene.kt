package com.reco1l.andengine

import android.util.Log
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.*
import javax.microedition.khronos.opengles.GL10
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.util.GLHelper


/**
 * Scene with extended functionality.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
open class UIScene : Scene(), IShape {

    /**
     * The time multiplier for the scene.
     *
     * Setting this will affect the speed of every entity attached to this scene.
     */
    var timeMultiplier = 1f

    /**
     * Whether this [UIScene] should clip its children.
     */
    open var clipToBounds = false


    private var cameraWidth = 0f

    private var cameraHeight = 0f


    init {
        super.setTouchAreaBindingEnabled(true)
        super.setOnAreaTouchTraversalFrontToBack()
    }


    //region Update

    override fun onManagedUpdate(deltaTimeSec: Float) {
        super.onManagedUpdate(deltaTimeSec * timeMultiplier)
    }

    override fun setChildScene(childScene: Scene?, modalDraw: Boolean, modalUpdate: Boolean, modalTouch: Boolean) {
        childScene?.onDetached()
        super.setChildScene(childScene, modalDraw, modalUpdate, modalTouch)
        childScene?.onAttached()
    }

    override fun clearChildScene() {
        childScene?.onDetached()
        super.clearChildScene()
    }

    override fun onAttached() {

        fun IEntity.propagateSkinChanges() {

            if (this is UIComponent) {
                onThemeChanged(Theme.current)
            }

            if (this is ISkinnable) {
                onSkinChanged()
            }

            forEach { it.propagateSkinChanges() }
        }

        propagateSkinChanges()
    }

    //endregion

    //region Drawing

    override fun onDraw(gl: GL10, camera: Camera) {
        if (!isVisible) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = GLHelper.isEnableScissorTest()
            GLHelper.enableScissorTest(gl)

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            onManagedDraw(gl, camera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                GLHelper.disableScissorTest(gl)
            }
        } else {
            onManagedDraw(gl, camera)
        }
    }

    override fun onManagedDrawChildren(gl: GL10, camera: Camera) {
        cameraWidth = camera.widthRaw
        cameraHeight = camera.heightRaw
        super.onManagedDrawChildren(gl, camera)
    }

    //endregion

    //region Input

    override fun registerTouchArea(area: ITouchArea) {

        if (area == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(area)
    }

    override fun setTouchAreaBindingEnabled(pTouchAreaBindingEnabled: Boolean) {
        Log.w("ExtendedScene", "Touch area binding is always enabled for ExtendedScene.")
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        // This should never be called, as the scene itself is not a touch area.
        return true
    }

    //endregion

    //region Delegation

    override fun contains(pX: Float, pY: Float): Boolean = true

    override fun getWidth(): Float = cameraWidth

    override fun getHeight(): Float = cameraHeight

    override fun collidesWith(shape: IShape): Boolean = false

    override fun getBaseWidth(): Float = cameraWidth

    override fun getBaseHeight(): Float = cameraHeight

    override fun getWidthScaled(): Float = cameraWidth * scaleX

    override fun getHeightScaled(): Float = cameraHeight * scaleY

    override fun isCullingEnabled(): Boolean = false

    //endregion

    //region Unsupported methods

    override fun setColor(pRed: Float, pGreen: Float, pBlue: Float) {
        Log.w("ExtendedScene", "Color is not supported for scenes.")
    }

    override fun setCullingEnabled(pCullingEnabled: Boolean) {
        Log.w("ExtendedScene", "Culling is not supported for scenes.")
    }

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        Log.w("ExtendedScene", "Blend functions are not supported for scenes.")
    }

    //endregion

    open fun show() {
        ExtendedEngine.Current.scene = this
    }
}