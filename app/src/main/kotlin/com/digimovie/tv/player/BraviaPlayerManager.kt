package com.digimovie.tv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
class BraviaPlayerManager(private val context: Context) {

    companion object {
        const val SONY_BRAVIA_TEST_STREAM =
            "https://vjs.zencdn.net/v/oceans.mp4"
    }

    var exoPlayer: ExoPlayer? = null
        private set

    private var attachedPlayerView: PlayerView? = null
    private var playbackReadyCallback: (() -> Unit)? = null

    fun createPlayer(
        playerView: PlayerView,
        videoUrl: String,
        onPlaybackReady: () -> Unit = {}
    ): ExoPlayer {
        release()
        attachedPlayerView = playerView
        playbackReadyCallback = onPlaybackReady

        val audioSink = DefaultAudioSink.Builder(context)
            .setEnableAudioTrackPlaybackParams(true)
            .setEnableFloatOutput(false)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return audioSink
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setMediaCodecSelector(MediaCodecSelector.DEFAULT)
            setEnableDecoderFallback(true)
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; BRAVIA 4K Build/URT2.221028.001) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 DigiMovieTV/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        playerView.player = player
        playerView.useController = false // Native custom TV D-pad controls

        exoPlayer = player
        onPlaybackReady()

        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        return player
    }

    fun playUrl(videoUrl: String) {
        val player = exoPlayer
        if (player != null) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } else if (attachedPlayerView != null) {
            createPlayer(attachedPlayerView!!, videoUrl, playbackReadyCallback ?: {})
        }
    }

    fun seekForward() {
        exoPlayer?.let {
            val next = (it.currentPosition + 10_000).coerceAtMost(it.duration)
            it.seekTo(next)
        }
    }

    fun seekBackward() {
        exoPlayer?.let {
            val prev = (it.currentPosition - 10_000).coerceAtLeast(0)
            it.seekTo(prev)
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let {
            it.playWhenReady = !it.playWhenReady
        }
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        attachedPlayerView = null
        playbackReadyCallback = null
    }
}
