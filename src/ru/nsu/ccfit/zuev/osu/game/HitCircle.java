package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.scoring.ResultType;

public class HitCircle extends GameObject {
    private final Sprite circle;
    private final Sprite overlay;
    private final Sprite approachCircle;
    private final RGBColor color = new RGBColor();
    private CircleNumber number;
    private float scale;
    private GameObjectListener listener;
    private Scene scene;
    private int soundId;
    private int sampleSet;
    private int addition;
    private String externalSound;
    //private PointF pos;
    private float radius;
    private float passedTime;
    private float time;
    private boolean isFirstNote;
    private boolean kiai;

    public HitCircle() {
        // Getting sprites from sprite pool
        circle = SpritePool.getInstance().getSprite("hitcircle");
        overlay = SpritePool.getInstance().getSprite("hitcircleoverlay");
        approachCircle = SpritePool.getInstance().getSprite("approachcircle");
    }

    public void init(final GameObjectListener listener, final Scene pScene,
                     final PointF pos, final float time, final float r, final float g,
                     final float b, final float scale, int num, final int sound, final String tempSound, final boolean isFirstNote) {
        // Storing parameters into fields
        //Log.i("note-ini", time + "s");
        this.replayObjectData = null;
        this.scale = scale;
        this.pos = pos;
        this.listener = listener;
        this.scene = pScene;
        this.soundId = sound;
        this.sampleSet = 0;
        this.addition = 0;
        // TODO: 外部音效文件支持
        this.externalSound = "";
        this.time = time;
        this.isFirstNote = isFirstNote;
        passedTime = 0;
        startHit = false;
        kiai = GameHelper.isKiai();
        color.set(r, g, b);

        if (!Utils.isEmpty(tempSound)) {
            final String[] group = tempSound.split(":");
            this.sampleSet = Integer.parseInt(group[0]);
            this.addition = Integer.parseInt(group[1]);
            if (group.length > 4) {
                this.externalSound = group[4];
            }
        }

        // Calculating position of top/left corner for sprites and hit radius
        final TextureRegion tex = circle.getTextureRegion();
		/*final PointF rpos = new PointF(pos.x - tex.getWidth() / 2, pos.y
				- tex.getHeight() / 2);*/
        radius = Utils.toRes(128) * scale / 2;
        radius *= radius;

        // Initializing sprites
        //circle.setPosition(rpos.x, rpos.y);
        circle.setColor(r, g, b);
        circle.setScale(scale);
        circle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, circle);

