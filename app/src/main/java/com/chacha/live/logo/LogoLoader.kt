package com.chacha.live.logo

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * 按顺序尝试候选地址，前一个取不到就换下一个；全部失败则留空。
 * 失败过的地址记在会话内，避免滚动回来重复请求必然 404 的图。
 */
object LogoLoader {

    private val failed = HashSet<String>()

    fun load(view: ImageView, urls: List<String>, width: Int = 0, height: Int = 0) {
        view.setImageDrawable(null)
        val queue = urls.filter { it.isNotEmpty() && !failed.contains(it) }
        if (queue.isEmpty()) {
            return
        }
        request(view, queue, 0, width, height)
    }

    private fun request(view: ImageView, queue: List<String>, index: Int, width: Int, height: Int) {
        val builder = Glide.with(view).load(queue[index])
        if (width > 0 && height > 0) {
            builder.override(width, height)
        }
        builder.centerInside()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    failed.add(queue[index])
                    if (index + 1 < queue.size) {
                        // Glide 禁止在回调内对同一 view 发起新请求（clear 防死锁断言），
                        // post 到回调返回之后再加载备选
                        view.post { request(view, queue, index + 1, width, height) }
                        return true
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean = false
            })
            .into(view)
    }
}
