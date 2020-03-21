package com.dgsrz.bancho.ui;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

/**
 * Based on native touch event
 * <p>
 * Created by dgsrz on 15/10/11.
 */
public class TouchEventTestActivity extends LayeredGameActivity {

    private static final int INVALID_POINTER_ID = -1;

    private static final int CAMERA_WIDTH = 640;
    private static final int CAMERA_HEIGHT = 360;

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private float mVerticalScaleRatio = 0f;
    private float mHorizontalScaleRatio = 0f;

    private boolean isCursorVisible = false;
    private float cursorPosX = 0f;
    private float cursorPosY = 0f;

    private TextureRegion mCursorTextureRegion;
    private Sprite mCursor;

    private long lastCompleteTime = 0L;

    @Override
    public Engine onLoadEngine() {
        Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        return new Engine(new EngineOptions(true,
                EngineOptions.ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT),
                camera));
    }

    @Override
    public void onLoadResources() {
        BitmapTextureAtlas bitmapAtlas = new BitmapTextureAtlas(128, 128, TextureOptions.BILINEAR);
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        mCursorTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                bitmapAtlas, this, "cursor.png", 0, 0);
        getEngine().getTextureManager().loadTexture(bitmapAtlas);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mSurfaceWidth = dm.widthPixels;
        mSurfaceHeight = dm.heightPixels;

        Log.i("CREATE SCENE", "mSurfaceWidth/" + mSurfaceWidth);
        Log.i("CREATE SCENE", "mSurfaceHeight/" + mSurfaceHeight);

        mVerticalScaleRatio = 1.0f * CAMERA_HEIGHT / mSurfaceHeight;
        mHorizontalScaleRatio = 1.0f * CAMERA_WIDTH / mSurfaceWidth;

        if (isSurfaceHolderReady()) {
            MediaPlayer player = new MediaPlayer();
            try {
                AssetFileDescriptor fd = getAssets().openFd("game2.mp4");
                player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                player.setDisplay(getSurfaceHolder());
                player.prepare();
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        player.start();
                        player.setLooping(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Scene onLoadScene() {
        final Scene scene = new Scene();
        // scene.setBackground(new ColorBackground(0.09804f, 0.6274f, 0.8784f));
        scene.setBackground(new ColorBackground(0f, 0f, 0f, 0f));
        scene.registerUpdateHandler(new FPSLogger(1f));

        scene.registerUpdateHandler(new IUpdateHandler() {
            @Override
            public void onUpdate(float pSecondsElapsed) {
                if (isCursorVisible) {
                    Log.i("position", "position set to: x=" + cursorPosX + ", y=" + cursorPosY);
                    mCursor.setPosition(cursorPosX, cursorPosY);
                    mCursor.setVisible(true);
                } else {
                    mCursor.setVisible(false);
                }
            }

            @Override
            public void reset() {

            }
        });

        final int centerX = (CAMERA_WIDTH - this.mCursorTextureRegion.getWidth()) / 2;
        final int centerY = (CAMERA_HEIGHT - this.mCursorTextureRegion.getHeight()) / 2;

        mCursor = new Sprite(centerX, centerY, this.mCursorTextureRegion);
        mCursor.setVisible(false);
        scene.attachChild(mCursor);

        return scene;
    }

    @Override
    public void onLoadComplete() {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        // int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);
        float posX = MotionEventCompat.getX(event, pointerIndex);
        float posY = MotionEventCompat.getY(event, pointerIndex);

        // Normalize
        posX = posX * mHorizontalScaleRatio - 38;
        posY = posY * mVerticalScaleRatio - 38;

        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                //mCursor.setPosition(posX, posY);
                //mCursor.setVisible(true);
                cursorPosX = posX;
                cursorPosY = posY;
                isCursorVisible = true;
                break;

            case MotionEvent.ACTION_MOVE:
                // mCursor.setPosition(posX, posY);
                cursorPosX = posX;
                cursorPosY = posY;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                //if (< 500)
                // mCursor.setVisible(false);
                isCursorVisible = false;
                break;
        }

        long currTimestamp = System.currentTimeMillis();
        long elapsed = currTimestamp - lastCompleteTime;
        String name = Thread.currentThread().getName();
        System.out.println(name + " update in : " + elapsed + " ms");
        lastCompleteTime = currTimestamp;
        return true;
    }
}
