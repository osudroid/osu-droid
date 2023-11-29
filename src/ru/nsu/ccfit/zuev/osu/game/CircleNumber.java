package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.core.util.Supplier;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CircleNumber extends Entity {

    private final int num;

    public CircleNumber(final int number) {
        super(0, 0);
        num = number;
        final String snum = String.valueOf(Math.abs(number));

        for (int i = 0; i < snum.length(); i++) {
            var tex = ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().getHitCirclePrefix(), String.valueOf(snum.charAt(i)));

            attachChild(new Sprite(0, 0, tex));
        }
    }

    public void init(final PointF pos, float scale) {
        scale *= OsuSkin.get().getComboTextScale();

        var overlap = OsuSkin.get().getHitCircleOverlap();
        float maxWidthScaled = 0f;
        float maxHeight = 0f;

        for (int i = 0; i < getChildCount(); i++) {
            // We assume all attached child are Sprite
            var sprite = (Sprite) getChild(i);

            sprite.setScale(scale);
            sprite.setPosition(maxWidthScaled, 0f);

            maxWidthScaled += sprite.getWidthScaled() - overlap;
            maxHeight = Math.max(maxHeight, sprite.getHeight());
        }

        // Computing max width without scale, so we can properly align the entity.
        var maxWidth = getLastChild().getX() + ((Sprite) getLastChild()).getWidth();

        setPosition(pos.x - maxWidth / 2f, pos.y - maxHeight / 2f);
    }

    public int getNum() {
        return num;
    }

    @Override
    public float getAlpha() {
        if (getFirstChild() != null) {
            return getFirstChild().getAlpha();
        }

        return super.getAlpha();
    }

    @Override
    public void setAlpha(float pAlpha) {
        var count = getChildCount();

        if (count > 0) {
            for (int i = 0; i < count; i++) {
                getChild(i).setAlpha(pAlpha);
            }
        }

        super.setAlpha(pAlpha);
    }

    // The default registerEntityModifier() doesn't apply the modifiers to the nested Entities, so we've to apply to each one.
    // Modifiers cannot be shared between multiple Entities, and using deepCopy() can be expensive, so we use a supplier instead.
    public void registerEntityModifiers(Supplier<IEntityModifier> modifier) {
        for (int i = 0; i < getChildCount(); i++) {
            getChild(i).registerEntityModifier(modifier.get());
        }
    }

}
