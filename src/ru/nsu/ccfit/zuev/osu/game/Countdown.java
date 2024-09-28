package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Anchor;

import org.anddev.andengine.entity.modifier.DelayModifier;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;

public class Countdown extends GameObject {
    public static final float COUNTDOWN_LENGTH = 3f;
    private final ExtendedSprite ready;
    private final Sprite count1, count2, count3;
    private final ExtendedSprite go;
    private final GameObjectListener listener;
    private final float speed;
    private float timepassed;
    private Scene scene;

    public Countdown(final GameObjectListener listener, final Scene scene,
                     final float speed, final float offset, final float time) {
        this.listener = listener;
        this.speed = speed;
        this.scene = scene;
        timepassed = -time + COUNTDOWN_LENGTH * speed;
        final PointF center = Utils.trackToRealCoords(new PointF((float) Constants.MAP_WIDTH / 2, (float) Constants.MAP_HEIGHT / 2));

        ready = new ExtendedSprite();
        ready.setOrigin(Anchor.Center);
        ready.setPosition(center.x, center.y);
        ready.setTextureRegion(ResourceManager.getInstance().getTexture("ready"));

        ready.registerEntityModifier(new SequenceEntityModifier(
                new ParallelEntityModifier(new FadeInModifier(COUNTDOWN_LENGTH
                        * speed / 9), new RotationModifier(COUNTDOWN_LENGTH
                        * speed / 9, -90, 0)), new DelayModifier(
                COUNTDOWN_LENGTH * speed / 9),
                new ParallelEntityModifier(new FadeOutModifier(COUNTDOWN_LENGTH
                        * speed / 9), new ScaleModifier(COUNTDOWN_LENGTH
                        * speed / 9, 1, 1.5f))));
        ready.setRotation(-90);
        ready.setVisible(false);
        ready.setIgnoreUpdate(true);

        count3 = new Sprite(0, 0, ResourceManager.getInstance().getTexture(
                "count3"));
        count3.setPosition(0, center.y - count3.getHeight() / 2);
        count3.setVisible(false);
        count3.setIgnoreUpdate(true);
        count3.registerEntityModifier(new SequenceEntityModifier(
                new FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                new DelayModifier(COUNTDOWN_LENGTH * speed * 8 / 18),
                new FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)));

        count2 = new Sprite(0, 0, ResourceManager.getInstance().getTexture(
                "count2"));
        count2.setPosition(Config.getRES_WIDTH() - count2.getWidth(), center.y
                - count2.getHeight() / 2);
        count2.setVisible(false);
        count2.setIgnoreUpdate(true);
        count2.registerEntityModifier(new SequenceEntityModifier(
                new FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                new DelayModifier(COUNTDOWN_LENGTH * speed * 5 / 18),
                new FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)));

        count1 = new Sprite(0, 0, ResourceManager.getInstance().getTexture(
                "count1"));
        count1.setPosition(center.x - count1.getWidth() / 2,
                center.y - count1.getHeight() / 2);
        count1.setVisible(false);
        count1.setIgnoreUpdate(true);
        count1.registerEntityModifier(new SequenceEntityModifier(
                new FadeInModifier(COUNTDOWN_LENGTH * speed / 18),
                new DelayModifier(COUNTDOWN_LENGTH * speed * 2 / 18),
                new FadeOutModifier(COUNTDOWN_LENGTH * speed / 18)));

        go = new ExtendedSprite();
        go.setOrigin(Anchor.Center);
        go.setPosition(center.x, center.y);
        go.setTextureRegion(ResourceManager.getInstance().getTexture("go"));

        go.registerEntityModifier(new SequenceEntityModifier(
                new ParallelEntityModifier(new FadeInModifier(COUNTDOWN_LENGTH
                        * speed / 18), new RotationModifier(COUNTDOWN_LENGTH
                        * speed / 18, -180, 0)), new DelayModifier(
                COUNTDOWN_LENGTH * speed / 18), new FadeOutModifier(
                COUNTDOWN_LENGTH * speed / 18)));
        go.setRotation(-180);
        go.setVisible(false);
        go.setIgnoreUpdate(true);

        scene.attachChild(ready, 0);
        scene.attachChild(go, 0);
        scene.attachChild(count1, 0);
        scene.attachChild(count2, 0);
        scene.attachChild(count3, 0);
    }

    private void playIfNotNull(String resname) {
        var sound = ResourceManager.getInstance().getCustomSound(resname, 1);
        if (sound != null)
            sound.play();
    }

    @Override
    public void update(final float dt) {
        if (scene == null) {
            return;
        }
        timepassed += dt;

        if (timepassed >= 0 && timepassed - dt < 0) {
            playIfNotNull("readys");
            ready.setVisible(true);
            ready.setIgnoreUpdate(false);
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 2 / 6
                && timepassed - dt < COUNTDOWN_LENGTH * speed * 2 / 6) {
            playIfNotNull("count3s");
            count3.setVisible(true);
            count3.setIgnoreUpdate(false);
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 3 / 6
                && timepassed - dt < COUNTDOWN_LENGTH * speed * 3 / 6) {
            playIfNotNull("count2s");
            count2.setVisible(true);
            count2.setIgnoreUpdate(false);
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 4 / 6
                && timepassed - dt < COUNTDOWN_LENGTH * speed * 4 / 6) {
            playIfNotNull("count1s");
            count1.setVisible(true);
            count1.setIgnoreUpdate(false);
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed * 5 / 6
                && timepassed - dt < COUNTDOWN_LENGTH * speed * 5 / 6) {
            playIfNotNull("gos");
            go.setVisible(true);
            go.setIgnoreUpdate(false);
        }

        if (timepassed >= COUNTDOWN_LENGTH * speed
                && timepassed - dt < COUNTDOWN_LENGTH * speed) {
            scene = null;
            listener.removePassiveObject(Countdown.this);
            ready.detachSelf();
            go.detachSelf();
            count1.detachSelf();
            count2.detachSelf();
            count3.detachSelf();
        }
    }

}
