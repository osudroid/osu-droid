package ru.nsu.ccfit.zuev.osu.game;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.UIScene;
import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.Anchor;
import com.osudroid.ui.v2.game.NumberedCirclePiece;
import com.reco1l.framework.Color4;
import com.osudroid.beatmaps.HitWindow;
import com.osudroid.beatmaps.constants.HitObjectType;
import com.osudroid.beatmaps.hitobjects.HitCircle;
import com.osudroid.game.GameplayHitSampleInfo;
import com.osudroid.mods.ModHidden;

import java.util.ArrayList;

import kotlin.Unit;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameplayHitCircle extends GameObject {

    private final UISprite approachCircle;
    private Color4 comboColor = new Color4();
    private GameObjectListener listener;
    private UIScene scene;
    private HitCircle beatmapCircle;
    private float passedTime;
    private float timePreempt;
    private float hitOffset;
    private boolean kiai;
    private boolean successfulHit;
    private final ArrayList<GameplayHitSampleInfo> hitSamples = new ArrayList<>(5);

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

    public void init(final GameObjectListener listener, final UIScene pScene, final HitCircle beatmapCircle,
                     final Color4 comboColor) {
        // Storing parameters into fields
        this.beatmapCircle = beatmapCircle;
        replayObjectData = null;

        var stackedPosition = beatmapCircle.getScreenSpaceGameplayStackedPosition();
        position.set(stackedPosition.x, stackedPosition.y);

        endsCombo = beatmapCircle.isLastInCombo();
        this.listener = listener;
        scene = pScene;
        timePreempt = (float) beatmapCircle.timePreempt / 1000;

        float mehWindow = (float) beatmapCircle.hitWindow.getMehWindow() / 1000;
        hitOffset = mehWindow;

        hitTime = (float) beatmapCircle.startTime / 1000;
        passedTime = -timePreempt;
        startHit = false;
        successfulHit = false;
        kiai = GameHelper.isKiai();
        this.comboColor = comboColor;

        float initialModifierTime = hitTime - timePreempt;
        float scale = beatmapCircle.getScreenSpaceGameplayScale();
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

        circlePiece.setNumberText(comboNum);
        circlePiece.setNumberScale(OsuSkin.get().getComboTextScale());
        circlePiece.setVisible(!GameHelper.isTraceable() ||
                (Config.isShowFirstApproachCircle() && GameHelper.getTraceable().getFirstObject() == beatmapCircle));

        approachCircle.setColor(comboColor);
        approachCircle.setScale(scale * 3 * (float) (beatmapCircle.timePreempt / GameHelper.getOriginalTimePreempt()));
        approachCircle.setAlpha(0);
        approachCircle.setPosition(this.position.x, this.position.y);
        approachCircle.setVisible(!GameHelper.isHidden() ||
                (Config.isShowFirstApproachCircle() && GameHelper.getHidden().getFirstObject() == beatmapCircle));

        scene.attachChild(circlePiece, 0);
        scene.attachChild(approachCircle);

        boolean fadeOutCircle = GameHelper.isHidden() && !GameHelper.getHidden().isOnlyFadeApproachCircles();

        circlePiece.beginAbsoluteSequence(initialModifierTime, sequence -> {
            sequence.fadeIn(fadeInDuration);

            if (fadeOutCircle) {
                float fadeOutDuration = timePreempt * (float) ModHidden.FADE_OUT_DURATION_MULTIPLIER;
                sequence.then().fadeOut(fadeOutDuration);
            }

            return Unit.INSTANCE;
        });

        if (!fadeOutCircle && circlePiece.isVisible()) {
            float okWindow = (float) beatmapCircle.hitWindow.getOkWindow() / 1000;

            circlePiece.beginAbsoluteSequence(hitTime + okWindow, sequence -> {
                sequence.fadeOut(mehWindow - okWindow);

                return Unit.INSTANCE;
            });
        }

        if (approachCircle.isVisible()) {
            Easing easing;
            var approachDifferentMod = GameHelper.getApproachDifferent();

            if (approachDifferentMod != null) {
                approachCircle.setScale(scale * approachDifferentMod.getScale());
                easing = approachDifferentMod.getEasing();
            } else {
                easing = Easing.None;
            }

            approachCircle.beginAbsoluteSequence(initialModifierTime, sequence -> {
                sequence.fadeTo(0.9f, Math.min(fadeInDuration * 2, timePreempt))
                        .scaleTo(scale, timePreempt, easing)
                        .after(e -> e.setAlpha(0));

                return Unit.INSTANCE;
            });
        }

        if (Config.isDimHitObjects() && circlePiece.isVisible()) {
            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            var colorDim = 195f / 255f;

            circlePiece.setColor(colorDim, colorDim, colorDim);
            circlePiece.beginAbsoluteSequence(hitTime - (float) HitWindow.MISS_WINDOW / 1000, sequence -> {
                sequence.colorTo(1, 1, 1, 0.1f);

                return Unit.INSTANCE;
            });
        }

        // Initialize samples
        var parsedSamples = beatmapCircle.getSamples();
        hitSamples.ensureCapacity(parsedSamples.size());

        for (int i = 0, size = parsedSamples.size(); i < size; i++) {
            var gameplaySample = GameplayHitSampleInfo.pool.obtain();
            gameplaySample.init(parsedSamples.get(i));

            if (GameHelper.isSamplesMatchPlaybackRate()) {
                gameplaySample.setFrequency(GameHelper.getSpeedMultiplier());
            }

            hitSamples.add(gameplaySample);
        }

        setLifetimeEnd(Float.MAX_VALUE);
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }

        setLifetimeEnd(hitTime + hitOffset);

        for (int i = hitSamples.size() - 1; i >= 0; --i) {
            var sample = hitSamples.get(i);

            sample.reset();
            GameplayHitSampleInfo.pool.free(sample);

            hitSamples.remove(i);
        }

        circlePiece.clearEntityModifiers();
        approachCircle.clearEntityModifiers();
        approachCircle.detachSelf();

        if (successfulHit || !circlePiece.isVisible() || circlePiece.getAlpha() == 0) {
            circlePiece.detachSelf();
        } else {
            extendLifetime(circlePiece.fadeOut(0.1f).after(e -> Execution.updateThread(e::detachSelf)));
        }

        scene = null;
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

        if (isJudged()) {
            return;
        }

        passedTime = listener.getElapsedTime() - hitTime;

        double mehWindow = beatmapCircle.hitWindow.getMehWindow() / 1000;

        // If we have clicked circle
        if (replayObjectData != null) {
            if (passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                hitOffset = replayObjectData.accuracy / 1000f;
                listener.registerAccuracy(HitObjectType.Normal, hitOffset);
                startHit = true;
                successfulHit = Math.abs(hitOffset) <= mehWindow;
                // Remove circle and register hit in update thread
                listener.onCircleHit(id, hitOffset, position, endsCombo, replayObjectData.result, comboColor);
                if (successfulHit) {
                    playHitSamples();
                }
                removeFromScene();
                return;
            }
        } else if (!autoPlay && listener.isObjectHittable(this)) {
            var hittingCursor = getHittingCursor(listener, beatmapCircle, passedTime);

            if (hittingCursor != null) {
                hitOffset = (float) (hittingCursor.getHitTime() - beatmapCircle.startTime) / 1000;
                listener.registerAccuracy(HitObjectType.Normal, hitOffset);
                startHit = true;
                successfulHit = Math.abs(hitOffset) <= mehWindow;
                // Remove circle and register hit in update thread
                listener.onCircleHit(id, hitOffset, position, endsCombo, (byte) 0, comboColor);
                if (successfulHit) {
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

        // We are still at approach time. Let entity modifiers finish first.
        if (passedTime < 0) {
            return;
        }

        if (autoPlay) {
            // Remove circle and register hit in update thread
            hitOffset = 0;
            listener.registerAccuracy(HitObjectType.Normal, 0);
            listener.onCircleHit(id, 0, position, endsCombo, ResultType.HIT300.getId(), comboColor);
            startHit = true;
            successfulHit = true;
            playHitSamples();
            removeFromScene();
        } else {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(1 - FMath.clamp(passedTime / 0.05f, 0, 1));

            // If passed too much time, counting it as miss
            if (passedTime > mehWindow) {
                startHit = true;
                final byte forcedScore = (replayObjectData == null) ? 0 : replayObjectData.result;

                removeFromScene();
                listener.registerAccuracy(HitObjectType.Normal, mehWindow + 1);
                listener.onCircleHit(id, 10, position, false, forcedScore, comboColor);
            }
        }
    }

    @Override
    public void onExpire() {
        circlePiece.clearEntityModifiers();
        approachCircle.clearEntityModifiers();

        circlePiece.detachSelf();
        approachCircle.detachSelf();

        GameObjectPool.getInstance().putCircle(this);
    }

    @Override
    public boolean isJudged() {
        return startHit;
    }
}
