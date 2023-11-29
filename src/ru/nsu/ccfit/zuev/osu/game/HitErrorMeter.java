package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import java.util.LinkedList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;

/**
 * Created by dgsrz on 15/10/18.
 */
public class HitErrorMeter extends GameObject {

    private final Scene bgScene;

    private final PointF barAnchor;

    private final float barHeight;

    private final float boundary;

    private final List<Rectangle> onDisplayIndicators;

    private final List<Rectangle> recycledIndicators;

    public HitErrorMeter(Scene scene, PointF anchor, float difficulty, float height, DifficultyHelper difficultyHelper) {
        barAnchor = anchor;
        barHeight = height;
        bgScene = scene;

        onDisplayIndicators = new LinkedList<>();
        recycledIndicators = new LinkedList<>();

        boundary = difficultyHelper.hitWindowFor50(difficulty);

        float totalLen = boundary * 1500;
        Rectangle hitMeter = new Rectangle(anchor.x - totalLen / 2, anchor.y - height, totalLen, height * 2);
        hitMeter.setColor(0f, 0f, 0f, 0.8f);
        scene.attachChild(hitMeter);

        Rectangle hit50 = new Rectangle(anchor.x - totalLen / 2, anchor.y - height / 2, totalLen, height);
        hit50.setColor(200f / 255f, 180f / 255f, 110f / 255f, 0.8f);
        scene.attachChild(hit50);

        float hit100Len = difficultyHelper.hitWindowFor100(difficulty) * 1500;
        Rectangle hit100 = new Rectangle(anchor.x - hit100Len / 2, anchor.y - height / 2, hit100Len, height);
        hit100.setColor(100f / 255f, 220f / 255f, 40f / 255f, 0.8f);
        scene.attachChild(hit100);

        float hit300Len = difficultyHelper.hitWindowFor300(difficulty) * 1500;
        Rectangle hit300 = new Rectangle(anchor.x - hit300Len / 2, anchor.y - height / 2, hit300Len, height);
        hit300.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.8f);
        scene.attachChild(hit300);

        Rectangle hitIndicator = new Rectangle(anchor.x - 2, anchor.y - height, 4, height * 2);
        hitIndicator.setColor(1f, 1f, 1f, 0.8f);
        hitIndicator.setZIndex(15);
        scene.attachChild(hitIndicator);
    }

    @Override
    public void update(float dt) {
        while (!onDisplayIndicators.isEmpty()) {
            if (onDisplayIndicators.get(0).getAlpha() <= 0) {
                Rectangle removed = onDisplayIndicators.remove(0);
                removed.setVisible(false);
                removed.setIgnoreUpdate(true);
                removed.detachSelf();
                recycledIndicators.add(removed);
            } else {
                break;
            }
        }
        for (int i = 0, size = onDisplayIndicators.size(); i < size; i++) {
            var result = onDisplayIndicators.get(i);
            float currentAlpha = result.getAlpha() - 0.002f;
            result.setAlpha(currentAlpha);
        }
    }

    public void putErrorResult(float errorResult) {
        if (Math.abs(errorResult) > boundary) {
            return;
        }
        errorResult = errorResult * 750;
        if (recycledIndicators.isEmpty()) {
            Rectangle indicator = new Rectangle(barAnchor.x - 2, barAnchor.y - barHeight, 4, barHeight * 2);
            float posX = indicator.getX() + errorResult;
            float posY = indicator.getY();
            indicator.setPosition(posX, posY);
            indicator.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.6f);
            indicator.setZIndex(10);
            bgScene.attachChild(indicator);
            onDisplayIndicators.add(indicator);
        } else {
            Rectangle indicator = recycledIndicators.remove(0);
            float posX = barAnchor.x - 2 + errorResult;
            float posY = indicator.getY();
            indicator.setPosition(posX, posY);
            indicator.setColor(70f / 255f, 180f / 255f, 220f / 255f, 0.6f);
            indicator.setZIndex(10);
            indicator.setVisible(true);
            indicator.setIgnoreUpdate(false);
            bgScene.attachChild(indicator);
            onDisplayIndicators.add(indicator);
        }
    }

}
