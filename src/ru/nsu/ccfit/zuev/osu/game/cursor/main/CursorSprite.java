package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import android.graphics.PointF;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.particle.Particle;
import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;

public class CursorSprite extends Entity {
    private Sprite sprite;
    private ParticleSystem particles = null;
    private PointParticleEmitter emitter = null;
    private boolean showing = false;
    private float csize = 2f * Config.getCursorSize();
    //private PointF oldPoint = null;
    private float particleOffsetX, particleOffsetY;
    //private ArrayList<Particle> tails;
    public CursorSprite() {
        TextureRegion tex;

        if (Config.isUseParticles()) {
            tex = ResourceManager.getInstance().getTexture("cursortrail");

            particleOffsetX = -tex.getWidth() / 2;
            particleOffsetY = -tex.getHeight() / 2;

            emitter = new PointParticleEmitter(particleOffsetX, particleOffsetY);
            //构造粒子系统，40,40,30分别是最低粒子生成率、最高粒子生成率、粒子数上限
            //particles = new ParticleSystem(emitter, 40, 40, 30, tex);
            if (Config.isUseLongTrail()) {
                particles = new ParticleSystem(emitter, 60, 60, 120, tex);
                particles.addParticleModifier(new ExpireModifier(0.5f));
                particles.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, 0.5f));
            }
            else{
                particles = new ParticleSystem(emitter, 30, 30, 30, tex);
                particles.addParticleModifier(new ExpireModifier(0.25f));
                particles.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, 0.25f));
            }
            particles.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            particles.addParticleInitializer(new ScaleInitializer(csize));
            //设置粒子消失时间
            //particles.addParticleModifier(new ExpireModifier(0.25f));
            //设置粒子透明度变化
            //particles.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, 0.25f));

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
            //TODO:finish Longer Tail
            /*
            if (Config.isUseLongTrail()) {
                if (oldPoint != null){
                    float px = sprite.getX() - oldPoint.x;
                    float py = sprite.getY() - oldPoint.y;
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
                oldPoint = new PointF(sprite.getX(), sprite.getY());
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
            }*/
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
