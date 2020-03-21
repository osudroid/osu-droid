package lt.ekgame.beatmap_analyzer.beatmap;

public class BreakPeriod {

    private int startTime, endTime;

    public BreakPeriod(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public BreakPeriod clone() {
        return new BreakPeriod(startTime, endTime);
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
}
