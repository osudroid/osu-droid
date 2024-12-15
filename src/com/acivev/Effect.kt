package com.acivev

import android.content.Context
import org.anddev.andengine.entity.particle.ParticleSystem
import org.anddev.andengine.entity.particle.emitter.PointParticleEmitter
import org.anddev.andengine.entity.particle.initializer.AccelerationInitializer
import org.anddev.andengine.entity.particle.initializer.RotationInitializer
import org.anddev.andengine.entity.particle.initializer.VelocityInitializer
import org.anddev.andengine.entity.particle.modifier.AlphaModifier
import org.anddev.andengine.entity.particle.modifier.ExpireModifier
import org.anddev.andengine.entity.particle.modifier.ScaleModifier
import org.anddev.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import javax.microedition.khronos.opengles.GL10


fun addSnowfall(scene: Scene, context: Context) {
    val snowflakeTexture = ResourceManager.getInstance().getTexture("menu-snow")

    val isHighEndDevice = DevicePerformanceUtil.isLowEndDevice(context)
    val maxParticles = if (isHighEndDevice) 15 else 75

    val snowParticleSystem = ParticleSystem(
        PointParticleEmitter(Config.getRES_WIDTH() / 2f, 0f),  // Emitter at the top center
        10f, 15f, maxParticles,  // Min rate, max rate, max particles
        snowflakeTexture
    ).also { particleSystem ->

        // Random horizontal position across the screen
        particleSystem.addParticleInitializer { particle ->
            particle.setPosition(
                Math.random().toFloat() * Config.getRES_WIDTH(), 0f
            )
        }

        // Random horizontal and vertical speed
        particleSystem.addParticleInitializer(VelocityInitializer(-50f, 50f, 100f, 300f))

        // Gravity effect
        particleSystem.addParticleInitializer(AccelerationInitializer(0f, 10f))

        // Random rotation
        particleSystem.addParticleInitializer(RotationInitializer(0.0f, 360.0f))

        // Random size from .2 to .8
        particleSystem.addParticleModifier(ScaleModifier(0.2f, .8f, 0.0f, 1.0f))

        // Fade out
        particleSystem.addParticleModifier(AlphaModifier(1.0f, 0.0f, 0.0f, 4.0f))

        // Random lifetime between 3 to 7 seconds
        particleSystem.addParticleModifier(ExpireModifier(1.0f, 7.0f))

        particleSystem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        particleSystem.isParticlesSpawnEnabled = true
    }
    scene.attachChild(snowParticleSystem)
}