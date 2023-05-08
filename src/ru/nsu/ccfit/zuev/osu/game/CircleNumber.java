package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import androidx.core.util.Supplier;
import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CircleNumber extends Entity
{

    private final int num;

    public CircleNumber(final int number) {
        super(0, 0);
        num = number;
        final String snum = String.valueOf(Math.abs(number));

        for (int i = 0; i < snum.length(); i++) {
            var tex = ResourceManager.getInstance().getTexture("default-" + snum.charAt(i));

            attachChild(new Sprite(0, 0, tex));
        }
    }

    public void init(final PointF pos, float scale) {
        scale *= OsuSkin.get().getComboTextScale();
        final String snum = String.valueOf(Math.abs(num));

        float twidth = 0;
        final PointF hitpos = new PointF();

        for (int i = 0; i < getChildCount(); i++)
        {
            // We assume all attached child are Sprite
            var sprite = (Sprite) getChild(i);
            var tex = sprite.getTextureRegion();

            hitpos.set(twidth + pos.x, pos.y - tex.getHeight() / 2f);
            twidth += (snum.charAt(i) == '1' && snum.length() > 1) ? scale * tex.getWidth() / 1.5f : scale * tex.getWidth();

            sprite.setPosition(hitpos.x, hitpos.y);
            sprite.setScale(scale);
        }

        twidth /= 2;

        for (int i = 0; i < getChildCount(); i++)
        {
            var sp = getChild(i);
            sp.setPosition(sp.getX() - twidth / scale, sp.getY());
        }
    }

    public int getNum() {
        return num;
    }

    @Override
    public float getAlpha()
    {
        if (getFirstChild() != null)
        {
            return getFirstChild().getAlpha();
        }
        return super.getAlpha();
    }

    // The default registerEntityModifier() doesn't apply the modifiers to the nested Entities, so we've to apply to each one.
    // Modifiers cannot be shared between multiple Entities, and using deepCopy() can be expensive, so we use a supplier instead.
    public void registerEntityModifiers(Supplier<IEntityModifier> modifier)
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            getChild(i).registerEntityModifier(modifier.get());
        }
    }
}
