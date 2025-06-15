package com.reco1l.andengine.component

/**
 * Represents the mode of attachment for a component.
 */
enum class AttachmentMode {
    /**
     * The component is not attached to any parent.
     */
    None,

    /**
     * The component is attached to a parent as a child.
     */
    Child,

    /**
     * The component is attached to a parent as a decorator.
     */
    Decorator,
}