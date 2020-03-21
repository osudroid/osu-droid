package lt.ekgame.beatmap_analyzer.beatmap.ctb;

import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.utils.Vec2;

public abstract class CatchObject extends HitObject {

    protected boolean isNewCombo;

    public CatchObject(Vec2 position, int startTime, int endTime, int hitSound, boolean isNewCombo) {
        super(position, startTime, endTime, hitSound);
        this.isNewCombo = isNewCombo;
    }

    public boolean isNewCombo() {
        return isNewCombo;
    }

}
