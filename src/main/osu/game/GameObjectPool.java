package main.osu.game;

import com.reco1l.tools.helpers.ScoringHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import main.osu.Config;

public class GameObjectPool {
    public static GameObjectPool instance = new GameObjectPool();
    public LinkedList<HitCircle> circles = new LinkedList<HitCircle>();
    public Map<Integer, LinkedList<CircleNumber>> numbers = new HashMap<Integer, LinkedList<CircleNumber>>();
    public Map<String, LinkedList<GameEffect>> effects = new HashMap<String, LinkedList<GameEffect>>();
    public LinkedList<Slider> sliders = new LinkedList<Slider>();
    public LinkedList<FollowTrack> tracks = new LinkedList<FollowTrack>();
    public LinkedList<Spinner> spinners = new LinkedList<Spinner>();
    private int objectsCreated = 0;
    private GameObjectPool() {
    }

    public static GameObjectPool getInstance() {
        return instance;
    }

    public HitCircle getCircle() {
        if (circles.isEmpty() == false) {
            return circles.poll();
        }

        objectsCreated++;
        return new HitCircle();
    }

    public void putCircle(final HitCircle circle) {
        circles.add(circle);
    }

    public Spinner getSpinner() {
        if (spinners.isEmpty() == false) {
            return spinners.poll();
        }

        objectsCreated++;
        if (Config.isUseModernStyle()) {
            return new ModernSpinner();
        } else {
            return new Spinner();
        }
    }

    public void putSpinner(final Spinner spinner) {
        spinners.add(spinner);
    }

    public CircleNumber getNumber(final int num) {
        if (numbers.containsKey(num) && numbers.get(num).isEmpty() == false) {
            return numbers.get(num).poll();
        }

        objectsCreated++;
        return new CircleNumber(num);
    }

    public void putNumber(final CircleNumber number) {
        if (numbers.containsKey(number.getNum()) == false) {
            numbers.put(number.getNum(), new LinkedList<CircleNumber>());
        }
        numbers.get(number.getNum()).add(number);
    }

    public GameEffect getEffect(String texname) {
        texname = ScoringHelper.handleLegacyTextures(texname);

        if (effects.containsKey(texname)
                && effects.get(texname).isEmpty() == false) {
            return effects.get(texname).poll();
        }

        objectsCreated++;
        return new GameEffect(texname);
    }

    public void putEffect(final GameEffect effect) {
        if (effects.containsKey(effect.getTexname()) == false) {
            effects.put(effect.getTexname(), new LinkedList<GameEffect>());
        }
        effects.get(effect.getTexname()).add(effect);
    }

    public Slider getSlider() {
        if (sliders.isEmpty() == false) {
            return sliders.poll();
        }

        objectsCreated++;
        return new Slider();
    }

    public void putSlider(final Slider slider) {
        sliders.add(slider);
    }

    public FollowTrack getTrack() {
        if (tracks.isEmpty() == false) {
            return tracks.poll();
        }

        objectsCreated++;
        return new FollowTrack();
    }

    public void putTrac(final FollowTrack track) {
        tracks.add(track);
    }

    public int getObjectsCreated() {
        return objectsCreated;
    }

    public void purge() {
        effects.clear();
        circles.clear();
        numbers.clear();
        sliders.clear();
        tracks.clear();
        objectsCreated = 0;
    }

    public void preload() {
        for (int i = 0; i < 10; i++) {
            putCircle(new HitCircle());
            putNumber(new CircleNumber(i + 1));
        }
        for (int i = 0; i < 5; i++) {
            putSlider(new Slider());
            putTrac(new FollowTrack());
        }
        new Spinner();
        objectsCreated = 31;
    }
}
