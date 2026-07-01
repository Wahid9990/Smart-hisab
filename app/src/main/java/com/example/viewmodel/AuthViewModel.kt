package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.UserProfile
import com.example.service.AuthService
import com.example.service.UserResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authService = AuthService(application)
    private val prefs = application.getSharedPreferences("smart_hisab_prefs", Context.MODE_PRIVATE)

    // Active UI language ("en" or "ur")
    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // UI state for authentication processes (Login, Signup, Loading)
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // State holding the currently authenticated user's profile
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Flag indicating whether real Firebase is available or if we are in demo mode
    val isFirebaseAvailable: Boolean = authService.isFirebaseAvailable

    // Navigation events to notify the UI when to navigate
    private val _navigationEvent = MutableSharedFlow<NavigationTarget>()
    val navigationEvent: SharedFlow<NavigationTarget> = _navigationEvent.asSharedFlow()

    init {
        // Check if user is already logged in on application startup
        checkLoginStatus()
    }

    // Set and persist active language
    fun setLanguage(lang: String) {
        viewModelScope.launch {
            _appLanguage.value = lang
            prefs.edit().putString("app_language", lang).apply()
            
            val currentProfile = _userProfile.value
            if (currentProfile != null) {
                // Update on database/local JSON profile as well
                when (val result = authService.updateLanguage(currentProfile.uid, lang)) {
                    is UserResult.Success -> {
                        _userProfile.value = result.profile
                    }
                    is UserResult.Error -> {
                        // Log or handle error gracefully
                    }
                }
            }
        }
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            if (authService.isUserLoggedIn()) {
                val uid = authService.getCurrentUid()
                if (uid != null) {
                    _uiState.value = AuthUiState.Loading
                    when (val result = authService.getUserProfile(uid)) {
                        is UserResult.Success -> {
                            _userProfile.value = result.profile
                            _appLanguage.value = result.profile.language
                            prefs.edit().putString("app_language", result.profile.language).apply()
                            _uiState.value = AuthUiState.Success(result.profile)
                            _navigationEvent.emit(NavigationTarget.Dashboard)
                        }
                        is UserResult.Error -> {
                            // If profile fetch fails, we still consider them logged in but clear if data corrupted
                            _uiState.value = AuthUiState.Idle
                            _navigationEvent.emit(NavigationTarget.Login)
                        }
                    }
                } else {
                    _navigationEvent.emit(NavigationTarget.Login)
                }
            } else {
                _navigationEvent.emit(NavigationTarget.Login)
            }
        }
    }

    // Email/Password login
    fun logIn(email: String, password: String) {
        if (!validateLoginInputs(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authService.logIn(email, password)) {
                is UserResult.Success -> {
                    _userProfile.value = result.profile
                    _appLanguage.value = result.profile.language
                    prefs.edit().putString("app_language", result.profile.language).apply()
                    _uiState.value = AuthUiState.Success(result.profile)
                    _navigationEvent.emit(NavigationTarget.Dashboard)
                }
                is UserResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    // Email/Password signup with profile data
    fun signUp(
        email: String,
        password: String,
        shopName: String,
        ownerName: String,
        phone: String?
    ) {
        if (!validateSignUpInputs(email, password, shopName, ownerName)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val initialLang = _appLanguage.value
            when (val result = authService.signUp(email, password, shopName, ownerName, phone)) {
                is UserResult.Success -> {
                    // Update initial language in profile immediately
                    val finalProfile = if (result.profile.language != initialLang) {
                        when (val updateResult = authService.updateLanguage(result.profile.uid, initialLang)) {
                            is UserResult.Success -> updateResult.profile
                            else -> result.profile
                        }
                    } else {
                        result.profile
                    }
                    _userProfile.value = finalProfile
                    _uiState.value = AuthUiState.Success(finalProfile)
                    _navigationEvent.emit(NavigationTarget.Dashboard)
                }
                is UserResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    // Sign out user and navigate to Login
    fun logOut() {
        viewModelScope.launch {
            authService.logOut()
            _userProfile.value = null
            _uiState.value = AuthUiState.Idle
            _navigationEvent.emit(NavigationTarget.Login)
        }
    }

    // Reset error messages to idle state
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    // Input Validation
    private fun validateLoginInputs(email: String, password: String): Boolean {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Email is required.")
            return false
        }
        if (!isValidEmail(email)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email format.")
            return false
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Password is required.")
            return false
        }
        return true
    }

    private fun validateSignUpInputs(
        email: String,
        password: String,
        shopName: String,
        ownerName: String
    ): Boolean {
        if (shopName.isBlank()) {
            _uiState.value = AuthUiState.Error("Shop Name is required.")
            return false
        }
        if (ownerName.isBlank()) {
            _uiState.value = AuthUiState.Error("Owner Name is required.")
            return false
        }
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Email is required.")
            return false
        }
        if (!isValidEmail(email)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email format.")
            return false
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Password is required.")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return false
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}

// Sealed class for UI state of Auth operations
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val profile: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

// Navigation target routes
sealed interface NavigationTarget {
    object Login : NavigationTarget
    object Dashboard : NavigationTarget
}
