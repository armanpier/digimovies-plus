package com.digimovie.tv.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.digimovie.tv.network.ApiClient
import com.digimovie.tv.network.MovieDetailResponse
import com.digimovie.tv.player.BraviaPlayerManager
import com.digimovie.tv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(
    movieId: Int,
    authToken: String?,
    onPlayVideo: (videoUrl: String, title: String) -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var detailResponse by remember { mutableStateOf<MovieDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val playFocusRequester = remember { FocusRequester() }

    LaunchedEffect(movieId) {
        isLoading = true
        errorMessage = null
        try {
            val authHeader = if (!authToken.isNullOrEmpty()) "Bearer $authToken" else null
            val response = ApiClient.api.getMovieDetail(movieId, authHeader)
            if (response.isSuccessful && response.body() != null) {
                detailResponse = response.body()
            } else {
                errorMessage = "خطا در دریافت جزئیات فیلم (کد: ${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "خطای ارتباط با سرور: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(detailResponse) {
        if (detailResponse != null) {
            delay(200)
            try {
                playFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DigiGold)
            }
            return@Box
        }

        if (!errorMessage.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = errorMessage!!, color = DangerRed, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackClick) { Text("بازگشت") }
            }
            return@Box
        }

        val detail = detailResponse ?: return@Box
        val info = detail.movieInfo

        // Background Backdrop
        val bgUrl = info?.imageBack ?: info?.featuredImageUrl ?: info?.imageUrl
        if (!bgUrl.isNullOrEmpty()) {
            AsyncImage(
                model = bgUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BgDark.copy(alpha = 0.8f),
                                BgDark
                            )
                        )
                    )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(36.dp)
        ) {
            // Header Row: Back button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    val isBackFocused by backInteraction.collectIsFocusedAsState()

                    Button(
                        onClick = onBackClick,
                        interactionSource = backInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackFocused) DigiGold else SurfaceDark,
                            contentColor = if (isBackFocused) BgDark else TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            width = if (isBackFocused) 2.dp else 1.dp,
                            color = if (isBackFocused) FocusBorder else SurfaceHighlight,
                            shape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(text = "➔ بازگشت به خانه", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Movie Main Details (Split: Poster + Info)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Poster Card
                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(250.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceHighlight, RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = info?.imageUrl,
                            contentDescription = info?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Metadata Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info?.title ?: "",
                            color = TextWhite,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!info?.titleEn.isNullOrEmpty()) {
                            Text(
                                text = info.titleEn,
                                color = TextMuted,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            info?.imdbRate?.let { rate ->
                                Text(
                                    text = "★ $rate IMDB",
                                    color = DigiGold,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFF2E2412), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .border(1.dp, DigiGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            info?.releaseYear?.let { year ->
                                Text(text = year, color = TextWhite, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            info?.duration?.let { dur ->
                                Text(text = dur, color = TextMuted, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        info?.genre?.let { genres ->
                            Text(
                                text = "ژانر: ${genres.joinToString("، ")}",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Play Trailer / Play Video Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val playInteraction = remember { MutableInteractionSource() }
                            val isPlayFocused by playInteraction.collectIsFocusedAsState()

                            val playableUrl = detail.trailerUrl
                                ?: detail.movieDownloadUrls?.firstOrNull()?.url
                                ?: detail.serieDownloadUrls?.firstOrNull()?.episodes?.firstOrNull()?.downloadUrls?.firstOrNull()?.url

                            if (!playableUrl.isNullOrEmpty()) {
                                Button(
                                    onClick = {
                                        onPlayVideo(playableUrl, info?.title ?: "پخش ویدیو")
                                    },
                                    interactionSource = playInteraction,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlayFocused) DigiGoldLight else DigiGold,
                                        contentColor = BgDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .focusRequester(playFocusRequester)
                                        .scale(if (isPlayFocused) 1.05f else 1f)
                                        .border(
                                            width = if (isPlayFocused) 3.dp else 0.dp,
                                            color = TextWhite,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                                 keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                                onPlayVideo(playableUrl, info?.title ?: "پخش ویدیو")
                                                true
                                            } else false
                                        }
                                ) {
                                    Text(
                                        text = "▶ شروع پخش ویدیو (پلیر سخت‌افزاری)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Direct Hardware Stream Demo Button
                            val demoInteraction = remember { MutableInteractionSource() }
                            val isDemoFocused by demoInteraction.collectIsFocusedAsState()

                            Button(
                                onClick = {
                                    onPlayVideo(
                                        BraviaPlayerManager.SONY_BRAVIA_TEST_STREAM,
                                        "${info?.title ?: "ویدیو"} (نمونه Full HD)"
                                    )
                                },
                                interactionSource = demoInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDemoFocused) SurfaceHighlight else SurfaceDark,
                                    contentColor = if (isDemoFocused) DigiGoldLight else TextWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .scale(if (isDemoFocused) 1.05f else 1f)
                                    .border(
                                        width = if (isDemoFocused) 2.dp else 1.dp,
                                        color = if (isDemoFocused) FocusBorder else SurfaceHighlight,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                             keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                            onPlayVideo(
                                                BraviaPlayerManager.SONY_BRAVIA_TEST_STREAM,
                                                "${info?.title ?: "ویدیو"} (نمونه Full HD)"
                                            )
                                            true
                                        } else false
                                    }
                            ) {
                                Text(
                                    text = "🎬 تست استریم Sony Bravia Full HD",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Subscription Notice (if content requires VIP)
            if (detail.needSubscription == true) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(DigiGold, SurfaceHighlight))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "👑 نیازمند اشتراک ویژه VIP دیجی‌موویز",
                                    color = DigiGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "برای دسترسی کامل به لینک‌های دانلود با کیفیت 4K و 1080p و تمام قسمت‌ها، وارد حساب ویژه خود شوید.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }

                            val loginBtnInteraction = remember { MutableInteractionSource() }
                            val isLoginBtnFocused by loginBtnInteraction.collectIsFocusedAsState()

                            Button(
                                onClick = onLoginClick,
                                interactionSource = loginBtnInteraction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLoginBtnFocused) DigiGoldLight else DigiGold,
                                    contentColor = BgDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .scale(if (isLoginBtnFocused) 1.05f else 1f)
                                    .border(
                                        width = if (isLoginBtnFocused) 2.dp else 0.dp,
                                        color = TextWhite,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(text = "ورود با QR یا کلمه عبور", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Plot Description
            item {
                info?.shortDescription?.let { desc ->
                    Text(
                        text = "خلاصه داستان:",
                        color = DigiGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = desc,
                        color = TextWhite,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Series Season / Episode Selector (if available)
            detail.serieDownloadUrls?.let { seasons ->
                if (seasons.isNotEmpty()) {
                    item {
                        Text(
                            text = "فصل‌ها و قسمت‌ها:",
                            color = DigiGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(seasons) { season ->
                        Text(
                            text = season.seasonName ?: "فصل",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        season.episodes?.let { episodes ->
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                items(episodes) { ep ->
                                    val epInteraction = remember { MutableInteractionSource() }
                                    val isEpFocused by epInteraction.collectIsFocusedAsState()
                                    val streamUrl = ep.downloadUrls?.firstOrNull()?.url ?: detail.trailerUrl

                                    Button(
                                        onClick = {
                                            if (!streamUrl.isNullOrEmpty()) {
                                                onPlayVideo(streamUrl, "${info?.title} - ${ep.episodeName}")
                                            }
                                        },
                                        interactionSource = epInteraction,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEpFocused) DigiGold else SurfaceDark,
                                            contentColor = if (isEpFocused) BgDark else TextWhite
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .border(
                                                width = if (isEpFocused) 2.dp else 1.dp,
                                                color = if (isEpFocused) FocusBorder else SurfaceHighlight,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .onPreviewKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                                    if (!streamUrl.isNullOrEmpty()) {
                                                        onPlayVideo(streamUrl, "${info?.title} - ${ep.episodeName}")
                                                    }
                                                    true
                                                } else false
                                            }
                                    ) {
                                        Text(text = ep.episodeName ?: "قسمت", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Movie Download Qualities (if available)
            detail.movieDownloadUrls?.let { downloadList ->
                if (downloadList.isNotEmpty()) {
                    item {
                        Text(
                            text = "کیفیت‌های موجود جهت پخش:",
                            color = DigiGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(downloadList) { dl ->
                                val dlInteraction = remember { MutableInteractionSource() }
                                val isDlFocused by dlInteraction.collectIsFocusedAsState()

                                Button(
                                    onClick = {
                                        dl.url?.let { url ->
                                            onPlayVideo(url, "${info?.title} (${dl.quality})")
                                        }
                                    },
                                    interactionSource = dlInteraction,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDlFocused) DigiGold else SurfaceDark,
                                        contentColor = if (isDlFocused) BgDark else TextWhite
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .border(
                                            width = if (isDlFocused) 2.dp else 1.dp,
                                            color = if (isDlFocused) FocusBorder else SurfaceHighlight,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                ) {
                                    Text(text = dl.quality ?: "پخش", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
