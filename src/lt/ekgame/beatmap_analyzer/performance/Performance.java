package lt.ekgame.beatmap_analyzer.performance;

public class Performance {

    private double accuracy, performance, aimPerformance, speedPerformance, accuracyPerformance;

    public Performance(double accuracy, double performance, double aimPerformance, double speedPerformance, double accuracyPerformance) {
        this.accuracy = accuracy;
        this.performance = performance;
        this.aimPerformance = aimPerformance;
        this.speedPerformance = speedPerformance;
        this.accuracyPerformance = accuracyPerformance;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getPerformance() {
        return performance;
    }

    public double getAimPerformance() {
        return aimPerformance;
    }

    public double getSpeedPerformance() {
        return speedPerformance;
    }

    public double getAccuracyPerformance() {
        return accuracyPerformance;
    }
}
