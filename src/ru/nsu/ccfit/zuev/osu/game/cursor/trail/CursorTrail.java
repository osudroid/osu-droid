package ru.nsu.ccfit.zuev.osu.game.cursor.trail;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.ScaleInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorTrail extends ParticleSystem {
    private final CursorSprite cursor;

    public CursorTrail(
            PointParticleEmitter emitter,
            int spawnRate,
            TextureRegion pTextureRegion,
            CursorSprite cursor
    ) {
        super(emitter, spawnRate, spawnRate, spawnRate, pTextureRegion);

        this.cursor = cursor;

        // Cancelling the speed multiplier for the trail.
        addParticleModifier(new ExpireModifier(0.1f * GameHelper.getSpeedMultiplier()));
        addParticleModifier(new AlphaModifier(GameHelper.getSpeedMultiplier(), 0.0f, 0f, 0.10f));

        setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        addParticleInitializer(new ScaleInitializer(cursor.baseSize));
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
