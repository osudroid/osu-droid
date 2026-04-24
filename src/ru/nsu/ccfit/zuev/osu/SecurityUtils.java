package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import kotlin.io.ByteStreamsKt;

/**
 * 加解密工具类
 *
 * @author dgsrz (dgsrz@vip.qq.com)
 */
public final class SecurityUtils {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final int[] BIT_MASK = {0x7f, 0x3f, 0x1f, 0x0f, 0x07, 0x03, 0x01};
    private static final int[] COMPRESSED = new int[]{
            0xd7, 0x32, 0x73, 0xc, 0x6b, 0x96, 0x1,
            0xd4, 0xf7, 0x8, 0x36, 0xaf, 0x87, 0x0
    };  // WeLc0MeTo#0su!

    private static byte[] secretBuffer = null;

    private static final String flashlightCursorHash = "3b3afff3dab87f214053ded3163ff4e91cc3474e";
    private static final String flashlightDimLayerHash = "59dfd45eecdfbeb7a91761a7af4b3e0162d13e9f";

    /**
     * @return 得到整数x高20位对3389取模后的结果
     */
    public static int getHigh16Bits(int x) {
        return (x >> 12) % 3389;
    }

    /**
     * @return 7-bit解码
     */
    public static byte[] getSecretKey() {
        if (secretBuffer == null) {
            int highest = 0;
            secretBuffer = new byte[COMPRESSED.length];
            for (int i = 0; i < COMPRESSED.length; i++) {
                int index = i % 7;
                highest = (index == 0) ? 0 : highest;
                int lowest = COMPRESSED[i] & BIT_MASK[index];
                secretBuffer[i] = (byte) ((lowest << index) | highest);
                highest = (COMPRESSED[i] & (0xff ^ BIT_MASK[index])) >> (7 - index);
            }
        }
        return secretBuffer;
    }

    public static boolean verifyFileIntegrity(Context context) {
        return verifyFileIntegrity(context, "flashlight_cursor.png", flashlightCursorHash)
                && verifyFileIntegrity(context, "flashlight_dim_layer.png", flashlightDimLayerHash);
    }

    private static boolean verifyFileIntegrity(Context context, String fileName, String hash) {
        try (var in = context.getAssets().open(fileName)) {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(getSecretKey(), mac.getAlgorithm());
            mac.init(secret);
            byte[] bytes = ByteStreamsKt.readBytes(in);

            return toHexString(mac.doFinal(bytes)).equals(hash);
        } catch (IOException e) {
            Log.e("SecurityUtils", "Failed to check file integrity for " + fileName, e);

            return false;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHexString(byte[] bytes) {
        int buffSize = bytes.length;
        StringBuilder sb = new StringBuilder(buffSize << 1);

        for (byte v : bytes) {
            sb.append(HEX_DIGITS[(v >>> 4) & 0x0f]);
            sb.append(HEX_DIGITS[v & 0x0f]);
        }
        return sb.toString();
    }
}