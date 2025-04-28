package com.reco1l.andengine

import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.entity.IEntity


inline fun IEntity.text(builder: ExtendedText.() -> Unit): ExtendedText {
    return ExtendedText().apply(builder).also(::attachChild)
}

inline fun IEntity.container(builder: Container.() -> Unit): Container {
    return Container().apply(builder).also(::attachChild)
}

inline fun IEntity.linearContainer(builder: LinearContainer.() -> Unit): LinearContainer {
    return LinearContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.constraintContainer(builder: ConstraintContainer.() -> Unit): ConstraintContainer {
    return ConstraintContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.scrollableContainer(builder: ScrollableContainer.() -> Unit): ScrollableContainer {
    return ScrollableContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.sprite(builder: ExtendedSprite.() -> Unit): ExtendedSprite {
    return ExtendedSprite().apply(builder).also(::attachChild)
}

inline fun IEntity.box(builder: Box.() -> Unit): Box {
    return Box().apply(builder).also(::attachChild)
}

inline fun IEntity.circle(builder: Circle.() -> Unit): Circle {
    return Circle().apply(builder).also(::attachChild)
}

inline fun IEntity.triangle(builder: Triangle.() -> Unit): Triangle {
    return Triangle().apply(builder).also(::attachChild)
}

inline fun IEntity.textButton(builder: TextButton.() -> Unit): TextButton {
    return TextButton().apply(builder).also(::attachChild)
}

inline fun IEntity.iconButton(builder: IconButton.() -> Unit): IconButton {
    return IconButton().apply(builder).also(::attachChild)
}

inline fun IEntity.collapsibleCard(builder: Card.() -> Unit): Card {
    return Card().apply(builder).also(::attachChild)
}

inline fun IEntity.badge(builder: Badge.() -> Unit): Badge {
    return Badge().apply(builder).also(::attachChild)
}

inline fun IEntity.labeledBadge(builder: LabeledBadge.() -> Unit): LabeledBadge {
    return LabeledBadge().apply(builder).also(::attachChild)
}

inline fun IEntity.compoundText(builder: CompoundText.() -> Unit): CompoundText {
    return CompoundText().apply(builder).also(::attachChild)
}