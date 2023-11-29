package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.SingleValueSpanEntityModifier;

public class UniversalModifier extends SingleValueSpanEntityModifier {

    ValueType type;

    ;

    public UniversalModifier(
        final float duration, final float from, final float to, final ValueType type) {
        super(duration, from, to);
        this.type = type;
    }

    public UniversalModifier(final UniversalModifier modifier) {
        super(modifier);
        this.type = modifier.type;
    }

    @Override
    protected void onSetInitialValue(final IEntity pItem, final float pValue) {
        switch (type) {
            case ALPHA:
                pItem.setAlpha(pValue);
                break;
            case SCALE:
                pItem.setScale(pValue);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSetValue(
        final IEntity pItem, final float pPercentageDone, final float pValue) {
        switch (type) {
            case ALPHA:
                pItem.setAlpha(pValue);
                break;
            case SCALE:
                pItem.setScale(pValue);
                break;
            default:
                break;
        }
    }

    @Override
    public UniversalModifier deepCopy() {
        return new UniversalModifier(this);
    }

    public void init(
        final float duration, final float from, final float to, final ValueType type) {
        reset();
        mDuration = duration;
        mFromValue = from;
        mValueSpan = to - from;
        this.type = type;
    }

    @Override
    protected void onModifierFinished(final IEntity pItem) {
        super.onModifierFinished(pItem);
        this.mModifierListeners.clear();
        ModifierFactory.putModifier(this);
    }


    public enum ValueType {
        NONE, ALPHA, SCALE
    }

}
