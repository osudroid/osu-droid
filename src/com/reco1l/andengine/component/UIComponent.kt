package com.reco1l.andengine.component

import android.util.*
import android.view.*
import com.edlplan.framework.easing.Easing
import com.osudroid.*
import com.osudroid.math.Precision
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.andengine.modifier.*
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IClockReceiver
import com.rian.andengine.timing.IFrameBasedClock
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import org.anddev.andengine.util.constants.Constants.*
import java.util.function.Consumer
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class UIComponent : Entity(0f, 0f),
    ITouchArea, IThemeable, IClockProvider<IFrameBasedClock?>, IClockReceiver<IFrameBasedClock?> {

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

    @Suppress("NOTHING_TO_INLINE")
    private inline fun computeSizeValue(value: Float, padding: Float, position: Float, isRelative: Boolean, contentSize: Float, containerSize: Float): Float {
        return when (value) {
            MatchContent -> contentSize + padding
            FillParent -> containerSize - position
            FitParent -> min(contentSize + padding, containerSize - position)

            else -> if (isRelative) value * containerSize else value
        }
    }

    /**
     * The minimum width of the entity.
     */
    var minWidth = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The maximum width of the entity.
     */
    var maxWidth = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }
    /**
     * The width of the entity.
     */
    var width: Float = 0f
        get() = computeSizeValue(
            value = field,
            padding = padding.horizontal,
            position = x,
            isRelative = relativeSizeAxes.isHorizontal,
            contentSize = contentWidth,
            containerSize = parent.innerWidth,
        ).coerceAtMost(maxWidth).coerceAtLeast(minWidth)
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The minimum height of the entity.
     */
    var minHeight = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }
    /**
     * The maximum height of the entity.
     */
    var maxHeight = Float.MAX_VALUE
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
        get() = computeSizeValue(
            value = field,
            padding = padding.vertical,
            position = y,
            isRelative = relativeSizeAxes.isVertical,
            contentSize = contentHeight,
            containerSize = parent.innerHeight,
        ).coerceAtMost(maxHeight).coerceAtLeast(minHeight)
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

    override var applyTheme: ThemeApplier = {}

    /**
     * The background entity. This entity will be drawn before the entity children and will not be
     * affected by padding.
     */
    open var background: UIComponent? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("UIComponent", "The background entity is already attached to another entity.")
                    return
                }
                field?.detachSelf()
                field = value
                field?.setParent(this, AttachmentMode.Decorator)
            }
        }

    /**
     * The foreground entity. This entity will be drawn after the entity children and will not be
     * affected by padding.
     */
    open var foreground: UIComponent? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("UIComponent", "The foreground entity is already attached to another entity.")
                    return
                }
                field?.detachSelf()
                field = value
                field?.setParent(this, AttachmentMode.Decorator)
            }
        }

    /**
     * The color of the entity boxed in a [Color4] object.
     */
    open var color: Color4
        get() = Color4(mRed, mGreen, mBlue, mAlpha)
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
     * Whether the component is currently animating.
     */
    val isAnimating
        get() = !mEntityModifiers.isNullOrEmpty() || universalModifierTrackers.any { !it.modifiers.isEmpty() }

    /**
     * The mode in which the entity is attached to its parent.
     */
    var attachmentMode = AttachmentMode.None
        private set

    /**
     * Whether the entity should be culled when it is outside the parent's bounds.
     */
    var cullingMode = CullingMode.Disabled

    /**
     * The current invalidation flags. Indicates which properties were updated and need to be handled.
     *
     * @see InvalidationFlag
     */
    protected var invalidationFlags = InvalidationFlag.Position or InvalidationFlag.Size

    /**
     * The input bindings of the entity. This is used to handle touch events.
     */
    protected val inputBindings = arrayOfNulls<ITouchArea>(10)

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

    //region Events

    /**
     * Called every update thread tick, avoid heavy operations here.
     */
    var onUpdateTick: OnUpdateEvent? = null

    //endregion

    //region Attachment

    /**
     * Called when the content of the entity has changed. This usually is called when a child is added or removed.
     */
    protected open fun onContentChanged() {}


    override fun detachSelf(): Boolean {

        if (parent == null) {
            return false
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            parent = null
            onDetached()
            return true
        }

        return super.detachSelf()
    }

    fun setParent(entity: IEntity?, mode: AttachmentMode?) {

        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is UIComponent -> parent.onChildDetached(this)
        }

        super.setParent(entity)

        attachmentMode = if (entity == null) AttachmentMode.None else mode ?: AttachmentMode.Child

        when (entity) {
            is Scene -> entity.registerTouchArea(this)
            is UIComponent -> entity.onChildAttached(this)
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            if (entity == null) {
                onDetached()
            } else {
                onAttached()
            }
        }
    }

    override fun setParent(parent: IEntity?) {
        setParent(parent, null)
    }

    /**
     * Called when a child is attached to this entity.
     */
    open fun onChildAttached(child: IEntity) {
        onContentChanged()
    }

    /**
     * Called when a child is detached from this entity.
     */
    open fun onChildDetached(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    override fun setVisible(value: Boolean) {
        if (mVisible != value) {
            mVisible = value
            invalidate(InvalidationFlag.Size)
        }
    }

    override fun onAttached() {
        onThemeChanged(Theme.current)
        onHandleInvalidations(false)
        updateClock((parent as? IClockProvider<*>)?.clock as? IFrameBasedClock)
    }

    override fun onDetached() {
        updateClock(null)

        super.onDetached()
    }

    //endregion

    //region Size

    /**
     * Called when the size of a child entity changes.
     */
    open fun onChildSizeChanged(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    /**
     * Called when the size of this entity changes.
     */
    open fun onSizeChanged() {
        (parent as? UIComponent)?.onChildSizeChanged(this)
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
     * Whether the entity is outside the camera's or parent's bounds.
     */
    open fun isCulled(camera: Camera): Boolean {

        when (cullingMode) {

            CullingMode.CameraBounds -> {
                val (x1, y1) = convertLocalToSceneCoordinates(0f, 0f)
                val (x2, y2) = convertLocalToSceneCoordinates(width, height)

                return x2 < camera.minX || y2 < camera.minY || x1 > camera.maxX || y1 > camera.maxY
            }

            CullingMode.ParentBounds -> {

                if (parent !is UIComponent && parent !is UIScene) {
                    return false
                }

                val x1 = absoluteX
                val y1 = absoluteY
                val x2 = x1 + width
                val y2 = y1 + height

                return x2 < 0f || y2 < 0f || x1 > parent.getWidth() || y1 > parent.getHeight()
            }

            else -> return false
        }

    }

    /**
     * Called when the position of a child entity changes.
     */
    open fun onChildPositionChanged(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    /**
     * Called when the position of this entity changes.
     */
    open fun onPositionChanged() {
        (parent as? UIComponent)?.onChildPositionChanged(this)
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
        var parent = parent
        var multiplyColor = inheritAncestorsColor

        while (parent != null) {

            // If this entity is a decoration we only multiply the alpha.
            if (attachmentMode == AttachmentMode.Child && multiplyColor) {
                red *= parent.red
                green *= parent.green
                blue *= parent.blue
            }
            alpha *= parent.alpha

            // We'll assume at this point there's no need to keep multiplying.
            if (red == 0f && green == 0f && blue == 0f && alpha == 0f) {
                break
            }

            if (parent is UIComponent && !parent.inheritAncestorsColor) {
                multiplyColor = false
            }

            parent = parent.parent
        }

        GLHelper.setColor(gl, red, green, blue, alpha)
    }

    override fun onDraw(gl: GL10, camera: Camera) {

        val isCulled = isCulled(camera)

        if (!isVisible || isCulled) {
            // We're going to still handle invalidations flags even if the entity is not visible
            // because some of them like size-related flags might change the parent's layout.
            onHandleInvalidations()

            mChildren?.fastForEach { child ->
                if (child is UIComponent) {
                    onHandleInvalidations()
                }
            }
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

    /**
     * Called when an invalidation flag should be processed.
     */
    protected open fun processInvalidationFlag(flag: Int) {
        when (flag) {
            InvalidationFlag.Position -> onPositionChanged()
            InvalidationFlag.Content -> onContentChanged()
            InvalidationFlag.Size -> onSizeChanged()
            InvalidationFlag.Transformations -> onInvalidateTransformations()
            InvalidationFlag.InputBindings -> onInvalidateInputBindings()
        }
    }

    open fun onHandleInvalidations(restoreFlags: Boolean = true) {

        val flags = invalidationFlags

        if (flags == 0) {
            return
        }

        operator fun Int.contains(flag: Int): Boolean {
            return flags and flag != 0
        }

        if (InvalidationFlag.Size in flags) {
            processInvalidationFlag(InvalidationFlag.Size)
        }

        if (InvalidationFlag.Content in flags) {
            processInvalidationFlag(InvalidationFlag.Content)
        }

        if (InvalidationFlag.Position in flags) {
            processInvalidationFlag(InvalidationFlag.Position)
        }

        if (InvalidationFlag.Transformations in flags || InvalidationFlag.Size in flags || InvalidationFlag.Position in flags) {
            processInvalidationFlag(InvalidationFlag.Transformations)
        }

        if (InvalidationFlag.InputBindings in flags) {
            processInvalidationFlag(InvalidationFlag.InputBindings)
        }

        // During the invalidation process the flags could be changed.
        if (this.invalidationFlags == flags && restoreFlags) {
            this.invalidationFlags = 0
        }

        background?.onHandleInvalidations()
        foreground?.onHandleInvalidations()
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        onHandleInvalidations()

        gl.glPushMatrix()
        onApplyTransformations(gl, camera)

        background?.setSize(width, height)
        background?.onDraw(gl, camera)

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        foreground?.setSize(width, height)
        foreground?.onDraw(gl, camera)


        if (BuildSettings.SHOW_ENTITY_BOUNDARIES && DEBUG_FOREGROUND != this) {
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

    final override fun onUpdate(deltaTimeSec: Float) {
        if (loadState == LoadState.NotLoaded) {
            return
        }

        if (processCustomClock) {
            customClock?.processFrame()
        }

        if (loadState == LoadState.Ready) {
            loadState = LoadState.Loaded
            onLoadComplete()
        }

        if (!isIgnoreUpdate) {
            // Fallback to parent or engine-provided delta time in case clock is not present.
            onManagedUpdate(clock?.elapsedFrameTime ?: deltaTimeSec)
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        onUpdateTick?.invoke(deltaTimeSec)

        background?.onManagedUpdate(deltaTimeSec)
        foreground?.onManagedUpdate(deltaTimeSec)

        updateModifiers()

        super.onManagedUpdate(deltaTimeSec)
    }

    private var loadState = LoadState.NotLoaded

    /**
     * Whether this [UIComponent] is currently loaded and part of the active update loop.
     *
     * This becomes `true` after [onLoadComplete] is called and stays `true` until this [UIComponent] is unloaded.
     */
    val isLoaded
        get() = loadState == LoadState.Loaded

    /**
     * Called when this [UIComponent] is fully loaded and ready for use.
     *
     * This is invoked when [onUpdate] is called for the first time after this [UIComponent] receives a valid [clock]
     * **and** before [onManagedUpdate]. It is safe to start animations and modifiers here.
     *
     * Note that this is called regardless of [isIgnoreUpdate], and can be called multiple times during this
     * [UIComponent]'s lifetime if it is detached and re-attached.
     */
    protected open fun onLoadComplete() {}

    /**
     * Called when this [UIComponent] is being unloaded.
     *
     * This is invoked when this [UIComponent] loses its [clock], usually when it is detached from its [parent].
     * If this [UIComponent] is re-attached, [onLoadComplete] will be called again.
     *
     * Subclasses should use this to clear persistent modifiers or stop animations if needed (e.g., if this
     * [UIComponent] is intended to be pooled and re-used).
     */
    protected open fun onUnload() {}

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
        mParentToLocalTransformationDirty = true

        // This recreates and calculates the transformation matrices.
        localToParentTransformation
        parentToLocalTransformation

        mChildren?.fastForEach {
            if (it is UIComponent) {
                it.onInvalidateTransformations()
            }
        }
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

    private val universalModifierTrackers = mutableListOf<UniversalModifierTargetTracker>()

    /**
     * Appends a [UniversalModifier] to this [UIComponent]. The [UniversalModifier] will be tracked and
     * automatically.
     *
     * @param modifier The [UniversalModifier] to append.
     */
    fun appendModifier(modifier: UniversalModifier) {
        getTrackerFor(modifier.type, true)!!.add(modifier)
    }

    /**
     * Removes a [UniversalModifier] from this [UIComponent].
     *
     * @param modifier The [UniversalModifier] to remove.
     * @return Whether the [UniversalModifier] was removed.
     */
    fun removeModifier(modifier: UniversalModifier): Boolean {
        if (modifier.target != this) {
            return false
        }

        return getTrackerFor(modifier.type)?.remove(modifier) ?: false
    }

    private inline fun appendModifier(
        type: ModifierType,
        duration: Float,
        easing: Easing,
        crossinline block: UniversalModifier.() -> Unit
    ): UniversalModifier {
        val modifier = UniversalModifier.GlobalPool.acquire() ?: UniversalModifier()

        modifier.target = this
        modifier.type = type
        modifier.startTime = modifierStartTime
        modifier.duration = duration
        modifier.eased(easing)
        modifier.block()

        appendModifier(modifier)

        return modifier
    }

    /**
     * Obtains the [UniversalModifierTargetTracker] for the specified [ModifierType].
     *
     * @param type The [ModifierType] to get the [UniversalModifierTargetTracker] for.
     * @param createIfNotExisting Whether to create the [UniversalModifierTargetTracker] if it does not exist.
     * @return The [UniversalModifierTargetTracker] for [type], `null` if it did not exist and [createIfNotExisting] was
     * `false`.
     */
    private fun getTrackerFor(type: ModifierType, createIfNotExisting: Boolean = false): UniversalModifierTargetTracker? {
        for (i in universalModifierTrackers.indices) {
            val tracker = universalModifierTrackers[i]

            if (tracker.targetMember == type.targetMember) {
                return tracker
            }
        }

        if (!createIfNotExisting) {
            return null
        }

        val tracker = UniversalModifierTargetTracker(type.targetMember, this)
        universalModifierTrackers += tracker

        return tracker
    }

    /**
     * Resets [modifierDelay] and processes updates to [UniversalModifier]s.
     */
    protected fun updateModifiers() {
        modifierDelay = 0f

        updateModifiers(time?.current ?: return)
    }

    private fun updateModifiers(time: Float) {
        universalModifierTrackers.fastForEach { it.update(time) }
    }

    override fun clearEntityModifiers() {
        super.clearEntityModifiers()

        universalModifierTrackers.fastForEach { it.clear() }
    }

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType] from this [UIComponent].
     *
     * Unlike the vararg variant, this method avoids array allocation in cases where there is only one [ModifierType] to
     * remove.
     *
     * @param type The [ModifierType] to clear.
     * @param propagateChildren Whether to also clear [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiers(type: ModifierType, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clear(type) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiers(type, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType]s from this [UIComponent].
     *
     * @param propagateChildren Whether to also clear [UniversalModifier]s of children. Defaults to `false`.
     * @param types The [ModifierType]s to remove.
     */
    @JvmOverloads
    fun clearModifiers(propagateChildren: Boolean = false, vararg types: ModifierType) {
        universalModifierTrackers.fastForEach { it.clear(*types) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiers(true, *types) }
        }
    }

    /**
     * Clears [UniversalModifier]s that start after [time].
     * 
     * @param time The time to clear [UniversalModifier]s after.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clearAfter(time) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s with the given [ModifierType] that start after [time].
     *
     * @param time The time to clear [UniversalModifier]s after.
     * @param type The [ModifierType] to clear.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, type: ModifierType, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clearAfter(time, type) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, type, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s with the given [ModifierType]s that start after [time].
     *
     * @param time The time to clear [UniversalModifier]s after.
     * @param types The [ModifierType]s to clear.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, propagateChildren: Boolean = false, vararg types: ModifierType) {
        universalModifierTrackers.fastForEach { it.clearAfter(time, *types) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, true, *types) }
        }
    }

    /**
     * Finishes all [UniversalModifier]s, using their [UniversalModifier.finalValues] and calling their
     * [UniversalModifier.onFinished] callbacks.
     *
     * @param propagateChildren Whether to also finish [UniversalModifier]s of children. Defaults to `false`.
      * @param type The [ModifierType] to finish, or `null` to finish all [UniversalModifier]s regardless of their type.
     */
    @JvmOverloads
    fun finishModifiers(propagateChildren: Boolean = false, type: ModifierType? = null) {
        if (type != null) {
            getTrackerFor(type)?.finish()
        } else {
            universalModifierTrackers.fastForEach { it.finish() }
        }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.finishModifiers(true, type) }
        }
    }

    /**
     * Starting time to use for new [UniversalModifier]s.
     */
    val modifierStartTime
        get() = (clock?.currentTime ?: 0f) + modifierDelay

    /**
     * Delay from the current time until new [UniversalModifier]s are started, in seconds. **This is made public so that
     * [beginAbsoluteSequence] and [beginDelayedSequence] can be inlined for performance and should not be altered
     * externally**.
     */
    var modifierDelay = 0f

    /**
     * Adds a delay duration to [modifierDelay], in seconds.
     *
     * @param duration The delay duration to add.
     * @param propagateChildren Whether we also delay down the child.
     */
    @JvmOverloads
    open fun addDelay(duration: Float, propagateChildren: Boolean = false) {
        if (duration == 0f) {
            return
        }

        modifierDelay += duration

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.addDelay(duration, true) }
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s. The block will be provided with a [UniversalModifierSequence] that can
     * be used to add [UniversalModifier]s.
     *
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginModifierSequence(crossinline block: UniversalModifierSequence.() -> Unit) =
        UniversalModifierSequence.obtain(this).use { it.block() }

    /**
     * Starts a sequence of [UniversalModifier]s. The block will be provided with a [UniversalModifierSequence] that can
     * be used to add [UniversalModifier]s.
     *
     * This is a Java-friendly overload of [beginModifierSequence] that accepts a [Consumer].
     *
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    fun beginModifierSequence(block: Consumer<UniversalModifierSequence>) =
        beginModifierSequence { block.accept(this) }

    /**
     * Starts a sequence of [UniversalModifier]s from an absolute time value (adjusts [modifierStartTime]).
     *
     * @param newModifierStartTime The new value for [modifierStartTime].
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginAbsoluteSequence(
        newModifierStartTime: Float,
        propagateChildren: Boolean = true,
        crossinline block: UniversalModifierSequence.() -> Unit
    ) {
        val prevModifierStartTime = modifierStartTime

        adjustAbsoluteSequenceTime(newModifierStartTime, propagateChildren)

        try {
            beginModifierSequence(block)
        } finally {
            restoreAbsoluteSequenceTime(prevModifierStartTime, propagateChildren)
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s from an absolute time value (adjusts [modifierStartTime]).
     *
     * This is a Java-friendly overload of [beginAbsoluteSequence] that accepts a [Consumer].
     *
     * @param newModifierStartTime The new value for [modifierStartTime].
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmOverloads
    fun beginAbsoluteSequence(
        newModifierStartTime: Float,
        propagateChildren: Boolean = true,
        block: Consumer<UniversalModifierSequence>
    ) = beginAbsoluteSequence(newModifierStartTime, propagateChildren) { block.accept(this) }

    /**
     * Adjusts [modifierStartTime] to a new absolute time value. **This is used internally for [beginAbsoluteSequence]
     * but is made public for inlining, and should not be used externally**.
     *
     * @param newModifierStartTime The new [modifierStartTime].
     * @param propagateChildren Whether to propagate [newModifierStartTime] to children. Defaults to `true`.
     */
    fun adjustAbsoluteSequenceTime(newModifierStartTime: Float, propagateChildren: Boolean = true) {
        modifierDelay += newModifierStartTime - modifierStartTime

        if (propagateChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.adjustAbsoluteSequenceTime(newModifierStartTime)
            }
        }
    }

    /**
     * Restores [modifierStartTime] to its previous absolute time value. **This is used internally for
     * [beginAbsoluteSequence] but is made public for inlining, and should not be used externally**.
     *
     * @param prevModifierStartTime The previous [modifierStartTime].
     * @param propagateChildren Whether to propagate [prevModifierStartTime]. Defaults to `true`.
     */
    fun restoreAbsoluteSequenceTime(prevModifierStartTime: Float, propagateChildren: Boolean = true) {
        modifierDelay += prevModifierStartTime - modifierStartTime

        if (!Precision.almostEquals(prevModifierStartTime, modifierStartTime)) {
            throw IllegalStateException(
                "${this::class.simpleName}'s modifierStartTime at the end of absolute sequence is " +
                        "not the same as at the beginning (begin=$prevModifierStartTime end=$modifierStartTime)"
            )
        }

        if (propagateChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.restoreAbsoluteSequenceTime(prevModifierStartTime)
            }
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s with a (cumulative) relative delay applied.
     *
     * @param delay The offset in seconds from current time. Note that this stacks with other nested sequences.
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginDelayedSequence(
        delay: Float,
        propagateChildren: Boolean = true,
        crossinline block: UniversalModifierSequence.() -> Unit
    ) {
        val oldDelay = modifierDelay
        addDelay(delay, propagateChildren)

        try {
            beginModifierSequence(block)
        } finally {
            addDelay(-delay, propagateChildren)

            val newDelay = modifierDelay

            if (!Precision.almostEquals(oldDelay, newDelay)) {
                throw IllegalStateException("${this::class.simpleName}'s modifierDelay at the end of delayed sequence " +
                    "is not the same as at the beginning (begin=$oldDelay end=$newDelay)")
            }
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s with a (cumulative) relative delay applied.
     *
     * This is a Java-friendly overload of [beginDelayedSequence] that accepts a [Consumer].
     *
     * @param delay The offset in seconds from current time. Note that this stacks with other nested sequences.
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmOverloads
    fun beginDelayedSequence(
        delay: Float,
        propagateChildren: Boolean = true,
        block: Consumer<UniversalModifierSequence>
    ) = beginDelayedSequence(delay, propagateChildren) { block.accept(this) }

    //endregion

    //region Modifiers - Translation

    /**
     * Smoothly adjusts this [UIComponent]'s [translationX] and [translationY] over time.
     *
     * @param x The final [translationX] to reach at the end of the [UniversalModifier].
     * @param y The final [translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [translationX] over time.
     *
     * @param value The final [translationX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [translationY] over time.
     *
     * @param value The final [translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Move

    /**
     * Smoothly adjusts this [UIComponent]'s [mX] and [mY] over time.
     *
     * @param x The final [mX] to reach at the end of the [UniversalModifier].
     * @param y The final [mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [mX] over time.
     *
     * @param value The final [mX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [mY] over time.
     *
     * @param value The final [mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Scale

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleX] and [mScaleY] over time.
     *
     * @param value The final [mScaleX] and [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleXY, duration, easing) {
            finalValues[0] = value
            finalValues[1] = value
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleX] over time.
     *
     * @param value The final [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleY] over time.
     *
     * @param value The final [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Alpha

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] over time.
     *
     * @param value The final [alpha] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] to 1 over time.
     *
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] from 0 to 1 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeInFromZero(duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) {
            hasInitialValues = true
            initialValues[0] = 0f
            finalValues[0] = 1f
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] to 0 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] from 1 to 0 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeOutFromOne(duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) {
            hasInitialValues = true
            initialValues[0] = 1f
            finalValues[0] = 0f
        }

    //endregion

    //region Modifiers - Color

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param color The final color to reach at the end of the [UniversalModifier], in 0xRRGGBB format.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(color: Long, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(
            red = ((color ushr 16) and 0xFF) / 255f,
            green = ((color ushr 8) and 0xFF) / 255f,
            blue = (color and 0xFF) / 255f,
            duration = duration,
            easing = easing
        )

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param color The final [Color4] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(color.red, color.green, color.blue, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param red The final red component of [color] to reach at the end of the [UniversalModifier], in the
     * range [0, 1].
     * @param green The final green component of [color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param blue The final blue component of [color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(red: Float, green: Float, blue: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Color, duration, easing) {
            finalValues[0] = red
            finalValues[1] = green
            finalValues[2] = blue
        }

    //endregion

    //region Modifiers - Rotation

    /**
     * Smoothly adjusts this [UIComponent]'s [mRotation] over time.
     *
     * @param value The final [mRotation] to reach at the end of the [UniversalModifier], in degrees.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Rotation, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Size

    /**
     * Smoothly adjusts this [UIComponent]'s [width] and [height] over time.
     *
     * @param width The final [width] to reach at the end of the [UniversalModifier].
     * @param height The final [height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun sizeTo(width: Float, height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Size, duration, easing) {
            finalValues[0] = width
            finalValues[1] = height
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [width] over time.
     *
     * @param width The final [width] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun widthTo(width: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Width, duration, easing) { finalValues[0] = width }

    /**
     * Smoothly adjusts this [UIComponent]'s [height] over time.
     *
     * @param height The final [height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun heightTo(height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Height, duration, easing) { finalValues[0] = height }

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

        mChildren?.fastForEach { child ->
            if (child is UIComponent) {
                child.onInvalidateInputBindings()
            }
        }
    }

    /**
     * Called when a key is pressed.
     */
    open fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding is IEntity && inputBinding.parent == this) {
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
                if (child is ITouchArea && child.contains(localX, localY)) {
                    if (child.onAreaTouched(event, localX - child.absoluteX, localY - child.absoluteY)) {
                        inputBindings[event.pointerID] = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("UIComponent", "A child entity was removed during touch event propagation.", e)
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

    //region Timekeeping

    /**
     * Whether [IFrameBasedClock.processFrame] should be automatically invoked on this [UIComponent]'s [customClock] in
     * [onUpdate]. This should only be set to false in scenarios where the clock is updated elsewhere.
     */
    var processCustomClock = true

    private var customClock: IFrameBasedClock? = null
    // Cache inherited clock here to avoid parent tree climbing.
    private var inheritedClock: IFrameBasedClock? = null

    /**
     * The [IFrameBasedClock] of this [UIComponent]. Used for keeping track of time across frames. By default, this is
     * inherited from [parent].
     *
     * If set, then the provided value is used as a custom clock and [parent]'s [IFrameBasedClock] is ignored.
     */
    override var clock: IFrameBasedClock?
        get() = customClock ?: inheritedClock
        set(value) {
            customClock = value
            updateClock(inheritedClock)
        }

    /**
     * The current frame's time as observed by this class' [clock].
     */
    val time
        get() = clock?.timeInfo

    override fun updateClock(clock: IFrameBasedClock?) {
        inheritedClock = clock
        val currentClock = this.clock

        if (currentClock != null) {
            if (loadState == LoadState.NotLoaded) {
                loadState = LoadState.Ready
            }
        } else {
            if (loadState == LoadState.Loaded) {
                onUnload()
            }

            loadState = LoadState.NotLoaded
        }

        background?.updateClock(currentClock)
        foreground?.updateClock(currentClock)

        mChildren?.fastForEach {
            @Suppress("UNCHECKED_CAST")
            (it as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
        }
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
            UIBox().apply {
                paintStyle = PaintStyle.Outline
                color = Color4.White
                lineWidth = 1f
            }
        }
    }

}

/**
 * A function that applies a [Theme] to a [UIComponent].
 */
typealias ThemeApplier = UIComponent.(theme: Theme) -> Unit

/**
 * Combines two [ThemeApplier]s into one.
 *
 * The original [ThemeApplier] will be applied first if present, followed by [other].
 *
 * @param other The other [ThemeApplier] to combine with.
 * @return A new [ThemeApplier] that applies both the original and the other [ThemeApplier].
 */
operator fun ThemeApplier?.plus(other: ThemeApplier): ThemeApplier = { theme ->
    this@plus?.invoke(this, theme)
    other.invoke(this, theme)
}
