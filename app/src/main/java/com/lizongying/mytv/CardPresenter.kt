package com.lizongying.mytv

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.lizongying.mytv.models.ChannelItem

class CardPresenter(
    private val context: Context,
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val channelItem = item as ChannelItem
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = channelItem.getTV().title
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)

        cardView.mainImageView?.let {
            Glide.with(viewHolder.view.context)
                .load(channelItem.getTV().logo)
                .centerInside()
                .into(it)
        }

        cardView.setBackgroundColor(context.getColor(R.color.card_background))
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)

//        cardView.setOnFocusChangeListener { v, hasFocus ->
//            run {
//                if (hasFocus) {
//                    if (v != null) {
//                        (v as ImageCardView).setInfoAreaBackgroundColor(context.resources.getColor(R.color.focus))
//                    }
//                } else {
//                    if (v != null) {
//                        (v as ImageCardView).setInfoAreaBackgroundColor(context.resources.getColor(R.color.ic_launcher_background))
//                    }
//                }
//            }
//        }

        cardView.contentText = channelItem.getTV().sourceName
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }

    companion object {
        private const val TAG = "CardPresenter"
        private const val CARD_WIDTH = 300
        private const val CARD_HEIGHT = 101
    }
}