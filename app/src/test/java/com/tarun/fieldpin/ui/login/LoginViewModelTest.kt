package com.tarun.fieldpin.ui.login

import com.tarun.fieldpin.domain.model.User
import com.tarun.fieldpin.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestAuthRepository : AuthRepository {
    private val currentUser = MutableStateFlow<User?>(null)
    var shouldSucceed = true

    override fun observeCurrentUser(): Flow<User?> = currentUser

    override suspend fun login(email: String, password: String): Result<User> {
        return if (shouldSucceed) {
            val user = User("1", email, "Officer", "token")
            currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid credentials"))
        }
    }

    override suspend fun logout() {
        currentUser.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: TestAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = TestAuthRepository()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiStateHasDefaultDemoCredentials() {
        val state = viewModel.uiState.value
        assertEquals("officer@fieldpin.com", state.email)
        assertEquals("password123", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun emptyFieldsShowValidationErrors() {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Email is required", state.emailError)
        assertEquals("Password is required", state.passwordError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun invalidEmailFormatShowsValidationError() {
        viewModel.onEmailChanged("invalidemail")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Enter a valid email address", state.emailError)
    }

    @Test
    fun successfulLoginUpdatesUiStateToSuccess() = runTest {
        authRepository.shouldSucceed = true
        viewModel.onEmailChanged("officer@fieldpin.com")
        viewModel.onPasswordChanged("password123")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun failedLoginSetsErrorMessage() = runTest {
        authRepository.shouldSucceed = false
        viewModel.onEmailChanged("officer@fieldpin.com")
        viewModel.onPasswordChanged("wrongpass")

        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
}
