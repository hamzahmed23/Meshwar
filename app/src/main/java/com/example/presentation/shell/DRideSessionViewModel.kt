package com.example.presentation.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.toArabicFriendlyMessage
import com.example.data.model.DriverProfile
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverRepository
import com.example.data.repository.UserProfileRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionUiState {
  data object Loading : SessionUiState

  data object NeedsOnboarding : SessionUiState

  data class Ready(val userProfile: UserProfile) : SessionUiState

  data class Error(val message: String) : SessionUiState
}

enum class ShellTopMode {
  PASSENGER,
  DRIVER,
  ADMIN,
}

class DRideSessionViewModel(
  private val userProfileRepository: UserProfileRepository,
  private val driverRepository: DriverRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _isSavingOnboarding = MutableStateFlow(false)
  val isSavingOnboarding: StateFlow<Boolean> = _isSavingOnboarding.asStateFlow()

  private val _onboardingError = MutableStateFlow<String?>(null)
  val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()

  private val _selectedTopMode = MutableStateFlow<ShellTopMode?>(null)
  val selectedTopMode: StateFlow<ShellTopMode?> = _selectedTopMode.asStateFlow()

  val isAdmin: Boolean
    get() = AuthRepository.isBootstrappedAdmin(Firebase.auth.currentUser)

  val sessionState: StateFlow<SessionUiState> =
    userProfileRepository
      .observeUserProfile(currentUserId)
      .map { profile ->
        if (profile == null) SessionUiState.NeedsOnboarding
        else SessionUiState.Ready(profile)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(SessionUiState.Error((firestoreError ?: error).toArabicFriendlyMessage()))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SessionUiState.Loading,
      )

  val driverProfile: StateFlow<DriverProfile?> =
    driverRepository
      .observeDriverProfile(currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )

  fun createInitialProfile(
    fullName: String,
    phone: String,
    email: String?,
    photoBase64: String?,
  ) {
    _isSavingOnboarding.value = true
    _onboardingError.value = null
    viewModelScope.launch {
      userProfileRepository
        .createProfile(fullName, phone, email, photoBase64)
        .onSuccess { _isSavingOnboarding.value = false }
        .onFailure { err ->
          _isSavingOnboarding.value = false
          _onboardingError.value = err.toArabicFriendlyMessage()
        }
    }
  }

  fun switchMode(mode: ShellTopMode, currentProfile: UserProfile) {
    _selectedTopMode.value = mode
    if (mode == ShellTopMode.PASSENGER || mode == ShellTopMode.DRIVER) {
      val wireMode = if (mode == ShellTopMode.DRIVER) "driver" else "passenger"
      viewModelScope.launch {
        userProfileRepository.switchMode(wireMode, currentProfile.roles)
      }
    }
  }
}
