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
import com.reco1l.tables.AnimationTable;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.BlurRender;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.utils.helpers.BitmapHelper;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class Background extends BaseFragment {

    public static Background instance;

    private ImageView
            mImage0,
            mImage1;

    private AsyncTask mBitmapTask;

    private String mImagePath;
    private Bitmap mBitmap;

    private boolean
            mIsDark = false,
            mIsReload = false,
            mIsBlurEnabled = false;

    //--------------------------------------------------------------------------------------------//

    public Background() {
        super(Screens.Main, Screens.Selector, Screens.Loader, Screens.Summary);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "b";
    }

    @Override
    protected int getLayout() {
        return R.layout.background;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mImage0 = find("image0");
        mImage1 = find("image1");

        if (mBitmap == null) {
            mBitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
        }
        mImage0.setImageBitmap(mBitmap);
    }

    //--------------------------------------------------------------------------------------------//

    public void setBlur(boolean pEnabled) {
        if (pEnabled == mIsBlurEnabled) {
            return;
        }
        mIsBlurEnabled = pEnabled;
        mIsReload = true;
        changeFrom(mImagePath);
    }

    // Returns if the current background predominant color is dark
    public boolean isDark() {
        return mIsDark;
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized void changeFrom(String pPath) {
        if (!mIsReload) {
            if (pPath != null && pPath.equals(mImagePath)) {
                return;
            }
        }
        mIsReload = false;
        mImagePath = pPath;

        if (mBitmapTask != null) {
            mBitmapTask.cancel(true);
        }

        mBitmapTask = new AsyncTask() {
            Bitmap newBitmap;

            public void run() {
                int quality = Math.max(Config.getBackgroundQuality(), 1);

                if (mImagePath == null) {
                    newBitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
                } else {
                    newBitmap = BitmapFactory.decodeFile(mImagePath).copy(ARGB_8888, true);
                }
                newBitmap = BitmapHelper.compress(newBitmap, 100 / quality);
                parseColor(newBitmap);

                if (mIsBlurEnabled) {
                    newBitmap = BlurRender.applyTo(newBitmap, 25);
                }
            }

            public void onComplete() {
                handleChange(newBitmap);
                Game.resourcesManager.loadBackground(pPath);
            }
        };
        mBitmapTask.execute();
    }

    private void parseColor(Bitmap pBitmap) {
        Palette palette = Palette.from(pBitmap).generate();

        int color = palette.getDominantColor(Color.BLACK);
        mIsDark = Color.luminance(color) < 0.5;
    }

    private void handleChange(Bitmap pNewBitmap) {
        if (!isLoaded()) {
            return;
        }

        boolean cursor = mImage0.getVisibility() == View.VISIBLE;

        ImageView front = cursor ? mImage0 : mImage1;
        ImageView back = cursor ? mImage1 : mImage0;

        Game.activity.runOnUiThread(() -> {
            back.setImageBitmap(pNewBitmap);
            back.setVisibility(View.VISIBLE);
            back.setAlpha(1f);
        });

        AnimationTable.fadeOut(front)
                .runOnEnd(() -> {
                    front.setImageBitmap(null);
                    front.setVisibility(View.GONE);
                    front.setElevation(0f);

                    back.setElevation(1f);

                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    mBitmap = pNewBitmap;
                })
                .play(500);
    }

    public void reload() {
        if (isAdded() && mImagePath != null) {
            mIsReload = true;
            changeFrom(mImagePath);
        }
    }
}
