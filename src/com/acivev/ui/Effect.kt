package com.acivev.ui

import android.content.Context
import com.acivev.utils.DevicePerformanceUtil
import org.anddev.andengine.engine.handler.timer.TimerHandler
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
import java.util.Calendar
import javax.microedition.khronos.opengles.GL10

fun addSnowfall(scene: Scene, context: Context) {
    val snowflakeTextures = ResourceManager.getInstance().loadHighQualityAsset("snow", "snow.png")

    val isLowEndDevice = DevicePerformanceUtil.isLowEndDevice(context)
    val maxParticles = if (isLowEndDevice) 15 else 75

    val snowParticleSystem = ParticleSystem(
        PointParticleEmitter(Config.getRES_WIDTH() / 2f, 0f),  // Emitter at the top center
        10f, 15f, maxParticles,  // Min rate, max rate, max particles
        snowflakeTextures
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

        // Random lifetime between 1 to 7 seconds
        particleSystem.addParticleModifier(ExpireModifier(1.0f, 7.0f))

        particleSystem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        particleSystem.isParticlesSpawnEnabled = true
    }
    scene.attachChild(snowParticleSystem)
}

fun addFireworks(scene: Scene, context: Context) {
    val rocketTexture = ResourceManager.getInstance().loadHighQualityAsset("firework_rocket", "firework_rocket.png")
    val explosionTexture1 = ResourceManager.getInstance().loadHighQualityAsset("firework_explosion1", "firework_explosion1.png")
    val explosionTexture2 = ResourceManager.getInstance().loadHighQualityAsset("firework_explosion2", "firework_explosion2.png")
    val explosionTexture3 = ResourceManager.getInstance().loadHighQualityAsset("firework_explosion3", "firework_explosion3.png")

    val explosionTextures = listOf(explosionTexture1, explosionTexture2, explosionTexture3)

    val isLowEndDevice = DevicePerformanceUtil.isLowEndDevice(context)
    val maxParticles = if (isLowEndDevice) 10 else 25

    fun launchSingleFirework() {
        // Random launch position along bottom of screen
        val launchX = (Math.random().toFloat() * Config.getRES_WIDTH() * 0.9f) + (Config.getRES_WIDTH() * 0.05f)
        val launchY = Config.getRES_HEIGHT() * 0.9f

        // Explosion height (top area)
        val explosionY = Config.getRES_HEIGHT() * 0.2f

        // Trail effect
        val trailSystem = ParticleSystem(
            PointParticleEmitter(launchX, launchY),
            2f, 5f, 10,
            rocketTexture
        ).apply {

            // Random horizontal and vertical speed
            addParticleInitializer(VelocityInitializer(-20f, 20f, -800f, -600f))

            // Random size from .8 to .3
            addParticleModifier(ScaleModifier(0.8f, 0.3f, 0f, 0.5f))

            // Fade out
            addParticleModifier(AlphaModifier(1.0f, 0.0f, 0f, 0.5f))

            // Random lifetime between .3 to .6 seconds
            addParticleModifier(ExpireModifier(0.3f, 0.6f))

            setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
            isParticlesSpawnEnabled = true
        }

        // Explosion effect (burst at top)
        scene.registerUpdateHandler(TimerHandler(0.8f) {
            trailSystem.isParticlesSpawnEnabled = false

            val randomExplosionTexture = explosionTextures.random()

            val explosionSystem = ParticleSystem(
                PointParticleEmitter(launchX, explosionY),
                maxParticles.toFloat(), maxParticles.toFloat(), maxParticles,
                randomExplosionTexture
            ).apply {
                addParticleInitializer(VelocityInitializer(-400f, 400f, -400f, 400f))
                addParticleInitializer(AccelerationInitializer(-50f, 50f))

                // Add random color initializer
                addParticleInitializer { particle ->
                    val colors = listOf(
                        Triple(1.0f, 0.84f, 0.0f),   // Gold
                        Triple(1.0f, 0.0f, 0.0f),    // Red
                        Triple(0.0f, 0.5f, 1.0f),    // Blue
                        Triple(0.0f, 1.0f, 0.0f),    // Green
                        Triple(0.8f, 0.0f, 1.0f),    // Purple
                        Triple(1.0f, 0.5f, 0.0f),    // Orange
                        Triple(1.0f, 1.0f, 1.0f)     // White
                    )
                    val randomColor = colors.random()
                    particle.setColor(randomColor.first, randomColor.second, randomColor.third)
                }

                addParticleModifier(ScaleModifier(0.6f, 0.1f, 0f, 1.2f))
                addParticleModifier(AlphaModifier(1.0f, 0.0f, 0.3f, 1.2f))
                addParticleModifier(ExpireModifier(0.8f, 1.5f))
                setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE)
                isParticlesSpawnEnabled = true
            }

            scene.attachChild(explosionSystem, 1)

            scene.registerUpdateHandler(
                TimerHandler(
                    2.0f
                ) {
                    scene.detachChild(trailSystem)
                    scene.detachChild(explosionSystem)
                }
            )
        })

        //I intentionally want the trail to be visible, so that it is visible before the logo.
        scene.attachChild(trailSystem)
    }

    fun scheduleNextFirework() {
        // Random delay between 1.5 and 4 seconds
        val randomDelay = 1.5f + Math.random().toFloat() * 2.5f

        scene.registerUpdateHandler(TimerHandler(randomDelay) {
            launchSingleFirework()
            scheduleNextFirework()
        })
    }

    launchSingleFirework()
    scheduleNextFirework()
}


// Check if current date is within the snowfall period (Dec 15 - Jan 6), excluding fireworks time
fun addSnowfallWithPeriod(scene: Scene, context: Context) {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    // Check if it's fireworks period (January 1st, 00:00 to 20:00)
    val isFireworksPeriod = (month == Calendar.JANUARY && day == 1 && hour < 20)

    // December is month 11 (January is month 0)
    val isSnowfallPeriod = (month == Calendar.DECEMBER && day >= 15)
            || (month == Calendar.JANUARY && day <= 6)

    // Show snowfall only if it's snowfall period
    if (!isSnowfallPeriod || isFireworksPeriod) {
        return  // Don't show snowfall outside the period or during fireworks
    }

    addSnowfall(scene, context)
}

// Check if current date is January 1st within 20hrs
fun addFireworksWithPeriod(scene: Scene, context: Context) {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    // January 1st (January is month 0), first 20 hours (00:00 to 20:00)
    val isFireworksPeriod = (month == Calendar.JANUARY && day == 1 && hour < 20)

    if (!isFireworksPeriod) {
        return  // Don't show fireworks outside the period
    }

    addFireworks(scene, context)
}