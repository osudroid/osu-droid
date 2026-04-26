package com.edlplan.framework.support.batch.object;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.edlplan.framework.support.batch.AbstractBatch;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.batch.StoryboardBatchShader;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.util.BufferUtil;

import org.andengine.opengl.texture.ITexture;

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

        // --- Bind shader --------------------------------------------------------
        GLES20.glUseProgram(shader.programID);

        // --- Upload MVP uniform -------------------------------------------------
        if (shader.uMVPLoc >= 0) {
            GLES20.glUniformMatrix4fv(shader.uMVPLoc, 1, false, mvp, 0);
        }

        // --- Bind texture -------------------------------------------------------
        GLWrapped.blend.setIsPreM(bindTexture.getTextureOptions().mPreMultiplyAlpha);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bindTexture.getHardwareTextureID());
        if (shader.uTextureLoc >= 0) {
            GLES20.glUniform1i(shader.uTextureLoc, 0);
        }

        // --- Upload vertex data (client-side array, no VBO) ---------------------
        // Unbind any VBO so glVertexAttribPointer reads from the client buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        buffer.position(0);
        buffer.put(ary, 0, offset);

        // a_position  (2 floats at stride STEP, byte offset 0)
        buffer.position(0);
        GLES20.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_POSITION);
        GLES20.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_POSITION, 2,
                GLES20.GL_FLOAT, false, STEP, buffer);

        // a_texCoord  (2 floats at stride STEP, byte offset 2*4 = 8)
        buffer.position(OFFSET_COORD);
        GLES20.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_TEXCOORD);
        GLES20.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_TEXCOORD, 2,
                GLES20.GL_FLOAT, false, STEP, buffer);

        // a_color     (4 floats at stride STEP, byte offset 4*4 = 16)
        buffer.position(OFFSET_COLOR);
        GLES20.glEnableVertexAttribArray(StoryboardBatchShader.ATTRIB_COLOR);
        GLES20.glVertexAttribPointer(StoryboardBatchShader.ATTRIB_COLOR, 4,
                GLES20.GL_FLOAT, false, STEP, buffer);

        // --- Draw ---------------------------------------------------------------
        indicesBuffer.position(0);
        GLWrapped.drawElements(GLES20.GL_TRIANGLES,
                offset / SIZE_PER_QUAD * 6,
                GLES20.GL_UNSIGNED_SHORT,
                indicesBuffer);

        // --- Restore state ------------------------------------------------------
        GLES20.glDisableVertexAttribArray(StoryboardBatchShader.ATTRIB_POSITION);
        GLES20.glDisableVertexAttribArray(StoryboardBatchShader.ATTRIB_TEXCOORD);
        GLES20.glDisableVertexAttribArray(StoryboardBatchShader.ATTRIB_COLOR);
        GLES20.glUseProgram(0);

        return true;
    }

}
