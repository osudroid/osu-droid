package com.edlplan.framework.support.batch.object;

import com.edlplan.framework.support.batch.AbstractBatch;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.util.BufferUtil;

import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.util.GLHelper;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

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
        if (offset != 0) {

            GLWrapped.blend.setIsPreM(bindTexture.getTextureOptions().mPreMultipyAlpha);

            //handle render operation
            GL10 pGL = BatchEngine.pGL;
            bindTexture.bind(pGL);
            GLHelper.enableTextures(pGL);
            GLHelper.enableTexCoordArray(pGL);
            pGL.glEnableClientState(GL10.GL_COLOR_ARRAY);
            pGL.glShadeModel(GL10.GL_SMOOTH);
            GLHelper.enableVertexArray(pGL);
            GLHelper.disableCulling(pGL);

            buffer.position(0);
            buffer.put(ary, 0, offset);
            buffer.position(0).limit(offset);

            pGL.glVertexPointer(2, GL10.GL_FLOAT, STEP, buffer);
            buffer.position(OFFSET_COORD);
            pGL.glTexCoordPointer(2, GL10.GL_FLOAT, STEP, buffer);
            buffer.position(OFFSET_COLOR);
            pGL.glColorPointer(4, GL10.GL_FLOAT, STEP, buffer);


            pGL.glDrawElements(
                    GL10.GL_TRIANGLES,
                    offset / SIZE_PER_QUAD * 6,
                    GL10.GL_UNSIGNED_SHORT,
                    indicesBuffer);

            pGL.glDisableClientState(GL10.GL_COLOR_ARRAY);


            /*shader.useThis();
            shader.loadShaderGlobals(BatchEngine.getShaderGlobals());
            shader.loadTexture(bindTexture);

            buffer.position(0);
            buffer.put(ary, 0, offset);
            buffer.position(0).limit(offset);

            shader.loadBuffer(buffer);

            BatchEngine.pGL.glDrawElements(
                    GL10.GL_TRIANGLES,
                    offset / SIZE_PER_QUAD * 6,
                    GL10.GL_UNSIGNED_SHORT,
                    indicesBuffer);*/
            return true;
        } else {
            return false;
        }
    }

}
