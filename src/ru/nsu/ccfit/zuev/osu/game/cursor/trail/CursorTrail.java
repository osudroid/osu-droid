package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;

public class CursorTrail extends ParticleSystem {
    float DEFAULT_TRAIL_TIME = 0.25f;
    float LONG_TRAIL_TIME = DEFAULT_TRAIL_TIME * 2;


    public CursorTrail(PointParticleEmitter emitter, TextureRegion pTextureRegion) {

        super(
            emitter,
            Config.isUseLongTrail()? TrailConst.LONG_TRAIL_RATE_MIN.v : TrailConst.DEFAULT_TRAIL_RATE_MIN.v,
            Config.isUseLongTrail()? TrailConst.LONG_TRAIL_RATE_MAX.v : TrailConst.DEFAULT_TRAIL_RATE_MAX.v,
            Config.isUseLongTrail()? TrailConst.LONG_TRAIL_MAX_PARTICLES.v : TrailConst.DEFAULT_MAX_PARTICLES.v,
            pTextureRegion
        );

        this.doStartEffects();
    }

    private void doStartEffects() {
        this.fadeOut();
        this.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        this.addParticleInitializer(new ScaleInitializer(CursorSprite.BASE_SIZE));
        this.setParticlesSpawnEnabled(false);
    }

    private void fadeOut() {
        float lifeTime = Config.isUseLongTrail()? LONG_TRAIL_TIME : DEFAULT_TRAIL_TIME;

        this.addParticleModifier(new ExpireModifier(lifeTime));
        this.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, lifeTime));
    }

}
