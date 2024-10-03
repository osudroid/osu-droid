package com.reco1l.andengine

import android.util.*
import androidx.annotation.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.primitive.*
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 *
 * @author Reco1l
 */
abstract class ExtendedEntity(

    private var width: Float = 0f,

    private var height: Float = 0f,

    private var vertexBuffer: VertexBuffer? = null

) : Shape(0f, 0f), IModifierChain {


    /**
     * The modifier pool for the scene this entity is attached to.
     */
    var modifierPool: Pool<UniversalModifier>? = null

    /**
     * Determines which axes the entity should automatically adjust its size to.
     */
    open var autoSizeAxes = Axes.None

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
     * Intended for internal use only.
     *
     * Despite [setWidth] this will ignore [autoSizeAxes] and force the width to be set, as well the buffer will not be updated.
     */
    protected var internalWidth
        get() = width
        set(value) {
            width = value
        }

    /**
     * Intended for internal use only.
     *
     * Despite [setHeight] this will ignore [autoSizeAxes] and force the height to be set, as well the buffer will not be updated.
     */
    protected var internalHeight
        get() = height
        set(value) {
            height = value
        }


    // Positions

    open fun setAnchor(anchor: Anchor) {
        anchorX = anchor.factorX
        anchorY = anchor.factorY
    }

    open fun setOrigin(origin: Anchor) {
        originX = origin.factorX
        originY = origin.factorY
        mRotationCenterX = origin.factorX
        mRotationCenterY = origin.factorY
        mScaleCenterX = origin.factorX
        mScaleCenterY = origin.factorY
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


    // Attachment

    @CallSuper
    override fun onDetached() {
        modifierPool = null
    }

    @CallSuper
    override fun onAttached() {
        // Finding the modifier pool from the parent scene if it's set.
        modifierPool = findHierarchically(IEntity::getParent) { (it as? ExtendedScene)?.modifierPool }
    }


    // Drawing

    override fun applyTranslation(pGL: GL10) {

        val parent = parent
        if (parent is Container) {
            parent.onApplyChildTranslation(pGL, this)
            return
        }

        val originOffsetX = width * originX
        val originOffsetY = height * originY

        val anchorOffsetX = if (parent is IShape) parent.width * anchorX else 0f
        val anchorOffsetY = if (parent is IShape) parent.height * anchorY else 0f

        val finalX = x - originOffsetX + anchorOffsetX + translationX
        val finalY = y - originOffsetY + anchorOffsetY + translationY

        if (finalX != 0f || finalY != 0f) {
            pGL.glTranslatef(finalX, finalY, 0f)
        }
    }

    override fun applyRotation(pGL: GL10) {

        if (rotation == 0f) {
            return
        }

        val offsetX = width * rotationCenterX
        val offsetY = height * rotationCenterY

        pGL.glTranslatef(offsetX, offsetY, 0f)
        pGL.glRotatef(rotation, 0f, 0f, 1f)
        pGL.glTranslatef(-offsetX, -offsetY, 0f)
    }

    override fun applyScale(pGL: GL10) {

        if (scaleX == 1f && scaleY == 1f) {
            return
        }

        val offsetX = width * scaleCenterX
        val offsetY = height * scaleCenterY

        pGL.glTranslatef(offsetX, offsetY, 0f)
        pGL.glScalef(scaleX, scaleY, 1f)
        pGL.glTranslatef(-offsetX, -offsetY, 0f)
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

    override fun onApplyTransformations(pGL: GL10) {
        applyTranslation(pGL)
        applyRotation(pGL)
        applyScale(pGL)
        applyColor(pGL)
        applyBlending(pGL)
    }

    override fun onInitDraw(pGL: GL10) {
        GLHelper.enableVertexArray(pGL)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        if (vertexBuffer != null) {
            pGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4)
        }
    }

    override fun onApplyVertices(pGL: GL10) {
        if (vertexBuffer != null) {
            super.onApplyVertices(pGL)
        }
    }

    override fun getVertexBuffer(): VertexBuffer? {
        return vertexBuffer
    }


    // Size

    /**
     * Applies the size of the entity.
     *
     * Despite [setSize] this is intended to be used internally to report a new
     * size for when [autoSizeAxes] allows it for one or both axes.
     */
    protected fun onApplyInternalSize(width: Float, height: Float) {

        if (autoSizeAxes == Axes.None) {
            return
        }

        if (internalWidth != width || internalHeight != height) {

            if (autoSizeAxes == Axes.X || autoSizeAxes == Axes.Both) {
                internalWidth = width
            }

            if (autoSizeAxes == Axes.Y || autoSizeAxes == Axes.Both) {
                internalHeight = height
            }

            updateVertexBuffer()
        }
    }

    open fun setSize(weight: Float, height: Float) {

        if (autoSizeAxes == Axes.Both) {
            Log.w("ExtendedEntity", "Cannot set size when autoSizeAxes is set to Both.")
            return
        }

        if (internalWidth != weight || internalHeight != height) {

            if (autoSizeAxes == Axes.None || autoSizeAxes == Axes.Y) {
                internalWidth = weight
            }

            if (autoSizeAxes == Axes.None || autoSizeAxes == Axes.X) {
                internalHeight = height
            }

            updateVertexBuffer()

            if (parent is Container) {
                (parent as Container).onChildSizeChanged(this)
            }
        }
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

    override fun contains(pX: Float, pY: Float): Boolean {
        return RectangularShapeCollisionChecker.checkContains(this, pX, pY)
    }

    override fun isCulled(pCamera: Camera): Boolean {
        return mX > pCamera.maxX
            || mY > pCamera.maxY
            || mX + this.width < pCamera.minX
            || mY + this.height < pCamera.minY
    }

    override fun getSceneCenterCoordinates(): FloatArray {
        return this.convertLocalToSceneCoordinates(width * 0.5f, height * 0.5f)
    }


    // Transformation

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        blendingFunction = null
        super.setBlendFunction(pSourceBlendFunction, pDestinationBlendFunction)
    }

    override fun delay(durationSec: Float): UniversalModifier {
        throw IllegalStateException("Cannot call this directly to an entity. Use beginDelayChain() instead.")
    }

    override fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier {

        val modifier = modifierPool?.obtain() ?: UniversalModifier()
        modifier.setToDefault()
        modifier.entity = this
        block(modifier)

        registerEntityModifier(modifier)
        return modifier
    }

}


