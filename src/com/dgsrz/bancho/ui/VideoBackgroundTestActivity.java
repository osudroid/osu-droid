package com.dgsrz.bancho.ui;

import android.os.Environment;
import android.util.Log;

import com.dgsrz.bancho.game.sprite.VideoSprite;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import java.io.IOException;

/**
 * Created by dgsrz on 15/10/7.
 */
public class VideoBackgroundTestActivity extends BaseGameActivity {

    private static final int CAMERA_WIDTH = 640;
    private static final int CAMERA_HEIGHT = 360;

    private VideoSprite mVideo;

    private TextureRegion mCursorTextureRegion;
    private Sprite mCursor;

    private TextureRegion mMenuBackTextureRegion;
    private Sprite mMenuBack;

    public Engine onLoadEngine() {
        Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        return new Engine(new EngineOptions(true,
                EngineOptions.ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT),
                camera));
    }

    public void onLoadResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        BitmapTextureAtlas cursorAtlas = new BitmapTextureAtlas(128, 128, TextureOptions.BILINEAR);
        mCursorTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                cursorAtlas, this, "cursor.png", 0, 0);
        getEngine().getTextureManager().loadTexture(cursorAtlas);

        BitmapTextureAtlas menuBackAtlas = new BitmapTextureAtlas(128, 128, TextureOptions.BILINEAR);
        mMenuBackTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                menuBackAtlas, this, "menu-back.png", 0, 0);
        getEngine().getTextureManager().loadTexture(menuBackAtlas);

        try {
            mVideo = new VideoSprite(0, 0, 640, 360);
            mVideo.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/butterfly.avi");
        } catch (IOException e) {
            Log.i("Load avi", e.getMessage());
        }
    }

    public Scene onLoadScene() {
        final Scene scene = new Scene();
        scene.setBackground(new ColorBackground(0f, 0f, 0f));

        // missing texCoordinatePointer
        scene.attachChild(mVideo);

        mCursor = new Sprite(CAMERA_WIDTH - this.mCursorTextureRegion.getWidth(), CAMERA_HEIGHT - this.mCursorTextureRegion.getHeight(), this.mCursorTextureRegion);
        scene.attachChild(mCursor);

        mMenuBack = new Sprite(0, CAMERA_HEIGHT - this.mMenuBackTextureRegion.getHeight(), this.mMenuBackTextureRegion);
        // mMenuBack.setColor(1, 0, 0);
        scene.attachChild(mMenuBack);

        // scene.sortChildren();

        return scene;
    }

    public void onLoadComplete() {
        mVideo.play();
    }
}
