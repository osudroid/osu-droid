package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.SkinManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;

public class FollowTrack extends GameObject {
    private final ArrayList<Sprite> points = new ArrayList<Sprite>();
    private final int frameCount;
    private GameObjectListener listener;
    private float timeLeft;
    private float time;
    private boolean empty;
    private float approach;

    public FollowTrack() {
        frameCount = SkinManager.getFrames("followpoint");
    }

    public void init(final GameObjectListener listener, final Scene scene,
                     final PointF start, final PointF end, final float time,
                     final float approachtime, final float scale) {
        this.listener = listener;
        this.approach = approachtime;
        timeLeft = time;
        this.time = 0;

        final float dist = Utils.distance(start, end);
        final float angle = (float) Math.atan2(end.y - start.y, end.x - start.x);
        TextureRegion region = ResourceManager.getInstance().getTexture(
                frameCount > 1 ? "followpoint-0" : "followpoint");
        if (region == null) {
            region = ResourceManager.getInstance().getTexture("followpoint");
        }
        final float pointSize = region.getWidth() * scale;
        int count = (int) ((dist - 64 * scale) / pointSize);
        if (count > 0) {
            count--;
        }
        count = Math.min(count, 30);
        if (count <= 0) {
            empty = true;
            GameObjectPool.getInstance().putTrac(this);
            return;
        }
        empty = false;

        points.clear();
        final PointF pos = new PointF();
        float percent;
        for (int i = 0; i < count; i++) {
            percent = 1 - (i + 1) / (float) (count + 1)/* - 32 * scale / dist */;
            pos.x = start.x * percent + end.x * (1 - percent);
            pos.y = start.y * percent + end.y * (1 - percent);
            final Sprite point;
            if (frameCount == 1) {
                point = SpritePool.getInstance().getCenteredSprite(
                        "followpoint", pos);
            } else {
                point = SpritePool.getInstance().getAnimSprite("followpoint-",
                        frameCount);
                point.setPosition(pos.x - pointSize * 0.5f, pos.y - pointSize
                        * 0.5f);
            }
            point.setScale(scale);
            point.setAlpha(0);
            point.setRotation((float) (angle * 180 / Math.PI));
            scene.attachChild(point, 0);
            points.add(point);
        }

        listener.addPassiveObject(this);
    }


    @Override
    public void update(final float dt) {
        if (empty) {
            return;
        }
        time += dt;

        if (timeLeft <= approach) {
            float percent = (time) / (approach * 0.5f);
            if (percent > 1) {
                percent = 1;
            }
            for (int i = 0; i < points.size(); i++) {
                points.get(i).setAlpha(percent);
            }
        } else if (time < timeLeft - approach) {
            float percent = (time) / (timeLeft - approach);
            if (percent > 1) {
                percent = 1;
            }
            for (int i = 0; i < percent * points.size(); i++) {
                points.get(i).setAlpha(1);
            }
            if (percent < 1) {
                points.get((int) (percent * points.size())).setAlpha(
                        percent - (int) percent);
            }
        } else {
            float percent = 1 - (timeLeft - time) / approach;
            if (percent > 1) {
                percent = 1;
            }
            for (int i = 0; i < percent * points.size(); i++) {
                points.get(i).setAlpha(0);
            }
            if (percent >= 0 && percent < 1) {
                points.get((int) (percent * points.size())).setAlpha(
                        1 - percent + (int) percent);
            }
        }

        if (time >= timeLeft) {
            empty = true;
            SyncTaskManager.getInstance().run(new Runnable() {


                public void run() {
                    for (final Sprite sp : points) {
                        sp.detachSelf();
                        if (sp instanceof AnimSprite) {
                            SpritePool.getInstance().putAnimSprite(
                                    "followpoint-", (AnimSprite) sp);
                        } else {
                            SpritePool.getInstance().putSprite("followpoint",
                                    sp);
                        }
                    }
                    listener.removePassiveObject(FollowTrack.this);
                    GameObjectPool.getInstance().putTrac(FollowTrack.this);
                }
            });
        }
    }

}
