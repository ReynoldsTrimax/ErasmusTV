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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.RatingGold
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.SurfaceElevated
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import com.erasmustv.app.ui.components.ProfileAvatarView
import androidx.compose.ui.focus.focusProperties
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable

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
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Remote BACK handler:
    // 1. If sign-out dialog is showing, BACK dismisses the dialog
    // 2. If currently in manage mode, BACK exits manage mode
    // 3. If active profile already exists and not managing, BACK returns to the app
    val canGoBack = (uiState as? ProfileUiState.Success)?.activeProfile != null
    BackHandler(enabled = showSignOutDialog || isManageMode || canGoBack) {
        if (showSignOutDialog) {
            showSignOutDialog = false
        } else if (isManageMode) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = !showSignOutDialog },
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "E R A S M U S",
                        style = ErasmusTvTypography.Wordmark
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Who's Watching?",
                        style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 30.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a profile to start streaming",
                        style = ErasmusTvTypography.Body.copy(fontSize = 13.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(44.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(SurfaceElevated)
                                        .border(1.dp, BorderHairline)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(14.dp)
                                        .background(SurfaceElevated)
                                )
                            }
                        }
                    }
                }
            }
            is ProfileUiState.Error -> {
                val errorRetryRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(100)
                    try {
                        errorRetryRequester.requestFocus()
                    } catch (_: Exception) {}
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "E R A S M U S",
                        style = ErasmusTvTypography.Wordmark
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Unable to Load Profiles",
                        style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 24.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = ErasmusTvTypography.Body.copy(fontSize = 13.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvFocusableCard(
                            onClick = { viewModel.loadProfiles() },
                            shape = RectangleShape,
                            focusedScale = 1.025f,
                            focusedBorderWidth = 1.5.dp,
                            focusedBorderColor = FocusWhite,
                            modifier = Modifier.focusRequester(errorRetryRequester)
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .background(if (isFocused) SurfaceCard else SurfacePill, RectangleShape)
                                    .border(1.dp, if (isFocused) Color.Transparent else BorderHairline, RectangleShape)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    style = ErasmusTvTypography.ButtonText,
                                    color = if (isFocused) FocusWhite else TextPrimary
                                )
                            }
                        }

                        TvFocusableCard(
                            onClick = { showSignOutDialog = true },
                            shape = RectangleShape,
                            focusedScale = 1.025f,
                            focusedBorderWidth = 1.5.dp,
                            focusedBorderColor = FocusWhite
                        ) { isFocused ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .background(if (isFocused) SurfaceCard else SurfacePill, RectangleShape)
                                    .border(1.dp, if (isFocused) Color.Transparent else BorderHairline, RectangleShape)
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = if (isGuest) "Exit Guest" else "Sign Out",
                                    tint = if (isFocused) FocusWhite else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isGuest) "Exit Guest" else "Sign Out",
                                    style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                                    color = if (isFocused) FocusWhite else TextPrimary
                                )
                            }
                        }
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
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isManageMode) "Manage Profiles" else "Who's Watching?",
                        style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 30.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isManageMode) "Choose a profile to edit or delete" else "Select a profile to start streaming",
                        style = ErasmusTvTypography.Body.copy(fontSize = 13.sp),
                        color = TextSecondary
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Remote focusable "Manage Profiles" / "Done" button
                        TvFocusableCard(
                            onClick = { isManageMode = !isManageMode },
                            shape = RectangleShape,
                            focusedScale = 1.025f,
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
                                        color = if (isFocused) Color.Transparent else BorderHairline,
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

                        // Remote focusable "Sign Out" button
                        if (!isManageMode) {
                            TvFocusableCard(
                                onClick = { showSignOutDialog = true },
                                shape = RectangleShape,
                                focusedScale = 1.025f,
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
                                            color = if (isFocused) Color.Transparent else BorderHairline,
                                            shape = RectangleShape
                                        )
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = if (isGuest) "Exit Guest" else "Sign Out",
                                        tint = if (isFocused) FocusWhite else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isGuest) "Exit Guest" else "Sign Out",
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
    }

        if (showSignOutDialog) {
            SignOutConfirmationDialog(
                isGuest = isGuest,
                onConfirm = {
                    showSignOutDialog = false
                    viewModel.signOut(onSignOut)
                },
                onDismiss = {
                    showSignOutDialog = false
                }
            )
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
            focusedScale = 1.025f,
            focusedBorderWidth = 1.5.dp,
            focusedBorderColor = FocusWhite,
            modifier = modifier
        ) { isFocused ->
            Box(
                modifier = Modifier.size(110.dp),
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
        if (isSelected && !isManageMode) {
            Text(
                text = "ACTIVE",
                style = ErasmusTvTypography.Badge.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = RatingGold
                )
            )
        }
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
            focusedScale = 1.025f,
            focusedBorderWidth = 1.5.dp,
            focusedBorderColor = FocusWhite
        ) { isFocused ->
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        if (isFocused) SurfaceCard else SurfaceDark,
                        RectangleShape
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

@Composable
private fun SignOutConfirmationDialog(
    isGuest: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(60)
        try {
            cancelFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, BorderHairline, RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .focusProperties {
                    onExit = { FocusRequester.Cancel }
                }
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = RatingGold,
                    modifier = Modifier.size(42.dp)
                )

                Text(
                    text = if (isGuest) "Exit Guest Session?" else "Sign Out?",
                    style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 22.sp),
                    color = TextPrimary
                )

                Text(
                    text = if (isGuest) {
                        "Are you sure you want to exit guest mode? You will return to the sign-in screen."
                    } else {
                        "Are you sure you want to sign out of your Erasmus account? You can sign in with another account at any time."
                    },
                    style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvFocusableCard(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(6.dp),
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = FocusWhite,
                        modifier = Modifier.focusRequester(cancelFocusRequester)
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) SurfaceCard else SurfacePill, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isFocused) Color.Transparent else BorderHairline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text("Cancel", style = ErasmusTvTypography.ButtonText, color = TextPrimary)
                        }
                    }

                    TvFocusableCard(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(6.dp),
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = FocusWhite
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) Color(0xFFFF453A) else Color(0xCCFF453A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isGuest) "Exit" else "Sign Out",
                                style = ErasmusTvTypography.ButtonText,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}


