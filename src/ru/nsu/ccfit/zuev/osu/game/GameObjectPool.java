package ru.nsu.ccfit.zuev.osu.game;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;

public class GameObjectPool {
    public static GameObjectPool instance = new GameObjectPool();
    public LinkedList<HitCircle> circles = new LinkedList<>();
    public Map<String, LinkedList<GameEffect>> effects = new HashMap<>();
    public LinkedList<Slider> sliders = new LinkedList<>();
    public LinkedList<FollowTrack> tracks = new LinkedList<>();
    public LinkedList<Spinner> spinners = new LinkedList<>();
    private int objectsCreated = 0;
    private GameObjectPool() {
    }

    public static GameObjectPool getInstance() {
        return instance;
    }

    public HitCircle getCircle() {
        if (!circles.isEmpty()) {
            return circles.poll();
        }

        objectsCreated++;
        return new HitCircle();
    }

    public void putCircle(final HitCircle circle) {
        circles.add(circle);
    }

    public Spinner getSpinner() {
        if (!spinners.isEmpty()) {
            return spinners.poll();
        }

        objectsCreated++;

        return Config.getSpinnerStyle() == 1 ? new ModernSpinner() : new Spinner();
    }

    public void putSpinner(final Spinner spinner) {
        spinners.add(spinner);
    }

    public GameEffect getEffect(final String texname) {
        if (effects.containsKey(texname) && !effects.get(texname).isEmpty()) {
            return effects.get(texname).poll();
        }

        objectsCreated++;
        return new GameEffect(texname);
    }

    public void putEffect(final GameEffect effect) {
        if (!effects.containsKey(effect.getTexname())) {
            effects.put(effect.getTexname(), new LinkedList<>());
        }

        effects.get(effect.getTexname()).add(effect);
    }

    public Slider getSlider() {
        if (!sliders.isEmpty()) {
            return sliders.poll();
        }

        objectsCreated++;
        return new Slider();
    }

    public void putSlider(final Slider slider) {
        sliders.add(slider);
    }

    public FollowTrack getTrack() {
        if (!tracks.isEmpty()) {
            return tracks.poll();
        }

        objectsCreated++;
        return new FollowTrack();
    }

    public void putTrack(final FollowTrack track) {
        tracks.add(track);
    }

    public int getObjectsCreated() {
        return objectsCreated;
    }

    public void purge() {
        effects.clear();
        circles.clear();
        sliders.clear();
        tracks.clear();
        spinners.clear();

        objectsCreated = 0;
    }

    public void preload() {
        for (int i = 0; i < 10; i++) {
            putCircle(new HitCircle());
        }
        for (int i = 0; i < 5; i++) {
            putSlider(new Slider());
            putTrack(new FollowTrack());
        }
        for (int i = 0; i < 3; i++) {
            putSpinner(Config.getSpinnerStyle() == 1 ? new ModernSpinner() : new Spinner());
        }

        objectsCreated = 33;
    }
}
