package com.digimovie.tv.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digimovie.tv.network.ApiClient
import com.digimovie.tv.network.LoginRequest
import com.digimovie.tv.ui.components.QrGenerator
import com.digimovie.tv.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (token: String, user: String) -> Unit,
    onBackClick: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrToken by remember { mutableStateOf<String?>(null) }
    var qrStatusText by remember { mutableStateOf("در حال تولید QR کد ورود...") }
    var qrTimeLeft by remember { mutableStateOf(120) }
    var isPollingActive by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isPasswordLoading by remember { mutableStateOf(false) }

    // Fetch initial QR Code
    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.api.createQrLogin()
            if (response.isSuccessful && response.body()?.status == true) {
                val data = response.body()!!
                qrToken = data.token
                qrTimeLeft = data.expiresIn ?: 120
                data.url?.let { url ->
                    qrBitmap = QrGenerator.generateQrBitmap(url, 400)
                }
                qrStatusText = "با دوربین گوشی اسکن کنید تا وارد شوید"
                isPollingActive = true
            } else {
                qrStatusText = "خطا در تولید QR کد"
            }
        } catch (e: Exception) {
            qrStatusText = "خطای ارتباط با سرور: ${e.message}"
        }
    }

    // QR Status Poller Loop
    LaunchedEffect(isPollingActive, qrToken) {
        val token = qrToken ?: return@LaunchedEffect
        while (isActive && isPollingActive && qrTimeLeft > 0) {
            delay(3000)
            try {
                val checkResp = ApiClient.api.checkQrLogin(token)
                if (checkResp.isSuccessful) {
                    val result = checkResp.body()
                    if (result?.loginStatus == "approved" && result.authToken != null) {
                        isPollingActive = false
                        qrStatusText = "ورود با موفقیت انجام شد!"
                        delay(600)
                        onLoginSuccess(result.authToken, result.userInfo?.displayName ?: "کاربر")
                        break
                    } else if (result?.msg != null) {
                        qrStatusText = result.msg
                    }
                }
            } catch (e: Exception) {
                // Ignore temporary network flickers
            }
        }
    }

    // Expiry timer
    LaunchedEffect(isPollingActive) {
        while (isActive && isPollingActive && qrTimeLeft > 0) {
            delay(1000)
            qrTimeLeft--
        }
        if (qrTimeLeft <= 0) {
            qrStatusText = "کد QR منقضی شد. لطفاً صفحه را مجدداً باز کنید."
            isPollingActive = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(36.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ورود به حساب کاربری دیجی‌موویز",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

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
                    Text(text = "بازگشت ➔", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Split: Left QR Login, Right Password Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // LEFT: QR Code Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "روش سریع: اسکن با گوشی",
                            color = DigiGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR Code Login",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(color = DigiGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = qrStatusText,
                            color = TextWhite,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        if (isPollingActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اعتبار کد: $qrTimeLeft ثانیه",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // RIGHT: Standard Password Login
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ورود با نام کاربری و رمز",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("نام کاربری یا ایمیل") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = DigiGold,
                                unfocusedBorderColor = SurfaceHighlight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("کلمه عبور") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = DigiGold,
                                unfocusedBorderColor = SurfaceHighlight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!passwordError.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = passwordError!!, color = DangerRed, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val submitInteraction = remember { MutableInteractionSource() }
                        val isSubmitFocused by submitInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = {
                                if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                    passwordError = "لطفاً نام کاربری و کلمه عبور را وارد کنید"
                                    return@Button
                                }
                                isPasswordLoading = true
                                passwordError = null
                                coroutineScope.launch {
                                    try {
                                        val req = LoginRequest(usernameInput.trim(), passwordInput)
                                        val response = ApiClient.api.login(req)
                                        isPasswordLoading = false
                                        if (response.isSuccessful && response.body()?.status == true) {
                                            val body = response.body()!!
                                            onLoginSuccess(body.authToken ?: "", body.userInfo?.displayName ?: usernameInput)
                                        } else {
                                            passwordError = response.body()?.msg ?: "نام کاربری یا کلمه عبور اشتباه است"
                                        }
                                    } catch (e: Exception) {
                                        isPasswordLoading = false
                                        passwordError = "خطای ارتباط: ${e.message}"
                                    }
                                }
                            },
                            interactionSource = submitInteraction,
                            enabled = !isPasswordLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSubmitFocused) DigiGoldLight else DigiGold,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSubmitFocused) 2.dp else 0.dp,
                                    color = FocusBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            if (isPasswordLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BgDark)
                            } else {
                                Text(text = "ورود به حساب", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
