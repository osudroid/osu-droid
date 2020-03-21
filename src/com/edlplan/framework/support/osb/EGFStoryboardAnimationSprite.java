package com.edlplan.framework.support.osb;

import com.edlplan.edlosbsupport.elements.StoryboardAnimationSprite;
import com.edlplan.framework.support.batch.object.MultipleFlippableTextureQuad;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;
import java.util.List;

public class EGFStoryboardAnimationSprite extends EGFStoryboardSprite {

    public EGFStoryboardAnimationSprite(OsbContext context) {
        super(context);
    }

    @Override
    public void update(double time) {
        super.update(time);
        StoryboardAnimationSprite sprite = (StoryboardAnimationSprite) this.sprite;
        int idx = (int) (Math.max(0, time - sprite.startTime()) / sprite.frameDelay);
        if (idx >= sprite.frameCount) {
            switch (sprite.loopType) {
                case LoopOnce:
                    idx = sprite.frameCount - 1;
                    break;
                case LoopForever:
                default:
                    idx %= sprite.frameCount;
            }
        }
        ((MultipleFlippableTextureQuad) textureQuad).switchTexture(idx);
    }

    @Override
    protected void onLoad() {
        StoryboardAnimationSprite sprite = (StoryboardAnimationSprite) this.sprite;
        MultipleFlippableTextureQuad textureQuad = new MultipleFlippableTextureQuad();

        List<TextureRegion> paths = new ArrayList<>(sprite.frameCount);
        for (int i = 0; i < sprite.frameCount; i++) {
            paths.add(context.texturePool.get(sprite.buildPath(i)));
        }
        textureQuad.initialWithTextureList(paths);
        textureQuad.switchTexture(0);
        textureQuad.position.x.value = sprite.startX;
        textureQuad.position.y.value = sprite.startY;
        textureQuad.anchor = sprite.origin.value;
        this.textureQuad = textureQuad;
    }

}
