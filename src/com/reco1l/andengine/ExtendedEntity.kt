package com.reco1l.andengine

import android.util.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 *
 * @author Reco1l
 */
abstract class ExtendedEntity : Entity(0f, 0f), ITouchArea, IModifierChain {


    /**
     * Determines which axes the entity should automatically adjust its size to.
     *
     * In this case the size will equal to the content size of the entity. Some
     * types of entities requires the user to manually set the size, in those
     * cases this property might be ignored.
     */
    open var autoSizeAxes = Axes.None
        set(value) {
            if (field != value) {
                field = value
                onContentSizeMeasured()
            }
        }

    /**
     * Determines which axes the entity should adjust its size relative to its parent.
     *
     * Depending on the type, the entity's width and height will be taken as a multiplier
     * for the parent's width and height in order to calculate the final size.
     *
     * Example:
     *
     * If [relativeSizeAxes] is set to [Axes.Both] and we set the size to 0.5, the entity's
     * size will be half the size of the parent.
     *
     * Note: [autoSizeAxes] has priority over [relativeSizeAxes]. For example, if [autoSizeAxes]
     * is set to [Axes.Both] and [relativeSizeAxes] is set to [Axes.Both], [relativeSizeAxes]
     * will be ignored.
     */
    open var relativeSizeAxes = Axes.None
        set(value) {
            if (field != value) {
                field = value
                onContentSizeMeasured()
            }
        }

    /**
     * Determines which axes the entity should adjust its position relative to its parent.
     *
     * Depending on the type, the entity's position will be taken as a multiplier applied to
     * the parent's width and height in order to calculate the final position.
     *
     * Example:
     *
     * If [relativePositionAxes] is set to [Axes.Both] and we set the position to 0.5 for both axes,
     * the entity's position will be at the center of the parent.
     */
    open var relativePositionAxes = Axes.None
        set(value) {
            if (field != value) {
                field = value
                invalidateTransformations()
            }
        }

