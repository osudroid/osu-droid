package com.edlplan.framework.support.graphics;


import android.opengl.GLES10;

import com.edlplan.framework.support.batch.BatchEngine;
import com.edlplan.framework.utils.interfaces.Copyable;

public class BlendProperty implements Copyable {

    public boolean enable = true;

    public boolean isPreM = false;

    public BlendType blendType = BlendType.Normal;

    public BlendProperty() {

    }

    public BlendProperty(BlendProperty b) {
        set(b);
    }

    public BlendProperty(boolean e, boolean isPreM, BlendType t) {
        this.isPreM = isPreM;
        this.enable = e;
        this.blendType = t;
    }

    public void set(BlendProperty b) {
        this.enable = b.enable;
        this.blendType = b.blendType;
        this.isPreM = b.isPreM;
    }

    public void applyToGL() {
        BatchEngine.flush();
        if (enable) {
            GLES10.glEnable(GLES10.GL_BLEND);
            if (isPreM) {
                GLES10.glBlendFunc(blendType.srcTypePreM, blendType.dstTypePreM);
            } else {
                GLES10.glBlendFunc(blendType.srcType, blendType.dstType);
            }
        } else {
            GLES10.glDisable(GLES10.GL_BLEND);
        }
    }

    public boolean equals(boolean _enable, boolean isPreM, BlendType _blendType) {
        return this.enable == _enable && this.blendType == _blendType && this.isPreM == isPreM;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlendProperty) {
            BlendProperty b = (BlendProperty) obj;
            return (enable == b.enable) && (blendType == b.blendType) && (isPreM == b.isPreM);
        } else return false;
    }

    @Override
    public Copyable copy() {
        return new BlendProperty(this);
    }
}
