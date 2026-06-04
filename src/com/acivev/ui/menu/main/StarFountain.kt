package com.acivev.ui.menu.main

import com.reco1l.andengine.UIScene
import org.andengine.entity.particle.ParticleSystem
import org.andengine.entity.particle.emitter.PointParticleEmitter
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer
import org.andengine.entity.particle.initializer.RotationParticleInitializer
import org.andengine.entity.particle.initializer.VelocityParticleInitializer
import org.andengine.entity.particle.modifier.AlphaParticleModifier
import org.andengine.entity.particle.modifier.ExpireParticleInitializer
import org.andengine.entity.particle.modifier.ScaleParticleModifier
import org.andengine.entity.sprite.Sprite
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.vbo.VertexBufferObjectManager
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Lazer-style star fountain, two particle emitters (left + right) that fire a sweeping arc
 * of stars on kiai onset.
 */
class StarFountain(screenWidth: Float, screenHeight: Float) {

    private val systemLeft:  ParticleSystem<Sprite>?
    private val systemRight: ParticleSystem<Sprite>?

    private val velLeft = VelocityParticleInitializer<Sprite>(0f, 0f, -480f, -520f)
    private val velRight = VelocityParticleInitializer<Sprite>(0f, 0f, -480f, -520f)

    // Shoot state
    var isActive = false
        private set
    private var order = 0
    private var beginMs = 0

    private val shootSound: BassSoundProvider? by lazy {
        ResourceManager.getInstance().loadSound("fountain-shoot", "sfx/fountain-shoot.ogg", false)
    }

    private val loopSound: BassSoundProvider? by lazy {
        ResourceManager.getInstance().loadSound("fountain-loop", "sfx/fountain-loop.ogg", false)
            ?.also { it.setLooping(true) }
    }

    private var loopPlaying = false
    /** Timestamp of the last shoot() call, for the 500 ms retrigger guard. */
    private var lastShootMs = -1

    init {
        val starRegion: TextureRegion? = ResourceManager.getInstance().getTexture("star2")
        val vbo: VertexBufferObjectManager = GlobalManager.getInstance().engine.vertexBufferObjectManager

        val emitY = screenHeight
        systemLeft  = starRegion?.let { buildSystem(screenWidth * 0.25f, emitY, it, vbo, velLeft)  }
        systemRight = starRegion?.let { buildSystem(screenWidth * 0.75f, emitY, it, vbo, velRight) }
    }


    /** Attach the particle systems to [scene] so they are rendered and updated. */
    fun attachTo(scene: UIScene) {
        systemLeft?.let { scene.attachChild(it) }
        systemRight?.let { scene.attachChild(it) }
    }

    /**
     * Fire a burst with a randomly-chosen direction.
     * Call once when kiai starts.
     *
     * @param songPosMs current song position in milliseconds
     */
    fun shoot(songPosMs: Int) {
        // Pick direction: -1 = inward cross, 0 = straight up, 1 = outward spread
        order = listOf(-1, 0, 1).random()
        beginMs = songPosMs
        isActive = true

        // first shoot / gap > 500 ms → stop loop, play one-shot
        // rapid shoot (< 500 ms) → start loop (if not already looping)
        if (lastShootMs < 0 || songPosMs - lastShootMs > SHOOT_RETRIGGER_MS) {
            loopSound?.stop(); loopPlaying = false
            shootSound?.play()
        } else if (!loopPlaying) {
            loopSound?.play(); loopPlaying = true
        }
        lastShootMs = songPosMs

        // Prime initial X angle immediately so frame-0 particles already have the right lean
        applyVelocity(
            leftX = (-order * SWEEP_X_VEL),
            rightX = ( order * SWEEP_X_VEL)
        )
        systemLeft?.isParticlesSpawnEnabled = true
        systemRight?.isParticlesSpawnEnabled = true
    }

    fun update(songPosMs: Int) {
        if (!isActive) return

        val elapsed = (songPosMs - beginMs).toFloat() / 1000f
        if (elapsed >= SHOOT_DURATION_SEC) {
            systemLeft?.isParticlesSpawnEnabled  = false
            systemRight?.isParticlesSpawnEnabled = false
            isActive = false
        } else {
            val sweep = SWEEP_X_VEL * (1f - 2f * elapsed / SHOOT_DURATION_SEC)
            applyVelocity(leftX = -order * sweep, rightX = order * sweep)
        }

        // Stop loop SHOOT_RETRIGGER_MS after the last shoot
        if (loopPlaying && lastShootMs >= 0 && songPosMs - lastShootMs > SHOOT_RETRIGGER_MS) {
            loopSound?.stop(); loopPlaying = false
        }
    }


    /** Immediately stop all particle emission and audio — called on scene exit. */
    fun stop() {
        systemLeft?.isParticlesSpawnEnabled  = false
        systemRight?.isParticlesSpawnEnabled = false
        isActive = false
        if (loopPlaying) { loopSound?.stop(); loopPlaying = false }
        // Reset so the next kiai onset always plays the shoot sound fresh,
        // even if it arrives within SHOOT_RETRIGGER_MS of this stop.
        lastShootMs = -1
    }

    private fun applyVelocity(leftX: Float, rightX: Float) {
        velLeft.setVelocityX(leftX   - X_VARIANCE, leftX   + X_VARIANCE)
        velRight.setVelocityX(rightX - X_VARIANCE, rightX  + X_VARIANCE)
    }

    private fun buildSystem(
        x: Float, y: Float,
        texture: TextureRegion,
        vbo: VertexBufferObjectManager,
        velInit: VelocityParticleInitializer<Sprite>
    ): ParticleSystem<Sprite> = ParticleSystem(
        { px, py -> Sprite(px, py, texture, vbo) },
        PointParticleEmitter(x, y),
        32f, 48f, 128
    ).also {
        it.addParticleInitializer(velInit)
        it.addParticleInitializer(AccelerationParticleInitializer(0f, 150f))
        it.addParticleInitializer(RotationParticleInitializer(0f, 360f))
        it.addParticleModifier(ScaleParticleModifier(0f, 0.8f, 0.5f, 2.2f))
        it.addParticleModifier(AlphaParticleModifier(0f, 1f, 1f, 0f))
        it.addParticleInitializer(ExpireParticleInitializer(0.9f))
        it.isParticlesSpawnEnabled = false
    }

    companion object {
        /** Burst duration*/
        const val SHOOT_DURATION_SEC = 0.8f

        /** Peak X velocity magnitude*/
        const val SWEEP_X_VEL = 500f

        /** Per-particle X random variance */
        const val X_VARIANCE = 60f

        /** Minimum ms between shoot sound retriggering */
        private const val SHOOT_RETRIGGER_MS = 500
    }
}
