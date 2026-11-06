package com.digimovie.tv.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.digimovie.tv.network.MovieItem
import com.digimovie.tv.network.ShowcaseSection
import com.digimovie.tv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    sections: List<ShowcaseSection>,
    isLoading: Boolean,
    errorMessage: String?,
    isLoggedIn: Boolean,
    username: String?,
    onMovieSelected: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var heroMovie by remember { mutableStateOf<MovieItem?>(null) }
    val heroFocusRequester = remember { FocusRequester() }

    LaunchedEffect(sections) {
        if (sections.isNotEmpty()) {
            val firstWithItems = sections.firstOrNull { !it.items.isNullOrEmpty() }
            heroMovie = firstWithItems?.items?.firstOrNull()
        }
    }

    LaunchedEffect(sections, isLoading) {
        if (!isLoading && sections.isNotEmpty()) {
            delay(200)
            try {
                heroFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Hero Background Image with Gradient
        heroMovie?.let { hero ->
            val bgUrl = hero.featuredImageUrl ?: hero.imageUrl
            if (!bgUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = bgUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BgDark.copy(alpha = 0.7f),
                                    BgDark
                                )
                            )
                        )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Top Navigation Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DIGIMOVIES",
                            color = DigiGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+",
                            color = BgDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .background(DigiGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Login / Account Action
                    val loginInteractionSource = remember { MutableInteractionSource() }
                    val isLoginFocused by loginInteractionSource.collectIsFocusedAsState()

                    Button(
                        onClick = { if (isLoggedIn) onLogoutClick() else onLoginClick() },
                        interactionSource = loginInteractionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoginFocused) DigiGold else SurfaceDark,
                            contentColor = if (isLoginFocused) BgDark else TextWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(
                                width = if (isLoginFocused) 2.dp else 1.dp,
                                color = if (isLoginFocused) DigiGoldLight else SurfaceHighlight,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                    if (isLoggedIn) onLogoutClick() else onLoginClick()
                                    true
                                } else false
                            }
                    ) {
                        Text(
                            text = if (isLoggedIn) "خروج ($username)" else "ورود با QR کد / حساب",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hero Details Section
            item {
                heroMovie?.let { hero ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = hero.title ?: hero.titleEn ?: "",
                            color = TextWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!hero.titleEn.isNullOrEmpty() && hero.titleEn != hero.title) {
                            Text(
                                text = hero.titleEn,
                                color = TextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            hero.imdbRate?.let { rate ->
                                Text(
                                    text = "★ $rate",
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
                            hero.genre?.let { genres ->
                                Text(
                                    text = genres.joinToString(" • "),
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Watch Action Button
                        val heroButtonInteraction = remember { MutableInteractionSource() }
                        val isHeroBtnFocused by heroButtonInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = { hero.id?.let { onMovieSelected(it) } },
                            interactionSource = heroButtonInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHeroBtnFocused) DigiGoldLight else DigiGold,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .focusRequester(heroFocusRequester)
                                .scale(if (isHeroBtnFocused) 1.05f else 1f)
                                .border(
                                    width = if (isHeroBtnFocused) 2.dp else 0.dp,
                                    color = TextWhite,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                         keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                        hero.id?.let { onMovieSelected(it) }
                                        true
                                    } else false
                                }
                        ) {
                            Text(text = "▶ مشاهده و پخش", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Loading / Error States
            if (isLoading && sections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DigiGold)
                    }
                }
            }

            if (!errorMessage.isNullOrEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage, color = DangerRed, fontSize = 16.sp)
                    }
                }
            }

            // Movie Carousels by Section
            items(sections) { section ->
                val itemsList = section.items
                if (!itemsList.isNullOrEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 22.dp)
                    ) {
                        Text(
                            text = section.name,
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 36.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(itemsList) { movie ->
                                MovieCard(
                                    movie = movie,
                                    onFocused = { heroMovie = movie },
                                    onClick = { movie.id?.let { onMovieSelected(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: MovieItem,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused()
        }
    }

    val scale by animateFloatAsState(targetValue = if (isFocused) 1.10f else 1.0f, label = "scale")

    Column(
        modifier = Modifier
            .width(135.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onClick()
                    true
                } else false
            }
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) FocusBorder else SurfaceHighlight,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            AsyncImage(
                model = movie.imageUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            movie.imdbRate?.let { rate ->
                Text(
                    text = "$rate",
                    color = DigiGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title ?: movie.titleEn ?: "",
            color = if (isFocused) DigiGoldLight else TextWhite,
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}