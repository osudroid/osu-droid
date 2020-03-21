package lt.ekgame.beatmap_analyzer.beatmap.osu;

import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class OsuCircle extends OsuObject {

    public OsuCircle(Vec2 position, int timestamp, int hitSound, boolean isNewCombo) {
        super(position, timestamp, timestamp, hitSound, isNewCombo);
    }

    @Override
    public OsuObject clone() {
        return new OsuCircle(position.clone(), startTime, hitSound, isNewCombo);
    }

}
