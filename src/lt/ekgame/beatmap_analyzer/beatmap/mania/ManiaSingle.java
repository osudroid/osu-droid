package lt.ekgame.beatmap_analyzer.beatmap.mania;

import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class ManiaSingle extends ManiaObject {

    public ManiaSingle(Vec2 position, int startTime, int hitSound) {
        super(position, startTime, startTime, hitSound);
    }

    @Override
    public ManiaObject clone() {
        return new ManiaSingle(position.clone(), startTime, hitSound);
    }

}
