package com.reco1l.osu

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.net.JsonArrayRequest
import com.reco1l.framework.net.WebRequest
import com.reco1l.toolkt.data.writeToFile
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.io.File

object BannerLoader {

    @JvmStatic
    fun loadBannerSprite(): BannerSprite? {

        val banners = mutableListOf<Banner>()

        try {

            val bannersFolder = File(Config.getCachePath(), "banners").apply {
                delete()
                mkdirs()
            }

            JsonArrayRequest(OnlineManager.endpoint + "game/banner.php").use { request ->
                request.execute()

                for (i in 0 until request.json.length()) {

                    try {
                        val banner = request.json.getJSONObject(i)

                        val url = banner.getString("Url")
                        val imageLink = banner.getString("ImageLink")

                        WebRequest(imageLink).use { imageRequest ->

                            val bannerFile = File(bannersFolder, "${i}.png")
                            bannerFile.createNewFile()

                            imageRequest.execute()
                            imageRequest.responseBody.byteStream().writeToFile(bannerFile)

                            ResourceManager.getInstance().loadHighQualityFile("banner@${i}", bannerFile)
                        }

                        banners.add(Banner(url, ResourceManager.getInstance().getTexture("banner@${i}")))

                    } catch (e: Exception) {
                        Log.e("BannerManager", "Failed to get banner at index ${i}.", e)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("BannerManager", "Failed to get banners from server.", e)
        }

        if (banners.isEmpty()) {
            return null
        }

        return BannerSprite(banners)
    }

}

data class Banner(
    val url: String,
    val image: TextureRegion,
)

class BannerSprite(private val banners: List<Banner>) : ExtendedSprite() {


    private var currentBannerIndex = 0

    private var elapsedTimeSinceLastChange = 0f


    init {
        textureRegion = banners[currentBannerIndex].image
        alpha = 0f
        fadeIn(0.75f)
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (elapsedTimeSinceLastChange > BANNER_DURATION) {
            elapsedTimeSinceLastChange %= BANNER_DURATION

            currentBannerIndex++
            currentBannerIndex %= banners.size

            val banner = banners[currentBannerIndex]

            fadeOut(0.5f).then {
                textureRegion = banner.image
                fadeIn(0.5f)
            }
        }
        elapsedTimeSinceLastChange += pSecondsElapsed

        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        elapsedTimeSinceLastChange = 0f

        if (event.isActionDown) {
            clearEntityModifiers()
            scaleTo(0.95f, 0.1f)
        }

        if (event.isActionUp || event.isActionCancel || event.isActionOutside) {
            clearEntityModifiers()
            scaleTo(1f, 0.1f)

            val banner = banners[currentBannerIndex]

            if (event.isActionUp && banner.url.isNotBlank()) {
                val uri = Uri.parse(banner.url)
                val intent = Intent(ACTION_VIEW, uri)
                GlobalManager.getInstance().mainActivity.startActivity(intent)
            }
        }

        return true
    }


    companion object {

        private const val BANNER_DURATION = 10f // Seconds

    }

}