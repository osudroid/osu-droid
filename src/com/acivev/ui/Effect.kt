package com.acivev.ui

import android.content.Context
import android.opengl.GLES20
import com.acivev.utils.DevicePerformanceUtil
import org.andengine.engine.handler.timer.TimerHandler
import org.andengine.entity.particle.SpriteParticleSystem
import org.andengine.entity.particle.emitter.PointParticleEmitter
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer
import org.andengine.entity.particle.initializer.RotationParticleInitializer
import org.andengine.entity.particle.initializer.VelocityParticleInitializer
import org.andengine.entity.particle.modifier.AlphaParticleModifier
import org.andengine.entity.particle.modifier.ExpireParticleInitializer
import org.andengine.entity.particle.modifier.ScaleParticleModifier
import org.andengine.entity.scene.Scene
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.util.Calendar

fun addSnowfall(scene: Scene, context: Context) {
    val snowflakeTextures = ResourceManager.getInstance().loadHighQualityAsset("snow", "snow.png")

    val isLowEndDevice = DevicePerformanceUtil.isLowEndDevice(context)
    val maxParticles = if (isLowEndDevice) 15 else 75

    val engine = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().engine

    val snowParticleSystem = SpriteParticleSystem(
        PointParticleEmitter(Config.getRES_WIDTH() / 2f, 0f),
        10f, 15f, maxParticles,
        snowflakeTextures,
        engine.vertexBufferObjectManager
    ).also { particleSystem ->

        // Random horizontal position across the screen
        particleSystem.addParticleInitializer { particle ->
            particle.entity.setPosition(
                Math.random().toFloat() * Config.getRES_WIDTH(), 0f
            )
        }

        // Random horizontal and vertical speed
        particleSystem.addParticleInitializer(VelocityParticleInitializer(-50f, 50f, 100f, 300f))

        // Gravity effect
        particleSystem.addParticleInitializer(AccelerationParticleInitializer(0f, 10f))

        // Random rotation
        particleSystem.addParticleInitializer(RotationParticleInitializer(0.0f, 360.0f))

        // Random size from .2 to .8
        particleSystem.addParticleModifier(ScaleParticleModifier(0.2f, .8f, 0.0f, 1.0f))

        // Fade out
        particleSystem.addParticleModifier(AlphaParticleModifier(1.0f, 0.0f, 0.0f, 4.0f))

        // Random lifetime between 1 to 7 seconds
        particleSystem.addParticleInitializer(ExpireParticleInitializer<org.andengine.entity.sprite.Sprite>(1.0f, 7.0f))

        particleSystem.addParticleInitializer { particle ->
            particle.entity.setBlendingEnabled(true)
            particle.entity.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        }
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

    val engine = ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance().engine

    fun launchSingleFirework() {
        val launchX = (Math.random().toFloat() * Config.getRES_WIDTH() * 0.9f) + (Config.getRES_WIDTH() * 0.05f)
        val launchY = Config.getRES_HEIGHT() * 0.9f
        val explosionY = Config.getRES_HEIGHT() * 0.2f

        val trailSystem = SpriteParticleSystem(
            PointParticleEmitter(launchX, launchY),
            2f, 5f, 10,
            rocketTexture,
            engine.vertexBufferObjectManager
        ).apply {
            addParticleInitializer(VelocityParticleInitializer(-20f, 20f, -800f, -600f))
            addParticleModifier(ScaleParticleModifier(0.8f, 0.3f, 0f, 0.5f))
            addParticleModifier(AlphaParticleModifier(1.0f, 0.0f, 0f, 0.5f))
            addParticleInitializer(ExpireParticleInitializer<org.andengine.entity.sprite.Sprite>(0.3f, 0.6f))
            addParticleInitializer { particle ->
                particle.entity.setBlendingEnabled(true)
                particle.entity.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            }
            isParticlesSpawnEnabled = true
        }

        scene.registerUpdateHandler(TimerHandler(0.8f) {
            trailSystem.isParticlesSpawnEnabled = false

            val randomExplosionTexture = explosionTextures.random()

            val explosionSystem = SpriteParticleSystem(
                PointParticleEmitter(launchX, explosionY),
                maxParticles.toFloat(), maxParticles.toFloat(), maxParticles,
                randomExplosionTexture,
                engine.vertexBufferObjectManager
            ).apply {
                addParticleInitializer(VelocityParticleInitializer(-400f, 400f, -400f, 400f))
                addParticleInitializer(AccelerationParticleInitializer(-50f, 50f))

                addParticleInitializer { particle ->
                    val colors = listOf(
                        Triple(1.0f, 0.84f, 0.0f),
                        Triple(1.0f, 0.0f, 0.0f),
                        Triple(0.0f, 0.5f, 1.0f),
                        Triple(0.0f, 1.0f, 0.0f),
                        Triple(0.8f, 0.0f, 1.0f),
                        Triple(1.0f, 0.5f, 0.0f),
                        Triple(1.0f, 1.0f, 1.0f)
                    )
                    val randomColor = colors.random()
                    particle.entity.setColor(randomColor.first, randomColor.second, randomColor.third)
                }

                addParticleModifier(ScaleParticleModifier(0.6f, 0.1f, 0f, 1.2f))
                addParticleModifier(AlphaParticleModifier(1.0f, 0.0f, 0.3f, 1.2f))
                addParticleInitializer(ExpireParticleInitializer<org.andengine.entity.sprite.Sprite>(0.8f, 1.5f))
                addParticleInitializer { particle ->
                    particle.entity.setBlendingEnabled(true)
                    particle.entity.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
                }
                isParticlesSpawnEnabled = true
            }

            scene.attachChild(explosionSystem)

            scene.registerUpdateHandler(TimerHandler(2.0f) {
                scene.detachChild(trailSystem)
                scene.detachChild(explosionSystem)
            })
        })

        scene.attachChild(trailSystem)
    }

    fun scheduleNextFirework() {
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