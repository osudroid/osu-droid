package com.reco1l.ui.data;

// Created by Reco1l on 10/8/22 02:05

import android.graphics.Color;
import android.widget.TextView;

import com.reco1l.utils.Animation;
import com.reco1l.utils.Res;

import java.text.SimpleDateFormat;

import ru.nsu.ccfit.zuev.osuplus.R;

public abstract class Property {

    public TextView view;

    public PropertyFormat format;
    public boolean
            lessIsBetter = false,
            allowColorChange = true;

    //--------------------------------------------------------------------------------------------//

    protected abstract Object getValue();

    //--------------------------------------------------------------------------------------------//

    protected abstract Evaluator getEvaluator();

    public void update() {
        if (view == null || view.getText() == getValue())
            return;

        if (allowColorChange && getEvaluator() != null) {
            State color = State.NORMAL;

            if (getEvaluator().isHigher()) {
                color = lessIsBetter ? State.GREEN : State.RED;
            } else if (getEvaluator().isLower()) {
                color = lessIsBetter ? State.RED : State.GREEN;
            }
            Utils.changeColor(view, color);
        }

        if (format != null) {
            Utils.setText(view, format.apply(getValue()));
        } else {
            Utils.setText(view, "" + getValue());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public enum State {
        RED(Res.color(R.color.propertyTextRed)),
        GREEN(Res.color(R.color.propertyTextGreen)),
        NORMAL(0xFFFFFFFF);

        int hex;

        State(int hex) {
            this.hex = hex;
        }
    }

    protected interface Evaluator {
        boolean isHigher();

        boolean isLower();
    }

    public interface PropertyFormat {
        String apply(Object value);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Utils {

        static void setText(TextView view, String text) {
            if (view == null)
                return;

            final int color = view.getCurrentTextColor();
            final int alpha = Color.alpha(color);
            final int[] rgb = {
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color)
            };

            new Animation(view)
                    .ofArgb(color, Color.argb(0, rgb[0], rgb[1], rgb[2]))
                    .runOnUpdate(val -> view.setTextColor((int) val.getAnimatedValue()))
                    .cancelPending(false)
                    .play(250);

            new Animation(view)
                    .ofArgb(view.getCurrentTextColor(), Color.argb(alpha, rgb[0], rgb[1], rgb[2]))
                    .runOnUpdate(val -> view.setTextColor((int) val.getAnimatedValue()))
                    .runOnStart(() -> view.setText(text))
                    .cancelPending(false)
                    .delay(250)
                    .play(250);
        }

        static void changeColor(TextView view, State toColor) {
            if (view == null)
                return;

            final int color = view.getCurrentTextColor();
            if (color == toColor.hex)
                return;

            new Animation(view)
                    .ofArgb(color, toColor.hex)
                    .runOnUpdate(val -> view.setTextColor((int) val.getAnimatedValue()))
                    .cancelPending(false)
                    .play(800);
        }
    }

    // Custom types
    //--------------------------------------------------------------------------------------------//

    public static class Bpm {

        public TextView view;
        public float min, max;
        public PropertyFormat format;

        private float originalValue;

        //----------------------------------------------------------------------------------------//

        public void set(float min, float max) {
            originalValue = min;
            this.min = min;
            this.max = max;
        }

        public void update() {
            State color = State.NORMAL;
            if (min > originalValue) {
                color = State.RED;
            } else if (min < originalValue) {
                color = State.GREEN;
            }

            Utils.changeColor(this.view, color);

            if (max != min) {
                Utils.setText(this.view, format.apply(min) + "-" + format.apply(max));
                return;
            }
            Utils.setText(this.view, "" + format.apply(min));
        }

    }

    public static class Length {

        public long val;
        public TextView view;

        private long originalValue;

        //----------------------------------------------------------------------------------------//

        public void set(long length) {
            val = length;
            originalValue = length;
        }

        public void update() {
            SimpleDateFormat date;

            State color = State.NORMAL;
            if (val > originalValue) {
                color = State.RED;
            } else if (val < originalValue) {
                color = State.GREEN;
            }

            Utils.changeColor(view, color);

            if (val > 3600 * 1000) {
                date = new SimpleDateFormat("HH:mm:ss");
            } else {
                date = new SimpleDateFormat("mm:ss");
            }

            Utils.setText(view, date.format(val));
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Float extends Property {

        public float val;
        private float originalValue;

        //----------------------------------------------------------------------------------------//

        @Override
        protected Object getValue() {
            return val;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected Evaluator getEvaluator() {
            return new Evaluator() {
                @Override
                public boolean isHigher() {
                    return val > originalValue;
                }

                @Override
                public boolean isLower() {
                    return val < originalValue;
                }
            };
        }

        //----------------------------------------------------------------------------------------//

        public void set(float value) {
            val = value;
            originalValue = value;
        }
    }

    public static class Int extends Property {

        public int val;
        private int originalValue;

        //----------------------------------------------------------------------------------------//

        @Override
        protected Object getValue() {
            return val;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected Evaluator getEvaluator() {
            return new Evaluator() {
                @Override
                public boolean isHigher() {
                    return val > originalValue;
                }

                @Override
                public boolean isLower() {
                    return val < originalValue;
                }
            };
        }

        //----------------------------------------------------------------------------------------//

        public void set(int value) {
            val = value;
            originalValue = value;
        }
    }

}
