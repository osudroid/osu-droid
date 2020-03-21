package com.edlplan.framework.support.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class BufferUtil {

    public static FloatBuffer createFloatBuffer(int floatCount) {
        ByteBuffer bb = ByteBuffer.allocateDirect(floatCount * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asFloatBuffer();
    }

    public static ShortBuffer createShortBuffer(int shortCount) {
        ByteBuffer bb = ByteBuffer.allocateDirect(shortCount * 2);
        bb.order(ByteOrder.nativeOrder());
        return bb.asShortBuffer();
    }

    public static IntBuffer createIntBuffer(int intCount) {
        ByteBuffer bb = ByteBuffer.allocateDirect(intCount * 4);
        bb.order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    }

    public static class ReusedFloatBuffer {

        private FloatBuffer buffer;

        public FloatBuffer load(float[] ary) {
            if (buffer == null || buffer.capacity() < ary.length) {
                buffer = createFloatBuffer(ary.length * 3 / 2 + 20);
            }
            buffer.position(0).limit(ary.length);
            buffer.put(ary);
            buffer.position(0);
            return buffer;
        }

        public FloatBuffer getBuffer() {
            return buffer;
        }

    }

    public static class ReusedShortBuffer {

        private ShortBuffer buffer;

        public ShortBuffer load(short[] ary) {
            if (buffer == null || buffer.capacity() < ary.length) {
                buffer = createShortBuffer(ary.length * 3 / 2 + 20);
            }
            buffer.position(0).limit(ary.length);
            buffer.put(ary);
            buffer.position(0);
            return buffer;
        }

        public ShortBuffer getBuffer() {
            return buffer;
        }

    }

}
