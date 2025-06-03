package com.reco1l.andengine.component


/**
 * Culling mode for entities.
 */
enum class CullingMode {

    /**
     * Culling is disabled, entities are always drawn.
     */
    Disabled,

    /**
     * Culling is enabled, entities are drawn only if they are within the camera's bounds.
     */
    CameraBounds,

    /**
     * Culling is enabled, entities are drawn only if they are within the parent's bounds.
     */
    ParentBounds,
}