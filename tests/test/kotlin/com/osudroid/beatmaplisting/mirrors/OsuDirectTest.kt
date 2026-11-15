package com.osudroid.beatmaplisting.mirrors

import TestResourceManager
import com.osudroid.beatmaplisting.BeatmapSetModel
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.RankedStatus

@RunWith(RobolectricTestRunner::class)
class OsuDirectTest {
    @Test
    fun `Beatmap link fetches beatmap then set`() {
        val fetch = fake()
        val set = OsuDirectBeatmapLinkHandler(fetch)(2101856)

        set.assertProperties()

        assertEquals(7.03159, set.beatmaps[0].starRating, 1e-9)
        assertTrue(set.thumbnail!!.startsWith("https://assets.ppy.sh/beatmaps/1004248/covers/card.jpg"))
        assertEquals(
            listOf("https://osu.direct/api/v2/b/2101856", "https://osu.direct/api/v2/s/1004248"),
            fetch.requested
        )
    }

    @Test
    fun `Beatmapset link fetches set once`() {
        val fetch = fake()
        val set = OsuDirectBeatmapsetLinkHandler(fetch)(1004248)

        set.assertProperties()
        assertEquals(listOf("https://osu.direct/api/v2/s/1004248"), fetch.requested)
    }

    @Test
    fun `Search response uses same set parsing`() {
        val set = fixture("osudirect-beatmapset-1004248")
        val fromLink = OsuDirectBeatmapsetLinkHandler(fake())(1004248)

        assertEquals(fromLink, OsuDirectSearchResponseModel()(JSONArray().put(set)).single())
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

    private class FakeFetch(private val responses: Map<String, JSONObject>) : (HttpUrl) -> JSONObject {
        val requested = mutableListOf<String>()

        override fun invoke(url: HttpUrl): JSONObject {
            requested += url.toString()
            return responses[url.toString()] ?: throw AssertionError("Unexpected URL: $url")
        }
    }

    private fun fake() = FakeFetch(
        mapOf(
            "https://osu.direct/api/v2/b/2101856" to fixture("osudirect-beatmap-2101856"),
            "https://osu.direct/api/v2/s/1004248" to fixture("osudirect-beatmapset-1004248"),
        )
    )
}
