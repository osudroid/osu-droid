package ru.nsu.ccfit.zuev.osu.helper;

import com.reco1l.osu.graphics.ExtendedSprite;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.StringSkinData;

public class AnimSprite extends ExtendedSprite {

    public enum LoopType {
        STOP, // stop at last frame
        LOOP, // loop from start
        DISAPPEAR, // disappear after last frame
        FROZE // do not automatically update frame
    }

    private final int count;
    private final TextureRegion[] regions;
    private int frame = 0;
    private float animTime = 0;
    private float fps;
    private LoopType loopType = LoopType.LOOP;

    public AnimSprite(float px, float py, StringSkinData prefix, String name, int count, float fps) {

        setPosition(px, py);
        setTextureRegion(ResourceManager.getInstance().getTextureWithPrefix(prefix, (name != null ? name : "") + (count == 1 ? "" : "0")));

        if (count == 0) {
            count = 1;
        }
        this.count = count;
        this.fps = fps;

        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTextureWithPrefix(prefix, (name != null ? name : "") + (count == 1 ? "" : i));
        }

        if (fps == 0) {
            loopType = LoopType.FROZE;
        }
    }

    public AnimSprite(final float px, final float py, final String texname, int count, final float fps) {

        setPosition(px, py);
        setTextureRegion(ResourceManager.getInstance().getTexture(texname + "0"));

        if (count == 0) {
            count = 1;
        }
        this.count = count;
        this.fps = fps;
        regions = new TextureRegion[count];
        for (int i = 0; i < count; i++) {
            regions[i] = ResourceManager.getInstance().getTexture(texname + i);
        }
        if (fps == 0) {
            loopType = LoopType.FROZE;
        }
    }

    public AnimSprite(final float px, final float py, final float fps, final String... textures) {

        setPosition(px, py);
        setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded(textures[0]));

        this.count = textures.length;
        this.fps = fps;
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
                setFrameInternal(frameByTime % count);
                break;
            case STOP:
                setFrameInternal(Math.min(frameByTime, count - 1));
                break;
            case DISAPPEAR:
                setFrameInternal(Math.min(frameByTime, count));
                break;
            default:
                break;
        }
    }

    /**
     * It's not recommended to call this method if you are not initialing this sprite
     */
    public void setFps(final float fps) {
        setFrameInternal(0);
        this.fps = fps;
    }


    /**
     * Force set animation to target frame.
     * @param frame target frame
     */
    public void setFrame(int frame) {
        if (this.loopType == LoopType.FROZE || fps == 0) {
            setFrameInternal(frame);
        } else {
            this.animTime = (frame + 0.0001f) / fps;
            updateFrame();
        }
    }

    private void setFrameInternal(int frame) {
        this.frame = frame;
        setTextureRegion(regions[frame]);
    }


    public int getFrame() {
        return frame;
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

}
