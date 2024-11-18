package com.reco1l.andengine

import android.graphics.PointF
import android.util.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.primitive.*
import org.anddev.andengine.entity.scene.CameraScene
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*


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
     */
    open var autoSizeAxes = Axes.None
        set(value) {
            if (field != value) {
                field = value
                onContentSizeMeasured()
            }
        }

    /**
     * The origin factor of the entity in the X axis.
     */
    open var originX = 0f

    /**
     * The origin factor of the entity in the Y axis.
     */
    open var originY = 0f

    /**
     * The anchor factor of the entity in the X axis.
     * This is used to determine the position of the entity in a container.
     *
     * Note: This will not take effect if the entity is not a child of a [Container].
     */
    open var anchorX = 0f

    /**
     * The anchor factor of the entity in the Y axis.
     * This is used to determine the position of the entity in a container.
     *
     * Note: This will not take effect if the entity is not a child of a [Container].
     */
    open var anchorY = 0f

    /**
     * The translation in the X axis.
     */
    open var translationX = 0f

    /**
     * The translation in the Y axis.
     */
    open var translationY = 0f

    /**
     * Whether the color should be inherited from all the parents in the hierarchy.
     */
    open var inheritColor = true

    /**
     * Whether the depth buffer should be cleared before drawing the entity.
     * This is useful when the entity is drawn on top of other entities by overlapping them.
     *
     * It will only take effect if the entities on the front have the depth buffer test enabled.
     *
     * @see [testWithDepthBuffer]
     */
    open var clearDepthBufferBeforeDraw = false

    /**
     * Whether the entity should be tested with the depth buffer.
     */
    open var testWithDepthBuffer = false

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
     * The color blending function.
     */
    open var blendingFunction: BlendingFunction? = null
        set(value) {
            if (field != value) {
                field = value

                if (value != null) {
                    mSourceBlendFunction = value.source
                    mDestinationBlendFunction = value.destination
                }
            }
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
     * The raw X position of the entity.
     * This is the position without taking into account the origin, anchor, or translation.
     */
    open val drawX: Float
        get() {
            if (parent is Container) {
                return (parent as Container).getChildDrawX(this)
            }
            return x + totalOffsetX
        }

    /**
     * The raw Y position of the entity.
     * This is the position without taking into account the origin, anchor, or translation.
     */
    open val drawY: Float
        get() {
            if (parent is Container) {
                return (parent as Container).getChildDrawY(this)
            }
            return y + totalOffsetY
        }

    /**
     * The offset applied to the X axis according to the origin factor.
     */
    open val originOffsetX: Float
        get() = -(width * originX)

    /**
     * The offset applied to the Y axis according to the origin factor.
     */
    open val originOffsetY: Float
        get() = -(height * originY)

    /**
     * The offset applied to the X axis according to the anchor factor.
     */
    open val anchorOffsetX: Float
        get() = when (parent) {
            is IShape -> (parent as IShape).width * anchorX
            is CameraScene -> ((parent as CameraScene).camera?.widthRaw ?: 0f) * anchorX
            else -> 0f
        }

    /**
     * The offset applied to the Y axis according to the anchor factor.
     */
    open val anchorOffsetY: Float
        get() = when (parent) {
            is IShape -> (parent as IShape).height * anchorY
            is CameraScene -> ((parent as CameraScene).camera?.heightRaw ?: 0f) * anchorY
            else -> 0f
        }

    /**
     * The total offset applied to the X axis.
     */
    open val totalOffsetX
        get() = originOffsetX + anchorOffsetX + translationX

    /**
     * The total offset applied to the Y axis.
     */
    open val totalOffsetY
        get() = originOffsetY + anchorOffsetY + translationY


    private var width = 0f

    private var height = 0f

    private var isVertexBufferDirty = true


    // Attachment

    override fun setParent(pEntity: IEntity?) {
        (parent as? Scene)?.unregisterTouchArea(this)
        super.setParent(pEntity)
        (pEntity as? ExtendedScene)?.registerTouchArea(this)
    }


    // Positions

    open fun setAnchor(anchor: Anchor) {
        anchorX = anchor.factorX
        anchorY = anchor.factorY
    }

    open fun setOrigin(origin: Anchor) {
        originX = origin.factorX
        originY = origin.factorY
    }

    fun setPosition(position: PointF) {
        setPosition(position.x, position.y)
    }

    override fun setPosition(pX: Float, pY: Float) {
        if (mX != pX || mY != pY) {
            super.setPosition(pX, pY)
            (parent as? Container)?.onChildPositionChanged(this)
        }
    }

    open fun setX(value: Float) {
        if (mX != value) {
            setPosition(value, mY)
        }
    }

    open fun setY(value: Float) {
        if (mY != value) {
            setPosition(mX, value)
        }
    }

    open fun setTranslation(x: Float, y: Float) {
        translationX = x
        translationY = y
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

        // This will ensure getSceneCenterCoordinates() applies the correct transformation.
        mRotationCenterX = width * originX
        mRotationCenterY = height * originY

        if (rotation != 0f) {
            pGL.glRotatef(rotation, 0f, 0f, 1f)
        }
    }

    override fun applyScale(pGL: GL10) {

        // This will ensure getSceneCenterCoordinates() applies the correct transformation.
        mScaleCenterX = width * originX
        mScaleCenterY = height * originY

        if (scaleX != 1f || scaleY != 1f) {
            pGL.glScalef(scaleX, scaleY, 1f)
        }
    }

    protected open fun applyColor(pGL: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha

        if (inheritColor) {

            var parent = parent
            while (parent != null) {

                red *= parent.red
                green *= parent.green
                blue *= parent.blue
                alpha *= parent.alpha

                parent = parent.parent
            }
        }

        GLHelper.setColor(pGL, red, green, blue, alpha)
    }

    protected open fun applyBlending(pGL: GL10) {

        // If there's a blending function set, apply it instead of the engine's method.
        val blendingFunction = blendingFunction

        if (blendingFunction != null) {

            val parent = parent as? ExtendedEntity

            // If the blending function is set to inherit, apply the parent's blending function.
            if (blendingFunction == BlendingFunction.Inherit && parent != null) {
                GLHelper.blendFunction(pGL, parent.mSourceBlendFunction, parent.mDestinationBlendFunction)
            } else {
                GLHelper.blendFunction(pGL, blendingFunction.source, blendingFunction.destination)
            }

        } else {
            GLHelper.blendFunction(pGL, mSourceBlendFunction, mDestinationBlendFunction)
        }
    }

    override fun onApplyTransformations(pGL: GL10, camera: Camera) {
        applyTranslation(pGL, camera)

        if (rotation != 0f || scaleX != 1f || scaleY != 1f) {
            pGL.glTranslatef(-originOffsetX, -originOffsetY, 0f)
            applyRotation(pGL)
            applyScale(pGL)
            pGL.glTranslatef(originOffsetX, originOffsetY, 0f)
        }

        applyColor(pGL)
        applyBlending(pGL)
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (isVertexBufferDirty) {
            isVertexBufferDirty = false
            onUpdateVertexBuffer()
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun onInitDraw(pGL: GL10) {

        if (vertexBuffer != null) {
            GLHelper.enableVertexArray(pGL)
        }

        if (clearDepthBufferBeforeDraw) {
            pGL.glClear(GL_DEPTH_BUFFER_BIT)
        }

        GLHelper.setDepthTest(pGL, testWithDepthBuffer)
    }

    override fun onApplyVertices(pGL: GL10) {

        if (vertexBuffer != null) {
            super.onApplyVertices(pGL)
        }

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

            if (autoSizeAxes == Axes.X || autoSizeAxes == Axes.Both) {
                width = contentWidth
            }

            if (autoSizeAxes == Axes.Y || autoSizeAxes == Axes.Both) {
                height = contentHeight
            }

            updateVertexBuffer()

            (parent as? Container)?.onChildSizeChanged(this)
            return true
        }
        return false
    }

    /**
     * Sets the size of the entity.
     *
     * @return Whether the size of the entity was changed or not, this depends on the [autoSizeAxes] property.
     */
    open fun setSize(newWidth: Float, newHeight: Float): Boolean {

        if (autoSizeAxes == Axes.Both) {
            Log.w("ExtendedEntity", "Cannot set size when autoSizeAxes is set to Both.")
            return false
        }

        if (width != newWidth || height != newHeight) {

            if (autoSizeAxes == Axes.None || autoSizeAxes == Axes.Y) {
                width = newWidth
            }

            if (autoSizeAxes == Axes.None || autoSizeAxes == Axes.X) {
                height = newHeight
            }

            updateVertexBuffer()

            (parent as? Container)?.onChildSizeChanged(this)
            return true
        }
        return false
    }

    open fun setWidth(value: Float) {
        setSize(value, height)
    }

    override fun getWidth(): Float {
        return width
    }

    open fun setHeight(value: Float) {
        setSize(width, value)
    }

    override fun getHeight(): Float {
        return height
    }


    // Unsupported methods

    @Deprecated("Base width is not preserved in ExtendedEntity, use getWidth() instead.")
    override fun getBaseWidth() = width

    @Deprecated("Base height is not preserved in ExtendedEntity, use getHeight() instead.")
    override fun getBaseHeight() = height

    @Deprecated("Rotation center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setRotationCenter(pRotationCenterX: Float, pRotationCenterY: Float) {}

    @Deprecated("Rotation center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setRotationCenterX(pRotationCenterX: Float) {}

    @Deprecated("Rotation center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setRotationCenterY(pRotationCenterY: Float) {}

    @Deprecated("Scale center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setScaleCenter(pScaleCenterX: Float, pScaleCenterY: Float) {}

    @Deprecated("Scale center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setScaleCenterX(pScaleCenterX: Float) {}

    @Deprecated("Scale center is determined by the entity's origin, use setOrigin() instead.")
    final override fun setScaleCenterY(pScaleCenterY: Float) {}


    // Collision

    override fun collidesWith(shape: IShape): Boolean = when (shape) {

        is RectangularShape -> RectangularShapeCollisionChecker.checkCollision(this, shape)
        is Line -> RectangularShapeCollisionChecker.checkCollision(this, shape)

        else -> false
    }

    override fun contains(x: Float, y: Float): Boolean {
        if (width == 0f || height == 0f) {
            return false
        }

        return RectangularShapeCollisionChecker.checkContains(this, x - totalOffsetX, y - totalOffsetY)
    }

    override fun isCulled(pCamera: Camera): Boolean {
        return drawX > pCamera.maxX || drawX + width < pCamera.minX
            || drawY > pCamera.maxY || drawY + height < pCamera.minY
    }

    override fun getSceneCenterCoordinates(): FloatArray {
        return this.convertLocalToSceneCoordinates(width * 0.5f, height * 0.5f)
    }


    // Transformation

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        blendingFunction = null
        super.setBlendFunction(pSourceBlendFunction, pDestinationBlendFunction)
    }

    override fun applyModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        val modifier = UniversalModifier.GlobalPool.obtain()
        modifier.setToDefault()
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }

}


