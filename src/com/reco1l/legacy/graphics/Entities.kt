@file:JvmName("Entities")
package com.reco1l.legacy.graphics

import org.andengine.entity.IEntity
import org.andengine.entity.shape.IAreaShape


/**
 * Scales the shape to fit the given width and height.
 */
fun <T> T.scaleCropCenter(targetWidth: Float, targetHeight: Float) where T : IAreaShape, T : IEntity
{
    setScale(if (targetWidth > width) targetWidth / width else targetHeight / height)
}