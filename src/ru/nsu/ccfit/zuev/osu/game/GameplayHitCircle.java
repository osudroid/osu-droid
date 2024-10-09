package ru.nsu.ccfit.zuev.osu.game;

import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Anchor;
import com.reco1l.osu.playfield.NumberedCirclePiece;
import com.rian.osu.beatmap.hitobject.HitCircle;
import com.rian.osu.mods.ModHidden;

import org.anddev.andengine.entity.scene.Scene;

import javax.annotation.Nullable;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameplayHitCircle extends GameObject {

    private final RGBColor comboColor = new RGBColor();
    private HitCircle beatmapCircle;
    private GameObjectListener listener;
    private Scene scene;
    private float radiusSquared;
    private float passedTime;
    private float timePreempt;
    private boolean kiai;

    /**
     * The circle piece that represents the circle body and overlay.
     */
    private final NumberedCirclePiece circlePiece;

    /**
     * The approach circle that appears before the hit circle.
     */
    @Nullable
    private ExtendedSprite approachCircle;


    public GameplayHitCircle() {
        circlePiece = new NumberedCirclePiece("hitcircle", "hitcircleoverlay");
    }

    public void init(final GameObjectListener listener, final Scene pScene,
                     final HitCircle beatmapCircle, final float secPassed,
                     final RGBColor comboColor) {
        // Storing parameters into fields
        replayObjectData = null;
        this.beatmapCircle = beatmapCircle;

        var stackedPosition = beatmapCircle.getGameplayStackedPosition();
        position.set(stackedPosition.x, stackedPosition.y);

        endsCombo = beatmapCircle.isLastInCombo();
        this.listener = listener;
        scene = pScene;
        timePreempt = (float) beatmapCircle.timePreempt / 1000;

        passedTime = secPassed - ((float) beatmapCircle.startTime / 1000 - timePreempt);
        startHit = false;
        kiai = GameHelper.isKiai();
        this.comboColor.set(comboColor.r(), comboColor.g(), comboColor.b());

        // Calculating position of top/left corner for sprites and hit radius
        final float scale = beatmapCircle.getGameplayScale();
        radiusSquared = (float) beatmapCircle.getGameplayRadius();
        radiusSquared *= radiusSquared;

        // Initializing sprites
        circlePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
        circlePiece.setScale(scale);
        circlePiece.setAlpha(0f);
        circlePiece.setPosition(this.position.x, this.position.y);
        circlePiece.setNumberText(beatmapCircle.getIndexInCurrentCombo() + 1);
        circlePiece.setNumberScale(OsuSkin.get().getComboTextScale());

        float fadeInDuration = (float) beatmapCircle.timeFadeIn / 1000f;

        if (GameHelper.isHidden()) {
            float fadeOutDuration = timePreempt * (float) ModHidden.FADE_OUT_DURATION_MULTIPLIER;

            circlePiece.beginSequenceChain(s -> {
                s.fadeIn(fadeInDuration);
                s.fadeOut(fadeOutDuration);
            });
        } else {
            circlePiece.fadeIn(fadeInDuration);
        }

        if (Config.isDimHitObjects()) {
            // Source: https://github.com/peppy/osu/blob/60271fb0f7e091afb754455f93180094c63fc3fb/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuHitObject.cs#L101
            var dimDelaySec = timePreempt - objectHittableRange;
            var colorDim = 195f / 255f;

            circlePiece.setColor(colorDim, colorDim, colorDim);
            circlePiece.delay(dimDelaySec).colorTo(1f, 1f, 1f, 0.1f);
        }

        scene.attachChild(circlePiece, 0);

        if (!GameHelper.isHidden() || Config.isShowFirstApproachCircle() && beatmapCircle.isFirstNote()) {

            if (approachCircle == null) {
                approachCircle = new ExtendedSprite();
                approachCircle.setOrigin(Anchor.Center);
                approachCircle.setTextureRegion(ResourceManager.getInstance().getTexture("approachcircle"));
            }

            approachCircle.setPosition(this.position.x, this.position.y);
            approachCircle.setColor(comboColor.r(), comboColor.g(), comboColor.b());
            approachCircle.setScale(scale * 3);
            approachCircle.setAlpha(0f);

            approachCircle.fadeTo(0.9f, fadeInDuration);
            approachCircle.scaleTo(scale, timePreempt);

            scene.attachChild(approachCircle);
        }
    }

    private void playSound() {
        listener.playSamples(beatmapCircle);
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }

        circlePiece.clearEntityModifiers();

        if (approachCircle != null) {
            approachCircle.clearEntityModifiers();
            approachCircle.detachSelf();
        }

        // Detach all objects
        circlePiece.detachSelf();
        listener.removeObject(this);
        GameObjectPool.getInstance().putCircle(this);
        scene = null;
    }

    private boolean canBeHit() {
        return passedTime >= Math.max(0, timePreempt - objectHittableRange);
    }

    private boolean isHit() {
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radiusSquared;
            if (GameHelper.isRelaxMod() && passedTime - timePreempt >= 0 && inPosition) {
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
        // 因为这里是阻塞队列, 所以提前点的地方会影响判断
        for (int i = 0, count = listener.getCursorsCount(); i < count; i++) {

            var inPosition = Utils.squaredDistance(position, listener.getMousePos(i)) <= radiusSquared;
            if (GameHelper.isRelaxMod() && passedTime - timePreempt >= 0 && inPosition) {
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
        // PassedTime < 0 means circle logic is over
        if (passedTime < 0) {
            removeFromScene();
            return;
        }
        // If we have clicked circle
        if (replayObjectData != null) {
            if (passedTime - timePreempt + dt / 2 > replayObjectData.accuracy / 1000f) {
                final float acc = Math.abs(replayObjectData.accuracy / 1000f);
                if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                    playSound();
                }
                listener.registerAccuracy(replayObjectData.accuracy / 1000f);
                passedTime = -1;
                // Remove circle and register hit in update thread
                listener.onCircleHit(id, replayObjectData.accuracy / 1000f, position,endsCombo, replayObjectData.result, comboColor);
                removeFromScene();
                return;
            }
        } else if (isHit() && canBeHit()) {
            float signAcc = passedTime - timePreempt;
            if (Config.isFixFrameOffset()) {
                signAcc += (float) hitOffsetToPreviousFrame() / 1000f;
            }
            final float acc = Math.abs(signAcc);
            if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                playSound();
            }
            listener.registerAccuracy(signAcc);
            passedTime = -1;
            // Remove circle and register hit in update thread
            float finalSignAcc = signAcc;
            startHit = true;
            listener.onCircleHit(id, finalSignAcc, position, endsCombo, (byte) 0, comboColor);
            removeFromScene();
            return;
        }

        if (GameHelper.isKiai()) {
            var kiaiModifier = (float) Math.max(0, 1 - GameHelper.getCurrentBeatTime() / GameHelper.getBeatLength()) * 0.5f;
            var r = Math.min(1, comboColor.r() + (1 - comboColor.r()) * kiaiModifier);
            var g = Math.min(1, comboColor.g() + (1 - comboColor.g()) * kiaiModifier);
            var b = Math.min(1, comboColor.b() + (1 - comboColor.b()) * kiaiModifier);
            kiai = true;
            circlePiece.setCircleColor(r, g, b);
        } else if (kiai) {
            circlePiece.setCircleColor(comboColor.r(), comboColor.g(), comboColor.b());
            kiai = false;
        }

        passedTime += dt;

        // We are still at approach time. Let entity modifiers finish first.
        if (passedTime < timePreempt) {
            return;
        }

        if (autoPlay) {
            playSound();
            passedTime = -1;
            // Remove circle and register hit in update thread
            listener.onCircleHit(id, 0, position, endsCombo, ResultType.HIT300.getId(), comboColor);
            removeFromScene();
        } else {
            if (approachCircle != null) {
                approachCircle.clearEntityModifiers();
                approachCircle.setAlpha(0);
            }

            // If passed too much time, counting it as miss
            if (passedTime > timePreempt + GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                passedTime = -1;
                final byte forcedScore = (replayObjectData == null) ? 0 : replayObjectData.result;

                removeFromScene();
                listener.onCircleHit(id, 10, position, false, forcedScore, comboColor);
            }
        }
    } // update(float dt)

    @Override
    public void tryHit(final float dt) {
        if (isHit() && canBeHit()) {
            float signAcc = passedTime - timePreempt;
            if (Config.isFixFrameOffset()) {
                signAcc += (float) hitOffsetToPreviousFrame() / 1000f;
            }
            final float acc = Math.abs(signAcc);
            if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getOverallDifficulty())) {
                playSound();
            }
            listener.registerAccuracy(signAcc);
            passedTime = -1;
            // Remove circle and register hit in update thread
            float finalSignAcc = signAcc;
            listener.onCircleHit(id, finalSignAcc, position, endsCombo, (byte) 0, comboColor);
            removeFromScene();
        }
    }

}
