package com.example.taskmate.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.example.taskmate.data.repository.AuthRepository
import com.example.taskmate.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthViewModel.kt
 * Location: ui/auth/AuthViewModel.kt
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<FirebaseUser>>()
    val authState: LiveData<Resource<FirebaseUser>> = _authState

    private val _resetEmailState = MutableLiveData<Resource<Unit>>()
    val resetEmailState: LiveData<Resource<Unit>> = _resetEmailState

    val isUserLoggedIn: Boolean get() = authRepository.isLoggedIn

    // ── Register ───────────────────────────────────────────────────
    fun registerWithEmail(name: String, email: String, password: String) {
        when {
            name.isBlank() -> {
                _authState.value = Resource.Error("Name is required")
                return
            }
            email.isBlank() -> {
                _authState.value = Resource.Error("Email is required")
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _authState.value = Resource.Error("Enter a valid email address")
                return
            }
            password.length < 6 -> {
                _authState.value = Resource.Error("Password must be at least 6 characters")
                return
            }
        }
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.registerWithEmail(name, email, password)
        }
    }

    // ── Login ──────────────────────────────────────────────────────
    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = Resource.Error("Email and password are required")
            return
        }
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.loginWithEmail(email, password)
        }
    }

    // ── Google ─────────────────────────────────────────────────────
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.loginWithGoogle(idToken)
        }
    }

    // ── Reset password ─────────────────────────────────────────────
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _resetEmailState.value = Resource.Error("Enter your email address")
            return
        }
        viewModelScope.launch {
            _resetEmailState.value = Resource.Loading
            _resetEmailState.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    // ── Sign out ───────────────────────────────────────────────────
    fun signOut() {
        authRepository.signOut()
        _authState.value = null
    }
}