package ru.nsu.ccfit.zuev.audio;

public class MP3Decoder {
    public static boolean isAvailable;

    static {
        try {
            System.loadLibrary("audio-tools");
            isAvailable = true;
        } catch (final UnsatisfiedLinkError e) {
            isAvailable = false;
        }

    }

    private int handle;

    private native int prepareHandle(String file);

    private native void closeFile(int handle);

    private native int readSamples(int handle, short[] buffer, int size);

    private native int skipSamples(int handle, int count);

    private native int getSampleRate(int handle);

    private native int getBitRate(int handle);

    public void openFile(final String file) {
        handle = prepareHandle(file);
    }

    public int getSampleRate() {
        return getSampleRate(handle);
    }

    public int getBitRate() {
        return getBitRate(handle);
    }

    public int readSamples(final short[] samples) {
        final int readSamples = readSamples(handle, samples, samples.length);
        if (readSamples == 0) {
            closeFile(handle);
            return 0;
        }
        return samples.length;
    }

    public void skip(final int count) {
        skipSamples(handle, count);
    }

    public void release() {
        closeFile(handle);
    }
}
