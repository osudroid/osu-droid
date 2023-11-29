package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.util.Debug;

import java.math.BigInteger;
import java.security.MessageDigest;

public class MD5Calcuator {

    public static String getStringMD5(final String str) {
        StringBuilder md5 = new StringBuilder();
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(str.getBytes());

            final BigInteger hash = new BigInteger(1, digester.digest());
            md5 = new StringBuilder(hash.toString(16));
            while (md5.length() < 32) {
                md5.insert(0, "0");
            }
        } catch (Exception e) {
            Debug.e("MD5Calculator: " + e.getMessage());
        }

        return md5.toString();
    }

}
