package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.entity.modifier.IEntityModifier;
import ru.nsu.ccfit.zuev.osu.helper.UniversalModifier.ValueType;

import java.util.LinkedList;
import java.util.Queue;

public class ModifierFactory {

    private static ModifierFactory instance = new ModifierFactory();

    private final Queue<UniversalModifier> pool = new LinkedList<>();

    private ModifierFactory() {
    }

    public static IEntityModifier newFadeInModifier(final float duration) {
        return instance.newModifier(duration, 0, 1, ValueType.ALPHA);
    }

    public static IEntityModifier newFadeOutModifier(final float duration) {
        return instance.newModifier(duration, 1, 0, ValueType.ALPHA);
    }

    public static IEntityModifier newAlphaModifier(final float duration,
                                                   final float from, final float to) {
        return instance.newModifier(duration, from, to, ValueType.ALPHA);
    }

    public static IEntityModifier newScaleModifier(final float duration,
                                                   final float from, final float to) {
        return instance.newModifier(duration, from, to, ValueType.SCALE);
    }

    public static IEntityModifier newDelayModifier(final float duration) {
        return instance.newModifier(duration, 0, 0, ValueType.NONE);
    }

    public static void putModifier(final UniversalModifier mod) {
        instance.pool.add(mod);
    }

    public static void clear() {
        instance.pool.clear();
    }

    private UniversalModifier newModifier(final float duration,
                                          final float from, final float to, final ValueType type) {
        if (!pool.isEmpty()) {
            UniversalModifier mod = null;

            synchronized (pool) {
                if (!pool.isEmpty()) {
                    mod = pool.poll();
                }
            }
            if (mod != null) {
                mod.init(duration, from, to, type);
                return mod;
            }
        }
        return new UniversalModifier(duration, from, to, type);
    }

}
