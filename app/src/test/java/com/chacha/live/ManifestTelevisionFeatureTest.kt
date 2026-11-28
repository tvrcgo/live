package com.chacha.live

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * 回归：TCL 灵控桌面按 android.hardware.type.television 判定 TV 应用，
 * 缺这条声明时应用只拿到方形小图标，不显示 16:9 大 banner。
 */
class ManifestTelevisionFeatureTest {

    private fun featureNamed(name: String): Element? {
        val manifest = File("src/main/AndroidManifest.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val nodes = doc.getElementsByTagName("uses-feature")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .firstOrNull { it.getAttribute("android:name") == name }
    }

    @Test
    fun declaresTelevisionFeatureAsRequired() {
        val television = featureNamed("android.hardware.type.television")

        assertNotNull("清单缺少 android.hardware.type.television，桌面会按手机应用渲染方形图标", television)
        assertEquals("必须是 required=true，否则不被视为 TV 应用", "true", television!!.getAttribute("android:required"))
    }

    @Test
    fun declaresLeanbackFeatureAsRequired() {
        val leanback = featureNamed("android.software.leanback")

        assertNotNull("清单缺少 android.software.leanback，部分桌面不给 16:9 大 banner", leanback)
        assertEquals("必须是 required=true，对齐主流 TV 应用声明", "true", leanback!!.getAttribute("android:required"))
    }

    /**
     * 回归：TCL 灵控桌面 7.7+ 读取 launcher Activity 的 beidouui_icon meta-data，
     * 从应用 assets 加载指定图片作为全幅大图卡（逆向 MlAppsApiImpl.loadAppInfo 实证）。
     */
    @Test
    fun declaresBeidouIconMetaDataWithAsset() {
        val manifest = File("src/main/AndroidManifest.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifest)
        val activities = doc.getElementsByTagName("activity")
        var meta: Element? = null
        var mainActivity: Element? = null
        for (i in 0 until activities.length) {
            val act = activities.item(i) as Element
            if (act.getAttribute("android:name") == ".MainActivity") {
                mainActivity = act
                val metas = act.getElementsByTagName("meta-data")
                for (j in 0 until metas.length) {
                    val m = metas.item(j) as Element
                    if (m.getAttribute("android:name") == "beidouui_icon") meta = m
                }
            }
        }

        assertNotNull("MainActivity 缺少 beidouui_icon meta-data，灵控桌面 7.7+ 不显示大图卡", meta)
        val value = meta!!.getAttribute("android:value")
        assertNotNull("beidouui_icon meta-data 缺少 android:value", value)
        assertTrue("beidouui_icon 值不能为空", value.isNotEmpty())

        val asset = File("src/main/assets/$value")
        assertTrue("assets/$value 不存在，桌面加载大图会失败并回退小图标", asset.isFile)
    }
}
