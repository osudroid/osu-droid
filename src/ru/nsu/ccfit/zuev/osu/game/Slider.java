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
import com.rian.osu.beatmap.timings.TimingControlPoint;
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
import java.util.Arrays;
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
    private PointF endPos;
    private Scene scene;
    private GameObjectListener listener;
    private TimingControlPoint timingControlPoint;
    private CircleNumber number;
    private SliderPath path;
    private double passedTime;
    private int completedSpanCount;
    private boolean reverse;
    private int[] soundId = new int[3];
    private int[] sampleSet = new int[3];
    private int[] addition = new int[3];

    private int soundIdIndex;
    private int ticksGot;
    private double tickTime;
    private double tickInterval;
    private int currentTick;

    private final AnimSprite ball;
    private final Sprite followCircle;

    // Temporarily used PointF to avoid allocations
    private final PointF tmpPoint = new PointF();
    private float ballAngle;

    private boolean kiai;
    private final RGBColor color = new RGBColor();
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
                     final RGBColor comboColor, final float tickRate, final int sound,
                     final BeatmapControlPoints controlPoints, final String customSound,
                     final String tempSound, final boolean isFirstNote, SliderPath sliderPath) {
        this.listener = listener;
        this.scene = scene;
        this.beatmapSlider = beatmapSlider;
        this.timingControlPoint = controlPoints.timing.controlPointAt(beatmapSlider.startTime);
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
        currentTick = 0;
        replayTickIndex = 0;
        firstHitAccuracy = 0;
        tickSet.clear();
        kiai = GameHelper.isKiai();
        preStageFinish = false;
        color.set(comboColor.r(), comboColor.g(), comboColor.b());
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            var sliderBodyColor = OsuSkin.get().getSliderBodyColor();
            color.set(sliderBodyColor.r(), sliderBodyColor.g(), sliderBodyColor.b());
        }
        circleColor.set(comboColor.r(), comboColor.g(), comboColor.b());

        int spanCount = beatmapSlider.getSpanCount();
        if (soundId.length < spanCount) {
            soundId = new int[spanCount];
            sampleSet = new int[spanCount];
            addition = new int[spanCount];
        }

        Arrays.fill(soundId, sound);
        Arrays.fill(sampleSet, 0);
        Arrays.fill(addition, 0);

        if (customSound != null) {
            final String[] pars = customSound.split("[|]");
            for (int i = 0; i < soundId.length; i++) {
                if (i < pars.length) {
                    soundId[i] = Integer.parseInt(pars[i]);
                }
            }
        }

        if (!Utils.isEmpty(tempSound)) {
            final String[] pars = tempSound.split("[|]");
            for (int i = 0; i < pars.length; i++) {
                final String[] group = pars[i].split(":");
                if (i < sampleSet.length) {
                    sampleSet[i] = Integer.parseInt(group[0]);
                }
                if (i < addition.length) {
                    addition[i] = Integer.parseInt(group[1]);
                }
            }
        }
        soundIdIndex = 1;

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
        endPos = beatmapSlider.getGameplayStackedEndPosition().toPointF();
        endCircle.setScale(scale);
        endCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        endCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endCircle);

        endOverlay.setScale(scale);
        endOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endOverlay);

        scene.attachChild(startOverlay, 0);
        // Repeat arrow at start
        if (spanCount > 2) {
            startArrow.setAlpha(0);
            startArrow.setScale(scale);
            startArrow.setRotation(MathUtils.radToDeg(Utils.direction(pos, path.points.get(1))));

            Utils.putSpriteAnchorCenter(pos, startArrow);
            scene.attachChild(startArrow, 0);
        }

        float fadeInDuration = (float) beatmapSlider.timeFadeIn / 1000 / GameHelper.getSpeedMultiplier();

        if (GameHelper.isHidden()) {
            float fadeOutDuration = (float) (beatmapSlider.timePreempt * ModHidden.FADE_OUT_DURATION_MULTIPLIER) / GameHelper.getSpeedMultiplier();

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
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(path.points.get(path.points.size() - 2), endPos)));

            Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endArrow);
            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(endCircle, 0);

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

            if (OsuSkin.get().isSliderHintEnable() && beatmapSlider.getPath().getExpectedDistance() > OsuSkin.get().getSliderHintShowMinLength()) {
                abstractSliderBody.setEnableHint(true);
                abstractSliderBody.setHintAlpha(OsuSkin.get().getSliderHintAlpha());
                abstractSliderBody.setHintWidth(Math.min(OsuSkin.get().getSliderHintWidth() * scale, bodyWidth));
                RGBColor hintColor = OsuSkin.get().getSliderHintColor();
                if (hintColor != null) {
                    abstractSliderBody.setHintColor(hintColor.r(), hintColor.g(), hintColor.b());
                } else {
                    abstractSliderBody.setHintColor(color.r(), color.g(), color.b());
                }
            }

            abstractSliderBody.applyToScene(scene, Config.isSnakingInSliders());
            abstractSliderBody.setBodyColor(color.r(), color.g(), color.b());
            RGBColor sliderColor = GameHelper.getSliderColor();
            abstractSliderBody.setBorderColor(sliderColor.r(), sliderColor.g(), sliderColor.b());
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private PointF getPercentPosition(final float percentage, final boolean updateBallAngle) {
        if (path.points.isEmpty()) {
            tmpPoint.set(pos);
            return tmpPoint;
        }

        if (percentage >= 1) {
            tmpPoint.set(endPos);
            return tmpPoint;
        } else if (percentage <= 0) {
            if (updateBallAngle && path.points.size() >= 2) {
                ballAngle = MathUtils.radToDeg(Utils.direction(path.points.get(1), pos));
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
        ++completedSpanCount;

        int totalSpanCount = beatmapSlider.getSpanCount();
        int remainingSpans = totalSpanCount - completedSpanCount;
        boolean stillHasSpan = remainingSpans > 0;

        if (mWasInRadius && replayObjectData == null ||
                replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
            if (soundIdIndex < soundId.length)
                Utils.playHitSound(listener, soundId[soundIdIndex],
                        sampleSet[soundIdIndex], addition[soundIdIndex]);
            ticksGot++;
            tickSet.set(replayTickIndex++, true);
            if (stillHasSpan) {
                listener.onSliderHit(id, 30, null,
                        reverse ? pos : endPos,
                        false, color, GameObjectListener.SLIDER_REPEAT);
            }
        } else {
            tickSet.set(replayTickIndex++, false);
            if (stillHasSpan) {
                listener.onSliderHit(id, -1, null,
                        reverse ? pos : endPos,
                        false, color, GameObjectListener.SLIDER_REPEAT);
            }
        }
        soundIdIndex++;

        // If slider has more spans
        if (stillHasSpan) {
            double spanDuration = beatmapSlider.getSpanDuration() / 1000;
            reverse = !reverse;
            passedTime -= spanDuration;
            tickTime = passedTime;
            ball.setFlippedHorizontal(reverse);
            // Restore ticks
            for (int i = 0, size = tickSprites.size(); i < size; i++) {
                tickSprites.get(i).setAlpha(1);
            }
            currentTick = reverse ? tickSprites.size() - 1 : 0;

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
                    !reverse ? pos : endPos,
                    reverse ? endArrow.getRotation() : startArrow.getRotation(),
                    color);
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
            float od = GameHelper.getDifficulty();

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
                    endPos, pos, endsCombo, color, GameObjectListener.SLIDER_END);
        } else {
            Slider.this.listener.onSliderHit(id, score, pos,
                    endPos, endsCombo, color, GameObjectListener.SLIDER_END);
        }
        if (!startHit) {
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) * 1000 + 13);
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
        float radius = Utils.sqr(64 * beatmapSlider.getGameplayScale());
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
            if (passedTime > GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                startHit = true;
                listener.onSliderHit(id, -1, null, pos, false, color, GameObjectListener.SLIDER_START);
                firstHitAccuracy = (int) (passedTime * 1000);
            } else if (autoPlay && passedTime >= 0) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, pos, false, color, GameObjectListener.SLIDER_START);
            } else if (replayObjectData != null &&
                    Math.abs(replayObjectData.accuracy / 1000f) <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) &&
                    passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, pos, false, color, GameObjectListener.SLIDER_START);
            } else if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                // if we clicked
                listener.registerAccuracy(passedTime);
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, pos,
                        false, color, GameObjectListener.SLIDER_START);
            }
        }

        if (GameHelper.isKiai()) {
            final float kiaiModifier = (float) Math.max(0, 1 - GameHelper.getGlobalTime() / GameHelper.getKiaiTickLength()) * 0.50f;
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

                    var position = getPercentPosition(percentage, false);

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

                    Utils.putSpriteAnchorCenter(endPos, endCircle);
                    Utils.putSpriteAnchorCenter(endPos, endOverlay);
                    Utils.putSpriteAnchorCenter(endPos, endArrow);
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

            ball.setFps((float) (100 * GameHelper.getSpeed() * scale / timingControlPoint.msPerBeat));
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
        final PointF ballPos = getPercentPosition(reverse ? 1 - percentage : percentage, true);
        // Calculating if cursor in follow circle bounds
        final float followCircleRadius = 128 * scale;
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
            float remainTime = (float) (spanDuration / GameHelper.getSpeedMultiplier() * beatmapSlider.getSpanCount() - passedTime);

            if (inRadius && !mWasInRadius) {
                mWasInRadius = true;
                mIsAnimating = true;

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
            mWasInRadius = inRadius;
            followCircle.setAlpha(inRadius ? 1 : 0);
        }

        // Some magic with slider ticks. If it'll crash it's not my fault ^_^"
        while (!tickSprites.isEmpty() && percentage < 1 - 0.02f / spanDuration
                && tickTime > tickInterval) {
            tickTime -= tickInterval;
            if (followCircle.getAlpha() > 0 && replayObjectData == null ||
                    replayObjectData != null && replayObjectData.tickSet.get(replayTickIndex)) {
                Utils.playHitSound(listener, 16);
                listener.onSliderHit(id, 10, null, ballPos, false, color, GameObjectListener.SLIDER_TICK);

                if (Config.isAnimateFollowCircle() && !mIsAnimating) {
                    followCircle.clearEntityModifiers();
                    followCircle.registerEntityModifier(Modifiers.scale((float) Math.min(tickInterval, 0.2f) / GameHelper.getSpeedMultiplier(), scale * 1.1f, scale).setEaseFunction(EaseQuadOut.getInstance()));
                }

                ticksGot++;
                tickSet.set(replayTickIndex++, true);
            } else {
                listener.onSliderHit(id, -1, null, ballPos, false, color, GameObjectListener.SLIDER_TICK);
                tickSet.set(replayTickIndex++, false);
            }
            tickSprites.get(currentTick).setAlpha(0);
            if (reverse && currentTick > 0) {
                currentTick--;
            } else if (!reverse && currentTick < tickSprites.size() - 1) {
                currentTick++;
            }
        }
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

    private void applyBodyFadeAdjustments(float fadeInDuration) {
        if (abstractSliderBody == null) {
            return;
        }

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float fadeOutDuration = (float) (beatmapSlider.getDuration() + beatmapSlider.timePreempt) / GameHelper.getSpeedMultiplier() - fadeInDuration;

            abstractSliderBody.applyFadeAdjustments(fadeInDuration, fadeOutDuration);
        } else {
            abstractSliderBody.applyFadeAdjustments(fadeInDuration);
        }
    }

    @Override
    public void tryHit(final float dt) {
        if (!startHit) // If we didn't get start hit(click)
        {
            if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) // if
            {
                listener.registerAccuracy(passedTime);
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, pos,
                        false, color, GameObjectListener.SLIDER_START);
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
