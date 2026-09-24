package com.erasmustv.app.ui.screens.profiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusShapes
import com.erasmustv.app.core.theme.ErasmusSpacing
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.SurfaceCard
import com.erasmustv.app.core.theme.SurfaceDark
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TextSecondary
import com.erasmustv.app.data.model.ProfileAvatarRegistry
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.ui.components.ProfileAvatarView
import com.erasmustv.app.ui.components.TvFocusableCard

@Composable
fun EditProfileScreen(
    profile: WatchProfile,
    onSave: (WatchProfile) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    var profileName by remember { mutableStateOf(profile.name) }
    var currentAvatarKey by remember { mutableStateOf(profile.avatarKey) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    fun commitAndExit() {
        // Persist only if something actually changed. Opening the editor and
        // leaving without edits should not fire a Supabase write + profiles reload.
        if (profileName != profile.name || currentAvatarKey != profile.avatarKey) {
            onSave(profile.copy(name = profileName, avatarKey = currentAvatarKey))
        }
        onBack()
    }

    BackHandler {
        commitAndExit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Top Bar matching Netflix Reference Image 2
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Header: Back button + Title & Subtitle + Rename action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium)
                    ) {
                        TvFocusableCard(
                            onClick = { commitAndExit() },
                            shape = CircleShape,
                            focusedScale = 1.0f,
                            focusedBorderWidth = 1.5.dp,
                            focusedBorderColor = Color.White,
                            contentDescription = "Back",
                            modifier = Modifier.size(44.dp)
                        ) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (isFocused) SurfaceCard else SurfaceDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = FocusWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Edit Profile",
                                    style = ErasmusTvTypography.PageTitle,
                                    color = TextPrimary
                                )

                                TvFocusableCard(
                                    onClick = { showRenameDialog = true },
                                    shape = ErasmusShapes.Button,
                                    focusedScale = 1.0f,
                                    focusedBorderWidth = 1.5.dp,
                                    focusedBorderColor = Color.White,
                                    contentDescription = "Rename"
                                ) { isFocused ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .background(if (isFocused) SurfaceCard else SurfaceDark, ErasmusShapes.Button)
                                            .padding(horizontal = ErasmusSpacing.MediumSmall, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            tint = if (isFocused) FocusWhite else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Rename",
                                            style = ErasmusTvTypography.Badge.copy(fontSize = 12.sp),
                                            color = if (isFocused) FocusWhite else TextPrimary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Choose a profile icon.",
                                style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                                color = TextMuted
                            )
                        }
                    }

                    // Right Header Preview: Profile Name + Current Avatar preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = profileName,
                            style = ErasmusTvTypography.SectionTitle,
                            color = TextPrimary
                        )

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                        ) {
                            ProfileAvatarView(
                                avatarKey = currentAvatarKey,
                                profileName = profileName,
                                modifier = Modifier.fillMaxSize(),
                                shape = ErasmusShapes.Card,
                                iconSize = 28.dp
                            )
                        }
                    }
                }
            }

            // Categories with horizontal avatar rows (Matching Image 2)
            ProfileAvatarRegistry.categories.forEach { category ->
                item(key = "title_${category.title}") {
                    Text(
                        text = category.title,
                        style = ErasmusTvTypography.HeroTitleLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 48.dp, top = 20.dp, bottom = 10.dp)
                    )
                }

                item(key = "row_${category.title}") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(category.avatars, key = { it.id }) { avatar ->
                            val isSelected = (currentAvatarKey == avatar.fileName)

                            TvFocusableCard(
                                onClick = {
                                    // Preview only — the choice is persisted once on
                                    // exit via commitAndExit(). Previously every tap
                                    // fired a Supabase PATCH + a full profiles GET,
                                    // so browsing avatars sent a burst of redundant
                                    // writes/reads for selections the user discarded.
                                    currentAvatarKey = avatar.fileName
                                },
                                shape = ErasmusShapes.Card,
                                focusedScale = 1.0f,
                                focusedBorderColor = FocusWhite,
                                focusedBorderWidth = 1.5.dp,
                                unfocusedBorderColor = Color.Transparent,
                                contentDescription = avatar.name,
                                modifier = Modifier.size(96.dp)
                            ) { isFocused ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isFocused) SurfaceCard else SurfaceDark, ErasmusShapes.Card)
                                ) {
                                    AsyncImage(
                                        model = avatar.assetUrl,
                                        contentDescription = avatar.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(ErasmusShapes.Card),
                                        onError = { state ->
                                            android.util.Log.e("AvatarError", "Failed to load ${avatar.fileName}: ${state.result.throwable.message}", state.result.throwable)
                                        },
                                        onSuccess = {
                                            android.util.Log.d("AvatarSuccess", "Loaded ${avatar.fileName}")
                                        }
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                                .border(1.dp, FocusWhite, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = FocusWhite,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delete Profile option at the very bottom
            item(key = "delete_profile_section") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TvFocusableCard(
                        onClick = { showDeleteConfirmation = true },
                        shape = ErasmusShapes.Button,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = Color(0xFFFF453A),
                        contentDescription = "Delete Profile"
                    ) { isFocused ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .background(
                                    if (isFocused) Color(0x44FF453A) else Color(0x18FF453A),
                                    ErasmusShapes.Button
                                )
                                .padding(horizontal = 26.dp, vertical = ErasmusSpacing.MediumSmall)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Profile",
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Delete Profile",
                                style = ErasmusTvTypography.ButtonText.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF453A)
                            )
                        }
                    }
                }
            }
        }

        // Rename Dialog
        if (showRenameDialog) {
            RenameProfileDialog(
                initialName = profileName,
                onSave = { newName ->
                    // Preview only — persisted once on exit via commitAndExit(),
                    // so renaming no longer fires its own Supabase PATCH + reload
                    // on top of the exit save.
                    profileName = newName
                    showRenameDialog = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            DeleteProfileConfirmationDialog(
                profileName = profileName,
                onConfirm = {
                    showDeleteConfirmation = false
                    onDelete()
                },
                onDismiss = { showDeleteConfirmation = false }
            )
        }
    }
}

