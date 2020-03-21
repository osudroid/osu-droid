package com.dgsrz.bancho.ui;

import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;

public abstract class LayeredGameActivity extends BaseGameActivity {

    private FrameLayout mLayout;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private boolean mSurfaceHolderReady = false;

    @Override
    protected synchronized void onSetContentView() {
        this.mLayout = new FrameLayout(this);
        this.mLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER));

        this.mRenderSurfaceView = new RenderSurfaceView(this);
        this.mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // this.mRenderSurfaceView.setZOrderOnTop(true);
        this.mRenderSurfaceView.setRenderer(this.mEngine);
        this.mLayout.addView(mRenderSurfaceView, this.createSurfaceViewLayoutParams());

        this.mSurfaceHolderReady = false;
        this.mSurfaceView = new SurfaceView(this);
        this.mSurfaceHolder = mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolderReady = true;
                Debug.i("SurfaceHolder is ready.");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        mLayout.addView(mSurfaceView, this.createSurfaceViewLayoutParams());

        this.setContentView(mLayout);
    }

    public boolean isSurfaceHolderReady() {
        return mSurfaceHolderReady;
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }
}
