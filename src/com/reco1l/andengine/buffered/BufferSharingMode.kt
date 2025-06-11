package com.reco1l.andengine.buffered

/**
 * Defines how a buffer is shared between multiple entities.
 */
enum class BufferSharingMode {
    /**
     * The buffer is not shared and is only used by a single entity.
     */
    Off,

    /**
     * The buffer is shared between multiple entities and is updated
     * dynamically each frame according to the entity's data.
     *
     * This mode might be CPU intensive depending on the number of
     * entities and how they update their data, but consumes less
     * memory.
     */
    Dynamic,

    /**
     * The buffer is shared between multiple entities and is updated
     * only once, when the entity is created or when its needed.
     *
     * This mode is the most memory and CPU efficient, but its not as
     * flexible as the dynamic mode, since the data is not updated
     * every frame. This is only recommended for entities where its
     * data does not change.
     */
    Static,
}