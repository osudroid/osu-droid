package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.ScaleParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class CursorTrail extends SpriteParticleSystem {

    public CursorTrail(PointParticleEmitter emitter, int spawnRate, float trailSize, TextureRegion pTextureRegion) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());

        addParticleInitializer(new ScaleParticleInitializer<>(trailSize));
        addParticleInitializer(new ExpireParticleInitializer<>(0.10f));

        addParticleModifier(new AlphaParticleModifier<>(0.0f, 0.10f, 1f, 0f));

        setParticlesSpawnEnabled(false);
    }
}
