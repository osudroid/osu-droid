package com.reco1l.utils;

// Created by Reco1l on 16/11/2022, 04:54

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.reco1l.Game;

public class BlurEffect {

    public static Bitmap applyTo(Bitmap bitmap, int radius) {
        if (bitmap == null) {
            return null;
        }

        RenderScript renderScript = RenderScript.create(Game.mActivity);

        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);

        Allocation input = Allocation.createFromBitmap(renderScript, bitmap);
        Allocation output = Allocation.createFromBitmap(renderScript, newBitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));

        blur.setInput(input);
        blur.setRadius(radius);
        blur.forEach(output);

        output.copyTo(newBitmap);
        renderScript.destroy();

        return newBitmap;
    }

}
