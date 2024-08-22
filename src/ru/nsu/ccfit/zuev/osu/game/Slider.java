package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody2D;
import com.reco1l.framework.Pool;
import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;
import com.rian.osu.beatmap.sections.BeatmapControlPoints;
import com.rian.osu.beatmap.timings.TimingControlPoint;

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
    private final ArrayList<Sprite> ticks = new ArrayList<>();
    private PointF startPosition, endPosition;
    private Scene scene;
    private GameObjectListener listener;
    private TimingControlPoint timingControlPoint;
    private CircleNumber number;
    private SliderPath path;
    private double passedTime;
    private double preTime;
    private double tickTime;
    private double maxTime;
    private float scale;
    private int repeatCount;
    private boolean reverse;
    private int[] soundId = new int[3];
    private int[] sampleSet = new int[3];
    private int[] addition = new int[3];

    private int soundIdIndex;
    private int ticksGot;
    private int ticksTotal;
    private int currentTick;
    private double tickInterval;

    private final AnimSprite ball;
    private final Sprite followCircle;

    private PointF tmpPoint = new PointF();
    private float ballAngle;

    private boolean kiai;
    private RGBColor color = new RGBColor();
    private final RGBColor circleColor = new RGBColor();

    //for replay
    private int firstHitAccuracy;
    private final BitSet tickSet = new BitSet();
    private int tickIndex;

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
                     final PointF pos, final float offset, final float time, final RGBColor comboColor,
                     final float scale, int num, final int sound, final int repeats, final float length,
                     final String data, final BeatmapControlPoints controlPoints, final String customSound,
                     final String tempSound, final boolean isFirstNote, final double realTime) {
        init(listener, scene, pos, offset, time, comboColor, scale, num, sound, repeats, length, data, controlPoints, customSound, tempSound, isFirstNote, realTime, null);
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final PointF pos, final float offset, final float time, final RGBColor comboColor, final float scale, int num, final int sound, final int repeats,
                     final float length, final String data, final BeatmapControlPoints controlPoints, final String customSound,
                     final String tempSound, final boolean isFirstNote, final double realTime, SliderPath sliderPath) {
        this.listener = listener;
        this.scene = scene;
        this.timingControlPoint = controlPoints.timing.controlPointAt(realTime);
        this.scale = scale;
        this.pos = pos;
        passedTime = -time;
        preTime = time;
        path = sliderPath != null ?
                sliderPath :
                GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                        data.split("[|]"), Math.max(0, length), offset);

        num += 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            num %= 10;
        }
        number = GameObjectPool.getInstance().getNumber(num);
        number.init(pos, scale);

        double speedMultiplier = controlPoints.difficulty.controlPointAt(realTime).speedMultiplier;

        double scoringDistance = GameHelper.getSpeed() * speedMultiplier;
        double velocity = scoringDistance / timingControlPoint.msPerBeat;
        double spanDuration = length / velocity;
        if (spanDuration <= 0) {
            spanDuration = 0;
        }

        mIsOver = false;
        mIsAnimating = false;
        mWasInRadius = false;

        maxTime = (float) (spanDuration / 1000);
        repeatCount = repeats;
        reverse = false;
        startHit = false;
        ticksGot = 0;
        ticksTotal = 1;
        tickTime = 0;
        currentTick = 0;
        tickIndex = 0;
        firstHitAccuracy = 0;
        tickSet.clear();
        kiai = GameHelper.isKiai();
        preStageFinish = false;
        color.set(comboColor.r(), comboColor.g(), comboColor.b());
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            color = new RGBColor(OsuSkin.get().getSliderBodyColor());
        }
        circleColor.set(comboColor.r(), comboColor.g(), comboColor.b());

        if (soundId.length < repeats + 1) {
            soundId = new int[repeats + 1];
            sampleSet = new int[repeats + 1];
            addition = new int[repeats + 1];
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
        startPosition = pos;
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
        PointF endPos = pos;
        if (!path.points.isEmpty()) {
            endPos = path.points.get(path.points.size() - 1);
        }
        endCircle.setScale(scale);
        endCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
        endCircle.setAlpha(0);
        endPosition = endPos;
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endCircle);

        endOverlay.setScale(scale);
        endOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endOverlay);

        scene.attachChild(startOverlay, 0);
        // Repeat arrow at start
        if (repeatCount > 2) {
            startArrow.setAlpha(0);
            startArrow.setScale(scale);
            startArrow.setRotation(MathUtils.radToDeg(Utils.direction(
                    path.points.get(0), path.points.get(1))));
            Utils.putSpriteAnchorCenter(pos, startArrow);
            scene.attachChild(startArrow, 0);
        }

        float fadeInDuration;

        if (GameHelper.isHidden()) {
            fadeInDuration = time * 0.4f * GameHelper.getTimeMultiplier();
            float fadeOutDuration = time * 0.3f * GameHelper.getTimeMultiplier();

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
            // Preempt time can go below 450ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
            // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear function above.
            // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
            // This adjustment is necessary for AR>10, otherwise TimePreempt can become smaller leading to hitcircles not fully fading in.
            fadeInDuration = 0.4f * Math.min(1, time / ((float) GameHelper.ar2ms(10) / 1000)) * GameHelper.getTimeMultiplier();

            number.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            startCircle.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            startOverlay.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            endCircle.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
            endOverlay.registerEntityModifier(Modifiers.fadeIn(fadeInDuration));
        }

        if (approachCircle.isVisible()) {
            approachCircle.registerEntityModifier(Modifiers.alpha(Math.min(fadeInDuration * 2, time * GameHelper.getTimeMultiplier()), 0, 0.9f));
            approachCircle.registerEntityModifier(Modifiers.scale(time * GameHelper.getTimeMultiplier(), scale * 3, scale));
        }

        scene.attachChild(number, 0);
        scene.attachChild(startCircle, 0);
        scene.attachChild(approachCircle);
        scene.attachChild(endOverlay, 0);
        // Repeat arrow at end
        if (repeatCount > 1) {
            endArrow.setAlpha(0);
            endArrow.setScale(scale);
            if (path.points.size() >= 2) {
                int lastIndex = path.points.size() - 1;
                endArrow.setRotation(MathUtils.radToDeg(Utils.direction(
                        path.points.get(lastIndex), path.points.get(lastIndex - 1))));
            }
            Utils.putSpriteAnchorCenter(Config.isSnakingInSliders() ? pos : endPos, endArrow);
            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(endCircle, 0);

        tickInterval = timingControlPoint.msPerBeat / 1000;
        int tickCount = (int) (maxTime * GameHelper.getTickRate() / tickInterval);
        if (Double.isNaN(tickInterval) || tickInterval < GameHelper.getSliderTickLength() / 1000) {
            tickCount = 0;
        }
        if ((maxTime * GameHelper.getTickRate() / tickInterval)
                - (int) (maxTime * GameHelper.getTickRate() / tickInterval) < 0.001f) {
            tickCount--;
        }
        ticks.clear();
        for (int i = 1; i <= tickCount; i++) {
            var pos1 = getPercentPosition((float) (i * tickInterval / (maxTime * GameHelper.getTickRate())), null);
            var tick = tickSpritePool.obtain();
            tick.setPosition(pos1.x, pos1.y);
            tick.setScale(scale);
            tick.setAlpha(0);
            ticks.add(tick);
            scene.attachChild(tick, 0);
        }

        // Slider track
        if (!path.points.isEmpty()) {
            superPath = new LinePath();
            for (PointF p : path.points) {
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

            if (OsuSkin.get().isSliderHintEnable() && length > OsuSkin.get().getSliderHintShowMinLength()) {
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
            RGBColor scolor = GameHelper.getSliderColor();
            abstractSliderBody.setBorderColor(scolor.r(), scolor.g(), scolor.b());
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private PointF getPercentPosition(final float percentage, final Float angle) {
        if (path.points.isEmpty()) {
            tmpPoint.set(startPosition);
            return tmpPoint;
        }

        if (percentage >= 1) {
            tmpPoint.set(endPosition);
            return tmpPoint;
        } else if (percentage <= 0) {
            if (angle != null && path.points.size() >= 2) {
                ballAngle = MathUtils.radToDeg(Utils.direction(
                        path.points.get(1), startPosition));
            }
            tmpPoint.set(startPosition);
            return tmpPoint;
        }

        if (path.length.size() == 1) {
            final PointF p = tmpPoint;
            p.x = startPosition.x * percentage + path.points.get(1).x
                    * (1 - percentage);
            p.y = startPosition.y * percentage + path.points.get(1).y
                    * (1 - percentage);
            return p;
        }
        int left = 0, right = path.length.size();
        int index = right / 2;
        final float realLength = percentage
                * path.length.get(path.length.size() - 1);
        while (left < right) {
            if (index < path.length.size() - 1
                    && path.length.get(index + 1) < realLength) {
                left = index;
            } else if (path.length.get(index) >= realLength) {
                right = index;
            } else {
                break;
            }
            index = (right + left) / 2;
        }

        float addlength = realLength - path.length.get(index);
        addlength /= path.length.get(index) - path.length.get(index + 1);
        final PointF p = tmpPoint;
        p.x = path.points.get(index).x * addlength
                + path.points.get(index + 1).x * (1 - addlength);
        p.y = path.points.get(index).y * addlength
                + path.points.get(index + 1).y * (1 - addlength);
        if (angle != null) {
            ballAngle = MathUtils.radToDeg(Utils.direction(
                    path.points.get(index), path.points.get(index + 1)));
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
                abstractSliderBody.removeFromScene(scene, 0.24f * GameHelper.getTimeMultiplier(), this);
            }
        }

        ball.registerEntityModifier(Modifiers.fadeOut(0.1f * GameHelper.getTimeMultiplier()).setOnFinished(entity -> {
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
        for (int i = 0, ticksSize = ticks.size(); i < ticksSize; i++) {
            Sprite sp = ticks.get(i);
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

    private void over() {
        repeatCount--;
        if (mWasInRadius && replayObjectData == null ||
                replayObjectData != null && replayObjectData.tickSet.get(tickIndex)) {
            if (soundIdIndex < soundId.length)
                Utils.playHitSound(listener, soundId[soundIdIndex],
                        sampleSet[soundIdIndex], addition[soundIdIndex]);
            ticksGot++;
            tickSet.set(tickIndex++, true);
            if (repeatCount > 0) {
                listener.onSliderHit(id, 30, null,
                        reverse ? startPosition : endPosition,
                        false, color, GameObjectListener.SLIDER_REPEAT);
            }
        } else {
            tickSet.set(tickIndex++, false);
            if (repeatCount > 0) {
                listener.onSliderHit(id, -1, null,
                        reverse ? startPosition : endPosition,
                        false, color, GameObjectListener.SLIDER_REPEAT);
            }
        }
        soundIdIndex++;
        ticksTotal++;
        // If slider has more repeats
        if (repeatCount > 0) {
            reverse = !reverse;
            passedTime -= maxTime;
            tickTime = passedTime;
            ball.setFlippedHorizontal(reverse);
            // Restore ticks
            for (final Sprite sp : ticks) {
                sp.setAlpha(1);
            }
            currentTick = reverse ? ticks.size() - 1 : 0;
            // Setting visibility of repeat arrows
            if (reverse && repeatCount <= 2) {
                endArrow.setAlpha(0);
            }

            if (reverse && repeatCount > 1) {
                startArrow.setAlpha(1);
            }

            if (!reverse && repeatCount <= 2) {
                startArrow.setAlpha(0);
            }
            ((GameScene) listener).onSliderReverse(
                    !reverse ? startPosition : endPosition,
                    reverse ? endArrow.getRotation() : startArrow.getRotation(),
                    color);
            if (passedTime >= maxTime) {
                over();
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
        if (ticksGot >= ticksTotal / 2 && (!GameHelper.isScoreV2() || firstHitScore >= 100)) {
            score = 100;
        }
        if (ticksGot >= ticksTotal && (!GameHelper.isScoreV2() || firstHitScore == 300)) {
            score = 300;
        }
        // If slider was in reverse mode, we should swap start and end points
        if (reverse) {
            Slider.this.listener.onSliderHit(id, score,
                    endPosition, startPosition, endsCombo, color, GameObjectListener.SLIDER_END);
        } else {
            Slider.this.listener.onSliderHit(id, score, startPosition,
                    endPosition, endsCombo, color, GameObjectListener.SLIDER_END);
        }
        if (!startHit) {
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) * 1000 + 13);
        }
        listener.onSliderEnd(id, firstHitAccuracy, tickSet);
        // Remove slider from scene

        if (Config.isAnimateFollowCircle() && mWasInRadius) {
            mIsAnimating = true;

            followCircle.clearEntityModifiers();
            followCircle.registerEntityModifier(Modifiers.scale(0.2f * GameHelper.getTimeMultiplier(), followCircle.getScaleX(), followCircle.getScaleX() * 0.8f).setEaseFunction(EaseQuadOut.getInstance()));
            followCircle.registerEntityModifier(
                Modifiers.alpha(0.2f * GameHelper.getTimeMultiplier(), followCircle.getAlpha(), 0f).setOnFinished(entity -> {
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
        float radius = Utils.sqr(64 * scale);
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(startPosition, listener.getMousePos(i)) <= radius;
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
                listener.onSliderHit(id, -1, null, startPosition, false, color, GameObjectListener.SLIDER_START);
                firstHitAccuracy = (int) (passedTime * 1000);
            } else if (autoPlay && passedTime >= 0) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START);
            } else if (replayObjectData != null &&
                    Math.abs(replayObjectData.accuracy / 1000f) <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) &&
                    passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, startPosition, false, color, GameObjectListener.SLIDER_START);
            } else if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                // if we clicked
                listener.registerAccuracy(passedTime);
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, startPosition,
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
            if (startHit) {
                // Hide the approach circle if the slider is already hit.
                approachCircle.clearEntityModifiers();
                approachCircle.setAlpha(0);
            }

            float percentage = (float) (1 + passedTime / preTime);
            if (percentage <= 0.5f) {
                // Following core doing a very cute show animation ^_^"
                percentage = Math.min(1, percentage * 2);

                for (int i = 0; i < ticks.size(); i++) {
                    if (percentage > (float) (i + 1) / ticks.size()) {
                        ticks.get(i).setAlpha(1);
                    }
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(percentage);
                }

                if (Config.isSnakingInSliders()) {
                    if (superPath != null && abstractSliderBody != null) {
                        float l = superPath.getMeasurer().maxLength() * percentage;

                        abstractSliderBody.setEndLength(l);
                        abstractSliderBody.onUpdate();
                    }

                    tmpPoint = getPercentPosition(percentage, null);

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }
            } else if (percentage - dt / preTime <= 0.5f) {
                // Setting up positions of slider parts
                for (int i = 0, ticksSize = ticks.size(); i < ticksSize; i++) {
                    ticks.get(i).setAlpha(1);
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(1);
                }
                if (Config.isSnakingInSliders()) {
                    if (!preStageFinish && superPath != null && abstractSliderBody != null) {
                        abstractSliderBody.setEndLength(superPath.getMeasurer().maxLength());
                        abstractSliderBody.onUpdate();
                        preStageFinish = true;
                    }

                    tmpPoint = endPosition;

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }
            }
            return;
        } else {
            startCircle.setAlpha(0);
            startOverlay.setAlpha(0);
        }

        if (!ball.hasParent()) {
            number.detachSelf();

            approachCircle.clearEntityModifiers();
            approachCircle.setAlpha(0);

            ball.setFps((float) (100 * GameHelper.getSpeed() * scale / timingControlPoint.msPerBeat));
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);
            ball.registerEntityModifier(Modifiers.fadeIn(0.1f * GameHelper.getTimeMultiplier()));

            followCircle.setAlpha(0);
            if (!Config.isAnimateFollowCircle()) {
                followCircle.setScale(scale);
            }

            scene.attachChild(ball);
            scene.attachChild(followCircle);
        }

        // Ball positiong
        final float percentage = (float) (passedTime / maxTime);
        final PointF ballpos = getPercentPosition(reverse ? 1 - percentage : percentage, ballAngle);
        // Calculating if cursor in follow circle bounds
        final float radius = 128 * scale;
        boolean inRadius = false;
        for (int i = 0, cursorCount = listener.getCursorsCount(); i < cursorCount; i++) {

            var isPressed = listener.isMouseDown(i);

            if (autoPlay || (isPressed && Utils.squaredDistance(listener.getMousePos(i), ballpos) <= radius * radius)) {
                inRadius = true;
                break;
            }
            if (GameHelper.isAutopilotMod() && isPressed)
                inRadius = true;
        }
        listener.onTrackingSliders(inRadius);
        tickTime += dt;

        if (Config.isAnimateFollowCircle()) {
            float remainTime = (float) ((maxTime * GameHelper.getTimeMultiplier() * repeatCount) - passedTime);

            if (inRadius && !mWasInRadius) {
                mWasInRadius = true;
                mIsAnimating = true;

                // If alpha doesn't equal 0 means that it has been into an animation before
                float initialScale = followCircle.getAlpha() == 0 ? scale * 0.5f : followCircle.getScaleX();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.alpha(Math.min(remainTime, 0.06f * GameHelper.getTimeMultiplier()), followCircle.getAlpha(), 1f));
                followCircle.registerEntityModifier(
                    Modifiers.scale(Math.min(remainTime, 0.18f * GameHelper.getTimeMultiplier()), initialScale, scale)
                        .setEaseFunction(EaseQuadOut.getInstance())
                        .setOnFinished(entity -> mIsAnimating = false)
                );
            } else if (!inRadius && mWasInRadius) {
                mWasInRadius = false;
                mIsAnimating = true;

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(Modifiers.scale(0.1f * GameHelper.getTimeMultiplier(), followCircle.getScaleX(), scale * 2f));
                followCircle.registerEntityModifier(
                    Modifiers.alpha(0.1f * GameHelper.getTimeMultiplier(), followCircle.getAlpha(), 0f).setOnFinished(entity -> {
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
        while (ticks.size() > 0 && percentage < 1 - 0.02f / maxTime
                && tickTime * GameHelper.getTickRate() > tickInterval) {
            tickTime -= tickInterval / GameHelper.getTickRate();
            if (followCircle.getAlpha() > 0 && replayObjectData == null ||
                    replayObjectData != null && replayObjectData.tickSet.get(tickIndex)) {
                Utils.playHitSound(listener, 16);
                listener.onSliderHit(id, 10, null, ballpos, false, color, GameObjectListener.SLIDER_TICK);

                if (Config.isAnimateFollowCircle() && !mIsAnimating) {
                    followCircle.clearEntityModifiers();
                    followCircle.registerEntityModifier(Modifiers.scale((float) Math.min(tickInterval / GameHelper.getTickRate(), 0.2f) * GameHelper.getTimeMultiplier(), scale * 1.1f, scale).setEaseFunction(EaseQuadOut.getInstance()));
                }

                ticksGot++;
                tickSet.set(tickIndex++, true);
            } else {
                listener.onSliderHit(id, -1, null, ballpos, false, color, GameObjectListener.SLIDER_TICK);
                tickSet.set(tickIndex++, false);
            }
            ticks.get(currentTick).setAlpha(0);
            if (reverse && currentTick > 0) {
                currentTick--;
            } else if (!reverse && currentTick < ticks.size() - 1) {
                currentTick++;
            }
            ticksTotal++;
        }
        // Setting position of ball and follow circle
        followCircle.setPosition(ballpos.x - followCircle.getWidth() / 2,
                ballpos.y - followCircle.getHeight() / 2);
        ball.setPosition(ballpos.x - ball.getWidth() / 2,
                ballpos.y - ball.getHeight() / 2);
        ball.setRotation(ballAngle);

        if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
            listener.updateAutoBasedPos(ballpos.x, ballpos.y);
        }

        // If we got 100% time, finishing slider
        if (percentage >= 1) {
            over();
        }
    }

    private void applyBodyFadeAdjustments(float fadeInDuration) {
        if (abstractSliderBody == null) {
            return;
        }

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float realFadeInDuration = fadeInDuration / GameHelper.getTimeMultiplier();
            float fadeOutDuration = (float) ((maxTime * repeatCount + preTime - realFadeInDuration) * GameHelper.getTimeMultiplier());

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
                listener.onSliderHit(id, 30, null, startPosition,
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
