package com.erasmustv.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun setError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    fun login(onSuccess: () -> Unit) {
        val trimmedEmail = _email.value.trim()
        val rawPassword = _password.value

        if (trimmedEmail.isBlank() && rawPassword.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email and password.")
            return
        }
        if (trimmedEmail.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email address.")
            return
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _uiState.value = LoginUiState.Error("Please enter a valid email address.")
            return
        }
        if (rawPassword.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(trimmedEmail, rawPassword).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Unable to sign in. Please verify your credentials."
                    )
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.loginWithGoogle(idToken).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Google Sign-In failed. Please try again."
                    )
                }
            )
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.continueAsGuest().fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Unable to start guest session."
                    )
                }
            )
        }
    }
}

