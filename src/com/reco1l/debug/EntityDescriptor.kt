package com.reco1l.debug

import com.reco1l.andengine.*
import com.reco1l.andengine.text.*
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.reflect.full.*


class EntityDescriptor : DesplegablePanel() {


    init {
        title.text = "Entity Descriptor"

        content.width = PANEL_WIDTH
        content.height = PANEL_HEIGHT
        content.attachChild(ExtendedText().apply {
            font = ResourceManager.getInstance().getFont("smallFont")
        })
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val entity = EntityInspector.SELECTED_ENTITY
        if (entity != null) {
            content.get<ExtendedText>(0).text = """
                ${entity::class.simpleName ?: entity::class.superclasses.first().simpleName ?: "Unknown"}
                superclass: ${entity::class.superclasses.firstOrNull()?.simpleName ?: ""}
                isVisible: ${entity.isVisible}
                x: ${entity.x}
                y: ${entity.y}
                width: ${entity.getWidth()}
                height: ${entity.getHeight()}
                scale: ${entity.scaleX},${entity.scaleY}
                scaleCenter: ${entity.scaleCenterX},${entity.scaleCenterY}
                rotation: ${entity.rotation}
                rotationCenter: ${entity.rotationCenterX},${entity.rotationCenterY}
                ${if (entity is ExtendedEntity) """
                anchor: ${Anchor.getName(entity.anchor, false)}
                origin: ${Anchor.getName(entity.origin, false)}
                translation: ${entity.translationX},${entity.translationY}
                """ else ""}
            """.trimIndent()
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {
        private const val PANEL_WIDTH = 300f
        private const val PANEL_HEIGHT = 400f
    }
}
