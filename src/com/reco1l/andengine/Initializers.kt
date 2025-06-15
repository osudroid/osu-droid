package com.reco1l.andengine

import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import org.anddev.andengine.entity.IEntity


inline fun IEntity.text(builder: UIText.() -> Unit): UIText {
    return UIText().apply(builder).also(::attachChild)
}

inline fun IEntity.container(builder: UIContainer.() -> Unit): UIContainer {
    return UIContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.linearContainer(builder: UILinearContainer.() -> Unit): UILinearContainer {
    return UILinearContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.constraintContainer(builder: UIConstraintContainer.() -> Unit): UIConstraintContainer {
    return UIConstraintContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.flexContainer(builder: UIFlexContainer.() -> Unit): UIFlexContainer {
    return UIFlexContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.scrollableContainer(builder: UIScrollableContainer.() -> Unit): UIScrollableContainer {
    return UIScrollableContainer().apply(builder).also(::attachChild)
}

inline fun IEntity.sprite(builder: UISprite.() -> Unit): UISprite {
    return UISprite().apply(builder).also(::attachChild)
}

inline fun IEntity.box(builder: UIBox.() -> Unit): UIBox {
    return UIBox().apply(builder).also(::attachChild)
}

inline fun IEntity.circle(builder: UICircle.() -> Unit): UICircle {
    return UICircle().apply(builder).also(::attachChild)
}

inline fun IEntity.triangle(builder: UITriangle.() -> Unit): UITriangle {
    return UITriangle().apply(builder).also(::attachChild)
}

inline fun IEntity.textButton(builder: UITextButton.() -> Unit): UITextButton {
    return UITextButton().apply(builder).also(::attachChild)
}

inline fun IEntity.iconButton(builder: UIIconButton.() -> Unit): UIIconButton {
    return UIIconButton().apply(builder).also(::attachChild)
}

inline fun IEntity.collapsibleCard(builder: UICard.() -> Unit): UICard {
    return UICard().apply(builder).also(::attachChild)
}

inline fun IEntity.badge(builder: UIBadge.() -> Unit): UIBadge {
    return UIBadge().apply(builder).also(::attachChild)
}

inline fun IEntity.labeledBadge(builder: UILabeledBadge.() -> Unit): UILabeledBadge {
    return UILabeledBadge().apply(builder).also(::attachChild)
}

inline fun IEntity.compoundText(builder: CompoundText.() -> Unit): CompoundText {
    return CompoundText().apply(builder).also(::attachChild)
}