package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import android.opengl.GLES32;

import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.BlendFunctionParticleInitializer;
import org.andengine.entity.particle.initializer.ScaleParticleInitializer;
import org.andengine.entity.particle.modifier.AlphaParticleModifier;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends SpriteParticleSystem {
    private final CursorSprite cursor;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion,
                GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());

        this.cursor = cursor;

        // Cancelling the speed multiplier for the trail.
        addParticleInitializer(new ExpireParticleInitializer(0.1f * GameHelper.getSpeedMultiplier()));
        addParticleModifier(new AlphaParticleModifier(0.0f, 0.1f * GameHelper.getSpeedMultiplier(), 1.0f, 0.0f));

        addParticleInitializer(new BlendFunctionParticleInitializer<>(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA));
        addParticleInitializer(new ScaleParticleInitializer(cursor.baseSize));
        setParticlesSpawnEnabled(false);
        prewarm();
        updateRotation();
    }

    public void update() {
        updateRotation();
    }

    private void prewarm() {
        setParticlesSpawnEnabled(true);
        // Fill every particle slot with one large-delta update.
        onManagedUpdate(10.0f);
        // Drain the spawn accumulator (rate * 10 - max) back to ~0.
        // Each 0.3 s pass spawns a batch and lets it fully expire, consuming
        // the surplus without leaving a burst on the first real gameplay frame.
        // 20 passes covers all realistic spawn rates and speed multipliers.
        for (int i = 0; i < 20; i++) {
            onManagedUpdate(0.3f);
        }
        setParticlesSpawnEnabled(false);
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}
