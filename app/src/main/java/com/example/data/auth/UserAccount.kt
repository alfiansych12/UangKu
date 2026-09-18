package com.example.data.auth

data class UserAccount(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val isGoogleUser: Boolean = true,
    val lastLoginMillis: Long = System.currentTimeMillis()
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: UserAccount) : AuthState
    data class Error(val message: String) : AuthState
}
