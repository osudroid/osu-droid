package com.reco1l.andengine.container

import android.util.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.Orientation.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.input.touch.*
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * A recyclable container is a scrollable container that holds components
 * that are recycled once they are no longer visible.
 */
open class UIRecyclerContainer<T : Any, C : RecyclableComponent<T>>(private val initialPoolSize: Int = 8) : UIScrollableContainer() {

    /**
     * The components that currently being bound.
     *
     * This contains those components that are inside of the container's bounds or the container's visible area,
     * and those that are not recyclable determined by [isRecyclable][RecyclableComponent.isRecyclable] property.
     */
    val boundComponents: MutableMap<T, C> = ConcurrentHashMap()

    /**
     * The component wrapper.
     */
    val componentWrapper = RecyclerComponentWrapper()


    /**
     * The component supplier.
     */
    var onCreateComponent: (() -> C)? = null
        set(value) {
            field = value
            initializeComponents()
        }

    /**
     * The data to bind to the components.
     */
    var data: List<T> = emptyList()


    var orientation by componentWrapper::orientation
    var spacing by componentWrapper::spacing


    private val componentPool = LinkedList<C>()


    init {
        super.attachChild(componentWrapper)
    }


    private fun initializeComponents() {
        boundComponents.clear()
        componentPool.clear()
        componentWrapper.detachChildren()

        repeat(initialPoolSize) {
            createComponentForPool()
        }
    }

    private fun createComponentForPool(): C? {
        val component = onCreateComponent?.invoke() ?: return null
        component.isVisible = false

        componentPool.offer(component)
        componentWrapper.attachChild(component)
        Log.i("UIRecyclerContainer", "Created new component for the pool: ${component::class}")
        return component
    }

    private fun fetchComponentOrFallback(data: T): C {
        return boundComponents[data]
            ?: componentPool.peek()
            ?: createComponentForPool()
            ?: throw IllegalStateException("Failed to create a component for data, make sure pool size is enough.")
    }

    private fun unbindComponent(data: T, component: C) {
        if (component.boundData != null) {
            Log.i("UIRecyclerContainer", "Unbinding component for data: ${component.boundData}")
            component.boundData = null
            component.isVisible = false
            component.onRecycle()

            boundComponents.remove(data)
            componentPool.offer(component)
            Log.v("UIRecyclerContainer", "Unbinding component, pool size: ${componentPool.size}, bound components: ${boundComponents.size}")
        }
    }

    private fun bindComponent(data: T, component: C) {
        if (component.boundData != data) {
            Log.i("UIRecyclerContainer", "Binding component for data: $data")
            component.boundData = data
            component.isVisible = true
            component.onBind(data)

            componentPool.remove(component)
            boundComponents[data] = component
            Log.v("UIRecyclerContainer", "Binding component, pool size: ${componentPool.size}, bound components: ${boundComponents.size}")
        }
    }

    private fun isInContainerBounds(component: C): Boolean {
        // Override the culling check to be straightforward
        // with the carrousel and panels making it fast.

        val containerSize: Float
        val currentScroll: Float
        val positionOnContainer: Float
        val componentSize: Float

        when (orientation) {

            Vertical -> {
                containerSize = height
                currentScroll = scrollY
                positionOnContainer = component.absoluteY - currentScroll
                componentSize = component.height
            }

            Horizontal -> {
                containerSize = width
                currentScroll = scrollX
                positionOnContainer = component.absoluteX - currentScroll
                componentSize = component.width
            }
        }

        return positionOnContainer + componentSize > 0f && positionOnContainer < containerSize
    }


    /**
     * Applies the given data to a component, binding it and executing the block.
     *
     * Since components are recycled, this method will fetch a component from the pool
     * and bind it to the data avoiding it to be recycled.
     */
    fun applyComponent(data: T, block: C.(data: T) -> Unit): C {
        val component = fetchComponentOrFallback(data)
        bindComponent(data, component)
        component.block(data)
        return component
    }


    protected open fun onDrawComponent(gl: GL10, camera: Camera, component: C) {
        // Override this method to customize how components are drawn.
        component.onDraw(gl, camera)
    }


    override fun attachChild(pEntity: IEntity?) {
        throw UnsupportedOperationException("RecyclableContainer does not support attaching children directly.")
    }

    override fun attachChild(pEntity: IEntity?, pIndex: Int): Boolean {
        throw UnsupportedOperationException("RecyclableContainer does not support attaching children directly.")
    }

    /**
     * A wrapper for recyclable components.
     * This container is a direct and unique child of a [UIRecyclerContainer].
     */
    inner class RecyclerComponentWrapper : UILinearContainer() {

        @Suppress("UNCHECKED_CAST")
        override fun getParent(): UIRecyclerContainer<T, C>? {
            return super.getParent() as? UIRecyclerContainer<T, C>
        }

        override fun onContentChanged() {
            val dataList = data

            var contentWidth = 0f
            var contentHeight = 0f

            for (i in dataList.indices) {

                val data = dataList[i]
                val component = fetchComponentOrFallback(data)

                when (orientation) {
                    Horizontal -> {
                        component.x = contentWidth

                        contentWidth += component.width
                        contentHeight = max(contentHeight, component.height)

                        if (dataList.size > 1 && i < dataList.size - 1) {
                            contentWidth += spacing
                        }
                    }

                    Vertical -> {
                        component.y = contentHeight

                        contentHeight += component.height
                        contentWidth = max(contentWidth, component.width)

                        if (dataList.size > 1 && i < dataList.size - 1) {
                            contentHeight += spacing
                        }
                    }
                }

                if (isInContainerBounds(component)) {
                    if (component.isRecyclable && component.boundData != data) {
                        bindComponent(data, component)
                    }
                } else {
                    if (component.isRecyclable && component.boundData != null) {
                        unbindComponent(data, component)
                    }
                }
            }

            this.contentWidth = contentWidth
            this.contentHeight = contentHeight
        }


        override fun onDrawChildren(gl: GL10, camera: Camera) {
            for (component in boundComponents.values) {
                onDrawComponent(gl, camera, component)
            }
        }

        override fun attachChild(entity: IEntity) {
            if (entity !is RecyclableComponent<*>) {
                throw IllegalArgumentException("Entity must be a RecyclableComponent.")
            }
            super.attachChild(entity)
        }

        override fun attachChild(entity: IEntity?, index: Int): Boolean {
            if (entity !is RecyclableComponent<*>) {
                throw IllegalArgumentException("Entity must be a RecyclableComponent.")
            }
            return super.attachChild(entity, index)
        }
    }

}

abstract class RecyclableComponent<T : Any> : UIContainer() {

    /**
     * The current bound data for the component.
     *
     * This should be used only if the component is bound, otherwise this can be extremely volatile
     * to rely on and should be avoided.
     *
     * It is set by the [UIRecyclerContainer] and should not be explicitly changed.
     */
    var boundData: T? = null

    /**
     * Whether the component is bound. This usually indicates [boundData] is not null.
     */
    val isBound: Boolean
        get() = boundData != null


    /**
     * Whether the component can be recycled.
     */
    abstract val isRecyclable: Boolean

    /**
     * Binds the data to the component.
     */
    abstract fun onBind(data: T)

    /**
     * Called when the component is recycled.
     * This is usually used to reset the component state.
     */
    open fun onRecycle() = Unit


    override fun contains(x: Float, y: Float): Boolean {
        return isBound && super.contains(x, y)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!isBound) {
            return false
        }
        return super.onAreaTouched(event, localX, localY)
    }
}