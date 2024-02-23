package com.edlplan.framework.support.batch.object;

import android.opengl.GLES20;

import com.edlplan.framework.support.batch.AbstractBatch;
import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.support.graphics.GLWrapped;
import com.edlplan.framework.support.util.BufferUtil;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram;
import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.util.GLState;

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

            GLWrapped.blend.setIsPreM(bindTexture.getTextureOptions().mPreMultiplyAlpha);

            GLState pGL = BatchEngine.pGL;
            pGL.disableCulling();

            // Compiling and linking the shader in case it wasn't yet and apply all attribute pointers.
            ShaderProgram shader = PositionColorTextureCoordinatesShaderProgram.getInstance();
            shader.bind(pGL, Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT);

            buffer.position(0);
            buffer.put(ary, 0, offset);
            buffer.position(0).limit(offset);

            bindTexture.bind(pGL);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, offset / SIZE_PER_QUAD * 6, GLES20.GL_UNSIGNED_SHORT, indicesBuffer);
            return true;
        } else {
            return false;
        }
    }

}
