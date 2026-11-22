package com.chacha.live

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 回归：换台时播放器会盖一层纯黑遮罩，面板底色贴黑就看不出信息条区域。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InfoPanelVisibilityTest {

    @Test
    fun infoPanelStaysVisibleOnBlackScreen() {
        val themed: Context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_Live
        )
        val root = LayoutInflater.from(themed).inflate(R.layout.info, null)
        val background = requireNotNull(root.background as? GradientDrawable) {
            "信息条面板底色应为 shape drawable"
        }
        val panel = requireNotNull(background.color?.defaultColor) {
            "信息条面板底色缺少 solid 颜色"
        }

        val alpha = Color.alpha(panel)
        val composited = maxOf(
            Color.red(panel) * alpha / 255,
            Color.green(panel) * alpha / 255,
            Color.blue(panel) * alpha / 255
        )

        assertTrue(
            "面板叠在黑色遮罩上的亮度 $composited 过低（面板=$(Integer.toHexString(panel))），信息条区域将不可辨",
            composited >= MIN_COMPOSITED_LEVEL
        )
    }

    private companion object {
        const val MIN_COMPOSITED_LEVEL = 0x20
    }
}
