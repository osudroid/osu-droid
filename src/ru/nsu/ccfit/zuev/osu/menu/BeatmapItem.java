package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.DatabaseManager;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;

import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class BeatmapItem extends Sprite {

    private static final RGBColor DEFAULT_COLOR = new RGBColor(25 / 255f, 25 / 255f, 240 / 255f);
    private static final RGBColor SELECTED_COLOR = new RGBColor(1, 1, 1);

    private static final RGBColor DEFAULT_TEXT_COLOR = new RGBColor(1, 1, 1);
    private static final RGBColor SELECTED_TEXT_COLOR = new RGBColor(0, 0, 0);
    private final ChangeableText beatmapTitle, beatmapLeftText;
    private final Sprite[] stars;
    private final Sprite halfStar;
    private boolean moved = false;
    private float dx = 0, dy = 0;
    private WeakReference<BeatmapSetItem> item;
    private BeatmapInfo beatmapInfo;
    private String currentMark = null;
    private Sprite mark;
    private float downTime = -1;

    public BeatmapItem() {
        super(0, 0, ResourceManager.getInstance().getTexture(
                "menu-button-background"));

        beatmapTitle = new ChangeableText(Utils.toRes(32), Utils.toRes(22),
                ResourceManager.getInstance().getFont("font"), "", 200);
        beatmapLeftText = new ChangeableText(Utils.toRes(350), Utils.toRes(22),
                ResourceManager.getInstance().getFont("font"), "", 30);
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(beatmapTitle, beatmapLeftText);
        setAlpha(0.8f);
        attachChild(beatmapTitle);
//		attachChild(trackLeftText);

        stars = new Sprite[10];
        for (int i = 0; i < 10; i++) {
            stars[i] = new Sprite(Utils.toRes(60 + 52 * i), Utils.toRes(50),
                    ResourceManager.getInstance().getTexture("star"));
            attachChild(stars[i]);
        }
        final TextureRegion starTex = ResourceManager.getInstance()
                .getTexture("star").deepCopy();
//		starTex.setWidth((starTex.getWidth() / 2));
        halfStar = new Sprite(0, 0, starTex);
        attachChild(halfStar);
    }

    public void setItem(final BeatmapSetItem it) {
        item = new WeakReference<>(it);
    }

    public void setBeatmapInfo(final BeatmapInfo beatmapInfo) {
        this.beatmapInfo = beatmapInfo;
        beatmapTitle.setText(beatmapInfo.getVersion() + " (" + beatmapInfo.getCreator() + ")");
        beatmapLeftText.setText("\n" + beatmapInfo.getTitleText());

        for (final Sprite s : stars) {
            s.setVisible(false);
        }
        halfStar.setVisible(false);

        final float diff = Math.min(beatmapInfo.getStarRating(), 10);

        int fInt = (int) (diff);
        BigDecimal b1 = new BigDecimal(Float.toString(diff));
        BigDecimal b2 = new BigDecimal(Integer.toString(fInt));
        float fPoint = b1.subtract(b2).floatValue();

        for (int j = 0; j < fInt; j++) {
            if (j < stars.length) {
                stars[j].setVisible(true);
            }
        }

        if (fPoint > 0 && fInt != 10) {
            halfStar.setVisible(true);
            halfStar.setPosition(Utils.toRes(60 + 52 * fInt),
                    Utils.toRes(50));
            halfStar.setScale(fPoint);
        }
        updateMark();
    }

    public void updateMark() {
        if (beatmapInfo == null) {
            return;
        }
        var newmark = DatabaseManager.getScoreInfoTable().getBestMark(beatmapInfo.getMD5());
        if (currentMark != null && currentMark.equals(newmark)) {
            return;
        }

        if (mark != null) {
            mark.detachSelf();
        }
        if (newmark != null) {
            mark = new Sprite(Utils.toRes(25), Utils.toRes(55), ResourceManager
                    .getInstance().getTexture("ranking-" + newmark + "-small"));
            attachChild(mark);
        } else {
            mark = null;
        }
        currentMark = newmark;
    }

    public BeatmapInfo getBeatmapInfo() {
        return beatmapInfo;
    }

    public void setDeselectColor() {
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(beatmapTitle, beatmapLeftText);
    }

    public void setSelectedColor() {
        OsuSkin.get().getColor("MenuItemVersionsSelectedColor", SELECTED_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemSelectedTextColor", SELECTED_TEXT_COLOR).applyAll(beatmapTitle, beatmapLeftText);
    }

    @Override
    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                 final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            downTime = 0;
            moved = false;
            setSelectedColor();
            dx = pTouchAreaLocalX;
            dy = pTouchAreaLocalY;
            if (item != null) {
                item.get().stopScroll(getY() + pTouchAreaLocalY);
            }
            return true;
        } else if (pSceneTouchEvent.isActionUp() && !moved) {
            downTime = -1;
            if (item == null) {
                return true;
            }
            if (!item.get().isBeatmapSelected(this)) {
                ResourceManager.getInstance().getSound("menuclick").play();
                item.get().deselectBeatmap();
            }
            item.get().selectBeatmap(this, false);

            return true;
        } else if (pSceneTouchEvent.isActionOutside()
                || pSceneTouchEvent.isActionMove()
                && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                pTouchAreaLocalY) > 50)) {
            downTime = -1;
            setDeselectColor();
            moved = true;
            return false;
        } else {
            return !pSceneTouchEvent.isActionUp();
        }
    }

    void update(final float dt) {
        if (downTime >= 0) {
            downTime += dt;
        }
        if (downTime > 0.5) {
            setSelectedColor();
            moved = true;
            if (item != null) {
                item.get().showPropertiesMenu();
            }
            downTime = -1;
        }
    }
}