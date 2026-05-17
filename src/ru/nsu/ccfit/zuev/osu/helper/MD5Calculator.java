package ru.nsu.ccfit.zuev.osu.helper;

import org.andengine.util.debug.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class MD5Calculator {
    public static String getFileMD5(final File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
            }

            final BigInteger hash = new BigInteger(1, digester.digest());
            StringBuilder md5 = new StringBuilder(hash.toString(16));
            while (md5.length() < 32) {
                md5.insert(0, "0");
            }

            return md5.toString();
        } catch (Exception e) {
            Debug.e("MD5Calculator: " + e.getMessage());
        }

        return "";
    }

    public static String getStringMD5(final String str) {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(str.getBytes());

            final BigInteger hash = new BigInteger(1, digester.digest());
            StringBuilder md5 = new StringBuilder(hash.toString(16));
            while (md5.length() < 32) {
                md5.insert(0, "0");
            }
            return md5.toString();
        } catch (Exception e) {
            Debug.e("MD5Calculator: " + e.getMessage());
        }

        return "";
    }
}
