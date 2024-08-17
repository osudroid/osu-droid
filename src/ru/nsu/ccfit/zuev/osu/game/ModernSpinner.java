package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;
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
    private final ScoreNumber bonusScore;

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
    private PointF oldMouse;
    private float totalTime;

    private final PointF currMouse = new PointF();


    public ModernSpinner() {
        ResourceManager.getInstance().checkEvoSpinnerTextures();
        center = Utils.trackToRealCoords(new PointF((float) Constants.MAP_WIDTH / 2,
                (float) Constants.MAP_HEIGHT / 2));
        middle = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-middle"));
        middle2 = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-middle2"));
        bottom = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-bottom"));
        top = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-top"));
        glow = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-glow"));

        bonusScore = new ScoreNumber(center.x, center.y + 100, "", 1.1f, true);
    }

    public void init(GameObjectListener listener, Scene scene,
                     float aheadTime, float time, float rps,
                     int sound, String tempSound, StatisticV2 stat) {
        this.scene = scene;
        this.needRotations = rps * time;
        this.listener = listener;
        this.soundId = sound;
        this.stat = stat;
        this.totalTime = time;
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

        top.registerEntityModifier(Modifiers.sequence(
            Modifiers.fadeIn(aheadTime),
            Modifiers.delay(time).setOnFinished(entity -> Execution.updateThread(this::removeFromScene))
        ));

        bottom.registerEntityModifier(Modifiers.fadeIn(aheadTime));
        middle.registerEntityModifier(Modifiers.fadeIn(aheadTime));
        middle2.registerEntityModifier(Modifiers.fadeIn(aheadTime));
    }

    @Override
    public void update(float dt) {

        PointF mouse = null;

        for (int i = 0, count = listener.getCursorsCount(); i < count; ++i) {
            if (mouse == null) {
                if (autoPlay) {
                    mouse = center;
                } else if (listener.isMouseDown(i)) {
                    mouse = listener.getMousePos(i);
                } else {
                    continue;
                }
                currMouse.set(mouse.x - center.x, mouse.y - center.y);
            }

            if (oldMouse == null || listener.isMousePressed(this, i)) {
                if (oldMouse == null) {
                    oldMouse = new PointF();
                }
                oldMouse.set(currMouse);
                return;
            }
        }

        if (mouse == null)
            return;

        float degree = MathUtils.radToDeg(Utils.direction(currMouse));
        top.setRotation(degree);
        bottom.setRotation(degree / 2);

        var len1 = Utils.length(currMouse);
        var len2 = Utils.length(oldMouse);
        var dfill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2);

        if (Math.abs(len1) < 0.0001f || Math.abs(len2) < 0.0001f)
            dfill = 0;

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
                if (bonusScore.hasParent()) {
                    scene.detachChild(bonusScore);
                }
                rotations -= 1 * Math.signum(rotations);
                bonusScore.setText(String.valueOf(score * 1000));
                listener.onSpinnerHit(id, 1000, false, 0);
                score++;
                scene.attachChild(bonusScore);
                ResourceManager.getInstance().getSound("spinnerbonus").play();
                glow.registerEntityModifier(
                    Modifiers.sequence(
                        Modifiers.color(0.1f, 0f, 1f, 0.8f, 1f, 1f, 1f),
                        Modifiers.color(0.1f, 1f, 0f, 1f, 0.8f, 1f, 1f)
                    )
                );
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

        oldMouse.set(currMouse);
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
        scene.detachChild(bonusScore);

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