@Composable
private fun RenameProfileDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val textFieldFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .background(Color(0xFF161616), ErasmusShapes.CardLarge)
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Rename Profile",
                    style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 22.sp),
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = ErasmusTvTypography.Body.copy(color = TextPrimary, fontSize = 16.sp),
                    placeholder = { Text("Enter profile name", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) onSave(name.trim())
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFieldFocusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionDown -> {
                                        saveFocusRequester.requestFocus()
                                        true
                                    }
                                    Key.Enter, Key.NumPadEnter -> {
                                        if (name.isNotBlank()) onSave(name.trim())
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FocusWhite,
                        unfocusedBorderColor = BorderHairline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = FocusWhite
                    ),
                    shape = ErasmusShapes.Input
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kept bespoke: both buttons carry their own DirectionUp
                    // handling plus a focus requester driving the text field
                    // above them, and Save inverts fill/label on focus.
                    TvFocusableCard(
                        onClick = onDismiss,
                        shape = ErasmusShapes.Button,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = Color.White,
                        contentDescription = "Cancel",
                        modifier = Modifier
                            .focusRequester(cancelFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                    textFieldFocusRequester.requestFocus()
                                    true
                                } else false
                            }
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) SurfaceCard else SurfaceDark, ErasmusShapes.Button)
                                .padding(horizontal = ErasmusSpacing.Large, vertical = 10.dp)
                        ) {
                            Text("Cancel", style = ErasmusTvTypography.ButtonText, color = TextPrimary)
                        }
                    }

                    TvFocusableCard(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim())
                            }
                        },
                        shape = ErasmusShapes.Button,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = Color.White,
                        contentDescription = "Save",
                        modifier = Modifier
                            .focusRequester(saveFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                    textFieldFocusRequester.requestFocus()
                                    true
                                } else false
                            }
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) FocusWhite else Color(0xFF262626), ErasmusShapes.Button)
                                .padding(horizontal = ErasmusSpacing.Large, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Save",
                                style = ErasmusTvTypography.ButtonText,
                                color = if (isFocused) PitchBlack else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteProfileConfirmationDialog(
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .background(Color(0xFF141414), ErasmusShapes.CardLarge)
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(42.dp)
                )

                Text(
                    text = "Delete Profile?",
                    style = ErasmusTvTypography.BillboardTitle.copy(fontSize = 22.sp),
                    color = TextPrimary
                )

                Text(
                    text = "Are you sure you want to delete '$profileName'? This profile's watch history and preferences will be permanently removed.",
                    style = ErasmusTvTypography.Body.copy(fontSize = 14.sp),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(ErasmusSpacing.Small))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ErasmusSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kept bespoke: paired with a destructive red confirm, which
                    // has no ErasmusButtonStyle equivalent.
                    TvFocusableCard(
                        onClick = onDismiss,
                        shape = ErasmusShapes.Button,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = Color.White,
                        contentDescription = "Cancel",
                        modifier = Modifier.focusRequester(cancelFocusRequester)
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) SurfaceCard else SurfaceDark, ErasmusShapes.Button)
                                .padding(horizontal = ErasmusSpacing.Large, vertical = 10.dp)
                        ) {
                            Text("Cancel", style = ErasmusTvTypography.ButtonText, color = TextPrimary)
                        }
                    }

                    TvFocusableCard(
                        onClick = onConfirm,
                        shape = ErasmusShapes.Button,
                        focusedScale = 1.0f,
                        focusedBorderWidth = 1.5.dp,
                        focusedBorderColor = Color.White,
                        contentDescription = "Delete"
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .background(if (isFocused) Color(0xFFFF453A) else Color(0xCCFF453A), ErasmusShapes.Button)
                                .padding(horizontal = ErasmusSpacing.Large, vertical = 10.dp)
                        ) {
                            Text("Delete", style = ErasmusTvTypography.ButtonText, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
