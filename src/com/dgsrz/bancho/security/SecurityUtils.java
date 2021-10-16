package com.dgsrz.bancho.security;

import android.os.Build;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ru.nsu.ccfit.zuev.osu.BuildType;

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

    private static String appSignature = null;
    private static byte[] secretBuffer = null;

    /**
     * @return 得到整数x的非零位个数
     */
    public static int getNonZeroBitsCount(int x) {
        x = (x & 0x55555555) + ((x & 0xaaaaaaaa) >> 1);
        x = (x & 0x33333333) + ((x & 0xcccccccc) >> 2);
        x = (x & 0x0f0f0f0f) + ((x & 0xf0f0f0f0) >> 4);
        x = (x & 0x00ff00ff) + ((x & 0xff00ff00) >> 8);
        x = (x & 0x0000ffff) + ((x & 0xffff0000) >> 16);
        return x;
    }

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


    public static void getAppSignature(Context context, String packageName) {
        if (!BuildType.hasOnlineAccess()) {
            return;
        }
        if (appSignature != null || packageName == null || packageName.length() == 0) {
            return;
        }
        PackageManager pkgMgr = context.getPackageManager();
        PackageInfo info = null;
        Signature[] signatures;

        try {
            if (pkgMgr == null) {
                return;
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signInfo = info.signingInfo;

                if(signInfo.hasMultipleSigners()) {
                    signatures = signInfo.getApkContentsSigners();
                    appSignature = getHashCode(signatures[0].toByteArray());
                }else {
                    signatures = signInfo.getSigningCertificateHistory();
                    appSignature = getHashCode(signatures[0].toByteArray());
                }
            }else {
                info = pkgMgr.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                if(info != null && info.signatures != null && info.signatures.length > 0) {
                    Signature sign = info.signatures[0];
                    appSignature = getHashCode(sign.toByteArray());
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
    }

    /**
     * @return 对请求加签，包含请求文本、APk签名哈希、会话ID
     */
    public static String signRequest(String request) {
        if (appSignature == null) {
            return null;
        }
        try {
            String digest = String.format("%s_%s", appSignature, request);
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(getSecretKey(), mac.getAlgorithm());
            mac.init(secret);
            return toHexString(mac.doFinal(digest.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Unsupported Algorithm");
        }
    }

    private static String getHashCode(byte[] bytes) {
        try {
            MessageDigest digestInst = MessageDigest.getInstance("SHA1");
            digestInst.update(bytes);
            return toHexString(digestInst.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported Algorithm");
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