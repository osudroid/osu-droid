package com.edlplan.osu.support.slider;

import com.edlplan.framework.math.line.LinePath;

import org.anddev.andengine.entity.scene.Scene;

public abstract class AbstractSliderBody {

    protected LinePath path;

    public AbstractSliderBody(LinePath path) {
        this.path = path;
    }

    public void setSliderBodyBaseAlpha(float sliderBodyBaseAlpha) {
    }

    public abstract void onUpdate();

    public abstract void setBodyWidth(float width);

    public abstract void setBorderWidth(float width);

    public abstract void setBodyColor(float r, float g, float b);

    public abstract void setBorderColor(float r, float g, float b);

    public abstract void setStartLength(float length);

    public abstract void setEndLength(float length);

    public abstract void applyToScene(Scene scene, boolean emptyOnStart);

    public abstract void removeFromScene(Scene scene);

}
