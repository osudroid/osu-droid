package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseSineOut;
import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dgsrz on 15/11/1.
 */
public class ComboBurst {

    private final List<Sprite> comboBursts = new ArrayList<Sprite>();

    private final List<BassSoundProvider> comboBurstVocals = new ArrayList<BassSoundProvider>();

    private final float rightX;

    private final float bottomY;

    private int nextKeyComboNum = 0;

    private float fromX = 0;

    private int nextShowId = 0;

    private int nextSoundId = 0;

    public ComboBurst(float rightX, float bottomY) {
        this.rightX = rightX;
        this.bottomY = bottomY;
        breakCombo();

        TextureRegion globalTex = ResourceManager.getInstance().getTexture("comboburst");
        if (globalTex != null) {
            Sprite sprite = new Sprite(0, 0, globalTex);
            sprite.setAlpha(0f);
            sprite.setIgnoreUpdate(true);
            comboBursts.add(sprite);
        }
        BassSoundProvider sound = ResourceManager.getInstance().getSound("comboburst");
        if (sound != null) {
            comboBurstVocals.add(sound);
        }
        for (int i = 0; i < 10; i++) {
            TextureRegion tex = ResourceManager.getInstance().getTexture("comboburst-" + i);
            if (tex != null) {
                Sprite sprite = new Sprite(0, 0, tex);
                sprite.setAlpha(0f);
                sprite.setIgnoreUpdate(true);
                comboBursts.add(sprite);
            }
            sound = ResourceManager.getInstance().getSound("comboburst-" + i);
            if (sound != null) {
                comboBurstVocals.add(sound);
            }
        }
    }

    public void checkAndShow(int currentCombo) {
        if (Config.isComboburst() && currentCombo >= nextKeyComboNum) {
            if (!comboBurstVocals.isEmpty()) {
                comboBurstVocals.get(nextSoundId).play(0.8f);
            }
            if (!comboBursts.isEmpty()) {
                Sprite sprite = comboBursts.get(nextShowId);
                float toX;
                if (fromX > 0) {
                    toX = fromX - sprite.getWidth();
                } else {
                    fromX = -sprite.getWidth();
                    toX = 0;
                }
                sprite.setIgnoreUpdate(false);
                sprite.setPosition(fromX, bottomY - sprite.getHeight());
                sprite.registerEntityModifier(new SequenceEntityModifier(
                        new IEntityModifier.IEntityModifierListener() {
                            @Override
                            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

                            }

                            @Override
                            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                                pItem.setAlpha(0f);
                                pItem.setIgnoreUpdate(true);
                            }
                        },
                        new ParallelEntityModifier(
                                new MoveXModifier(0.5f, fromX, toX, EaseSineOut.getInstance()),
                                new FadeInModifier(0.5f)
                        ),
                        new DelayModifier(1.0f),
                        new ParallelEntityModifier(
                                new MoveXModifier(0.5f, toX, fromX, EaseSineOut.getInstance()),
                                new FadeOutModifier(0.5f)
                        )
                ));
            }

            if (!comboBursts.isEmpty()) {
                int length = comboBursts.size();
                nextShowId = (nextShowId + 1) % length;
            }
            if (!comboBurstVocals.isEmpty()) {
                int length = comboBurstVocals.size();
                nextSoundId = (nextSoundId + 1) % length;
            }
            if (nextKeyComboNum == 30) {
                nextKeyComboNum = 60;
                fromX = rightX;
            } else if (nextKeyComboNum == 60) {
                nextKeyComboNum = 100;
                fromX = -1;
            } else {
                nextKeyComboNum += 100;
                int mod = nextKeyComboNum / 100;
                if (mod % 2 == 0) {
                    fromX = rightX;
                } else {
                    fromX = -1;
                }
            }
        }
    }

    public void breakCombo() {
        fromX = 0;
        nextKeyComboNum = 30;
    }

    public void attachAll(Scene scene) {
        for (final Sprite sprite : comboBursts) {
            scene.attachChild(sprite);
        }
    }

    public void detachAll() {
        for (final Sprite sprite : comboBursts) {
            sprite.detachSelf();
        }
    }

}
