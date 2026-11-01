package com.tarun.fieldpin.ui.login

data class LoginUiState(
    val email: String = "officer@fieldpin.com",
    val password: String = "password123",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
