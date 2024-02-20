package com.reco1l.legacy.graphics

import com.edlplan.andengine.SpriteCache
import com.edlplan.framework.math.line.LinePath
import org.andengine.entity.IEntity
import org.andengine.entity.modifier.FadeInModifier
import org.andengine.entity.modifier.FadeOutModifier
import org.andengine.entity.modifier.SequenceEntityModifier
import org.andengine.entity.scene.Scene
import org.andengine.util.modifier.IModifier
import org.andengine.util.modifier.ease.EaseQuadOut
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager
import ru.nsu.ccfit.zuev.osu.helper.ModifierListener

class SliderBody(val path: LinePath)
{

    var bodyWidth = 0f
    var borderWidth = 0f

    var endLength = 0f
        set(value)
        {
            if (field != value)
            {
                field = value
                shouldRedrawTriangles = true
            }
        }

    var startLength = 0f
        set(value)
        {
            if (field != value)
            {
                field = value
                shouldRedrawTriangles = true
            }
        }


    private var shouldRedrawTriangles = true

    private var bodyColor: Float = 0f
    private var centerColor: Float = 1f
    private var borderColor: Float = 1f

    private var body: PathMesh? = null
    private var border: PathMesh? = null

    private val drawer = PathMeshDrawer()
    private val localPath = LinePath()


    fun applyFadeAdjustments(fadeInDuration: Float)
    {
        body?.registerEntityModifier(FadeInModifier(fadeInDuration))
        border?.registerEntityModifier(FadeInModifier(fadeInDuration))
    }

    fun applyFadeAdjustments(fadeInDuration: Float, fadeOutDuration: Float)
    {
        body?.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(fadeInDuration),
                FadeOutModifier(fadeOutDuration, EaseQuadOut.getInstance())
            )
        )

        border?.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(fadeInDuration),
                FadeOutModifier(fadeOutDuration, EaseQuadOut.getInstance())
            )
        )
    }


    fun setBodyColor(color: RGBColor)
    {
        bodyColor = Float.fromBits((0 shl 24)
            .or((255f * color.b()).toInt() shl 16)
            .or((255f * color.g()).toInt() shl 8)
            .or((255f * color.r()).toInt() shl 0) and -0x1)
    }

    fun setCenterColor(color: RGBColor)
    {
        centerColor = Float.fromBits((0 shl 24)
            .or((255f * color.b()).toInt() shl 16)
            .or((255f * color.g()).toInt() shl 8)
            .or((255f * color.r()).toInt() shl 0) and -0x1)
    }

    fun setBorderColor(color: RGBColor)
    {
        borderColor = Float.fromBits((0 shl 24)
            .or((255f * color.b()).toInt() shl 16)
            .or((255f * color.g()).toInt() shl 8)
            .or((255f * color.r()).toInt() shl 0) and -0x1)
    }


    private fun updateVertices(path: LinePath)
    {
        border!!.setVertices(drawer.drawToBuffer(
            path = path,
            width = borderWidth,
            inColor = borderColor,
            outColor = borderColor,
            calculateSegments = true
        ))

        body!!.setVertices(drawer.drawToBuffer(
            path = path,
            width = bodyWidth,
            inColor = centerColor,
            outColor = bodyColor,
            calculateSegments = false
        ))
    }


    fun onUpdate()
    {
        if (body == null || border == null || !shouldRedrawTriangles)
            return

        shouldRedrawTriangles = false

        updateVertices(path.cutPath(startLength, endLength).fitToLinePath(localPath))
    }


    fun applyToScene(scene: Scene, emptyOnStart: Boolean)
    {
        border = SpriteCache.trianglePackCache.get()
        border!!.alpha = 0f

        body = SpriteCache.trianglePackCache.get()
        body!!.clearDepth = true
        body!!.alpha = 0f

        if (!emptyOnStart)
        {
            startLength = 0f
            endLength = path.measurer.maxLength()

            updateVertices(path)
        }

        scene.attachChild(body, 0)
        scene.attachChild(border, 0)
    }

    fun removeFromScene(duration: Float)
    {
        body?.registerEntityModifier(FadeOutModifier(duration))

        border?.registerEntityModifier(FadeOutModifier(duration, object : ModifierListener() {

            override fun onModifierFinished(m: IModifier<IEntity>, i: IEntity)
            {
                SyncTaskManager.getInstance().run { removeFromScene() }
            }
        }))
    }


    fun removeFromScene()
    {
        body?.detachSelf()
        SpriteCache.trianglePackCache.save(body)
        body = null

        border?.detachSelf()
        SpriteCache.trianglePackCache.save(border)
        border = null
    }
}
