package main.osu.game;

import android.graphics.PointF;

import main.osu.Utils;

public class GameObjectData {
    private final int time;
    private final int comboCode;
    private final String[] rawdata;
    // private SliderHelper.SliderPath path = null;
    private final PointF pos;
    private float posOffset;

    public GameObjectData(final String line) {
        String[] data = line.split("[,]");

        //Ignoring v10 features
        int dataSize = data.length;
        while (dataSize > 0 && data[dataSize - 1].matches("([0-9][:][0-9][|]?)+")) {
            dataSize--;
        }
        if (dataSize < data.length) {
            rawdata = new String[dataSize];
            for (int i = 0; i < rawdata.length; i++) {
                rawdata[i] = data[i];
            }
        } else
            rawdata = data;

        time = Integer.parseInt(rawdata[2]);
        comboCode = Integer.parseInt(rawdata[3]);
        pos = Utils.trackToRealCoords(new PointF(Float.parseFloat(rawdata[0]),
                Float.parseFloat(rawdata[1])));
        posOffset = 0;
    }

    public PointF getPos() {
        return pos;
    }

    public float getPosOffset() {
        return posOffset;
    }

    public void setPosOffset(final float posOffset) {
        this.posOffset = posOffset;
    }

    public PointF getEnd() {
        if (rawdata.length >= 8) {
            final int repeats = Integer.parseInt(rawdata[6]);
            if ((repeats % 2) != 1) {
                return pos;
            }
            final String[] endP = rawdata[5].substring(
                    rawdata[5].lastIndexOf('|') + 1).split("[:]");
            PointF end;
            try {
                end = Utils.trackToRealCoords(new PointF(Float
                        .parseFloat(endP[0]), Float.parseFloat(endP[1])));
            } catch (final NumberFormatException e) {
                end = pos;
            }
            return end;
        }
        return pos;
    }

    public boolean isNewCombo() {
        return (comboCode & 4) > 0;
    }

    public float getTime() {
        return time / 1000.0f;
    }

    public int getRawTime() {
        return time;
    }

    public String[] getData() {
        return rawdata;
    }
}
