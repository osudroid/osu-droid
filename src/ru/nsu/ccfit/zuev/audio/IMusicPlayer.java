package ru.nsu.ccfit.zuev.audio;

public interface IMusicPlayer {

    void prepare();

    void play();

    void pause();

    void stop();

    void release();

    Status getStatus();

    int getPosition();

    int getLength();

    void seekTo(int ms);

    void setUseSoftDecoder(int decoder);

    void setDecoderMultiplier(int multiplier);

    float getVolume();

    void setVolume(float volume);

}