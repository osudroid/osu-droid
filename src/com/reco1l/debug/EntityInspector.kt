package com.reco1l.debug

import android.util.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.util.*
import ru.nsu.ccfit.zuev.osu.ResourceManager
import javax.microedition.khronos.opengles.*
import kotlin.math.*
import kotlin.reflect.full.*


private val IEntity.name: String
    get() = this::class.simpleName ?: this::class.superclasses.first().simpleName ?: "Unknown"


class EntityInspector(val engine: Engine) : DesplegablePanel() {


    private val savedData = mutableListOf<EntityData>()

    private val item = ItemEntity()


    private var lastUpdated = System.currentTimeMillis()


    init {
        title.text = "Entity Inspector"

        content.width = PANEL_WIDTH
        content.height = PANEL_HEIGHT - TITLE_BAR_HEIGHT
        content.attachChild(object : ExtendedEntity() {

            private var drawingIndex = 0


            init {
                mChildren = SmartList(0)
                height = FitContent
                width = FitContent

                item.parent = this
            }


            private fun drawItem(data: EntityData, gl: GL10, camera: Camera) {

                data.positionY = contentHeight

                item.apply {
                    name.text = data.entity.name.prependIndent("    ".repeat(data.level)).padEnd(32, ' ')

                    y = data.positionY
                    background!!.color = if (drawingIndex % 2 == 0) ColorARGB(0xFF232334) else ColorARGB(0xFF1C1C2A)
                    foreground!!.alpha = if (data.entity == SELECTED_ENTITY) 0.2f else 0f

                    collapse.apply {
                        rotation = if (data.isExpanded) 0f else 180f
                        alpha = if (data.isExpanded) 0.75f else 0.25f
                        isVisible = data.children.isNotEmpty()
                    }

                }.onDraw(gl, camera)

                contentHeight += item.height
                contentWidth = max(contentWidth, item.width)
                drawingIndex++

                if (data.isExpanded) {
                    for (child in data.children) {
                        drawItem(child, gl, camera)
                    }
                }
            }

            override fun onManagedDrawChildren(gl: GL10, camera: Camera) {
                contentWidth = PANEL_WIDTH
                contentHeight = 0f

                drawingIndex = 0
                for (data in savedData) {
                    drawItem(data, gl, camera)
                }

                item.width = contentWidth
            }

            private fun onTouchItem(event: TouchEvent, localY: Float, localX: Float, data: EntityData): Boolean {

                if (localY >= data.positionY && localY < data.positionY + item.height) {
                    if (localX < item.collapse.width) {
                        data.isExpanded = !data.isExpanded
                    } else {
                        SELECTED_ENTITY = data.entity
                    }
                    return true
                }

                if (data.isExpanded) {
                    for (child in data.children) {
                        if (onTouchItem(event, localY, localX, child)) {
                            return true
                        }
                    }
                }

                return false
            }

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (event.isActionUp) {
                    for (item in savedData) {
                        if (onTouchItem(event, localY, localX, item)) {
                            break
                        }
                    }
                }
                return true
            }
        })
    }



    private fun updateEntityData(list: MutableList<EntityData>, entity: IEntity, level: Int) {

        val data = list.firstOrNull { it.entity == entity } ?: EntityData(entity, level).also { list.add(it) }

        if (entity is Scene && entity.childScene != null) {
            updateEntityData(data.children, entity.childScene, level + 1)
        }

        for (i in 0 until entity.childCount) {
            updateEntityData(data.children, entity.getChild(i), level + 1)
        }

        data.children.removeAll {
            entity.getChildIndex(it.entity) < 0 && (entity !is Scene || entity.childScene != it.entity)
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        item.onUpdate(deltaTimeSec)
        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if ((System.currentTimeMillis() - lastUpdated) > UPDATE_TIME) {
            lastUpdated = System.currentTimeMillis()

            val scene = engine.scene
            if (scene != null) {
                updateEntityData(savedData, scene, 0)
            }

            val hud = engine.camera.hud
            if (hud != null) {
                updateEntityData(savedData, hud, 0)
            }

            savedData.removeAll { item -> item.entity != engine.camera.hud && item.entity != engine.scene }
        }

        super.onManagedDraw(gl, camera)
    }


    private data class EntityData(
        val entity: IEntity,
        val level: Int,
        val children: MutableList<EntityData> = mutableListOf(),
        var positionY: Float = 0f,
        var isExpanded: Boolean = false
    )


    private inner class ItemEntity : LinearContainer() {

        val collapse = Triangle()

        val name = ExtendedText()


        init {
            orientation = Orientation.Horizontal
            background = Box()
            foreground = Box().apply { color = ColorARGB(0x0000FF00) }

            +collapse.apply {
                color = ColorARGB.White
                padding = Vec4(12f, 8f)
                width = 12f
                height = 12f
                rotationCenter = Anchor.Center
            }

            +name.apply {
                font = ResourceManager.getInstance().getFont("smallFont")
                text = " ".repeat(32)
            }
        }

    }


    companion object {

        /**
         * The currently selected entity.
         */
        var SELECTED_ENTITY: IEntity? = null
            private set(value) {
                if (field != value) {
                    field = value

                    if (value != null) {
                        Log.i("EntityBrowser", "Selected: ${value::class.simpleName}")
                    }
                }
            }


        private const val PANEL_WIDTH = 400f
        private const val PANEL_HEIGHT = 400f
        private const val UPDATE_TIME = 1000L

    }
}
