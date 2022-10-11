package com.reco1l.andengine.entity;

// Created by Reco1l on 18/9/22 19:43

import static com.reco1l.interfaces.IReferences.resources;

import android.util.Log;

import com.reco1l.andengine.IAttachableEntity;

import org.anddev.andengine.entity.particle.ParticleSystem;
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter;
import org.anddev.andengine.entity.particle.initializer.AccelerationInitializer;
import org.anddev.andengine.entity.particle.initializer.RotationInitializer;
import org.anddev.andengine.entity.particle.initializer.VelocityInitializer;
import org.anddev.andengine.entity.particle.modifier.AlphaModifier;
import org.anddev.andengine.entity.particle.modifier.ExpireModifier;
import org.anddev.andengine.entity.particle.modifier.ScaleModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.microedition.khronos.opengles.GL10;

public class ParticleScatter implements IAttachableEntity {

    public boolean isEnabled = false;

    private final ParticleSystem[] scatters;

    //--------------------------------------------------------------------------------------------//

    public ParticleScatter() {
        scatters = new ParticleSystem[2];
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(Scene scene) {
        TextureRegion texture = resources.getTexture("star");

        PointParticleEmitter ppe1 = new PointParticleEmitter(-40, screenHeight * 3 / 4f);
        PointParticleEmitter ppe2 = new PointParticleEmitter(screenWidth, screenHeight * 3 / 4f);

        // Left
        scatters[0] = new ParticleSystem(ppe1, 32, 48, 128, texture);

        scatters[0].addParticleInitializer(new VelocityInitializer(150, 430, -480, -520));
        scatters[0].addParticleInitializer(new AccelerationInitializer(10, 30));

        // Right
        scatters[1] = new ParticleSystem(ppe2, 32, 48, 128, texture);

        scatters[1].addParticleInitializer(new VelocityInitializer(-150, -430, -480, -520));
        scatters[1].addParticleInitializer(new AccelerationInitializer(-10, 30));

        for (ParticleSystem scatter : scatters) {
            scatter.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            scatter.addParticleInitializer(new RotationInitializer(0.0f, 360.0f));

            scatter.addParticleModifier(new ScaleModifier(0.5f, 2.0f, 0.0f, 1.0f));
            scatter.addParticleModifier(new AlphaModifier(1.0f, 0.0f, 0.0f, 1.0f));
            scatter.addParticleModifier(new ExpireModifier(1.0f));

            scatter.setParticlesSpawnEnabled(false);

            scene.attachChild(scatter, 1);
        }
    }

    public void start() {
        if (isEnabled)
            return;

        for (ParticleSystem scatter : scatters) {
            scatter.setParticlesSpawnEnabled(true);
        }
        isEnabled = true;
    }

    public void end() {
        if (!isEnabled)
            return;

        for (ParticleSystem scatter : scatters) {
            scatter.setParticlesSpawnEnabled(false);
        }
        isEnabled = false;
    }

    @Override
    public void update() {}
}
