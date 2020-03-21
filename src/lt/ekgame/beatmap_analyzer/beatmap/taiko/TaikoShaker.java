package lt.ekgame.beatmap_analyzer.beatmap.taiko;

import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class TaikoShaker extends TaikoObject {

    public TaikoShaker(Vec2 position, int startTime, int endTime, int hitSound) {
        super(position, startTime, endTime, hitSound);
    }

    @Override
    public HitObject clone() {
        return new TaikoShaker(position.clone(), startTime, endTime, hitSound);
    }

    @Override
    public int getCombo() {
        return 0;
    }
}
