package com.reco1l.data;

// Created by Reco1l on 16/9/22 18:46

import android.widget.TextView;

import com.reco1l.tables.Res;

import java.text.SimpleDateFormat;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapProperty<T extends Number> {

    public T value;
    public TextView view;
    public ExpressionFormat<T> format;

    public boolean lessIsBetter = false;
    public boolean allowColorChange = true;

    private T baseValue;
    private State state;
    private final Evaluator evaluator;

    //--------------------------------------------------------------------------------------------//

    public BeatmapProperty() {
        evaluator = getEvaluator();
        state = State.NORMAL;
    }

    //--------------------------------------------------------------------------------------------//

    public enum State {
        RED(Res.color(R.color.propertyTextRed)),
        GREEN(Res.color(R.color.propertyTextGreen)),
        NORMAL(0xFFFFFFFF);

        int color;

        State(int color) {
            this.color = color;
        }
    }

    //--------------------------------------------------------------------------------------------//

    protected interface Evaluator {
        boolean isHigher();
        boolean isLower();
    }

    @FunctionalInterface
    public interface ExpressionFormat<T extends Number> {
        T apply(T value);
    }

    //--------------------------------------------------------------------------------------------//

    // Only supports Float, Integer and Long values, but you can add support for Double and Short values
    private Evaluator getEvaluator() {
        if (value instanceof Float) {
            return new Evaluator() {
                public boolean isHigher() {
                    return (Float) value > (Float) baseValue;
                }
                public boolean isLower() {
                    return (Float) value > (Float) baseValue;
                }
            };
        } else if (value instanceof Integer) {
            return new Evaluator() {
                public boolean isHigher() {
                    return (Integer) value > (Integer) baseValue;
                }
                public boolean isLower() {
                    return (Integer) value > (Integer) baseValue;
                }
            };
        } else if (value instanceof Long) {
            return new Evaluator() {
                public boolean isHigher() {
                    return (Long) value > (Long) baseValue;
                }
                public boolean isLower() {
                    return (Long) value > (Long) baseValue;
                }
            };
        }
        return null;
    }

    private State getState() {
        if (evaluator == null)
            return State.NORMAL;

        if (evaluator.isHigher()) {
            return lessIsBetter ? State.GREEN : State.RED;
        } else if (evaluator.isLower()) {
            return lessIsBetter ? State.RED : State.GREEN;
        }
        return State.NORMAL;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onUpdate(T value, State state) {
        if (view == null)
            return;

        view.setText("" + value);
        view.setTextColor(state.color);

        // It only work with drawableStart.
        if (view.getCompoundDrawablesRelative()[0] != null) {
            view.getCompoundDrawablesRelative()[0].setTint(state.color);
        }
    }

    public void set(T value) {
        this.value = value;
        this.baseValue = value;
        this.state = State.NORMAL;
    }

    public void update() {
        if (allowColorChange) {
            state = getState();
        }

        if (format != null) {
            onUpdate(format.apply(value), state);
        } else {
            onUpdate(value, state);
        }
    }

    // Custom types
    //--------------------------------------------------------------------------------------------//

    public static class BPM {

        public TextView view;

        private final BeatmapProperty<Float> min;
        private final BeatmapProperty<Float> max;

        private float minValue, maxValue;

        //----------------------------------------------------------------------------------------//

        public BPM() {
            min = new BeatmapProperty<Float>() {
                protected void onUpdate(Float value, State state) {
                    minValue = value;
                }
            };

            max = new BeatmapProperty<Float>() {
                protected void onUpdate(Float value, State state) {
                    maxValue = value;
                }
            };

            min.format = value -> GameHelper.Round(value, 1);
            max.format = value -> GameHelper.Round(value, 1);
        }

        //----------------------------------------------------------------------------------------//

        public void set(float min, float max) {
            this.min.set(min);
            this.max.set(max);
        }

        public void multiply(float multiplier) {
            min.value *= multiplier;
            max.value *= multiplier;
        }

        public void update() {
            min.update();
            max.update();

            if (view == null)
                return;
            view.setText(minValue + "-" + maxValue);

            if (min.state != null) {
                view.setTextColor(min.state.color);

                if (view.getCompoundDrawablesRelative()[0] != null) {
                    view.getCompoundDrawablesRelative()[0].setTint(min.state.color);
                }
            }
        }

    }

    public static class Length extends BeatmapProperty<Long> {
        @Override
        protected void onUpdate(Long value, State state) {
            SimpleDateFormat sdf;

            if (value > 3600 * 1000) {
                sdf = new SimpleDateFormat("HH:mm:ss");
            } else {
                sdf = new SimpleDateFormat("mm:ss");
            }

            view.setText(sdf.format(value));
            view.setTextColor(state.color);

            // It only work with drawableStart.
            if (view.getCompoundDrawablesRelative()[0] != null) {
                view.getCompoundDrawablesRelative()[0].setTint(state.color);
            }
        }
    }
}
