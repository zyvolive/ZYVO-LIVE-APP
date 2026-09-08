package com.example.zyvo.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zyvo.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing Firebase Authentication state and user profile operations.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Loading
        )

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        // Automatically check/restore persistent Firebase session upon initialization
        viewModelScope.launch {
            authRepository.checkSession()
        }
    }

    fun signInWithEmail(email: String, password: String, onSuccess: ((User) -> Unit)? = null) {
        if (email.isBlank() || password.isBlank()) {
            _actionError.value = "Email and password cannot be empty."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null
            val result = authRepository.signInWithEmail(email.trim(), password)
            _isSubmitting.value = false
            result.onSuccess { user ->
                onSuccess?.invoke(user)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Sign in failed"
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        username: String,
        avatar: String = "👑",
        onSuccess: ((User) -> Unit)? = null
    ) {
        if (email.isBlank() || password.isBlank()) {
            _actionError.value = "Email and password are required."
            return
        }
        if (password.length < 6) {
            _actionError.value = "Password must be at least 6 characters long."
            return
        }
        if (displayName.isBlank()) {
            _actionError.value = "Please provide a display name."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null
            val result = authRepository.signUpWithEmail(
                email = email.trim(),
                password = password,
                displayName = displayName.trim(),
                username = username.ifBlank { displayName.lowercase().replace(" ", "_") },
                avatar = avatar
            )
            _isSubmitting.value = false
            result.onSuccess { user ->
                onSuccess?.invoke(user)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Sign up failed"
            }
        }
    }

    fun signInAnonymously(
        displayName: String = "ZYVO Broadcaster",
        username: String = "broadcaster",
        avatar: String = "🚀",
        onSuccess: ((User) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _actionError.value = null
            val result = authRepository.signInAnonymously(
                displayName = displayName,
                username = username,
                avatar = avatar
            )
            _isSubmitting.value = false
            result.onSuccess { user ->
                onSuccess?.invoke(user)
            }.onFailure { error ->
                _actionError.value = error.message ?: "Quick sign-in failed"
            }
        }
    }

    fun updateUserProfile(updatedUser: User, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = authRepository.updateUserProfile(updatedUser)
            _isSubmitting.value = false
            result.onSuccess {
                onComplete?.invoke(true)
            }.onFailure { e ->
                _actionError.value = e.message ?: "Failed to update profile"
                onComplete?.invoke(false)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _actionError.value = null
    }

    fun clearError() {
        _actionError.value = null
    }
}
