package com.edlplan.ui.design;

import com.edlplan.framework.math.Color4;

public interface IColor {

    Color4 getColor();

    default boolean isContextNeeded() {
        return false;
    }

}
