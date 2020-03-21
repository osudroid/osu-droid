package com.edlplan.andengine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class TriangleTexture3DRenderer {

    private static TriangleTexture3DRenderer triangleRenderer = new TriangleTexture3DRenderer();
    FloatBuffer buffer, coordBuffer;

    public static TriangleTexture3DRenderer get() {
        return triangleRenderer;
    }

    public synchronized void renderTriangles(float[] ver, float[] coord, GL10 pGL) {
        {
            int offset = ver.length;
            if (buffer == null || buffer.capacity() < offset) {
                ByteBuffer bb = ByteBuffer.allocateDirect((offset + 18) * 4);
                bb.order(ByteOrder.nativeOrder());
                buffer = bb.asFloatBuffer();
            }
            buffer.position(0).limit(buffer.capacity());
            buffer.put(ver, 0, ver.length);
            buffer.position(0).limit(offset);
        }
        {
            int offset = coord.length;
            if (coordBuffer == null || coordBuffer.capacity() < offset) {
                ByteBuffer bb = ByteBuffer.allocateDirect((offset + 12) * 4);
                bb.order(ByteOrder.nativeOrder());
                coordBuffer = bb.asFloatBuffer();
            }
            coordBuffer.position(0).limit(coordBuffer.capacity());
            coordBuffer.put(coord, 0, coord.length);
            coordBuffer.position(0).limit(offset);
        }

        pGL.glTexCoordPointer(2, GL10.GL_FLOAT, 0, coordBuffer);
        pGL.glVertexPointer(3, GL10.GL_FLOAT, 0, buffer);
        pGL.glDrawArrays(GL10.GL_TRIANGLES, 0, ver.length / 3);
    }

}