        //overlay.setPosition(rpos.x, rpos.y);
        overlay.setScale(scale);
        overlay.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, overlay);

        //approachCircle.setPosition(rpos.x, rpos.y);
        approachCircle.setColor(r, g, b);
        approachCircle.setScale(scale * 2);
        approachCircle.setAlpha(0);
        Utils.putSpriteAnchorCenter(pos, approachCircle);
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(Config.isShowFirstApproachCircle() && this.isFirstNote);
        }

        // Attach sprites to scene
        scene.attachChild(overlay, 0);
        // and getting new number from sprite pool
        num += 1;
        if (OsuSkin.get().isLimitComboTextLength()) {
            num %= 10;
        }
        number = GameObjectPool.getInstance().getNumber(num);
        if (GameHelper.isHidden()) {
            number.init(scene, pos, GameHelper.getScale(),
                    new SequenceEntityModifier(new FadeInModifier(time / 4 * GameHelper.getTimeMultiplier()),
                            new FadeOutModifier(time * 0.35f * GameHelper.getTimeMultiplier())));
            overlay.registerEntityModifier(new SequenceEntityModifier(new FadeInModifier(time / 4 * GameHelper.getTimeMultiplier()),
                    new FadeOutModifier(time * 0.35f * GameHelper.getTimeMultiplier())));
            circle.registerEntityModifier(new SequenceEntityModifier(new FadeInModifier(time / 4 * GameHelper.getTimeMultiplier()),
                    new FadeOutModifier(time * 0.35f * GameHelper.getTimeMultiplier())));
        } else {
            number.init(scene, pos, GameHelper.getScale(), new FadeInModifier(
                    time / 2 * GameHelper.getTimeMultiplier()));
        }
        scene.attachChild(circle, 0);
        scene.attachChild(approachCircle);
    }

    private void playSound() {
        // Sound is playing only if we hit in time
        if (approachCircle.getScaleX() <= scale * 1.5f) {
            Utils.playHitSound(listener, soundId, sampleSet, addition);
        }
    }

    private void removeFromScene() {
        if (scene == null) {
            return;
        }
        // Detach all objects
        overlay.detachSelf();
        circle.detachSelf();
        approachCircle.detachSelf();
        number.detach(false);
        listener.removeObject(this);
        // Put circle and number into pool
        GameObjectPool.getInstance().putCircle(this);
        GameObjectPool.getInstance().putNumber(number);
        scene = null;
    }

    private boolean isHit() {
        // 因为这里是阻塞队列, 所以提前点的地方会影响判断
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (listener.isMousePressed(this, i)
                    && Utils.squaredDistance(pos, listener.getMousePos(i)) <= radius) {
                return true;
            } else if (GameHelper.isAutopilotMod() && listener.isMousePressed(this, i)) {
                return true;
            } else if (GameHelper.isRelaxMod() && passedTime - time >= 0 &&
                    Utils.squaredDistance(pos, listener.getMousePos(i)) <= radius) {
                return true;
            }
        }
        return false;
    }

    private double hitOffsetToPreviousFrame() {
        // 因为这里是阻塞队列, 所以提前点的地方会影响判断
        for (int i = 0; i < listener.getCursorsCount(); i++) {
            if (listener.isMousePressed(this, i)
                    && Utils.squaredDistance(pos, listener.getMousePos(i)) <= radius) {
                return listener.downFrameOffset(i);
            } else if (GameHelper.isAutopilotMod() && listener.isMousePressed(this, i)) {
                return 0;
            } else if (GameHelper.isRelaxMod() && passedTime - time >= 0 &&
                    Utils.squaredDistance(pos, listener.getMousePos(i)) <= radius) {
                return 0;
            }
        }
        return 0;
    }


    @Override
    public void update(final float dt) {
        // PassedTime < 0 means circle logic is over
        if (passedTime < 0) {
            return;
        }
        // If we have clicked circle
        if (replayObjectData != null) {
            if (passedTime - time + dt / 2 > replayObjectData.accuracy / 1000f) {
                final float acc = Math.abs(replayObjectData.accuracy / 1000f);
                if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                    playSound();
                }
                listener.registerAccuracy(replayObjectData.accuracy / 1000f);
                passedTime = -1;
                // Remove circle and register hit in update thread
                SyncTaskManager.getInstance().run(new Runnable() {
                    public void run() {
                        HitCircle.this.listener
                                .onCircleHit(id, replayObjectData.accuracy / 1000f, pos,
                                        endsCombo, replayObjectData.result, color);
                        removeFromScene();
                    }
                });
                return;
            }
        } else if (passedTime * 2 > time && isHit()) {
            float signAcc = passedTime - time;
            if (Config.isFixFrameOffset()) {
                signAcc += (float) hitOffsetToPreviousFrame() / 1000f;
            }
            final float acc = Math.abs(signAcc);
            //Log.i("note-ini", "signAcc: " + signAcc);
            if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                playSound();
            }
            listener.registerAccuracy(signAcc);
            passedTime = -1;
            // Remove circle and register hit in update thread
            float finalSignAcc = signAcc;
            startHit = true;
            SyncTaskManager.getInstance().run(() -> {
                HitCircle.this.listener
                        .onCircleHit(id, finalSignAcc, pos, endsCombo, (byte) 0, color);
                removeFromScene();
            });
            return;
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
            circle.setColor(r, g, b);
        } else if (kiai == true) {
            circle.setColor(color.r(), color.g(), color.b());
            kiai = false;
        }

        if (autoPlay && passedTime - time >= 0) {
            playSound();
            passedTime = -1;
            // Remove circle and register hit in update thread
            SyncTaskManager.getInstance().run(() -> {
                HitCircle.this.listener.onCircleHit(id, 0, pos, endsCombo, ResultType.HIT300.getId(), color);
                removeFromScene();
            });
            return;
        }


        passedTime += dt;

        // if it's too early to click
        if (passedTime < time) {
            float percentage = passedTime / time;
            // calculating size of approach circle
            approachCircle.setScale(scale * (1 + 2f * (1 - percentage)));
            // and if we just begun
            if (GameHelper.isHidden() == false || (isFirstNote && Config.isShowFirstApproachCircle())) {
                if (passedTime < time / 2) {
                    // calculating alpha of all sprites
                    percentage = passedTime * 2 / time;
                    overlay.setAlpha(percentage);
                    circle.setAlpha(percentage);
                    approachCircle.setAlpha(percentage);
//			} else if (GameHelper.isHidden()) {
//				if (passedTime < time * 0.9f) {
////					percentage = 1 - passedTime / (time * 0.85f);
////					overlay.setAlpha(percentage);
////					circle.setAlpha(percentage);
////				} else {
//					overlay.setAlpha(0);
//					circle.setAlpha(0);
//				}
                } else if (!GameHelper.isHidden())// if circle already has to be shown, set all alphas to 1
                {
                    overlay.setAlpha(1);
                    circle.setAlpha(1);
                    approachCircle.setAlpha(1);
                }
            }
        } else if (autoPlay == false)// if player didn't click circle in time
        {
            approachCircle.setAlpha(0);
            // If passed too many time, counting it as miss
            if (passedTime > time + GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                passedTime = -1;
                final byte forcedScore = (replayObjectData == null) ? 0 : replayObjectData.result;
                SyncTaskManager.getInstance().run(() -> {
                    removeFromScene();
                    HitCircle.this.listener.onCircleHit(id, 10, pos, false, forcedScore, color);
                });
            }
        }
    } // update(float dt)

    @Override
    public void tryHit(final float dt){
        if (passedTime * 2 > time && isHit()) {
            float signAcc = passedTime - time;
            if (Config.isFixFrameOffset()) {
                signAcc += (float) hitOffsetToPreviousFrame() / 1000f;
            }
            final float acc = Math.abs(signAcc);
            //Log.i("note-ini", "signAcc: " + signAcc);
            if (acc <= GameHelper.getDifficultyHelper().hitWindowFor50(GameHelper.getDifficulty())) {
                playSound();
            }
            listener.registerAccuracy(signAcc);
            passedTime = -1;
            // Remove circle and register hit in update thread
            float finalSignAcc = signAcc;
            SyncTaskManager.getInstance().run(() -> {
                HitCircle.this.listener
                        .onCircleHit(id, finalSignAcc, pos, endsCombo, (byte) 0, color);
                removeFromScene();
            });
            
        }
    }
}
