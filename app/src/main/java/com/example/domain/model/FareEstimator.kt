package com.example.domain.model

import kotlin.math.ceil
import kotlin.math.roundToInt

data class FareEstimate(
  val recommendedMinEgp: Int,
  val recommendedMaxEgp: Int,
  val suggestedEgp: Int,
  val estimatedMinutes: Int,
)

enum class FareFairnessLevel {
  EXCELLENT,
  FAIR,
  LOW,
}

object FareEstimator {
  fun estimate(category: VehicleCategory, distanceKm: Double): FareEstimate {
    val safeKm = distanceKm.coerceIn(0.3, 120.0)
    val (baseFare, perKm, avgSpeedKmh) =
      when (category) {
        VehicleCategory.TUK_TUK -> Triple(8.0, 5.5, 22.0)
        VehicleCategory.MOTORCYCLE -> Triple(7.0, 4.5, 32.0)
        VehicleCategory.CAR -> Triple(15.0, 9.0, 36.0)
      }

    val raw = (baseFare + safeKm * perKm).coerceAtLeast(category.minFareEgp)
    val suggested = roundToNearest5(raw).coerceAtLeast(category.minFareEgp.toInt())
    val minRec = (suggested - 5).coerceAtLeast(category.minFareEgp.toInt())
    val maxRec = (suggested + 10).coerceAtLeast(minRec + 5)
    val minutes = ceil((safeKm / avgSpeedKmh) * 60.0).toInt().coerceAtLeast(3)

    return FareEstimate(
      recommendedMinEgp = minRec,
      recommendedMaxEgp = maxRec,
      suggestedEgp = suggested,
      estimatedMinutes = minutes,
    )
  }

  fun evaluateProposedFare(proposedEgp: Double, estimate: FareEstimate): FareFairnessLevel =
    when {
      proposedEgp >= estimate.suggestedEgp -> FareFairnessLevel.EXCELLENT
      proposedEgp >= estimate.recommendedMinEgp -> FareFairnessLevel.FAIR
      else -> FareFairnessLevel.LOW
    }

  private fun roundToNearest5(value: Double): Int = ((value / 5.0).roundToInt() * 5).coerceAtLeast(5)
}
