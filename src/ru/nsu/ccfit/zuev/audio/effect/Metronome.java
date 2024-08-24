package ru.nsu.ccfit.zuev.audio.effect;

import com.rian.osu.beatmap.timings.TimingControlPoint;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class Metronome {

    private final ResourceManager resources = ResourceManager.getInstance();

    private final BassSoundProvider kickSound = resources.getSound("nightcore-kick");
    private final BassSoundProvider finishSound = resources.getSound("nightcore-finish");
    private final BassSoundProvider clapSound = resources.getSound("nightcore-clap");
    private final BassSoundProvider hatSound = resources.getSound("nightcore-hat");

    private int lastBeatIndex = -1;

    public void update(float elapsedTime, TimingControlPoint activeTimingPoint) {
        float timingPointTime = (float) activeTimingPoint.time / 1000;

        if (elapsedTime - timingPointTime <= 0) {
            return;
        }

        float playSeconds = elapsedTime - timingPointTime;
        int beatIndex = (int) (playSeconds * 2 / (activeTimingPoint.msPerBeat / 1000));

        if (beatIndex < 0) {
            return;
        }
        if (beatIndex == lastBeatIndex) {
            return;
        }
        lastBeatIndex = beatIndex;

        int beatInBar = beatIndex % GameHelper.getTimeSignature();

        // 每隔8小节在第4拍kick+finish
        if (beatIndex % (8 * GameHelper.getTimeSignature()) == 0) {
            kickSound.play();
            if (beatIndex > 0) {
                finishSound.play();
            }
            return;
        }
        // 每小节第4拍kick
        if (beatInBar % 4 == 0) {
            kickSound.play();
            return;
        }
        // 每小节第2拍clap
        if (beatInBar % 4 == 2) {
            clapSound.play();
            return;
        }
        // 每小节奇数拍hat
        hatSound.play();
    }
}
