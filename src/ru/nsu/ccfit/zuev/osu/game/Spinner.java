package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo;
import com.rian.osu.beatmap.hitobject.HitSampleInfo;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class Spinner extends GameObject {
    private final Sprite background;
    public final PointF center;
    private final Sprite circle;
    private final Sprite approachCircle;
    private final Sprite metre;
    private final Sprite spinText;
    private final TextureRegion metreRegion;
    private final Sprite clearText;
    private final ScoreNumber bonusScore;
    protected com.rian.osu.beatmap.hitobject.Spinner beatmapSpinner;
    private PointF oldMouse;
    protected GameObjectListener listener;
    private Scene scene;
    private int fullRotations = 0;
    private float rotations = 0;
    private float needRotations;
    private boolean clear = false;
    private int score = 1;
    private float metreY;
    private StatisticV2 stat;
    private float duration;
    protected HitSampleInfo spinnerSpinSample;
    protected HitSampleInfo spinnerBonusSample;

    private final PointF currMouse = new PointF();


    public Spinner() {
        ResourceManager.getInstance().checkSpinnerTextures();
        this.pos = new PointF((float) Constants.MAP_WIDTH / 2, (float) Constants.MAP_HEIGHT / 2);
        center = Utils.trackToRealCoords(pos);
        background = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-background"));
        final float scaleX = Config.getRES_WIDTH() / background.getWidth();
        background.setScale(scaleX);

        circle = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-circle"));
        metreRegion = ResourceManager.getInstance().getTexture("spinner-metre")
                .deepCopy();
        metre = new Sprite(center.x - (float) Config.getRES_WIDTH() / 2,
                Config.getRES_HEIGHT(), metreRegion);
        metre.setWidth(Config.getRES_WIDTH());
        metre.setHeight(background.getHeightScaled());
        approachCircle = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-approachcircle"));
        spinText = new CentredSprite(center.x, center.y * 1.5f, ResourceManager
                .getInstance().getTexture("spinner-spin"));

        clearText = new CentredSprite(center.x, center.y * 0.5f, ResourceManager.getInstance().getTexture("spinner-clear"));
        bonusScore = new ScoreNumber(center.x, center.y + 100, "", 1.1f, true);
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final com.rian.osu.beatmap.hitobject.Spinner beatmapSpinner, final float rps,
                     final StatisticV2 stat) {
        fullRotations = 0;
        rotations = 0;
        this.scene = scene;
        this.duration = (float) beatmapSpinner.getDuration() / 1000 / GameHelper.getSpeedMultiplier();
        this.beatmapSpinner = beatmapSpinner;
        endsCombo = beatmapSpinner.isLastInCombo();

        needRotations = rps * duration;
        if (duration < 0.05f) {
            needRotations = 0.1f;
        }

        this.listener = listener;
        this.stat = stat;
        startHit = true;
        clear = duration <= 0f;
        score = 1;

        ResourceManager.getInstance().checkSpinnerTextures();

        float timePreempt = (float) beatmapSpinner.timePreempt / 1000 / GameHelper.getSpeedMultiplier();

        background.setAlpha(0);
        background.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(timePreempt * 0.75f),
            Modifiers.fadeIn(timePreempt * 0.25f)
        ));

        circle.setAlpha(0);
        circle.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(timePreempt * 0.75f),
            Modifiers.fadeIn(timePreempt * 0.25f)
        ));

        metreY = (Config.getRES_HEIGHT() - background.getHeightScaled()) / 2;
        metre.setAlpha(0);
        metre.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(timePreempt * 0.75f),
            Modifiers.fadeIn(timePreempt * 0.25f)
        ));
        metreRegion.setTexturePosition(0, (int) metre.getHeightScaled());

        approachCircle.setAlpha(0);
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(false);
        }
        approachCircle.registerEntityModifier(Modifiers.sequence(
            entity -> Execution.updateThread(this::removeFromScene),
            Modifiers.delay(timePreempt),
            Modifiers.parallel(
                Modifiers.alpha(duration, 0.75f, 1),
                Modifiers.scale(duration, 2.0f, 0)
            )
        ));

        spinText.setAlpha(0);
        spinText.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(timePreempt * 0.75f),
            Modifiers.fadeIn(timePreempt * 0.25f),
            Modifiers.delay(timePreempt / 2),
            Modifiers.fadeOut(timePreempt * 0.25f)
        ));

        scene.attachChild(spinText, 0);
        scene.attachChild(approachCircle, 0);
        scene.attachChild(circle, 0);
        scene.attachChild(metre, 0);
        scene.attachChild(background, 0);

        oldMouse = null;

    }

    void removeFromScene() {
        scene.detachChild(clearText);
        scene.detachChild(spinText);
        scene.detachChild(background);
        approachCircle.detachSelf();
        scene.detachChild(circle);
        scene.detachChild(metre);
        scene.detachChild(bonusScore);

        listener.removeObject(Spinner.this);
        GameObjectPool.getInstance().putSpinner(this);

        int score = 0;
        if (replayObjectData != null) {
            //int bonusRot = (int) (replayData.accuracy / 4 - needRotations + 1);
            //while (bonusRot < 0) {
            //    bonusRot++;
            //    listener.onSpinnerHit(id, 1000, false, 0);
            //}

            //if (rotations count < the rotations in replay), let rotations count = the rotations in replay
            while (fullRotations + this.score < replayObjectData.accuracy / 4 + 1){
                fullRotations++;
                listener.onSpinnerHit(id, 1000, false, 0);
            }
            if (fullRotations >= needRotations)
                clear = true;
        }
        float percentfill = (Math.abs(rotations) + fullRotations) / needRotations;
        if(needRotations <= 0.1f){
            clear = true;
            percentfill = 1;
        }
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
            score = switch (replayObjectData.accuracy % 4) {
                case 0 -> 0;
                case 1 -> 50;
                case 2 -> 100;
                case 3 -> 300;
                default -> score;
            };
        }
        listener.onSpinnerHit(id, score, endsCombo, this.score + fullRotations - 1);
        if (score > 0) {
            listener.playSamples(beatmapSpinner);
        }
    }


    @Override
    public void update(final float dt) {
        if (circle.getAlpha() == 0) {
            return;
        }
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

        circle.setRotation(MathUtils.radToDeg(Utils.direction(currMouse)));

        var len1 = Utils.length(currMouse);
        var len2 = Utils.length(oldMouse);
        var dfill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2);

        if (Math.abs(len1) < 0.0001f || Math.abs(len2) < 0.0001f)
            dfill = 0;

        if (autoPlay) {
            dfill = 5 * 4 * dt;
            circle.setRotation((rotations + dfill / 4f) * 360);
            //auto时，FL光圈绕中心旋转
            if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
               float angle = (rotations + dfill / 4f) * 360;
               float pX = center.x + 50 * (float)Math.sin(angle);
               float pY = center.y + 50 * (float)Math.cos(angle);
               listener.updateAutoBasedPos(pX, pY);
            }
        }

        if (dfill > 0) {
            playSpinnerSpinSound();
        }

        rotations += dfill / 4f;
        float percentfill = (Math.abs(rotations) + fullRotations) / needRotations;

        if (percentfill > 1 || clear) {
            percentfill = 1;
            if (!clear) {
                float speedMultiplier = GameHelper.getSpeedMultiplier();
                clearText.registerEntityModifier(Modifiers.fadeIn(0.25f / speedMultiplier));
                clearText.registerEntityModifier(Modifiers.scale(0.25f / speedMultiplier, 1.5f, 1));
                scene.attachChild(clearText);
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
                playSpinnerBonusSound();
                float rate = 0.375f;
                if (GameHelper.getHealthDrain() > 0) {
                    rate = 1 + (GameHelper.getHealthDrain() / 4f);
                }
                stat.changeHp(rate * 0.01f * duration / needRotations);
            }
        } else if (Math.abs(rotations) > 1) {
            rotations -= 1 * Math.signum(rotations);
            if (replayObjectData == null || replayObjectData.accuracy / 4 > fullRotations) {
                fullRotations++;
                stat.registerSpinnerHit();
                float rate = 0.375f;
                if (GameHelper.getHealthDrain() > 0) {
                    rate = 1 + (GameHelper.getHealthDrain() / 2f);
                }
                stat.changeHp(rate * 0.01f * duration / needRotations);
            }
        }
        metre.setPosition(metre.getX(),
                metreY + metre.getHeight() * (1 - Math.abs(percentfill)));
        metreRegion.setTexturePosition(0,
                (int) (metre.getBaseHeight() * (1 - Math.abs(percentfill))));

        oldMouse.set(currMouse);
    }

    protected void reloadHitSounds() {
        spinnerBonusSample = null;
        spinnerSpinSample = null;

        for (var sample : beatmapSpinner.getAuxiliarySamples()) {
            if (spinnerBonusSample != null && spinnerSpinSample != null) {
                break;
            }

            if (!(sample instanceof BankHitSampleInfo bankSample)) {
                continue;
            }

            if (bankSample.name.equals("spinnerbonus")) {
                spinnerBonusSample = bankSample;
            } else if (bankSample.name.equals("spinnerspin")) {
                spinnerSpinSample = bankSample;
            }
        }
    }

    @Override
    public void stopAuxiliarySamples() {
        if (spinnerBonusSample != null) {
            listener.stopSample(spinnerBonusSample);
        }

        if (spinnerSpinSample != null) {
            listener.stopSample(spinnerSpinSample);
        }
    }

    protected void playSpinnerBonusSound() {
        if (spinnerBonusSample != null) {
            listener.playSample(spinnerBonusSample, false);
        }
    }

    protected void playSpinnerSpinSound() {
        if (spinnerSpinSample != null) {
            listener.playSample(spinnerSpinSample, false);
        }
    }
}
