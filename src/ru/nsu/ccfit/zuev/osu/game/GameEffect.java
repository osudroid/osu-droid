package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.modifier.IModifier;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;

public class GameEffect extends GameObject implements IEntityModifierListener {
    Sprite hit;
    String texname;
    private boolean anim;
    private int fcount;

    public GameEffect(final String texname) {
        this.texname = texname;
        if (texname.startsWith("hit0") || texname.startsWith("hit50") || texname.startsWith("hit100") || texname.startsWith("hit300")){
            if (ResourceManager.getInstance().isTextureLoaded(texname + "-0")) {
                List<String> loadedScoreBarTextures = new ArrayList<>();
                for (int i = 0; i < 60; i++) {
                    if (ResourceManager.getInstance().isTextureLoaded(texname + "-" + i))
                        loadedScoreBarTextures.add(texname + "-" + i);
                    else break;
                }
                hit = new AnimSprite(0, 0, 60,
                        loadedScoreBarTextures.toArray(new String[loadedScoreBarTextures.size()]));
                anim = true;
                fcount = loadedScoreBarTextures.size();
            }
            else{
                hit = new Sprite(0, 0, ResourceManager.getInstance()
                .getTexture(texname));
                anim = false;
            }
        }
        else{
            hit = new Sprite(0, 0, ResourceManager.getInstance()
            .getTexture(texname));
            anim = false;
        }
    }

    public void setColor(final RGBColor color) {
        hit.setColor(color.r(), color.g(), color.b());
    }

    public void init(final Scene scene, final PointF pos, final float scale,
                     final IEntityModifier... entityModifiers) {
        hit.setPosition(pos.x - hit.getTextureRegion().getWidth() / 2, pos.y
                - hit.getTextureRegion().getHeight() / 2);
        if (anim){
            hit.registerEntityModifier(new ParallelEntityModifier(this,
                    new SequenceEntityModifier(
                    ModifierFactory.newDelayModifier(0.016f * fcount),
                    ModifierFactory.newAlphaModifier(0f,1f,0f),
                    ModifierFactory.newFadeOutModifier(0f))));
        }
        else hit.registerEntityModifier(new ParallelEntityModifier(this, entityModifiers));
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


    public void onModifierStarted(final IModifier<IEntity> pModifier,
                                  final IEntity pItem) {

    }


    public void onModifierFinished(final IModifier<IEntity> pModifier,
                                   final IEntity pItem) {
        SyncTaskManager.getInstance().run(new Runnable() {


            public void run() {
                hit.detachSelf();
                hit.clearEntityModifiers();
                GameObjectPool.getInstance().putEffect(GameEffect.this);
            }
        });
    }
}
