package com.lizongying.mytv

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

abstract class FlatDialogFragment(
    private val widthFraction: Float = 0.6f,
    private val heightFraction: Float = 0.9f,
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onStart() {
        super.onStart()
        val metrics = requireContext().resources.displayMetrics
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            setLayout(
                (metrics.widthPixels * widthFraction).toInt(),
                if (heightFraction > 0f) {
                    (metrics.heightPixels * heightFraction).toInt()
                } else {
                    WindowManager.LayoutParams.WRAP_CONTENT
                }
            )
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.3f)
        }
    }
}
