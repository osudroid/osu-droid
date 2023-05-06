package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.edlplan.andengine.TrianglePack;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.osu.support.slider.SliderBody2D;
import com.edlplan.osu.support.timing.controlpoint.TimingControlPoint;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.sprite.batch.SpriteGroup;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadIn;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.ListIterator;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper.SliderPath;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.polygon.Polygon;

public class Slider extends GameObject {
    private final Sprite startCircle, endCircle;
    private final Sprite startOverlay, endOverlay;
    private final Sprite approachCircle;
    private final Sprite startArrow, endArrow;
    private final ArrayList<Sprite> trackSprites = new ArrayList<>();
    private final ArrayList<Sprite> trackBorders = new ArrayList<>();
    private final ArrayList<Sprite> ticks = new ArrayList<>();
    private final ArrayList<Sprite> trackBoundaries = new ArrayList<>();
    private PointF startPosition, endPosition;
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
    private Sprite followCircle;

    private PointF tmpPoint = new PointF();
    private Float ballAngle = 0f;
    private SpriteGroup group = null;
    private SpriteGroup borderGroup = null;
    private Polygon trackPoly = null;
    private Polygon borderPoly = null;
    private float[] trackPolyVerts = null;
    private float[] borderPolyVerts = null;

    private boolean kiai;
    private RGBColor color = new RGBColor();

    //for replay
    private int firstHitAccuracy;
    private final BitSet tickSet = new BitSet();
    private int tickIndex;

    private TrianglePack body = null, border = null;
    private LinePath superPath = null;
    private boolean preStageFinish = false;

    private SliderBody2D abstractSliderBody = null;

    private boolean
            mIsOver,
            mWasInRadius;

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
        init(listener,scene,pos,offset,time,r,g,b,scale,num,sound,repeats,length,data,timing,customSound,tempSound,isFirstNote,realTime,null);
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
        if (sliderPath != null){
            path = sliderPath;
        } else{
                if (length < 0){
                    path = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                    data.split("[|]"), 0, offset);
                }
                else {
                    path = GameHelper.calculatePath(Utils.realToTrackCoords(pos),
                    data.split("[|]"), length, offset);
                }
            //}
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
        if(spanDuration <= 0){
            spanDuration = 0;
        }

        mIsOver = false;
        mWasInRadius = false;

        maxTime = (float) (spanDuration / 1000);
        ball = null;
        followCircle = null;
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
        startPosition = pos;
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
        PointF endPos = pos;
        if (!path.points.isEmpty()) {
            endPos = path.points.get(path.points.size() - 1);
        }
        endCircle.setScale(scale);
        endCircle.setColor(r, g, b);
        endCircle.setAlpha(0);
        endPosition = endPos;
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

        float fadeInDuration;

