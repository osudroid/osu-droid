package com.osudroid.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapLink.Beatmap
import com.osudroid.beatmaplisting.BeatmapLink.BeatmapSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeatmapLinkTest {
    @Test
    @Suppress("KotlinMisorderedAssertEqualsArguments")
    fun `Parse valid beatmap and beatmap set links`() {
        assertEquals(Beatmap(2101856), BeatmapLink.parse("osu://b/2101856"))
        assertEquals(BeatmapSet(1004248), BeatmapLink.parse("osu://s/1004248"))
        assertEquals(BeatmapSet(1004248), BeatmapLink.parse("osu://dl/1004248"))
    }

    @Test
    @Suppress("KotlinMisorderedAssertEqualsArguments")
    fun `Parse links with ignored suffixes`() {
        assertEquals(Beatmap(2101856), BeatmapLink.parse("osu://b/2101856?mode=0"))
        assertEquals(Beatmap(2101856), BeatmapLink.parse("osu://b/2101856/"))
        assertEquals(Beatmap(2101856), BeatmapLink.parse("osu://b/2101856#osu"))
    }

    @Test
    @Suppress("KotlinMisorderedAssertEqualsArguments")
    fun `Parse links case-insensitively`() {
        assertEquals(Beatmap(2101856), BeatmapLink.parse("OSU://B/2101856"))
    }

    @Test
    fun `Reject invalid links`() {
        val invalidLinks = listOf(
            "osu://b/",
            "osu://b",
            "osu://b/abc",
            "osu://b/12abc",
            "osu://b/-5",
            "osu://b/0",
            "osu://b/99999999999999999999",
            "osu://u/123",
            "osu://chan/123",
            "https://osu.ppy.sh/b/2101856",
            "https://odmp/123",
            "osu://\u017F/123",
            ""
        )

        for (link in invalidLinks) {
            assertNull("Expected null for link: $link", BeatmapLink.parse(link))
        }
    }
}
