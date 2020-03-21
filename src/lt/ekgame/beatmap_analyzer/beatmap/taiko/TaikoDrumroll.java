package lt.ekgame.beatmap_analyzer.beatmap.taiko;

import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class TaikoDrumroll extends TaikoObject {

    private double pixelLength;
    private boolean isBig;

    public TaikoDrumroll(Vec2 position, int startTime, int hitSound, double pixelLength, boolean isBig) {
        super(position, startTime, startTime, hitSound);
        this.pixelLength = pixelLength;
        this.isBig = isBig;
    }

    @Override
    public TaikoObject clone() {
        return new TaikoDrumroll(position.clone(), startTime, hitSound, pixelLength, isBig);
    }

    @Override
    public int getCombo() {
        return 0;
    }
}