        if (GameHelper.isHidden()) {
            fadeInDuration = time * 0.4f * GameHelper.getTimeMultiplier();
            float fadeOutDuration = time * 0.3f * GameHelper.getTimeMultiplier();

            number.init(scene, pos, scale, new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration)
            ));

            startCircle.registerEntityModifier(new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration)
            ));

            startOverlay.registerEntityModifier(new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration)
            ));

            endCircle.registerEntityModifier(new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeInDuration)
            ));

            endOverlay.registerEntityModifier(new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration)
            ));

        } else {
            // Preempt time can go below 450ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
            // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear function above.
            // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
            // This adjustment is necessary for AR>10, otherwise TimePreempt can become smaller leading to hitcircles not fully fading in.
            fadeInDuration = 0.4f * Math.min(1, time / ((float) GameHelper.ar2ms(10) / 1000)) * GameHelper.getTimeMultiplier();

            number.init(scene, pos, scale, new FadeInModifier(fadeInDuration));

            startCircle.registerEntityModifier(new FadeInModifier(fadeInDuration));
            startOverlay.registerEntityModifier(new FadeInModifier(fadeInDuration));
            endCircle.registerEntityModifier(new FadeInModifier(fadeInDuration));
            endOverlay.registerEntityModifier(new FadeInModifier(fadeInDuration));
        }
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
            Utils.putSpriteAnchorCenter(endPos, endArrow);
            scene.attachChild(endArrow, 0);
        }
        scene.attachChild(endCircle, 0);

        tickInterval = timing.getBeatLength() * speedMultiplier;
        int tickCount = (int) (maxTime * GameHelper.getTickRate() / tickInterval);
        if(Double.isNaN(tickInterval) || tickInterval < GameHelper.getSliderTickLength() / 1000){
            tickCount = 0;
        }
        if ((maxTime * GameHelper.getTickRate() / tickInterval)
        - (int) (maxTime * GameHelper.getTickRate() / tickInterval) < 0.001f){
            tickCount--;
        }
        ticks.clear();
        for (int i = 1; i <= tickCount; i++) {
            final Sprite tick = SpritePool.getInstance().getCenteredSprite(
                    "sliderscorepoint",
                    getPercentPosition((float)(i * tickInterval
                            / (maxTime * GameHelper.getTickRate())), null));
            tick.setScale(scale);
            tick.setAlpha(0);
            ticks.add(tick);
            scene.attachChild(tick, 0);
        }

        // Slider track
        if (!path.points.isEmpty()) {
            if (Config.isUseSuperSlider()) {
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

                abstractSliderBody.applyToScene(scene, Config.isSnakingInSliders());
                abstractSliderBody.setBodyColor(color.r(), color.g(), color.b());
                RGBColor scolor = GameHelper.getSliderColor();
                abstractSliderBody.setBorderColor(scolor.r(), scolor.g(), scolor.b());

            } else {
                initLowPolyTrack();
            }
        }

        applyBodyFadeAdjustments(fadeInDuration);
    }

    private void initLowPolyTrack() {
        final TextureRegion tex = startCircle.getTextureRegion();
        final PointF startPos = new PointF(startPosition.x
                - tex.getWidth() / 2f, startPosition.y - tex.getHeight()
                / 2f);
        trackPolyVerts = createPolygon(Utils.toRes(54) * scale);
        float[] verts;
        if (Config.isSnakingInSliders()) {
            verts = new float[trackPolyVerts.length];
            for (int i = 0; i < verts.length; i++) {
                verts[i] = trackPolyVerts[(i % 2)];
            }
        } else {
            verts = trackPolyVerts;
            trackPolyVerts = null;
        }
        trackPoly = new Polygon(0, 0, verts);
        trackPoly.setColor(color.r(), color.g(), color.b());
        scene.attachChild(trackPoly, 0);

        trackBoundaries.clear();
        for (final Integer i : path.boundIndexes) {
            final Sprite sprite = SpritePool.getInstance()
                    .getSprite("::track2");
            if (!Config.isSnakingInSliders()) {
                sprite.setPosition(
                        path.points.get(i).x - sprite.getWidth() / 2,
                        path.points.get(i).y - sprite.getHeight() / 2);
            } else {
                sprite.setPosition(startPos.x, startPos.y);
            }
            sprite.setScale(scale);
            sprite.setColor(color.r(), color.g(), color.b());
            sprite.setAlpha(0.7f);
            scene.attachChild(sprite, 0);
            trackBoundaries.add(sprite);
        }

        if (Config.isSliderBorders()) {
            final RGBColor scolor = GameHelper.getSliderColor();

            borderPolyVerts = createPolygon(Utils.toRes(57)
                    * scale);
            if (Config.isSnakingInSliders()) {
                verts = new float[borderPolyVerts.length];
                for (int i = 0; i < verts.length; i++) {
                    verts[i] = borderPolyVerts[(i % 2)];
                }
            } else {
                verts = borderPolyVerts;
                borderPolyVerts = null;
            }
            borderPoly = new Polygon(0, 0, verts);
            borderPoly.setColor(scolor.r(), scolor.g(), scolor.b());
            scene.attachChild(borderPoly, 0);
        }
    }

    private float[] createPolygon(final float size) {
        int vi = 0;

        final LinkedList<PointF> points = new LinkedList<>();
        for (int i = 0; i < path.points.size(); i++) {
            points.add(getPointPos(i, -1, size));
            points.add(getPointPos(i, 1, size));
        }

        final ArrayList<PointF> addPoints = new ArrayList<>();
        final ListIterator<PointF> iterator = points.listIterator();
        final float sqrDist = Utils
                .sqr(((scale * Constants.HIGH_SLIDER_STEP)) - 1);
        for (int i = 0; i < points.size(); i++) {
            if (i + 2 >= points.size()) {
                break;
            }
            if (!iterator.hasNext()) {
                break;
            }
            final PointF p1 = iterator.next();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            if (!iterator.hasNext()) {
                break;
            }
            final PointF p2 = iterator.next();
            iterator.previous();
            iterator.previous();

            if (!addPoints.isEmpty()) {
                for (int j = 0; j < addPoints.size(); j++) {
                    iterator.add(addPoints.get(j));
                    iterator.add(Utils.inter(p1, p2, (j + 1)
                            / (float) (addPoints.size() + 1)));
                }
                addPoints.clear();
            }

            int numPoints = (int) (Utils.squaredDistance(p1, p2) / sqrDist);
            if (numPoints < 1) {
                continue;
            }
            numPoints = (int) Math.sqrt(numPoints);

            for (int j = 0; j < numPoints; j++) {
                final float t = (j + 1) / (float) (numPoints + 1);
                final PointF p = Utils.inter(p1, p2, t);
                final int idx = i / 2;
                final float dx = path.points.get(idx).x * (1 - t)
                        + path.points.get(idx + 1).x * t;
                final float dy = path.points.get(idx).y * (1 - t)
                        + path.points.get(idx + 1).y * t;
                p.x -= dx;
                p.y -= dy;
                float len = Utils.length(p);
                len = size / len;
                if (Math.abs(len - 1) < 0.025f) {
                    continue;
                }
                p.x *= len;
                p.y *= len;
                p.x += dx;
                p.y += dy;

                addPoints.add(p);
            }
        }

        final float[] verts = new float[points.size() * 2];
        for (final PointF p : points) {
            verts[vi++] = p.x;
            verts[vi++] = p.y;
        }
        return verts;
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

    private PointF getPointPos(final int i, final int sign, final float size) {
        if (path.points.isEmpty()) {
            return startPosition;
        }

        int j = i;
        if (j + 1 >= path.points.size()) {
            j = path.points.size() - 2;
        }
        float nx = -path.points.get(j + 1).y + path.points.get(j).y;
        float ny = path.points.get(j + 1).x - path.points.get(j).x;
        if (sign < 0) {
            nx *= -1;
            ny *= -1;
        }
        final PointF p = new PointF();
        p.x = path.points.get(i).x;
        p.y = path.points.get(i).y;
        final float nlen = (float) Math.sqrt(nx * nx + ny * ny);
        if (nlen > 0) {
            nx /= nlen;
            ny /= nlen;
            nx *= size;
            ny *= size;
            p.x += nx;
            p.y += ny;
        }
        return p;
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }
        // Detach all objects
        if (group != null) {
            group.detachSelf();
        }
        if (trackPoly != null) {
            trackPoly.detachSelf();
        }
        if (borderPoly != null) {
            borderPoly.detachSelf();
        }
        if (borderGroup != null) {
            borderGroup.detachSelf();
        }
        if (body != null) {
            body.detachSelf();
        }
        if (border != null) {
            border.detachSelf();
        }
        if (abstractSliderBody != null) {
            if (GameHelper.isHidden())
            {
                abstractSliderBody.removeFromScene(scene);
            }
            else
            {
                abstractSliderBody.removeFromScene(scene, 0.24f * GameHelper.getTimeMultiplier());
            }
        }

        ball.registerEntityModifier(new FadeOutModifier(0.1f * GameHelper.getTimeMultiplier(), new IEntityModifier.IEntityModifierListener()
        {
            @Override public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {}

            @Override public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem)
            {
                SyncTaskManager.getInstance().run(pItem::detachSelf);
            }
        }));

        if (!Config.isComplexAnimations())
        {
            followCircle.detachSelf();
        }
        startCircle.detachSelf();
        endCircle.detachSelf();
        startOverlay.detachSelf();
        endOverlay.detachSelf();
        approachCircle.detachSelf();
        startArrow.detachSelf();
        endArrow.detachSelf();
        SpritePool.getInstance().putAnimSprite("sliderb", ball);
        SpritePool.getInstance().putSprite("sliderfollowcircle", followCircle);
        for (final Sprite sp : trackSprites) {
            sp.detachSelf();
            SpritePool.getInstance().putSprite("::track", sp);
        }
        for (final Sprite sp : trackBoundaries) {
            sp.detachSelf();
            SpritePool.getInstance().putSprite("::track2", sp);
        }
        trackBoundaries.clear();
        for (final Sprite sp : trackBorders) {
            sp.detachSelf();
            SpritePool.getInstance().putSprite("::trackborder", sp);
        }
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
            if (passedTime >= maxTime){
                over();
            }
            return;
        }
        mIsOver = true;

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
                    endPosition, startPosition, endsCombo, color, GameObjectListener.SLIDER_END);
        } else {
            Slider.this.listener.onSliderHit(id, score, startPosition,
                    endPosition, endsCombo, color, GameObjectListener.SLIDER_END);
        }
        if(!startHit){
            firstHitAccuracy = (int) (GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty()) * 1000 + 13);
        }
        listener.onSliderEnd(id, firstHitAccuracy, tickSet);
        // Remove slider from scene

        if (Config.isComplexAnimations() && mWasInRadius)
        {
            followCircle.clearEntityModifiers();
            followCircle.registerEntityModifier(new ParallelEntityModifier(
                    new ScaleModifier(0.2f * GameHelper.getTimeMultiplier(), followCircle.getScaleX(), followCircle.getScaleX() * 0.8f, EaseQuadOut.getInstance()),
                    new AlphaModifier(0.2f * GameHelper.getTimeMultiplier(), followCircle.getAlpha(), 0f, new IEntityModifier.IEntityModifierListener()
                    {
                        @Override
                        public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem)
                        {

                        }

                        @Override
                        public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem)
                        {
                            SyncTaskManager.getInstance().run(pItem::detachSelf);
                        }
                    }, EaseQuadIn.getInstance())
            ));
        }

        SyncTaskManager.getInstance().run(this::removeFromScene);
    }

    private float getPercentPositionOnTrack(final float[] verts,
                                            final float percent, final int i) {
        if (i <= 3) {
            return verts[i];
        }
        if (percent >= 1) {
            return verts[i];
        }
        if (percent <= 0) {
            return verts[i % 4];
        }
        float t = percent * (i / 4f);
        int index = (int) (t);
        t -= index;
        index = index * 4 + i % 4;
        return verts[index] + (verts[index + 4] - verts[index]) * t;
    }

    private boolean isHit() {
        float radius = Utils.sqr(Utils.toRes(64) * scale);
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (listener.isMousePressed(this, i)
                    && Utils.squaredDistance(startPosition, listener.getMousePos(i)) <= radius) {
                return true;
            } else if (GameHelper.isAutopilotMod() && listener.isMousePressed(this, i)) {
                return true;
            } else if (GameHelper.isRelaxMod() && passedTime >= 0 &&
                    Utils.squaredDistance(startPosition, listener.getMousePos(i)) <= radius) {
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
            if (group != null) {
                group.setColor(r, g, b);
            }
            if (trackPoly != null) {
                trackPoly.setColor(r, g, b);
            }
            if (body != null) {
                body.setColor(r, g, b);
            }
            for (final Sprite sp : trackBoundaries) {
                sp.setColor(r, g, b);
            }
        } else if (kiai) {
            startCircle.setColor(color.r(), color.g(), color.b());
            endCircle.setColor(color.r(), color.g(), color.b());
            if (group != null) {
                group.setColor(color.r(), color.g(), color.b());
            }
            if (trackPoly != null) {
                trackPoly.setColor(color.r(), color.g(), color.b());
            }
            kiai = false;
            for (final Sprite sp : trackBoundaries) {
                sp.setColor(color.r(), color.g(), color.b());
            }
        }

        if (passedTime < 0) // we at approach time
        {
            float percentage = 1 + passedTime / preTime;
            // calculating size of approach circle
            approachCircle.setScale(scale * (1 + 2f * (1 - percentage)));
            if (startHit)
            {
                approachCircle.setAlpha(0);
            }
            if (percentage <= 0.5f) {
                // Following core doing a very cute show animation ^_^"
                percentage = Math.min(1, percentage * 2);
                if (!startHit) {
                    approachCircle.setAlpha(percentage);
                }

                for (int i = 0; i < ticks.size(); i++) {
                    if (percentage > (float) (i + 1) / ticks.size()) {
                        ticks.get(i).setAlpha(1);
                    }
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(percentage);
                }

                if (Config.isSnakingInSliders()) {
                    //final float offset = startCircle.getWidth() / 2;
                    if (Config.isUseSuperSlider()) {
                        if (superPath != null && abstractSliderBody != null) {
                            float l = superPath.getMeasurer().maxLength() * percentage;

                            abstractSliderBody.setEndLength(l);
                            abstractSliderBody.onUpdate();
                        }
                    } else {
                        tmpPoint = getPercentPosition(percentage, null);
                        for (int i = 0; i < path.boundIndexes.size(); i++) {
                            final float ppos = path.boundIndexes.get(i)
                                    / (float) path.points.size();
                            PointF tpoint = tmpPoint;
                            if (percentage >= ppos) {
                                tpoint = path.points.get(path.boundIndexes
                                        .get(i));
                            }
                            Utils.putSpriteAnchorCenter(tpoint, trackBoundaries.get(i));
                            if (!trackBorders.isEmpty()) {
                                Utils.putSpriteAnchorCenter(tpoint, trackBorders.get(i));
                            }
                        }
                        if (trackPolyVerts != null) {
                            final float[] verts = trackPoly.getVertices();
                            for (int i = 0; i < verts.length; i++) {
                                verts[i] = getPercentPositionOnTrack(
                                        trackPolyVerts, percentage, i);
                            }
                            trackPoly.updateShape();
                        }
                        if (borderPolyVerts != null) {
                            final float[] verts = borderPoly.getVertices();
                            for (int i = 0; i < verts.length; i++) {
                                verts[i] = getPercentPositionOnTrack(
                                        borderPolyVerts, percentage, i);
                            }
                            borderPoly.updateShape();
                        }
                    }

                    tmpPoint = getPercentPosition(percentage, null);

                    Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }
            } else if (percentage - dt / preTime <= 0.5f) {
                // Setting up positions of slider parts
                approachCircle.setAlpha(1);
                for (final Sprite sp : ticks) {
                    sp.setAlpha(1);
                }
                if (repeatCount > 1) {
                    endArrow.setAlpha(1);
                }
                if (Config.isSnakingInSliders()) {
                    final float offset = startCircle.getWidth() / 2;
                    if (Config.isUseSuperSlider()) {
                        if (!preStageFinish && superPath != null && abstractSliderBody != null) {
                            abstractSliderBody.setEndLength(superPath.getMeasurer().maxLength());
                            abstractSliderBody.onUpdate();
                            preStageFinish = true;
                        }

                    } else {

                        for (int i = 0; i < path.boundIndexes.size(); i++) {
                            tmpPoint = path.points
                                    .get(path.boundIndexes.get(i));
                            trackBoundaries.get(i).setPosition(
                                    tmpPoint.x - offset, tmpPoint.y - offset);
                            if (!trackBorders.isEmpty()) {
                                trackBorders.get(i).setPosition(
                                        trackBoundaries.get(i));
                            }
                        }
                        if (trackPolyVerts != null) {
                            final float[] verts = trackPoly.getVertices();
                            System.arraycopy(trackPolyVerts, 0, verts, 0, verts.length);
                            trackPoly.updateShape();
                            trackPolyVerts = null;
                        }
                        if (borderPolyVerts != null) {
                            final float[] verts = borderPoly.getVertices();
                            System.arraycopy(borderPolyVerts, 0, verts, 0, verts.length);
                            borderPoly.updateShape();
                            borderPolyVerts = null;
                        }
                    }

                    tmpPoint = endPosition;

                    if (!trackBoundaries.isEmpty()) {
                        endCircle.setPosition(trackBoundaries
                                .get(trackBoundaries.size() - 1));
                    } else {
                        Utils.putSpriteAnchorCenter(tmpPoint, endCircle);
                    }
                    Utils.putSpriteAnchorCenter(tmpPoint, endOverlay);
                    Utils.putSpriteAnchorCenter(tmpPoint, endArrow);
                }

            }
            return;
        } else {
            if (Config.isUseSuperSlider()) {
                startCircle.setAlpha(0);
                startOverlay.setAlpha(0);
            }
        }

        if (ball == null) // if ball still don't exist
        {
            number.detach(true);
            approachCircle.setAlpha(0);

            ball = SpritePool.getInstance().getAnimSprite("sliderb",
                    SkinManager.getFrames("sliderb"));
            ball.setFps(0.1f * GameHelper.getSpeed() * scale
                    / timing.getBeatLength());
            ball.setScale(scale);
            ball.setFlippedHorizontal(false);

            ball.registerEntityModifier(new FadeInModifier(0.1f * GameHelper.getTimeMultiplier()));

            followCircle = SpritePool.getInstance().getSprite("sliderfollowcircle");
            followCircle.setAlpha(0);

            scene.attachChild(ball);
            scene.attachChild(followCircle);
        }
        // Ball positiong
        final float percentage = passedTime / maxTime;
        final PointF ballpos = getPercentPosition(reverse ? 1 - percentage
                : percentage, ballAngle);
        // Calculating if cursor in follow circle bounds
        final float radius = Utils.toRes(128) * scale;
        boolean inRadius = false;
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (autoPlay
                    || (listener.isMouseDown(i) && Utils
                    .squaredDistance(listener.getMousePos(i), ballpos) <= radius
                    * radius)) {
                inRadius = true;
                break;
            }
            if (GameHelper.isAutopilotMod() && listener.isMouseDown(i))
                inRadius = true;
        }
        listener.onTrackingSliders(inRadius);
        tickTime += dt;

        float fcScale = FMath.clamp(
                scale * (1.1f - 0.1f * tickTime * GameHelper.getTickRate() / timing.getBeatLength()),
                scale * -1.1f,
                scale * 1.1f);

        if (Config.isComplexAnimations())
        {
            float remainTime = (maxTime * GameHelper.getTimeMultiplier() * repeatCount) - passedTime;

            if (inRadius && !mWasInRadius)
            {
                mWasInRadius = true;

                // If alpha doesn't equal 0 means that it has been into an animation before
                float initialScale = followCircle.getAlpha() == 0 ? fcScale * 0.5f : followCircle.getScaleX();

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(new ParallelEntityModifier(
                        new ScaleModifier(Math.min(remainTime, 0.18f * GameHelper.getTimeMultiplier()), initialScale, fcScale, EaseQuadOut.getInstance()),
                        new AlphaModifier(Math.min(remainTime, 0.06f * GameHelper.getTimeMultiplier()), followCircle.getAlpha(), 1f)
                ));
            }
            else if (!inRadius && mWasInRadius)
            {
                mWasInRadius = false;

                followCircle.clearEntityModifiers();
                followCircle.registerEntityModifier(new ParallelEntityModifier(
                        new ScaleModifier(0.1f * GameHelper.getTimeMultiplier(), followCircle.getScaleX(), fcScale * 2f),
                        new AlphaModifier(0.1f * GameHelper.getTimeMultiplier(), followCircle.getAlpha(), 0f, new IEntityModifier.IEntityModifierListener()
                        {
                            @Override
                            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem)
                            {

                            }

                            @Override
                            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem)
                            {
                                if (mIsOver)
                                {
                                    SyncTaskManager.getInstance().run(pItem::detachSelf);
                                }
                            }
                        })
                ));
            }
        }
        else
        {
            mWasInRadius = inRadius;
            followCircle.setAlpha(inRadius ? 1 : 0);
            followCircle.setScale(fcScale);
        }

        // Some magic with slider ticks. If it'll crash it's not my fault ^_^"
        while (ticks.size() > 0 && percentage < 1 - 0.02f / maxTime
                && tickTime * GameHelper.getTickRate() > tickInterval) {
            tickTime -= tickInterval / GameHelper.getTickRate();
            if (followCircle.getAlpha() > 0 && replayObjectData == null ||
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
        final EaseQuadOut easing = EaseQuadOut.getInstance();

        if (GameHelper.isHidden()) {
            // New duration from completed fade in to end (before fading out)
            float realFadeInDuration = fadeInDuration / GameHelper.getTimeMultiplier();
            float fadeOutDuration = (maxTime * repeatCount + preTime - realFadeInDuration) * GameHelper.getTimeMultiplier();

            SequenceEntityModifier modifier = new SequenceEntityModifier(
                    new FadeInModifier(fadeInDuration),
                    new FadeOutModifier(fadeOutDuration, easing)
            );

            if (group != null) {
                group.registerEntityModifier(modifier);
            }
            if (trackPoly != null) {
                trackPoly.registerEntityModifier(modifier);
            }
            if (borderPoly != null) {
                borderPoly.registerEntityModifier(modifier);
            }
            if (borderGroup != null) {
                borderGroup.registerEntityModifier(modifier);
            }
            if (body != null) {
                body.registerEntityModifier(modifier);
            }
            if (border != null) {
                border.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackSprites) {
                sp.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackBorders) {
                sp.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackBoundaries) {
                sp.registerEntityModifier(modifier);
            }
            if (abstractSliderBody != null) {
                abstractSliderBody.applyFadeAdjustments(fadeInDuration, fadeOutDuration);
            }
        } else {
            FadeInModifier modifier = new FadeInModifier(fadeInDuration);

            if (group != null) {
                group.registerEntityModifier(modifier);
            }
            if (trackPoly != null) {
                trackPoly.registerEntityModifier(modifier);
            }
            if (borderPoly != null) {
                borderPoly.registerEntityModifier(modifier);
            }
            if (borderGroup != null) {
                borderGroup.registerEntityModifier(modifier);
            }
            if (body != null) {
                body.registerEntityModifier(modifier);
            }
            if (border != null) {
                border.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackSprites) {
                sp.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackBorders) {
                sp.registerEntityModifier(modifier);
            }
            for (final Sprite sp : trackBoundaries) {
                sp.registerEntityModifier(modifier);
            }
            if (abstractSliderBody != null) {
                abstractSliderBody.applyFadeAdjustments(fadeInDuration);
            }
        }
    }

    @Override
    public void tryHit(final float dt){
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
                if (startHit)
                {
                    approachCircle.setAlpha(0);
                }
            }
        }
    }

}
