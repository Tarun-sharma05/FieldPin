package com.tarun.fieldpin.domain.repository

import com.tarun.fieldpin.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
}
