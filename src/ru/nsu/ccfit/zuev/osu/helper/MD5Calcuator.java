package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class MD5Calcuator {

    public static String getFileMD5(final File file) {
        String md5 = "";
        try {
            FileInputStream in = new FileInputStream(file);
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
            }

            final BigInteger hash = new BigInteger(1, digester.digest());
            md5 = hash.toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }

            in.close();
        } catch (Exception e) {
            Debug.e("MD5Calculator: " + e.getMessage());
        }

        return md5;
    }

    public static String getStringMD5(final String str) {
        String md5 = "";
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(str.getBytes());

            final BigInteger hash = new BigInteger(1, digester.digest());
            md5 = hash.toString(16);
            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
        } catch (Exception e) {
            Debug.e("MD5Calculator: " + e.getMessage());
        }

        return md5;
    }

}
