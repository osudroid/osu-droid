package lt.ekgame.beatmap_analyzer.beatmap.osu;

import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.utils.Vec2;

public abstract class OsuObject extends HitObject {

    protected boolean isNewCombo;

    public OsuObject(Vec2 position, int startTime, int endTime, int hitSound, boolean isNewCombo) {
        super(position, startTime, endTime, hitSound);
        this.isNewCombo = isNewCombo;
    }

    public boolean isNewCombo() {
        return isNewCombo;
    }
}
