package com.edlplan.andengine;

import com.edlplan.framework.utils.FloatArraySlice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class TriangleRenderer {

    private static TriangleRenderer triangleRenderer = new TriangleRenderer();
    FloatBuffer buffer;

    public static TriangleRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(FloatArraySlice ver, GL10 pGL) {
        int offset = ver.length;
        if (buffer == null || buffer.capacity() < offset) {
            ByteBuffer bb = ByteBuffer.allocateDirect((offset + 12) * 4);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        }
        buffer.position(0).limit(buffer.capacity());
        buffer.put(ver.ary, ver.offset, ver.length);
        buffer.position(0).limit(offset);

        pGL.glVertexPointer(2, GL10.GL_FLOAT, 0, buffer);
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.length / 2);
    }

}
