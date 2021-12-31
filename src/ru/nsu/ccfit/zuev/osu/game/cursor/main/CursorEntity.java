package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
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

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            particles = new CursorTrail(
                    emitter, 30, 2, 4, cursorSprite.baseSize, trailTex
            );
        }

        attachChild(cursorSprite);
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

    // TODO:finish Longer Tail
    /* private void handleLongerTrail() {  
        if (isShowing && Config.isUseLongTrail()) {
            if (oldPoint != null){
                float px = cursorSprite.getX() - oldPoint.x;
                float py = cursorSprite.getY() - oldPoint.y;
                float ds = (float)Math.sqrt(px * px + py * py);
                float length = (float)Math.sqrt(particleOffsetX * particleOffsetX + particleOffsetY * particleOffsetY);
                int count = (int)(ds / length);
                for (int i = 1; i < count - 1; i++){
                    final Particle particle = new Particle(oldPoint.x + px * i / count + particleOffsetX,
                            oldPoint.y + py * i / count + particleOffsetY, ResourceManager.getInstance().getTexture("cursortrail"));
                    new ExpireModifier(0.25f).onInitializeParticle(particle);
                    new AlphaModifier(1.0f, 0.0f, 0f, 0.25f).onInitializeParticle(particle);
                    particle.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
                    new ScaleInitializer(csize).onInitializeParticle(particle);
                    if (tails == null){
                        tails = new ArrayList<>();
                    }
                    tails.add(particle);
                    GlobalManager.getInstance().getGameScene().getScene().attachChild(particle);
                }
            }
            oldPoint = new PointF(cursorSprite.getX(), cursorSprite.getY());
            if (tails != null){
                for (Particle p : tails){
                    new ExpireModifier(0.25f).onUpdateParticle(p);
                    new AlphaModifier(1.0f, 0.0f, 0f, 0.25f).onUpdateParticle(p);
                    if (p.getAlpha() == 0f){
                        p.setDead(true);
                    }
                }
                for (int i = tails.size() - 1; i >= 0; i--){
                    Particle p = tails.get(i);
                    if (p.isDead()) {
                        GlobalManager.getInstance().getGameScene().getScene().detachChild(p);
                        tails.remove(p);
                    }
                }
            }
        }
    } */

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
