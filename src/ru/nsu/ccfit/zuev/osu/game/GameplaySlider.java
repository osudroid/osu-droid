package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody;
import com.reco1l.osu.Execution;
import com.reco1l.andengine.sprite.AnimatedSprite;
import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Modifiers;
import com.reco1l.andengine.Anchor;
import com.reco1l.osu.hitobjects.SliderTickSprite;
import com.reco1l.osu.playfield.CirclePiece;
import com.reco1l.osu.playfield.NumberedCirclePiece;
import com.reco1l.osu.hitobjects.SliderTickContainer;
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick;
import com.rian.osu.gameplay.GameplayHitSampleInfo;
import com.rian.osu.gameplay.GameplaySequenceHitSampleInfo;
import com.rian.osu.math.Interpolation;
import com.rian.osu.mods.ModHidden;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.MathUtils;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

import java.util.BitSet;

public class GameplaySlider extends GameObject {

    private final ExtendedSprite approachCircle;
    private final ExtendedSprite startArrow, endArrow;
    private Slider beatmapSlider;
    private Scene scene;
    private GameObjectListener listener;
    private SliderPath path;
    private double elapsedSpanTime;
    private float timePreempt;
    private double duration;
    private double spanDuration;
    private int completedSpanCount;
    private boolean reverse;
    private final boolean isSliderBallFlip;

    private GameplayHitSampleInfo[][] nestedHitSamples;
    private final GameplaySequenceHitSampleInfo sliderSlideSample;
    private final GameplaySequenceHitSampleInfo sliderWhistleSample;

    private int currentNestedObjectIndex;
    private int ticksGot;
    private int currentTickSpriteIndex;

    private final ExtendedSprite followCircle;

    // Temporarily used PointF to avoid allocations
    private final PointF tmpPoint = new PointF();
    private float ballAngle;

    private boolean kiai;
    private final RGBColor bodyColor = new RGBColor();
    private final RGBColor circleColor = new RGBColor();

    //for replay
    private int firstHitAccuracy;
    private final BitSet tickSet = new BitSet();
    private int replayTickIndex;

    private LinePath superPath = null;
    private boolean preStageFinish = false;

    private final SliderBody sliderBody;

    /**
     * The absolute slider's path end position.
     * This already takes into account the absolute object's position.
     */
    private final PointF pathEndPosition = new PointF();

    /**
     * The slider ball sprite.
     */
    private final AnimatedSprite ball;

    /**
     * The start circle piece of the slider.
     */
    private final NumberedCirclePiece headCirclePiece;

    /**
     * The end circle piece of the slider.
     */
    private final CirclePiece tailCirclePiece;

    /**
     * The slider tick container.
     */
    private final SliderTickContainer tickContainer;

    /**
     * Whether the slider has ended (and all its spans).
     */
    private boolean isOver;

    /**
     * Whether the follow circle sprite is being animated.
     */
    private boolean isFollowCircleAnimating;

    /**
     * Whether the cursor is in the slider's radius.
     */
    private boolean isInRadius;


    public GameplaySlider() {

        headCirclePiece = new NumberedCirclePiece("sliderstartcircle", "sliderstartcircleoverlay");
        tailCirclePiece = new CirclePiece("sliderendcircle", "sliderendcircleoverlay");

        approachCircle = new ExtendedSprite();
        approachCircle.setOrigin(Anchor.Center);
        approachCircle.setTextureRegion(ResourceManager.getInstance().getTexture("approachcircle"));

        startArrow = new ExtendedSprite();
        startArrow.setOrigin(Anchor.Center);
        startArrow.setTextureRegion(ResourceManager.getInstance().getTexture("reversearrow"));

        endArrow = new ExtendedSprite();
        endArrow.setOrigin(Anchor.Center);
        endArrow.setTextureRegion(ResourceManager.getInstance().getTexture("reversearrow"));

        ball = new AnimatedSprite("sliderb", false, 60f);
        ball.setOrigin(Anchor.Center);

        // Avoid to use AnimatedSprite if not necessary.
        if (ResourceManager.getInstance().isTextureLoaded("sliderfollowcircle-0")) {
            followCircle = new AnimatedSprite("sliderfollowcircle", true, OsuSkin.get().getAnimationFramerate());
        } else {
            followCircle = new ExtendedSprite();
            followCircle.setTextureRegion(ResourceManager.getInstance().getTexture("sliderfollowcircle"));
        }
        followCircle.setOrigin(Anchor.Center);

        sliderBody = new SliderBody(OsuSkin.get().isSliderHintEnable());
        tickContainer = new SliderTickContainer();
        isSliderBallFlip = OsuSkin.get().isSliderBallFlip();

        sliderSlideSample = new GameplaySequenceHitSampleInfo();
        sliderWhistleSample = new GameplaySequenceHitSampleInfo();
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final Slider beatmapSlider, final float secPassed, final RGBColor comboColor,
                     final RGBColor borderColor, final SliderPath sliderPath, final LinePath renderPath) {
        this.listener = listener;
        this.scene = scene;
        this.beatmapSlider = beatmapSlider;

        var stackedPosition = beatmapSlider.getGameplayStackedPosition();
        position.set(stackedPosition.x, stackedPosition.y);

        endsCombo = beatmapSlider.isLastInCombo();
        elapsedSpanTime = secPassed - beatmapSlider.startTime / 1000;
        duration = beatmapSlider.getDuration() / 1000;
        spanDuration = beatmapSlider.getSpanDuration() / 1000;
        path = sliderPath;

        reloadHitSounds();

        float scale = beatmapSlider.getGameplayScale();

        isOver = false;
        isFollowCircleAnimating = false;
        isInRadius = false;

        reverse = false;
        startHit = false;
        ticksGot = 0;
        completedSpanCount = 0;
        currentTickSpriteIndex = 0;
        replayTickIndex = 0;
        firstHitAccuracy = 0;
        tickSet.clear();
        kiai = GameHelper.isKiai();
        preStageFinish = false;
        bodyColor.set(comboColor.r(), comboColor.g(), comboColor.b());
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            var sliderBodyColor = OsuSkin.get().getSliderBodyColor();
            bodyColor.set(sliderBodyColor.r(), sliderBodyColor.g(), sliderBodyColor.b());
        }
        circleColor.set(comboColor.r(), comboColor.g(), comboColor.b());
        currentNestedObjectIndex = 0;

