package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.osu.Execution;
import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Modifiers;
import com.reco1l.andengine.Anchor;
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo;
import com.rian.osu.beatmap.hitobject.Spinner;
import com.rian.osu.gameplay.GameplayHitSampleInfo;
import com.rian.osu.gameplay.GameplaySequenceHitSampleInfo;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameplaySpinner extends GameObject {
    private final ExtendedSprite background;
    public final PointF center;
    private final ExtendedSprite circle;
    private final ExtendedSprite approachCircle;
    private final Sprite metre;
    private final ExtendedSprite spinText;
    private final TextureRegion metreRegion;
    private final ExtendedSprite clearText;
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

    protected final boolean isSpinnerFrequencyModulate;
    protected GameplayHitSampleInfo[] hitSamples;
    protected final GameplaySequenceHitSampleInfo spinnerSpinSample;
    protected final GameplaySequenceHitSampleInfo spinnerBonusSample;

    private final PointF currMouse = new PointF();

    public GameplaySpinner() {
        ResourceManager.getInstance().checkSpinnerTextures();
        position = new PointF(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f);
        center = Utils.trackToRealCoords(position);

        background = new ExtendedSprite();
        background.setOrigin(Anchor.Center);
        background.setPosition(center.x, center.y);
        background.setTextureRegion(ResourceManager.getInstance().getTexture("spinner-background"));
        background.setScale(Config.getRES_WIDTH() / background.getWidth());

        circle = new ExtendedSprite();
        circle.setOrigin(Anchor.Center);
        circle.setPosition(center.x, center.y);
        circle.setTextureRegion(ResourceManager.getInstance().getTexture("spinner-circle"));

        metreRegion = ResourceManager.getInstance().getTexture("spinner-metre").deepCopy();

        metre = new Sprite(center.x - Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT(), metreRegion);
        metre.setWidth(Config.getRES_WIDTH());
        metre.setHeight(background.getHeightScaled());

        approachCircle = new ExtendedSprite();
        approachCircle.setOrigin(Anchor.Center);
        approachCircle.setPosition(center.x, center.y);
        approachCircle.setTextureRegion(ResourceManager.getInstance().getTexture("spinner-approachcircle"));

        spinText = new ExtendedSprite();
        spinText.setOrigin(Anchor.Center);
        spinText.setPosition(center.x, center.y * 1.5f);
        spinText.setTextureRegion(ResourceManager.getInstance().getTexture("spinner-spin"));

        clearText = new ExtendedSprite();
        clearText.setOrigin(Anchor.Center);
        clearText.setPosition(center.x, center.y * 0.5f);
        clearText.setTextureRegion(ResourceManager.getInstance().getTexture("spinner-clear"));

        bonusScore = new ScoreNumber(center.x, center.y + 100, "", 1.1f, true);

        isSpinnerFrequencyModulate = OsuSkin.get().isSpinnerFrequencyModulate();
        spinnerSpinSample = new GameplaySequenceHitSampleInfo();
        spinnerBonusSample = new GameplaySequenceHitSampleInfo();

        // Spinners always end combo.
        endsCombo = true;
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final Spinner beatmapSpinner, final float rps, final StatisticV2 stat) {
        fullRotations = 0;
        rotations = 0;
        this.scene = scene;
        this.duration = (float) beatmapSpinner.getDuration() / 1000f;
        this.beatmapSpinner = beatmapSpinner;

        needRotations = rps * duration;
        if (duration < 0.05f) {
            needRotations = 0.1f;
        }

        this.listener = listener;
        this.stat = stat;
        startHit = true;
        clear = duration <= 0f;
        score = 1;

        reloadHitSounds();
        ResourceManager.getInstance().checkSpinnerTextures();

        float timePreempt = (float) beatmapSpinner.timePreempt / 1000f;

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
        approachCircle.registerEntityModifier(Modifiers.sequence(e -> Execution.updateThread(this::removeFromScene),
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

        listener.removeObject(GameplaySpinner.this);
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
        stopLoopingSamples();
        listener.onSpinnerHit(id, score, endsCombo, this.score + fullRotations - 1);
        playAndFreeHitSamples(score);
    }


    @Override
    public void update(final float dt) {
        if (circle.getAlpha() == 0) {
            return;
        }

        updateSamples(dt);
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

        rotations += dfill / 4f;
        float percentfill = (Math.abs(rotations) + fullRotations) / needRotations;

        if (dfill != 0) {
            updateSpinSampleFrequency(percentfill);
            spinnerSpinSample.play();
        } else {
            spinnerSpinSample.stop();
        }

        if (percentfill > 1 || clear) {
            percentfill = 1;
            if (!clear) {
                clearText.registerEntityModifier(Modifiers.fadeIn(0.25f));
                clearText.registerEntityModifier(Modifiers.scale(0.25f, 1.5f, 1));
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
                spinnerBonusSample.play();
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
        var parsedSamples = beatmapSpinner.getSamples();
        hitSamples = new GameplayHitSampleInfo[parsedSamples.size()];

        for (int i = 0; i < hitSamples.length; ++i) {
            var gameplaySample = GameplayHitSampleInfo.pool.obtain();
            gameplaySample.init(parsedSamples.get(i));

            if (GameHelper.isSamplesMatchPlaybackRate()) {
                gameplaySample.setFrequency(GameHelper.getSpeedMultiplier());
            }

            hitSamples[i] = gameplaySample;
        }

        spinnerSpinSample.reset();
        spinnerSpinSample.setLooping(true);

        spinnerBonusSample.reset();

        float startTime = (float) beatmapSpinner.startTime;

        for (int i = 0, size = beatmapSpinner.getAuxiliarySamples().size(); i < size; ++i) {
            if (spinnerSpinSample.isInitialized() && spinnerBonusSample.isInitialized()) {
                return;
            }

            var auxiliarySample = beatmapSpinner.getAuxiliarySamples().get(i);
            var firstSample = auxiliarySample.get(0).getSecond();

            if (!(firstSample instanceof BankHitSampleInfo bankSample)) {
                continue;
            }

            if (bankSample.name.equals("spinnerbonus")) {
                spinnerBonusSample.init(startTime, auxiliarySample);
            } else if (bankSample.name.equals("spinnerspin")) {
                spinnerSpinSample.init(startTime, auxiliarySample);
            }
        }
    }

    protected void playAndFreeHitSamples(int obtainedScore) {
        for (int i = 0; i < hitSamples.length; ++i) {
            var sample = hitSamples[i];

            if (obtainedScore > 0) {
                sample.play();
            }

            sample.reset();
            GameplayHitSampleInfo.pool.free(sample);
        }

        hitSamples = null;
    }

    @Override
    public void stopLoopingSamples() {
        spinnerSpinSample.stopAll();
    }

    protected void updateSamples(float dt) {
        spinnerSpinSample.update(dt);
        spinnerBonusSample.update(dt);
    }

    protected void updateSpinSampleFrequency(float progress) {
        boolean applyTrackRate = GameHelper.isSamplesMatchPlaybackRate();

        if (isSpinnerFrequencyModulate) {
            // Note that osu!stable sets the frequency directly at BassSoundProvider level.
            // This implementation tries to closely follow that behavior with the default frequency in mind.
            float frequency = Math.min(100000, 20000 + 40000 * progress) / SongService.defaultFrequency;

            if (applyTrackRate) {
                frequency *= GameHelper.getSpeedMultiplier();
            }

            spinnerSpinSample.setFrequency(frequency);
        } else if (applyTrackRate) {
            spinnerSpinSample.setFrequency(GameHelper.getSpeedMultiplier());
        } else {
            spinnerSpinSample.setFrequency(1);
        }
    }
}
