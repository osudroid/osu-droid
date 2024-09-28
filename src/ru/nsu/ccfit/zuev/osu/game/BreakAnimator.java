package ru.nsu.ccfit.zuev.osu.game;

import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Anchor;
import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.osu.multiplayer.RoomScene;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class BreakAnimator extends GameObject {
    private final Scene scene;
    private final StatisticV2 stat;
    private final Sprite[] arrows = new Sprite[4];
    private float length = 0;
    private float time;
    private ExtendedSprite passfail;
    private String ending;
    private Sprite mark = null;
    private boolean showMark = false;
    private boolean isbreak = false;
    private boolean over = false;
    private Rectangle dimRectangle = null;

    public BreakAnimator(final GameObjectListener listener, final Scene scene,
                         final StatisticV2 stat, final boolean showMark, Rectangle bgSprtie) {
        length = 0;
        this.showMark = showMark;
        this.scene = scene;
        this.stat = stat;
        this.dimRectangle = bgSprtie;
        listener.addPassiveObject(this);

        for (int i = 0; i < 4; i++) {
            arrows[i] = new Sprite(0, 0, ResourceManager.getInstance()
                    .getTexture("play-warningarrow").deepCopy());
            arrows[i]
                    .registerEntityModifier(new LoopEntityModifier(
                            new SequenceEntityModifier(
                                    new FadeInModifier(0.05f),
                                    new DelayModifier(0.1f),
                                    new FadeOutModifier(0.1f))));
            if (i > 1) {
                arrows[i].setFlippedHorizontal(true);
            }
        }
        arrows[0].setPosition(Utils.toRes(64), Utils.toRes(72));
        arrows[1].setPosition(Utils.toRes(64), Config.getRES_HEIGHT()
                - arrows[1].getHeight());
        arrows[2].setPosition(Config.getRES_WIDTH() - arrows[1].getWidth()
                - Utils.toRes(64), Utils.toRes(72));
        arrows[3].setPosition(Config.getRES_WIDTH() - arrows[1].getWidth()
                        - Utils.toRes(64),
                Config.getRES_HEIGHT() - arrows[1].getHeight());

    }

    public boolean isBreak() {
        return isbreak;
    }

    public boolean isOver() {
        final boolean isover = over;
        over = false;
        return isover;
    }

    public void init(final float length) {
        if (this.length > 0 && time < this.length) {
            return;
        }
        isbreak = true;
        over = false;
        this.length = length;
        time = 0;
        ending = stat.getHp() > 0.5f ? "pass" : "fail";

        passfail = new ExtendedSprite();
        passfail.setOrigin(Anchor.Center);
        passfail.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        passfail.setTextureRegion(ResourceManager.getInstance().getTexture("section-" + ending));

        scene.attachChild(passfail, 0);
        passfail.setVisible(false);

        for (int i = 0; i < 4; i++) {
            arrows[i].setVisible(false);
            arrows[i].setIgnoreUpdate(true);
            scene.attachChild(arrows[i], 0);
        }
        if (showMark) {
            final TextureRegion zeroRect = ResourceManager.getInstance()
                    .getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "0");
            mark = new Sprite(Config.getRES_WIDTH() - zeroRect.getWidth() * 11,
                    Utils.toRes(5), ResourceManager.getInstance().getTexture(
                    "ranking-" + stat.getMark() + "-small"));
            mark.setScale(1.2f);
            scene.attachChild(mark, 0);
        }
    }

    private void setBgFade(float percent) {
        if (dimRectangle != null && !Config.isNoChangeDimInBreaks()) {
            dimRectangle.setAlpha((1 - Config.getBackgroundBrightness()) * (1 - percent));
        }
    }

    private void resumeBgFade() {
        if (dimRectangle != null && !Config.isNoChangeDimInBreaks()) {
            dimRectangle.setAlpha(1 - Config.getBackgroundBrightness());
        }
    }


    @Override
    public void update(final float dt) {
        if (length == 0 || time >= length) {
            return;
        }
        time += dt;

        if (length > 3 && time > (length - 1) / 2
                && time - dt < (length - 1) / 2) {
            passfail.setVisible(true);
            passfail.registerEntityModifier(new SequenceEntityModifier(
                    new DelayModifier(0.25f), new FadeOutModifier(0.025f),
                    new DelayModifier(0.025f), new FadeInModifier(0.025f),
                    new DelayModifier(0.6725f), new FadeOutModifier(0.3f)));

            var sound = ResourceManager.getInstance().getCustomSound("section" + ending, 1);
            if (sound != null)
                sound.play();
        }
        if (length - time <= 1 && length - time + dt > 1) {
            for (final Sprite sp : arrows) {
                sp.setVisible(true);
                sp.setIgnoreUpdate(false);
            }

            if (Multiplayer.isMultiplayer)
                RoomScene.INSTANCE.getChat().dismiss();
        }
        if (length > 1) {
            if (time < 0.5f) {
                setBgFade(time * 2);
            } else if (length - time < 0.5f) {
                setBgFade((length - time) * 2);
            } else if (time >= 0.5f && time - dt < 0.5f)
                setBgFade(1);
        }

        if (time >= length) {
            isbreak = false;
            over = true;
            resumeBgFade();
            if (mark != null) {
                mark.detachSelf();
            }
            for (final Sprite sp : arrows) {
                sp.detachSelf();
            }
            passfail.detachSelf();
        }
    }

}
