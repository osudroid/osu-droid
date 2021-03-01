package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.modifier.IModifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;

public class GameEffect extends GameObject implements IEntityModifierListener {
    private static final HashSet<String> animationEffects = new HashSet<>(Arrays.asList(
            "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
    ));

    Sprite hit;
    String texname;

    public GameEffect(final String texname) {
        this.texname = texname;

        if (isAnimationEffect(texname) && ResourceManager.getInstance().isTextureLoaded(texname + "-0")) {
            List<String> loadedScoreBarTextures = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (ResourceManager.getInstance().isTextureLoaded(texname + "-" + i))
                    loadedScoreBarTextures.add(texname + "-" + i);
                else break;
            }
            AnimSprite hit = new AnimSprite(0, 0, 60, loadedScoreBarTextures.toArray(new String[0]));
            hit.setLoopType(AnimSprite.LoopType.STOP);
            this.hit = hit;
        } else {
            hit = new Sprite(0, 0, ResourceManager.getInstance().getTexture(texname));
        }
    }

    private static boolean isAnimationEffect(String textureName) {
        return animationEffects.contains(textureName);
    }

    public void setColor(final RGBColor color) {
        hit.setColor(color.r(), color.g(), color.b());
    }

    public void init(final Scene scene, final PointF pos, final float scale,
                     final IEntityModifier... entityModifiers) {
        if (hit instanceof AnimSprite) {
            ((AnimSprite) hit).setAnimTime(0);
        }
        hit.setPosition(pos.x - hit.getTextureRegion().getWidth() / 2f, pos.y
                - hit.getTextureRegion().getHeight() / 2f);
        hit.registerEntityModifier(new ParallelEntityModifier(this,
                entityModifiers));
        hit.setScale(scale);
        hit.setAlpha(1);
        hit.detachSelf();
        hit.setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT);
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

    @Override
    public void onModifierStarted(final IModifier<IEntity> pModifier, final IEntity pItem) {

    }

    @Override
    public void onModifierFinished(final IModifier<IEntity> pModifier, final IEntity pItem) {
        SyncTaskManager.getInstance().run(() -> {
            hit.detachSelf();
            hit.clearEntityModifiers();
            GameObjectPool.getInstance().putEffect(GameEffect.this);
        });
    }
}
