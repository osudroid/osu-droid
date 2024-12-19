package com.reco1l.osu

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.reco1l.framework.net.JsonObjectRequest
import com.reco1l.framework.net.WebRequest
import com.reco1l.toolkt.data.writeToFile
import org.anddev.andengine.entity.modifier.AlphaModifier
import org.anddev.andengine.entity.modifier.ScaleModifier
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.io.File

object BannerLoader {

    @JvmStatic
    fun loadBanner(scene: Scene) {

        async {
            val bannerFile = File(Config.getCachePath(), "banner.png")
            if (bannerFile.exists()) {
                bannerFile.delete()
            }

            var bannerUrl = ""

            try {
                JsonObjectRequest("https://osudroid.moe/api/game/banner.php").use { request ->

                    val response = request.execute().json
                    val imageLink = response.getString("ImageLink")

                    bannerUrl = response.getString("Url")

                    if (imageLink.isNotBlank()) {
                        WebRequest(imageLink).use { imageRequest ->
                            bannerFile.createNewFile()
                            imageRequest.execute().response.body!!.byteStream().writeToFile(bannerFile)
                        }
                    }
                }

            } catch (e: Exception) {
                bannerFile.delete()
                Log.e("BannerManager", "Failed to get banner while requesting server.", e)
            }

            if (bannerFile.exists()) {
                ResourceManager.getInstance().loadHighQualityFile("banner", bannerFile)

                val bannerSprite = object : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("banner")) {

                    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                        if (event.isActionDown) {
                            clearEntityModifiers()
                            registerEntityModifier(ScaleModifier(0.1f, scaleX, 0.95f))
                        }

                        if (event.isActionUp || event.isActionCancel || event.isActionOutside) {
                            clearEntityModifiers()
                            registerEntityModifier(ScaleModifier(0.1f, scaleX, 1f))

                            if (event.isActionUp && bannerUrl.isNotBlank()) {
                                GlobalManager.getInstance().mainActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bannerUrl)))
                            }
                        }

                        return true
                    }
                }
                bannerSprite.setScaleCenter(bannerSprite.width / 2f, bannerSprite.height / 2f)
                bannerSprite.setPosition(Config.getRES_WIDTH() - bannerSprite.width - 10f, Config.getRES_HEIGHT() - bannerSprite.height - 10f)
                bannerSprite.alpha = 0f
                bannerSprite.registerEntityModifier(AlphaModifier(0.2f, 0f, 1f))

                scene.attachChild(bannerSprite)
                scene.registerTouchArea(bannerSprite)
            }

        }
    }

}