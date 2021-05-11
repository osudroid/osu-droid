package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectData;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;

public class AutoCursor extends CursorEntity {
    private MoveModifier currentModifier;
    /**
     * Whether or not the cursor is currently tracking a slider or spinning a spinner.
     */
    public boolean isMovingAutoSliderOrSpinner;
    /**
     * The ID of the object that the cursor is currently active on.
     */
    private int currentObjectId = -1;

    public AutoCursor() {
        super();
        this.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        this.setShowing(true);
    }

    private void doEasingAutoMove(float pX, float pY, float durationS) {
        this.unregisterEntityModifier(currentModifier);
        currentModifier = new MoveModifier(durationS, this.getX(), pX, this.getY(), pY, EaseExponentialOut.getInstance());
        this.registerEntityModifier(currentModifier);
    }

    private void doAutoMove(float pX, float pY, float durationS, GameObjectListener listener) {
        if (durationS <= 0) {
            this.setPosition(pX, pY);
            listener.onUpdatedAutoCursor(pX, pY);
        } else if (!this.isMovingAutoSliderOrSpinner) {
            doEasingAutoMove(pX, pY, durationS);
            listener.onUpdatedAutoCursor(pX, pY);
        }
    }

    /**
     * Directly moves the cursor's position to the specified position.
     *
     * @param pX The X coordinate of the new cursor position.
     * @param pY The Y coordinate of the new cursor position.
     * @param listener The listener that listens to when this cursor is moved.
     */
    public void setPosition(float pX, float pY, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        this.isMovingAutoSliderOrSpinner = true;
        this.doAutoMove(pX, pY, -1, listener);
    }

    /**
     * Moves the cursor to the specified object.
     *
     * @param object The object to move the cursor to.
     * @param secPassed The amount of seconds that have passed since the game has started.
     * @param listener The listener that listens to when this cursor is moved.
     */
    public void moveToObject(GameObject object, float secPassed, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        if (object == null || currentObjectId == object.getId()) {
            return;
        }

        currentObjectId = object.getId();

        float movePositionX = object.getPos().x;
        float movePositionY = object.getPos().y;
        float moveDelay = object.getHitTime() - secPassed;

        this.doAutoMove(movePositionX, movePositionY, moveDelay, listener);
    }
}
