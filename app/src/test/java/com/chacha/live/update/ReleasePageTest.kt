package com.chacha.live.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleasePageTest {

    @Test
    fun `tag keeps v prefix from redirect location`() {
        assertEquals(
            "v1.2611.2",
            ReleasePage.tagFromRedirect("https://github.com/tvrcgo/live/releases/tag/v1.2611.2")
        )
    }

    @Test
    fun `location without tag path has no tag`() {
        assertNull(ReleasePage.tagFromRedirect("https://github.com/tvrcgo/live/releases"))
    }

    @Test
    fun `live asset keeps repo prefix`() {
        assertEquals(
            "https://github.com/tvrcgo/live/releases/download/v1.2611.2/live-1.2611.2.apk",
            ReleasePage.apkUrl(LIVE_ASSET_HTML)
        )
    }

    @Test
    fun `legacy my-tv asset still resolved`() {
        assertEquals(
            "https://github.com/tvrcgo/live/releases/download/v1.2611.1/my-tv-1.2611.1.apk",
            ReleasePage.apkUrl(LEGACY_ASSET_HTML)
        )
    }

    @Test
    fun `live asset preferred when both names present`() {
        assertEquals(
            "https://github.com/tvrcgo/live/releases/download/v1.2611.2/live-1.2611.2.apk",
            ReleasePage.apkUrl(LEGACY_ASSET_HTML + LIVE_ASSET_HTML)
        )
    }

    @Test
    fun `source archives without apk has no url`() {
        assertNull(ReleasePage.apkUrl(SOURCE_HTML))
    }

    private companion object {
        const val LIVE_ASSET_HTML = """
            <a href="/tvrcgo/live/releases/download/v1.2611.2/live-1.2611.2.apk" rel="nofollow" data-turbo="false" class="wb-break-all">
              <span class="text-bold">live-1.2611.2.apk</span>
            </a>
        """

        const val LEGACY_ASSET_HTML = """
            <a href="/tvrcgo/live/releases/download/v1.2611.1/my-tv-1.2611.1.apk" rel="nofollow" data-turbo="false" class="wb-break-all">
              <span class="text-bold">my-tv-1.2611.1.apk</span>
            </a>
        """

        const val SOURCE_HTML = """
            <a href="/tvrcgo/live/archive/refs/tags/v1.2611.2.zip" rel="nofollow">Source code (zip)</a>
            <a href="/tvrcgo/live/archive/refs/tags/v1.2611.2.tar.gz" rel="nofollow">Source code (tar.gz)</a>
        """
    }
}
