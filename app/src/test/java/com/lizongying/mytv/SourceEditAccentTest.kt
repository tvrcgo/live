package com.lizongying.mytv

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 回归：直播源编辑弹窗的强调色必须跟随主题 focus，不得另立一套色值。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SourceEditAccentTest {

    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.Theme_MyTV
    )

    @Test
    fun focusedUnderlineUsesThemeAccent() {
        val root = LayoutInflater.from(context).inflate(R.layout.source_edit, null)
        val field = root.findViewById<EditText>(R.id.edit_url)
        val background = requireNotNull(field.background as? StateListDrawable) {
            "输入框背景应为 stateful 的 shape selector"
        }

        background.setState(intArrayOf(android.R.attr.state_focused))
        val underline = (background.current as LayerDrawable).getDrawable(1) as GradientDrawable
        val actual = requireNotNull(underline.color?.defaultColor) { "下划线缺少 solid 颜色" }

        assertEquals("聚焦下划线应取主题强调色", accentColor(), actual.toLong())
    }

    @Test
    fun okButtonTintUsesThemeAccent() {
        val root = LayoutInflater.from(context).inflate(R.layout.source_edit, null)
        val ok = root.findViewById<Button>(R.id.edit_ok)
        val tint = requireNotNull(ok.backgroundTintList) { "确定按钮缺少 backgroundTint" }

        assertEquals("确定按钮底色应取主题强调色", accentColor(), tint.defaultColor.toLong())
    }

    private fun accentColor(): Long = ContextCompat.getColor(context, R.color.focus).toLong()
}
