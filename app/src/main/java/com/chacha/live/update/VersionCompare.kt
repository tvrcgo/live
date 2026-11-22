package com.chacha.live.update

object VersionCompare {

    // >0 表示 latest 更新；tag 允许带 v 前缀，非数字段按 0 处理
    fun compare(current: String, latest: String): Int {
        val a = parse(current)
        val b = parse(latest)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) {
                return y - x
            }
        }
        return 0
    }

    private fun parse(version: String): List<Int> =
        version.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
}
