package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.exoplayer2.DefaultLoadControl as LegacyLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player as LegacyPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.TVViewModel


class PlayerFragment : Fragment(), SurfaceHolder.Callback {

    private var _binding: PlayerBinding? = null
    private var playerView: PlayerView? = null
    private var tvViewModel: TVViewModel? = null
    private val aspectRatio = 16f / 9f


    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var exoPlayer: SimpleExoPlayer? = null
    private var pendingUrl: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private var retryUrl: String? = null

    private val retry = Runnable {
        tvViewModel?.let { play(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)

        if (Utils.isTmallDevice()) {
            _binding!!.playerView.visibility = View.GONE
            surfaceView = _binding!!.surfaceView
            surfaceHolder = surfaceView.holder
            surfaceHolder.addCallback(this)
        } else {
            _binding!!.surfaceView.visibility = View.GONE
            playerView = _binding!!.playerView
        }

        playerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            @OptIn(UnstableApi::class)
            override fun onGlobalLayout() {
                playerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                playerView!!.player = activity?.let {
                    ExoPlayer.Builder(it)
                        .setLoadControl(loadControl())
                        .build()
                }
                playerView!!.player?.playWhenReady = true
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

                    override fun onRenderedFirstFrame() {
                        retryCount = 0
                        hideBlackScreen()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)

                        Log.e(TAG, "PlaybackException $error")
                        scheduleRetry()
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
    fun play(tvViewModel: TVViewModel) {
        this.tvViewModel = tvViewModel
        val url = tvViewModel.getVideoUrlCurrent()
        handler.removeCallbacks(retry)
        if (url != retryUrl) {
            retryUrl = url
            retryCount = 0
        }
        showBlackScreen()
        hidePlayError()
        val player = playerView?.player
        if (player != null) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        } else if (playerView != null) {
            pendingUrl = url
        }
        exoPlayer?.run {
            setMediaItem(com.google.android.exoplayer2.MediaItem.fromUri(url))
            prepare()
        }
    }

    fun stop() {
        playerView?.player?.stop()
        exoPlayer?.stop()
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
        if (playerView != null && playerView!!.player?.isPlaying == false) {
            Log.i(TAG, "replay")
            playerView!!.player?.prepare()
            playerView!!.player?.play()
        }
        if (exoPlayer?.isPlaying == false) {
            Log.i(TAG, "replay")
            exoPlayer?.prepare()
            exoPlayer?.play()
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
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (playerView != null) {
            playerView!!.player?.release()
        }
        exoPlayer?.release()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scheduleRetry() {
        showBlackScreen()
        val delay = RetryPolicy.nextDelay(retryCount)
        if (delay == null) {
            showPlayError()
            return
        }
        retryCount++
        handler.postDelayed(retry, delay)
    }

    private fun showBlackScreen() {
        _binding?.blackScreen?.visibility = View.VISIBLE
    }

    private fun hideBlackScreen() {
        _binding?.blackScreen?.visibility = View.GONE
    }

    private fun showPlayError() {
        showBlackScreen()
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

    private fun legacyLoadControl(): LegacyLoadControl {
        return LegacyLoadControl.Builder()
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
        private const val TAG = "PlaybackVideoFragment"

        private const val MIN_BUFFER_MS = 4_000
        private const val MAX_BUFFER_MS = 20_000
        private const val BUFFER_FOR_PLAYBACK_MS = 2_000
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 4_000
        private const val TARGET_BUFFER_BYTES = 32 * 1024 * 1024
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        exoPlayer = SimpleExoPlayer.Builder(requireContext())
            .setLoadControl(legacyLoadControl())
            .build()
        exoPlayer?.setVideoSurfaceHolder(surfaceHolder)
        exoPlayer?.playWhenReady = true
        exoPlayer?.addVideoListener(object : VideoListener {
            override fun onRenderedFirstFrame() {
                hideBlackScreen()
            }
        })
        exoPlayer?.addListener(object : LegacyPlayer.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                Log.e(TAG, "LegacyPlaybackException $error")
                showPlayError()
            }
        })
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}