        // Start circle piece
        headCirclePiece.setScale(scale);
        headCirclePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
        headCirclePiece.setAlpha(0);
        headCirclePiece.setPosition(this.position.x, this.position.y);
        int comboNum = beatmapSlider.getIndexInCurrentCombo() + 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            comboNum %= 10;
        }
        headCirclePiece.setNumberText(comboNum);
        headCirclePiece.setNumberScale(OsuSkin.get().getComboTextScale());

        approachCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        approachCircle.setScale(scale * 3);
        approachCircle.setAlpha(0);
        approachCircle.setPosition(this.position.x, this.position.y);

        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && beatmapSlider.isFirstNote());
        }

        // End circle
        pathEndPosition.set(getAbsolutePathPosition(path.anchorCount - 1));

        tailCirclePiece.setScale(scale);
        tailCirclePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
        tailCirclePiece.setAlpha(0);

        if (Config.isSnakingInSliders()) {
            tailCirclePiece.setPosition(this.position.x, this.position.y);
        } else {
            tailCirclePiece.setPosition(pathEndPosition.x, pathEndPosition.y);
        }

        // Repeat arrow at start
        int spanCount = beatmapSlider.getSpanCount();
        if (spanCount > 2) {
            startArrow.setAlpha(0);
            startArrow.setScale(scale);
            startArrow.setPosition(this.position.x, this.position.y);

            PointF nextPoint = getAbsolutePathPosition(1);
            startArrow.setRotation(MathUtils.radToDeg(Utils.direction(position.x, position.y, nextPoint.x, nextPoint.y)));

            scene.attachChild(startArrow, 0);
        }

        timePreempt = (float) beatmapSlider.timePreempt / 1000;
        float fadeInDuration = (float) beatmapSlider.timeFadeIn / 1000;

        // When snaking in is enabled, the first repeat or tail needs to be delayed until the snaking completes.
        float fadeInDelay = Config.isSnakingInSliders() ? timePreempt / 3 : 0;

        if (GameHelper.isHidden()) {
            float fadeOutDuration = timePreempt * (float) ModHidden.FADE_OUT_DURATION_MULTIPLIER;
            float finalTailAlpha = (fadeInDuration - fadeInDelay) / fadeInDuration;

            headCirclePiece.registerEntityModifier(Modifiers.sequence(
                    Modifiers.fadeIn(fadeInDuration),
                    Modifiers.fadeOut(fadeOutDuration)
            ));

            tailCirclePiece.registerEntityModifier(Modifiers.sequence(
                    Modifiers.delay(fadeInDelay),
                    Modifiers.alpha(fadeInDuration - fadeInDelay, 0, finalTailAlpha),
                    Modifiers.alpha(fadeOutDuration, finalTailAlpha, 0)
            ));

        } else {
            headCirclePiece.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));

            tailCirclePiece.registerEntityModifier(Modifiers.sequence(
                    Modifiers.delay(fadeInDelay),
                    Modifiers.fadeIn(fadeInDuration)
            ));
        }

        if (approachCircle.isVisible()) {
            approachCircle.registerEntityModifier(Modifiers.alpha(Math.min(fadeInDuration * 2, timePreempt), 0, 0.9f));
            approachCircle.registerEntityModifier(Modifiers.scale(timePreempt, scale * 3, scale));
        }

        scene.attachChild(headCirclePiece, 0);
        scene.attachChild(approachCircle);
        // Repeat arrow at end
        if (spanCount > 1) {
            endArrow.setAlpha(0);
            endArrow.setScale(scale);

            PointF previousPoint = getAbsolutePathPosition(path.anchorCount - 2);
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(pathEndPosition.x, pathEndPosition.y, previousPoint.x, previousPoint.y)));

            if (Config.isSnakingInSliders()) {
                endArrow.setPosition(this.position.x, this.position.y);
            } else {
                endArrow.setPosition(pathEndPosition.x, pathEndPosition.y);
            }

            endArrow.registerEntityModifier(Modifiers.sequence(
                    Modifiers.delay(fadeInDelay),
                    Modifiers.fadeIn(fadeInDuration)
            ));

            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(tailCirclePiece, 0);

        // Slider track
        superPath = renderPath;
        sliderBody.init(superPath, Config.isSnakingInSliders(), stackedPosition);
        sliderBody.setBackgroundWidth(OsuSkin.get().getSliderBodyWidth() * scale);
        sliderBody.setBackgroundColor(bodyColor.r(), bodyColor.g(), bodyColor.b(), OsuSkin.get().getSliderBodyBaseAlpha());
        sliderBody.setBorderWidth(OsuSkin.get().getSliderBorderWidth() * scale);
        sliderBody.setBorderColor(borderColor.r(), borderColor.g(), borderColor.b());

        if (OsuSkin.get().isSliderHintEnable() && beatmapSlider.getDistance() > OsuSkin.get().getSliderHintShowMinLength()) {
            sliderBody.setHintVisible(true);
            sliderBody.setHintWidth(OsuSkin.get().getSliderHintWidth() * scale);

            RGBColor hintColor = OsuSkin.get().getSliderHintColor();
            if (hintColor != null) {
                sliderBody.setHintColor(hintColor.r(), hintColor.g(), hintColor.b(), OsuSkin.get().getSliderHintAlpha());
            } else {
                sliderBody.setHintColor(bodyColor.r(), bodyColor.g(), bodyColor.b(), OsuSkin.get().getSliderHintAlpha());
            }
        } else {
            sliderBody.setHintVisible(false);
        }

        tickContainer.init(secPassed, beatmapSlider);

        scene.attachChild(tickContainer, 0);
        scene.attachChild(sliderBody, 0);

        if (Config.isDimHitObjects()) {

            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            var dimDelaySec = timePreempt - objectHittableRange;
            var colorDim = 195f / 255f;

            headCirclePiece.setColor(colorDim, colorDim, colorDim);
            headCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    headCirclePiece.getRed(), 1f,
                    headCirclePiece.getGreen(), 1f,
                    headCirclePiece.getBlue(), 1f
                )
            ));

            tailCirclePiece.setColor(colorDim, colorDim, colorDim);
            tailCirclePiece.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    tailCirclePiece.getRed(), 1f,
                    tailCirclePiece.getGreen(), 1f,
                    tailCirclePiece.getBlue(), 1f
                )
            ));

            endArrow.setColor(colorDim, colorDim, colorDim);
            endArrow.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    endArrow.getRed(), 1f,
                    endArrow.getGreen(), 1f,
                    endArrow.getBlue(), 1f
                )
            ));

            sliderBody.setColor(colorDim, colorDim, colorDim);
            sliderBody.registerEntityModifier(Modifiers.sequence(
                Modifiers.delay(dimDelaySec),
                Modifiers.color(0.1f,
                    sliderBody.getRed(), 1f,
                    sliderBody.getGreen(), 1f,
                    sliderBody.getBlue(), 1f
                )
            ));
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private PointF getPositionAt(final float percentage, final boolean updateBallAngle, final boolean updateEndArrowRotation) {
        if (path.anchorCount < 2) {
            tmpPoint.set(position);
            return tmpPoint;
        }

        if (percentage >= 1) {
            tmpPoint.set(pathEndPosition);
            return tmpPoint;
        }

        if (percentage <= 0) {
            PointF nextPoint = getAbsolutePathPosition(1);

            if (updateBallAngle) {
                ballAngle = MathUtils.radToDeg(Utils.direction(nextPoint.x, nextPoint.y, position.x, position.y));
            }

            if (updateEndArrowRotation) {
                endArrow.setRotation(MathUtils.radToDeg(Utils.direction(position.x, position.y, nextPoint.x, nextPoint.y)));
            }

            tmpPoint.set(position);
            return tmpPoint;
        }

        // Directly taken from library-owned SliderPath
        int left = 0;
        int right = path.anchorCount - 2;
        float currentLength = percentage * path.getLength(path.anchorCount - 1);

        while (left <= right) {
            int pivot = left + ((right - left) >> 1);
            float length = path.getLength(pivot);

            if (length < currentLength) {
                left = pivot + 1;
            } else if (length > currentLength) {
                right = pivot - 1;
            } else {
                break;
            }
        }

        int index = left - 1;

        // Theoretically, this case should never be reached as it means the percentage would be less than 0
        // (which is already covered by the case above). However, people seem to be having IndexOutOfBoundsException
        // crashes here, so we'll just return the first point in the path.
        if (index < 0) {
            var nextPoint = getAbsolutePathPosition(1);

            if (updateBallAngle) {
                ballAngle = MathUtils.radToDeg(Utils.direction(position.x, position.y, nextPoint.x, nextPoint.y));
            }

            if (updateEndArrowRotation) {
                endArrow.setRotation(MathUtils.radToDeg(Utils.direction(nextPoint.x, nextPoint.y, position.x, position.y)));
            }

            tmpPoint.set(position);
            return tmpPoint;
        }

        float lengthProgress = (currentLength - path.getLength(index)) / (path.getLength(index + 1) - path.getLength(index));

        PointF currentPoint = getAbsolutePathPosition(index);
        var currentPointX = currentPoint.x;
        var currentPointY = currentPoint.y;

        PointF nextPoint = getAbsolutePathPosition(index + 1);
        var nextPointX = nextPoint.x;
        var nextPointY = nextPoint.y;

        tmpPoint.set(
            Interpolation.linear(currentPointX, nextPointX, lengthProgress),
            Interpolation.linear(currentPointY, nextPointY, lengthProgress)
        );

        if (updateBallAngle) {
            ballAngle = MathUtils.radToDeg(Utils.direction(currentPointX, currentPointY, nextPointX, nextPointY));
        }

        if (updateEndArrowRotation) {
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(nextPointX, nextPointY, currentPointX, currentPointY)));
        }

        return tmpPoint;
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }

        if (GameHelper.isHidden()) {
            sliderBody.detachSelf();

            // If the animation is enabled, at this point it will be still animating.
            if (!Config.isAnimateFollowCircle() || !isFollowCircleAnimating) {
                poolObject();
            }
        } else {
            sliderBody.registerEntityModifier(Modifiers.fadeOut(0.24f, e -> {
                Execution.updateThread(() -> {
                    sliderBody.detachSelf();

                    // We can pool the hit object once all animations are finished.
                    // The slider body is the last object to finish animating.
                    poolObject();
                });
            }));
        }

        ball.registerEntityModifier(Modifiers.fadeOut(0.1f, e -> {
            Execution.updateThread(ball::detachSelf);
        }));

        // Follow circle might still be animating when the slider is removed from the scene.
        if (!Config.isAnimateFollowCircle() || !isFollowCircleAnimating) {
            followCircle.detachSelf();
        }

        headCirclePiece.detachSelf();
        tailCirclePiece.detachSelf();
        approachCircle.detachSelf();
        startArrow.detachSelf();
        endArrow.detachSelf();
        tickContainer.detachSelf();

        listener.removeObject(this);
        stopSlidingSamples();

        for (int i = 0; i < nestedHitSamples.length; ++i) {
            for (int j = 0; j < nestedHitSamples[i].length; ++j) {
                nestedHitSamples[i][j].reset();
                GameplayHitSampleInfo.pool.free(nestedHitSamples[i][j]);
            }
        }

        nestedHitSamples = null;
        path = null;
        scene = null;
    }

    public void poolObject() {

        headCirclePiece.clearEntityModifiers();
        tailCirclePiece.clearEntityModifiers();

        startArrow.clearEntityModifiers();
        endArrow.clearEntityModifiers();
        approachCircle.clearEntityModifiers();
        followCircle.clearEntityModifiers();
        ball.clearEntityModifiers();
        sliderBody.clearEntityModifiers();
        tickContainer.clearEntityModifiers();

        GameObjectPool.getInstance().putSlider(this);
    }

    private void onSpanFinish() {
        var hitWindow = beatmapSlider.getHead().hitWindow;
        if (hitWindow == null) {
            return;
        }

        ++completedSpanCount;

        int totalSpanCount = beatmapSlider.getSpanCount();
        int remainingSpans = totalSpanCount - completedSpanCount;
        boolean stillHasSpan = remainingSpans > 0;
        boolean isTracking = isTracking();

        // If slider was in reverse mode, we should swap start and end points
        var spanEndJudgementPosition = reverse ? position : pathEndPosition;

        // Do not judge slider repeats if the slider head has not been hit.
        if (startHit) {
            if (isTracking) {
                playCurrentNestedObjectHitSound();
                ticksGot++;
                tickSet.set(replayTickIndex++, true);
            } else {
                tickSet.set(replayTickIndex++, false);
            }

            if (stillHasSpan) {
                listener.onSliderHit(id, isTracking ? 30 : -1, spanEndJudgementPosition, false,
                        bodyColor, GameObjectListener.SLIDER_REPEAT, isTracking);
            }

            currentNestedObjectIndex++;
        }

        // If slider has more spans
        if (stillHasSpan) {
            reverse = !reverse;
            elapsedSpanTime -= spanDuration;

            if (isSliderBallFlip) {
                ball.setFlippedHorizontal(reverse);
            }

            // Restore ticks
            tickContainer.onNewSpan(getGameplayPassedTimeMilliseconds() / 1000, completedSpanCount);
            currentTickSpriteIndex = reverse ? tickContainer.getChildCount() - 1 : 0;

            // Setting visibility of repeat arrows
            if (reverse) {
                if (remainingSpans <= 2) {
                    endArrow.setAlpha(0);
                    tailCirclePiece.setAlpha(0);
                }

                if (remainingSpans > 1) {
                    startArrow.setAlpha(1);
                }
            } else if (remainingSpans <= 2) {
                startArrow.setAlpha(0);
                headCirclePiece.setAlpha(0);
            }

            ((GameScene) listener).onSliderReverse(
                    spanEndJudgementPosition,
                    reverse ? endArrow.getRotation() : startArrow.getRotation(),
                    bodyColor);

            if (elapsedSpanTime >= spanDuration) {
                // This condition can happen under low frame rate and/or short span duration, which will cause all
                // slider tick judgements in this span to be skipped. Ensure that all slider ticks in the current
                // span has been judged before proceeding to the next span.
                judgeSliderTicks();

                onSpanFinish();
            }

            return;
        }
        isOver = true;

        if (!startHit) {
            // Slider head was never hit - miss the entire slider before the end.
            // Add 0.013s to maintain pre-version 1.8 behavior where the slider head is judged 13ms after the 50 hit window in this case.
            onSliderHeadHit(hitWindow.getMehWindow() / 1000 + 0.013);
        }

        // Calculating score
        int score = 0;

        if (replayObjectData == null) {
            int firstHitScore = 0;

            if (GameHelper.isScoreV2()) {
                // If ScoreV2 is active, the accuracy of hitting the slider head is additionally accounted for when judging the entire slider:
                // Getting a 300 for a slider requires getting a 300 judgement for the slider head.
                // Getting a 100 for a slider requires getting a 100 judgement or better for the slider head.
                if (Math.abs(firstHitAccuracy) <= hitWindow.getGreatWindow()) {
                    firstHitScore = 300;
                } else if (Math.abs(firstHitAccuracy) <= hitWindow.getOkWindow()) {
                    firstHitScore = 100;
                }
            }

            int totalTicks = beatmapSlider.getNestedHitObjects().size();

            if (ticksGot > 0) {
                score = 50;
            }

            if (ticksGot >= totalTicks / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
                score = 100;
            }

            if (ticksGot >= totalTicks && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
                score = 300;
            }
        } else if (replayObjectData.result == ResultType.HIT300.getId()) {
            score = 300;
        } else if (replayObjectData.result == ResultType.HIT100.getId()) {
            score = 100;
        } else if (replayObjectData.result == ResultType.HIT50.getId()) {
            score = 50;
        }

        // In replays older than version 6, slider ends always give combo even when not being tracked.
        boolean awardCombo = isTracking || (replayObjectData != null && GameHelper.getReplayVersion() < 6);

        listener.onSliderHit(id, score, spanEndJudgementPosition, endsCombo, bodyColor,
            GameObjectListener.SLIDER_END, awardCombo);

        listener.onSliderEnd(id, firstHitAccuracy, tickSet);

        // Remove slider from scene
        if (Config.isAnimateFollowCircle() && isInRadius) {
            isFollowCircleAnimating = true;

            followCircle.clearEntityModifiers();
            followCircle.registerEntityModifier(Modifiers.scale(0.2f, followCircle.getScaleX(), followCircle.getScaleX() * 0.8f, null, Easing.OutQuad));
            followCircle.registerEntityModifier(Modifiers.alpha(0.2f, followCircle.getAlpha(), 0f, e -> {
                Execution.updateThread(() -> {
                    followCircle.detachSelf();

                    // When hidden mod is enabled, the follow circle is the last object to finish animating.
                    if (GameHelper.isHidden()) {
                        poolObject();
                    }
                });
                isFollowCircleAnimating = false;
            }));
        }

        removeFromScene();
    }

    private boolean canBeHit(float dt, float frameOffset) {
        // At this point, the object's state is already in the next update tick.
        // However, hit judgements require the object's state to be in the previous tick.
        // Therefore, we subtract dt to get the object's state in the previous tick.
        return elapsedSpanTime - dt + frameOffset >= -objectHittableRange;
    }

    private boolean isHit() {
        float radius = Utils.sqr((float) beatmapSlider.getGameplayRadius());
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radius;
            if (GameHelper.isRelaxMod() && elapsedSpanTime >= 0 && inPosition) {
                return true;
            }

            var isPressed = listener.isMousePressed(this, i);
            if (isPressed && inPosition) {
                return true;
            } else if (GameHelper.isAutopilotMod() && isPressed) {
                return true;
            }
        }
        return false;
    }

    private double hitOffsetToPreviousFrame() {
        if (!Config.isFixFrameOffset()) {
            return 0;
        }

        // Hit judgement is done in the update thread, but inputs are received in the main thread,
        // the time when the player clicked in advance will affect judgement. This offset is used
        // to offset the hit from the previous update tick.
        float radius = Utils.sqr((float) beatmapSlider.getGameplayRadius());
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radius;
            if (GameHelper.isRelaxMod() && elapsedSpanTime >= 0 && inPosition) {
                return 0;
            }

            var isPressed = listener.isMousePressed(this, i);
            if (isPressed && inPosition) {
                return listener.downFrameOffset(i);
            } else if (GameHelper.isAutopilotMod() && isPressed) {
                return 0;
            }
        }
        return 0;
    }


    @Override
    public void update(final float dt) {

        if (scene == null) {
            return;
        }
        elapsedSpanTime += dt;

        // If the slider head is not judged yet
        if (!startHit) {
            float frameOffset = (float) hitOffsetToPreviousFrame() / 1000;

            if (!autoPlay && canBeHit(dt, frameOffset) && isHit()) {
                // At this point, the object's state is already in the next update tick.
                // However, hit judgements require the object's state to be in the previous tick.
                // Therefore, we subtract dt to get the object's state in the previous tick.
                onSliderHeadHit(elapsedSpanTime - dt + frameOffset);
            } else if (!autoPlay && elapsedSpanTime > getLateHitThreshold()) {
                // If it's too late, mark this hit missing.
                onSliderHeadHit(elapsedSpanTime);
            } else if (autoPlay && elapsedSpanTime >= 0) {
                onSliderHeadHit(0);
            } else if (replayObjectData != null && elapsedSpanTime + dt / 2 > replayObjectData.accuracy / 1000d) {
                onSliderHeadHit(replayObjectData.accuracy / 1000d);
            }
        }

        if (GameHelper.isKiai()) {
            var kiaiModifier = (float) Math.max(0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()) * 0.5f;
            var r = Math.min(1, circleColor.r() + (1 - circleColor.r()) * kiaiModifier);
            var g = Math.min(1, circleColor.g() + (1 - circleColor.g()) * kiaiModifier);
            var b = Math.min(1, circleColor.b() + (1 - circleColor.b()) * kiaiModifier);
            kiai = true;
            headCirclePiece.setCircleColor(r, g, b);
        } else if (kiai) {
            headCirclePiece.setCircleColor(circleColor.r(), circleColor.g(), circleColor.b());
            kiai = false;
        }

        if (elapsedSpanTime < 0) // we at approach time
        {
            if (startHit) {
                // Hide the approach circle if the slider is already hit.
                approachCircle.clearEntityModifiers();
                approachCircle.setAlpha(0);
            }

            if (Config.isSnakingInSliders()) {
                float percentage = FMath.clamp((float) (timePreempt + elapsedSpanTime) / (timePreempt / 3), 0, 1);

                if (percentage < 1) {
                    if (superPath != null && sliderBody != null) {
                        float l = superPath.getMeasurer().maxLength() * percentage;

                        sliderBody.setEndLength(l);
                    }

                    var position = getPositionAt(percentage, false, true);

                    tailCirclePiece.setPosition(position.x, position.y);
                    endArrow.setPosition(position.x, position.y);
                } else {
                    if (!preStageFinish && superPath != null && sliderBody != null) {
                        sliderBody.setEndLength(superPath.getMeasurer().maxLength());
                        preStageFinish = true;
                    }

                    if (path.anchorCount >= 2) {
                        PointF lastPoint = getAbsolutePathPosition(path.anchorCount - 2);
                        endArrow.setRotation(MathUtils.radToDeg(Utils.direction(pathEndPosition.x, pathEndPosition.y, lastPoint.x, lastPoint.y)));
                    }

                    tailCirclePiece.setPosition(pathEndPosition.x, pathEndPosition.y);
                    endArrow.setPosition(pathEndPosition.x, pathEndPosition.y);
                }
            }
            return;
        }

        sliderSlideSample.update(dt);
        sliderWhistleSample.update(dt);
        headCirclePiece.setAlpha(0f);

        float scale = beatmapSlider.getGameplayScale();

        if (!ball.hasParent()) {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);

            ball.setFrameTime(1f / ((float) beatmapSlider.getVelocity() * Slider.BASE_SCORING_DISTANCE * scale));
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);
            ball.registerEntityModifier(Modifiers.fadeIn(0.1f));

            followCircle.setAlpha(0);
            if (!Config.isAnimateFollowCircle()) {
                followCircle.setScale(scale);
            }

            scene.attachChild(ball);
            scene.attachChild(followCircle);
        }

        final float percentage = FMath.clamp((float) (elapsedSpanTime / spanDuration), 0, 1);
        final float bodyProgress = reverse ? 1 - percentage : percentage;

        if (Config.isSnakingOutSliders() && completedSpanCount == beatmapSlider.getSpanCount() - 1) {
            float length = bodyProgress * superPath.getMeasurer().maxLength();

            if (reverse) {
                // In reverse, the snaking out animation starts from the end node.
                sliderBody.setEndLength(length);
            } else {
                sliderBody.setStartLength(length);
            }
        }

        // Ball position
        var ballPos = getPositionAt(bodyProgress, true, false);
        boolean isTracking = isCursorInFollowArea(ballPos, isInRadius);
        listener.onTrackingSliders(isTracking);
        updateTracking(isTracking);

        judgeSliderTicks();

        // Setting position of ball and follow circle
        followCircle.setPosition(ballPos.x, ballPos.y);
        ball.setPosition(ballPos.x, ballPos.y);
        ball.setRotation(ballAngle);

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            listener.updateAutoBasedPos(ballPos.x, ballPos.y);
        }

        // If we got 100% time, finishing slider
        if (percentage >= 1) {
            onSpanFinish();
        }
    }

    private float getTrackingDistanceThresholdSquared(boolean isTracking) {
        float radius = (float) beatmapSlider.getGameplayRadius();
        float distanceThresholdSquared = radius * radius;

        if (isTracking) {
            // Multiply by 4 as the follow circle radius is 2 times larger than the object radius.
            distanceThresholdSquared *= 4;
        }

        return distanceThresholdSquared;
    }

    private void onSliderHeadHit(double hitOffset) {
        // Reference: https://github.com/ppy/osu/blob/bca42e9d24f1b8e433f63db8dbf5d36d8b811b36/osu.Game.Rulesets.Osu/Objects/Drawables/SliderInputManager.cs#L78
        // The reference does not fully represent the cases below as they are mixed with replay handling.
        var hitWindow = beatmapSlider.getHead().hitWindow;

        if (startHit || hitWindow == null) {
            return;
        }

        startHit = true;

        // Override hit offset with replay data when available.
        firstHitAccuracy = replayObjectData != null ? replayObjectData.accuracy : (int) (hitOffset * 1000);
        float mehWindow = hitWindow.getMehWindow() / 1000;

        if (replayObjectData == null || GameHelper.getReplayVersion() >= 6 || mehWindow <= duration) {
            if (-mehWindow <= hitOffset && hitOffset <= mehWindow) {
                listener.registerAccuracy(hitOffset);
                playCurrentNestedObjectHitSound();
                ticksGot++;
                listener.onSliderHit(id, 30, position,
                        false, bodyColor, GameObjectListener.SLIDER_START, true);
            } else {
                listener.onSliderHit(id, -1, position,
                        false, bodyColor, GameObjectListener.SLIDER_START, false);
            }
        } else if (hitOffset <= duration) {
            // In replays older than version 6, when the 50 hit window is longer than the duration of the slider,
            // the slider head is considered to *not* exist if it was not hit until the slider is over.
            // It is a very weird behavior, but that's what it actually was...
            listener.registerAccuracy(hitOffset);
            playCurrentNestedObjectHitSound();
            ticksGot++;
            listener.onSliderHit(id, 30, position,
                    false, bodyColor, GameObjectListener.SLIDER_START, true);
        }

        currentNestedObjectIndex++;

        // When the head is hit late:
        // - If the cursor has at all times been within range of the expanded follow area, hit all nested objects that have been passed through.
        // - If the cursor has at some point left the expanded follow area, miss those nested objects instead.

        // Get current ball position.
        float progress = (float) FMath.clamp(elapsedSpanTime / spanDuration, 0, 1);
        var ballPos = getPositionAt(reverse ? 1 - progress : progress, false, false);

        float distanceTrackingThresholdSquared = getTrackingDistanceThresholdSquared(true);

        // Check whether the cursor is within the follow area.
        boolean isTracking = isCursorInFollowArea(ballPos, true);

        double currentTime = getGameplayPassedTimeMilliseconds();
        boolean allTicksInRange = false;

        var nestedObjects = beatmapSlider.getNestedHitObjects();

        // Replays force their hit results per nested object, so we do not need to check for tracking here.
        if (isTracking && replayObjectData == null) {
            allTicksInRange = true;

            // Do not judge the slider end as it will be judged in onSpanFinish.
            for (int i = 1; i < nestedObjects.size() - 1; ++i) {
                var nestedObject = nestedObjects.get(i);

                // Stop the process when a nested object that can't be hit before the current time is reached.
                if (nestedObject.startTime > currentTime) {
                    break;
                }

                // When the first nested object that is further outside the follow area is reached,
                // forcefully miss all other nested objects that would otherwise be valid to be hit.
                // This covers a case of a slider overlapping itself that requires tracking to a tick on an outer edge.
                var nestedPosition = nestedObject.getGameplayStackedPosition();
                var distanceSquared = Utils.squaredDistance(nestedPosition.x, nestedPosition.y, ballPos.x, ballPos.y);

                if (distanceSquared > distanceTrackingThresholdSquared) {
                    allTicksInRange = false;
                    break;
                }
            }
        }

        // Reset span count completion counter to properly account for judged nested objects.
        completedSpanCount = 0;

        // Do not judge the slider end as it will be judged in onSpanFinish.
        for (int i = 1; i < nestedObjects.size() - 1; ++i) {
            var nestedObject = nestedObjects.get(i);

            // Stop the process when a nested object that can't be hit before the current time is reached.
            if (nestedObject.startTime > currentTime) {
                break;
            }

            var nestedPosition = nestedObject.getGameplayStackedPosition();
            tmpPoint.set(nestedPosition.x, nestedPosition.y);

            boolean isSliderTick = nestedObject instanceof SliderTick;
            boolean isHit = allTicksInRange || (replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex));
            int type = isSliderTick ? GameObjectListener.SLIDER_TICK : GameObjectListener.SLIDER_REPEAT;

            if (isHit) {
                playCurrentNestedObjectHitSound();
                ticksGot++;
                tickSet.set(replayTickIndex++, true);
                listener.onSliderHit(id, isSliderTick ? 10 : 30, tmpPoint, false, bodyColor, type, true);
            } else {
                tickSet.set(replayTickIndex++, false);
                listener.onSliderHit(id, -1, tmpPoint, false, bodyColor, type, false);
            }

            if (!isSliderTick) {
                // When a repeat is encountered, one span has passed.
                ++completedSpanCount;
            }

            currentNestedObjectIndex++;
        }

        // If all ticks were hit so far, enable tracking the full extent.
        // If any ticks were missed, assume tracking would've broken at some point, and should only activate if the cursor is within the slider ball.
        // For the second case, this may be the last chance we have to enable tracking before other objects get judged, otherwise the same would normally happen via Update().
        updateTracking(allTicksInRange || isCursorInFollowArea(ballPos, false));
    }

    private boolean isCursorInFollowArea(PointF ballPosition, boolean isTracking) {
        if (autoPlay) {
            return true;
        }

        // Calculating if cursor in follow circle bounds
        float trackingDistanceThresholdSquared = getTrackingDistanceThresholdSquared(isTracking);

        for (int i = 0, cursorCount = listener.getCursorsCount(); i < cursorCount; i++) {
            var isPressed = listener.isMouseDown(i);

            if (GameHelper.isAutopilotMod() && isPressed) {
                return true;
            }

            float distanceSquared = Utils.squaredDistance(listener.getMousePos(i), ballPosition);

            if (isPressed && distanceSquared <= trackingDistanceThresholdSquared) {
                return true;
            }
        }

        return false;
    }

    private void updateTracking(boolean isTracking) {
        float scale = beatmapSlider.getGameplayScale();

        if (Config.isAnimateFollowCircle()) {
            float remainTime = (float) (duration - elapsedSpanTime);

            if (isTracking && !isInRadius) {
                isInRadius = true;
                isFollowCircleAnimating = true;
                playSlidingSamples();

                // If alpha doesn't equal 0 means that it has been into an animation before
                float initialScale = followCircle.getAlpha() == 0 ? scale * 0.5f : followCircle.getScaleX();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.alpha(Math.min(remainTime, 0.06f), followCircle.getAlpha(), 1f));
                followCircle.registerEntityModifier(Modifiers.scale(Math.min(remainTime, 0.18f), initialScale, scale, e -> {
                    isFollowCircleAnimating = false;
                }, Easing.OutQuad));
            } else if (!isTracking && isInRadius) {
                isInRadius = false;
                isFollowCircleAnimating = true;
                stopSlidingSamples();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.scale(0.1f, followCircle.getScaleX(), scale * 2f));
                followCircle.registerEntityModifier(Modifiers.alpha(0.1f, followCircle.getAlpha(), 0f, e -> {
                    if (isOver) {
                        Execution.updateThread(e::detachSelf);
                    }
                    isFollowCircleAnimating = false;
                }));
            }
        } else {
            if (isTracking && !isInRadius) {
                playSlidingSamples();
            } else if (!isTracking && isInRadius) {
                stopSlidingSamples();
            }

            isInRadius = isTracking;
            followCircle.setAlpha(isTracking ? 1 : 0);
        }
    }

    private float getLateHitThreshold() {
        var hitWindow = beatmapSlider.getHead().hitWindow;

        return hitWindow != null ? Math.min(hitWindow.getMehWindow() / 1000, (float) duration) : 0;
    }

    private double getGameplayPassedTimeMilliseconds() {
        return beatmapSlider.startTime + (completedSpanCount * spanDuration + elapsedSpanTime) * 1000;
    }

    private void judgeSliderTicks() {
        // Do not judge slider ticks until the slider head is hit.
        if (!startHit || tickContainer.getChildCount() == 0) {
            return;
        }

        float scale = beatmapSlider.getGameplayScale();

        var nestedObjects = beatmapSlider.getNestedHitObjects();
        var nestedObjectToJudge = nestedObjects.get(currentNestedObjectIndex);
        double currentTime = getGameplayPassedTimeMilliseconds();

        // Cap follow circle expand animation duration at the interval of each slider tick.
        float followCircleExpandDuration = Math.min((float) spanDuration / (tickContainer.getChildCount() + 1), 0.2f);

        while (nestedObjectToJudge instanceof SliderTick && currentTime >= nestedObjectToJudge.startTime) {
            boolean isTracking = isTracking();

            if (isTracking) {
                if (Config.isAnimateFollowCircle() && !isFollowCircleAnimating) {
                    followCircle.clearEntityModifiers();
                    followCircle.registerEntityModifier(Modifiers.scale(followCircleExpandDuration, scale * 1.1f, scale, null, Easing.OutQuad));
                }

                playCurrentNestedObjectHitSound();
                ticksGot++;
                tickSet.set(replayTickIndex++, true);
            } else {
                tickSet.set(replayTickIndex++, false);
            }

            var tickPosition = nestedObjectToJudge.getGameplayStackedPosition();
            tmpPoint.set(tickPosition.x, tickPosition.y);

            listener.onSliderHit(id, isTracking ? 10 : -1, tmpPoint, false, bodyColor, GameObjectListener.SLIDER_TICK, isTracking);
            currentNestedObjectIndex++;

            var tickSprite = (SliderTickSprite) tickContainer.getChild(currentTickSpriteIndex);
            tickSprite.onHit(isTracking);

            if (reverse && currentTickSpriteIndex > 0) {
                currentTickSpriteIndex--;
            } else if (!reverse && currentTickSpriteIndex < tickContainer.getChildCount() - 1) {
                currentTickSpriteIndex++;
            }

            nestedObjectToJudge = nestedObjects.get(currentNestedObjectIndex);
        }
    }

    private void applyBodyFadeAdjustments(float fadeInDuration) {

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float fadeOutDuration = (float) duration + timePreempt - fadeInDuration;

            sliderBody.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration, null, Easing.OutQuad)
            ));
        } else {
            sliderBody.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }
    }

    private void reloadHitSounds() {
        var nestedObjects = beatmapSlider.getNestedHitObjects();
        nestedHitSamples = new GameplayHitSampleInfo[nestedObjects.size()][];

        for (int i = 0; i < nestedHitSamples.length; ++i) {
            var nestedObjectSamples = nestedObjects.get(i).getSamples();
            nestedHitSamples[i] = new GameplayHitSampleInfo[nestedObjectSamples.size()];

            for (int j = 0; j < nestedHitSamples[i].length; ++j) {
                var gameplaySample = GameplayHitSampleInfo.pool.obtain();
                gameplaySample.init(nestedObjectSamples.get(j));

                if (GameHelper.isSamplesMatchPlaybackRate()) {
                    gameplaySample.setFrequency(GameHelper.getSpeedMultiplier());
                }

                nestedHitSamples[i][j] = gameplaySample;
            }
        }

        sliderSlideSample.reset();
        sliderWhistleSample.reset();

        float startTime = (float) beatmapSlider.startTime;

        for (int i = 0, size = beatmapSlider.getAuxiliarySamples().size(); i < size; ++i) {
            if (sliderSlideSample.isInitialized() && sliderWhistleSample.isInitialized()) {
                break;
            }

            var auxiliarySample = beatmapSlider.getAuxiliarySamples().get(i);
            var firstSample = auxiliarySample.get(0).getSecond();

            if (!(firstSample instanceof BankHitSampleInfo bankSample)) {
                continue;
            }

            if (bankSample.name.equals("sliderslide")) {
                sliderSlideSample.init(startTime, auxiliarySample);
            } else if (bankSample.name.equals("sliderwhistle")) {
                sliderWhistleSample.init(startTime, auxiliarySample);
            }
        }

        if (GameHelper.isSamplesMatchPlaybackRate()) {
            sliderSlideSample.setFrequency(GameHelper.getSpeedMultiplier());
            sliderWhistleSample.setFrequency(GameHelper.getSpeedMultiplier());
        }

        sliderSlideSample.setLooping(true);
        sliderWhistleSample.setLooping(true);
    }

    private void playCurrentNestedObjectHitSound() {
        var samples = nestedHitSamples[currentNestedObjectIndex];

        for (int i = 0; i < samples.length; ++i) {
            samples[i].play();
        }
    }

    @Override
    public void stopLoopingSamples() {
        sliderSlideSample.stopAll();
        sliderWhistleSample.stopAll();
    }

    private void playSlidingSamples() {
        sliderSlideSample.play();
        sliderWhistleSample.play();
    }

    private void stopSlidingSamples() {
        sliderSlideSample.stop();
        sliderWhistleSample.stop();
    }

    private boolean isTracking() {
        return isInRadius && replayObjectData == null || replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex);
    }

    @Override
    public void tryHit(final float dt) {
        if (startHit) {
            return;
        }

        float frameOffset = (float) hitOffsetToPreviousFrame() / 1000;

        if (canBeHit(dt, frameOffset) && isHit()) {
            // At this point, the object's state is already in the next update tick.
            // However, hit judgements require the object's state to be in the previous tick.
            // Therefore, we subtract dt to get the object's state in the previous tick.
            onSliderHeadHit(elapsedSpanTime - dt + frameOffset);
        }

        if (elapsedSpanTime < 0 && startHit) {
            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);
        }
    }


    /**
     * Gets the absolute position of a point on the path taking into account the slider's position.
     */
    private PointF getAbsolutePathPosition(int pathPointIndex) {
        tmpPoint.set(
            position.x + path.getX(pathPointIndex),
            position.y + path.getY(pathPointIndex)
        );
        return tmpPoint;
    }

}
