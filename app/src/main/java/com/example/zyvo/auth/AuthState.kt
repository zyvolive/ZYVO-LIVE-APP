package com.example.zyvo.auth

import com.example.zyvo.data.model.User

/**
 * Represents the authentication state of the ZYVO application.
 */
sealed interface AuthState {
    /** Auth state is being resolved or a network request is in flight */
    data object Loading : AuthState

    /** No authenticated Firebase session exists */
    data object Unauthenticated : AuthState

    /** Active authenticated session with persistent Firebase UID and profile */
    data class Authenticated(val user: User) : AuthState

    /** Authentication failure or credential error */
    data class Error(val message: String) : AuthState
}
