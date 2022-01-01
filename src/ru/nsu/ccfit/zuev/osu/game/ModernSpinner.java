package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ColorModifier;
import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.MathUtils;
import org.anddev.andengine.util.modifier.IModifier;

import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

/**
 * Created by dgsrz on 15/10/19.
 */
public class ModernSpinner extends Spinner {

    private final Sprite middle;
    private final Sprite middle2;
    private final Sprite bottom;
    private final Sprite top;
    private final Sprite glow;
    // private final Sprite spin;
    // private final Sprite clear;

    private GameObjectListener listener;
    private Scene scene;
    public PointF center;
    private float needRotations;
    private int fullRotations = 0;
    private float rotations = 0;
    private int soundId;
    private boolean clear;
    private int score = 1;
    private StatisticV2 stat;
    private ScoreNumber bonusScore;
    private PointF oldMouse;
    private float totalTime;

    public ModernSpinner() {
        ResourceManager.getInstance().checkEvoSpinnerTextures();
        center = Utils.trackToRealCoords(new PointF(Constants.MAP_WIDTH / 2,
                Constants.MAP_HEIGHT / 2));
        middle = SpritePool.getInstance().getCenteredSprite(
                "spinner-middle", center);
        middle2 = SpritePool.getInstance().getCenteredSprite(
                "spinner-middle2", center);
        bottom = SpritePool.getInstance().getCenteredSprite(
                "spinner-bottom", center);
        top = SpritePool.getInstance().getCenteredSprite(
                "spinner-top", center);
        glow = SpritePool.getInstance().getCenteredSprite(
                "spinner-glow", center);
    }

    public void init(GameObjectListener listener, Scene scene,
                     float aheadTime, float time, float rps,
                     int sound, String tempSound, StatisticV2 stat) {
        this.scene = scene;
        this.needRotations = rps * time;
        this.listener = listener;
        this.soundId = sound;
        this.stat = stat;
        this.clear = false;
        this.fullRotations = 0;
        this.rotations = 0;

        glow.setAlpha(0f);
        glow.setScale(0.9f);
        glow.setColor(0f, 0.8f, 1f);

        middle.setAlpha(0f);
        middle.setScale(0.9f);

        middle2.setAlpha(0f);
        middle2.setScale(0.9f);

        bottom.setAlpha(0f);
        bottom.setScale(0.9f);

        top.setAlpha(0f);
        top.setScale(0.9f);

        scene.attachChild(glow);
        scene.attachChild(bottom);
        scene.attachChild(top);
        scene.attachChild(middle);
        scene.attachChild(middle2);

        top.registerEntityModifier(
                new SequenceEntityModifier(
                        new IEntityModifier.IEntityModifierListener() {
                            @Override
                            public void onModifierStarted(final IModifier<IEntity> pModifier, final IEntity pItem) {
                            }

                            @Override
                            public void onModifierFinished(final IModifier<IEntity> pModifier, final IEntity pItem) {
                                SyncTaskManager.getInstance().run(new Runnable() {
                                    public void run() {
                                        removeFromScene();
                                    }
                                });
                            }
                        },
                        new SequenceEntityModifier(
                                new AlphaModifier(aheadTime, 0, 1f),
                                new DelayModifier(time)
                        )
                ));
        bottom.registerEntityModifier(new AlphaModifier(aheadTime, 0, 1f));
        middle.registerEntityModifier(new AlphaModifier(aheadTime, 0, 1f));
        middle2.registerEntityModifier(new AlphaModifier(aheadTime, 0, 1f));
    }

    @Override
    public void update(float dt) {
        boolean isTouched = false;
        int cursorIndex = -1;
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (listener.isMouseDown(i)) {
                cursorIndex = i;
                isTouched = true;
                break;
            }
        }
        if (!isTouched && !autoPlay) {
            return;
        }

