package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.osu.game.Spinner;

public class AutoCursor extends CursorEntity implements ISliderListener {
    private MoveModifier currentModifier;
    /**
     * The ID of the object that the cursor is currently active on.
     */
    private int currentObjectId = -1;

    /**
     * The Easing function to be used on the cursor.
     */
    private final IEaseFunction easeFunction = EaseQuadOut.getInstance();

    public AutoCursor() {
        super();
        this.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        this.setShowing(true);
    }

    private void doEasingAutoMove(float pX, float pY, float durationS) {
        unregisterEntityModifier(currentModifier);
        currentModifier = new MoveModifier(durationS, getX(), pX, getY(), pY, easeFunction);
        registerEntityModifier(currentModifier);
    }

    private void doAutoMove(float pX, float pY, float durationS, GameObjectListener listener) {
        if (durationS <= 0) {
            setPosition(pX, pY, listener);
            click();
        } else {
            doEasingAutoMove(pX, pY, durationS);
        }
        listener.onUpdatedAutoCursor(pX, pY);
    }

    /**
     * Directly moves the cursor's position to the specified position.
     *
     * @param pX       The X coordinate of the new cursor position.
     * @param pY       The Y coordinate of the new cursor position.
     * @param listener The listener that listens to when this cursor is moved.
     */
    public void setPosition(float pX, float pY, GameObjectListener listener) {
        setPosition(pX, pY);
        listener.onUpdatedAutoCursor(pX, pY);
    }

    /**
     * Moves the cursor to the specified object.
     *
     * @param object       The object to move the cursor to.
     * @param secPassed    The amount of seconds that have passed since the game has started.
     * @param approachRate The approach rate of the beatmap.
     * @param listener     The listener that listens to when this cursor is moved.
     */
    public void moveToObject(GameObject object, float secPassed, float approachRate, GameObjectListener listener) {
        if (object == null || currentObjectId == object.getId()) {
            return;
        }

        float movePositionX = object.getPos().x;
        float movePositionY = object.getPos().y;
        float deltaT = object.getHitTime() - secPassed;

        if (object instanceof Spinner) {
            movePositionX = ((Spinner) object).center.x;
            movePositionY = ((Spinner) object).center.y + 50;
        }

        currentObjectId = object.getId();
        if (GameHelper.ms2ar(approachRate * 1000f) > 12f) {
            approachRate *= 500f;
        } else if (GameHelper.ms2ar(approachRate * 1000f) > 10f) {
            approachRate *= 2f;
        }
        float moveDelay = (deltaT / (approachRate * 2f)) + 0.1f;
        doAutoMove(movePositionX, movePositionY, moveDelay, listener);
    }

    @Override
    public void onSliderStart() {
        cursorSprite.onSliderStart();
    }

    @Override
    public void onSliderTracking() {
        cursorSprite.onSliderTracking();
    }

    @Override
    public void onSliderEnd() {
        cursorSprite.onSliderEnd();
    }
}
