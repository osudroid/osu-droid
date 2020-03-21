package lt.ekgame.beatmap_analyzer.beatmap.osu;

import java.util.List;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapDifficulties;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapEditorState;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapGenerals;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapMetadata;
import lt.ekgame.beatmap_analyzer.beatmap.BreakPeriod;
import lt.ekgame.beatmap_analyzer.beatmap.TimingPoint;
import lt.ekgame.beatmap_analyzer.difficulty.OsuDifficulty;
import lt.ekgame.beatmap_analyzer.difficulty.OsuDifficultyCalculator;
import lt.ekgame.beatmap_analyzer.utils.MathUtils;
import lt.ekgame.beatmap_analyzer.utils.Mod;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class OsuBeatmap extends Beatmap {

    private List<OsuObject> hitObjects;

    public OsuBeatmap(BeatmapGenerals generals, BeatmapEditorState editorState, BeatmapMetadata metadata,
                      BeatmapDifficulties difficulties, List<BreakPeriod> breaks, List<TimingPoint> timingPoints, List<OsuObject> hitObjects) {
        super(generals, editorState, metadata, difficulties, breaks, timingPoints);
        this.hitObjects = hitObjects;

        finalizeObjects(hitObjects);

        applyDroid();
    }

    private void applyDroid() {
        difficulties.setCS(difficulties.getCS() / 1.3);
    }

    public double getOD() {
        return difficulties.getOD();
    }

    public double getOD(Mods mods) {
        if (!mods.isMapChanging())
            return getOD();

        double odMultiplier = 1;
        if (mods.has(Mod.HARDROCK)) odMultiplier *= 1.4;
        if (mods.has(Mod.EASY)) odMultiplier *= 0.5;
        return MathUtils.recalculateOverallDifficulty(getOD(), odMultiplier, mods.getSpeedMultiplier());
    }

    public double getAR() {
        return difficulties.getAR();
    }

    public double getAR(Mods mods) {
        if (!mods.isMapChanging())
            return getAR();

        double arMultiplier = 1;
        if (mods.has(Mod.HARDROCK)) arMultiplier *= 1.4;
        if (mods.has(Mod.EASY)) arMultiplier *= 0.5;
        return MathUtils.recalculateApproachRate(getAR(), arMultiplier, mods.getSpeedMultiplier());
    }

    public double getCS() {
        return difficulties.getCS();
    }

    public double getCS(Mods mods) {
        if (!mods.isMapChanging())
            return getCS();

        double csMultiplier = 1;
        if (mods.has(Mod.HARDROCK)) csMultiplier *= 1.3;
        if (mods.has(Mod.EASY)) csMultiplier *= 0.5;
        return MathUtils.recalculateCircleSize(getCS(), csMultiplier);
    }

    @Override
    public Gamemode getGamemode() {
        return Gamemode.OSU;
    }

    @Override
    public int getMaxCombo() {
        int c = 0;
        for (OsuObject o : hitObjects) {
            c += o.getCombo();
        }
        return c;
        //hitObjects.stream().mapToInt(o->o.getCombo()).sum();
    }

    public List<OsuObject> getHitObjects() {
        return hitObjects;
    }

    @Override
    public int getObjectCount() {
        return hitObjects.size();
    }

    @Override
    public OsuDifficultyCalculator getDifficultyCalculator() {
        return new OsuDifficultyCalculator();
    }

    @Override
    public OsuDifficulty getDifficulty(Mods mods) {
        return getDifficultyCalculator().calculate(mods, this);
    }

    @Override
    public OsuDifficulty getDifficulty() {
        return getDifficulty(Mods.NOMOD);
    }
}
