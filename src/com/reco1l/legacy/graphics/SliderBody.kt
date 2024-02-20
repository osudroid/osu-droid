package com.reco1l.legacy.graphics

import com.edlplan.framework.math.line.LinePath
import org.andengine.entity.IEntity
import org.andengine.entity.modifier.FadeInModifier
import org.andengine.entity.modifier.FadeOutModifier
import org.andengine.entity.modifier.SequenceEntityModifier
import org.andengine.entity.scene.Scene
import org.andengine.util.color.ColorUtils
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
                updateVertices(path.cutPath(startLength, endLength).fitToLinePath(localPath))
            }
        }

    var startLength = 0f
        set(value)
        {
            if (field != value)
            {
                field = value
                updateVertices(path.cutPath(startLength, endLength).fitToLinePath(localPath))
            }
        }


    private var bodyColor: Float = 0f
    private var centerColor: Float = 1f
    private var borderColor: Float = 1f


    private val localPath = LinePath()
    private val drawer = PathMeshDrawer()

    private val body = PathMesh(false)
    private val border = PathMesh(true)


    fun applyFadeAdjustments(fadeInDuration: Float)
    {
        body.registerEntityModifier(FadeInModifier(fadeInDuration))
        border.registerEntityModifier(FadeInModifier(fadeInDuration))
    }

    fun applyFadeAdjustments(fadeInDuration: Float, fadeOutDuration: Float)
    {
        body.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(fadeInDuration),
                FadeOutModifier(fadeOutDuration, EaseQuadOut.getInstance())
            )
        )

        border.registerEntityModifier(
            SequenceEntityModifier(
                FadeInModifier(fadeInDuration),
                FadeOutModifier(fadeOutDuration, EaseQuadOut.getInstance())
            )
        )
    }


    fun setBodyColor(color: RGBColor)
    {
        bodyColor = ColorUtils.convertRGBAToABGRPackedFloat(color.r(), color.g(), color.b(), 0f)
    }

    fun setCenterColor(color: RGBColor)
    {
        centerColor = ColorUtils.convertRGBAToABGRPackedFloat(color.r(), color.g(), color.b(), 0f)
    }

    fun setBorderColor(color: RGBColor)
    {
        borderColor = ColorUtils.convertRGBAToABGRPackedFloat(color.r(), color.g(), color.b(), 0f)
    }


    private fun updateVertices(path: LinePath)
    {
        border.setVertices(drawer.drawToBuffer(
            path = path,
            width = borderWidth,
            inColor = borderColor,
            outColor = borderColor,
            asFlat = true
        ))

        body.setVertices(drawer.drawToBuffer(
            path = path,
            width = bodyWidth,
            inColor = centerColor,
            outColor = bodyColor,
            asFlat = false
        ))
    }


    fun applyToScene(scene: Scene, emptyOnStart: Boolean)
    {
        border.alpha = 0f

        body.clearDepth = true
        body.alpha = 0f

        if (!emptyOnStart)
        {
            startLength = 0f
            endLength = path.measurer.maxLength()

            updateVertices(path)
        }

        scene.attachChild(border, 0)
        scene.attachChild(body, 0)
    }

    fun removeFromScene(duration: Float)
    {
        body.registerEntityModifier(FadeOutModifier(duration))

        border.registerEntityModifier(FadeOutModifier(duration, object : ModifierListener()
        {
            override fun onModifierFinished(m: IModifier<IEntity>, i: IEntity)
            {
                SyncTaskManager.getInstance().run { removeFromScene() }
            }
        }))
    }


    fun removeFromScene()
    {
        body.detachSelf()
        border.detachSelf()
    }
}
