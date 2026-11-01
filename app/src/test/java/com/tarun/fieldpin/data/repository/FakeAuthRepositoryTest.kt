package com.tarun.fieldpin.data.repository

import com.tarun.fieldpin.data.session.SessionStore
import com.tarun.fieldpin.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeSessionStore : SessionStore {
    private val _userState = MutableStateFlow<User?>(null)
    override val userFlow: Flow<User?> = _userState

    override suspend fun saveSession(user: User) {
        _userState.value = user
    }

    override suspend fun clearSession() {
        _userState.value = null
    }
}

class FakeAuthRepositoryTest {

    @Test
    fun loginWithValidCredentialsSucceedsAndSavesSession() = runTest {
        val sessionStore = FakeSessionStore()
        val repository = FakeAuthRepository(sessionStore = sessionStore)

        val result = repository.login("officer@fieldpin.com", "password123")

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertEquals("user_officer_01", user?.id)
        assertEquals("officer@fieldpin.com", user?.email)

        val savedUser = sessionStore.userFlow.first()
        assertEquals(user, savedUser)
    }

    @Test
    fun loginWithInvalidCredentialsFails() = runTest {
        val sessionStore = FakeSessionStore()
        val repository = FakeAuthRepository(sessionStore = sessionStore)

        val result = repository.login("officer@fieldpin.com", "wrongpassword")

        assertTrue(result.isFailure)
        assertNull(sessionStore.userFlow.first())
    }

    @Test
    fun loginWithEmptyCredentialsFails() = runTest {
        val sessionStore = FakeSessionStore()
        val repository = FakeAuthRepository(sessionStore = sessionStore)

        val result = repository.login("", "")

        assertTrue(result.isFailure)
        assertNull(sessionStore.userFlow.first())
    }

    @Test
    fun logoutClearsSession() = runTest {
        val sessionStore = FakeSessionStore()
        val repository = FakeAuthRepository(sessionStore = sessionStore)

        repository.login("officer@fieldpin.com", "password123")
        assertEquals("officer@fieldpin.com", sessionStore.userFlow.first()?.email)

        repository.logout()
        assertNull(sessionStore.userFlow.first())
    }
}
