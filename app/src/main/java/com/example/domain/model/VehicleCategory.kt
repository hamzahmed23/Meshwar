package com.example.domain.model

enum class VehicleCategory(
  val id: String,
  val minFareEgp: Double,
  val maxPassengers: Int,
) {
  TUK_TUK(id = "tuk_tuk", minFareEgp = 10.0, maxPassengers = 3),
  MOTORCYCLE(id = "motorcycle", minFareEgp = 10.0, maxPassengers = 1),
  CAR(id = "car", minFareEgp = 20.0, maxPassengers = 4);

  fun validateProposedFare(amountEgp: Double): FareValidationResult {
    if (amountEgp.isNaN() || amountEgp.isInfinite() || amountEgp <= 0.0) {
      return FareValidationResult.InvalidAmount
    }
    if (amountEgp < minFareEgp) {
      return FareValidationResult.BelowMinimum(minFareEgp)
    }
    return FareValidationResult.Valid(amountEgp)
  }
}

sealed interface FareValidationResult {
  data class Valid(val amountEgp: Double) : FareValidationResult

  data class BelowMinimum(val minimumEgp: Double) : FareValidationResult

  data object InvalidAmount : FareValidationResult
}
