package com.construccionia.app.auth

import android.app.Application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockAuthRepository = mockk<AuthRepository>()

    private lateinit var viewModel: AuthViewModel
    private val authStateCallback = slot<(AuthState) -> Unit>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Capturar el callback que AuthViewModel registra en onAuthStateChanged
        every {
            mockAuthRepository.onAuthStateChanged(capture(authStateCallback))
        } just runs

        viewModel = AuthViewModel(mockApplication, mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Estado inicial ──

    @Test
    fun `authState starts as Loading`() = runTest {
        assertEquals(
            "authState should start as Loading",
            AuthState.Loading,
            viewModel.authState.value
        )
    }

    @Test
    fun `isProcessing starts as false`() = runTest {
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun `authState changes to Unauthenticated when repo callback fires`() = runTest {
        // Simular que el repositorio emite Unauthenticated
        authStateCallback.captured.invoke(AuthState.Unauthenticated)

        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun `authState changes to Authenticated when repo callback fires`() = runTest {
        val testUser = AuthUser(
            uid = "user123",
            displayName = "Test User",
            email = "test@example.com",
            photoUrl = null
        )
        authStateCallback.captured.invoke(AuthState.Authenticated(testUser))

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("user123", (state as AuthState.Authenticated).user.uid)
        assertEquals("Test User", state.user.displayName)
        assertEquals("test@example.com", state.user.email)
    }

    // ── signInWithEmail ──

    @Test
    fun `signInWithEmail sets isProcessing true then false`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.success(
            AuthUser("uid1", "User", "user@test.com", null)
        )

        viewModel.signInWithEmail("user@test.com", "password123")
        assertTrue("isProcessing should be true during sign in", viewModel.isProcessing.value)

        advanceUntilIdle()
        assertFalse("isProcessing should be false after sign in", viewModel.isProcessing.value)
    }

    @Test
    fun `signInWithEmail calls repository with correct credentials`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.success(
            AuthUser("uid1", "User", "user@test.com", null)
        )

        viewModel.signInWithEmail("user@example.com", "mypassword")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockAuthRepository.signInWithEmail("user@example.com", "mypassword")
        }
    }

    @Test
    fun `signInWithEmail on success does not set error state`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.success(
            AuthUser("uid2", "User", "u@test.com", null)
        )

        // Estado inicial después de simular Unauthenticated
        authStateCallback.captured.invoke(AuthState.Unauthenticated)

        viewModel.signInWithEmail("u@test.com", "pass")
        advanceUntilIdle()

        // El estado NO debe ser Error (sigue siendo Unauthenticated hasta que el listener cambie)
        assertFalse(viewModel.authState.value is AuthState.Error)
    }

    @Test
    fun `signInWithEmail on failure sets error state`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.failure(
            Exception("Invalid credentials")
        )

        viewModel.signInWithEmail("user@test.com", "wrong")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Should be Error state on failure", state is AuthState.Error)
        assertEquals("Invalid credentials", (state as AuthState.Error).message)
    }

    @Test
    fun `signInWithEmail handles empty email`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.failure(
            Exception("Email cannot be empty")
        )

        viewModel.signInWithEmail("", "pass")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Email cannot be empty", (state as AuthState.Error).message)
    }

    @Test
    fun `signInWithEmail handles network error`() = runTest {
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.failure(
            Exception("Network error: No internet connection")
        )

        viewModel.signInWithEmail("user@test.com", "pass")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertTrue((state as AuthState.Error).message.contains("Network error"))
    }

    // ── registerWithEmail ──

    @Test
    fun `registerWithEmail calls repository`() = runTest {
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } returns Result.success(
            AuthUser("uid3", "New", "new@test.com", null)
        )

        viewModel.registerWithEmail("new@test.com", "newpassword")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockAuthRepository.registerWithEmail("new@test.com", "newpassword")
        }
    }

    @Test
    fun `registerWithEmail sets error on failure`() = runTest {
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } returns Result.failure(
            Exception("Email already in use")
        )

        viewModel.registerWithEmail("existing@test.com", "pass")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Email already in use", (state as AuthState.Error).message)
    }

    @Test
    fun `registerWithEmail manages processing state`() = runTest {
        coEvery { mockAuthRepository.registerWithEmail(any(), any()) } returns Result.success(
            AuthUser("u", "U", "u@t.com", null)
        )

        viewModel.registerWithEmail("u@t.com", "pass")
        assertTrue(viewModel.isProcessing.value)

        advanceUntilIdle()
        assertFalse(viewModel.isProcessing.value)
    }

    // ── signInWithGoogle ──

    @Test
    fun `signInWithGoogle calls repository with idToken`() = runTest {
        coEvery { mockAuthRepository.signInWithGoogle(any()) } returns Result.success(
            AuthUser("gid", "Google User", "g@google.com", null)
        )

        viewModel.signInWithGoogle("google-id-token-123")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockAuthRepository.signInWithGoogle("google-id-token-123")
        }
    }

    @Test
    fun `signInWithGoogle sets error on failure`() = runTest {
        coEvery { mockAuthRepository.signInWithGoogle(any()) } returns Result.failure(
            Exception("Google sign-in failed")
        )

        viewModel.signInWithGoogle("bad-token")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Google sign-in failed", (state as AuthState.Error).message)
    }

    // ── signOut ──

    @Test
    fun `signOut calls repository signOut`() = runTest {
        every { mockAuthRepository.signOut() } just runs

        viewModel.signOut()

        verify(exactly = 1) { mockAuthRepository.signOut() }
    }

    // ── sendPasswordReset ──

    @Test
    fun `sendPasswordReset calls repository`() = runTest {
        coEvery { mockAuthRepository.sendPasswordReset(any()) } returns Result.success(Unit)

        viewModel.sendPasswordReset("user@test.com")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            mockAuthRepository.sendPasswordReset("user@test.com")
        }
    }

    @Test
    fun `sendPasswordReset on success sets Unauthenticated`() = runTest {
        coEvery { mockAuthRepository.sendPasswordReset(any()) } returns Result.success(Unit)

        viewModel.sendPasswordReset("user@test.com")
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun `sendPasswordReset on failure sets error`() = runTest {
        coEvery { mockAuthRepository.sendPasswordReset(any()) } returns Result.failure(
            Exception("Email not found")
        )

        viewModel.sendPasswordReset("nonexistent@test.com")
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Error)
        assertEquals("Email not found", (state as AuthState.Error).message)
    }

    // ── clearError ──

    @Test
    fun `clearError resets Error to Unauthenticated`() = runTest {
        // Provocar un error
        coEvery { mockAuthRepository.signInWithEmail(any(), any()) } returns Result.failure(
            Exception("Error")
        )
        viewModel.signInWithEmail("test", "test")
        advanceUntilIdle()
        assertTrue(viewModel.authState.value is AuthState.Error)

        viewModel.clearError()
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun `clearError does nothing when state is not Error`() = runTest {
        authStateCallback.captured.invoke(AuthState.Unauthenticated)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)

        viewModel.clearError()
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    // ── AuthState sealed class ──

    @Test
    fun `AuthState sealed class variants`() = runTest {
        assertTrue(AuthState.Loading is AuthState)
        assertTrue(AuthState.Unauthenticated is AuthState)
        assertTrue(AuthState.Authenticated(AuthUser("u", "n", "e", null)) is AuthState)
        assertTrue(AuthState.Error("msg") is AuthState)
    }

    @Test
    fun `AuthState Authenticated contains user data`() = runTest {
        val user = AuthUser("uid", "Name", "email@test.com", null)
        val state = AuthState.Authenticated(user)

        assertEquals("uid", state.user.uid)
        assertEquals("Name", state.user.displayName)
        assertEquals("email@test.com", state.user.email)
    }

    @Test
    fun `AuthState Error contains message`() = runTest {
        val state = AuthState.Error("Something went wrong")
        assertEquals("Something went wrong", state.message)
    }

    // ── AuthUser data class ──

    @Test
    fun `AuthUser data class creation`() = runTest {
        val user = AuthUser(
            uid = "uid123",
            displayName = "Test User",
            email = "test@example.com",
            photoUrl = null
        )

        assertEquals("uid123", user.uid)
        assertEquals("Test User", user.displayName)
        assertEquals("test@example.com", user.email)
        assertEquals(null, user.photoUrl)
    }

    @Test
    fun `AuthUser with photoUrl`() = runTest {
        val uri = android.net.Uri.parse("https://example.com/photo.jpg")
        val user = AuthUser("u", "U", "e@e.com", uri)
        assertEquals(uri, user.photoUrl)
    }
}
