package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.edlplan.framework.easing.Easing;
import com.reco1l.andengine.Modifiers;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.GameObject;
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner;

public class AutoCursor extends CursorEntity implements ISliderListener {
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
        clearEntityModifiers();
        registerEntityModifier(Modifiers.move(durationS, getX(), pX, getY(), pY, null, Easing.Out));
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
     * @param listener     The listener that listens to when this cursor is moved.
     */
    public void moveToObject(GameObject object, float secPassed, GameObjectListener listener) {
        if (object == null || currentObjectId == object.getId()) {
            return;
        }

        float movePositionX = object.getPosition().x;
        float movePositionY = object.getPosition().y;
        float deltaT = object.getHitTime() - secPassed;

        if (object instanceof GameplaySpinner spinner) {
            movePositionX = spinner.center.x;
            movePositionY = spinner.center.y + 50;
        }

        currentObjectId = object.getId();

        if (deltaT < 0.085f && !(object instanceof GameplaySpinner)) {
            deltaT = 0.085f;
        }

        doAutoMove(movePositionX, movePositionY, deltaT, listener);
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
