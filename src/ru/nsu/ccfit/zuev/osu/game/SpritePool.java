package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.sprite.Sprite;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;

public class SpritePool {

    private static SpritePool instance = new SpritePool();

    private static int CAPACITY = 250;

    private final Map<String, LinkedList<Sprite>> sprites = new HashMap<String, LinkedList<Sprite>>();

    private final Map<String, LinkedList<AnimSprite>> animsprites = new HashMap<String, LinkedList<AnimSprite>>();

    int count = 0;

    private int spritesCreated = 0;

    private SpritePool() {
    }

    public static SpritePool getInstance() {
        return instance;
    }

    public int getSpritesCreated() {
        return spritesCreated;
    }

    synchronized public void putSprite(final String name, final Sprite sprite) {
        if (count > CAPACITY) {
            return;
        }
        if (sprite.hasParent()) {
            return;
        }

        sprite.setAlpha(1);
        sprite.setColor(1, 1, 1);
        sprite.setScale(1);
        sprite.clearEntityModifiers();
        sprite.clearUpdateHandlers();
        count++;
        if (sprites.containsKey(name)) {
            sprites.get(name).add(sprite);
        } else {
            final LinkedList<Sprite> list = new LinkedList<Sprite>();
            list.add(sprite);
            sprites.put(name, list);
        }
    }

    synchronized public Sprite getSprite(final String name) {
        if (sprites.containsKey(name)) {
            final LinkedList<Sprite> list = sprites.get(name);
            while (list.isEmpty() == false && list.peek().hasParent() == true) {
                list.poll();
            }
            if (list.isEmpty() == false) {
                count--;
                return list.poll();
            }
        }

        spritesCreated++;
        return new Sprite(0, 0, ResourceManager.getInstance().getTexture(name));
    }

    synchronized public Sprite getCenteredSprite(
        final String name, final PointF pos) {
        if (sprites.containsKey(name)) {
            final LinkedList<Sprite> list = sprites.get(name);
            while (list.isEmpty() == false && list.peek().hasParent() == true) {
                list.poll();
            }
            if (list.isEmpty() == false) {
                count--;
                final Sprite sp = list.poll();
                sp.setPosition(pos.x - sp.getWidth() / 2, pos.y - sp.getHeight() / 2);
                return sp;
            }
        }

        spritesCreated++;
        return new CentredSprite(pos.x, pos.y, ResourceManager.getInstance().getTexture(name));
    }

    synchronized public AnimSprite getAnimSprite(final String name, int count) {
        if (animsprites.containsKey(name)) {
            final LinkedList<AnimSprite> list = animsprites.get(name);
            while (list.isEmpty() == false && list.peek().hasParent() == true) {
                list.poll();
            }
            if (list.isEmpty() == false) {
                count--;
                return list.poll();
            }
        }

        spritesCreated++;
        return new AnimSprite(0, 0, name, count, count);
    }

    synchronized public void putAnimSprite(
        final String name, final AnimSprite sprite) {
        if (count > CAPACITY) {
            return;
        }
        if (sprite.hasParent()) {
            return;
        }

        sprite.setAlpha(1);
        sprite.setColor(1, 1, 1);
        sprite.setScale(1);
        sprite.clearEntityModifiers();
        sprite.clearUpdateHandlers();
        count++;
        if (animsprites.containsKey(name)) {
            animsprites.get(name).add(sprite);
        } else {
            final LinkedList<AnimSprite> list = new LinkedList<AnimSprite>();
            list.add(sprite);
            animsprites.put(name, list);
        }
    }

    public void purge() {
        count = 0;
        spritesCreated = 0;
        sprites.clear();
        animsprites.clear();
    }

}