        final PointF mouse = autoPlay ? center : listener.getMousePos(cursorIndex);
        final PointF v = new PointF(mouse.x - center.x, mouse.y - center.y);
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (oldMouse == null || listener.isMousePressed(this, i)) {
                oldMouse = v;
                return;
            }
        }
        float degree = MathUtils.radToDeg(Utils.direction(v));
        top.setRotation(degree);
        bottom.setRotation(degree / 2);
        // bottom.setRotation(-degree);
        final PointF v1 = Utils.normalize(v);
        final PointF v2 = Utils.normalize(oldMouse);
        float dfill = v1.x * v2.y - v1.y * v2.x;
        if (autoPlay) {
            dfill = 5 * 4 * dt;
            degree = (rotations + dfill / 4f) * 360;
            top.setRotation(degree);
            //auto时，FL光圈绕中心旋转
            if (GameHelper.isAutopilotMod() || GameHelper.isAuto()) {
                float pX = center.x + 50 * (float) Math.sin(degree);
                float pY = center.y + 50 * (float) Math.cos(degree);
                listener.updateAutoBasedPos(pX, pY);
            }
            // bottom.setRotation(-degree);
        }
        rotations += dfill / 4f;
        float percentfill = (Math.abs(rotations) + fullRotations) / needRotations;
        float percent = percentfill > 1 ? 1 : percentfill;

        middle.setColor(1, 1 - percent, 1 - percent);
        top.setScale(0.9f + percent * 0.1f);
        bottom.setScale(0.9f + percent * 0.1f);
        middle.setScale(0.9f + percent * 0.1f);
        middle2.setScale(0.9f + percent * 0.1f);
        glow.setAlpha(percent * 0.8f);
        glow.setScale(0.9f + percent * 0.1f);

        if (percentfill > 1 || clear) {
            percentfill = 1;
            if (!clear) {
                // Clear Sprite
                clear = true;
            } else if (Math.abs(rotations) > 1) {
                if (bonusScore != null) {
                    bonusScore.detachFromScene(scene);
                }
                rotations -= 1 * Math.signum(rotations);
                bonusScore = new ScoreNumber(center.x, center.y + 100,
                        String.valueOf(score * 1000), 1.1f, true);
                listener.onSpinnerHit(id, 1000, false, 0);
                score++;
                bonusScore.attachToScene(scene);
                ResourceManager.getInstance().getSound("spinnerbonus").play();
                glow.registerEntityModifier(new SequenceEntityModifier(
                        new ColorModifier(0.1f, 0f, 1f, 0.8f, 1f, 1f, 1f),
                        new ColorModifier(0.1f, 1f, 0f, 1f, 0.8f, 1f, 1f)
                ));
                float rate = 0.375f;
                if (GameHelper.getDrain() > 0) {
                    rate = 1 + (GameHelper.getDrain() / 4f);
                }
                stat.changeHp(rate * 0.01f * totalTime / needRotations);
            }
        } else if (Math.abs(rotations) > 1) {
            rotations -= 1 * Math.signum(rotations);
            if (replayObjectData == null || replayObjectData.accuracy / 4 > fullRotations) {
                fullRotations++;
                stat.registerSpinnerHit();
                float rate = 0.375f;
                if (GameHelper.getDrain() > 0) {
                    rate = 1 + (GameHelper.getDrain() / 2f);
                }
                stat.changeHp(rate * 0.01f * totalTime / needRotations);
            }
        }
        oldMouse = v;
    }

    public void removeFromScene() {
//        if (clearText != null) {
//            scene.detachChild(clearText);
//            SpritePool.getInstance().putSprite("spinner-clear", clearText);
//        }
        glow.clearEntityModifiers();
        scene.detachChild(middle);
        scene.detachChild(middle2);
        scene.detachChild(bottom);
        scene.detachChild(top);
        scene.detachChild(glow);
        // GameObjectPool.getInstance().putSpinner(this);

        if (bonusScore != null) {
            bonusScore.detachFromScene(scene);
        }
        listener.removeObject(ModernSpinner.this);
        int score = 0;
        if (replayObjectData != null) {
            if (fullRotations < replayObjectData.accuracy / 4)
                fullRotations = replayObjectData.accuracy / 4;
            if (fullRotations >= needRotations)
                clear = true;
            int bonusRot = (int) (replayObjectData.accuracy / 4 - needRotations + 1);
            while (bonusRot < score) {
                bonusRot++;
                listener.onSpinnerHit(id, 1000, false, 0);
            }
        }
        float percentfill = (Math.abs(rotations) + fullRotations)
                / needRotations;
        if (percentfill > 0.9f) {
            score = 50;
        }
        if (percentfill > 0.95f) {
            score = 100;
        }
        if (clear) {
            score = 300;
        }
        if (replayObjectData != null) {
            switch (replayObjectData.accuracy % 4) {
                case 0:
                    score = 0;
                    break;
                case 1:
                    score = 50;
                    break;
                case 2:
                    score = 100;
                    break;
                case 3:
                    score = 300;
                    break;
            }
        }
        listener.onSpinnerHit(id, score, endsCombo, this.score + fullRotations - 1);
        if (score > 0) {
            Utils.playHitSound(listener, soundId);
        }
    }
}
