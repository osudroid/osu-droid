package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.cursor.trail.CursorTrail;

public class CursorEntity extends Entity {
    protected final CursorSprite cursorSprite;
    private ParticleSystem particles = null;
    private PointParticleEmitter emitter = null;
    private boolean isShowing = false;
    private float particleOffsetX, particleOffsetY;

    public CursorEntity() {
        TextureRegion cursorTex = ResourceManager.getInstance().getTexture("cursor");
        cursorSprite = new CursorSprite(-cursorTex.getWidth() / 2f, -cursorTex.getWidth() / 2f, cursorTex);

        if (Config.isUseParticles()) {
            TextureRegion trailTex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -trailTex.getWidth() / 2f;
            particleOffsetY = -trailTex.getHeight() / 2f;

            var spawnRate = 55;

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            particles = new CursorTrail(emitter, spawnRate, cursorSprite.baseSize, trailTex);
            particles.setParticlesSpawnEnabled(false);
        }

        attachChild(cursorSprite);
        setVisible(false);

        // Not necessary to update by itself since it's done by GameScene.
        setIgnoreUpdate(true);
    }

    public void setShowing(boolean showing) {
        isShowing = showing;
        setVisible(showing);
        if (particles != null)
            particles.setParticlesSpawnEnabled(showing);
    }

    public void click() {
        cursorSprite.handleClick();
    }

    public void update(float pSecondsElapsed) {
        // this.handleLongerTrail();
        if(isShowing) {
            cursorSprite.update(pSecondsElapsed);
        }
        super.onManagedUpdate(pSecondsElapsed);
    }

    public void attachToScene(Scene fgScene) {
        if (particles != null) {
            fgScene.attachChild(particles);
        }
        fgScene.attachChild(this);
    }

    @Override
    public void setPosition(float pX, float pY) {
        if (emitter != null)
            emitter.setCenter(pX + particleOffsetX, pY + particleOffsetY);

        super.setPosition(pX, pY);
    }
}
