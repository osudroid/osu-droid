package ru.nsu.ccfit.zuev.osu.beatmap;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

import java.io.File;
import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapColor;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapDifficulty;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapEvents;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapGeneral;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapMetadata;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

/**
 * A structure containing information about a beatmap.
 */
public class BeatmapData {
    /**
     * General information about this beatmap.
     */
    public final BeatmapGeneral general;

    /**
     * Information used to identify this beatmap.
     */
    public final BeatmapMetadata metadata;

    /**
     * Difficulty settings of this beatmap.
     */
    public final BeatmapDifficulty difficulty;

    /**
     * Events of this beatmap.
     */
    public final BeatmapEvents events;

    /**
     * Combo and skin colors of this beatmap.
     */
    public final BeatmapColor colors;

    /**
     * Raw timing points data in this beatmap.
     */
    public final ArrayList<String> rawTimingPoints = new ArrayList<>();

    /**
     * The manager of timing points in this beatmap.
     */
    public final BeatmapControlPointsManager timingPoints;

    /**
     * Raw hit objects data in this beatmap.
     */
    public final ArrayList<String> rawHitObjects = new ArrayList<>();

    /**
     * The manager of hit objects in this beatmap.
     */
    public final BeatmapHitObjectsManager hitObjects;

    /**
     * The path of parent folder of this beatmap.
     */
    private String folder;

    /**
     * The format version of this beatmap.
     */
    private int formatVersion = 14;

    public BeatmapData() {
        general = new BeatmapGeneral();
        metadata = new BeatmapMetadata();
        difficulty = new BeatmapDifficulty();
        events = new BeatmapEvents();
        colors = new BeatmapColor();
        timingPoints = new BeatmapControlPointsManager();
        hitObjects = new BeatmapHitObjectsManager();
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapData(BeatmapData source) {
        folder = source.folder;
        formatVersion = source.formatVersion;

        general = source.general.deepClone();
        metadata = source.metadata.deepClone();
        difficulty = source.difficulty.deepClone();
        events = source.events.deepClone();
        colors = source.colors.deepClone();
        timingPoints = source.timingPoints.deepClone();
        hitObjects = source.hitObjects.deepClone();

        rawTimingPoints.addAll(source.rawTimingPoints);
        rawHitObjects.addAll(source.rawHitObjects);
    }

    /**
     * Deep clones this instance.
     *
     * @return The deep cloned instance.
     */
    public BeatmapData deepClone() {
        return new BeatmapData(this);
    }

    /**
     * Gets the path of the parent folder of this beatmap.
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the path of the parent folder of this beatmap.
     *
     * @param path The path of the parent folder.
     */
    public void setFolder(String path) {
        folder = path;
    }

    /**
     * Gets the max combo of this beatmap.
     */
    public int getMaxCombo() {
        int combo = 0;

        for (HitObject object : hitObjects.getObjects()) {
            ++combo;

            if (object instanceof Slider) {
                combo += ((Slider) object).getNestedHitObjects().size() - 1;
            }
        }

        return combo;
    }

    /**
     * Gets the format version of this beatmap.
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * Sets the format version of this beatmap.
     *
     * @param formatVersion The format version of this beatmap.
     */
    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    public double getOffsetTime(double time) {
        return time + (formatVersion < 5 ? 24 : 0);
    }

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    public int getOffsetTime(int time) {
        return time + (formatVersion < 5 ? 24 : 0);
    }

    /**
     * Given a <code>BeatmapInfo</code> and <code>TrackInfo</code>, populate its metadata
     * with this <code>BeatmapData</code>.
     *
     * @param info The <code>BeatmapInfo</code> to populate.
     * @param track The <code>TrackInfo</code> to populate.
     * @return Whether the given <code>BeatmapInfo</code> and <code>TrackInfo</code> was successfully populated.
     */
    public boolean populateMetadata(final BeatmapInfo info, final TrackInfo track) {
        // General
        if (info.getMusic() == null) {
            final File musicFile = new File(info.getPath(), general.audioFilename);
            if (!musicFile.exists()) {
                ToastLogger.showText(StringTable.format(R.string.beatmap_parser_music_not_found,
                        track.getFilename().substring(0, track.getFilename().length() - 4)), true);
                return false;
            }
            info.setMusic(musicFile.getPath());
            info.setPreviewTime(general.previewTime);
        }

        // Metadata
        if (info.getTitle() == null) {
            info.setTitle(metadata.title);
        }
        if (info.getTitleUnicode() == null) {
            String titleUnicode = metadata.titleUnicode;
            if (!titleUnicode.isEmpty()) {
                info.setTitleUnicode(titleUnicode);
            }
        }
        if (info.getArtist() == null) {
            info.setArtist(metadata.artist);
        }
        if (info.getArtistUnicode() == null) {
            String artistUnicode = metadata.artist;
            if (!artistUnicode.isEmpty()) {
                info.setArtistUnicode(artistUnicode);
            }
        }
        if (info.getSource() == null) {
            info.setSource(metadata.source);
        }
        if (info.getTags() == null) {
            info.setTags(metadata.tags);
        }

        track.setCreator(metadata.creator);
        track.setMode(metadata.version);
        track.setPublicName(metadata.artist + " - " + metadata.title);
        track.setBeatmapID(metadata.beatmapID);
        track.setBeatmapSetID(metadata.beatmapSetID);

        // Difficulty
        track.setOverallDifficulty(difficulty.od);
        track.setApproachRate(difficulty.ar);
        track.setHpDrain(difficulty.hp);
        track.setCircleSize(difficulty.cs);

        // Events
        track.setBackground(events.backgroundFilename);

        // Timing points
        for (TimingControlPoint point : timingPoints.timing.getControlPoints()) {
            float bpm = (float) point.getBPM();

            track.setBpmMin(track.getBpmMin() != Float.MAX_VALUE ? Math.min(track.getBpmMin(), bpm) : bpm);
            track.setBpmMax(track.getBpmMax() != 0 ? Math.max(track.getBpmMax(), bpm) : bpm);
        }

        // Hit objects
        if (hitObjects.getObjects().isEmpty()) {
            return false;
        }

        track.setTotalHitObjectCount(hitObjects.getObjects().size());
        track.setHitCircleCount(hitObjects.getCircleCount());
        track.setSliderCount(hitObjects.getSliderCount());
        track.setSpinnerCount(hitObjects.getSpinnerCount());

        HitObject lastObject = hitObjects.getObjects().get(hitObjects.getObjects().size() - 1);

        track.setMusicLength((int) lastObject.getStartTime());
        if (lastObject instanceof HitObjectWithDuration) {
            track.setMusicLength((int) ((HitObjectWithDuration) lastObject).getEndTime());
        }
        track.setMaxCombo(getMaxCombo());

        DifficultyAttributes attributes = BeatmapDifficultyCalculator.calculateDifficulty(
                BeatmapDifficultyCalculator.constructDifficultyBeatmap(this)
        );

        track.setDifficulty(GameHelper.Round(attributes.starRating, 2));

        return true;
    }
}
