package com.erasmustv.app.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalContext
import com.erasmustv.app.core.config.AppConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ElectricBlue
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.ErrorRed
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.ui.components.ErasmusActionButton
import com.erasmustv.app.ui.components.ErasmusButtonStyle
import com.erasmustv.app.ui.components.TvFocusableCard

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var isEmailFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    val emailRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }
    val signInRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(AppConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("ErasmusAuth", "Google Sign In Activity Result: resultCode=${result.resultCode}, data=${result.data}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            android.util.Log.d("ErasmusAuth", "Google account=${account?.email}, hasIdToken=${!idToken.isNullOrBlank()}")
            if (!idToken.isNullOrBlank()) {
                viewModel.loginWithGoogle(idToken, onLoginSuccess)
            } else {
                android.util.Log.e("ErasmusAuth", "Google Sign-In returned null or empty ID token")
                viewModel.setError("Google Sign-In did not return a valid token. Please check configuration.")
            }
        } catch (e: ApiException) {
            android.util.Log.e("ErasmusAuth", "Google Sign In ApiException: statusCode=${e.statusCode}, status=${e.status}, message=${e.message}", e)
            if (e.statusCode != 12501 && e.statusCode != 16) { // 12501: SIGN_IN_CANCELLED, 16: CANCELLED
                val errorMsg = when (e.statusCode) {
                    10 -> "Google Sign-In configuration error (Developer Error 10). The app SHA-1 fingerprint must be registered in Google Cloud Console."
                    7 -> "Network error connecting to Google. Please check your connection."
                    12500 -> "Google Sign-In failed (12500). Please ensure Google Play Services is up to date."
                    else -> "Google Sign-In failed (${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }
                viewModel.setError(errorMsg)
            }
        } catch (e: Exception) {
            android.util.Log.e("ErasmusAuth", "Google Sign In unexpected error", e)
            viewModel.setError("Google Sign-In error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    // Request initial focus on Email (or Sign In if email already populated) without stealing on recomposition
    var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasRequestedInitialFocus) {
            try {
                if (email.isEmpty()) {
                    emailRequester.requestFocus()
                } else {
                    signInRequester.requestFocus()
                }
                hasRequestedInitialFocus = true
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // Subtle ambient blue glow on background
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Cinematic Brand Showcase
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "E R A S M U S",
                    style = ErasmusTvTypography.Wordmark.copy(letterSpacing = 7.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(3.dp)
                        .background(ElectricBlue)
                )

                Spacer(modifier = Modifier.height(ErasmusSpacing.Large))

                Text(
                    text = "Cinema Engineered\nfor Television.",
                    style = ErasmusTvTypography.BillboardTitle.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Stream high-bitrate films and series with zero ads, multi-source failover clusters, and cross-device cloud profile synchronization.",
                    style = ErasmusTvTypography.BodyLarge.copy(
                        color = TextMuted,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.width(440.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Feature Highlights
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill(text = "4K HDR")
                    FeaturePill(text = "Dolby Audio")
                    FeaturePill(text = "Multi-Profile Sync")
                }
            }

            // Right Column: Elevated Login Card
            Box(
                modifier = Modifier
                    .width(450.dp)
                    .background(SurfaceDark, ErasmusShapes.CardLarge)
                    .border(1.dp, BorderHairline, ErasmusShapes.CardLarge)
                    .padding(horizontal = 30.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sign In",
                        style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 22.sp),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Enter your Erasmus credentials to continue",
                        style = ErasmusTvTypography.Body.copy(fontSize = 12.sp),
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "EMAIL",
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (isEmailFocused) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isEmailFocused) FocusWhite else TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(ErasmusSpacing.XSmall))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(if (isEmailFocused) Color(0x14FFFFFF) else PitchBlack, ErasmusShapes.Input)
                                .border(
                                    width = if (isEmailFocused) 1.5.dp else 1.dp,
                                    color = if (isEmailFocused) FocusWhite else BorderHairline,
                                    shape = ErasmusShapes.Input
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isEmailFocused) FocusWhite else TextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (email.isEmpty() && !isEmailFocused) {
                                        Text(
                                            text = "name@example.com",
                                            style = ErasmusTvTypography.Body.copy(fontSize = 13.sp, color = TextMuted.copy(alpha = 0.5f))
                                        )
                                    }
                                    BasicTextField(
                                        value = email,
                                        onValueChange = { viewModel.onEmailChange(it) },
                                        textStyle = ErasmusTvTypography.Body.copy(fontSize = 13.sp, color = TextPrimary),
                                        cursorBrush = SolidColor(FocusWhite),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Email,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { passwordRequester.requestFocus() }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(emailRequester)
                                            .onFocusChanged { isEmailFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionDown -> {
                                                            passwordRequester.requestFocus()
                                                            true
                                                        }
                                                        Key.DirectionUp -> true
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PASSWORD",
                            style = ErasmusTvTypography.Badge.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (isPasswordFocused) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isPasswordFocused) FocusWhite else TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.height(ErasmusSpacing.XSmall))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(if (isPasswordFocused) Color(0x14FFFFFF) else PitchBlack, ErasmusShapes.Input)
                                .border(
                                    width = if (isPasswordFocused) 1.5.dp else 1.dp,
                                    color = if (isPasswordFocused) FocusWhite else BorderHairline,
                                    shape = ErasmusShapes.Input
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isPasswordFocused) FocusWhite else TextMuted.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (password.isEmpty() && !isPasswordFocused) {
                                        Text(
                                            text = "••••••••••••",
                                            style = ErasmusTvTypography.Body.copy(fontSize = 13.sp, color = TextMuted.copy(alpha = 0.5f))
                                        )
                                    }
                                    BasicTextField(
                                        value = password,
                                        onValueChange = { viewModel.onPasswordChange(it) },
                                        textStyle = ErasmusTvTypography.Body.copy(fontSize = 13.sp, color = TextPrimary),
                                        cursorBrush = SolidColor(FocusWhite),
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                viewModel.login(onLoginSuccess)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(passwordRequester)
                                            .onFocusChanged { isPasswordFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionUp -> {
                                                            emailRequester.requestFocus()
                                                            true
                                                        }
                                                        Key.DirectionDown -> {
                                                            signInRequester.requestFocus()
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // User-facing Error Banner
                    if (uiState is LoginUiState.Error) {
                        Spacer(modifier = Modifier.height(ErasmusSpacing.Small))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ErrorRed.copy(alpha = 0.12f), ErasmusShapes.CardSmall)
                                .border(1.dp, ErrorRed.copy(alpha = 0.4f), ErasmusShapes.CardSmall)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = ErrorRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = (uiState as LoginUiState.Error).message,
                                style = ErasmusTvTypography.Body.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                                color = ErrorRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign In Button or Loading
                    if (uiState is LoginUiState.Loading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(SurfaceDark, ErasmusShapes.Button),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Authenticating...",
                                style = ErasmusTvTypography.ButtonText.copy(fontSize = 13.sp, color = TextPrimary)
                            )
                        }
                    } else {
                        ErasmusActionButton(
                            text = "Sign In",
                            onClick = { viewModel.login(onLoginSuccess) },
                            style = ErasmusButtonStyle.Primary,
                            height = 40.dp,
                            onNavigateUp = { passwordRequester.requestFocus() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(signInRequester)
                        )
                    }

                    Spacer(modifier = Modifier.height(ErasmusSpacing.Small))

                    // Sign in with Google Option
                    // Kept bespoke: the Google "G" badge is a composable glyph, not an
                    // ImageVector, so it cannot be passed to ErasmusActionButton(icon=).
                    TvFocusableCard(
                        onClick = {
                            try {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            } catch (_: Exception) {}
                        },
                        focusedScale = 1.025f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = FocusWhite,
                        shape = ErasmusShapes.Button,
                        contentDescription = "Sign in with Google",
                        modifier = Modifier.fillMaxWidth()
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isFocused) Color.White.copy(alpha = 0.15f) else SurfaceDark,
                                    ErasmusShapes.Button
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFocused) Color.Transparent else BorderHairline,
                                    shape = ErasmusShapes.Button
                                )
                                .padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoogleIconBadge()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = ErasmusTvTypography.ButtonText.copy(fontSize = 13.sp),
                                color = if (isFocused) Color.White else TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ErasmusSpacing.Small))

                    // Continue as Guest Option
                    ErasmusActionButton(
                        text = "Continue as Guest",
                        onClick = { viewModel.continueAsGuest(onLoginSuccess) },
                        style = ErasmusButtonStyle.Secondary,
                        height = 40.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleIconBadge() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            style = ErasmusTvTypography.ButtonText.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4285F4)
            )
        )
    }
}

@Composable
private fun FeaturePill(text: String) {
    Box(
        modifier = Modifier
            .background(SurfaceDark, ErasmusShapes.Button)
            .border(1.dp, BorderHairline, ErasmusShapes.Button)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = ErasmusTvTypography.Badge.copy(
                fontSize = 11.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
