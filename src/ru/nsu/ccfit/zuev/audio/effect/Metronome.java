package ru.nsu.ccfit.zuev.audio.effect;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;

public class Metronome {
    private final static float volume = 1.0f;

    private final BassSoundProvider kickSound;
    private final BassSoundProvider finishSound;
    private final BassSoundProvider clapSound;
    private final BassSoundProvider hatSound;

    private int lastBeatIndex = -1;

    public Metronome() {
        ResourceManager resources = ResourceManager.getInstance();

        resources.loadSound("nightcore-kick", "sfx/nightcore-kick.wav", false);
        resources.loadSound("nightcore-finish", "sfx/nightcore-finish.wav", false);
        resources.loadSound("nightcore-clap", "sfx/nightcore-clap.wav", false);
        resources.loadSound("nightcore-hat", "sfx/nightcore-hat.wav", false);

        kickSound = resources.getSound("nightcore-kick");
        finishSound = resources.getSound("nightcore-finish");
        clapSound = resources.getSound("nightcore-clap");
        hatSound = resources.getSound("nightcore-hat");
    }

    public void update(float elapsedTime) {
        if (elapsedTime - GameHelper.getTimingOffset() <= 0) {
            return;
        }

        float playSeconds = elapsedTime - GameHelper.getTimingOffset();
        int beatIndex = (int) (playSeconds * 2 / GameHelper.getBeatLength());

        if (beatIndex < 0 || beatIndex == lastBeatIndex) {
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
