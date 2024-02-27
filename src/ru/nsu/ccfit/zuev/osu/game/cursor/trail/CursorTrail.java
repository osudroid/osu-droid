package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import android.opengl.GLES20;

import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.BlendFunctionParticleInitializer;
import org.andengine.entity.particle.initializer.ScaleParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class CursorTrail extends SpriteParticleSystem {

    public CursorTrail(PointParticleEmitter emitter, int spawnRate, float trailSize, TextureRegion pTextureRegion) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion, null);

        addParticleInitializer(new BlendFunctionParticleInitializer<>(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA));
        addParticleInitializer(new ScaleParticleInitializer<>(trailSize));
        addParticleInitializer(new ExpireParticleInitializer<>(0.10f));

        addParticleModifier(new AlphaParticleModifier<>(1.0f, 0.0f, 0f, 0.10f));

        setParticlesSpawnEnabled(false);
    }
}
