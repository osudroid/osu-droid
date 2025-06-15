package ru.nsu.ccfit.zuev.osu.game;

import com.edlplan.framework.easing.Easing;
import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.modifier.Modifiers;
import com.reco1l.andengine.Anchor;
import com.osudroid.ui.v2.game.NumberedCirclePiece;
import com.reco1l.framework.Color4;
import com.rian.osu.beatmap.hitobject.HitCircle;
import com.rian.osu.gameplay.GameplayHitSampleInfo;
import com.rian.osu.mods.ModHidden;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameplayHitCircle extends GameObject {

    private final UISprite approachCircle;
    private Color4 comboColor = new Color4();
    private GameObjectListener listener;
    private Scene scene;
    private HitCircle beatmapCircle;
    private float radiusSquared;
    private float passedTime;
    private float timePreempt;
    private boolean kiai;
    private GameplayHitSampleInfo[] hitSamples;

    /**
     * The circle piece that represents the circle body and overlay.
     */
    private final NumberedCirclePiece circlePiece;


    public GameplayHitCircle() {
        circlePiece = new NumberedCirclePiece("hitcircle", "hitcircleoverlay");
        approachCircle = new UISprite();
        approachCircle.setOrigin(Anchor.Center);
        approachCircle.setTextureRegion(ResourceManager.getInstance().getTexture("approachcircle"));
    }

    public void init(final GameObjectListener listener, final Scene pScene, final HitCircle beatmapCircle,
                     final float secPassed, final Color4 comboColor) {
        // Storing parameters into fields
        this.beatmapCircle = beatmapCircle;
        replayObjectData = null;

        var stackedPosition = beatmapCircle.getScreenSpaceGameplayStackedPosition();
        position.set(stackedPosition.x, stackedPosition.y);

        endsCombo = beatmapCircle.isLastInCombo();
        this.listener = listener;
        scene = pScene;
        timePreempt = (float) beatmapCircle.timePreempt / 1000;

        hitTime = (float) beatmapCircle.startTime / 1000;
        passedTime = secPassed - (hitTime - timePreempt);
        startHit = false;
        kiai = GameHelper.isKiai();
        this.comboColor = comboColor;

        // Calculating position of top/left corner for sprites and hit radius
        final float scale = beatmapCircle.getScreenSpaceGameplayScale();
        radiusSquared = (float) beatmapCircle.getScreenSpaceGameplayRadius();
        radiusSquared *= radiusSquared;

        float fadeInDuration = (float) beatmapCircle.timeFadeIn / 1000f;

        // Initializing sprites
        circlePiece.setCircleColor(comboColor);
        circlePiece.setScale(scale);
        circlePiece.setAlpha(0);
        circlePiece.setPosition(this.position.x, this.position.y);

        int comboNum = beatmapCircle.getIndexInCurrentCombo() + 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            comboNum %= 10;
        }

        boolean applyIncreasedVisibility = Config.isShowFirstApproachCircle() && beatmapCircle.isFirstNote();

        circlePiece.setNumberText(comboNum);
        circlePiece.setNumberScale(OsuSkin.get().getComboTextScale());
        circlePiece.setVisible(!GameHelper.isTraceable() || applyIncreasedVisibility);

        approachCircle.setColor(comboColor);
        approachCircle.setScale(scale * 3 * (float) (beatmapCircle.timePreempt / GameHelper.getOriginalTimePreempt()));
        approachCircle.setAlpha(0);
        approachCircle.setPosition(this.position.x, this.position.y);
        approachCircle.setVisible(!GameHelper.isHidden() || applyIncreasedVisibility);

        if (GameHelper.getHidden() != null && !GameHelper.getHidden().isOnlyFadeApproachCircles()) {
            float fadeOutDuration = timePreempt * (float) ModHidden.FADE_OUT_DURATION_MULTIPLIER;

            circlePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));
        } else if (circlePiece.isVisible()) {
            circlePiece.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }

        if (approachCircle.isVisible()) {
            var easing = Easing.None;
            var approachDifferentMod = GameHelper.getApproachDifferent();

            if (approachDifferentMod != null) {
                approachCircle.setScale(scale * approachDifferentMod.getScale());
                easing = approachDifferentMod.getEasing();
            }

            approachCircle.registerEntityModifier(
                Modifiers.alpha(Math.min(fadeInDuration * 2, timePreempt), 0, 0.9f)
            );

            approachCircle.registerEntityModifier(
                Modifiers.scale(timePreempt, approachCircle.getScaleX(), scale, e -> e.setAlpha(0), easing)
            );
        }

        if (Config.isDimHitObjects() && circlePiece.isVisible()) {

            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            var dimDelaySec = timePreempt - objectHittableRange;
            var colorDim = 195f / 255f;

            circlePiece.setColor(colorDim, colorDim, colorDim);
            circlePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    circlePiece.getRed(), 1f,
                    circlePiece.getGreen(), 1f,
                    circlePiece.getBlue(), 1f
                )
            ));
        }

        // Initialize samples
        var parsedSamples = beatmapCircle.getSamples();
        hitSamples = new GameplayHitSampleInfo[parsedSamples.size()];

        for (int i = 0, size = parsedSamples.size(); i < size; i++) {
            var gameplaySample = GameplayHitSampleInfo.pool.obtain();
            gameplaySample.init(parsedSamples.get(i));

            if (GameHelper.isSamplesMatchPlaybackRate()) {
                gameplaySample.setFrequency(GameHelper.getSpeedMultiplier());
            }

            hitSamples[i] = gameplaySample;
        }

        scene.attachChild(circlePiece, 0);
        scene.attachChild(approachCircle);
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }

        for (int i = 0; i < hitSamples.length; ++i) {
            hitSamples[i].reset();
            GameplayHitSampleInfo.pool.free(hitSamples[i]);
        }

        hitSamples = null;

        circlePiece.clearEntityModifiers();
        approachCircle.clearEntityModifiers();

        // Detach all objects
        circlePiece.detachSelf();
        approachCircle.detachSelf();
        listener.removeObject(this);
        GameObjectPool.getInstance().putCircle(this);
        scene = null;
    }

    private boolean canBeHit(float dt, float frameHitOffset) {
        // At this point, the object's state is already in the next update tick.
        // However, hit judgements require the object's state to be in the previous tick.
        // Therefore, we subtract dt to get the object's state in the previous tick.
        return passedTime - dt + frameHitOffset >= Math.max(0, timePreempt - objectHittableRange);
    }

    private boolean isHit() {
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radiusSquared;
            if (GameHelper.isRelax() && passedTime - timePreempt >= 0 && inPosition) {
                return true;
            }

            var isPressed = listener.isMousePressed(this, i);
            if (isPressed && inPosition) {
                return true;
            } else if (GameHelper.isAutopilot() && isPressed) {
                return true;
            }
        }
        return false;
    }

    private double hitOffsetToPreviousFrame() {
        if (!Config.isFixFrameOffset()) {
            return 0;
        }

        // 因为这里是阻塞队列, 所以提前点的地方会影响判断
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radiusSquared;
            if (GameHelper.isRelax() && passedTime - timePreempt >= 0 && inPosition) {
                return 0;
            }

            var isPressed = listener.isMousePressed(this, i);
            if (isPressed && inPosition) {
                return listener.downFrameOffset(i);
            } else if (GameHelper.isAutopilot() && isPressed) {
                return 0;
            }
        }
        return 0;
    }

    private void playHitSamples() {
        listener.playHitSamples(hitSamples);
    }

    @Override
    public void update(final float dt) {
        if (beatmapCircle.hitWindow == null) {
            // Circle somehow does not have a judgement window - abandon.
            return;
        }

        // PassedTime < 0 means circle logic is over
        if (passedTime < 0) {
            removeFromScene();
            return;
        }

        float mehWindow = beatmapCircle.hitWindow.getMehWindow() / 1000;

        // If we have clicked circle
        if (replayObjectData != null) {
            if (passedTime - timePreempt + dt / 2 > replayObjectData.accuracy / 1000f) {
                listener.registerAccuracy(replayObjectData.accuracy / 1000f);
                passedTime = -1;
                // Remove circle and register hit in update thread
                listener.onCircleHit(id, replayObjectData.accuracy / 1000f, position,endsCombo, replayObjectData.result, comboColor);
                if (Math.abs(replayObjectData.accuracy / 1000f) <= mehWindow) {
                    playHitSamples();
                }
                removeFromScene();
                return;
            }
        } else {
            float frameHitOffset = (float) hitOffsetToPreviousFrame() / 1000;

            // dt is 0 here as the current time is updated *after* this judgement.
            if (canBeHit(0, frameHitOffset) && isHit()) {
                float signAcc = passedTime - timePreempt + frameHitOffset;
                listener.registerAccuracy(signAcc);
                passedTime = -1;
                // Remove circle and register hit in update thread
                startHit = true;
                listener.onCircleHit(id, signAcc, position, endsCombo, (byte) 0, comboColor);
                if (Math.abs(signAcc) <= mehWindow) {
                    playHitSamples();
                }
                removeFromScene();
                return;
            }
        }

        if (circlePiece.isVisible()) {
            if (GameHelper.isKiai()) {
                var kiaiModifier = (float) Math.max(0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()) * 0.5f;
                var r = Math.min(1, comboColor.getRed() + (1 - comboColor.getRed()) * kiaiModifier);
                var g = Math.min(1, comboColor.getGreen() + (1 - comboColor.getGreen()) * kiaiModifier);
                var b = Math.min(1, comboColor.getBlue() + (1 - comboColor.getBlue()) * kiaiModifier);
                kiai = true;
                circlePiece.setCircleColor(r, g, b);
            } else if (kiai) {
                circlePiece.setCircleColor(comboColor);
                kiai = false;
            }
        }

        passedTime += dt;

        // We are still at approach time. Let entity modifiers finish first.
        if (passedTime < timePreempt) {
            return;
        }

        if (autoPlay) {
            passedTime = -1;
            // Remove circle and register hit in update thread
            listener.onCircleHit(id, 0, position, endsCombo, ResultType.HIT300.getId(), comboColor);
            playHitSamples();
            removeFromScene();
        } else {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);

            // If passed too much time, counting it as miss
            if (passedTime > timePreempt + mehWindow) {
                passedTime = -1;
                final byte forcedScore = (replayObjectData == null) ? 0 : replayObjectData.result;

                removeFromScene();
                listener.onCircleHit(id, 10, position, false, forcedScore, comboColor);
            }
        }
    } // update(float dt)

    @Override
    public void tryHit(final float dt) {
        if (beatmapCircle.hitWindow == null) {
            return;
        }

        float frameHitOffset = (float) hitOffsetToPreviousFrame() / 1000;

        if (canBeHit(dt, frameHitOffset) && isHit()) {
            // At this point, the object's state is already in the next update tick.
            // However, hit judgements require the object's state to be in the previous tick.
            // Therefore, we subtract dt to get the object's state in the previous tick.
            float signAcc = passedTime - timePreempt - dt + frameHitOffset;
            listener.registerAccuracy(signAcc);
            passedTime = -1;
            // Remove circle and register hit in update thread
            listener.onCircleHit(id, signAcc, position, endsCombo, (byte) 0, comboColor);
            if (Math.abs(signAcc) <= beatmapCircle.hitWindow.getMehWindow() / 1000) {
                playHitSamples();
            }
            removeFromScene();
        }
    }

}
