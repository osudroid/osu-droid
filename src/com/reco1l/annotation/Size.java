package com.reco1l.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef(value = {Size.XXS, Size.XS, Size.S, Size.M, Size.L, Size.XL, Size.XXL})
@Retention(RetentionPolicy.SOURCE)
public @interface Size {
    int XXS = -2;
    int XS = -1;
    int S = 0;
    int M = 1;
    int L = 2;
    int XL = 3;
    int XXL = 4;
}
