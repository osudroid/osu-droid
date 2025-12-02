package com.osudroid.ui.v2.mainmenu

import com.edlplan.framework.easing.Easing
import com.osudroid.BuildSettings
import com.osudroid.MusicManager
import com.osudroid.RythimManager
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.resources.R
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.component.forEach
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.container
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.fillContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.modifier.ModifierType
import com.reco1l.andengine.scrollableContainer
import com.reco1l.andengine.shape.UIGradientBox
import com.reco1l.andengine.sprite
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.textButton
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.theme.vh
import com.reco1l.andengine.ui.UIButton
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.LibraryManager

import com.reco1l.andengine.ui.plus
import org.anddev.andengine.engine.camera.Camera
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs


object MainScene : UIScene() {

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: UITextButton
    private lateinit var leftFlash: UIGradientBox
    private lateinit var rightFlash: UIGradientBox
    private lateinit var background: UISprite
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: UIContainer
    private lateinit var menuGradientBox: UIGradientBox

    private val isMenuExpanded
        get() = menuContainer.width > 0f


    init {

        container {
            width = Size.Full
            height = Size.Full

            background = sprite {
                width = Size.Full
                height = Size.Full
                scaleType = ScaleType.Crop
            }

            +UIGradientBox().apply {
                height = Size.Full
                gradientAngle = 0f
                colorStart = Colors.White
                colorEnd = Colors.Transparent
                style = {
                    width = 12f.rem
                }

                alpha = 0f
                leftFlash = this
            }

            +UIGradientBox().apply {
                height = Size.Full
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                gradientAngle = 180f
                colorStart = Colors.White
                colorEnd = Colors.Transparent
                style = {
                    width = 12f.rem
                }

                alpha = 0f
                rightFlash = this
            }

            +UIGradientBox().apply {
                width = Size.Full
                height = Size.Full
                gradientAngle = 270f
                alpha = 0f
                style = {
                    colorStart = it.accentColor * 0.1f
                    colorEnd = (it.accentColor * 0.1f) / 0.5f
                }
                menuGradientBox = this
            }
        }

        linearContainer {
            orientation = Orientation.Horizontal
            anchor = Anchor.Center
            origin = Anchor.Center

            +OsuLogo().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleCenter = Anchor.Center
                style = {
                    width = 16f.rem
                    height = 16f.rem
                }

                onActionUp = {
                    menuContainer.apply {
                        clearModifiers(ModifierType.SizeX, ModifierType.Alpha)

                        if (isMenuExpanded) {
                            sizeToX(0f, 0.2f, Easing.OutQuint)
                            fadeOut(0.2f, Easing.OutQuint)
                        } else {
                            sizeToX(intrinsicWidth, 0.2f, Easing.OutQuint)
                            fadeIn(0.2f, Easing.OutQuint)
                        }
                    }

                    menuGradientBox.apply {
                        clearModifiers(ModifierType.Alpha)

                        if (isMenuExpanded) {
                            fadeOut(0.4f, Easing.OutQuint)
                        } else {
                            fadeIn(0.4f, Easing.OutQuint)
                        }
                    }
                }

                logo = this
            }

            menuContainer = scrollableContainer {
                scrollAxes = Axes.Y
                verticalIndicator = null
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    padding = Vec4(8f.srem, 0f, 0f, 0f)
                    maxHeight = 1f.vh
                }
                width = 0f
                alpha = 0f

                +CarrouselLinearContainer(logo).apply {
                    orientation = Orientation.Vertical
                    style = {
                        spacing = 3f.srem
                        padding = Vec4(0f, 8f.rem)
                    }

                    +MenuButton(Icon.User, "Solo").apply {
                        onActionUp = {
                            async {
                                LoadingScreen().show()

                                GlobalManager.getInstance().mainActivity.checkNewSkins()
                                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                                if (LibraryManager.getLibrary().isEmpty()) {
                                    UIEngine.current.scene = this@MainScene
                                    BeatmapListing().show()
                                } else {
                                    GlobalManager.getInstance().songService.isGaming = true
                                    GlobalManager.getInstance().songMenu.reload()
                                    GlobalManager.getInstance().songMenu.show()
                                    GlobalManager.getInstance().songMenu.select()
                                }
                            }
                        }
                    }

                    +MenuButton(Icon.UserGroup, "Multi").apply {
                        onActionUp = action@{
                            if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_MULTIPLAYER) {
                                ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                                return@action
                            }

                            GlobalManager.getInstance().songService.isGaming = true
                            Multiplayer.isMultiplayer = true

                            async {
                                LoadingScreen().show()

                                GlobalManager.getInstance().mainActivity.checkNewSkins()
                                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                                GlobalManager.getInstance().songMenu.reload()
                                GlobalManager.getInstance().engine.scene = LobbyScene()
                            }
                        }
                    }

                    +MenuButton(Icon.TableList, "Browse").apply {
                        onActionUp = {
                            mainThread { BeatmapListing().show() }
                        }
                    }

                    +MenuButton(Icon.Gear, "Settings").apply {
                        onActionUp = {
                            mainThread { SettingsFragment().show() }
                        }
                    }

                    +MenuButton(Icon.ArrowRightFromBracket, "Exit").apply {
                        onActionUp = {

                        }
                    }

                }

            }

        }

        container {
            width = Size.Full
            style = {
                padding = UIEngine.current.safeArea.copy(y = 2f.srem, w = 2f.srem)
            }

            +PlayerButton().apply {
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                scaleCenter = Anchor.Center
                playerButton = this
            }

            musicButton = textButton {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                scaleCenter = Anchor.Center
                leadingIcon = FontAwesomeIcon(Icon.Music)

                val musicPlayer = MusicPlayer(this)
                onActionUp = {
                    musicPlayer.show()
                }
            }
        }

        RythimManager.addOnBeatChangeListener(this) {
            if (!RythimManager.isKiai) {
                if (RythimManager.beatIndex == 0) {
                    leftFlash.alpha = 0.35f
                    rightFlash.alpha = 0.35f
                }
            } else {
                // +1 Because 0 is also even and we don't want double flashes on the first beat
                if ((RythimManager.beatIndex + 1) % 2 == 0) {
                    leftFlash.alpha = 0.4f
                } else {
                    rightFlash.alpha = 0.4f
                }
            }

        }

        MusicManager.addOnBeatmapChangeListener(this) { beatmap ->
            val textureRegion = if (beatmap != null) ResourceManager.getInstance().loadBackground(beatmap.backgroundPath) else null

            if (textureRegion != null) {
                background.textureRegion = textureRegion
            } else {
                background.textureRegion = ResourceManager.getInstance().getTexture("menu-background")
            }
        }
    }

    override fun onAttached() {
        super.onAttached()

        if (!MusicManager.isPlaying || MusicManager.currentBeatmap == null) {
            if (MusicManager.currentBeatmap == null) {
                MusicManager.currentBeatmap = LibraryManager.getLibrary().random().beatmaps.random()
            }
            MusicManager.load()
            //MusicManager.play()
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        musicButton.text = "${MusicManager.currentBeatmap?.titleText} - ${MusicManager.currentBeatmap?.artistText}"

        val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f * (if (RythimManager.isKiai) 1f else RythimManager.beatSignature.toFloat())
        leftFlash.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), leftFlash.alpha, 0f, 0f, beatLengthSeconds)
        rightFlash.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), rightFlash.alpha, 0f, 0f, beatLengthSeconds)

        super.onManagedUpdate(deltaTimeSec)
    }
}

