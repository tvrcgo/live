package com.lizongying.mytv

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 回归：appcompat 注入轨道 tint 在进程首个 SwitchCompat 上会失效，
 * 导致轨道不 stateful、state_checked 传不进去、颜色停在关态。
 * 布局里四个开关都必须显式带 trackTint。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SwitchTrackTintTest {

    private fun inflateSetting(): View {
        val themed: Context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_MyTV
        )
        return LayoutInflater.from(themed).inflate(R.layout.setting, null)
    }

    @Test
    fun allSettingSwitchesDeclareTrackTint() {
        val root = inflateSetting()
        val ids = listOf(
            R.id.switch_channel_reversal,
            R.id.switch_channel_num,
            R.id.switch_time,
            R.id.switch_boot_startup,
        )
        ids.forEach { id ->
            val sw = root.findViewById<SwitchCompat>(id)
            assertNotNull("$id 缺少显式 trackTint", sw.trackTintList)
        }
    }

    @Test
    fun firstSwitchTrackReceivesCheckedState() {
        val root = inflateSetting()
        val sw = root.findViewById<SwitchCompat>(R.id.switch_channel_reversal)
        val track = requireNotNull(sw.trackDrawable)

        assertTrue("轨道必须 stateful，否则 state_checked 传不进 drawable", track.isStateful)

        sw.isChecked = true
        assertTrue(
            "开态应把 state_checked 传给轨道，实际状态=${track.state.contentToString()}",
            track.state.contains(android.R.attr.state_checked)
        )
        assertEquals(
            expectedCheckedTrackColor(),
            sw.trackTintList!!.getColorForState(track.state, 0).toLong()
        )
    }

    // 轨道开态 = 主题色 + switch_track.xml 的 alpha 0.3，色号跟随 R.color.focus
    private fun expectedCheckedTrackColor(): Long {
        val accent = ContextCompat.getColor(
            ApplicationProvider.getApplicationContext(),
            R.color.focus
        )
        return ((TRACK_CHECKED_ALPHA shl 24) or (accent and 0x00FFFFFF)).toLong()
    }

    private companion object {
        const val TRACK_CHECKED_ALPHA = 0x4D
    }
}
