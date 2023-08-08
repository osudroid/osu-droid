package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

    import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

public class CursorTrail extends ParticleSystem {

    public CursorTrail(
            PointParticleEmitter emitter,
            int minRate,
            int maxRate, int max,
            float trailSize,
            TextureRegion pTextureRegion
    ) {
        super(
                emitter,
                minRate,
                maxRate,
                max,
                pTextureRegion
        );

        fadeOut();
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(trailSize));
        setParticlesSpawnEnabled(false);
    }

    private void fadeOut() {
        float lifeTime = 0.0950f;

        addParticleModifier(new ExpireModifier(lifeTime));
        addParticleModifier(new AlphaModifier(1f, 0f, 1f, 1f));
    }
}
