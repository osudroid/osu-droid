package main.osu.game;

import android.graphics.PointF;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody2D;
import com.edlplan.osu.support.timing.controlpoint.TimingControlPoint;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.util.MathUtils;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import main.osu.Config;
import main.osu.RGBColor;
import main.skins.OsuSkin;
import main.skins.SkinManager;
import main.osu.Utils;
import main.osu.async.SyncTaskManager;
import main.osu.game.GameHelper.SliderPath;
import main.osu.helper.AnimSprite;

public class Slider extends GameObject {
    private final Sprite startCircle, endCircle;
    private final Sprite startOverlay, endOverlay;
    private final Sprite approachCircle;
    private final Sprite startArrow, endArrow;
    private final ArrayList<Sprite> ticks = new ArrayList<>();
    private Scene scene;
    private GameObjectListener listener;
    private TimingPoint timing;
    private CircleNumber number;
    private SliderPath path;
    private float passedTime;
    private float preTime;
    private float tickTime;
    private float maxTime;
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

    private AnimSprite ball;
    private Sprite followcircle;

    private PointF tmpPoint = new PointF();
    private Float ballAngle = 0f;

    private boolean kiai;
    private boolean isHiddenFadeOutActive = false;
    private RGBColor color = new RGBColor();
    private RGBColor borderColor = new RGBColor();

    //for replay
    private int firstHitAccuracy;
    private final BitSet tickSet = new BitSet();
    private int tickIndex;

    private LinePath superPath = null;
    private boolean preStageFinish = false;

    private SliderBody2D abstractSliderBody = null;

