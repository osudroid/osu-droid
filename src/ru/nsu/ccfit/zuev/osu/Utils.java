package ru.nsu.ccfit.zuev.osu;

import android.graphics.PointF;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;

public class Utils {

    private static int accSign = 0;

    private static int soundMask = 0;

    public static float sqr(final float x) {
        return (x * x);
    }

    public static void putSpriteAnchorCenter(PointF pos, Sprite sprite) {
        final TextureRegion tex = sprite.getTextureRegion();
        sprite.setPosition(pos.x - tex.getWidth() / 2f, pos.y - tex.getHeight() / 2f);
    }

    public static PointF trackToRealCoords(final PointF coords) {
        final PointF pos = scaleToReal(coords);
        pos.y += (Config.getRES_HEIGHT() - Constants.MAP_ACTUAL_HEIGHT) / 2f;
        pos.x += (Config.getRES_WIDTH() - Constants.MAP_ACTUAL_WIDTH) / 2f;
        if (GameHelper.isHardrock()) {
            pos.y -= Config.getRES_HEIGHT() / 2f;
            pos.y *= -1;
            pos.y += Config.getRES_HEIGHT() / 2f;
        }
        return pos;
    }

    public static PointF realToTrackCoords(final PointF coords) {
        return realToTrackCoords(coords, Config.getRES_WIDTH(), Config.getRES_HEIGHT(), false);
    }

    public static PointF realToTrackCoords(final PointF coords, float width, float height, boolean isOld) {
        final PointF pos = new PointF(coords.x, coords.y);
        if (GameHelper.isHardrock()) {
            pos.y -= height / 2;
            pos.y *= -1;
            pos.y += height / 2;
        }
        final int i1 = (isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT);
        pos.y -= (height - i1) / 2f;
        final int i = (isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH);
        pos.x -= (width - i) / 2f;
        return scaleToTrack(pos, isOld);
    }

    public static PointF scaleToReal(final PointF v) {
        final PointF pos = new PointF(v.x, v.y);
        pos.x *= Constants.MAP_ACTUAL_WIDTH / (float) Constants.MAP_WIDTH;
        pos.y *= Constants.MAP_ACTUAL_HEIGHT / (float) Constants.MAP_HEIGHT;
        return pos;
    }

    public static PointF scaleToTrack(final PointF v, boolean isOld) {
        final PointF pos = new PointF(v.x, v.y);
        final float i1 = (float) (isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH);
        pos.x *= Constants.MAP_WIDTH / i1;
        final float i = (float) (isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT);
        pos.y *= Constants.MAP_HEIGHT / i;
        return pos;
    }

    public static float direction(final float x, final float y) {
        float len = (float) Math.sqrt(x * x + y * y);
        if (Math.abs(len) < 0.0001f) {
            return 0;
        }
        if (x > 0) {
            len = (float) Math.asin(y / len);
        } else {
            len = (float) (Math.PI - Math.asin(y / len));
        }
        return len;
    }

    public static float direction(final PointF vector) {
        return direction(vector.x, vector.y);
    }

    public static float direction(final PointF v1, final PointF v2) {
        return direction(v2.x - v1.x, v2.y - v1.y);
    }

    public static float length(final PointF vector) {
        return (float) Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    public static float distance(final PointF v1, final PointF v2) {
        return MathUtils.distance(v1.x, v1.y, v2.x, v2.y);
    }

    public static float squaredDistance(final PointF v1, final PointF v2) {
        return ((v2.x - v1.x) * (v2.x - v1.x) + (v2.y - v1.y) * (v2.y - v1.y));
    }

    public static void clearSoundMask() {
        soundMask = 0;
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static void playHitSound(final GameObjectListener listener, final int soundId) {
        playHitSound(listener, soundId, 0, 0);
    }

    public static void playHitSound(final GameObjectListener listener, final int soundId, final int sampleSet, final int addition) {
        if ((soundId & 32) > 0) {
            return;
        }

        if ((soundId & 16) > 0 && (soundMask & 16) == 0) {
            soundMask |= 16;
            listener.playSound("slidertick", sampleSet, addition);
            return;
        }

        if ((soundMask & 1) == 0) {
            soundMask |= 1;
            listener.playSound("hitnormal", sampleSet, addition);
        }
        if ((soundId & 2) > 0 && (soundMask & 2) == 0) {
            soundMask |= 2;
            listener.playSound("hitwhistle", sampleSet, addition);
        }
        if ((soundId & 4) > 0 && (soundMask & 4) == 0) {
            soundMask |= 4;
            listener.playSound("hitfinish", sampleSet, addition);
        }
        if ((soundId & 8) > 0 && (soundMask & 8) == 0) {
            soundMask |= 8;
            listener.playSound("hitclap", sampleSet, addition);
        }
    }

    public static int tryParseInt(String str, int defaultVal) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }

}
