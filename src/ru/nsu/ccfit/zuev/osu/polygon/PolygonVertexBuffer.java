package ru.nsu.ccfit.zuev.osu.polygon;

import org.anddev.andengine.opengl.util.FastFloatBuffer;
import org.anddev.andengine.opengl.vertex.VertexBuffer;

public class PolygonVertexBuffer extends VertexBuffer {

    final static int BYTES_PER_FLOAT = Float.SIZE / 8;

    public PolygonVertexBuffer(final int pVerticesCount, final int pDrawType, final boolean managed) {
        super(2 * pVerticesCount * BYTES_PER_FLOAT, pDrawType, managed);
    }

}
