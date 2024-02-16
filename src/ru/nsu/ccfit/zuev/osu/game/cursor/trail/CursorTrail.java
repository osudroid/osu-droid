package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.ScaleParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class CursorTrail extends SpriteParticleSystem {
    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            float trailSize,
            TextureRegion pTextureRegion
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());

        fadeOut();
        addParticleInitializer(new ScaleParticleInitializer<>(trailSize));
        setParticlesSpawnEnabled(false);
    }

    private void fadeOut() {
        addParticleInitializer(new ExpireParticleInitializer<>(0.10f));
        addParticleModifier(new AlphaParticleModifier<>(1.0f, 0.0f, 0f, 0.10f));
    }
}