    public Slider() {
        startCircle = SpritePool.getInstance().getSprite("sliderstartcircle");
        endCircle = SpritePool.getInstance().getSprite("sliderendcircle");
        startOverlay = SpritePool.getInstance().getSprite("sliderstartcircleoverlay");
        endOverlay = SpritePool.getInstance().getSprite("sliderendcircleoverlay");
        approachCircle = SpritePool.getInstance().getSprite("approachcircle");
        startArrow = SpritePool.getInstance().getSprite("reversearrow");
        endArrow = SpritePool.getInstance().getSprite("reversearrow");
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final PointF pos, final float offset, final float time, final float r, final float g,
                     final float b, final float scale, int num, final int sound, final int repeats,
                     final float length, final String data, final TimingPoint timing,
                     final String customSound, final String tempSound, final boolean isFirstNote, final double realTime) {
        init(listener, scene, pos, offset, time, r, g, b, scale, num, sound, repeats, length, data, timing, customSound, tempSound, isFirstNote, realTime, null);
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final PointF pos, final float offset, final float time, final float r, final float g,
                     final float b, final float scale, int num, final int sound, final int repeats,
                     final float length, final String data, final TimingPoint timing,
                     final String customSound, final String tempSound, final boolean isFirstNote, final double realTime,
                     SliderPath sliderPath) {
        this.listener = listener;
        this.scene = scene;
        this.timing = timing;
        this.scale = scale;
        this.pos = pos;
        passedTime = -time;
        preTime = time;
        if (sliderPath != null) {
            path = sliderPath;
        } else {
            if (length < 0) {
                path = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                        data.split("[|]"), 0, offset);
            } else {
                path = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                        data.split("[|]"), length, offset);
            }
        }

        num += 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            num %= 10;
        }
        number = GameObjectPool.getInstance().getNumber(num);


        TimingControlPoint timingPoint = GameHelper.controlPoints.getTimingPointAt(realTime);
        double speedMultiplier = GameHelper.controlPoints.getDifficultyPointAt(realTime).getSpeedMultiplier();

        double scoringDistance = GameHelper.getSpeed() * speedMultiplier;
        double velocity = scoringDistance / timingPoint.getBeatLength();
        double spanDuration = length / velocity;
        //fixed negative length silder bug
        if (spanDuration <= 0) {
            spanDuration = 0;
        }

        maxTime = (float) (spanDuration / 1000);
        ball = null;
        followcircle = null;
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
        color.set(r, g, b);
        borderColor.set(r, g, b);
        if (!OsuSkin.get().isSliderBorderFollowComboColor()) {
            borderColor = new RGBColor(OsuSkin.get().getSliderBorderColor());
        }
        if (!OsuSkin.get().isSliderFollowComboColor()) {
            color = new RGBColor(OsuSkin.get().getSliderBodyColor());
        }

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
        startCircle.setColor(r, g, b);
        startCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, startCircle);

        startOverlay.setScale(scale);
        startOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, startOverlay);

        approachCircle.setColor(r, g, b);
        approachCircle.setScale(scale * 2);
        approachCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, approachCircle);
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && isFirstNote);
        }

        // End circle
        final int lastIndex = path.points.size() - 1;
        final PointF endPos = path.points.get(lastIndex);

        endCircle.setScale(scale);
        endCircle.setColor(r, g, b);
        endCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(endPos, endCircle);

        endOverlay.setScale(scale);
        endOverlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(endPos, endOverlay);

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
        if (GameHelper.isHidden()) {
            number.init(scene, pos, scale,
                    new SequenceEntityModifier(new FadeInModifier(time / 4 * GameHelper.getTimeMultiplier()),
                            new FadeOutModifier(time / 4 * GameHelper.getTimeMultiplier())));
        } else {
            number.init(scene, pos, scale, new FadeInModifier(
                    time / 2 * GameHelper.getTimeMultiplier()));
        }
        scene.attachChild(startCircle, 0);
        scene.attachChild(approachCircle);
        scene.attachChild(endOverlay, 0);
        // Repeat arrow at end
        if (repeatCount > 1) {
            //endArrow.setPosition(endPos.x, endPos.y);
            endArrow.setAlpha(0);
            endArrow.setScale(scale);
            endArrow.setRotation(MathUtils.radToDeg(Utils.direction(
                    path.points.get(lastIndex), path.points.get(lastIndex - 1))));
            Utils.putSpriteAnchorCenter(endPos, endArrow);
            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(endCircle, 0);

        // Ticks
        // Try to fix incorrect slider tick count bug
        tickInterval = timing.getBeatLength() * speedMultiplier;
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
            final Sprite tick = SpritePool.getInstance().getCenteredSprite(
                    "sliderscorepoint",
                    getPercentPosition((float) (i * tickInterval
                            / (maxTime * GameHelper.getTickRate())), null));
            tick.setScale(scale);
            tick.setAlpha(0);
            ticks.add(tick);
            scene.attachChild(tick, 0);
        }

        // Slider track
        superPath = new LinePath();
        for (PointF p : path.points) {
            superPath.add(new Vec2(p.x, p.y));
        }
        superPath.measure();
        superPath.bufferLength(path.length.get(path.length.size() - 1));
        superPath = superPath.fitToLinePath();
        superPath.measure();

        abstractSliderBody = new SliderBody2D(superPath);
        abstractSliderBody.setBodyWidth(
                Utils.toRes(OsuSkin.get().getSliderBodyWidth() - OsuSkin.get().getSliderBorderWidth())
                        * scale);
        abstractSliderBody.setBorderWidth(Utils.toRes(OsuSkin.get().getSliderBodyWidth()) * scale);
        abstractSliderBody.setSliderBodyBaseAlpha(OsuSkin.get().getSliderBodyBaseAlpha());

        if (OsuSkin.get().isSliderHintEnable()) {
            if (length > OsuSkin.get().getSliderHintShowMinLength()) {
                abstractSliderBody.setEnableHint(true);
                abstractSliderBody.setHintAlpha(OsuSkin.get().getSliderHintAlpha());
                abstractSliderBody.setHintWidth(Utils.toRes(OsuSkin.get().getSliderHintWidth()));
                RGBColor hintColor = OsuSkin.get().getSliderHintColor();
                if (hintColor != null) {
                    abstractSliderBody.setHintColor(hintColor.r(), hintColor.g(), hintColor.b());
                } else {
                    abstractSliderBody.setHintColor(color.r(), color.g(), color.b());
                }
            }
        }

        abstractSliderBody.applyToScene(scene, Config.isComplexAnimations());
        abstractSliderBody.setBodyColor(color.r(), color.g(), color.b());
        abstractSliderBody.setBorderColor(borderColor.r(), borderColor.g(), borderColor.g());
    }

    private PointF getPercentPosition(final float percentage, final Float angle) {
        if (percentage >= 1) {
            tmpPoint.set(path.points.get(path.points.size() - 1).x,
                    path.points.get(path.points.size() - 1).y);
            return tmpPoint;
        } else if (percentage <= 0) {
            if (angle != null) {
                ballAngle = MathUtils.radToDeg(Utils.direction(
                        path.points.get(1), path.points.get(0)));
            }
            tmpPoint.set(path.points.get(0).x, path.points.get(0).y);
            return tmpPoint;
        }

        if (path.length.size() == 1) {
            final PointF p = tmpPoint;
            p.x = path.points.get(0).x * percentage + path.points.get(1).x
                    * (1 - percentage);
            p.y = path.points.get(0).y * percentage + path.points.get(1).y
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
            // Well, i should just add flag "save angle" instead of passing
            // PointF, but... screw it
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
            abstractSliderBody.removeFromScene(scene);
        }
        startCircle.detachSelf();
        endCircle.detachSelf();
        startOverlay.detachSelf();
        endOverlay.detachSelf();
        approachCircle.detachSelf();
        startArrow.detachSelf();
        endArrow.detachSelf();
        ball.detachSelf();
        followcircle.detachSelf();
        SpritePool.getInstance().putAnimSprite("sliderb", ball);
        SpritePool.getInstance().putSprite("sliderfollowcircle", followcircle);
        for (final Sprite sp : ticks) {
            sp.detachSelf();
            SpritePool.getInstance().putSprite("sliderscorepoint", sp);
        }
        listener.removeObject(this);
        // Put this and number into pool
        GameHelper.putPath(path);
        GameObjectPool.getInstance().putSlider(this);
        GameObjectPool.getInstance().putNumber(number);
        scene = null;
        isHiddenFadeOutActive = false;
    }

    private void over() {
        //int type = repeatCount > 0 ? GameObjectListener.SLIDER_REPEAT : GameObjectListener.SLIDER_END;
        repeatCount--;
        // If alpha > 0 means cursor in slider ball bounds
        if (followcircle.getAlpha() > 0 && replayObjectData == null ||
                replayObjectData != null && replayObjectData.tickSet.get(tickIndex)) {
            if (soundIdIndex < soundId.length)
                Utils.playHitSound(listener, soundId[soundIdIndex],
                        sampleSet[soundIdIndex], addition[soundIdIndex]);
            ticksGot++;
            tickSet.set(tickIndex++, true);
            if (repeatCount > 0) {
                listener.onSliderHit(id, 30, null,
                        path.points.get(reverse ? 0 : path.points.size() - 1),
                        false, color, GameObjectListener.SLIDER_REPEAT);
            }
        } else {
            tickSet.set(tickIndex++, false);
            if (repeatCount > 0) {
                listener.onSliderHit(id, -1, null,
                        path.points.get(reverse ? 0 : path.points.size() - 1),
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
            //if (repeatCount > 1) {
            ((GameScene) listener).onSliderReverse(
                    path.points.get(!reverse ? 0 : path.points.size() - 1),
                    reverse ? endArrow.getRotation() : startArrow.getRotation()
            );
            //}
            // and go on...
            if (passedTime >= maxTime) {
                over();
            }
            return;
        }
        // Calculating score
        int score = 0;
        if (ticksGot > 0) {
            score = 50;
        }
        if (ticksGot >= ticksTotal / 2) {
            score = 100;
        }
        if (ticksGot >= ticksTotal) {
            score = 300;
        }
        // If slider was in reverse mode, we should swap start and end points
        if (reverse) {
            Slider.this.listener.onSliderHit(id, score,
                    path.points.get(path.points.size() - 1),
                    path.points.get(0), endsCombo, color, GameObjectListener.SLIDER_END);
        } else {
            Slider.this.listener.onSliderHit(id, score, path.points.get(0),
                    path.points.get(path.points.size() - 1), endsCombo, color, GameObjectListener.SLIDER_END);
        }
        if (!startHit) {
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) * 1000 + 13);
        }
        listener.onSliderEnd(id, firstHitAccuracy, tickSet);
        // Remove slider from scene
        SyncTaskManager.getInstance().run(this::removeFromScene);
    }

    private boolean isHit() {
        float radius = Utils.sqr(Utils.toRes(64) * scale);
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (listener.isMousePressed(this, i)
                    && Utils.squaredDistance(path.points.get(0), listener.getMousePos(i)) <= radius) {
                return true;
            } else if (GameHelper.isAutopilotMod() && listener.isMousePressed(this, i)) {
                return true;
            } else if (GameHelper.isRelaxMod() && passedTime >= 0 &&
                    Utils.squaredDistance(path.points.get(0), listener.getMousePos(i)) <= radius) {
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

        if (startHit == false) // If we didn't get start hit(click)
        {
            // If it's too late, mark this hit missing
            if (passedTime > GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                startHit = true;
                listener.onSliderHit(id, -1, null, path.points.get(0), false, color, GameObjectListener.SLIDER_START);
                firstHitAccuracy = (int) (passedTime * 1000);
            } else if (autoPlay && passedTime >= 0) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, path.points.get(0), false, color, GameObjectListener.SLIDER_START);
            } else if (replayObjectData != null &&
                    Math.abs(replayObjectData.accuracy / 1000f) <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) &&
                    passedTime + dt / 2 > replayObjectData.accuracy / 1000f) {
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                listener.onSliderHit(id, 30, null, path.points.get(0), false, color, GameObjectListener.SLIDER_START);
            } else {
                if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) // if
                // we
                // clicked
                {
                    listener.registerAccuracy(passedTime);
                    startHit = true;
                    Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                    ticksGot++;
                    firstHitAccuracy = (int) (passedTime * 1000);
                    listener.onSliderHit(id, 30, null, path.points.get(0),
                            false, color, GameObjectListener.SLIDER_START);
                }
            }
        }

        if (GameHelper.isKiai()) {
            final float kiaiModifier = Math
                    .max(0,
                            1 - GameHelper.getGlobalTime()
                                    / GameHelper.getKiaiTickLength()) * 0.50f;

            final float r = Math.min(1, color.r() + (1 - color.r())
                    * kiaiModifier);
            final float g = Math.min(1, color.g() + (1 - color.g())
                    * kiaiModifier);
            final float b = Math.min(1, color.b() + (1 - color.b())
                    * kiaiModifier);
            kiai = true;
            startCircle.setColor(r, g, b);
            endCircle.setColor(r, g, b);
        } else if (kiai == true) {
            startCircle.setColor(color.r(), color.g(), color.b());
            endCircle.setColor(color.r(), color.g(), color.b());
            kiai = false;
        }

        if (passedTime < 0) // we at approach time
        {
            float percentage = 1 + passedTime / preTime;
            // calculating size of approach circle
            approachCircle.setScale(scale
                    * (1 + 2f * (1 - percentage)));
            if (startHit == true) {
                approachCircle.setAlpha(0);
            }
            if (percentage <= 0.5f) {
                // Following core doing a very cute show animation ^_^"
                percentage = Math.min(1, percentage * 2);
                if (startHit == false) {
                    approachCircle.setAlpha(percentage);
                }


                startCircle.setAlpha(percentage);
                startOverlay.setAlpha(percentage);
                endCircle.setAlpha(percentage);
                endOverlay.setAlpha(percentage);
                for (int i = 0; i < ticks.size(); i++) {
                    if (percentage > (float) (i + 1) / ticks.size()) {
                        ticks.get(i).setAlpha(1);
                    }
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(percentage);
                }

                if (Config.isComplexAnimations()) {
                    float l = superPath.getMeasurer().maxLength() * percentage;

                    abstractSliderBody.setEndLength(l);
                    abstractSliderBody.onUpdate();

                    tmpPoint = getPercentPosition(percentage, null);

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }
            } else if (percentage - dt / preTime <= 0.5f) {
                // Setting up positions of slider parts
                approachCircle.setAlpha(1);
                startCircle.setAlpha(1);
                startOverlay.setAlpha(1);
                endCircle.setAlpha(1);
                endOverlay.setAlpha(1);
                for (final Sprite sp : ticks) {
                    sp.setAlpha(1);
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(1);
                }
                if (Config.isComplexAnimations()) {
                    if (!preStageFinish) {
                        abstractSliderBody.setEndLength(superPath.getMeasurer().maxLength());
                        abstractSliderBody.onUpdate();
                        preStageFinish = true;
                    }

                    tmpPoint = path.points.get(path.points.size() - 1);

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }

            }
            return;
        } else {
            startCircle.setAlpha(0);
            startOverlay.setAlpha(0);

            // Slider body, border, and hint gradually fade in Hidden mod
            if (GameHelper.isHidden()) {
                hiddenFadeOut();
            }
        }

        if (ball == null) // if ball still don't exists
        {
            number.detach(true);
            approachCircle.setAlpha(0);

            ball = SpritePool.getInstance().getAnimSprite("sliderb",
                    SkinManager.getFrames("sliderb"));
            ball.setFps(0.1f * GameHelper.getSpeed() * scale
                    / timing.getBeatLength());
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);

            followcircle = SpritePool.getInstance().getSprite(
                    "sliderfollowcircle");
            followcircle.setScale(scale);

            scene.attachChild(ball);
            scene.attachChild(followcircle);
        }
        // Ball positiong
        final float percentage = passedTime / maxTime;
        final PointF ballpos = getPercentPosition(reverse ? 1 - percentage
                : percentage, ballAngle);
        // Calculating if cursor in follow circle bounds
        final float radius = Utils.toRes(128) * scale;
        boolean inRadius = false;
        for (int i = 0; i < listener.getCursorsCount(); i++) {

            if (autoPlay != false
                    || (listener.isMouseDown(i) != false
                    && !(Utils.squaredDistance(listener.getMousePos(i), ballpos) > radius * radius))) {
                inRadius = true;
                break;
            }

            if (GameHelper.isAutopilotMod() && listener.isMouseDown(i))
                inRadius = true;
        }
        followcircle.setAlpha(inRadius ? 1 : 0);
        listener.onTrackingSliders(inRadius);

        tickTime += dt;
        //try fixed big followcircle bug
        float fcscale = scale * (1.1f - 0.1f * tickTime * GameHelper.getTickRate() / timing.getBeatLength());
        if (fcscale <= scale * 1.1f && fcscale >= scale * -1.1f) {
            followcircle.setScale(fcscale);
        }
        // Some magic with slider ticks. If it'll crash it's not my fault ^_^"
        while (ticks.size() > 0 && percentage < 1 - 0.02f / maxTime
                && tickTime * GameHelper.getTickRate() > tickInterval) {
            tickTime -= tickInterval / GameHelper.getTickRate();
            if (followcircle.getAlpha() > 0 && replayObjectData == null ||
                    replayObjectData != null && replayObjectData.tickSet.get(tickIndex)) {
                Utils.playHitSound(listener, 16);
                listener.onSliderHit(id, 10, null, ballpos, false, color, GameObjectListener.SLIDER_TICK);
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
        followcircle.setPosition(ballpos.x - followcircle.getWidth() / 2,
                ballpos.y - followcircle.getHeight() / 2);
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

    private void hiddenFadeOut() {
        if (isHiddenFadeOutActive) {
            return;
        }
        isHiddenFadeOutActive = true;
        final float realDuration = maxTime * repeatCount * GameHelper.getTimeMultiplier();
        final EaseQuadOut easing = EaseQuadOut.getInstance();
        startCircle.registerEntityModifier(new AlphaModifier(realDuration,
                startCircle.getAlpha(), 0, easing));
        startOverlay.registerEntityModifier(new AlphaModifier(realDuration,
                startOverlay.getAlpha(), 0, easing));
        endCircle.registerEntityModifier(new AlphaModifier(realDuration,
                endCircle.getAlpha(), 0, easing));
        endOverlay.registerEntityModifier(new AlphaModifier(realDuration,
                endOverlay.getAlpha(), 0, easing));
        if (abstractSliderBody != null) {
            abstractSliderBody.fadeOut(realDuration);
        }
    }

    @Override
    public void tryHit(final float dt) {
        if (startHit == false) // If we didn't get start hit(click)
        {
            if (isHit() && -passedTime < GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) // if
            // we
            // clicked
            {
                listener.registerAccuracy(passedTime);
                startHit = true;
                Utils.playHitSound(listener, soundId[0], sampleSet[0], addition[0]);
                ticksGot++;
                firstHitAccuracy = (int) (passedTime * 1000);
                listener.onSliderHit(id, 30, null, path.points.get(0),
                        false, color, GameObjectListener.SLIDER_START);
            }
            if (passedTime < 0) // we at approach time
            {
                if (startHit == true) {
                    approachCircle.setAlpha(0);
                }
            }
        }
    }

}
