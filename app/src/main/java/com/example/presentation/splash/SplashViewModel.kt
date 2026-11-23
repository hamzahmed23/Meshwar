package com.example.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ConfigRepository
import com.example.data.repository.LocalLaunchConfigRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
  private val configRepository: ConfigRepository = LocalLaunchConfigRepository(),
  private val splashInitDelayMs: Long = 850L,
) : ViewModel() {

  private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Initializing)
  val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

  init {
    initializeBrandSession()
  }

  private fun initializeBrandSession() {
    viewModelScope.launch {
      if (splashInitDelayMs > 0L) {
        delay(splashInitDelayMs)
      }
      _uiState.value =
        SplashUiState.Ready(
          launchCity = configRepository.getLaunchCity(),
          supportedVehicles = configRepository.getSupportedVehicleCategories(),
          paymentMethods = configRepository.getSupportedPaymentMethods(),
        )
    }
  }
}
