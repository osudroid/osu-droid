package com.reco1l.andengine

import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.entity.IEntity


fun IEntity.text(builder: ExtendedText.() -> Unit): ExtendedText {
    return ExtendedText().apply(builder).also(::attachChild)
}

fun IEntity.container(builder: Container.() -> Unit): Container {
    return Container().apply(builder).also(::attachChild)
}

fun IEntity.linearContainer(builder: LinearContainer.() -> Unit): LinearContainer {
    return LinearContainer().apply(builder).also(::attachChild)
}

fun IEntity.constraintContainer(builder: ConstraintContainer.() -> Unit): ConstraintContainer {
    return ConstraintContainer().apply(builder).also(::attachChild)
}

fun IEntity.scrollableContainer(builder: ScrollableContainer.() -> Unit): ScrollableContainer {
    return ScrollableContainer().apply(builder).also(::attachChild)
}

fun IEntity.sprite(builder: ExtendedSprite.() -> Unit): ExtendedSprite {
    return ExtendedSprite().apply(builder).also(::attachChild)
}

fun IEntity.box(builder: Box.() -> Unit): Box {
    return Box().apply(builder).also(::attachChild)
}

fun IEntity.circle(builder: Circle.() -> Unit): Circle {
    return Circle().apply(builder).also(::attachChild)
}

fun IEntity.triangle(builder: Triangle.() -> Unit): Triangle {
    return Triangle().apply(builder).also(::attachChild)
}

fun IEntity.button(builder: Button.() -> Unit): Button {
    return Button().apply(builder).also(::attachChild)
}

fun IEntity.collapsibleCard(builder: Card.() -> Unit): Card {
    return Card().apply(builder).also(::attachChild)
}