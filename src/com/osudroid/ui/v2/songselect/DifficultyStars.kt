package com.osudroid.ui.v2.songselect

import android.opengl.GLES32
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.ISkinnable
import org.andengine.opengl.shader.PositionTextureCoordinatesUniformColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.min

/**
 * A row of skinnable `star` sprites visualizing a star rating, like the legacy song menu did:
 * one full star per whole point and a scaled-down star for the fractional part.
 *
 * All stars are drawn from a single buffer with one draw call.
 */
class DifficultyStars : UIBufferedComponent<DifficultyStars.StarsVertexBuffer>(), ISkinnable {

    /**
     * The star rating to display. Values above [MAX_STARS] are capped.
     */
    var rating = 0f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }

    /**
     * The size of a single star in scene units.
     */
    var starSize = 16f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }

    /**
     * The space between two stars.
     */
    var spacing = 2f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }


    private var texture: TextureRegion? = null

    private val uvBuffer = StarsUVBuffer()

    /**
     * Number of stars drawn, including the fractional one.
     */
    private var visibleStars = 0


    init {
        width = MatchContent
        height = MatchContent
        onSkinChanged()
    }


    override fun onSkinChanged() {
        texture = ResourceManager.getInstance().getTexture("star")

        val texture = texture
        if (texture != null) {
            blendInfo = if (texture.texture.textureOptions.mPreMultiplyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
        }
        requestBufferUpdate()
    }

    private fun onStarsChanged() {
        val capped = min(rating, MAX_STARS.toFloat())
        val fullStars = capped.toInt()
        visibleStars = if (capped - fullStars > 0f) fullStars + 1 else fullStars

        contentWidth = if (visibleStars > 0) visibleStars * starSize + (visibleStars - 1) * spacing else 0f
        contentHeight = starSize
        requestBufferUpdate()
    }


    override fun onCreateBuffer(): StarsVertexBuffer {
        return buffer ?: StarsVertexBuffer()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
        uvBuffer.update(texture)
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(shader.uniformMVPMatrixLocation, 1, false, pGLState.modelViewProjectionGLMatrix, 0)
        }

        if (shader.uniformTexture0Location >= 0) {
            GLES32.glUniform1i(shader.uniformTexture0Location, 0)
        }

        if (shader.uniformColorLocation >= 0) {
            GLES32.glUniform4f(shader.uniformColorLocation, drawRed, drawGreen, drawBlue, drawAlpha)
        }

        // This shader uses a uniform color.
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        uvBuffer.beginDraw(pGLState)
    }

    override fun onDrawBuffer(gl: GLState) {
        val texture = texture ?: return
        if (visibleStars == 0) {
            return
        }

        texture.texture.bind(gl)
        super.onDrawBuffer(gl)
    }


    class StarsVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_STAR * MAX_STARS,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        fun update(entity: DifficultyStars) {
            val capped = min(entity.rating, MAX_STARS.toFloat())
            val size = entity.starSize
            var index = 0

            for (i in 0 until entity.visibleStars) {
                // The last star is scaled by the fractional part, centered vertically like the full stars.
                val scale = if (i < capped.toInt()) 1f else capped - capped.toInt()
                val starWidth = size * scale
                val x = i * (size + entity.spacing)
                val y = (size - starWidth) / 2f

                putVertex(index++, x, y)
                putVertex(index++, x, y + starWidth)
                putVertex(index++, x + starWidth, y + starWidth)
                putVertex(index++, x + starWidth, y + starWidth)
                putVertex(index++, x + starWidth, y)
                putVertex(index++, x, y)
            }
        }

        override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
            GLES32.glDrawArrays(drawTopology, 0, VERTICES_PER_STAR * (entity as DifficultyStars).visibleStars)
        }
    }

    class StarsUVBuffer : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_STAR * MAX_STARS,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        fun update(texture: TextureRegion?) {
            val u = texture?.u ?: 0f
            val v = texture?.v ?: 0f
            val u2 = texture?.u2 ?: 0f
            val v2 = texture?.v2 ?: 0f
            var index = 0

            // Every star uses the whole texture region, in the same vertex order as the positions.
            for (i in 0 until MAX_STARS) {
                putVertex(index++, u, v)
                putVertex(index++, u, v2)
                putVertex(index++, u2, v2)
                putVertex(index++, u2, v2)
                putVertex(index++, u2, v)
                putVertex(index++, u, v)
            }
            invalidateOnHardware()
        }
    }


    companion object {
        const val MAX_STARS = 10

        private const val VERTICES_PER_STAR = 6
    }

}
