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
        updateRotation();
    }

    public void update() {
        updateRotation();
    }

    private void updateRotation() {
        if (OsuSkin.get().isRotateCursorTrail()) {
            setRotation(cursor.getRotation());
        }
    }
}
