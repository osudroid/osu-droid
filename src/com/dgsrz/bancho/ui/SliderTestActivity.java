package com.dgsrz.bancho.ui;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
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
import org.anddev.andengine.ui.activity.BaseGameActivity;

/**
 * Based on native touch event
 * <p>
 * Created by dgsrz on 15/10/11.
 */
public class SliderTestActivity extends BaseGameActivity {

    private static final int INVALID_POINTER_ID = -1;

    private static final int CAMERA_WIDTH = 640;
    private static final int CAMERA_HEIGHT = 360;

    private BitmapTextureAtlas bitmapAtlas;
    private TextureRegion mCursorTextureRegion;

    @Override
    public Engine onLoadEngine() {
        Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        return new Engine(new EngineOptions(true,
                EngineOptions.ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT),
                camera));
    }

    @Override
    public void onLoadResources() {
        bitmapAtlas = new BitmapTextureAtlas(128, 128, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
        mCursorTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(bitmapAtlas, this, "cursor.png", 0, 0);
        getEngine().getTextureManager().loadTexture(bitmapAtlas);
    }

    @Override
    public Scene onLoadScene() {
        final Scene scene = new Scene();
        scene.setBackground(new ColorBackground(0.09804f, 0.6274f, 0.8784f));
        this.mEngine.registerUpdateHandler(new FPSLogger());

        Sprite mCursor = new Sprite(0, 0, this.mCursorTextureRegion);
        mCursor.setZIndex(1);
        scene.attachChild(mCursor);

        Sprite mCursor2 = new Sprite(16, 16, this.mCursorTextureRegion);
        mCursor2.setZIndex(0);
        scene.attachChild(mCursor2);

        scene.sortChildren();
        return scene;
    }

    @Override
    public void onLoadComplete() {

    }
}
