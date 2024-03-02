package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Arrays;

import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;

public class MenuItemTrack extends Sprite {

    private static final RGBColor DEFAULT_COLOR = new RGBColor(25 / 255f, 25 / 255f, 240 / 255f);
    private static final RGBColor SELECTED_COLOR = new RGBColor(1, 1, 1);

    private static final RGBColor DEFAULT_TEXT_COLOR = new RGBColor(1, 1, 1);
    private static final RGBColor SELECTED_TEXT_COLOR = new RGBColor(0, 0, 0);
    private final ChangeableText trackTitle, trackLeftText;
    private final Sprite[] stars;
    private final Sprite[] halfStars;
    private boolean moved = false;
    private float dx = 0, dy = 0;
    private WeakReference<MenuItem> item;
    private TrackInfo track;
    private String currentMark = null;
    private Sprite mark;
    private float downTime = -1;

    public MenuItemTrack() {
        super(0, 0, ResourceManager.getInstance().getTexture(
                "menu-button-background"));

        trackTitle = new ChangeableText(Utils.toRes(32), Utils.toRes(22),
                ResourceManager.getInstance().getFont("font"), "", 200);
        trackLeftText = new ChangeableText(Utils.toRes(350), Utils.toRes(22),
                ResourceManager.getInstance().getFont("font"), "", 30);
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, trackLeftText);
        setAlpha(0.8f);
        attachChild(trackTitle);
//		attachChild(trackLeftText);

        stars = new Sprite[20];
        for (int i = 0; i < 20; i++) {
            stars[i] = new Sprite(Utils.toRes(60 + 52 * i), Utils.toRes(50),
                    ResourceManager.getInstance().getTexture("star"));
            stars[i].setVisible(false);
            attachChild(stars[i]);
        }
        final TextureRegion starTex = ResourceManager.getInstance()
                .getTexture("star").deepCopy();
//		starTex.setWidth((starTex.getWidth() / 2));
        halfStars = new Sprite[2];
        for (int i = 0; i < 2; i++) {
            halfStars[i] = new Sprite(0, 0, starTex);
            halfStars[i].setVisible(false);
            attachChild(halfStars[i]);
        }
    }

    public void setItem(final MenuItem it) {
        item = new WeakReference<>(it);
    }

    public void setTrack(final TrackInfo track, final BeatmapInfo info) {
        this.track = track;
        trackTitle.setText(track.getMode() + " (" + track.getCreator() + ")");
        trackLeftText.setText("\n" + info.getTitle());

        for (var s : stars) {
            s.setVisible(false);
        }

        for (var s : halfStars) {
            s.setVisible(false);
        }

        if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.both) {
            float baseX = 30;
            float baseY = 45;

            constructStars(
                track.getDroidDifficulty(),
                Arrays.copyOfRange(stars, 0, 10),
                halfStars[0],
                baseX,
                baseY,
                0.5f
            );

            constructStars(
                track.getStandardDifficulty(),
                Arrays.copyOfRange(stars, 11, 20),
                halfStars[1],
                baseX,
                baseY + 25,
                0.5f
            );
        } else {
            constructStars(
                Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard ?
                    track.getStandardDifficulty() : track.getDroidDifficulty(),
                Arrays.copyOfRange(stars, 0, 10),
                halfStars[0],
                60,
                50,
                1
            );
        }

        updateMark();
    }

    public void updateMark() {
        if (track == null) {
            return;
        }
        final String newmark = ScoreLibrary.getInstance().getBestMark(
                track.getFilename());
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

    public TrackInfo getTrack() {
        return track;
    }

    public void setDeselectColor() {
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, trackLeftText);
    }

    public void setSelectedColor() {
        OsuSkin.get().getColor("MenuItemVersionsSelectedColor", SELECTED_COLOR).apply(this);
        OsuSkin.get().getColor("MenuItemSelectedTextColor", SELECTED_TEXT_COLOR).applyAll(trackTitle, trackLeftText);
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
            if (!item.get().isTrackSelected(this)) {
                ResourceManager.getInstance().getSound("menuclick").play();
                item.get().deselectTrack();
            }
            item.get().selectTrack(this, false);

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

    private void constructStars(float difficulty, Sprite[] stars, Sprite halfStar, float baseX, float y, float scale) {
        final float diff = Math.min(difficulty, 10);

        int fInt = (int) (diff);
        BigDecimal b1 = new BigDecimal(Float.toString(difficulty));
        BigDecimal b2 = new BigDecimal(Integer.toString(fInt));
        float fPoint = b1.subtract(b2).floatValue();

        for (int i = 0; i < fInt; i++) {
            if (i < stars.length) {
                stars[i].setPosition(baseX + 52 * i * scale, y);
                stars[i].setScale(scale);
                stars[i].setVisible(true);
            }
        }

        if (fPoint > 0 && fInt != 10) {
            halfStar.setPosition(baseX + 52 * fInt * scale, y);
            halfStar.setScale(fPoint * scale);
            halfStar.setVisible(true);
        }
    }
}
