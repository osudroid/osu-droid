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
        addParticleModifier(new AlphaParticleModifier(GameHelper.getSpeedMultiplier(), 0.0f, 0f, 0.10f));

        addParticleInitializer(new BlendFunctionParticleInitializer<>(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA));
        addParticleInitializer(new ScaleParticleInitializer(cursor.baseSize));
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
