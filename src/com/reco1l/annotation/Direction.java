package com.reco1l.annotation;

import androidx.annotation.IntDef;

@IntDef(value = {
        Direction.TOP_TO_BOTTOM,
        Direction.BOTTOM_TO_TOP,
        Direction.LEFT_TO_RIGHT,
        Direction.RIGHT_TO_LEFT
})
public @interface Direction {
    int TOP_TO_BOTTOM = 0;
    int BOTTOM_TO_TOP = 1;
    int LEFT_TO_RIGHT = 2;
    int RIGHT_TO_LEFT = 3;
}
