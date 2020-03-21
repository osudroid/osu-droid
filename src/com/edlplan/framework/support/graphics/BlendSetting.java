package com.edlplan.framework.support.graphics;

import com.edlplan.framework.utils.AbstractSRable;

public class BlendSetting extends AbstractSRable<BlendProperty> {

    public BlendSetting() {

    }

    public BlendSetting setUp() {
        initial();
        apply(getData());
        return this;
    }

    private void apply(BlendProperty p) {
        p.applyToGL();
    }

    public void apply() {
        apply(getData());
    }

    public boolean isEnable() {
        return getData().enable;
    }

    public void setEnable(boolean enable) {
        set(enable, isPreM(), getBlendType());
    }

    public BlendType getBlendType() {
        return getData().blendType;
    }

    public void setBlendType(BlendType type) {
        set(isEnable(), isPreM(), type);
    }

    public boolean isPreM() {
        return getData().isPreM;
    }

    public void set(boolean enable, boolean isPreM, BlendType blendType) {
        if (!getData().equals(enable, isPreM, blendType)) {
            BlendProperty prop = new BlendProperty(enable, isPreM, blendType);
            setCurrentData(prop);
            apply(prop);
        }
    }

    public void setIsPreM(boolean isPreM) {
        if (isPreM != isPreM()) {
            getData().isPreM = false;
            apply();
        }
    }

    @Override
    public void onSave(BlendProperty t) {

    }

    @Override
    public void onRestore(BlendProperty now, BlendProperty pre) {
        if (!now.equals(pre)) {
            apply(now);
        }
    }

    @Override
    public BlendProperty getDefData() {
        return new BlendProperty();
    }
}
