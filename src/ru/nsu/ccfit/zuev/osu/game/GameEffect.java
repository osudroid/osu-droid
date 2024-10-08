package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;


import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.modifier.UniversalModifier;
import com.reco1l.osu.Execution;
import com.reco1l.andengine.sprite.AnimatedSprite;
import com.reco1l.andengine.sprite.ExtendedSprite;

import org.anddev.andengine.entity.scene.Scene;

import java.util.Arrays;
import java.util.HashSet;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameEffect extends GameObject {
    private static final HashSet<String> animationEffects = new HashSet<>(Arrays.asList(
            "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
    ));

    ExtendedSprite hit;
    String texname;

    public GameEffect(final String texname) {
        this.texname = texname;

        if (isAnimationEffect(texname) && ResourceManager.getInstance().isTextureLoaded(texname + "-0")) {
            var hit = new AnimatedSprite(texname, true, OsuSkin.get().getAnimationFramerate());
            hit.setLoop(false);
            this.hit = hit;
        } else {
            hit = new ExtendedSprite();
            hit.setTextureRegion(ResourceManager.getInstance().getTexture(texname));
        }
    }

    private static boolean isAnimationEffect(String textureName) {
        return animationEffects.contains(textureName);
    }

    public void setColor(final RGBColor color) {
        if (color == null) {
            hit.setColor(1f, 1f, 1f);
        } else {
            hit.setColor(color.r(), color.g(), color.b());
        }
    }

    public void init(Scene scene, PointF position, float initialScale, float initialAlpha, UniversalModifier modifier) {
        if (hit instanceof AnimatedSprite animatedHit) {
            animatedHit.setElapsedSec(0f);
        }
        hit.setPosition(position.x, position.y);
        hit.setOrigin(Anchor.Center);
        hit.setScale(initialScale);
        hit.setAlpha(initialAlpha);
        hit.setRotation(0f);
        hit.detachSelf();

        modifier.then(e -> Execution.updateThread(() -> {
            hit.detachSelf();
            hit.clearEntityModifiers();
            GameObjectPool.getInstance().putEffect(GameEffect.this);
        }));

        scene.attachChild(hit);
    }

    public void setBlendFunction(int sourceBlend, int destBlend) {
        hit.setBlendFunction(sourceBlend, destBlend);
    }

    public String getTexname() {
        return texname;
    }

    @Override
    public void update(final float dt) {
    }

}
