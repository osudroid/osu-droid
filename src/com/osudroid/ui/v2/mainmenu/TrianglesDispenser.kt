package com.osudroid.ui.v2.mainmenu

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.shape.UITriangle
import com.reco1l.andengine.theme.Colors
import com.reco1l.framework.Color4
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class TrianglesDispenser : UIComponent() {

    /**
     * The minimum scale factor for triangles.
     */
    var triangleMinSize = 0.2f

    /**
     * The maximum scale factor for triangles.
     */
    var triangleMaxSize = 0.4f

    /**
     * The min velocity of the triangles measured in pixels per second.
     */
    var triangleMinVelocity = 40f

    /**
     * The max velocity of the triangles measured in pixels per second.
     */
    var triangleMaxVelocity = 80f

    /**
     * The color palette of the triangles.
     */
    var colorPalette = arrayOf(
        Colors.White * 0.1f,
        Colors.White * 0.125f,
        Colors.White * 0.15f,
    )

    /**
     * The spawn rate in triangles per second.
     */
    var spawnRate = 2f


    /**
     * The triangle shape used for dispensing.
     */
    val triangle = UITriangle()


    /**
     * The base size of the triangle shape relative to the dispenser height.
     */
    private val triangleBaseSize: Float
        get() = height * 0.6f


    private var activeTriangles = mutableListOf<TriangleInfo>()

    private var spawnTimer = 0f


    init {
        attachChild(triangle.apply {
            color = Colors.White
            scaleCenter = Anchor.Center
            bufferSharingMode = BufferSharingMode.Dynamic
        })
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        triangle.width = triangleBaseSize
        triangle.height = triangleBaseSize
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        // Update spawn timer
        spawnTimer += deltaTimeSec
        val spawnInterval = 1f / spawnRate

        // Spawn new triangles
        while (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval
            spawnTriangle()
        }

        // Update existing triangles
        activeTriangles.fastForEach { info ->
            info.update(deltaTimeSec)
        }

        activeTriangles.removeAll { it.y < -(triangleBaseSize * it.scale) }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        activeTriangles.fastForEach { info ->
            triangle.setScale(info.scale)
            triangle.y = info.y
            triangle.x = info.x
            triangle.color = info.color
            triangle.rotation = info.rotation
            triangle.onDraw(gl, camera)
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        super.onManagedDraw(gl, camera)
    }


    private fun spawnTriangle() {
        val scale = Random.nextFloat().let {
            triangleMinSize + it * (triangleMaxSize - triangleMinSize)
        }

        val velocity = Random.nextFloat().let {
            triangleMinVelocity + it * (triangleMaxVelocity - triangleMinVelocity)
        }

        val color = colorPalette.random()
        val x = Random.nextFloat() * width
        val y = height + triangleBaseSize * scale

        val rotation = if (Random.nextBoolean()) 0f else 180f

        activeTriangles.add(TriangleInfo(x, y, scale, velocity, color, rotation))
    }


    data class TriangleInfo(
        var x: Float,
        var y: Float,
        var scale: Float,
        var velocity: Float,
        var color: Color4,
        var rotation: Float,
    ) {
        fun update(deltaTimeSec: Float) {
            y -= velocity * deltaTimeSec
        }
    }

}