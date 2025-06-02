package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.andengine.component.ComponentsKt;
import com.reco1l.framework.ColorARGB;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;

public class MenuItemBackground extends Sprite {

    private static final ColorARGB DEFAULT_COLOR = new ColorARGB(240 / 255f, 150 / 255f, 0 / 255f);
    private static final ColorARGB ON_TOUCH_COLOR = new ColorARGB(1f, 1f, 1f);
    private final ChangeableText title, author;
    private final ColorARGB defColor = OsuSkin.get().getColor("MenuItemDefaultColor", DEFAULT_COLOR);
    private final ColorARGB onTouchColor = OsuSkin.get().getColor("MenuItemOnTouchColor", ON_TOUCH_COLOR);
    private boolean moved = false;
    private float dx = 0, dy = 0;
    private BeatmapSetItem item;

    public MenuItemBackground() {
        super(0, 0, ResourceManager.getInstance().getTexture(
                "menu-button-background"));

        setAlpha(0.8f);
        title = new ChangeableText(Utils.toRes(32), Utils.toRes(25),
                ResourceManager.getInstance().getFont("font"), "", 255);
        author = new ChangeableText(0, 0, ResourceManager.getInstance()
                .getFont("font"), "", 100);
        author.setPosition(Utils.toRes(150), Utils.toRes(60));

        ComponentsKt.setColorARGB(this, defColor);
        attachChild(title);
        attachChild(author);
    }


    @Override
    public void reset() {
        ComponentsKt.setColorARGB(this, defColor);
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
        if (pSceneTouchEvent.isActionDown()) {
            moved = false;
            ComponentsKt.setColorARGB(this, onTouchColor);
            if (item != null) {
                item.stopScroll(getY() + pTouchAreaLocalY);
            }
            dx = pTouchAreaLocalX;
            dy = pTouchAreaLocalY;
            return true;
        } else if (pSceneTouchEvent.isActionUp() && !moved) {
            ResourceManager.getInstance().getSound("menuclick").play();
            ComponentsKt.setColorARGB(this, defColor);
            if (item != null) {
                item.select();
            }
            return true;
        } else if (pSceneTouchEvent.isActionOutside()
                || pSceneTouchEvent.isActionMove()
                && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                pTouchAreaLocalY) > 50)) {
            ComponentsKt.setColorARGB(this, defColor);
            moved = true;
            return false;
        }
        return false;
    }
}