    /**
     * Where the entity should be anchored in the parent.
     */
    open var anchor = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidateTransformations()
            }
        }

    /**
     * Where the entity's origin should be.
     */
    open var origin = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value

                mRotationCenterX = value.x
                mRotationCenterY = value.y
                mScaleCenterX = value.x
                mScaleCenterY = value.y

                invalidateTransformations()
            }
        }

    /**
     * The padding of the entity.
     */
    open var padding = Vec4.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidateTransformations()
            }
        }

    /**
     * The translation in the X axis.
     */
    open var translationX = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateTransformations()
            }
        }

    /**
     * The translation in the Y axis.
     */
    open var translationY = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateTransformations()
            }
        }

    /**
     * The background entity. This entity will be drawn before the entity children and will not be
     * affected by padding.
     */
    open var background: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("ExtendedEntity", "The background entity is already attached to another entity.")
                    return
                }

                field = value
            }
        }

    /**
     * The foreground entity. This entity will be drawn after the entity children and will not be
     * affected by padding.
     */
    open var foreground: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("ExtendedEntity", "The foreground entity is already attached to another entity.")
                    return
                }

                field = value
            }
        }


    /**
     * Whether the entity should clip its children.
     */
    open var clipChildren = false


    /**
     * The depth information of the entity.
     */
    open var depthInfo = DepthInfo.None

    /**
     * The clear information of the entity.
     */
    open var clearInfo = ClearInfo.None

    /**
     * The blend information of the entity.
     */
    open var blendInfo = BlendInfo.Mixture


    /**
     * The color of the entity boxed in a [ColorARGB] object.
     */
    open var color: ColorARGB
        get() = ColorARGB(mRed, mGreen, mBlue, mAlpha)
        set(value) {
            mRed = value.red
            mGreen = value.green
            mBlue = value.blue
            mAlpha = value.alpha
        }

    /**
     * The width of the content inside the entity.
     */
    open var contentWidth = 0f
        protected set(value) {
            if (field != value) {
                field = value
                onContentSizeMeasured()
            }
        }

    /**
     * The height of the content inside the entity.
     */
    open var contentHeight = 0f
        protected set(value) {
            if (field != value) {
                field = value
                onContentSizeMeasured()
            }
        }

    /**
     * The real width of the entity in pixels.
     *
     * Due to compatibility reason, this doesn't take into account transformations like rotation or scaling.
     * @see [getWidthScaled]
     */
    open val drawWidth: Float
        get() {
            if (relativeSizeAxes.isHorizontal) {
                return parent.getPaddedWidth() * internalWidth
            }
            return internalWidth
        }

    /**
     * The real height of the entity in pixels.
     *
     * Due to compatibility reason, this doesn't take into account transformations like rotation or scaling.
     * @see [getHeightScaled]
     */
    open val drawHeight: Float
        get() {
            if (relativeSizeAxes.isVertical) {
                return parent.getPaddedHeight() * internalHeight
            }
            return internalHeight
        }

    /**
     * The raw X position of the entity.
     * This is the position without taking into account the origin, anchor, or translation.
     */
    open val drawX: Float
        get() {
            val parent = parent
            if (parent is Container) {
                return parent.getChildDrawX(this)
            }

            var x = x
            if (relativePositionAxes.isHorizontal) {
                x *= parent.getPaddedWidth()
            }

            return x + totalOffsetX
        }


    /**
     * The raw Y position of the entity.
     * This is the position without taking into account the origin, anchor, or translation.
     */
    open val drawY: Float
        get() {
            val parent = parent
            if (parent is Container) {
                return parent.getChildDrawY(this)
            }

            var y = y
            if (relativePositionAxes.isVertical) {
                y *= parent.getPaddedHeight()
            }

            return y + totalOffsetY
        }

    var width
        get() = internalWidth
        set(value) {
            if (autoSizeAxes.isHorizontal) {
                autoSizeAxes = if (autoSizeAxes == Axes.Both) Axes.Y else Axes.None
            }

            if (internalWidth != value) {
                internalWidth = value
                onSizeChanged()
            }
        }

    var height
        get() = internalHeight
        set(value) {
            if (autoSizeAxes.isVertical) {
                autoSizeAxes = if (autoSizeAxes == Axes.Both) Axes.X else Axes.None
            }

            if (internalHeight != value) {
                internalHeight = value
                onSizeChanged()
            }
        }


    var internalWidth = 0f

    var internalHeight = 0f

    private var isVertexBufferDirty = true


    // Attachment

    override fun setParent(pEntity: IEntity?) {

        val parent = parent
        if (parent is Scene) {
            parent.unregisterTouchArea(this)
        }

        super.setParent(pEntity)

        if (pEntity is ExtendedScene) {
            pEntity.registerTouchArea(this)
        }
    }


    // Positions

    open fun onPositionChanged() {
        invalidateTransformations()
        (parent as? Container)?.onChildPositionChanged(this)
    }

    override fun setPosition(x: Float, y: Float) {
        if (mX != x || mY != y) {
            mX = x
            mY = y
            onPositionChanged()
        }
    }

    open fun setX(value: Float) {
        if (mX != value) {
            mX = value
            onPositionChanged()
        }
    }

    open fun setY(value: Float) {
        if (mY != value) {
            mY = value
            onPositionChanged()
        }
    }

    open fun setTranslation(x: Float, y: Float) {
        if (translationX != x || translationY != y) {
            translationX = x
            translationY = y
            invalidateTransformations()
        }
    }

    open fun invalidateTransformations() {
        mLocalToParentTransformationDirty = true
        mParentToLocalTransformationDirty = true

        mChildren?.fastForEach {
            (it as? ExtendedEntity)?.invalidateTransformations()
        }
    }


    // Drawing

    override fun applyTranslation(pGL: GL10, camera: Camera) {

        val drawX = this.drawX
        val drawY = this.drawY

        if (drawX != 0f || drawY != 0f) {
            pGL.glTranslatef(drawX, drawY, 0f)
        }
    }

    override fun applyRotation(pGL: GL10) {

        if (rotation == 0f) {
            return
        }

        val offsetX = drawWidth * mRotationCenterX
        val offsetY = drawHeight * mRotationCenterY

        if (offsetX > 0f || offsetY > 0f) {
            pGL.glTranslatef(offsetX, offsetY, 0f)
            pGL.glRotatef(rotation, 0f, 0f, 1f)
            pGL.glTranslatef(-offsetX, -offsetY, 0f)
        } else {
            pGL.glRotatef(rotation, 0f, 0f, 1f)
        }
    }

    override fun applyScale(pGL: GL10) {

        if (scaleX == 1f && scaleY == 1f) {
            return
        }

        val offsetX = drawWidth * mScaleCenterX
        val offsetY = drawHeight * mScaleCenterY

        if (offsetX > 0f || offsetY > 0f) {
            pGL.glTranslatef(offsetX, offsetY, 0f)
            pGL.glScalef(scaleX, scaleY, 1f)
            pGL.glTranslatef(-offsetX, -offsetY, 0f)
        } else {
            pGL.glScalef(scaleX, scaleY, 1f)
        }
    }

    protected open fun applyColor(pGL: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha

        var parent = parent
        while (parent != null) {

            red *= parent.red
            green *= parent.green
            blue *= parent.blue
            alpha *= parent.alpha

            // We'll assume at this point there's no need to keep multiplying.
            if (red == 0f && green == 0f && blue == 0f && alpha == 0f) {
                break
            }

            parent = parent.parent
        }

        GLHelper.setColor(pGL, red, green, blue, alpha)
    }

    protected open fun applyBlending(pGL: GL10) {

        var sourceFactor = BLENDFUNCTION_SOURCE_DEFAULT
        var destinationFactor = BLENDFUNCTION_DESTINATION_DEFAULT

        if (blendInfo == BlendInfo.Inherit) {
            val parent = parent

            if (parent is ExtendedEntity) {
                sourceFactor = parent.blendInfo.sourceFactor
                destinationFactor = parent.blendInfo.destinationFactor
            }
        } else {
            sourceFactor = blendInfo.sourceFactor
            destinationFactor = blendInfo.destinationFactor
        }

        GLHelper.enableBlend(pGL)
        GLHelper.blendFunction(pGL, sourceFactor, destinationFactor)
    }

    override fun onApplyTransformations(pGL: GL10, camera: Camera) {
        applyTranslation(pGL, camera)
        applyRotation(pGL)
        applyScale(pGL)
        applyColor(pGL)
        applyBlending(pGL)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {

        val hasPaddingApplicable = padding.left > 0f || padding.top > 0f

        if (hasPaddingApplicable) {
            gl.glTranslatef(padding.left, padding.top, 0f)
        }

        if (clipChildren) {
            GLHelper.enableScissorTest(gl)

            var (bottomLeftX, bottomLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            var (topLeftX, topLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, getPaddedHeight()))
            var (topRightX, topRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(getPaddedWidth(), getPaddedHeight()))
            var (bottomRightX, bottomRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(getPaddedWidth(), 0f))

            // Flip the Y axis to match the OpenGL coordinate system.
            bottomLeftY = camera.surfaceHeight - bottomLeftY
            topLeftY = camera.surfaceHeight - topLeftY
            topRightY = camera.surfaceHeight - topRightY
            bottomRightY = camera.surfaceHeight - bottomRightY

            val minClippingX = minOf(bottomLeftX, topLeftX, topRightX, bottomRightX)
            val minClippingY = minOf(bottomLeftY, topLeftY, topRightY, bottomRightY)

            val maxClippingX = maxOf(bottomLeftX, topLeftX, topRightX, bottomRightX)
            val maxClippingY = maxOf(bottomLeftY, topLeftY, topRightY, bottomRightY)

            gl.glScissor(
                minClippingX.toInt(),
                minClippingY.toInt(),
                (maxClippingX - minClippingX).toInt(),
                (maxClippingY - minClippingY).toInt()
            )
        }

        super.onDrawChildren(gl, camera)

        if (clipChildren) {
            GLHelper.disableScissorTest(gl)
        }

        if (hasPaddingApplicable) {
            gl.glTranslatef(-padding.right, -padding.top, 0f)
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        gl.glPushMatrix()

        onApplyTransformations(gl, camera)

        background?.setSize(drawWidth, drawHeight)
        background?.onDraw(gl, camera)

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        foreground?.setSize(drawWidth, drawHeight)
        foreground?.onDraw(gl, camera)

        gl.glPopMatrix()
    }

    open fun beginDraw(gl: GL10) {

        // We haven't done any culling implementation so we disable it globally for all buffered entities.
        GLHelper.disableCulling(gl)
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)

        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GL10.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GL10.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GL10.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            gl.glClear(clearMask)
        }

        if (depthInfo.test) {
            gl.glDepthFunc(depthInfo.function)
            gl.glDepthMask(depthInfo.mask)

            GLHelper.enableDepthTest(gl)
        } else {
            GLHelper.disableDepthTest(gl)
        }
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        beginDraw(gl)
    }


    // Update

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        background?.onManagedUpdate(pSecondsElapsed)
        foreground?.onManagedUpdate(pSecondsElapsed)

        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun reset() {
        super.reset()
        blendInfo = BlendInfo.Mixture
    }


    // Size

    open fun onSizeChanged() {
        invalidateTransformations()
        (parent as? Container)?.onChildSizeChanged(this)
    }


    /**
     * Called when the content size is measured.
     *
     * @return Whether the size of the entity was changed or not, this depends on the [autoSizeAxes] property.
     */
    open fun onContentSizeMeasured(): Boolean {

        if (autoSizeAxes == Axes.None) {
            return false
        }

        if (contentWidth != internalWidth || contentHeight != internalHeight) {

            if (autoSizeAxes.isHorizontal) {
                internalWidth = contentWidth + padding.horizontal

                if (relativeSizeAxes.isHorizontal) {
                    internalWidth /= parent.getPaddedWidth()
                }
            }

            if (autoSizeAxes.isVertical) {
                internalHeight = contentHeight + padding.vertical

                if (relativeSizeAxes.isVertical) {
                    internalHeight /= parent.getPaddedHeight()
                }
            }
            onSizeChanged()
            return true
        }
        return false
    }


    override fun setRotation(pRotation: Float) {
        if (mRotation != pRotation) {
            mRotation = pRotation
            invalidateTransformations()
        }
    }

    /**
     * Sets the size of the entity.
     *
     * Note: This will change the [autoSizeAxes] property to [Axes.None] automatically.
     *
     * @return Whether the size of the entity was changed or not.
     */
    open fun setSize(newWidth: Float, newHeight: Float): Boolean {

        if (autoSizeAxes != Axes.None) {
            autoSizeAxes = Axes.None
        }

        if (internalWidth != newWidth || internalHeight != newHeight) {
            internalWidth = newWidth
            internalHeight = newHeight
            onSizeChanged()
            return true
        }
        return false
    }


    fun getWidthScaled(): Float {
        return drawWidth * scaleX
    }

    fun getHeightScaled(): Float {
        return drawHeight * scaleY
    }

    // Collision

    override fun contains(x: Float, y: Float): Boolean {

        if (drawWidth == 0f || drawHeight == 0f) {
            return false
        }

        return EntityCollision.contains(this, x, y, parent is Scene)
    }

    // Transformation

    override fun getLocalToParentTransformation(): Transformation {

        if (mLocalToParentTransformation == null) {
            mLocalToParentTransformation = Transformation()
        }

        if (mLocalToParentTransformationDirty) {
            mLocalToParentTransformation.setToIdentity()

            if (scaleX != 1f || scaleY != 1f) {
                val offsetX = drawWidth * mScaleCenterX
                val offsetY = drawHeight * mScaleCenterY

                mLocalToParentTransformation.postTranslate(-offsetX, -offsetY)
                mLocalToParentTransformation.postScale(scaleX, scaleY)
                mLocalToParentTransformation.postTranslate(offsetX, offsetY)
            }

            if (rotation != 0f) {
                val offsetX = drawWidth * mRotationCenterX
                val offsetY = drawHeight * mRotationCenterY

                mLocalToParentTransformation.postTranslate(-offsetX, -offsetY)
                mLocalToParentTransformation.postRotate(rotation)
                mLocalToParentTransformation.postTranslate(offsetX, offsetY)
            }

            mLocalToParentTransformation.postTranslate(drawX, drawY)
            mLocalToParentTransformationDirty = false
        }

        return mLocalToParentTransformation
    }

    override fun getParentToLocalTransformation(): Transformation {

        if (mParentToLocalTransformation == null) {
            mParentToLocalTransformation = Transformation()
        }

        if (mParentToLocalTransformationDirty) {
            mParentToLocalTransformation.setToIdentity()
            mParentToLocalTransformation.postTranslate(-drawX, -drawY)

            if (rotation != 0f) {
                val offsetX = drawWidth * mRotationCenterX
                val offsetY = drawHeight * mRotationCenterY

                mParentToLocalTransformation.postTranslate(-offsetX, -offsetY)
                mParentToLocalTransformation.postRotate(-rotation)
                mParentToLocalTransformation.postTranslate(offsetX, offsetY)
            }

            if (scaleX != 1f || scaleY != 1f) {
                val offsetX = drawWidth * mScaleCenterX
                val offsetY = drawHeight * mScaleCenterY

                mParentToLocalTransformation.postTranslate(-offsetX, -offsetY)
                mParentToLocalTransformation.postScale(1 / scaleX, 1 / scaleY)
                mParentToLocalTransformation.postTranslate(offsetX, offsetY)
            }

            mParentToLocalTransformationDirty = false
        }

        return mParentToLocalTransformation
    }


    open fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        blendInfo = BlendInfo(pSourceBlendFunction, pDestinationBlendFunction)
    }

    override fun applyModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        val modifier = UniversalModifier.GlobalPool.obtain()
        modifier.setToDefault()
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }


    //region Input

    private val inputBindings = arrayOfNulls<ExtendedEntity>(10)


    open fun invalidateInputBindings(recursively: Boolean = true) {
        inputBindings.fill(null)

        if (recursively) {
            mChildren?.fastForEach {
                (it as? ExtendedEntity)?.invalidateInputBindings()
            }
        }
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding != null && inputBinding.parent == this) {
            if (!inputBinding.onAreaTouched(event, localX - inputBinding.drawX, localY - inputBinding.drawY) || event.isActionUp) {
                inputBindings[event.pointerID] = null
                return false
            }
            return true
        } else {
            inputBindings[event.pointerID] = null
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)
                if (child is ExtendedEntity && child.contains(localX, localY)) {
                    if (child.onAreaTouched(event, localX - child.drawX, localY - child.drawY)) {
                        inputBindings[event.pointerID] = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("ExtendedEntity", "A child entity was removed during touch event propagation.", e)
        }

        return false
    }

    //endregion

}
