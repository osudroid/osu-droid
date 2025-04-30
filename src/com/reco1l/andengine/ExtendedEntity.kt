package com.reco1l.andengine

import android.util.*
import android.view.*
import com.osudroid.BuildSettings
import com.osudroid.debug.EntityInspector
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import org.anddev.andengine.util.constants.Constants.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*


/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ExtendedEntity : Entity(0f, 0f), ITouchArea, IModifierChain, IThemeable {

    //region Axes properties

    /**
     * Determines which axes for the size of the entity are relative w.r.t the parent.
     *
     * * If the value is [Axes.None], the unit for both [width] and [height] will be absolute.
     * * If the value is [Axes.X], the unit for [width] will be relative meanwhile [height] will remain as absolute.
     * * If the value is [Axes.Y], the unit for [height] will be relative meanwhile [width] will remain as absolute.
     * * If the value is [Axes.Both], both [width] and [height] will be relative.
     *
     * Relative values are calculated as a percentage of the parent's size minus its padding, that is, values passed
     * to [width] or [height] will be treated as a percentage (values from 0 to 1).
     */
    open var relativeSizeAxes = Axes.None

    /**
     * Determines which axes for the position of the entity are relative w.r.t the parent.
     *
     * * If the value is [Axes.None], the unit for both [x][setX] and [y][setY] will be absolute.
     * * If the value is [Axes.X], the unit for [x][setX] will be relative meanwhile [y][setY] will remain as absolute.
     * * If the value is [Axes.Y], the unit for [y][setY] will be relative meanwhile [x][setX] will remain as absolute.
     * * If the value is [Axes.Both], both [x][setX] and [y][setY] will be relative.
     *
     * Relative values are calculated as a percentage of the parent's size minus its padding, that is, values passed
     * to [x][setX] or [y][setY] will be treated as a percentage (values from 0 to 1).
     */
    open var relativePositionAxes = Axes.None

    //endregion

    //region Size related properties

    private fun isReservedSizeValue(value: Float): Boolean {
        return value == MatchContent
            || value == FillParent
            || value == FitParent
    }

    private fun handleReservedSizeValue(value: Float, contentSize: Float, padding: Float, parentInnerSize: Float, position: Float): Float {
        return when (value) {
            MatchContent -> contentSize + padding
            FillParent -> parentInnerSize - position
            FitParent -> min(contentSize + padding, parentInnerSize - position)
            else -> value
        }
    }


    /**
     * The width of the entity.
     */
    var width: Float = 0f
        get() = when {
            isReservedSizeValue(field) -> handleReservedSizeValue(field, contentWidth, padding.horizontal, parent.innerWidth, x)

            else -> if (relativeSizeAxes.isHorizontal) {
                field * parent.innerWidth
            } else {
                field + padding.horizontal
            }
        }
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The height of the entity.
     */
    var height = 0f
        get() = when {
            isReservedSizeValue(field) -> handleReservedSizeValue(field, contentHeight, padding.vertical, parent.innerHeight, y)

            else -> if (relativeSizeAxes.isVertical) {
                field * parent.innerHeight
            } else {
                field + padding.vertical
            }
        }
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The width of the content inside the entity.
     */
    open var contentWidth = 0f
        protected set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The height of the content inside the entity.
     */
    open var contentHeight = 0f
        protected set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The padding of the entity.
     */
    open var padding = Vec4.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    //endregion

    //region Position related properties

    override fun getX(): Float {
        if (relativePositionAxes.isHorizontal) {
            return mX * parent.innerWidth
        }
        return mX
    }
    fun setX(value: Float) {
        if (mX != value) {
            mX = value
            invalidate(InvalidationFlag.Position)
        }
    }

    override fun getY(): Float {
        if (relativePositionAxes.isVertical) {
            return mY * parent.innerHeight
        }
        return mY
    }
    fun setY(value: Float) {
        if (mY != value) {
            mY = value
            invalidate(InvalidationFlag.Position)
        }
    }

    /**
     * Where the entity should be anchored in the parent.
     */
    open var anchor = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
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
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * The translation in the X axis.
     */
    open var translationX = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * The translation in the Y axis.
     */
    open var translationY = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    //endregion

    //region Cosmetic properties

    override var applyTheme: ExtendedEntity.(theme: Theme) -> Unit = {}

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
                field?.decoratedEntity = null
                field = value
                field?.decoratedEntity = this
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
                field?.decoratedEntity = null
                field = value
                field?.decoratedEntity = this
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
     * Whether the entity's color should be multiplied by the color of its ancestor entities.
     */
    open var inheritAncestorsColor = true

    /**
     * Whether the entity should clip its children.
     */
    open var clipToBounds = false

    //endregion

    //region State properties

    /**
     * The modifier pool used to manage the modifiers of this entity. By default [UniversalModifier.GlobalPool].
     */
    var modifierPool = UniversalModifier.GlobalPool

    /**
     * The entity that is currently being decorated by this entity.
     */
    var decoratedEntity: ExtendedEntity? = null

    /**
     * The current invalidation flags. Indicates which properties were updated and need to be handled.
     *
     * @see InvalidationFlag
     */
    protected var invalidationFlags = InvalidationFlag.Position or InvalidationFlag.Size

    /**
     * The input bindings of the entity. This is used to handle touch events.
     */
    protected val inputBindings = arrayOfNulls<ExtendedEntity>(10)

    //endregion

    //region Invalidation

    /**
     * Adds the given flag to the invalidation list. Depending on each flag it will trigger a different action.
     *
     * @see InvalidationFlag
     */
    fun invalidate(flag: Int) {
        invalidationFlags = invalidationFlags or flag
    }

    //endregion

    //region Attachment

    /**
     * Called when the content of the entity has changed. This usually is called when a child is added or removed.
     */
    protected open fun onContentChanged() {}

    override fun setParent(newParent: IEntity?) {
        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is ExtendedEntity -> parent.onChildDetached(this)
        }
        super.setParent(newParent)
        when (newParent) {
            is ExtendedScene -> newParent.registerTouchArea(this)
            is ExtendedEntity -> newParent.onChildAttached(this)
        }
    }

    /**
     * Called when a child is attached to this entity.
     */
    open fun onChildAttached(child: IEntity) {}

    /**
     * Called when a child is detached from this entity.
     */
    open fun onChildDetached(child: IEntity) {}

    override fun setVisible(value: Boolean) {
        if (mVisible != value) {
            mVisible = value
            invalidate(InvalidationFlag.Size)
        }
    }

    override fun onAttached() {
        onThemeChanged(Theme.current)
    }

    //endregion

    //region Size

    /**
     * Called when the size of a child entity changes.
     */
    open fun onChildSizeChanged(child: IEntity) {}

    /**
     * Called when the size of this entity changes.
     */
    open fun onSizeChanged() {
        (parent as? ExtendedEntity)?.onChildSizeChanged(this)
    }

    /**
     * Sets the size of the entity.
     */
    open fun setSize(x: Float, y: Float) {
        width = x
        height = y
    }

    @Deprecated("Keeping this for the current usages.", ReplaceWith("transformedWidth"))
    fun getWidthScaled(): Float {
        return width * scaleX
    }

    @Deprecated("Keeping this for the current usages.", ReplaceWith("transformedHeight"))
    fun getHeightScaled(): Float {
        return height * scaleY
    }

    //endregion

    //region Position

    /**
     * Called when the position of a child entity changes.
     */
    open fun onChildPositionChanged(child: IEntity) {}

    /**
     * Called when the position of this entity changes.
     */
    open fun onPositionChanged() {
        (parent as? ExtendedEntity)?.onChildPositionChanged(this)
    }

    /**
     * Sets the position of the entity.
     */
    override fun setPosition(x: Float, y: Float) {
        setX(x)
        setY(y)
    }

    //endregion

    //region Drawing

    override fun onApplyTransformations(gl: GL10, camera: Camera) {
        val x = absoluteX
        val y = absoluteY

        if (x != 0f || y != 0f) {
            gl.glTranslatef(x, y, 0f)
        }

        if (mRotation != 0f) {
            val centerX = width * mRotationCenterX
            val centerY = height * mRotationCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glRotatef(mRotation, 0f, 0f, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glRotatef(mRotation, 0f, 0f, 1f)
            }
        }

        if (mScaleX != 1f || mScaleY != 1f) {
            val centerX = width * mScaleCenterX
            val centerY = height * mScaleCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glScalef(mScaleX, mScaleY, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glScalef(mScaleX, mScaleY, 1f)
            }
        }
    }

    open fun onApplyColor(gl: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha
        var parent = parent ?: decoratedEntity
        var multiplyColor = inheritAncestorsColor

        while (parent != null) {

            // If this entity is a decoration we only multiply the alpha.
            if (decoratedEntity == null && multiplyColor) {
                red *= parent.red
                green *= parent.green
                blue *= parent.blue
            }
            alpha *= parent.alpha

            // We'll assume at this point there's no need to keep multiplying.
            if (red == 0f && green == 0f && blue == 0f && alpha == 0f) {
                break
            }

            if (parent is ExtendedEntity && !parent.inheritAncestorsColor) {
                multiplyColor = false
            }

            parent = parent.parent
        }

        GLHelper.setColor(gl, red, green, blue, alpha)
    }

    override fun onDraw(gl: GL10, camera: Camera) {

        if (!isVisible) {
            // We're going to still handle invalidations flags even if the entity is not visible
            // because some of them like size-related flags might change the parent's layout.
            handleInvalidation(true)
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = GLHelper.isEnableScissorTest()
            GLHelper.enableScissorTest(gl)

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, height))

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

    fun handleInvalidation(handleRecursively: Boolean) {
        val invalidationFlags = invalidationFlags

        if (invalidationFlags != 0) {

            var recursiveInvalidationFlags = 0

            if (invalidationFlags and InvalidationFlag.Position != 0) {
                onPositionChanged()
            }

            if (invalidationFlags and InvalidationFlag.Content != 0) {
                onContentChanged()
            }

            if (invalidationFlags and InvalidationFlag.Size != 0) {
                onSizeChanged()
            }

            if (invalidationFlags and InvalidationFlag.Transformations != 0
                || invalidationFlags and InvalidationFlag.Position != 0
                || invalidationFlags and InvalidationFlag.Size != 0) {
                onInvalidateTransformations()
                recursiveInvalidationFlags = InvalidationFlag.Transformations
            }

            if (invalidationFlags and InvalidationFlag.InputBindings != 0) {
                onInvalidateInputBindings()
                recursiveInvalidationFlags = recursiveInvalidationFlags or InvalidationFlag.InputBindings
            }

            mChildren?.fastForEach { child ->
                if (child is ExtendedEntity) {
                    child.invalidate(recursiveInvalidationFlags)
                }
            }

            // During the invalidation process the flags could be changed.
            if (this.invalidationFlags == invalidationFlags) {
                this.invalidationFlags = 0
            }
        }

        if (handleRecursively) {
            mChildren?.fastForEach { child ->
                if (child is ExtendedEntity) {
                    child.handleInvalidation(true)
                }
            }
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        handleInvalidation(false)

        gl.glPushMatrix()
        onApplyTransformations(gl, camera)

        background?.setSize(width, height)
        background?.onDraw(gl, camera)

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        foreground?.setSize(width, height)
        foreground?.onDraw(gl, camera)


        if ((BuildSettings.SHOW_ENTITY_BOUNDARIES || EntityInspector.SELECTED_ENTITY == this) && DEBUG_FOREGROUND != this) {
            DEBUG_FOREGROUND.color = if (EntityInspector.SELECTED_ENTITY == this) ColorARGB(0xFF00FF00) else ColorARGB.White
            DEBUG_FOREGROUND.lineWidth = if (EntityInspector.SELECTED_ENTITY == this) 3f else 1f
            DEBUG_FOREGROUND.setSize(width, height)
            DEBUG_FOREGROUND.onDraw(gl, camera)
        }

        gl.glPopMatrix()
    }

    open fun beginDraw(gl: GL10) {
        // We haven't done any culling implementation so we disable it globally for all buffered entities.
        GLHelper.disableCulling(gl)
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)
        onApplyColor(gl)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        beginDraw(gl)
    }

    //endregion

    //region Update

    override fun onManagedUpdate(deltaTimeSec: Float) {

        background?.onManagedUpdate(deltaTimeSec)
        foreground?.onManagedUpdate(deltaTimeSec)

        super.onManagedUpdate(deltaTimeSec)
    }

    //endregion

    //region Collision

    override fun contains(x: Float, y: Float): Boolean {

        if (width == 0f || height == 0f) {
            return false
        }

        VERTICES_WRAPPER[0 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[0 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[2 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[2 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[4 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[4 + VERTEX_INDEX_Y] = height

        VERTICES_WRAPPER[6 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[6 + VERTEX_INDEX_Y] = height

        if (parent is Scene) {
            localToSceneTransformation.transform(VERTICES_WRAPPER)
        } else {
            localToParentTransformation.transform(VERTICES_WRAPPER)
        }

        return ShapeCollisionChecker.checkContains(VERTICES_WRAPPER, VERTICES_WRAPPER.size, x, y)
    }

    //endregion

    //region Transformations

    open fun onInvalidateTransformations() {
        mLocalToParentTransformationDirty = true
        localToParentTransformation

        mParentToLocalTransformationDirty = true
        parentToLocalTransformation
    }


    override fun setRotation(pRotation: Float) {
        if (mRotation != pRotation) {
            mRotation = pRotation
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setRotationCenterX(pRotationCenterX: Float) = setRotationCenter(pRotationCenterX, mRotationCenterY)
    override fun setRotationCenterY(pRotationCenterY: Float) = setRotationCenter(mRotationCenterX, pRotationCenterY)
    override fun setRotationCenter(pRotationCenterX: Float, pRotationCenterY: Float) {
        if (mRotationCenterX != pRotationCenterX || mRotationCenterY != pRotationCenterY) {
            mRotationCenterX = pRotationCenterX
            mRotationCenterY = pRotationCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleCenterX(pScaleCenterX: Float) = setScaleCenter(pScaleCenterX, mScaleCenterY)
    override fun setScaleCenterY(pScaleCenterY: Float) = setScaleCenter(mScaleCenterX, pScaleCenterY)
    override fun setScaleCenter(pScaleCenterX: Float, pScaleCenterY: Float) {
        if (mScaleCenterX != pScaleCenterX || mScaleCenterY != pScaleCenterY) {
            mScaleCenterX = pScaleCenterX
            mScaleCenterY = pScaleCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleX(pScaleX: Float) = setScale(pScaleX, mScaleY)
    override fun setScaleY(pScaleY: Float) = setScale(mScaleX, pScaleY)
    override fun setScale(pScale: Float) = setScale(pScale, pScale)
    override fun setScale(pScaleX: Float, pScaleY: Float) {
        if (mScaleX != pScaleX || mScaleY != pScaleY) {
            mScaleX = pScaleX
            mScaleY = pScaleY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun getLocalToParentTransformation(): Transformation {

        if (mLocalToParentTransformation == null) {
            mLocalToParentTransformation = Transformation()
        }

        if (mLocalToParentTransformationDirty) {
            mLocalToParentTransformation.setToIdentity()

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postScale(mScaleX, mScaleY)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            if (rotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postRotate(mRotation)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            mLocalToParentTransformation.postTranslate(absoluteX, absoluteY)
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
            mParentToLocalTransformation.postTranslate(-absoluteX, -absoluteY)

            if (mRotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postRotate(-mRotation)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postScale(1 / mScaleX, 1 / mScaleY)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            mParentToLocalTransformationDirty = false
        }

        return mParentToLocalTransformation
    }

    //endregion

    //region Modifiers

    fun clearModifiers(vararg type: ModifierType) {
        unregisterEntityModifiers { it is UniversalModifier && it.type in type }
    }

    override fun appendModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        val modifier = modifierPool.acquire() ?: UniversalModifier(modifierPool)
        modifier.setToDefault()
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }

    //endregion

    //region Input

    /**
     * Propagates a touch event to the entity.
     */
    protected fun propagateTouchEvent(action: Int, pointerIndex: Int = 0, localX: Float = 0f, localY: Float = 0f): Boolean {

        val motionEvent = MotionEvent.obtain(0, 0, action, 0f, 0f, 0)
        val touchEvent = TouchEvent.obtain(0f, 0f, action, pointerIndex, motionEvent)

        val result = onAreaTouched(touchEvent, localX, localY)

        touchEvent.recycle()
        motionEvent.recycle()
        return result
    }

    /**
     * Called when input bindings are invalidated and needs to be removed.
     */
    open fun onInvalidateInputBindings() {
        inputBindings.fastForEachIndexed { index, binding ->
            if (binding != null) {
                propagateTouchEvent(MotionEvent.ACTION_CANCEL, index)
            }
        }
        inputBindings.fill(null)
    }

    /**
     * Called when a key is pressed.
     */
    open fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding != null && inputBinding.parent == this) {
            inputBinding.onAreaTouched(event, localX - inputBinding.absoluteX, localY - inputBinding.absoluteY)

            if (event.isActionUp) {
                inputBindings[event.pointerID] = null
            }
            return true
        } else {
            inputBindings[event.pointerID] = null
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)
                if (child is ExtendedEntity && child.contains(localX, localY)) {
                    if (child.onAreaTouched(event, localX - child.absoluteX, localY - child.absoluteY)) {
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

    //region Cosmetic functions

    open fun onThemeChanged(theme: Theme) {
        background?.onThemeChanged(theme)
        foreground?.onThemeChanged(theme)

        applyTheme(theme)
    }

    //endregion


    @Suppress("ConstPropertyName")
    companion object {

        /**
         * The width and height of the entity will match the content size without any constraints.
         */
        const val MatchContent = -1f

        /**
         * The width and height of the entity will match the parent's inner size.
         */
        const val FillParent = -2f

        /**
         * The width and height of the entity will match the content size but will be constrained to the parent's inner size.
         */
        const val FitParent = -3f


        private val VERTICES_WRAPPER = FloatArray(8)

        private val DEBUG_FOREGROUND by lazy {
            Box().apply {
                paintStyle = PaintStyle.Outline
                color = ColorARGB.White
            }
        }
    }

}


