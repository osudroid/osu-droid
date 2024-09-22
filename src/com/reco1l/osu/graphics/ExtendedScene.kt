package com.reco1l.osu.graphics

import org.anddev.andengine.entity.scene.*


/**
 * Scene with extended functionality.
 */
class ExtendedScene : Scene() {

    /**
     * The time multiplier for the scene.
     *
     * Setting this will affect the speed of every entity attached to this scene.
     */
    var timeMultiplier = 1f


    override fun onManagedUpdate(secElapsed: Float) = super.onManagedUpdate(secElapsed * timeMultiplier)
}