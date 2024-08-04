package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.osu.Execution;
import com.reco1l.osu.graphics.Modifiers;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreNumber;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class Spinner extends GameObject {
    private final Sprite background;
    public final PointF center;
    private final Sprite circle;
    private final Sprite approachCircle;
    private final Sprite metre;
    private final Sprite spinText;
    private final TextureRegion mregion;
    private final Sprite clearText;
    private PointF oldMouse;
    private GameObjectListener listener;
    private Scene scene;
    private int fullrotations = 0;
    private float rotations = 0;
    private float needRotations;
    private boolean clear = false;
    private int soundId;
    private int sampleSet;
    private int addition;
    private ScoreNumber bonusScore = null;
    private int score = 1;
    private float metreY;
    private StatisticV2 stat;
    private float totalTime;
    private boolean did = false;

    private final PointF currMouse = new PointF();


    public Spinner() {
        ResourceManager.getInstance().checkSpinnerTextures();
        this.pos = new PointF((float) Constants.MAP_WIDTH / 2, (float) Constants.MAP_HEIGHT / 2);
        center = Utils.trackToRealCoords(pos);
        background = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-background"));
        final float scaleX = Config.getRES_WIDTH() / background.getWidth();
        background.setScale(scaleX);

        circle = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-circle"));
        mregion = ResourceManager.getInstance().getTexture("spinner-metre")
                .deepCopy();
        metre = new Sprite(center.x - (float) Config.getRES_WIDTH() / 2,
                Config.getRES_HEIGHT(), mregion);
        metre.setWidth(Config.getRES_WIDTH());
        metre.setHeight(background.getHeightScaled());
        approachCircle = new CentredSprite(center.x, center.y, ResourceManager.getInstance().getTexture("spinner-approachcircle"));
        spinText = new CentredSprite(center.x, center.y * 1.5f, ResourceManager
                .getInstance().getTexture("spinner-spin"));

        clearText = new CentredSprite(center.x, center.y * 0.5f, ResourceManager.getInstance().getTexture("spinner-clear"));
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final float pretime, final float time, final float rps,
                     final int sound, final String tempSound, final StatisticV2 stat) {
        fullrotations = 0;
        rotations = 0;
        this.scene = scene;
        needRotations = rps * time;
        if(time < 0.05f) needRotations = 0.1f;
        this.listener = listener;
        this.soundId = sound;
        this.sampleSet = 0;
        this.addition = 0;
        this.stat = stat;
        this.totalTime = time;
        startHit = true;
        clear = false;
        if(totalTime <= 0f) clear = true;
        bonusScore = null;
        score = 1;
        ResourceManager.getInstance().checkSpinnerTextures();

        if (!Utils.isEmpty(tempSound)) {
            final String[] group = tempSound.split(":");
            this.sampleSet = Integer.parseInt(group[0]);
            this.addition = Integer.parseInt(group[1]);
        }

        background.setAlpha(0);
        background.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(pretime * 0.75f),
            Modifiers.fadeIn(pretime * 0.25f)
        ));

        circle.setAlpha(0);
        circle.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(pretime * 0.75f),
            Modifiers.fadeIn(pretime * 0.25f)
        ));

        metreY = (Config.getRES_HEIGHT() - background.getHeightScaled()) / 2;
        metre.setAlpha(0);
        metre.registerEntityModifier(Modifiers.sequence(
            Modifiers.delay(pretime * 0.75f),
            Modifiers.fadeIn(pretime * 0.25f)
        ));
        mregion.setTexturePosition(0, (int) metre.getHeightScaled());

        approachCircle.setAlpha(0);
        if (GameHelper.isHidden()) {
            approachCircle.setVisible(false);
        }
        approachCircle.registerEntityModifier(
            Modifiers.sequence(
                Modifiers.delay(pretime),
                Modifiers.parallel(
                    Modifiers.alpha(time, 0.75f, 1),
                    Modifiers.scale(time, 2.0f, 0)
                )
            ).setOnFinished(entity -> Execution.updateThread(Spinner.this::removeFromScene))
        );

        spinText.setAlpha(0);
        spinText.registerEntityModifier(
            Modifiers.sequence(
                Modifiers.delay(pretime * 0.75f),
                Modifiers.fadeIn(pretime * 0.25f),
                Modifiers.delay(pretime / 2),
                Modifiers.fadeOut(pretime * 0.25f)
            )
        );

        scene.attachChild(spinText, 0);
        scene.attachChild(approachCircle, 0);
        scene.attachChild(circle, 0);
        scene.attachChild(metre, 0);
        scene.attachChild(background, 0);

        oldMouse = null;

    }

    void removeFromScene() {
        if (clearText != null) {
            scene.detachChild(clearText);
        }
        scene.detachChild(spinText);
        scene.detachChild(background);
        approachCircle.detachSelf();
        scene.detachChild(circle);
        scene.detachChild(metre);
        // GameObjectPool.getInstance().putSpinner(this);

        if (bonusScore != null) {
            bonusScore.detachFromScene(scene);
        }
        listener.removeObject(Spinner.this);
        int score = 0;
        if (replayObjectData != null) {
            //int bonusRot = (int) (replayData.accuracy / 4 - needRotations + 1);
            //while (bonusRot < 0) {
            //    bonusRot++;
            //    listener.onSpinnerHit(id, 1000, false, 0);
            //}

            //if (rotations count < the rotations in replay), let rotations count = the rotations in replay
            while (fullrotations + this.score < replayObjectData.accuracy / 4 + 1){
                fullrotations++;
                listener.onSpinnerHit(id, 1000, false, 0);
            }
            if (fullrotations >= needRotations)
                clear = true;
        }
        float percentfill = (Math.abs(rotations) + fullrotations)
                / needRotations;
        if(needRotations <= 0.1f){
            clear = true;
            percentfill = 1;
        }
        if (percentfill > 0.9f) {
            score = 50;
        }
        if (percentfill > 0.95f) {
            score = 100;
        }
        if (clear) {
            score = 300;
        }
        if (replayObjectData != null) {
            switch (replayObjectData.accuracy % 4) {
                case 0:
                    score = 0;
                    break;
                case 1:
                    score = 50;
                    break;
                case 2:
                    score = 100;
                    break;
                case 3:
                    score = 300;
                    break;
            }
        }
        listener.onSpinnerHit(id, score, endsCombo, this.score + fullrotations - 1);
        if (score > 0) {
            Utils.playHitSound(listener, soundId);
        }
    }


    @Override
    public void update(final float dt) {
        if (circle.getAlpha() == 0) {
            return;
        }
        PointF mouse = null;

        for (int i = 0, count = listener.getCursorsCount(); i < count; ++i) {
            if (mouse == null) {
                if (autoPlay) {
                    mouse = center;
                } else if (listener.isMouseDown(i)) {
                    mouse = listener.getMousePos(i);
                } else {
                    continue;
                }
                currMouse.set(mouse.x - center.x, mouse.y - center.y);
            }

            if (oldMouse == null || listener.isMousePressed(this, i)) {
                if (oldMouse == null) {
                    oldMouse = new PointF();
                }
                oldMouse.set(currMouse);
                return;
            }
        }

        if (mouse == null)
            return;

        circle.setRotation(MathUtils.radToDeg(Utils.direction(currMouse)));

        var len1 = Utils.length(currMouse);
        var len2 = Utils.length(oldMouse);
        var dfill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2);

        if (Math.abs(len1) < 0.0001f || Math.abs(len2) < 0.0001f)
            dfill = 0;

        if (autoPlay) {
            dfill = 5 * 4 * dt;
            circle.setRotation((rotations + dfill / 4f) * 360);
            //auto时，FL光圈绕中心旋转
            if (GameHelper.isAuto() || GameHelper.isAutopilotMod()) {
               float angle = (rotations + dfill / 4f) * 360;
               float pX = center.x + 50 * (float)Math.sin(angle);
               float pY = center.y + 50 * (float)Math.cos(angle);
               listener.updateAutoBasedPos(pX, pY);
            }
        }
        rotations += dfill / 4f;
        float percentfill = (Math.abs(rotations) + fullrotations)
                / needRotations;

        if (percentfill > 1 || clear) {
            percentfill = 1;
            if (!clear) {
                clearText.registerEntityModifier(Modifiers.fadeIn(0.25f));
                clearText.registerEntityModifier(Modifiers.scale(0.25f, 1.5f, 1));
                scene.attachChild(clearText);
                clear = true;
            } else if (Math.abs(rotations) > 1) {
                if (bonusScore != null) {
                    scene.detachChild(bonusScore);
                }
                rotations -= 1 * Math.signum(rotations);
                bonusScore = new ScoreNumber(center.x, center.y + 100,
                        String.valueOf(score * 1000), 1.1f, true);
                listener.onSpinnerHit(id, 1000, false, 0);
                score++;
                scene.attachChild(bonusScore);
                ResourceManager.getInstance().getSound("spinnerbonus").play();
                float rate = 0.375f;
                if (GameHelper.getDrain() > 0) {
                    rate = 1 + (GameHelper.getDrain() / 4f);
                }
                stat.changeHp(rate * 0.01f * totalTime / needRotations);
            }
        } else if (Math.abs(rotations) > 1) {
            rotations -= 1 * Math.signum(rotations);
            if (replayObjectData == null || replayObjectData.accuracy / 4 > fullrotations) {
                fullrotations++;
                stat.registerSpinnerHit();
                float rate = 0.375f;
                if (GameHelper.getDrain() > 0) {
                    rate = 1 + (GameHelper.getDrain() / 2f);
                }
                stat.changeHp(rate * 0.01f * totalTime / needRotations);
            }
        }
        metre.setPosition(metre.getX(),
                metreY + metre.getHeight() * (1 - Math.abs(percentfill)));
        mregion.setTexturePosition(0,
                (int) (metre.getBaseHeight() * (1 - Math.abs(percentfill))));

        oldMouse.set(currMouse);
    }
}
