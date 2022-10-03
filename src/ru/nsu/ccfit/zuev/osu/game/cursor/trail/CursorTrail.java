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
    public CursorTrail(
            PointParticleEmitter emitter,
            int trailAmount,
            int longTrailRateMultiplier, int longTrailMaxMultiplier,
            float trailSize,
            TextureRegion pTextureRegion
    ) {
        super(
            emitter,
            Config.isUseLongTrail()? trailAmount * longTrailRateMultiplier : trailAmount,
            Config.isUseLongTrail()? trailAmount * longTrailRateMultiplier : trailAmount,
            Config.isUseLongTrail()? trailAmount * longTrailMaxMultiplier : trailAmount,
            pTextureRegion
        );

        fadeOut();
        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(trailSize));
        setParticlesSpawnEnabled(false);
    }

    private void fadeOut() {
        float defaultLifeTime = 0.20f;
        float longLifeTime = defaultLifeTime * 2;
        float lifeTime = Config.isUseLongTrail()? longLifeTime : defaultLifeTime;

        addParticleModifier(new ExpireModifier(lifeTime));
        addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0f, lifeTime));
    }
}
