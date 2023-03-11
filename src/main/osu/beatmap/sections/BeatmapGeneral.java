package main.osu.beatmap.sections;

import main.osu.beatmap.constants.BeatmapCountdown;
import main.osu.beatmap.constants.SampleBank;

/**
 * Contains general information about a beatmap.
 */
public class BeatmapGeneral {
    /**
     * The location of the audio file relative to the beatmapset file.
     */
    public String audioFilename = "";

    /**
     * The amount of milliseconds of silence before the audio starts playing.
     */
    public int audioLeadIn;

    /**
     * The time in milliseconds when the audio preview should start.
     *
     * If -1, the audio should begin playing at 40% of its length.
     */
    public int previewTime = -1;

    /**
     * The speed of the countdown before the first hit object.
     */
    public BeatmapCountdown countdown = BeatmapCountdown.normal;

    /**
     * The sample bank that will be used if timing points do not override it.
     */
    public SampleBank sampleBank = SampleBank.normal;

    /**
     * The multiplier for the threshold in time where hit objects
     * placed close together stack, ranging from 0 to 1.
     */
    public float stackLeniency = 0.7f;
}