class MenuButton(icon: Int, title: String) : UIButton() {

    private lateinit var iconComponent: FontAwesomeIcon

    init {
        style += {
            width = 16f.rem
            radius = Radius.XL
            backgroundColor /= 0.6f
            padding = Vec4(6f.srem)
        }

        fillContainer {
            width = Size.Full
            orientation = Orientation.Horizontal
            style = {
                spacing = 4f.srem
            }

            +FontAwesomeIcon(icon).apply {
                iconSize = FontSize.XL
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleCenter = Anchor.Center

                iconComponent = this
            }

            text {
                width = Size.Full
                text = title
                style = {
                    fontSize = FontSize.MD
                }
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f

        if (RythimManager.beatElapsed / 1000f < beatLengthSeconds * 0.75f) {
            val threeQuarts = beatLengthSeconds * 0.75f
            iconComponent.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, threeQuarts), iconComponent.scaleX, 1f, 0f, threeQuarts))
        } else {
            val oneQuart = beatLengthSeconds * 0.25f
            iconComponent.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, oneQuart), iconComponent.scaleX, 0.9f, 0f, oneQuart))
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}


class CarrouselLinearContainer(private val logo: OsuLogo) : UILinearContainer() {

    var shear = 2f.rem

    override fun onDrawChildren(gl: GL10, camera: Camera) {

        val (_, logoTopY) = logo.convertLocalToSceneCoordinates(0f, 0f)
        val (_, logoBottomY) = logo.convertLocalToSceneCoordinates(0f, logo.height)
        val logoCenterY = (logoTopY + logoBottomY) / 2f

        forEach { component ->
            component as UIComponent
            val (_, componentMiddleY) = component.convertLocalToSceneCoordinates(0f, component.height / 2f)

            val percentageDistance = (componentMiddleY - logoCenterY) / (logo.height / 2f)
            val shearX = -shear * abs(percentageDistance)

            component.translationX = shearX
            component.onDraw(gl, camera)
        }

        super.onDrawChildren(gl, camera)
    }

}

