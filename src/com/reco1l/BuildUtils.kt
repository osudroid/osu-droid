@file:JvmName("BuildUtils")

package com.reco1l

/**
 * Whether to use textures or not.
 */
const val noTexturesMode = false

/**
 * Whether to keep the shape of the textures if no texture mode is enabled.
 *
 * Note: This can increase texture loading time.
 */
const val keepTexturesShapeInNoTexturesMode = false