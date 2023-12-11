package ru.nsu.ccfit.zuev.osu.helper;

public abstract class DifficultyHelper {

    public static final DifficultyHelper StdDifficulty = new DifficultyHelper() {

        @Override
        public float hitWindowFor300(float od) {
            return (75 + 25 * (5 - od) / 5) / 1000;
        }

        @Override
        public float hitWindowFor100(float od) {
            return (150 + 50 * (5 - od) / 5) / 1000;
        }

        @Override
        public float hitWindowFor50(float od) {
            return (250 + 50 * (5 - od) / 5) / 1000f;
        }
    };

    public static final DifficultyHelper HighDifficulty = new DifficultyHelper() {

        @Override
        public float hitWindowFor300(float od) {
            return (55 + 30 * (5 - od) / 5) / 1000f;
        }

        @Override
        public float hitWindowFor100(float od) {
            return (120 + 40 * (5 - od) / 5) / 1000f;
        }

        @Override
        public float hitWindowFor50(float od) {
            return (180 + 50 * (5 - od) / 5) / 1000f;
        }
    };

    public abstract float hitWindowFor300(float od);

    public abstract float hitWindowFor100(float od);

    public abstract float hitWindowFor50(float od);

}
