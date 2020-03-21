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

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;

public class GameEffect extends GameObject implements IEntityModifierListener {
    Sprite hit;
    String texname;

    public GameEffect(final String texname) {
        this.texname = texname;
        hit = new Sprite(0, 0, ResourceManager.getInstance()
                .getTexture(texname));
    }

    public void setColor(final RGBColor color) {
        hit.setColor(color.r(), color.g(), color.b());
    }

    public void init(final Scene scene, final PointF pos, final float scale,
                     final IEntityModifier... entityModifiers) {
        hit.setPosition(pos.x - hit.getTextureRegion().getWidth() / 2, pos.y
                - hit.getTextureRegion().getHeight() / 2);
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
