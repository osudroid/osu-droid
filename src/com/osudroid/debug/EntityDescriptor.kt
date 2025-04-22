package com.osudroid.debug

import com.reco1l.andengine.text.*
import org.anddev.andengine.entity.IEntity
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

class EntityDescriptor : CollapsiblePanel() {


    init {
        title.text = "Entity Descriptor"

        content.width = PANEL_WIDTH
        content.height = PANEL_HEIGHT - TITLE_BAR_HEIGHT
        content.attachChild(ExtendedText().apply {
            font = ResourceManager.getInstance().getFont("xs")
        })
    }


    @Suppress("UNCHECKED_CAST")
    override fun onManagedUpdate(deltaTimeSec: Float) {

        val entity = EntityInspector.SELECTED_ENTITY

        if (entity != null) {
            val clazz = entity::class
            val superclass = clazz.superclasses.firstOrNull()

            val readableMembers = clazz.memberProperties.joinToString("") { member ->
                member as KProperty1<IEntity, Any?>
                member.isAccessible = true

                try {
                    "${member.name}: ${member.getValue(entity, member)}\n"
                } catch (_: Exception) {
                    ""
                }
            }

            content.get<ExtendedText>(0).text = """
${clazz.simpleName ?: superclass?.simpleName ?: "Unknown"}
$readableMembers
            """
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {
        private const val PANEL_WIDTH = 300f
        private const val PANEL_HEIGHT = 400f
    }
}
