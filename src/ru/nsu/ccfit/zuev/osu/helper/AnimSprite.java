package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class AnimSprite extends Sprite {
    private final int count;
    private final TextureRegion[] regions;
    private float frame;
    private float fps;

    public AnimSprite(final float px, final float py, final String texname,
                      int count, final float fps) {
        super(px, py, ResourceManager.getInstance().getTexture(texname + "0"));
        if (count == 0) {
            count = 1;
        }
        this.count = count;
        this.fps = fps;
        this.frame = 0;
        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTexture(texname + i);
        }
    }

    public AnimSprite(final float px, final float py, final float fps,
                      final String... textures) {
        super(px, py, ResourceManager.getInstance().getTextureIfLoaded(textures[0]));
        this.count = textures.length;
        this.fps = fps;
        frame = 0;
        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTextureIfLoaded(textures[i]);
        }
    }


    @Override
    protected void onManagedUpdate(final float pSecondsElapsed) {
        if (count <= 1) {
            frame = 0;
        } else {
            frame += fps * pSecondsElapsed;
            frame %= count;
        }
        super.onManagedUpdate(pSecondsElapsed);
    }


    @Override
    protected void doDraw(final GL10 pGL, final Camera pCamera) {
        if ((int) frame < regions.length && frame >= 0) {
            regions[(int) frame].onApply(pGL);
        } else if (regions.length > 0) {
            regions[0].onApply(pGL);
        } else {
            return;
        }
        onInitDraw(pGL);
        onApplyVertices(pGL);
        drawVertices(pGL, pCamera);
    }


    @Override
    public void setFlippedHorizontal(final boolean pFlippedHorizontal) {
        for (final TextureRegion reg : regions) {
            reg.setFlippedHorizontal(pFlippedHorizontal);
        }
    }

    public void setFps(final float fps) {
        frame = 0;
        this.fps = fps;
    }

    public void setFrame(final float frame) {
        this.frame = frame % count;
    }

    public float getFrameWidth() {
        if ((int) frame < regions.length && frame >= 0) {
            return regions[(int) frame].getWidth();
        } else if (regions.length > 0) {
            return regions[0].getWidth();
        } else {
            return 40;
        }
    }

    public void setTextureRegion(final int index, final TextureRegion region) {
        regions[index] = region;
    }

    public TextureRegion getTextureRegionAt(final int index) {
        return regions[index];
    }

    public int getTextureRegionCount() {
        return regions.length;
    }
}
