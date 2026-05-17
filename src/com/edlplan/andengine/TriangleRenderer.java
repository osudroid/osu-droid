package com.edlplan.andengine;

import android.opengl.GLES20;

import com.edlplan.framework.utils.FloatArraySlice;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.util.GLState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TriangleRenderer {

    private static final TriangleRenderer triangleRenderer = new TriangleRenderer();

    /** Staging buffer for uploading vertex data to the GPU. */
    private ByteBuffer mStagingBuffer;
    private FloatBuffer mStagingFloatBuffer;

    /** GLES VBO handle; 0 means not yet created (or invalidated after context loss). */
    private int mVboId = 0;

    public static TriangleRenderer get() {
        return triangleRenderer;
    }

    /**
     * Must be called from {@code MainActivity.onSurfaceCreated} when the EGL context is
     * recreated so the old VBO handle is discarded and a fresh one is generated on next draw.
     */
    public void resetForContextLoss() {
        // The old VBO ID is invalid; glDeleteBuffers is unnecessary because the context is gone.
        mVboId = 0;
    }

    /**
     * Renders the supplied triangle vertices via a {@code GL_DYNAMIC_DRAW} VBO.
     * <p>
     * Must be called on the GL thread. The caller is responsible for enabling
     * {@link ShaderProgramConstants#ATTRIBUTE_POSITION_LOCATION} before this call.
     * After the call {@code GL_ARRAY_BUFFER} is left unbound (0) and
     * {@code pGLState.mCurrentArrayBufferID} is updated accordingly.
     */
    public void renderTriangles(FloatArraySlice ver, GLState pGLState) {
        final int floatCount = ver.length;
        final int byteCount = floatCount * 4;

        // Grow the staging buffer if needed (never shrinks — same policy as before).
        if (mStagingBuffer == null || mStagingBuffer.capacity() < byteCount) {
            final int capacity = (floatCount + 12) * 4;
            mStagingBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            mStagingFloatBuffer = mStagingBuffer.asFloatBuffer();
        }

        // Fill the staging buffer with the new vertex data.
        mStagingFloatBuffer.position(0);
        mStagingFloatBuffer.put(ver.ary, ver.offset, floatCount);
        mStagingBuffer.position(0).limit(byteCount);

        // Create the VBO on first use (or after a context-loss reset).
        if (mVboId == 0) {
            final int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            mVboId = ids[0];
        }

        // Upload data to the GPU. Route through GLState to keep its internal cache in sync.
        pGLState.bindArrayBuffer(mVboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, byteCount, mStagingBuffer, GLES20.GL_DYNAMIC_DRAW);

        // Point the position attribute at offset 0 inside the currently bound VBO.
        GLES20.glVertexAttribPointer(
            ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
            2, GLES20.GL_FLOAT, false, 0, 0
        );
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, floatCount / 2);

        // Leave no VBO bound. Use pGLState so its cache stays in sync with the actual GL state,
        // preventing a false cache hit that would cause the next VBO bind to be skipped.
        pGLState.bindArrayBuffer(0);
    }

}
