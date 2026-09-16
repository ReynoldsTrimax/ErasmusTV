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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.erasmustv.app.ui.components.ProfileAvatarView

@Composable
fun ProfilePickerScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val firstProfileRequester = remember { FocusRequester() }

    var isManageMode by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<WatchProfile?>(null) }

    // Remote BACK handler:
    // 1. If currently in manage mode, BACK exits manage mode
    // 2. If active profile already exists and not managing, BACK returns to the app
    val canGoBack = (uiState as? ProfileUiState.Success)?.activeProfile != null
    BackHandler(enabled = isManageMode || canGoBack) {
        if (isManageMode) {
            isManageMode = false
        } else {
            onProfileSelected()
        }
    }

    // If a profile was selected for editing, show the EditProfileScreen
    if (editingProfile != null) {
        EditProfileScreen(
            profile = editingProfile!!,
            onSave = { updated ->
                viewModel.updateProfile(updated)
            },
            onDelete = {
                viewModel.deleteProfile(editingProfile!!.id)
                editingProfile = null
                isManageMode = false
            },
            onBack = {
                editingProfile = null
            }
        )
        return
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
                        text = if (isManageMode) "Manage Profiles" else "Who's Watching?",
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
                                isManageMode = isManageMode,
                                modifier = if (index == 0) Modifier.focusRequester(firstProfileRequester) else Modifier,
                                onClick = {
                                    if (isManageMode) {
                                        editingProfile = profile
                                    } else {
                                        viewModel.selectProfile(profile, onProfileSelected)
                                    }
                                }
                            )
                        }

                        // Add Profile button if under 5 profiles, not guest, and not in manage mode
                        if (state.profiles.size < 5 && !isGuest && !isManageMode) {
                            AddProfileCard(
                                onClick = {
                                    val nextNum = state.profiles.size + 1
                                    viewModel.createProfile("Profile $nextNum", "ocean", 2000)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(44.dp))

                    // Remote focusable "Manage Profiles" / "Done" button
                    TvFocusableCard(
                        onClick = { isManageMode = !isManageMode },
                        shape = RectangleShape,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
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
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isManageMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isManageMode) "Done" else "Manage Profiles",
                                tint = if (isFocused) FocusWhite else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isManageMode) "Done" else "Manage Profiles",
                                style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                                color = if (isFocused) FocusWhite else TextPrimary
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
    isManageMode: Boolean = false,
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
            focusedScale = 1.0f,
            focusedBorderWidth = 1.5.dp,
            focusedBorderColor = FocusWhite,
            modifier = modifier
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(
                        width = 1.dp,
                        color = if (isFocused) FocusWhite else BorderHairline,
                        shape = RectangleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatarView(
                    avatarKey = profile.avatarKey,
                    profileName = profile.name,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    iconSize = 48.dp
                )

                if (isManageMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                .border(1.dp, FocusWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = FocusWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
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
            focusedScale = 1.0f,
            focusedBorderWidth = 1.5.dp,
            focusedBorderColor = FocusWhite
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
