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
    private boolean isFirstNote = true;
    private MoveModifier currentModifier;
    public boolean isMovingAutoSliderOrSpinner;

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

    public void setPosition(float pX, float pY, float durationS, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        this.isMovingAutoSliderOrSpinner = true;
        this.doAutoMove(pX, pY, durationS, listener);
    }

    public void setPosition(Queue<GameObject> activeObjects, float secPassed, LinkedList<GameObjectData> objects, GameObjectListener listener) {
        if (!GameHelper.isAuto()) {
            return;
        }

        GameObject currentObj = activeObjects.peek();

        if (currentObj == null) {
            return;
        }

        GameObjectData currentObjData = null;
        GameObjectData nextObjData = null;

        if (isFirstNote) {
            isFirstNote = false;
            try {
                nextObjData = objects.getFirst();
            } catch (NoSuchElementException ignore) {}
        } else {
            try {
                currentObjData = objects.get(currentObj.getId());
                nextObjData = objects.get(currentObj.getId() + 1);
            }  catch (IndexOutOfBoundsException ignore) {}
        }

        if (nextObjData == null  || currentObjData != null && secPassed < currentObjData.getTime()) {
            return;
        }

        float movePositionX = nextObjData.getPos().x;
        float movePositionY = nextObjData.getPos().y;
        float moveDelay = nextObjData.getTime() - secPassed;

        this.doAutoMove(movePositionX, movePositionY, moveDelay, listener);
    }
}
