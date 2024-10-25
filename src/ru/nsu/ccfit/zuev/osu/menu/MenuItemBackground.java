package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;

public class MenuItemBackground extends Sprite {

    private static final RGBColor DEFAULT_COLOR = new RGBColor(240 / 255f, 150 / 255f, 0 / 255f);
    private static final RGBColor ON_TOUCH_COLOR = new RGBColor(1, 1, 1);
    private final ChangeableText title, author;
    private final RGBColor defColor = OsuSkin.get().getColor("MenuItemDefaultColor", DEFAULT_COLOR);
    private final RGBColor onTouchColor = OsuSkin.get().getColor("MenuItemOnTouchColor", ON_TOUCH_COLOR);
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

        defColor.apply(this);
        attachChild(title);
        attachChild(author);
    }


    @Override
    public void reset() {
        defColor.apply(this);
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
            onTouchColor.apply(this);
            if (item != null) {
                item.stopScroll(getY() + pTouchAreaLocalY);
            }
            dx = pTouchAreaLocalX;
            dy = pTouchAreaLocalY;
            return true;
        } else if (pSceneTouchEvent.isActionUp() && !moved) {
            ResourceManager.getInstance().getSound("menuclick").play();
            defColor.apply(this);
            if (item != null) {
                item.select();
            }
            return true;
        } else if (pSceneTouchEvent.isActionOutside()
                || pSceneTouchEvent.isActionMove()
                && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                pTouchAreaLocalY) > 50)) {
            defColor.apply(this);
            moved = true;
            return false;
        }
        return false;
    }
}
