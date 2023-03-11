package com.reco1l.ui.fragments;

// Created by Reco1l on 13/11/2022, 21:13

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.management.Settings;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;
import com.reco1l.framework.drawing.BlurRender;
import com.reco1l.framework.execution.Async;
import com.reco1l.view.FadeImageView;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import main.osu.Config;

import com.rimu.R;

public final class Background extends BaseFragment {

    public static final Background instance = new Background();

    private Bitmap
            mBitmap,
            mRawBitmap,
            mDefaultBitmap;

    private View mLayer;
    private FadeImageView mImage;

    private String mImagePath;
    private Async mBitmapTask;

    private final Queue<Runnable> mCallbackQueue;

    private boolean
            mIsReload = false,
            mIsBlurEnabled = false,
            mIsCurrentChange = false;

    //--------------------------------------------------------------------------------------------//

    public Background() {
        super(Scenes.all());
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

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Background;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mImage = find("image");
        mLayer = find("dim");

        mDefaultBitmap = Game.bitmapManager.get("menu-background");

        if (mBitmap == null) {
            mBitmap = mDefaultBitmap;
        }
        mImage.setImageBitmap(mBitmap);
    }

    @Override
    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
        setBlur(newScene != Scenes.main);
        setDim(newScene != Scenes.main ? 50 : 0);
    }

    //--------------------------------------------------------------------------------------------//

    public void setDim(@IntRange(from = 0, to = 100) int value) {
        if (isLoaded()) {
            Animation.of(mLayer)
                     .toAlpha(value / 100f)
                     .play(300);
        }
    }

    public void setBlur(boolean pEnabled) {
        if (pEnabled == mIsBlurEnabled) {
            return;
        }
        mIsBlurEnabled = pEnabled;
        mIsReload = true;
        changeFrom(mImagePath);
    }

    public void postChange(@NonNull Runnable task) {
        if (!mIsCurrentChange) {
            task.run();
            return;
        }
        mCallbackQueue.add(task);
    }

    //--------------------------------------------------------------------------------------------//

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Bitmap getRawBitmap() {
        return mRawBitmap;
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized void changeFrom(String path) {
        mIsCurrentChange = true;

        if (Config.isSafeBeatmapBg()) {
            path = null;
        }

        if (!mIsReload && Objects.equals(path, mImagePath)) {
            return;
        }
        mImagePath = path;
        mIsReload = false;

        if (mBitmapTask != null && !mBitmapTask.getExecutor().isShutdown()) {
            mBitmapTask.getExecutor().shutdownNow();
        }

        mBitmapTask = Async.run(() -> {

            Bitmap newBitmap;

            if (mImagePath == null) {
                newBitmap = mDefaultBitmap;
            }
            else {
                newBitmap = BitmapFactory.decodeFile(mImagePath);
            }

            mRawBitmap = newBitmap;
            mBitmap = applyBlurEffect(mRawBitmap);

            Game.resourcesManager.loadBackground(mBitmap);
            Game.activity.runOnUiThread(() -> mImage.setImageBitmap(mBitmap));

            notifyCallbacks();
            mIsCurrentChange = false;
        });
    }

    private void notifyCallbacks() {
        while (!mCallbackQueue.isEmpty()) {
            Runnable callback = mCallbackQueue.poll();

            if (callback != null) {
                Game.activity.runOnUiThread(callback);
            }
        }
    }

    private Bitmap applyBlurEffect(Bitmap bitmap) {
        if (!mIsBlurEnabled) {
            return bitmap;
        }

        int value = Settings.<Integer>get("bgBlur", 100);

        if (value > 0) {
            float radius = 25 * (value / 100f);

            bitmap = BlurRender.applyTo(bitmap, radius);
        }

        return bitmap;
    }
}
