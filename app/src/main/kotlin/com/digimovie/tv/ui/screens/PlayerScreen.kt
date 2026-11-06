package com.digimovie.tv.ui.screens

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.digimovie.tv.MainActivity
import com.digimovie.tv.player.BraviaPlayerManager
import com.digimovie.tv.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    title: String,
    onClosePlayer: () -> Unit
) {
    val context = LocalContext.current
    val playerManager = remember { BraviaPlayerManager(context) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentActiveTitle by remember { mutableStateOf(title) }

    val errorRetryFocusRequester = remember { FocusRequester() }

    // Direct hardware TV remote key handler hook
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.playerKeyHandler = { event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                lastInteractionTime = System.currentTimeMillis()
                val keyCode = event.keyCode

                if (playbackError != null) {
                    // Let Compose focus handle buttons or handle Back
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        onClosePlayer()
                        true
                    } else {
                        false
                    }
                } else {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK -> {
                            onClosePlayer()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (!showControls) {
                                showControls = true
                            } else {
                                playerManager.togglePlayPause()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            showControls = true
                            playerManager.seekBackward()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            showControls = true
                            playerManager.seekForward()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            showControls = true
                            playerManager.togglePlayPause()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            showControls = true
                            playerManager.exoPlayer?.play()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            showControls = true
                            playerManager.exoPlayer?.pause()
                            true
                        }
                        else -> false
                    }
                }
            } else false
        }

        onDispose {
            activity?.playerKeyHandler = null
            playerManager.release()
        }
    }

    // Auto-focus retry button on error
    LaunchedEffect(playbackError) {
        if (playbackError != null) {
            delay(200)
            try {
                errorRetryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, lastInteractionTime, playbackError) {
        if (showControls && playbackError == null) {
            delay(4000)
            showControls = false
        }
    }

    // Time ticker loop
    LaunchedEffect(Unit) {
        while (isActive) {
            playerManager.exoPlayer?.let { player ->
                currentPosMs = player.currentPosition
                durationMs = player.duration.coerceAtLeast(0L)
                isPlaying = player.isPlaying
            }
            delay(500)
        }
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls && playbackError == null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "controlsAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (playbackError == null) {
                    showControls = !showControls
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
    ) {
        // Direct Hardware SurfaceView for Sony Bravia
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    playerManager.createPlayer(this, videoUrl) {
                        playerManager.exoPlayer?.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }

                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isBuffering = (playbackState == Player.STATE_BUFFERING)
                                if (playbackState == Player.STATE_READY) {
                                    playbackError = null
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                playbackError = "خطای استریم ویدیو: لینک ارائه‌شده توسط سرور در دسترس نیست یا منقضی شده است."
                                showControls = true
                            }
                        })
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DigiGold, modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "در حال بافر و همگام‌سازی ویدیو...",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // TV Player Controls Overlay with Smooth Alpha Animation
        if (controlsAlpha > 0.01f && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(controlsAlpha)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                // Top Bar: Title & Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentActiveTitle,
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val closeInteraction = remember { MutableInteractionSource() }
                    val isCloseFocused by closeInteraction.collectIsFocusedAsState()

                    Button(
                        onClick = onClosePlayer,
                        interactionSource = closeInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCloseFocused) DangerRed else SurfaceDark,
                            contentColor = TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            width = if (isCloseFocused) 2.dp else 1.dp,
                            color = if (isCloseFocused) TextWhite else SurfaceHighlight,
                            shape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(text = "✕ خروج از پلیر", fontWeight = FontWeight.Bold)
                    }
                }

                // Bottom Bar: Progress & Media Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Time Progress Row
                    val progress = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = DigiGold,
                        trackColor = SurfaceHighlight,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatTime(currentPosMs), color = TextWhite, fontSize = 13.sp)
                        Text(text = formatTime(durationMs), color = TextMuted, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media D-Pad Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvMediaButton(text = "⏪ -10s") {
                            playerManager.seekBackward()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        TvMediaButton(
                            text = if (isPlaying) "⏸ توقف" else "▶ پخش",
                            isPrimary = true
                        ) {
                            playerManager.togglePlayPause()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        TvMediaButton(text = "⏩ +10s") {
                            playerManager.seekForward()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }

        // TV Playback Error Modal Overlay
        playbackError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(580.dp)
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(DigiGold, DangerRed))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ خطا در دسترسی به استریم ویدیو",
                            color = DigiGold,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = err,
                            color = TextWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "برای بررسی عملکرد کامل سونی براویا و کنترل D-pad، می‌توانید نمونه تست Full HD سخت‌افزاری را پخش کنید.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Retry with Sample Stream Button (Auto-Focused)
                        val retryInteraction = remember { MutableInteractionSource() }
                        val isRetryFocused by retryInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = {
                                playbackError = null
                                currentActiveTitle = "$title (استریم تست سخت‌افزاری)"
                                playerManager.playUrl(BraviaPlayerManager.SONY_BRAVIA_TEST_STREAM)
                                showControls = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            interactionSource = retryInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRetryFocused) DigiGoldLight else DigiGold,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .focusRequester(errorRetryFocusRequester)
                                .scale(if (isRetryFocused) 1.05f else 1.0f)
                                .border(
                                    width = if (isRetryFocused) 3.dp else 0.dp,
                                    color = TextWhite,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                         keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                        playbackError = null
                                        currentActiveTitle = "$title (استریم تست سخت‌افزاری)"
                                        playerManager.playUrl(BraviaPlayerManager.SONY_BRAVIA_TEST_STREAM)
                                        showControls = true
                                        lastInteractionTime = System.currentTimeMillis()
                                        true
                                    } else false
                                }
                        ) {
                            Text(
                                text = "▶ پخش استریم تست سخت‌افزاری سونی (Full HD)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Close / Back Button
                        val backInteraction = remember { MutableInteractionSource() }
                        val isBackFocused by backInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = onClosePlayer,
                            interactionSource = backInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBackFocused) DangerRed else SurfaceHighlight,
                                contentColor = TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    width = if (isBackFocused) 2.dp else 1.dp,
                                    color = if (isBackFocused) TextWhite else SurfaceHighlight,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                         keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                        onClosePlayer()
                                        true
                                    } else false
                                }
                        ) {
                            Text(text = "✕ بازگشت به صفحه جزئیات", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvMediaButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isFocused -> DigiGoldLight
                isPrimary -> DigiGold
                else -> SurfaceDark
            },
            contentColor = if (isFocused || isPrimary) BgDark else TextWhite
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) FocusBorder else SurfaceHighlight,
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun formatTime(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
