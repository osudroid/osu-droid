package com.osudroid.beatmaplisting.mirrors

import TestResourceManager
import com.osudroid.beatmaplisting.BeatmapSetModel
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.RankedStatus

@RunWith(RobolectricTestRunner::class)
class CatboyTest {
    @Test
    fun `Beatmap link fetches once`() {
        val fetch = fake()
        val set = CatboyBeatmapLinkHandler(fetch)(2101856)

        set.assertProperties()
        assertEquals(6.78199, set.beatmaps[0].starRating, 1e-9)
        assertEquals("https://assets.ppy.sh/beatmaps/1004248/covers/card.jpg", set.thumbnail)
        assertEquals(listOf("https://catboy.best/api/v2/b/2101856"), fetch.requested)
    }

    @Test
    fun `Beatmapset link fetches set once`() {
        val fetch = fake()
        val set = CatboyBeatmapsetLinkHandler(fetch)(1004248)

        set.assertProperties()
        assertEquals(listOf("https://catboy.best/api/v2/s/1004248"), fetch.requested)
    }

    @Test
    fun `Beatmapset link prefers unicode fields when present`() {
        val json = fixture("catboy-beatmapset-1004248")
            .put("title_unicode", "テスト")
            .put("artist_unicode", "アーティスト")

        val set = CatboyBeatmapsetLinkHandler(fake(json))(1004248)

        assertEquals("テスト", set.titleUnicode)
        assertEquals("アーティスト", set.artistUnicode)
    }

    @Test
    fun `Beatmapset link falls back when unicode fields are JSON null`() {
        val json = fixture("catboy-beatmapset-1004248")
            .put("title_unicode", JSONObject.NULL)
            .put("artist_unicode", JSONObject.NULL)

        val set = CatboyBeatmapsetLinkHandler(fake(json))(1004248)

        assertEquals("RESISTANCE", set.titleUnicode)
        assertEquals("FELT", set.artistUnicode)
    }

    @Test
    fun `Search response uses same set parsing`() {
        val set = fixture("catboy-beatmapset-1004248")
        val fromLink = CatboyBeatmapsetLinkHandler(fake())(1004248)

        assertEquals(fromLink, CatboySearchResponseModel()(JSONArray().put(set)).single())
    }

    private fun BeatmapSetModel.assertProperties() {
        assertEquals(1004248L, id)
        assertEquals("RESISTANCE", title)
        assertEquals("RESISTANCE", titleUnicode)
        assertEquals("FELT", artist)
        assertEquals("FELT", artistUnicode)
        assertEquals("Kalibe", creator)
        assertEquals(RankedStatus.ranked, status)
        assertEquals(false, hasVideo)
        assertEquals(1, beatmaps.size)

        val beatmap = beatmaps[0]
        assertEquals(2101856L, beatmap.id)
        assertEquals("ENDURANCE", beatmap.version)
        assertEquals(9.4, beatmap.ar, 1e-9)
        assertEquals(4.2, beatmap.cs, 1e-9)
        assertEquals(6.0, beatmap.hp, 1e-9)
        assertEquals(8.7, beatmap.od, 1e-9)
        assertEquals(240.0, beatmap.bpm, 1e-9)
        assertEquals(400L, beatmap.lengthSec)
        assertEquals(1001, beatmap.circleCount)
        assertEquals(849, beatmap.sliderCount)
        assertEquals(4, beatmap.spinnerCount)
    }

    private fun fixture(name: String) =
        JSONObject(TestResourceManager.getTestResource("beatmaplisting/$name.json")!!.readText())

    private fun fake(setFixture: JSONObject = fixture("catboy-beatmapset-1004248")) = FakeFetch(
        mapOf(
            "https://catboy.best/api/v2/b/2101856" to fixture("catboy-beatmap-2101856"),
            "https://catboy.best/api/v2/s/1004248" to setFixture,
        )
    )

    private class FakeFetch(private val responses: Map<String, JSONObject>) : (HttpUrl) -> JSONObject {
        val requested = mutableListOf<String>()

        override fun invoke(url: HttpUrl): JSONObject {
            requested += url.toString()
            return responses[url.toString()] ?: throw AssertionError("Unexpected URL: $url")
        }
    }
}
