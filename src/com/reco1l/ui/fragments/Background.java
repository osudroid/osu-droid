package com.reco1l.ui.fragments;

// Created by Reco1l on 13/11/2022, 21:13

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import androidx.palette.graphics.Palette;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.BaseFragment;
import com.reco1l.data.tables.AnimationTable;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.utils.BlurRender;
import com.reco1l.utils.helpers.BitmapHelper;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class Background extends BaseFragment {

    public static Background instance;

    private ImageView image0, image1;

    private AsyncTask backgroundTask;

    private String imagePath;
    private Bitmap bitmap;

    private boolean
            isDark = false,
            isReload = false,
            isBlurEnabled = false;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "b";
    }

    @Override
    protected int getLayout() {
        return R.layout.background;
    }

    @Override
    protected Screens[] getParents() {
        return new Screens[]{Screens.Main, Screens.Selector, Screens.Loader, Screens.Summary};
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        image0 = find("image0");
        image1 = find("image1");

        if (bitmap == null) {
            bitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
        }
        image0.setImageBitmap(bitmap);
    }

    //--------------------------------------------------------------------------------------------//

    public void setBlur(boolean bool) {
        if (bool == isBlurEnabled) {
            return;
        }
        isBlurEnabled = bool;
        isReload = true;
        changeFrom(imagePath);
    }

    public boolean isDark() {
        return isDark;
    }

    //--------------------------------------------------------------------------------------------//

    public void changeFrom(String path) {
        if (!isReload) {
            if (path != null && path.equals(imagePath)) {
                return;
            }
        }
        isReload = false;
        imagePath = path;

        if (backgroundTask != null) {
            backgroundTask.cancel(true);
        }

        backgroundTask = new AsyncTask() {
            Bitmap newBitmap;

            public void run() {
                int quality = Math.max(Config.getBackgroundQuality(), 1);

                if (imagePath == null) {
                    newBitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
                } else {
                    newBitmap = BitmapFactory.decodeFile(imagePath);
                }
                newBitmap = BitmapHelper.compress(newBitmap, 100 / quality);
                parseColor(newBitmap);

                if (isBlurEnabled) {
                    newBitmap = BlurRender.applyTo(newBitmap, 25);
                }
            }

            public void onComplete() {
                handleChange(newBitmap);
                Game.resourcesManager.loadBackground(path);
            }
        };
        backgroundTask.execute();
    }

    private void parseColor(Bitmap bitmap) {
        Palette palette = Palette.from(bitmap).generate();

        int color = palette.getDominantColor(Color.BLACK);
        isDark = Color.luminance(color) < 0.5;
    }

    private void handleChange(Bitmap newBitmap) {

        boolean cursor = image0.getVisibility() == View.VISIBLE;

        ImageView front = cursor ? image0 : image1;
        ImageView back = cursor ? image1 : image0;

        Game.activity.runOnUiThread(() -> {
            back.setImageBitmap(newBitmap);
            back.setVisibility(View.VISIBLE);
            back.setAlpha(1f);
        });

        AnimationTable.fadeOut(front)
                .runOnEnd(() -> {
                    front.setImageBitmap(null);
                    front.setVisibility(View.GONE);
                    front.setElevation(0f);

                    back.setElevation(1f);

                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    bitmap = newBitmap;
                })
                .play(500);
    }

    public void reload() {
        if (isAdded() && imagePath != null) {
            isReload = true;
            changeFrom(imagePath);
        }
    }
}
