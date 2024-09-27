package com.reco1l.andengine

import android.util.*
import androidx.annotation.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.IEntity
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


    @Suppress("LeakingThis")
    final override var modifierChainTarget: IEntity? = this
        set(_) {
            Log.w("ExtendedEntity", "The modifier chain target property is read-only for this class.")
            field = this
        }


    /**
     * The modifier pool for the scene this entity is attached to.
     */
    var modifierPool: Pool<UniversalModifier>? = null
        private set
        get() {
            if (field != null) {
                return field
            }

            field = (this as IEntity).findHierarchically(IEntity::getParent) {
                (it as? ExtendedScene)?.modifierPool
            }

            return field
        }


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


    init {
        mRotationCenterX = 0.5f
        mRotationCenterY = 0.5f
        mScaleCenterX = 0.5f
        mScaleCenterY = 0.5f
    }


    open fun setAnchor(anchor: Origin) {
        anchorX = anchor.factorX
        anchorY = anchor.factorY
    }

    open fun setOrigin(origin: Origin) {
        originX = origin.factorX
        originY = origin.factorY
    }

    open fun setRotationCenter(center: Origin) {
        mRotationCenterX = center.factorX
        mRotationCenterY = center.factorY
    }

    open fun setScaleCenter(center: Origin) {
        mScaleCenterX = center.factorX
        mScaleCenterY = center.factorY
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


    @CallSuper
    override fun onDetached() {
        modifierPool = null
    }

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


    override fun onInitDraw(pGL: GL10) {
        applyColor(pGL)
        GLHelper.enableVertexArray(pGL)
        GLHelper.blendFunction(pGL, mSourceBlendFunction, mDestinationBlendFunction)
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


    /**
     * This will set the size of the entity without updating the buffer.
     *
     * Intended for internal use only to handle the [autoSizeAxes] property.
     */
    protected fun setSizeInternal(w: Float, h: Float) {
        width = w
        height = h
    }

    open fun setSize(w: Float, h: Float) {

        var updateBuffer = false

        if (width != w && (autoSizeAxes == Axes.None || autoSizeAxes == Axes.Y)) {
            width = w
            updateBuffer = true
        }

        if (height != h && (autoSizeAxes == Axes.None || autoSizeAxes == Axes.X)) {
            height = h
            updateBuffer = true
        }

        if (updateBuffer) {
            updateVertexBuffer()
        }
    }

    open fun setWidth(value: Float) {
        if (width != value && (autoSizeAxes == Axes.None || autoSizeAxes == Axes.Y)) {
            width = value
            updateVertexBuffer()
        }
    }

    override fun getWidth(): Float {
        return width
    }

    open fun setHeight(value: Float) {
        if (height != value && (autoSizeAxes == Axes.None || autoSizeAxes == Axes.X)) {
            height = value
            updateVertexBuffer()
        }
    }

    override fun getHeight(): Float {
        return height
    }


    // Base width and height are not needed for this class.

    override fun getBaseWidth(): Float {
        return width
    }

    override fun getBaseHeight(): Float {
        return height
    }


    override fun collidesWith(pOtherShape: IShape?): Boolean {
        return when (pOtherShape) {
            is RectangularShape -> RectangularShapeCollisionChecker.checkCollision(this, pOtherShape)
            is Line -> RectangularShapeCollisionChecker.checkCollision(this, pOtherShape)
            else -> false
        }
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

    override fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier? {

        val modifier = modifierPool?.obtain() ?: UniversalModifier()
        modifier.setToDefault()
        modifier.modifierChainTarget = modifierChainTarget
        block(modifier)

        registerEntityModifier(modifier)
        return modifier
    }

}


/**
 * Determines the origin of the sprite.
 */
enum class Origin(val factorX: Float, val factorY: Float) {

    TopLeft(0f, 0f),

    TopCenter(0.5f, 0f),

    TopRight(1f, 0f),

    CenterLeft(0f, 0.5f),

    Center(0.5f, 0.5f),

    CenterRight(1f, 0.5f),

    BottomLeft(0f, 1f),

    BottomCenter(0.5f, 1f),

    BottomRight(1f, 1f)

}


enum class Axes {

    /**
     * The entity will automatically adjust its size to the width of the parent.
     */
    X,

    /**
     * The entity will automatically adjust its size to the height of the parent.
     */
    Y,

    /**
     * The entity will automatically adjust its size to the width and height of the parent.
     */
    Both,

    /**
     * The entity will not automatically adjust its size.
     */
    None
}