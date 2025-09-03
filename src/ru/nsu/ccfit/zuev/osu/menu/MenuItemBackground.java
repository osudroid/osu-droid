package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.framework.Color4;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;

public class MenuItemBackground extends Sprite {

    private static final Color4 DEFAULT_COLOR = new Color4(240 / 255f, 150 / 255f, 0 / 255f);
    private static final Color4 ON_TOUCH_COLOR = new Color4(1f, 1f, 1f);
    private final ChangeableText title, author;
    private final Color4 defColor = OsuSkin.get().getColor("MenuItemDefaultColor", DEFAULT_COLOR);
    private final Color4 onTouchColor = OsuSkin.get().getColor("MenuItemOnTouchColor", ON_TOUCH_COLOR);
    private boolean moved = false;
    private float initialX = 0, initialY = 0;
    private BeatmapSetItem item;
    private final float[] tmp = new float[2];

    public MenuItemBackground() {
        super(0, 0, ResourceManager.getInstance().getTexture(
                "menu-button-background"));

        setAlpha(0.8f);
        title = new ChangeableText(Utils.toRes(32), Utils.toRes(25),
                ResourceManager.getInstance().getFont("font"), "", 255);
        author = new ChangeableText(0, 0, ResourceManager.getInstance()
                .getFont("font"), "", 100);
        author.setPosition(Utils.toRes(150), Utils.toRes(60));

        ComponentsKt.setColor4(this, defColor);
        attachChild(title);
        attachChild(author);
    }


    @Override
    public void reset() {
        ComponentsKt.setColor4(this, defColor);
    }

    public void setItem(final BeatmapSetItem it) {
        item = it;
    }

    public void setTitle(final String newTitle) {
        title.setText(newTitle);
    }

    public void setAuthor(final String newAuthor) {
        author.setText(newAuthor);
    }


    @Override
    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                 final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (!isVisible()) {
            return false;
        }

        float[] coords = convertLocalToSceneCoordinates(pTouchAreaLocalX, pTouchAreaLocalY, tmp);
        float x = coords[0];
        float y = coords[1];

        if (pSceneTouchEvent.isActionDown()) {
            moved = false;
            ComponentsKt.setColor4(this, onTouchColor);
            if (item != null) {
                item.stopScroll(getY() + pTouchAreaLocalY);
            }
            initialX = x;
            initialY = y;
            return true;
        } else if (pSceneTouchEvent.isActionUp() && !moved) {
            ResourceManager.getInstance().getSound("menuclick").play();
            ComponentsKt.setColor4(this, defColor);
            if (item != null) {
                item.select();
            }
            return true;
        } else if ((pSceneTouchEvent.isActionOutside() || pSceneTouchEvent.isActionMove()) && !moved
                && MathUtils.distance(initialX, initialY, x, y) > 50) {
            ComponentsKt.setColor4(this, defColor);
            moved = true;
        }
        return false;
    }
}
