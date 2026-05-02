package com.edlplan.andengine;

import android.opengl.GLES20;

import com.edlplan.framework.utils.FloatArraySlice;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TriangleRenderer {

    private static TriangleRenderer triangleRenderer = new TriangleRenderer();
    FloatBuffer buffer;

    public static TriangleRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(FloatArraySlice ver) {
        int offset = ver.length;
        if (buffer == null || buffer.capacity() < offset) {
            ByteBuffer bb = ByteBuffer.allocateDirect((offset + 12) * 4);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        }
        buffer.position(0).limit(buffer.capacity());
        buffer.put(ver.ary, ver.offset, ver.length);
        buffer.position(0).limit(offset);

        GLES20.glVertexAttribPointer(
            ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
            2, GLES20.GL_FLOAT, false, 0, buffer
        );
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, ver.length / 2);
    }

}
