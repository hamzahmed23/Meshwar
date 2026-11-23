package com.example.presentation.splash

import com.example.core.config.PaymentMethod
import com.example.core.config.ServiceCity
import com.example.domain.model.VehicleCategory

sealed interface SplashUiState {
  data object Initializing : SplashUiState

  data class Ready(
    val launchCity: ServiceCity,
    val supportedVehicles: List<VehicleCategory>,
    val paymentMethods: List<PaymentMethod>,
  ) : SplashUiState
}
