package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class AnimSprite extends Sprite {

    public enum LoopType {
        STOP, // stop at last frame
        LOOP, // loop from start
        DISAPPEAR, // disappear after last frame
        FROZE // do not automatically update frame
    }

    private final int count;
    private final TextureRegion[] regions;
    private int frame;
    private float animTime;
    private float fps;
    private LoopType loopType = LoopType.LOOP;

    public AnimSprite(final float px, final float py, final String texname,
                      int count, final float fps) {
        super(px, py, ResourceManager.getInstance().getTexture(texname + "0"));
        if (count == 0) {
            count = 1;
        }
        this.count = count;
        this.fps = fps;
        this.frame = 0;
        this.animTime = 0;
        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTexture(texname + i);
        }
        if (fps == 0) {
            loopType = LoopType.FROZE;
        }
    }

    public AnimSprite(final float px, final float py, final float fps,
                      final String... textures) {
        super(px, py, ResourceManager.getInstance().getTextureIfLoaded(textures[0]));
        this.count = textures.length;
        this.fps = fps;
        this.frame = 0;
        this.animTime = 0;
        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTextureIfLoaded(textures[i]);
        }
        if (fps == 0) {
            loopType = LoopType.FROZE;
        }
    }

    public void setLoopType(LoopType loopType) {
        this.loopType = loopType;
    }

    public LoopType getLoopType() {
        return loopType;
    }


    /**
     * Automatically update frame.
     * If loopType is {@link LoopType#FROZE} or fps is 0, this will do nothing
     */
    private void updateFrame() {
        if (loopType == LoopType.FROZE || fps == 0) {
            return;
        }
        int frameByTime = (int) (this.animTime * fps);
        switch (loopType) {
            case LOOP:
                frame = frameByTime % count;
                break;
            case STOP:
                frame = Math.min(frameByTime, count - 1);
                break;
            case DISAPPEAR:
                frame = Math.min(frameByTime, count);
                break;
            default:
                break;
        }
    }

    /**
     * It's not recommended to call this method if you are not initialing this sprite
     */
    public void setFps(final float fps) {
        frame = 0;
        this.fps = fps;
    }


    /**
     * Force set animation to target frame.
     * @param frame target frame
     */
    public void setFrame(int frame) {
        if (this.loopType == LoopType.FROZE || fps == 0) {
            this.frame = frame;
        } else {
            this.animTime = (frame + 0.0001f) / fps;
            updateFrame();
        }
    }

    public void setAnimTime(float animTime) {
        this.animTime = animTime;
        updateFrame();
    }

    @Override
    protected void onManagedUpdate(final float pSecondsElapsed) {
        this.animTime += pSecondsElapsed;
        updateFrame();
        super.onManagedUpdate(pSecondsElapsed);
    }


    @Override
    protected void doDraw(final GL10 pGL, final Camera pCamera) {
        if (regions.length == 0 || frame < 0 || frame >= regions.length) {
            return;
        }
        regions[frame].onApply(pGL);
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

    public float getFrameWidth() {
        if (frame < regions.length && frame >= 0) {
            return regions[frame].getWidth();
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
