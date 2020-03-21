package ru.nsu.ccfit.zuev.audio.effect;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class Metronome {

    private ResourceManager resources = ResourceManager.getInstance();

    private BassSoundProvider kickSound = resources.getSound("nightcore-kick");
    private BassSoundProvider finishSound = resources.getSound("nightcore-finish");
    private BassSoundProvider clapSound = resources.getSound("nightcore-clap");
    private BassSoundProvider hatSound = resources.getSound("nightcore-hat");

    private float volume = 1.0f;
    private int lastBeatIndex = -1;

    public void update(float elapsedTime) {
        if (elapsedTime - GameHelper.getTimingOffset() <= 0) {
            return;
        }

        float playSeconds = elapsedTime - GameHelper.getTimingOffset();
        int beatIndex = (int) (playSeconds * 2 / GameHelper.getBeatLength());

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
            kickSound.play(volume);
            if (beatIndex > 0) {
                finishSound.play(volume);
            }
            return;
        }
        // 每小节第4拍kick
        if (beatInBar % 4 == 0) {
            kickSound.play(volume);
            return;
        }
        // 每小节第2拍clap
        if (beatInBar % 4 == 2) {
            clapSound.play(volume);
            return;
        }
        // 每小节奇数拍hat
        hatSound.play(volume);
    }
}
