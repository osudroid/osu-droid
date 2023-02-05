package com.reco1l.data;

import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.reco1l.global.Game;

import java.util.function.Consumer;

public final class TrackAttribute<T extends Number> {

    public static final int
            BPM_MIN = 0,
            BPM_MAX = 1,
            LENGTH = 2,
            COMBO = 3,
            CIRCLES = 4,
            SLIDERS = 5,
            SPINNERS = 6,
            AR = 7,
            OD = 8,
            CS = 9,
            HP = 10,
            STARS = 11;

    private T mModifiedValue;

    private final T mMax;

    private int mCompareResult;

    private TextView mText;
    private Consumer<T> mConsumer;
    private ValueFormatter<T> mFormatter;

    private final Value<T> mValueGetter;

    //--------------------------------------------------------------------------------------------//

    public TrackAttribute(@NonNull Value<T> value) {
        this(value, null);
    }

    public TrackAttribute(@NonNull Value<T> value, T pMax) {
        mMax = pMax;
        mValueGetter = value;
        mModifiedValue = value.get();
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface Value<T> {
        T get();
    }

    @FunctionalInterface
    public interface ValueOperator<T extends Number> {
        T get(T pValue);
    }

    @FunctionalInterface
    public interface ValueFormatter<T extends Number> {
        String format(T pValue);
    }

    //--------------------------------------------------------------------------------------------//

    public TrackAttribute<T> setFormatter(ValueFormatter<T> formatter) {
        mFormatter = formatter;
        return this;
    }

    public TrackAttribute<T> setOnChange(Consumer<T> pConsumer) {
        mConsumer = pConsumer;
        return this;
    }

    public TrackAttribute<T> setView(TextView pText) {
        mText = pText;
        onChange();
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    private void onChange() {
        Game.activity.runOnUiThread(() -> {
            if (mText != null) {
                if (mFormatter != null) {
                    mText.setText(mFormatter.format(mModifiedValue));
                } else {
                    mText.setText(mModifiedValue.toString());
                }

                if (mCompareResult < 0) {
                    mText.setTextColor(Color.GREEN);
                } else if (mCompareResult > 0) {
                    mText.setTextColor(Color.RED);
                } else {
                    mText.setTextColor(Color.WHITE);
                }
            }

            if (mConsumer != null) {
                mConsumer.accept(mModifiedValue);
            }
        });
    }

    private void compare() {
        if (mValueGetter.get() instanceof Float) {
            mCompareResult = Float.compare((Float) mModifiedValue, (Float) mValueGetter.get());
        }
        else if (mValueGetter.get() instanceof Integer) {
            mCompareResult = Integer.compare((Integer) mModifiedValue, (Integer) mValueGetter.get());
        }
        else if (mValueGetter.get() instanceof Long) {
            mCompareResult = Long.compare((Long) mModifiedValue, (Long) mValueGetter.get());
        } else {
            mCompareResult = 0;
        }
    }

    //--------------------------------------------------------------------------------------------//

    // Do an arithmetic operation
    @SuppressWarnings("unchecked")
    public void opt(ValueOperator<T> pOperation) {

        if (mMax != null) {
            if (mValueGetter.get() instanceof Float) {
                mModifiedValue = (T) Float.valueOf(
                        Math.min(pOperation.get(mModifiedValue).floatValue(), mMax.floatValue())
                );
            }
            else if (mValueGetter.get() instanceof Integer) {
                mModifiedValue = (T) Integer.valueOf(
                        Math.min(pOperation.get(mModifiedValue).intValue(), mMax.intValue())
                );
            }
            else if (mValueGetter.get() instanceof Long) {
                mModifiedValue = (T) Long.valueOf(
                        Math.min(pOperation.get(mModifiedValue).longValue(), mMax.longValue())
                );
            }
        } else {
            mModifiedValue = pOperation.get(mModifiedValue);
        }

        compare();
        onChange();
    }

    public void reset() {
        mModifiedValue = mValueGetter.get();
        mCompareResult = 0;
        onChange();
    }

    public T getValue() {
        return mModifiedValue;
    }
}
