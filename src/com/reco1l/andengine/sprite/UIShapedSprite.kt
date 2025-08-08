package com.reco1l.andengine.sprite

import android.opengl.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*

class UIShapedSprite : UISprite() {

    /**
     * The shape that will be used to mask the sprite.
     */
    var shape: UIBufferedComponent<*>?
        get() = background as? UIBufferedComponent<*>
        set(value) {
            if (background != value) {
                super.background = value
                onShapeChanged()
            }
        }


    init {
        depthInfo = DepthInfo.None
        scaleType = ScaleType.Crop
    }


    private fun onShapeChanged() {
        shape?.apply {
            clearInfo = if (textureRegion == null) ClearInfo.None else ClearInfo.ClearDepthBuffer
            depthInfo = if (textureRegion == null) DepthInfo.None else SHAPE_DEPTH_INFO
        }
    }


    override fun onTextureRegionChanged() {
        super.onTextureRegionChanged()

        depthInfo = if (textureRegion == null) DepthInfo.None else SPRITE_DEPTH_INFO
        onShapeChanged()
    }


    companion object {

        private val SHAPE_DEPTH_INFO = DepthInfo(test = true, mask = true, function = GLES10.GL_ALWAYS)
        private val SPRITE_DEPTH_INFO = DepthInfo(test = true, mask = true, function = GLES10.GL_EQUAL)

    }
}