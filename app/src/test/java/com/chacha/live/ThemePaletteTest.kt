package com.chacha.live

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * 主题色单点维护的守护：强调色的浅色派生态必须与它同色相、且更亮。
 * 改了 R.color.focus 却漏改浅色 token 时，这里会红。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThemePaletteTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val paleAccents = listOf(
        "channel_badge" to R.color.channel_badge,
        "channel_sel_sub" to R.color.channel_sel_sub,
    )

    @Test
    fun paleAccentsShareAccentHue() {
        val accentHue = hueOf(R.color.focus)
        paleAccents.forEach { (name, id) ->
            val hue = hueOf(id)
            assertTrue(
                "$name 色相 ${hue.toInt()}° 偏离主题色 focus 的 ${accentHue.toInt()}° 超过 $HUE_TOLERANCE°，" +
                    "改主题色后需同步调整该派生态",
                hueGap(hue, accentHue) <= HUE_TOLERANCE
            )
        }
    }

    @Test
    fun paleAccentsGetLighterInOrder() {
        val accent = lightnessOf(R.color.focus)
        val badge = lightnessOf(R.color.channel_badge)
        val selSub = lightnessOf(R.color.channel_sel_sub)

        assertTrue("channel_badge 应比 focus 更亮：focus=$accent badge=$badge", accent < badge)
        assertTrue("channel_sel_sub 应比 channel_badge 更亮：badge=$badge selSub=$selSub", badge < selSub)
    }

    private fun hueOf(id: Int): Float {
        val hsv = FloatArray(3)
        val color = ContextCompat.getColor(context, id)
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv)
        return hsv[0]
    }

    private fun lightnessOf(id: Int): Float {
        val color = ContextCompat.getColor(context, id)
        val max = maxOf(Color.red(color), Color.green(color), Color.blue(color))
        val min = minOf(Color.red(color), Color.green(color), Color.blue(color))
        return (max + min) / 2f / 255f
    }

    private fun hueGap(a: Float, b: Float): Float {
        val diff = abs(a - b)
        return minOf(diff, 360f - diff)
    }

    private companion object {
        const val HUE_TOLERANCE = 20f
    }
}
