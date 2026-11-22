package com.chacha.live

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chacha.live.databinding.InfoBinding
import com.chacha.live.models.ChannelItem

class InfoFragment : Fragment() {
    private var _binding: InfoBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 3000

    private var baseDesc = ""
    private var playbackInfo = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBinding.inflate(inflater, container, false)
        _binding!!.root.visibility = View.GONE
        (activity as MainActivity).fragmentReady("InfoFragment")
        return binding.root
    }

    fun show(channelItem: ChannelItem) {
        val tv = channelItem.getTV()
        binding.textView.text = tv.title

        baseDesc = listOf(tv.channel, tv.sourceName)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
        playbackInfo = ""
        render()

        handler.removeCallbacks(removeRunnable)
        view?.visibility = View.VISIBLE
        handler.postDelayed(removeRunnable, delay)
    }

    fun updatePlaybackInfo(text: String) {
        playbackInfo = text
        if (_binding == null || view?.visibility != View.VISIBLE) {
            return
        }
        render()
        handler.removeCallbacks(removeRunnable)
        handler.postDelayed(removeRunnable, delay)
    }

    private fun render() {
        binding.infoDesc.text = listOf(baseDesc, playbackInfo)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(removeRunnable, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(removeRunnable)
    }

    private val removeRunnable = Runnable {
        view?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
