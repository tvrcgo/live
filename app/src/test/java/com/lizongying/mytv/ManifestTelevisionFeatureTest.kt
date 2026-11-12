package com.lizongying.mytv

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
