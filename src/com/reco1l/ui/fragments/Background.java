package com.reco1l.ui.fragments;

// Created by Reco1l on 13/11/2022, 21:13

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.reco1l.global.Game;
import com.reco1l.global.Scenes;
import com.reco1l.scenes.BaseScene;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.BlurRender;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.view.FadeImageView;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class Background extends BaseFragment {

    public static final Background instance = new Background();

    private Bitmap mBitmap;
    private String mImagePath;
    private FadeImageView mImage;
    private AsyncTask mBitmapTask;

    private final Queue<Runnable> mCallbackQueue;

    private boolean
            mIsReload = false,
            mIsBlurEnabled = false;

    //--------------------------------------------------------------------------------------------//

    public Background() {
        super(Scenes.main, Scenes.selector, Scenes.loader, Scenes.summary, Scenes.listing);
        mCallbackQueue = new LinkedList<>();
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
        mImage = find("image");

        if (mBitmap == null) {
            mBitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
        }
        mImage.setImageBitmap(mBitmap);
    }

    @Override
    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
        setBlur(newScene != Scenes.main);
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

    public void postChange(Runnable task) {
        mCallbackQueue.add(task);
    }

    //--------------------------------------------------------------------------------------------//

    public Bitmap getBitmap() {
        return mBitmap;
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized void changeFrom(String path) {
        if (Config.isSafeBeatmapBg()) {
            path = null;
        }

        if (!mIsReload && Objects.equals(path, mImagePath)) {
            return;
        }
        mImagePath = path;
        mIsReload = false;

        if (mBitmapTask != null) {
            mBitmapTask.cancel(true);
        }

        mBitmapTask = new AsyncTask() {

            private Bitmap mNewBitmap;

            public void run() {
                Game.resourcesManager.loadBackground(mImagePath);

                if (mImagePath == null) {
                    mNewBitmap = Game.bitmapManager.get("menu-background").copy(ARGB_8888, true);
                } else {
                    mNewBitmap = BitmapFactory.decodeFile(mImagePath).copy(ARGB_8888, true);
                }

                if (mIsBlurEnabled) {
                    mNewBitmap = BlurRender.applyTo(mNewBitmap, 25);
                }
            }

            public void onComplete() {
                mBitmap = mNewBitmap;
                Game.activity.runOnUiThread(() -> mImage.setImageBitmap(mBitmap));

                while (!mCallbackQueue.isEmpty()) {
                    Runnable callback = mCallbackQueue.poll();

                    if (callback != null) {
                        callback.run();
                    }
                }
            }
        };
        mBitmapTask.execute();
    }
}
