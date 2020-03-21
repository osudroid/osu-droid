package ru.nsu.ccfit.zuev.osu.game.cursor;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class CursorSprite extends Entity {
    private Sprite sprite;
    private ParticleSystem particles = null;
    private PointParticleEmitter emitter = null;
    private boolean showing = false;
    private float csize = 2f * Config.getCursorSize();

    private float particleOffsetX, particleOffsetY;

    public CursorSprite() {
        TextureRegion tex;

        if (Config.isUseParticles()) {
            tex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -tex.getWidth() / 2;
            particleOffsetY = -tex.getHeight() / 2;

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            particles = new ParticleSystem(emitter, 40, 40, 30, tex);
            particles.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            particles.addParticleInitializer(new ScaleInitializer(csize));

            particles.addParticleModifier(new ExpireModifier(0.25f));
            particles.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, 0.25f));

            particles.setParticlesSpawnEnabled(false);
        }


        tex = ResourceManager.getInstance().getTexture("cursor");
        sprite = new Sprite(-tex.getWidth() / 2, -tex.getHeight() / 2, tex);
        sprite.setScale(csize);
        attachChild(sprite);
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
        if (particles != null)
            particles.setParticlesSpawnEnabled(showing);
    }

    public void click() {
        sprite.setScale(csize * 1.25f);
    }

    public void update(float pSecondsElapsed) {
        if (showing) {
            if (!sprite.isVisible())
                sprite.setVisible(true);
            if (sprite.getAlpha() < 1)
                sprite.setAlpha(1);
        } else {
            if (sprite.getAlpha() > 0)
                sprite.setAlpha(Math.max(0, sprite.getAlpha() - 4f * pSecondsElapsed));
            else
                sprite.setVisible(false);
        }
        if (sprite.getScaleX() > 2f) {
            sprite.setScale(Math.max(csize, sprite.getScaleX() - (csize * 0.75f) * pSecondsElapsed));
        }
        super.onManagedUpdate(pSecondsElapsed);
    }


    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null)
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);
        super.setPosition(pX, pY);
    }


    public ParticleSystem getParticles() {
        return particles;
    }


}
