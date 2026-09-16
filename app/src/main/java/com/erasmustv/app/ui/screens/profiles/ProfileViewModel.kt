package com.erasmustv.app.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.AuthRepository
import com.erasmustv.app.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profiles: List<WatchProfile>, val activeProfile: WatchProfile?) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val userEmail: StateFlow<String?> = authRepository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isGuest: StateFlow<Boolean> = authRepository.isGuestFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            // Proactively verify / refresh token if expired
            authRepository.checkSession()
            profileRepository.getProfiles().fold(
                onSuccess = { profiles ->
                    val active = profileRepository.getActiveProfile() ?: profiles.firstOrNull()
                    if (profiles.isEmpty()) {
                        createDefaultProfile()
                    } else {
                        _uiState.value = ProfileUiState.Success(profiles, active)
                    }
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(
                        error.message ?: "Unable to load profiles. Please try again."
                    )
                }
            )
        }
    }

    fun selectProfile(profile: WatchProfile, onSelected: () -> Unit) {
        viewModelScope.launch {
            profileRepository.selectProfile(profile)
            onSelected()
        }
    }

    fun createProfile(name: String, avatarKey: String, birthYear: Int) {
        viewModelScope.launch {
            profileRepository.createProfile(name, avatarKey, birthYear)
            loadProfiles()
        }
    }

    fun updateProfile(profile: WatchProfile, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
            loadProfiles()
            onComplete?.invoke()
        }
    }

    fun deleteProfile(profileId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            loadProfiles()
            onComplete?.invoke()
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            profileRepository.clearActiveProfile()
            authRepository.logout()
            onSignedOut()
        }
    }

    private fun createDefaultProfile() {
        viewModelScope.launch {
            profileRepository.createProfile("Profile 1", "crimson", 2000)
            loadProfiles()
        }
    }
}

