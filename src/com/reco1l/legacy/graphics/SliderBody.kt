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

    var bodyColor: RGBColor = RGBColor()
    var centerColor: RGBColor = RGBColor()
    var borderColor: RGBColor = RGBColor()


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


    private fun updateVertices(path: LinePath)
    {
        drawer.drawToBuffer(
            path = path,
            width = bodyWidth,
            inColor = centerColor,
            outColor = bodyColor,
            calculateSegments = true
        )
        body!!.setVertices(drawer.triangles)

        drawer.drawToBuffer(
            path = path,
            width = borderWidth,
            inColor = borderColor,
            outColor = borderColor,
            calculateSegments = false
        )
        border!!.setVertices(drawer.triangles)
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
        body = SpriteCache.trianglePackCache.get()
        border = SpriteCache.trianglePackCache.get()

        body!!.setAlpha(0f)
        body!!.clearDepth = true

        border!!.setAlpha(0f)
        border!!.clearDepth = false

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
