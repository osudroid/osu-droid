package com.edlplan.framework.support.batch.object;

import android.opengl.GLES32;
import android.opengl.Matrix;

import com.edlplan.framework.support.batch.AbstractBatch;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.batch.StoryboardBatchShader;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.util.BufferUtil;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.util.GLState;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class TextureQuadBatch extends AbstractBatch<ATextureQuad> {

    private static final int SIZE_PER_QUAD = 4 * 8;

    private static final int STEP = (2 + 2 + 4) * 4;

    private static final int OFFSET_COORD = 2;

    private static final int OFFSET_COLOR = OFFSET_COORD + 2;

    private static TextureQuadBatch defaultBatch;
    private final int maxArySize;
    private FloatBuffer buffer;
    private ShortBuffer indicesBuffer;
    private float[] ary;
    private int offset;
    private ITexture bindTexture;

    /**
     * Base MVP matrix set by {@link com.edlplan.framework.support.SupportSprite} from
     * the AndEngine GLState before the storyboard draw call. This is the ortho projection
     * that maps screen-pixel coordinates to NDC.  It is combined with the storyboard
     * camera transform in {@link #applyToGL()} to produce the final MVP.
     */
    public static float[] sBaseGLMatrix = new float[16];

    /**
     * The active {@link GLState} injected by {@link com.edlplan.framework.support.SupportSprite}
     * before each storyboard draw. Used inside {@link #applyToGL()} so that shader and buffer
     * bindings go through {@link GLState}'s cache instead of bypassing it with raw GLES20 calls.
     * Reset to {@code null} after the draw completes.
     */
    public static GLState sGLState = null;

    static {
        // Default: identity (will be overwritten the first time SupportSprite.draw() runs)
        Matrix.setIdentityM(sBaseGLMatrix, 0);
    }

    private TextureQuadBatch(int size) {
        if (size > Short.MAX_VALUE / 4 - 10) {
            throw new IllegalArgumentException("过大的QuadBatch");
        }
        maxArySize = size * SIZE_PER_QUAD;
        ary = new float[maxArySize];
        buffer = BufferUtil.createFloatBuffer(maxArySize);
        indicesBuffer = BufferUtil.createShortBuffer(size * 6);
        short[] list = new short[size * 6];
        final int l = list.length;
        short j = 0;
        for (int i = 0; i < l; i += 6) {
            list[i] = j++;
            list[i + 1] = list[i + 3] = j++;
            list[i + 2] = list[i + 5] = j++;
            list[i + 4] = j++;
        }
        indicesBuffer.put(list);
        indicesBuffer.position(0);
    }

    public static TextureQuadBatch getDefaultBatch() {
        if (defaultBatch == null) {
            defaultBatch = new TextureQuadBatch(1023);
        }
        return defaultBatch;
    }

    @Override
    protected void onBind() {

    }

    @Override
    protected void onUnbind() {

    }

    @Override
    public void add(ATextureQuad textureQuad) {

        if (textureQuad.texture == null) {
            return;
        }

        if (!isBind()) {
            bind();
        }

        if (textureQuad.texture.getTexture() != bindTexture) {
            flush();
            bindTexture = textureQuad.texture.getTexture();
        }

        textureQuad.write(ary, offset);
        offset += SIZE_PER_QUAD;

        if (offset == maxArySize) {
            flush();
        }
    }

    @Override
    protected void clearData() {
        offset = 0;
        buffer.position(0);
        buffer.limit(maxArySize);
    }

    @Override
    protected boolean applyToGL() {
        if (offset == 0) return false;

        StoryboardBatchShader shader = StoryboardBatchShader.getInstance();
        if (!shader.ensureCompiled()) return true; // data lost, clear it anyway

        // --- Build final MVP = base ortho (screen→NDC) × camera (OSB→screen) ----
        float[] camMatrix = BatchEngine.shaderGlobals.camera.getFinalMatrix().data;
        float[] mvp = new float[16];
        Matrix.multiplyMM(mvp, 0, sBaseGLMatrix, 0, camMatrix, 0);

        // --- Bind shader via GLState so its cache stays authoritative ------------
        final GLState glState = sGLState;
        if (glState != null) {
            glState.useProgram(shader.getProgramID());
        } else {
            GLES32.glUseProgram(shader.getProgramID());
        }

        // --- Upload MVP uniform -------------------------------------------------
        if (shader.getUMVPLoc() >= 0) {
            GLES32.glUniformMatrix4fv(shader.getUMVPLoc(), 1, false, mvp, 0);
        }

        // --- Bind texture -------------------------------------------------------
        // osu!droid fix (Issue 28): route activeTexture + bindTexture through GLState
        // so its per-unit texture cache stays authoritative. The old code called
        // GLES32.glActiveTexture / glBindTexture directly, leaving GLState's
        // mCurrentBoundTextureIDs[0] stale. The next sprite that happened to carry
        // the same cached texture ID as was bound *before* the storyboard batch
        // would skip its glBindTexture call and render with the storyboard texture.
        GLWrapped.blend.setIsPreM(bindTexture.getTextureOptions().mPreMultiplyAlpha);
        if (glState != null) {
            glState.activeTexture(GLES32.GL_TEXTURE0);
            glState.bindTexture(bindTexture.getHardwareTextureID());
        } else {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
            GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, bindTexture.getHardwareTextureID());
        }
        if (shader.getUTextureLoc() >= 0) {
            GLES32.glUniform1i(shader.getUTextureLoc(), 0);
        }

        // --- Upload vertex data (client-side array, no VBO) ---------------------
        // Unbind any VBO so glVertexAttribPointer reads from the client buffer.
        // Use GLState for GL_ARRAY_BUFFER so the cache reflects the unbind.
        if (glState != null) {
            glState.bindArrayBuffer(0);
        } else {
            GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER, 0);
        }
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0);

        buffer.position(0);
        buffer.put(ary, 0, offset);

        // a_position  (2 floats at stride STEP, byte offset 0)
        buffer.position(0);
        GLES32.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_POSITION);
        GLES32.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_POSITION, 2,
                GLES32.GL_FLOAT, false, STEP, buffer);

        // a_texCoord  (2 floats at stride STEP, byte offset 2*4 = 8)
        buffer.position(OFFSET_COORD);
        GLES32.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_TEXCOORD);
        GLES32.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_TEXCOORD, 2,
                GLES32.GL_FLOAT, false, STEP, buffer);

        // a_color     (4 floats at stride STEP, byte offset 4*4 = 16)
        buffer.position(OFFSET_COLOR);
        GLES32.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_COLOR);
        GLES32.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_COLOR, 4,
                GLES32.GL_FLOAT, false, STEP, buffer);

        // --- Draw ---------------------------------------------------------------
        indicesBuffer.position(0);
        GLWrapped.drawElements(GLES32.GL_TRIANGLES,
                offset / SIZE_PER_QUAD * 6,
                GLES32.GL_UNSIGNED_SHORT,
                indicesBuffer);

        // --- Restore GL state so AndEngine's GLState cache remains authoritative -
        // Disable the storyboard-specific attrib array (texCoord slot = 1, color slot = 2)
        // and re-enable the two that AndEngine always expects to be active:
        //   slot 0 = ATTRIBUTE_POSITION, slot 1 = ATTRIBUTE_COLOR
        GLES32.glDisableVertexAttribArray(StoryboardBatchShader.ATTRIB_TEXCOORD);
        GLES32.glDisableVertexAttribArray(StoryboardBatchShader.ATTRIB_COLOR);
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION);
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

        // Release the shader binding through GLState so the cache reflects program = 0.
        if (glState != null) {
            glState.useProgram(0);
        } else {
            GLES32.glUseProgram(0);
        }

        return true;
    }

}
