package lt.ekgame.beatmap_analyzer.beatmap;

public class TimingPoint {

    private double timestamp;
    private double beatLength;
    private int meter;
    private int sampleType;
    private int sampleSet;
    private int volume;
    private boolean isInherited;
    private boolean isKiai;

    private TimingPoint() {
    }

    ;

    public TimingPoint(double timestamp, double beatLength, int meter, int sampleType, int sampleSet, int volume, boolean isInherited, boolean isKiai) {
        this.timestamp = timestamp;
        this.beatLength = beatLength;
        this.meter = meter;
        this.sampleType = sampleType;
        this.sampleSet = sampleSet;
        this.volume = volume;
        this.isInherited = isInherited;
        this.isKiai = isKiai;
    }

    public TimingPoint clone() {
        TimingPoint clone = new TimingPoint();
        clone.timestamp = this.timestamp;
        clone.beatLength = this.beatLength;
        clone.meter = this.meter;
        clone.sampleType = this.sampleType;
        clone.volume = this.volume;
        clone.isInherited = this.isInherited;
        clone.isKiai = this.isKiai;
        return clone;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public double getBeatLength() {
        return beatLength;
    }

    public void setBeatLength(double beatLength) {
        this.beatLength = beatLength;
    }

    public int getMeter() {
        return meter;
    }

    public void setMeter(int meter) {
        this.meter = meter;
    }

    public int getSampleType() {
        return sampleType;
    }

    public void setSampleType(int sampleType) {
        this.sampleType = sampleType;
    }

    public int getSampleSet() {
        return sampleSet;
    }

    public void setSampleSet(int sampleSet) {
        this.sampleSet = sampleSet;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isInherited() {
        return isInherited;
    }

    public void setInherited(boolean isInherited) {
        this.isInherited = isInherited;
    }

    public boolean isKiai() {
        return isKiai;
    }

    public void setKiai(boolean isKiai) {
        this.isKiai = isKiai;
    }
}
