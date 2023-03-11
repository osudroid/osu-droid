package com.reco1l.framework.drawing;

// Created by Reco1l on 16/11/2022, 04:54

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.reco1l.Game;

// TODO [BlurRender] RenderScript is deprecated in API 31, not a big deal for now
//  but if you want to migrate it go ahead.
public final class BlurRender {

    public static Bitmap applyTo(Bitmap bitmap, float radius) {
        if (bitmap == null) {
            return null;
        }
        bitmap = bitmap.copy(Config.ARGB_8888, true);

        RenderScript renderScript = RenderScript.create(Game.activity);

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
