package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;

public class CircleNumber extends GameObject {
    private final Sprite[] digits;
    private final int num;

    public CircleNumber(final int number) {
        num = number;
        final String snum = String.valueOf(Math.abs(number));

        digits = new Sprite[snum.length()];
        for (int i = 0; i < snum.length(); i++) {
            final TextureRegion tex = ResourceManager.getInstance().getTexture(
                    "default-" + snum.charAt(i));
            digits[i] = new Sprite(0, 0, tex);
        }
    }

    public void init(final Scene scene, final PointF pos, float scale,
                     final IEntityModifier... entityModifiers) {
        scale *= OsuSkin.get().getComboTextScale();
        final String snum = String.valueOf(Math.abs(num));

        float twidth = 0;
        final PointF hitpos = new PointF();
        for (int i = 0; i < snum.length(); i++) {
            final TextureRegion tex = digits[i].getTextureRegion();
            hitpos.set(twidth + pos.x, pos.y - tex.getHeight() / 2);
            twidth += (snum.charAt(i) == '1' && snum.length() > 1) ? scale
                    * tex.getWidth() / 1.5f : scale * tex.getWidth();
            digits[i].setPosition(hitpos.x, hitpos.y);

            digits[i].registerEntityModifier(new ParallelEntityModifier(
                    entityModifiers));
            digits[i].setScale(scale);
            scene.attachChild(digits[i], 0);
        }

        twidth /= 2;

        for (final Sprite sp : digits) {
            sp.setPosition(sp.getX() - twidth / scale, sp.getY());
        }
    }

    public int getNum() {
        return num;
    }

    public void detach(final boolean sync) {
        if (sync) {
            SyncTaskManager.getInstance().run(new Runnable() {


                public void run() {
                    for (final Sprite sp : digits) {
                        sp.clearEntityModifiers();
                        sp.detachSelf();
                    }
                }
            });
        } else {
            for (final Sprite sp : digits) {
                sp.clearEntityModifiers();
                sp.detachSelf();
            }
        } // if (sync)
    }


    @Override
    public void update(final float dt) {
    }
}
