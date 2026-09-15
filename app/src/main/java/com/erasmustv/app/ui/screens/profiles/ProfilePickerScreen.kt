package com.erasmustv.app.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.SurfacePill
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.core.theme.getAvatarGradient
import com.erasmustv.app.data.model.WatchProfile
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.erasmustv.app.ui.components.TvFocusableCard

import androidx.compose.material.icons.automirrored.filled.ExitToApp

@Composable
fun ProfilePickerScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val firstProfileRequester = remember { FocusRequester() }

    // If active profile already exists, remote BACK cleanly returns to the app
    val canGoBack = (uiState as? ProfileUiState.Success)?.activeProfile != null
    androidx.activity.compose.BackHandler(enabled = canGoBack) {
        onProfileSelected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 2.5.dp
                )
            }
            is ProfileUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = ErasmusTvTypography.Body,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TvFocusableCard(onClick = { viewModel.loadProfiles() }) {
                        Text(
                            text = "Retry",
                            style = ErasmusTvTypography.ButtonText,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is ProfileUiState.Success -> {
                LaunchedEffect(state.profiles) {
                    if (state.profiles.isNotEmpty()) {
                        firstProfileRequester.requestFocus()
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Erasmus Wordmark
                    Text(
                        text = "E R A S M U S",
                        style = ErasmusTvTypography.Wordmark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Who\'s Watching?",
                        style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 28.sp),
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(44.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.profiles.forEachIndexed { index, profile ->
                            ProfileCard(
                                profile = profile,
                                isSelected = profile.id == state.activeProfile?.id,
                                modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                onClick = {
                                    viewModel.selectProfile(profile, onProfileSelected)
                                }
                            )
                        }

                        // Add Profile button if under 5 profiles and not guest
                        if (state.profiles.size < 5 && !isGuest) {
                            AddProfileCard(
                                onClick = {
                                    val nextNum = state.profiles.size + 1
                                    viewModel.createProfile("Profile $nextNum", "ocean", 2000)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(44.dp))

                    // Remote focusable Sign Out / Exit Guest button
                    TvFocusableCard(
                        onClick = { viewModel.signOut(onSignOut) },
                        shape = RectangleShape,
                        focusedScale = 1.04f,
                        focusedBorderColor = FocusWhite
                    ) { isFocused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(
                                    if (isFocused) SurfaceCard else SurfacePill,
                                    RectangleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFocused) FocusWhite else BorderHairline,
                                    shape = RectangleShape
                                )
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = if (isFocused) FocusWhite else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    isGuest -> "Exit Guest Mode"
                                    !userEmail.isNullOrBlank() -> "Sign Out ($userEmail)"
                                    else -> "Sign Out"
                                },
                                style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                                color = if (isFocused) FocusWhite else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileCard(
    profile: WatchProfile,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TvFocusableCard(
            onClick = onClick,
            shape = RectangleShape,
            focusedScale = 1.05f,
            focusedBorderColor = FocusWhite,
            unfocusedBorderColor = BorderHairline,
            modifier = modifier
        ) { isFocused ->
            val gradient = getAvatarGradient(profile.avatarKey)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Brush.linearGradient(gradient), RectangleShape)
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else BorderHairline,
                        shape = RectangleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = profile.name,
                    tint = TextPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text(
            text = profile.name,
            style = ErasmusTvTypography.SectionTitle.copy(fontSize = 15.sp),
            color = if (isSelected) FocusWhite else TextSecondary
        )
    }
}

@Composable
private fun AddProfileCard(
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TvFocusableCard(
            onClick = onClick,
            shape = RectangleShape,
            focusedScale = 1.05f,
            focusedBorderColor = FocusWhite,
            unfocusedBorderColor = BorderHairline
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        if (isFocused) SurfaceCard else SurfaceDark,
                        RectangleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else BorderHairline,
                        shape = RectangleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = if (isFocused) FocusWhite else TextMuted,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = "Add Profile",
            style = ErasmusTvTypography.SectionTitle.copy(fontSize = 15.sp),
            color = TextMuted
        )
    }
}
