package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.ChannelItem


class PlayerFragment : Fragment() {

    private var _binding: PlayerBinding? = null
    private var playerView: PlayerView? = null
    private var channelItem: ChannelItem? = null
    private val aspectRatio = 16f / 9f

    private var pendingUrl: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private var retryUrl: String? = null
    private var playFailed = false
    private var lastNetworkRetryAt = 0L

    private val retry = Runnable {
        channelItem?.let { play(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)
        playerView = _binding!!.playerView

        playerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            @OptIn(UnstableApi::class)
            override fun onGlobalLayout() {
                playerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                playerView!!.player = activity?.let {
                    ExoPlayer.Builder(it)
                        .setLoadControl(loadControl())
                        .setMediaSourceFactory(mediaSourceFactory())
                        .build()
                }
                playerView!!.player?.playWhenReady = true
                // 禁用字幕轨：4K 频道的 DVB 图文（HLG/4K 角标）随流自动渲染，只干扰观看
                playerView!!.player?.let { player ->
                    player.trackSelectionParameters =
                        player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                }
                playerView!!.player?.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        val ratio = playerView?.measuredWidth?.div(playerView?.measuredHeight!!)
                        if (ratio != null) {
                            val layoutParams = playerView?.layoutParams
                            if (ratio < aspectRatio) {
                                layoutParams?.height =
                                    (playerView?.measuredWidth?.div(aspectRatio))?.toInt()
                                playerView?.layoutParams = layoutParams
                            } else if (ratio > aspectRatio) {
                                layoutParams?.width =
                                    (playerView?.measuredHeight?.times(aspectRatio))?.toInt()
                                playerView?.layoutParams = layoutParams
                            }
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val info = playbackInfo(tracks)
                        if (info.isNotEmpty()) {
                            (activity as? MainActivity)?.showPlaybackInfo(info)
                        }
                    }

                    override fun onRenderedFirstFrame() {
                        retryCount = 0
                        playFailed = false
                        hideBlackScreen()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)

                        Log.e(TAG, "PlaybackException $error")
                        scheduleRetry(errorText(error))
                    }
                })
                pendingUrl?.let {
                    pendingUrl = null
                    playerView?.player?.setMediaItem(MediaItem.fromUri(it))
                    playerView?.player?.prepare()
                }
            }
        })
        (activity as MainActivity).fragmentReady("PlayerFragment")
        return _binding!!.root
    }

    @OptIn(UnstableApi::class)
    fun play(channelItem: ChannelItem) {
        this.channelItem = channelItem
        val url = channelItem.currentUrl()
        handler.removeCallbacks(retry)
        if (url != retryUrl) {
            retryUrl = url
            retryCount = 0
        }
        playFailed = false
        showBlackScreen()
        hidePlayError()
        val player = playerView?.player
        if (player != null) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        } else if (playerView != null) {
            pendingUrl = url
        }
    }

    fun stop() {
        playerView?.player?.stop()
    }

    fun onNetworkAvailable() {
        if (!playFailed) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastNetworkRetryAt < NETWORK_RETRY_THROTTLE_MS) {
            return
        }
        lastNetworkRetryAt = now
        retryCount = 0
        channelItem?.let { play(it) }
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
        if (playerView != null && playerView!!.player?.isPlaying == false) {
            Log.i(TAG, "replay")
            playerView!!.player?.prepare()
            playerView!!.player?.play()
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(retry)
        if (playerView != null && playerView!!.player?.isPlaying == true) {
            playerView!!.player?.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (playerView != null) {
            playerView!!.player?.release()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scheduleRetry(message: String) {
        showBlackScreen()
        val delay = RetryPolicy.nextDelay(retryCount)
        if (delay == null) {
            showPlayError(message)
            return
        }
        retryCount++
        handler.postDelayed(retry, delay)
    }

    private fun errorText(error: PlaybackException): String {
        return getString(PlaybackErrors.messageRes(error.errorCode))
    }

    @OptIn(UnstableApi::class)
    private fun playbackInfo(tracks: Tracks): String {
        val parts = mutableListOf<String>()
        val video = trackFormat(tracks, C.TRACK_TYPE_VIDEO)
        if (video != null) {
            heightLabel(video.height)?.let { parts.add(it) }
            videoCodecName(video.sampleMimeType)?.let { parts.add(it) }
        }
        val audio = trackFormat(tracks, C.TRACK_TYPE_AUDIO)
        if (audio != null) {
            audioCodecName(audio.sampleMimeType)?.let { parts.add(it) }
        }
        return parts.joinToString(" · ")
    }

    private fun heightLabel(height: Int): String? {
        return when {
            height <= 0 -> null
            height >= 2160 -> "2160p"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 576 -> "576p"
            height >= 480 -> "480p"
            else -> "${height}p"
        }
    }

    private fun trackFormat(tracks: Tracks, type: Int): Format? {
        val group = tracks.groups.firstOrNull { it.type == type } ?: return null
        for (index in 0 until group.length) {
            if (group.isTrackSupported(index)) {
                return group.getTrackFormat(index)
            }
        }
        return null
    }

    @OptIn(UnstableApi::class)
    private fun videoCodecName(mimeType: String?): String? {
        return when (mimeType) {
            MimeTypes.VIDEO_H264 -> "H.264"
            MimeTypes.VIDEO_H265 -> "HEVC"
            MimeTypes.VIDEO_AV1 -> "AV1"
            MimeTypes.VIDEO_VP9 -> "VP9"
            MimeTypes.VIDEO_MPEG2 -> "MPEG-2"
            else -> null
        }
    }

    private fun audioCodecName(mimeType: String?): String? {
        return when (mimeType) {
            MimeTypes.AUDIO_AAC -> "AAC"
            MimeTypes.AUDIO_AC3 -> "AC3"
            MimeTypes.AUDIO_E_AC3 -> "EAC3"
            MimeTypes.AUDIO_MPEG_L1 -> "MP1"
            MimeTypes.AUDIO_MPEG_L2 -> "MP2"
            MimeTypes.AUDIO_MPEG -> "MP3"
            else -> null
        }
    }

    @OptIn(UnstableApi::class)
    private fun mediaSourceFactory(): DefaultMediaSourceFactory {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Utils.USER_AGENT)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
        return DefaultMediaSourceFactory(DefaultDataSource.Factory(requireContext(), http))
    }

    private fun showBlackScreen() {
        _binding?.blackScreen?.visibility = View.VISIBLE
    }

    private fun hideBlackScreen() {
        _binding?.blackScreen?.visibility = View.GONE
    }

    private fun showPlayError(message: String) {
        showBlackScreen()
        playFailed = true
        _binding?.playError?.text = message
        _binding?.playError?.visibility = View.VISIBLE
    }

    private fun hidePlayError() {
        _binding?.playError?.visibility = View.GONE
    }

    @OptIn(UnstableApi::class)
    private fun loadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    companion object {
        private const val TAG = "PlayerFragment"

        private const val MIN_BUFFER_MS = 4_000
        private const val MAX_BUFFER_MS = 20_000
        private const val BUFFER_FOR_PLAYBACK_MS = 2_000
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 4_000
        private const val TARGET_BUFFER_BYTES = 32 * 1024 * 1024

        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val NETWORK_RETRY_THROTTLE_MS = 3_000
    }
}
