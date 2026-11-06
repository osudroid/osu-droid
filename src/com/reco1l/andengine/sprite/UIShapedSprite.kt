package com.reco1l.andengine.sprite

import org.andengine.graphics.CompareFunction
import org.andengine.graphics.ClearBuffers
import org.andengine.graphics.DepthState
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
        depthState = DepthState.Disabled
        scaleType = ScaleType.Crop
    }


    private fun onShapeChanged() {
        shape?.apply {
            clearBuffers = if (textureRegion == null) ClearBuffers.None else ClearBuffers.Depth
            depthState = if (textureRegion == null) DepthState.Disabled else SHAPE_DEPTH
        }
    }


    override fun onTextureRegionChanged() {
        super.onTextureRegionChanged()

        depthState = if (textureRegion == null) DepthState.Disabled else SPRITE_DEPTH
        onShapeChanged()
    }


    companion object {

        private val SHAPE_DEPTH = DepthState(CompareFunction.Always, true)
        private val SPRITE_DEPTH = DepthState(CompareFunction.Equal, true)

    }
}