package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import java.util.Arrays;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class Utils {
    private static final String FSReservedChars = "|\\?*<\":>+[]/";
    private static int accSign = 0;

    public static <T> T[] oneObjectArray(T object, T[] ary) {
        Arrays.fill(ary, object);
        return ary;
    }

    public static void setAccelerometerSign(final int sign) {
        accSign = sign;
    }

    public static int getAccselerometerSign() {
        return accSign;
    }

    public static float sqr(final float x) {
        return (x * x);
    }

    public static PointF inter(final PointF v1, final PointF v2,
                               final float percent) {
        return new PointF(v1.x * (1 - percent) + v2.x * percent, v1.y
                * (1 - percent) + v2.y * percent);
    }

    public static void putSpriteAnchorCenter(float x, float y, Sprite sprite) {
        final TextureRegion tex = sprite.getTextureRegion();
        sprite.setPosition(x - tex.getWidth() / 2f, y - tex.getHeight() / 2f);
    }

    public static void putSpriteAnchorCenter(PointF pos, Sprite sprite) {
        final TextureRegion tex = sprite.getTextureRegion();
        sprite.setPosition(pos.x - tex.getWidth() / 2f, pos.y - tex.getHeight() / 2f);
    }

    public static PointF trackToRealCoords(final PointF coords) {
        return trackToRealCoords(coords, false);
    }

    public static PointF trackToRealCoords(final PointF pos, final boolean flipVertically) {

        // Utils.scaleToReal(coords);
        pos.x *= Constants.MAP_ACTUAL_WIDTH / (float) Constants.MAP_WIDTH;
        pos.y *= Constants.MAP_ACTUAL_HEIGHT / (float) Constants.MAP_HEIGHT;

        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f;
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f;

        if (flipVertically) {
            flipRealCoordsVertically(pos);
        }
        return pos;
    }

    public static void flipRealCoordsVertically(final PointF coords) {
        coords.y -= Config.getRES_HEIGHT() / 2f;
        coords.y *= -1;
        coords.y += Config.getRES_HEIGHT() / 2f;
    }

    public static void changeTrackToRealCoords(final PointF coords) {
        final PointF pos = scaleToRealC(coords);
        pos.y += (Config.getRES_HEIGHT() - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f;
        pos.x += (Config.getRES_WIDTH() - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f;
        if (GameHelper.isHardrock()) {
            pos.y -= (float) Config.getRES_HEIGHT() / 2;
            pos.y *= -1;
            pos.y += (float) Config.getRES_HEIGHT() / 2;
        }
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
        pos.y -= (height - toRes((isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT))) / 2f;
        pos.x -= (width - toRes((isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH))) / 2f;
        return scaleToTrack(pos, isOld);
    }

    public static short flipY(final short y) {
        final int height = Config.getRES_HEIGHT() / 2;
        return (short) (((y - height) * -1) + height);
    }

    public static PointF scaleToRealC(final PointF v) {
        v.x *= toRes(Constants.MAP_ACTUAL_WIDTH) / (float) Constants.MAP_WIDTH;
        v.y *= toRes(Constants.MAP_ACTUAL_HEIGHT)
                / (float) Constants.MAP_HEIGHT;
        return v;
    }

    public static PointF scaleToTrack(final PointF v, boolean isOld) {
        final PointF pos = new PointF(v.x, v.y);
        pos.x *= Constants.MAP_WIDTH
                / toRes((float) (isOld ? Constants.MAP_ACTUAL_WIDTH_OLD : Constants.MAP_ACTUAL_WIDTH));
        pos.y *= Constants.MAP_HEIGHT
                / toRes((float) (isOld ? Constants.MAP_ACTUAL_HEIGHT_OLD : Constants.MAP_ACTUAL_HEIGHT));
        return pos;
    }

    public static float direction(final float x, final float y) {
        float len = (float) Math.sqrt(x * x + y * y);

        if (len == 0) {
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

    public static PointF normalize(final PointF vector) {
        final float len = length(vector);
        if (Math.abs(len) < 0.0001f) {
            return new PointF(0, 0);
        }
        return new PointF(vector.x / len, vector.y / len);
    }

    public static float distance(final PointF v1, final PointF v2) {
        return MathUtils.distance(v1.x, v1.y, v2.x, v2.y);
    }

    public static float squaredDistance(final PointF v1, final PointF v2) {
        return ((v2.x - v1.x) * (v2.x - v1.x) + (v2.y - v1.y) * (v2.y - v1.y));
    }

    public static float squaredDistance(final float v1x, final float v1y,
                                        final float v2x, final float v2y) {
        return ((v2x - v1x) * (v2x - v1x) + (v2y - v1y) * (v2y - v1y));
    }

    public static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    public static int toRes(final int i) {
        return i / Config.getTextureQuality();
    }

    public static float toRes(final float i) {
        return i / Config.getTextureQuality();
    }

    public static String toFSValidString(final String s) {
        final StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (FSReservedChars.indexOf(s.charAt(i)) == -1) {
                nameBuilder.append(s.charAt(i));
            }
        }
        return nameBuilder.toString();
    }

    /**
     * 判断当前网络是否是wifi网络
     *
     * @param context
     * @return boolean
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    public static float tryParseFloat(String str, float defaultVal) {
        float val;
        try {
            val = Float.parseFloat(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
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

    public static double tryParseDouble(String str, double defaultVal) {
        double val;
        try {
            val = Double.parseDouble(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }
}
