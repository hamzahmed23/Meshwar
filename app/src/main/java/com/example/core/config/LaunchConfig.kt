package com.example.core.config

/**
 * Central application configuration for D-RIDE launch in Dayrout, Assiut Governorate, Egypt.
 * Designed so additional cities, currencies, and payment methods can be enabled without
 * restructuring the core architecture.
 */
object LaunchConfig {
  const val APP_NAME = "D-RIDE"
  const val CURRENCY_CODE = "EGP"
  const val CURRENCY_SYMBOL_AR = "ج.م"
  const val DEFAULT_LOCALE_TAG = "ar-EG"

  val initialCity =
    ServiceCity(
      id = "dayrout",
      nameAr = "ديروط",
      governorateAr = "محافظة أسيوط",
      countryCode = "EG",
      centerLatitude = 27.5561,
      centerLongitude = 30.8075,
      defaultRadiusKm = 15.0,
      isActive = true,
    )

  val initialPaymentMethods: List<PaymentMethod> = listOf(PaymentMethod.CASH)
}

data class ServiceCity(
  val id: String,
  val nameAr: String,
  val governorateAr: String,
  val countryCode: String,
  val centerLatitude: Double,
  val centerLongitude: Double,
  val defaultRadiusKm: Double,
  val isActive: Boolean,
)

enum class PaymentMethod(val id: String, val labelAr: String) {
  CASH(id = "cash", labelAr = "نقدي"),
}
