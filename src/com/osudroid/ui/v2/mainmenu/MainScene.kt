package com.osudroid.ui.v2.mainmenu

import android.content.*
import android.net.Uri
import android.util.*
import androidx.core.content.*
import com.edlplan.framework.easing.*
import com.osudroid.*
import com.osudroid.beatmaplisting.*
import com.osudroid.multiplayer.*
import com.osudroid.resources.R
import com.osudroid.ui.v1.*
import com.osudroid.ui.v2.multi.*
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*


object MainScene : UIScene() {

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: UITextButton
    private lateinit var leftFlash: UIGradientBox
    private lateinit var rightFlash: UIGradientBox
    private lateinit var background: UISprite
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: UIContainer
    private lateinit var menuGradientBox: UIGradientBox

    private var isMenuExpanded = false

    private var lastMusicChange = System.currentTimeMillis()


    init {

        container {
            width = Size.Full
            height = Size.Full

            background = sprite {
                width = Size.Full
                height = Size.Full
                scaleType = ScaleType.Crop
                textureRegion = ResourceManager.getInstance().getTexture("menu-background")
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
                    colorEnd = (it.accentColor * 0.1f).copy(alpha = 0.5f)
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
                    playClickEffect()

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

                    isMenuExpanded = !isMenuExpanded
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
                            childScene = SettingsMenu()
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
                    if (alpha > 0f)
                        musicPlayer.show()
                }
            }
        }

        clickableContainer {
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            style = {
                paddingLeft = UIEngine.current.safeArea.x
                paddingBottom = 4f.srem
            }

            text {
                text = "osu!droid ${BuildConfig.VERSION_NAME}"
                style = {
                    backgroundColor = Colors.Black.copy(alpha = 0.5f)
                    padding = Vec4(2f.srem, 1f.srem)
                    radius = Radius.MD
                    fontSize = FontSize.SM
                }
            }

            onActionUp = {
                BuildInformationDialog().show()
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

            if (textureRegion != null && !Config.isSafeBeatmapBg()) {
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

        logo.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), logo.scaleX, if (isMenuExpanded) 1f else 1.3f, 0f, 0.1f))

        val mightShowMusicButton = isMenuExpanded || System.currentTimeMillis() - lastMusicChange < 3000

        musicButton.text = "${MusicManager.currentBeatmap?.titleText} - ${MusicManager.currentBeatmap?.artistText}"
        musicButton.translationX = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButton.translationX, if (mightShowMusicButton) 0f else 8f.srem, 0f, 0.05f)
        musicButton.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButton.alpha, if (mightShowMusicButton) 1f else 0f, 0f, 0.05f)

        playerButton.translationX = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButton.translationX, if (isMenuExpanded) 0f else (-8f).srem, 0f, 0.05f)
        playerButton.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButton.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.05f)

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
            backgroundColor = backgroundColor.copy(alpha = 0.6f)
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


class BuildInformationDialog : UIDialog<UIScrollableContainer>(UIScrollableContainer().apply {
    width = Size.Full
    clipToBounds = true
    scrollAxes = Axes.Y
    style = {
        maxHeight = 0.7f.vh
    }
}) {
    init {
        title = "About"

        innerContent.apply {

            linearContainer {
                width = Size.Full
                orientation = Orientation.Vertical
                style = {
                    spacing = 4f.srem
                    paddingTop = 4f.srem
                    paddingBottom = 4f.srem
                }

                text {
                    text = "osu!droid"
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    style = {
                        fontSize = FontSize.XL
                        fontFamily = Fonts.TorusBold
                    }
                }

                text {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    text = "Version: ${BuildConfig.VERSION_NAME}"
                    style = { fontSize = FontSize.MD }
                }

                text {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    alignment = Anchor.TopCenter
                    text = "Made by the osu!droid team\nosu! is © peppy 2007-2026"
                }


                fun goToLink(link: String) {
                    hide()
                    val context = GlobalManager.getInstance().mainActivity
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(link)
                    context.startActivity(intent)
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleCenter = Anchor.Center
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = FontAwesomeIcon(Icon.ArrowUpRightFromSquare)
                    text = "Visit official osu! website"
                    onActionUp = {
                        goToLink("https://osu.ppy.sh")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleCenter = Anchor.Center
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = FontAwesomeIcon(Icon.ArrowUpRightFromSquare)
                    text = "Visit official osu!droid website"
                    onActionUp = {
                        goToLink("https://osudroid.moe")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleCenter = Anchor.Center
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = FontAwesomeIcon(Icon.ArrowUpRightFromSquare)
                    text = "Join the official Discord server"
                    onActionUp = {
                        goToLink("https://discord.gg/nyD92cE")
                    }
                }

            }
        }

        addButton {
            text = "Changelog"
            onActionUp = {
                hide()

                try {
                    val context = GlobalManager.getInstance().mainActivity
                    val changelogFile = File(context.cacheDir, "changelog.html")

                    context.assets.open("app/changelog.html").use { inputStream ->
                        changelogFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while ((inputStream.read(buffer).also { length = it }) > 0) {
                                outputStream.write(buffer, 0, length)
                            }
                        }
                    }

                    val changelogUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", changelogFile)

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(changelogUri, "text/html")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainScene", "Failed to load changelog", e)
                }
            }
        }

        addButton {
            text = "Close"
            onActionUp = {
                hide()
            }
        }
    }
}
