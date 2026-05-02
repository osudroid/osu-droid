package com.reco1l.andengine

import android.util.Log
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.*
import org.andengine.engine.camera.Camera
import org.andengine.entity.IEntity
import org.andengine.entity.scene.ITouchArea
import org.andengine.entity.scene.Scene
import org.andengine.entity.shape.IShape
import org.andengine.input.touch.TouchEvent
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.IVertexBufferObject
import org.andengine.opengl.vbo.VertexBufferObjectManager


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

    //region Events

    /**
     * Called every update thread tick, avoid heavy operations here.
     */
    var onUpdateTick: OnUpdateEvent? = null

    //endregion


    init {
        super.setTouchAreaBindingOnActionDownEnabled(true)
        super.setOnAreaTouchTraversalFrontToBack()
    }


    //region Update

    override fun onManagedUpdate(deltaTimeSec: Float) {
        onUpdateTick?.invoke(deltaTimeSec * timeMultiplier)
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

    override fun onManagedDraw(pGLState: GLState, pCamera: Camera) {
        cameraWidth = pCamera.widthRaw
        cameraHeight = pCamera.heightRaw

        if (!isVisible) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = pGLState.isScissorTestEnabled

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(cameraWidth, 0f))
            val (bottomRightX, bottomRightY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(cameraWidth, cameraHeight))
            val (bottomLeftX, bottomLeftY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, cameraHeight))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            pGLState.enableScissorTest()
            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            super.onManagedDraw(pGLState, pCamera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                pGLState.disableScissorTest()
            }
        } else {
            super.onManagedDraw(pGLState, pCamera)
        }
    }

    //endregion

    //region Input

    override fun registerTouchArea(area: ITouchArea) {

        if (area == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(area)
    }

    override fun setTouchAreaBindingOnActionDownEnabled(pTouchAreaBindingEnabled: Boolean) {
        Log.w("ExtendedScene", "Touch area binding is always enabled for ExtendedScene.")
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        // This should never be called, as the scene itself is not a touch area.
        return true
    }

    //endregion

    //region Delegation

    override fun contains(pX: Float, pY: Float): Boolean = true

    override fun collidesWith(shape: IShape): Boolean = false

    override fun isCullingEnabled(): Boolean = false

    //endregion

    //region IShape stubs

    override fun isBlendingEnabled(): Boolean = true

    override fun setBlendingEnabled(pBlendingEnabled: Boolean) { }

    override fun getBlendFunctionSource(): Int = IShape.BLENDFUNCTION_SOURCE_DEFAULT

    override fun getBlendFunctionDestination(): Int = IShape.BLENDFUNCTION_DESTINATION_DEFAULT

    override fun setBlendFunctionSource(pBlendFunctionSource: Int) { }

    override fun setBlendFunctionDestination(pBlendFunctionDestination: Int) { }

    override fun getVertexBufferObjectManager(): VertexBufferObjectManager? = null

    override fun getVertexBufferObject(): IVertexBufferObject? = null

    override fun getShaderProgram(): ShaderProgram? = null

    override fun setShaderProgram(pShaderProgram: ShaderProgram?) { }

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
        UIEngine.current.scene = this
    }
}