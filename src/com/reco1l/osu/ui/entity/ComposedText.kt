package com.reco1l.osu.ui.entity

import org.andengine.entity.Entity
import org.andengine.entity.text.ChangeableText
import org.andengine.opengl.font.Font

class ComposedText(x: Float, y: Float, font: Font, max: Int) : Entity(x, y) {

    val tag = ChangeableText(x, y, font, "", 16)

    val content = ChangeableText(x, y, font, "", max)

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
