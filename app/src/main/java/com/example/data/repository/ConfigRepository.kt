package com.example.data.repository

import com.example.core.config.LaunchConfig
import com.example.core.config.PaymentMethod
import com.example.core.config.ServiceCity
import com.example.domain.model.VehicleCategory

interface ConfigRepository {
  fun getLaunchCity(): ServiceCity

  fun getSupportedVehicleCategories(): List<VehicleCategory>

  fun getSupportedPaymentMethods(): List<PaymentMethod>
}

class LocalLaunchConfigRepository : ConfigRepository {
  override fun getLaunchCity(): ServiceCity = LaunchConfig.initialCity

  override fun getSupportedVehicleCategories(): List<VehicleCategory> =
    listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR)

  override fun getSupportedPaymentMethods(): List<PaymentMethod> =
    LaunchConfig.initialPaymentMethods
}
