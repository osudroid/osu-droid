package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody2D;
import com.reco1l.framework.Pool;
import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick;
import com.rian.osu.beatmap.sections.BeatmapControlPoints;
import com.rian.osu.math.Interpolation;
import com.rian.osu.mods.ModHidden;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.MathUtils;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinManager;

import java.util.ArrayList;
import java.util.BitSet;

public class Slider extends GameObject {

    public static final Pool<Sprite> tickSpritePool = new Pool<>(
        pool -> new CentredSprite(0f, 0f, ResourceManager.getInstance().getTexture("sliderscorepoint"))
    );

    private final Sprite startCircle, endCircle;
    private final Sprite startOverlay, endOverlay;
    private final Sprite approachCircle;
    private final Sprite startArrow, endArrow;
    private final ArrayList<Sprite> tickSprites = new ArrayList<>();
    private com.rian.osu.beatmap.hitobject.Slider beatmapSlider;
    private PointF curveEndPos;
    private Scene scene;
    private GameObjectListener listener;
    private CircleNumber number;
    private SliderPath path;
    private double passedTime;
    private int completedSpanCount;
    private boolean reverse;

    private int currentNestedObjectIndex;
    private int ticksGot;
    private double tickTime;
    private double tickInterval;
    private int currentTickSpriteIndex;

    private final AnimSprite ball;
    private PointF ballPos;
    private final Sprite followCircle;

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

    private SliderBody2D abstractSliderBody = null;

    private boolean
            mIsOver,
            mIsAnimating,
            mWasInRadius;

    public Slider() {
        startCircle = new Sprite(0, 0, ResourceManager.getInstance().getTexture("sliderstartcircle"));
        endCircle = new Sprite(0, 0, ResourceManager.getInstance().getTexture("sliderendcircle"));
        startOverlay = new Sprite(0, 0, ResourceManager.getInstance().getTexture("sliderstartcircleoverlay"));
        endOverlay = new Sprite(0, 0, ResourceManager.getInstance().getTexture("sliderendcircleoverlay"));
        approachCircle = new Sprite(0, 0, ResourceManager.getInstance().getTexture("approachcircle"));
        startArrow = new Sprite(0, 0, ResourceManager.getInstance().getTexture("reversearrow"));
        endArrow = new Sprite(0, 0, ResourceManager.getInstance().getTexture("reversearrow"));

        int ballFrameCount = SkinManager.getFrames("sliderb");
        ball = new AnimSprite(0, 0, "sliderb", ballFrameCount, ballFrameCount);
        followCircle = new Sprite(0, 0, ResourceManager.getInstance().getTexture("sliderfollowcircle"));
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final com.rian.osu.beatmap.hitobject.Slider beatmapSlider, final float secPassed,
                     final RGBColor comboColor, final float tickRate, final BeatmapControlPoints controlPoints,
                     final boolean isFirstNote, SliderPath sliderPath) {
        this.listener = listener;
        this.scene = scene;
        this.beatmapSlider = beatmapSlider;
        this.pos = beatmapSlider.getGameplayStackedPosition().toPointF();
        endsCombo = beatmapSlider.getLastInCombo();
        passedTime = secPassed - (float) beatmapSlider.startTime / 1000;
        path = sliderPath;

        float scale = beatmapSlider.getGameplayScale();
        int comboNum = beatmapSlider.getIndexInCurrentCombo() + 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            comboNum %= 10;
        }
        number = GameObjectPool.getInstance().getNumber(comboNum);
        number.init(pos, scale);

        mIsOver = false;
        mIsAnimating = false;
        mWasInRadius = false;

        reverse = false;
        startHit = false;
        ticksGot = 0;
        tickTime = 0;
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

