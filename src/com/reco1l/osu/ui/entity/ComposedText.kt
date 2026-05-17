package com.reco1l.osu.ui.entity

import org.andengine.entity.Entity
import org.andengine.entity.text.Text
import org.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

class ComposedText(x: Float, y: Float, font: Font, max: Int) : Entity(x, y) {

    val tag = Text(x, y, font, "", 16, getGlobal().engine.vertexBufferObjectManager)

    val content = Text(x, y, font, "", max, getGlobal().engine.vertexBufferObjectManager)

    init {
        attachChild(tag)
        attachChild(content)

        // Somehow alpha for Text in AndEngine modifies its brightness :skull:
        tag.alpha = 0f
        content.alpha = 0f
    }

    fun setTagText(text: String) {
        tag.text = text
        content.setPosition(tag.width, tag.y)
    }

    fun setContentText(text: String) {
        content.text = text
        content.setPosition(tag.width, tag.y)
    }
}
