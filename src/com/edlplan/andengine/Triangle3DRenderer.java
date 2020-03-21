package com.edlplan.andengine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Triangle3DRenderer {


    private static Triangle3DRenderer triangleRenderer = new Triangle3DRenderer();
    FloatBuffer buffer;

    public static Triangle3DRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(float[] ver, GL10 pGL) {
        int offset = ver.length;
        if (buffer == null || buffer.capacity() < offset) {
            ByteBuffer bb = ByteBuffer.allocateDirect(Math.max(offset + 18, 900) * 4);
            bb.order(ByteOrder.nativeOrder());
            buffer = bb.asFloatBuffer();
        }
        buffer.position(0).limit(buffer.capacity());
        buffer.put(ver, 0, ver.length);
        buffer.position(0).limit(offset);

        pGL.glVertexPointer(3, GL10.GL_FLOAT, 0, buffer);
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.length / 3);
    }

}