        startCircle.setScale(scale);
        startCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        startCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, startCircle);

        startOverlay.setScale(scale);
        startOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, startOverlay);

        approachCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        approachCircle.setScale(scale * 3);
        approachCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, approachCircle);
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && isFirstNote);
        }

        // End circle
        curveEndPos = path.points.get(path.points.size() - 1);
        endCircle.setScale(scale);
        endCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        endCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : curveEndPos, endCircle);

        endOverlay.setScale(scale);
        endOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : curveEndPos, endOverlay);

        scene.attachChild(startOverlay, 0);
        // Repeat arrow at start
        int spanCount = beatmapSlider.getSpanCount();
        if (spanCount > 2) {
            startArrow.setAlpha(0);
            startArrow.setScale(scale);
            startArrow.setRotation(MathUtils.radToDeg(Utils.direction(pos, path.points.get(1))));

            Utils.putSpriteAnchorCenter(pos, startArrow);
            scene.attachChild(startArrow, 0);
        }

        float fadeInDuration = (float) beatmapSlider.timeFadeIn / 1000 / GameHelper.getSpeedMultiplier();

        if (GameHelper.isHidden()) {
            float fadeOutDuration = (float) (beatmapSlider.timePreempt / 1000 * ModHidden.FADE_OUT_DURATION_MULTIPLIER) / GameHelper.getSpeedMultiplier();

            number.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

            startCircle.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

            startOverlay.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

            endCircle.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));

            endOverlay.registerEntityModifier(Modifiers.sequence(
                Modifiers.fadeIn(fadeInDuration),
                Modifiers.fadeOut(fadeOutDuration)
            ));
        } else {
            number.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            startCircle.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            startOverlay.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            endCircle.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            endOverlay.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }

        if (approachCircle.isVisible()) {
            float realTimePreempt = (float) beatmapSlider.timePreempt / 1000 / GameHelper.getSpeedMultiplier();

            approachCircle.registerEntityModifier(Modifiers.alpha(Math.min(fadeInDuration * 2, realTimePreempt), 0, 0.9f));
            approachCircle.registerEntityModifier(Modifiers.scale(realTimePreempt, scale * 3, scale));
        }

        scene.attachChild(number, 0);
        scene.attachChild(startCircle, 0);
        scene.attachChild(approachCircle);
        scene.attachChild(endOverlay, 0);
        // Repeat arrow at end
        if (spanCount > 1) {
            endArrow.setAlpha(0);
            endArrow.setScale(scale);
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(curveEndPos, path.points.get(path.points.size() - 2))));

            Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : curveEndPos, endArrow);
            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(endCircle, 0);

        var timingControlPoint = controlPoints.timing.controlPointAt(beatmapSlider.startTime);
        tickInterval = timingControlPoint.msPerBeat / 1000 / tickRate;
        tickSprites.clear();

        for (int i = 1; i < beatmapSlider.getNestedHitObjects().size(); ++i) {
            var obj = beatmapSlider.getNestedHitObjects().get(i);

            if (!(obj instanceof SliderTick tick)) {
                break;
            }

            var tickPosition = tick.getGameplayStackedPosition();
            var tickSprite = tickSpritePool.obtain();

            tickSprite.setPosition(tickPosition.x, tickPosition.y);
            tickSprite.setScale(scale);
            tickSprite.setAlpha(0);
            tickSprites.add(tickSprite);
            scene.attachChild(tickSprite, 0);
        }

        // Slider track
        if (!path.points.isEmpty()) {
            superPath = new LinePath();
            for (int i = 0, size = path.points.size(); i < size; ++i) {
                var p = path.points.get(i);
                superPath.add(new Vec2(p.x, p.y));
            }
            superPath.measure();
            superPath.bufferLength(path.length.get(path.length.size() - 1));
            superPath = superPath.fitToLinePath();
            superPath.measure();

            var bodyWidth = (OsuSkin.get().getSliderBodyWidth() - OsuSkin.get().getSliderBorderWidth()) * scale;
            abstractSliderBody = new SliderBody2D(superPath);
            abstractSliderBody.setBodyWidth(bodyWidth);
            abstractSliderBody.setBorderWidth(OsuSkin.get().getSliderBodyWidth() * scale);
            abstractSliderBody.setSliderBodyBaseAlpha(OsuSkin.get().getSliderBodyBaseAlpha());

            if (OsuSkin.get().isSliderHintEnable() && beatmapSlider.getDistance() > OsuSkin.get().getSliderHintShowMinLength()) {
                abstractSliderBody.setEnableHint(true);
                abstractSliderBody.setHintAlpha(OsuSkin.get().getSliderHintAlpha());
                abstractSliderBody.setHintWidth(Math.min(OsuSkin.get().getSliderHintWidth() * scale, bodyWidth));
                RGBColor hintColor = OsuSkin.get().getSliderHintColor();
                if (hintColor != null) {
                    abstractSliderBody.setHintColor(hintColor.r(), hintColor.g(), hintColor.b());
                } else {
                    abstractSliderBody.setHintColor(bodyColor.r(), bodyColor.g(), bodyColor.b());
                }
            }

            abstractSliderBody.applyToScene(scene, Config.isSnakingInSliders());
            abstractSliderBody.setBodyColor(bodyColor.r(), bodyColor.g(), bodyColor.b());
            RGBColor sliderColor = GameHelper.getSliderColor();
            abstractSliderBody.setBorderColor(sliderColor.r(), sliderColor.g(), sliderColor.b());
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private PointF getPositionAt(final float percentage, final boolean updateBallAngle, final boolean updateEndArrowRotation) {
        if (path.points.isEmpty()) {
            tmpPoint.set(pos);
            return tmpPoint;
        }

        if (percentage >= 1) {
            tmpPoint.set(curveEndPos);
            return tmpPoint;
        } else if (percentage <= 0) {
            if (path.points.size() >= 2) {
                if (updateBallAngle) {
                    ballAngle = MathUtils.radToDeg(Utils.direction(path.points.get(1), pos));
                }

                if (updateEndArrowRotation) {
                    endArrow.setRotation(MathUtils.radToDeg(Utils.direction(pos, path.points.get(1))));
                }
            }

            tmpPoint.set(pos);
            return tmpPoint;
        }

        // Directly taken from library-owned SliderPath
        int left = 0;
        int right = path.length.size() - 2;
        float currentLength = percentage * path.length.get(path.length.size() - 1);

        while (left <= right) {
            int pivot = left + ((right - left) >> 1);
            float length = path.length.get(pivot);

            if (length < currentLength) {
                left = pivot + 1;
            } else if (length > currentLength) {
                right = pivot - 1;
            } else {
                break;
            }
        }

        int index = left - 1;
        float lengthProgress = (currentLength - path.length.get(index)) / (path.length.get(index + 1) - path.length.get(index));

        var currentPoint = path.points.get(index);
        var nextPoint = path.points.get(index + 1);
        var p = tmpPoint;

        p.set(
            Interpolation.linear(currentPoint.x, nextPoint.x, lengthProgress),
            Interpolation.linear(currentPoint.y, nextPoint.y, lengthProgress)
        );

        if (updateBallAngle) {
            ballAngle = MathUtils.radToDeg(Utils.direction(currentPoint, nextPoint));
        }

        if (updateEndArrowRotation) {
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(nextPoint, currentPoint)));
        }

        return p;
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }
        // Detach all objects
        if (abstractSliderBody != null) {
            if (GameHelper.isHidden()) {
                abstractSliderBody.removeFromScene(scene);
            } else {
                abstractSliderBody.removeFromScene(scene, 0.24f / GameHelper.getSpeedMultiplier(), this);
            }
        }

        ball.registerEntityModifier(Modifiers.fadeOut(0.1f / GameHelper.getSpeedMultiplier()).setOnFinished(entity -> {
            Execution.updateThread(entity::detachSelf);
        }));

        if (!Config.isAnimateFollowCircle()) {
            followCircle.detachSelf();
        }

        startCircle.detachSelf();
        endCircle.detachSelf();
        startOverlay.detachSelf();
        endOverlay.detachSelf();
        approachCircle.detachSelf();
        startArrow.detachSelf();
        endArrow.detachSelf();
        for (int i = 0, size = tickSprites.size(); i < size; i++) {
            Sprite sp = tickSprites.get(i);
            sp.detachSelf();
            tickSpritePool.free(sp);
        }
        listener.removeObject(this);
        scene = null;
    }

    public void poolObject() {
        GameHelper.putPath(path);
        GameObjectPool.getInstance().putSlider(this);
        GameObjectPool.getInstance().putNumber(number);
    }

    private void onSpanFinish() {
        // Ensure that all slider ticks in the current span has been judged.
        judgeSliderTicks();

        ++completedSpanCount;

        int totalSpanCount = beatmapSlider.getSpanCount();
        int remainingSpans = totalSpanCount - completedSpanCount;
        boolean stillHasSpan = remainingSpans > 0;

        if (mWasInRadius && replayObjectData == null ||
                replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
            playCurrentNestedObjectHitSound();
            ticksGot++;
            tickSet.set(replayTickIndex++, true);

            if (stillHasSpan) {
                listener.onSliderHit(id, 30, null,
                        reverse ? pos : curveEndPos,
                        false, bodyColor, GameObjectListener.SLIDER_REPEAT);
            }
        } else {
            tickSet.set(replayTickIndex++, false);

            if (stillHasSpan) {
                listener.onSliderHit(id, -1, null,
                        reverse ? pos : curveEndPos,
                        false, bodyColor, GameObjectListener.SLIDER_REPEAT);
            }
        }

        currentNestedObjectIndex++;

        // If slider has more spans
        if (stillHasSpan) {
            double spanDuration = beatmapSlider.getSpanDuration() / 1000;
            reverse = !reverse;
            passedTime -= spanDuration;
            tickTime = passedTime;

            if (reverse) {
                // In reversed spans, a slider's tick position remains the same as the non-reversed span.
                // Therefore, we need to offset the tick time such that the travelled time is (tickInterval - nextTickTime).
                tickTime += tickInterval - spanDuration % tickInterval;
            }

            ball.setFlippedHorizontal(reverse);
            // Restore ticks
            for (int i = 0, size = tickSprites.size(); i < size; i++) {
                tickSprites.get(i).setAlpha(1);
            }
            currentTickSpriteIndex = reverse ? tickSprites.size() - 1 : 0;

            // Setting visibility of repeat arrows
            if (reverse) {
                if (remainingSpans <= 2) {
                    endArrow.setAlpha(0);
                }

                if (remainingSpans > 1) {
                    startArrow.setAlpha(1);
                }
            } else if (remainingSpans <= 2) {
                startArrow.setAlpha(0);
            }

            ((GameScene) listener).onSliderReverse(
                    !reverse ? pos : curveEndPos,
                    reverse ? endArrow.getRotation() : startArrow.getRotation(),
                    bodyColor);
            if (passedTime >= spanDuration) {
                onSpanFinish();
            }
            return;
        }
        mIsOver = true;

        // Calculating score
        int firstHitScore = 0;
        if (GameHelper.isScoreV2()) {
            // If ScoreV2 is active, the accuracy of hitting the slider head is additionally accounted for when judging the entire slider:
            // Getting a 300 for a slider requires getting a 300 judgement for the slider head.
            // Getting a 100 for a slider requires getting a 100 judgement or better for the slider head.
            DifficultyHelper diffHelper = GameHelper.getDifficultyHelper();
            float od = GameHelper.getOverallDifficulty();

            if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor300(od) * 1000) {
                firstHitScore = 300;
            } else if (Math.abs(firstHitAccuracy) <= diffHelper.hitWindowFor100(od) * 1000) {
                firstHitScore = 100;
            }
        }
        int score = 0;
        if (ticksGot > 0) {
            score = 50;
        }
        int totalTicks = beatmapSlider.getNestedHitObjects().size();
        if (ticksGot >= totalTicks / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
            score = 100;
        }
        if (ticksGot >= totalTicks && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
            score = 300;
        }
        // If slider was in reverse mode, we should swap start and end points
        if (reverse) {
            Slider.this.listener.onSliderHit(id, score,
                    curveEndPos, pos, endsCombo, bodyColor, GameObjectListener.SLIDER_END);
        } else {
            Slider.this.listener.onSliderHit(id, score, pos,
                    curveEndPos, endsCombo, bodyColor, GameObjectListener.SLIDER_END);
        }
        if (!startHit) {
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty()) * 1000 + 13);
        }
        listener.onSliderEnd(id, firstHitAccuracy, tickSet);
        // Remove slider from scene

        if (Config.isAnimateFollowCircle() && mWasInRadius) {
            mIsAnimating = true;

            followCircle.clearEntityModifiers();
            followCircle.registerEntityModifier(Modifiers.scale(0.2f / GameHelper.getSpeedMultiplier(), followCircle.getScaleX(), followCircle.getScaleX() * 0.8f).setEaseFunction(EaseQuadOut.getInstance()));
            followCircle.registerEntityModifier(
                Modifiers.alpha(0.2f / GameHelper.getSpeedMultiplier(), followCircle.getAlpha(), 0f).setOnFinished(entity -> {
                    Execution.updateThread(() -> {
                        entity.detachSelf();

                        mIsAnimating = false;

                        // We can pool the hit object once all animations are finished.
                        // The follow circle animation is the last one to finish if it's enabled.
                        poolObject();
                    });

                })
            );
        }

        removeFromScene();
    }

    private boolean isHit() {
        float radius = Utils.sqr((float) beatmapSlider.getGameplayRadius());
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(pos, listener.getMousePos(i)) <= radius;
            if (GameHelper.isRelaxMod() && passedTime >= 0 && inPosition) {
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


    @Override
    public void update(final float dt) {

        if (scene == null) {
            return;
        }
        passedTime += dt;

        if (!startHit) // If we didn't get start hit(click)
        {
            // If it's too late, mark this hit missing
            if (passedTime > GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                startHit = true;
                currentNestedObjectIndex++;
                listener.onSliderHit(id, -1, null, pos, false, bodyColor, GameObjectListener.SLIDER_START);
                firstHitAccuracy = (int) (passedTime * 1000);
            } else if (autoPlay && passedTime >= 0) {
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                listener.onSliderHit(id, 30, null, pos, false, bodyColor, GameObjectListener.SLIDER_START);
            } else if (replayObjectData != null &&
                    Math.abs(replayObjectData.accuracy / 1000f) <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty()) &&
                    passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                listener.onSliderHit(id, 30, null, pos, false, bodyColor, GameObjectListener.SLIDER_START);
            } else if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                // if we clicked
                listener.registerAccuracy(passedTime);
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, pos,
                        false, bodyColor, GameObjectListener.SLIDER_START);
            }
        }

        if (GameHelper.isKiai()) {
            final float kiaiModifier = (float) Math.max(0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()) * 0.5f;
            final float r = Math.min(1, circleColor.r() + (1 - circleColor.r()) * kiaiModifier);
            final float g = Math.min(1, circleColor.g() + (1 - circleColor.g()) * kiaiModifier);
            final float b = Math.min(1, circleColor.b() + (1 - circleColor.b()) * kiaiModifier);
            kiai = true;
            startCircle.setColor(r, g, b);
            endCircle.setColor(r, g, b);
        } else if (kiai) {
            startCircle.setColor(circleColor.r(), circleColor.g(), circleColor.b());
            endCircle.setColor(circleColor.r(), circleColor.g(), circleColor.b());
            kiai = false;
        }

        if (passedTime < 0) // we at approach time
        {
            float timePreempt = (float) beatmapSlider.timePreempt / 1000;
            if (startHit) {
                // Hide the approach circle if the slider is already hit.
                approachCircle.clearEntityModifiers();
                approachCircle.setAlpha(0);
            }

            float percentage = (float) (1 + passedTime / timePreempt);
            if (percentage <= 0.5f) {
                // Following core doing a very cute show animation ^_^"
                percentage = Math.min(1, percentage * 2);

                for (int i = 0, size = tickSprites.size(); i < size; i++) {
                    if (percentage > (float) (i + 1) / size) {
                        tickSprites.get(i).setAlpha(1);
                    }
                }

                if (beatmapSlider.getSpanCount() > 1) {
                    endArrow.setAlpha(percentage);
                }

                if (Config.isSnakingInSliders()) {
                    if (superPath != null && abstractSliderBody != null) {
                        float l = superPath.getMeasurer().maxLength() * percentage;

                        abstractSliderBody.setEndLength(l);
                        abstractSliderBody.onUpdate();
                    }

                    var position = getPositionAt(percentage, false, true);

                    Utils.putSpriteAnchorCenter(position, endCircle);
                    Utils.putSpriteAnchorCenter(position, endOverlay);
                    Utils.putSpriteAnchorCenter(position, endArrow);
                }
            } else if (percentage - dt / timePreempt <= 0.5f) {
                // Setting up positions of slider parts
                for (int i = 0, size = tickSprites.size(); i < size; i++) {
                    tickSprites.get(i).setAlpha(1);
                }
                if (beatmapSlider.getSpanCount() > 1) {
                    endArrow.setAlpha(1);
                }
                if (Config.isSnakingInSliders()) {
                    if (!preStageFinish && superPath != null && abstractSliderBody != null) {
                        abstractSliderBody.setEndLength(superPath.getMeasurer().maxLength());
                        abstractSliderBody.onUpdate();
                        preStageFinish = true;
                    }

                    endArrow.setRotation(
                        MathUtils.radToDeg(Utils.direction(curveEndPos, path.points.get(path.points.size() - 2)))
                    );

                    Utils.putSpriteAnchorCenter(curveEndPos, endCircle);
                    Utils.putSpriteAnchorCenter(curveEndPos, endOverlay);
                    Utils.putSpriteAnchorCenter(curveEndPos, endArrow);
                }
            }
            return;
        }

        startCircle.setAlpha(0);
        startOverlay.setAlpha(0);

        float scale = beatmapSlider.getGameplayScale();

        if (!ball.hasParent()) {
            number.detachSelf();

            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);

            // velocity = 100 * difficulty.sliderMultiplier / (timingPoint.msPerBeat * bpmMultiplier)

            ball.setFps((float) beatmapSlider.getVelocity() * 100 * scale);
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);
            ball.registerEntityModifier(Modifiers.fadeIn(0.1f / GameHelper.getSpeedMultiplier()));

            followCircle.setAlpha(0);
            if (!Config.isAnimateFollowCircle()) {
                followCircle.setScale(scale);
            }

            scene.attachChild(ball);
            scene.attachChild(followCircle);
        }

        // Ball position
        final float spanDuration = (float) beatmapSlider.getSpanDuration() / 1000;
        final float percentage = (float) passedTime / spanDuration;
        ballPos = getPositionAt(reverse ? 1 - percentage : percentage, true, false);

        // Calculating if cursor in follow circle bounds
        final float followCircleRadius = (float) beatmapSlider.getGameplayRadius() * 2;
        boolean inRadius = false;
        for (int i = 0, cursorCount = listener.getCursorsCount(); i < cursorCount; i++) {

            var isPressed = listener.isMouseDown(i);

            if (autoPlay || (isPressed && Utils.squaredDistance(listener.getMousePos(i), ballPos) <= followCircleRadius * followCircleRadius)) {
                inRadius = true;
                break;
            }
            if (GameHelper.isAutopilotMod() && isPressed)
                inRadius = true;
        }
        listener.onTrackingSliders(inRadius);
        tickTime += dt;

        if (Config.isAnimateFollowCircle()) {
            float realSliderDuration = (float) beatmapSlider.getDuration() / 1000 / GameHelper.getSpeedMultiplier();
            float remainTime = realSliderDuration - (float) passedTime;

            if (inRadius && !mWasInRadius) {
                mWasInRadius = true;
                mIsAnimating = true;
                playSlidingSamples();

                // If alpha doesn't equal 0 means that it has been into an animation before
                float initialScale = followCircle.getAlpha() == 0 ? scale * 0.5f : followCircle.getScaleX();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.alpha(Math.min(remainTime, 0.06f / GameHelper.getSpeedMultiplier()), followCircle.getAlpha(), 1f));
                followCircle.registerEntityModifier(
                    Modifiers.scale(Math.min(remainTime, 0.18f / GameHelper.getSpeedMultiplier()), initialScale, scale)
                        .setEaseFunction(EaseQuadOut.getInstance())
                        .setOnFinished(entity -> mIsAnimating = false)
                );
            } else if (!inRadius && mWasInRadius) {
                mWasInRadius = false;
                mIsAnimating = true;
                stopSlidingSamples();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.scale(0.1f / GameHelper.getSpeedMultiplier(), followCircle.getScaleX(), scale * 2f));
                followCircle.registerEntityModifier(
                    Modifiers.alpha(0.1f / GameHelper.getSpeedMultiplier(), followCircle.getAlpha(), 0f).setOnFinished(entity -> {
                        if (mIsOver) {
                            Execution.updateThread(entity::detachSelf);
                        }
                        mIsAnimating = false;
                    })
                );
            }
        } else {
            if (inRadius && !mWasInRadius) {
                playSlidingSamples();
            } else if (!inRadius && mWasInRadius) {
                stopSlidingSamples();
            }

            mWasInRadius = inRadius;
            followCircle.setAlpha(inRadius ? 1 : 0);
        }

        judgeSliderTicks();

        // Setting position of ball and follow circle
        followCircle.setPosition(ballPos.x - followCircle.getWidth() / 2,
                ballPos.y - followCircle.getHeight() / 2);
        ball.setPosition(ballPos.x - ball.getWidth() / 2,
                ballPos.y - ball.getHeight() / 2);
        ball.setRotation(ballAngle);

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            listener.updateAutoBasedPos(ballPos.x, ballPos.y);
        }

        // If we got 100% time, finishing slider
        if (percentage >= 1) {
            onSpanFinish();
        }
    }

    private void judgeSliderTicks() {
        if (tickSprites.isEmpty()) {
            return;
        }

        float scale = beatmapSlider.getGameplayScale();

        // Some magic with slider ticks. If it'll crash it's not my fault ^_^"
        while (tickTime >= tickInterval) {
            tickTime -= tickInterval;
            var tickSprite = tickSprites.get(currentTickSpriteIndex);

            if (tickSprite.getAlpha() == 0) {
                // All ticks in the current span had been judged.
                break;
            }

            if (mWasInRadius && replayObjectData == null ||
                    replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
                playCurrentNestedObjectHitSound();
                listener.onSliderHit(id, 10, null, ballPos, false, bodyColor, GameObjectListener.SLIDER_TICK);

                if (Config.isAnimateFollowCircle() && !mIsAnimating) {
                    followCircle.clearEntityModifiers();
                    followCircle.registerEntityModifier(Modifiers.scale((float) Math.min(tickInterval, 0.2f) / GameHelper.getSpeedMultiplier(), scale * 1.1f, scale).setEaseFunction(EaseQuadOut.getInstance()));
                }

                ticksGot++;
                tickSet.set(replayTickIndex++, true);
            } else {
                listener.onSliderHit(id, -1, null, ballPos, false, bodyColor, GameObjectListener.SLIDER_TICK);
                tickSet.set(replayTickIndex++, false);
            }

            currentNestedObjectIndex++;

            tickSprite.setAlpha(0);
            if (reverse && currentTickSpriteIndex > 0) {
                currentTickSpriteIndex--;
            } else if (!reverse && currentTickSpriteIndex < tickSprites.size() - 1) {
                currentTickSpriteIndex++;
            }
        }
    }

    private void applyBodyFadeAdjustments(float fadeInDuration) {
        if (abstractSliderBody == null) {
            return;
        }

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float fadeOutDuration = (float) (beatmapSlider.getDuration() + beatmapSlider.timePreempt) / 1000 / GameHelper.getSpeedMultiplier() - fadeInDuration;

            abstractSliderBody.applyFadeAdjustments(fadeInDuration, fadeOutDuration);
        } else {
            abstractSliderBody.applyFadeAdjustments(fadeInDuration);
        }
    }

    private void playCurrentNestedObjectHitSound() {
        listener.playSamples(beatmapSlider.getNestedHitObjects().get(currentNestedObjectIndex));
    }

    private void playSlidingSamples() {
        listener.playAuxiliarySamples(beatmapSlider);
    }

    private void stopSlidingSamples() {
        listener.stopAuxiliarySamples(beatmapSlider);
    }

    @Override
    public void tryHit(final float dt) {
        if (!startHit) // If we didn't get start hit(click)
        {
            if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) // if
            {
                listener.registerAccuracy(passedTime);
                startHit = true;
                playCurrentNestedObjectHitSound();
                currentNestedObjectIndex++;
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, pos,
                        false, bodyColor, GameObjectListener.SLIDER_START);
            }
            if (passedTime < 0) // we at approach time
            {
                if (startHit) {
                    approachCircle.clearEntityModifiers();
                    approachCircle.setAlpha(0);
                }
            }
        }
    }

}
