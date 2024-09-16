package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.core.util.Supplier;

import com.reco1l.osu.graphics.ExtendedEntity;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CircleNumber extends ExtendedEntity {

    private final int num;

    public CircleNumber(final int number) {
        super(0, 0);
        setOrigin(0.5f, 0.5f);

        num = number;
        var snum = String.valueOf(Math.abs(number));

        for (int i = 0; i < snum.length(); i++) {
            var tex = ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().getHitCirclePrefix(), String.valueOf(snum.charAt(i)));

            attachChild(new Sprite(0, 0, tex));
        }
    }

    public void init(final PointF pos, float scale) {

        setScale(scale);

        var overlap = OsuSkin.get().getHitCircleOverlap();
        var textScale = OsuSkin.get().getComboTextScale();

        float maxWidth = 0f;
        float maxHeight = 0f;

        for (int i = 0; i < getChildCount(); i++)
        {
            // We assume all attached child are Sprite
            var sprite = (Sprite) getChild(i);

            sprite.setScale(textScale);
            sprite.setPosition(maxWidth, 0f);

            maxWidth += sprite.getWidthScaled() - overlap;
            maxHeight = Math.max(maxHeight, sprite.getHeight());
        }

        setSize(maxWidth, maxHeight);
        setPosition(pos.x, pos.y);
    }

    public int getNum() {
        return num;
    }

    @Override
    public void setAlpha(float pAlpha) {

        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChild(i).setAlpha(pAlpha);
        }

        super.setAlpha(pAlpha);
    }

    @Override
    public float getAlpha()
    {
        if (getFirstChild() != null) {
            return getFirstChild().getAlpha();
        }

        return super.getAlpha();
    }

    @Override
    public void setColor(float pRed, float pGreen, float pBlue) {

        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChild(i).setColor(pRed, pGreen, pBlue);
        }

        super.setColor(pRed, pGreen, pBlue);
    }

    @Override
    public float getRed() {

        if (getFirstChild() != null) {
            return getFirstChild().getRed();
        }

        return super.getRed();
    }

    @Override
    public float getGreen() {

        if (getFirstChild() != null) {
            return getFirstChild().getGreen();
        }

        return super.getGreen();
    }

    @Override
    public float getBlue() {

        if (getFirstChild() != null) {
            return getFirstChild().getBlue();
        }

        return super.getBlue();
    }
}
