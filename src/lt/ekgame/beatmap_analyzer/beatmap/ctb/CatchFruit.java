package lt.ekgame.beatmap_analyzer.beatmap.ctb;

import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class CatchFruit extends CatchObject {

    public CatchFruit(Vec2 position, int startTime, int hitSound, boolean isNewCombo) {
        super(position, startTime, startTime, hitSound, isNewCombo);
    }

    @Override
    public CatchObject clone() {
        return new CatchFruit(position.clone(), startTime, hitSound, isNewCombo);
    }
}
