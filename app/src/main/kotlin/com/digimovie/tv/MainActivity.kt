package com.digimovie.tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digimovie.tv.data.AuthManager
import com.digimovie.tv.network.ApiClient
import com.digimovie.tv.network.ShowcaseSection
import com.digimovie.tv.ui.screens.DetailScreen
import com.digimovie.tv.ui.screens.HomeScreen
import com.digimovie.tv.ui.screens.LoginScreen
import com.digimovie.tv.ui.screens.PlayerScreen
import com.digimovie.tv.ui.theme.DigiMovieTVTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    DETAIL,
    PLAYER,
    LOGIN
}

class MainTvViewModel : ViewModel() {
    private val _sections = MutableStateFlow<List<ShowcaseSection>>(emptyList())
    val sections = _sections.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadShowcase()
    }

    fun loadShowcase() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = ApiClient.api.getShowcase()
                if (response.isSuccessful && response.body() != null) {
                    _sections.value = response.body()!!
                } else {
                    _errorMessage.value = "خطا در دریافت لیست فیلم‌ها (کد: ${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "خطای ارتباط با سرور: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    var playerKeyHandler: ((KeyEvent) -> Boolean)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (playerKeyHandler?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        enableEdgeToEdge()

        setContent {
            DigiMovieTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainTvViewModel = viewModel()
                    val sections by viewModel.sections.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()
                    val errorMessage by viewModel.errorMessage.collectAsState()

                    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
                    var selectedMovieId by remember { mutableStateOf<Int?>(null) }
                    var activeVideoUrl by remember { mutableStateOf("") }
                    var activeVideoTitle by remember { mutableStateOf("") }

                    var isLoggedIn by remember { mutableStateOf(authManager.isLoggedIn) }
                    var currentUsername by remember { mutableStateOf(authManager.username) }

                    // Global Android TV Back Button Handling
                    BackHandler(enabled = currentScreen != ScreenState.HOME) {
                        when (currentScreen) {
                            ScreenState.PLAYER -> currentScreen = ScreenState.DETAIL
                            ScreenState.DETAIL -> currentScreen = ScreenState.HOME
                            ScreenState.LOGIN -> currentScreen = ScreenState.HOME
                            ScreenState.HOME -> finish()
                        }
                    }

                    when (currentScreen) {
                        ScreenState.HOME -> {
                            HomeScreen(
                                sections = sections,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                isLoggedIn = isLoggedIn,
                                username = currentUsername,
                                onMovieSelected = { movieId ->
                                    selectedMovieId = movieId
                                    currentScreen = ScreenState.DETAIL
                                },
                                onLoginClick = {
                                    currentScreen = ScreenState.LOGIN
                                },
                                onLogoutClick = {
                                    authManager.logout()
                                    isLoggedIn = false
                                    currentUsername = null
                                }
                            )
                        }

                        ScreenState.DETAIL -> {
                            selectedMovieId?.let { id ->
                                DetailScreen(
                                    movieId = id,
                                    authToken = authManager.authToken,
                                    onPlayVideo = { url, title ->
                                        activeVideoUrl = url
                                        activeVideoTitle = title
                                        currentScreen = ScreenState.PLAYER
                                    },
                                    onLoginClick = {
                                        currentScreen = ScreenState.LOGIN
                                    },
                                    onBackClick = {
                                        currentScreen = ScreenState.HOME
                                    }
                                )
                            } ?: run {
                                currentScreen = ScreenState.HOME
                            }
                        }

                        ScreenState.PLAYER -> {
                            PlayerScreen(
                                videoUrl = activeVideoUrl,
                                title = activeVideoTitle,
                                onClosePlayer = {
                                    currentScreen = ScreenState.DETAIL
                                }
                            )
                        }

                        ScreenState.LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = { token, user ->
                                    authManager.authToken = token
                                    authManager.username = user
                                    authManager.isLoggedIn = true
                                    isLoggedIn = true
                                    currentUsername = user
                                    currentScreen = ScreenState.HOME
                                    viewModel.loadShowcase()
                                },
                                onBackClick = {
                                    currentScreen = ScreenState.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}