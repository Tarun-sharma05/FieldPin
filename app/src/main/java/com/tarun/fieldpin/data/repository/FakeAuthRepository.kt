package com.tarun.fieldpin.data.repository

import android.content.Context
import com.tarun.fieldpin.data.local.AppDatabase
import com.tarun.fieldpin.data.session.SessionStore
import com.tarun.fieldpin.domain.model.User
import com.tarun.fieldpin.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class FakeAuthRepository(
    private val sessionStore: SessionStore,
    private val appDatabase: AppDatabase? = null,
    private val context: Context? = null
) : AuthRepository {

    override fun observeCurrentUser(): Flow<User?> {
        return sessionStore.userFlow
    }

    override suspend fun login(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
        }

        if (!trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (trimmedEmail.equals("officer@fieldpin.com", ignoreCase = true) && trimmedPassword == "password123") {
            val user = User(
                id = "user_officer_01",
                email = trimmedEmail,
                name = "Field Officer",
                token = "fake_jwt_token_officer_01"
            )
            sessionStore.saveSession(user)
            return Result.success(user)
        }

        return Result.failure(IllegalArgumentException("Invalid email or password"))
    }

    override suspend fun logout() {
        sessionStore.clearSession()
        withContext(Dispatchers.IO) {
            appDatabase?.clearAllTables()
            context?.let { ctx ->
                val photoDir = File(ctx.filesDir, "photos")
                if (photoDir.exists()) {
                    photoDir.deleteRecursively()
                }
            }
        }
    }
}
