package ru.nsu.ccfit.zuev.osu.game;

import ru.nsu.ccfit.zuev.osu.Constants;

public class TimingPoint {

    private static String defaultSound = "normal";

    final double time;

    final String hitSound;

    final float volume;

    final boolean kiai;

    double beatLength;

    int signature = 4;

    int customSound = 0;

    boolean inherited = false;

    public TimingPoint(final String[] data, final TimingPoint prevData) {
        time = Float.parseFloat(data[0]) / 1000.0f;
        beatLength = Double.parseDouble(data[1]);
        if (beatLength < 0 && prevData != null) {
            inherited = true;
            beatLength = -prevData.getBeatLength() * (beatLength / 100.0f);
        } else {
            beatLength /= 1000.0f;
        }

        if (data.length > 2) {
            if ("4".equals(data[2])) {
                signature = 4;
            }
            if ("3".equals(data[2])) {
                signature = 3;
            }
        }

        if (data.length > 3) {
            if (data[3].equals("1")) {
                hitSound = Constants.SAMPLE_PREFIX[1];
            } else if (data[3].equals("3")) {
                hitSound = Constants.SAMPLE_PREFIX[3];
            } else {
                hitSound = Constants.SAMPLE_PREFIX[2];
            }
        } else {
            hitSound = getDefaultSound();
        }
        if (data.length > 4) {
            customSound = Integer.parseInt(data[4]);
        }
        if (data.length > 5) {
            volume = Integer.parseInt(data[5]) / 100f;
        } else {
            volume = 1;
        }
        if (data.length > 7) {
            kiai = (!data[7].equals("0"));
        } else {
            kiai = false;
        }
    }

    public static String getDefaultSound() {
        return defaultSound;
    }

    public static void setDefaultSound(final String defaultSound) {
        TimingPoint.defaultSound = defaultSound;
    }

    public boolean wasInderited() {
        return inherited;
    }

    public String getHitSound() {
        return hitSound;
    }

    public int getCustomSound() {
        return customSound;
    }

    public float getVolume() {
        return volume;
    }

    public double getBeatLength() {
        return beatLength;
    }

    public int getSignature() {
        return signature;
    }

    public double getTime() {
        return time;
    }

    public boolean isKiai() {
        return kiai;
    }

}
