package com.reco1l.andengine

import android.util.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.framework.math.Vec4
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.Scene.ITouchArea
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import org.anddev.andengine.util.Transformation
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 *
 * @author Reco1l
 */
abstract class ExtendedEntity(

    private var vertexBuffer: VertexBuffer? = null

) : Shape(0f, 0f), IModifierChain {

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
    open var depthInfo: DepthInfo? = null

    /**
     * The blending information of the entity.
     */
    open var blendInfo: BlendInfo? = BlendInfo.Default
        set(value) {
            if (field != value) {
                if (value != null) {
                    mSourceBlendFunction = value.function.source
                    mDestinationBlendFunction = value.function.destination
                }
                field = value
            }
        }

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
                return parent.getPaddedWidth() * width
            }
            return width
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
                return parent.getPaddedHeight() * height
            }
            return height
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


    private var width = 0f

    private var height = 0f

    private var isVertexBufferDirty = true

    private var currentBoundEntity: ITouchArea? = null


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

    override fun setPosition(x: Float, y: Float) {
        if (mX != x || mY != y) {
            mX = x
            mY = y
            invalidateTransformations()
            (parent as? Container)?.onChildPositionChanged(this)
        }
    }

    open fun setX(value: Float) {
        if (mX != value) {
            mX = value
            invalidateTransformations()
            (parent as? Container)?.onChildPositionChanged(this)
        }
    }

    open fun setY(value: Float) {
        if (mY != value) {
            mY = value
            invalidateTransformations()
            (parent as? Container)?.onChildPositionChanged(this)
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

        val blendInfo = blendInfo

        if (blendInfo == null) {
            GLHelper.blendFunction(pGL, mSourceBlendFunction, mDestinationBlendFunction)
            return
        }

        blendInfo.apply(pGL)
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

        if (isVertexBufferDirty) {
            isVertexBufferDirty = false
            onUpdateVertexBuffer()
        }

        gl.glPushMatrix()

        if (!isCullingEnabled || !isCulled(camera)) {
            onApplyTransformations(gl, camera)

            background?.setSize(drawWidth, drawHeight)
            background?.onDraw(gl, camera)

            doDraw(gl, camera)
            onDrawChildren(gl, camera)

            foreground?.setSize(drawWidth, drawHeight)
            foreground?.onDraw(gl, camera)
        }

        gl.glPopMatrix()
    }

    override fun onInitDraw(pGL: GL10) {

        if (vertexBuffer != null) {
            GLHelper.enableVertexArray(pGL)
        }

        depthInfo?.apply(pGL) ?: GLHelper.disableDepthTest(pGL)
    }

    override fun onApplyVertices(pGL: GL10) {
        if (vertexBuffer != null) {
            super.onApplyVertices(pGL)
        }
    }


    // Update

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        background?.onManagedUpdate(pSecondsElapsed)
        foreground?.onManagedUpdate(pSecondsElapsed)

        super.onManagedUpdate(pSecondsElapsed)
    }


    // Vertex buffer

    override fun updateVertexBuffer() {
        isVertexBufferDirty = true
    }

    fun updateVertexBufferNow() {
        isVertexBufferDirty = false
        onUpdateVertexBuffer()
    }

    /**
     * Sets the vertex buffer of the entity.
     *
     * Note: This will unload the previous buffer from the active buffer object manager if it's managed.
     * If it's not managed you will have to manually unload it otherwise it will cause a memory leak.
     */
    fun setVertexBuffer(buffer: VertexBuffer) {
        vertexBuffer?.unloadFromActiveBufferObjectManager()
        vertexBuffer = buffer
        updateVertexBuffer()
    }

    override fun getVertexBuffer(): VertexBuffer? {
        return vertexBuffer
    }


    // Size

    /**
     * Called when the content size is measured.
     *
     * @return Whether the size of the entity was changed or not, this depends on the [autoSizeAxes] property.
     */
    open fun onContentSizeMeasured(): Boolean {

        if (autoSizeAxes == Axes.None) {
            return false
        }

        if (contentWidth != width || contentHeight != height) {

            if (autoSizeAxes.isHorizontal) {
                width = contentWidth + padding.horizontal

                if (relativeSizeAxes.isHorizontal) {
                    width /= parent.getPaddedWidth()
                }
            }

            if (autoSizeAxes.isVertical) {
                height = contentHeight + padding.vertical

                if (relativeSizeAxes.isVertical) {
                    height /= parent.getPaddedHeight()
                }
            }

            updateVertexBuffer()
            invalidateTransformations()

            (parent as? Container)?.onChildSizeChanged(this)
            return true
        }
        return false
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

        if (width != newWidth || height != newHeight) {
            width = newWidth
            height = newHeight

            updateVertexBuffer()
            invalidateTransformations()

            val parent = parent
            if (parent is Container) {
                parent.onChildSizeChanged(this)
            }

            return true
        }
        return false
    }

    open fun setWidth(value: Float) {

        if (autoSizeAxes.isHorizontal) {
            autoSizeAxes = if (autoSizeAxes == Axes.Both) Axes.Y else Axes.None
        }

        if (width != value) {
            width = value

            updateVertexBuffer()
            invalidateTransformations()
            (parent as? Container)?.onChildSizeChanged(this)
        }
    }

    open fun setHeight(value: Float) {

        if (autoSizeAxes.isVertical) {
            autoSizeAxes = if (autoSizeAxes == Axes.Both) Axes.X else Axes.None
        }

        if (height != value) {
            height = value

            updateVertexBuffer()
            invalidateTransformations()
            (parent as? Container)?.onChildSizeChanged(this)
        }
    }

    override fun getWidth(): Float {
        return width
    }

    override fun getHeight(): Float {
        return height
    }

    override fun getWidthScaled(): Float {
        return drawWidth * scaleX
    }

    override fun getHeightScaled(): Float {
        return drawHeight * scaleY
    }


    // Unsupported methods

    @Deprecated("Base width is not preserved in ExtendedEntity, use getWidth() instead.")
    override fun getBaseWidth() = width

    @Deprecated("Base height is not preserved in ExtendedEntity, use getHeight() instead.")
    override fun getBaseHeight() = height


    // Collision

    override fun collidesWith(shape: IShape): Boolean {
        Log.w("ExtendedEntity", "Collision detection is not supported in ExtendedEntity.")
        return false
    }

    override fun contains(x: Float, y: Float): Boolean {

        if (drawWidth == 0f || drawHeight == 0f) {
            return false
        }

        return EntityCollision.contains(this, x, y, parent is Scene)
    }

    override fun isCulled(pCamera: Camera): Boolean {
        return drawX > pCamera.maxX || drawX + drawWidth < pCamera.minX
            || drawY > pCamera.maxY || drawY + drawHeight < pCamera.minY
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


    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        // We have to nullify the blend info to prevent these values from being overridden.
        if (blendInfo != null) {
            blendInfo = null
        }
        super.setBlendFunction(pSourceBlendFunction, pDestinationBlendFunction)
    }

    override fun applyModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        val modifier = UniversalModifier.GlobalPool.obtain()
        modifier.setToDefault()
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }


    // Input

    open fun invalidateInputBinding(recursively: Boolean = true) {
        currentBoundEntity = null

        if (recursively) {
            mChildren?.fastForEach {
                (it as? ExtendedEntity)?.invalidateInputBinding()
            }
        }
    }

    override fun onAreaTouched(
        event: TouchEvent,
        localX: Float,
        localY: Float
    ): Boolean {

        val boundEntity = currentBoundEntity
        if (boundEntity != null) {
            boundEntity as IEntity

            val transformedX = localX - boundEntity.getDrawX()
            val transformedY = localY - boundEntity.getDrawY()

            if (!boundEntity.onAreaTouched(event, transformedX, transformedY)) {
                currentBoundEntity = null
            }
            return true
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)

                if (child is ITouchArea && child.contains(localX, localY)) {

                    val transformedX = localX - child.getDrawX()
                    val transformedY = localY - child.getDrawY()

                    if (child.onAreaTouched(event, transformedX, transformedY)) {
                        currentBoundEntity = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("ExtendedEntity", "A child entity was removed during touch event propagation.", e)
        }
        return false
    }

}
