package ru.nsu.ccfit.zuev.osu.game;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;

public class GameObjectPool {
    public static GameObjectPool instance = new GameObjectPool();

    private final ArrayDeque<GameplayHitCircle> circles = new ArrayDeque<>();
    private final Map<String, ArrayDeque<GameEffect>> effects = new HashMap<>();
    private final ArrayDeque<GameplaySlider> sliders = new ArrayDeque<>();
    private final ArrayDeque<GameplaySpinner> spinners = new ArrayDeque<>();
    private int objectsCreated = 0;
    private GameObjectPool() {
    }

    public static GameObjectPool getInstance() {
        return instance;
    }

    public GameplayHitCircle getCircle() {
        var circle = circles.poll();

        if (circle != null) {
            return circle;
        }

        objectsCreated++;
        return new GameplayHitCircle();
    }

    public void putCircle(final GameplayHitCircle circle) {
        circles.add(circle);
    }

    public GameplaySpinner getSpinner() {
        var spinner = spinners.poll();

        if (spinner != null) {
            return spinner;
        }

        objectsCreated++;
        return createSpinner();
    }

    public void putSpinner(final GameplaySpinner spinner) {
        spinners.add(spinner);
    }

    public GameEffect getEffect(final String texname) {
        var pooled = effects.get(texname);
        var effect = pooled != null ? pooled.poll() : null;

        if (effect != null) {
            return effect;
        }

        objectsCreated++;
        return new GameEffect(texname);
    }

    public void putEffect(final GameEffect effect) {
        getEffectPool(effect.getTexname()).add(effect);
    }

    public GameplaySlider getSlider() {
        var slider = sliders.poll();

        if (slider != null) {
            return slider;
        }

        objectsCreated++;
        return new GameplaySlider();
    }

    public void putSlider(final GameplaySlider slider) {
        sliders.add(slider);
    }

    public int getObjectsCreated() {
        return objectsCreated;
    }

    public void purge() {
        effects.clear();
        circles.clear();
        sliders.clear();
        spinners.clear();

        objectsCreated = 0;
    }

    public void preload() {
        for (int i = 0; i < 10; i++) {
            putCircle(new GameplayHitCircle());
        }
        for (int i = 0; i < 5; i++) {
            putSlider(new GameplaySlider());
        }
        for (int i = 0; i < 3; i++) {
            putSpinner(createSpinner());
        }

        objectsCreated = circles.size() + sliders.size() + spinners.size();
    }

    private ArrayDeque<GameEffect> getEffectPool(final String texname) {
        var pool = effects.get(texname);

        if (pool == null) {
            pool = new ArrayDeque<>();
            effects.put(texname, pool);
        }

        return pool;
    }

    private static GameplaySpinner createSpinner() {
        return Config.getSpinnerStyle() == 1 ? new GameplayModernSpinner() : new GameplaySpinner();
    }
}
