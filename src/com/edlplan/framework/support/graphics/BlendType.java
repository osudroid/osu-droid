package com.edlplan.framework.support.graphics;

import android.opengl.GLES20;

public enum BlendType {
    Normal(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
    ),
    Additive(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE,
            GLES20.GL_ONE,
            GLES20.GL_ONE
    ),
    Delete(
            GLES20.GL_ZERO,
            GLES20.GL_ONE_MINUS_SRC_COLOR,
            GLES20.GL_ZERO,
            GLES20.GL_ONE_MINUS_SRC_COLOR
    ),
    Delete_Alpha(
            GLES20.GL_ZERO,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ZERO,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
    ),
    DeleteRepeat(
            GLES20.GL_ONE_MINUS_DST_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_DST_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
    );
    public final int srcType;
    public final int dstType;

    public final int srcTypePreM;
    public final int dstTypePreM;

    //public final boolean needPreMultiple;
    BlendType(int src, int dst, int srcTypePreM, int dstTypePreM) {
        srcType = src;
        dstType = dst;
        this.srcTypePreM = srcTypePreM;
        this.dstTypePreM = dstTypePreM;
        //needPreMultiple=prm;
    }
}
