package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.BlendFactor
import org.andengine.graphics.BlendMode
import org.andengine.graphics.PrimitiveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesPipelineTest {

    @Test
    fun blendFactorOf_reversesBlendFactor() {
        for (factor in BlendFactor.entries) {
            assertEquals(factor, GlesPipeline.blendFactorOf(GlesPipeline.blendFactor(factor)))
        }
        assertNull(GlesPipeline.blendFactorOf(-1))
    }

    @Test
    fun blendModeOf_usesTheSameFactorsForColorAndAlpha() {
        assertEquals(
            BlendMode(BlendFactor.SrcAlpha, BlendFactor.DstAlpha, BlendFactor.SrcAlpha, BlendFactor.DstAlpha),
            GlesPipeline.blendModeOf(GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA)
        )
        assertEquals(BlendMode.Premultiplied, GlesPipeline.blendModeOf(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA))
        assertNull(GlesPipeline.blendModeOf(GLES20.GL_ONE, -1))
        assertNull(GlesPipeline.blendModeOf(-1, GLES20.GL_ONE))
    }

    @Test
    fun primitiveTypeOf_reversesPrimitiveMode() {
        for (type in PrimitiveType.entries) {
            assertEquals(type, GlesPipeline.primitiveTypeOf(GlesPipeline.primitiveMode(type)))
        }
        assertNull(GlesPipeline.primitiveTypeOf(-1))
    }
}
