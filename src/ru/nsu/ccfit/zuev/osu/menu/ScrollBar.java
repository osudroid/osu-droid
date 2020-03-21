package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Utils;

public class ScrollBar {
    private final Rectangle barRectangle;
    private float maxY = 0;
    private boolean visible;

    public ScrollBar(final Scene scene, final IScrollBarListener listener) {
        visible = false;

        barRectangle = new Rectangle(Config.getRES_WIDTH() - Utils.toRes(20),
                0, Utils.toRes(20), Utils.toRes(50));
        barRectangle.setAlpha(0.8f);
        barRectangle.setColor(1, 1, 1);

        scene.attachChild(barRectangle);
        barRectangle.setVisible(false);
    }

    public void setPosition(final float vy, final float maxy) {
        maxY = maxy;
        if (!visible) {
            return;
        }
        barRectangle
                .setPosition(barRectangle.getX(),
                        (Config.getRES_HEIGHT() - barRectangle.getHeight())
                                * vy / maxY);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean vis) {
        barRectangle.setVisible(vis);
        if (vis == true && visible == false) {
            final IEntity parent = barRectangle.getParent();
            parent.detachChild(barRectangle);
            parent.attachChild(barRectangle);
        }
        visible = vis;
    }
}
