package com.reco1l.framework.annotation;

import androidx.annotation.StringDef;

import static com.reco1l.framework.annotation.Algorithm.MD5;
import static com.reco1l.framework.annotation.Algorithm.SHA_256;

/**
 * Hash algorithm constants.
 */
@StringDef(value = {
        MD5,
        SHA_256
})
public @interface Algorithm
{
    String MD5 = "MD5";
    String SHA_256 = "SHA-256";
}
