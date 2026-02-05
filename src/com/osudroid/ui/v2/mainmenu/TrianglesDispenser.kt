package com.osudroid.ui.v2.mainmenu

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.shape.UITriangle
import com.reco1l.andengine.theme.Colors
import com.reco1l.framework.Color4
import com.reco1l.toolkt.kotlin.fastForEach
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.engine.camera.Camera
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class TrianglesDispenser : UIComponent() {

    /**
     * The min size of triangles relative to the dispenser size.
     */
    var triangleMinSize = 0.2f

    /**
     * The max size of triangles relative to the dispenser size.
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


    private var activeTriangles = mutableListOf<TriangleInfo>()

    private var spawnTimer = 0f


    init {
        attachChild(triangle.apply {
            width = triangleMinSize
            color = Colors.White
            scaleCenter = Anchor.Center
            allowBufferCache = false
        })
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

        // Remove triangles that went off screen (above the dispenser)
        activeTriangles.removeAll { it.y < -it.size }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        activeTriangles.fastForEachIndexed { index, info ->
            triangle.width = info.size
            triangle.height = info.size
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
        val size = height * Random.nextFloat().let {
            triangleMinSize + it * (triangleMaxSize - triangleMinSize)
        }

        val velocity = Random.nextFloat().let {
            triangleMinVelocity + it * (triangleMaxVelocity - triangleMinVelocity)
        }

        val color = colorPalette.random()
        val x = Random.nextFloat() * width
        val y = height + size

        val rotation = if (Random.nextBoolean()) 0f else 180f

        activeTriangles.add(TriangleInfo(x, y, size, velocity, color, rotation))
    }


    data class TriangleInfo(
        var x: Float,
        var y: Float,
        var size: Float,
        var velocity: Float,
        var color: Color4,
        var rotation: Float,
    ) {
        fun update(deltaTimeSec: Float) {
            y -= velocity * deltaTimeSec
        }
    }

}