package com.example

import com.example.backend.UnconfiguredBackendBoundary
import com.example.core.config.LaunchConfig
import com.example.core.config.PaymentMethod
import com.example.data.repository.LocalLaunchConfigRepository
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.FareValidationResult
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DRideDomainTest {

  @Test
  fun `launch configuration targets Dayrout Egypt with EGP cash`() {
    val repo = LocalLaunchConfigRepository()
    assertEquals("dayrout", repo.getLaunchCity().id)
    assertEquals("ديروط", repo.getLaunchCity().nameAr)
    assertEquals("EGP", LaunchConfig.CURRENCY_CODE)
    assertEquals(listOf(PaymentMethod.CASH), repo.getSupportedPaymentMethods())
    assertEquals(
      listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR),
      repo.getSupportedVehicleCategories(),
    )
    assertFalse(UnconfiguredBackendBoundary.isConfigured)
  }

  @Test
  fun `ride status lifecycle allows valid transitions and blocks invalid ones`() {
    assertTrue(RideStatus.REQUESTED.canTransitionTo(RideStatus.SEARCHING))
    assertTrue(RideStatus.SEARCHING.canTransitionTo(RideStatus.OFFER_SELECTED))
    assertTrue(RideStatus.OFFER_SELECTED.canTransitionTo(RideStatus.DRIVER_ARRIVING))
    assertTrue(RideStatus.DRIVER_ARRIVING.canTransitionTo(RideStatus.DRIVER_ARRIVED))
    assertTrue(RideStatus.DRIVER_ARRIVED.canTransitionTo(RideStatus.IN_PROGRESS))
    assertTrue(RideStatus.IN_PROGRESS.canTransitionTo(RideStatus.COMPLETED))

    assertFalse(RideStatus.COMPLETED.canTransitionTo(RideStatus.SEARCHING))
    assertFalse(RideStatus.CANCELLED.canTransitionTo(RideStatus.IN_PROGRESS))
    assertFalse(RideStatus.REQUESTED.canTransitionTo(RideStatus.COMPLETED))
  }

  @Test
  fun `vehicle fare validation enforces positive EGP minimums`() {
    assertTrue(VehicleCategory.TUK_TUK.validateProposedFare(25.0) is FareValidationResult.Valid)
    assertTrue(VehicleCategory.CAR.validateProposedFare(15.0) is FareValidationResult.BelowMinimum)
    assertTrue(VehicleCategory.MOTORCYCLE.validateProposedFare(-5.0) is FareValidationResult.InvalidAmount)
    assertTrue(VehicleCategory.CAR.validateProposedFare(Double.NaN) is FareValidationResult.InvalidAmount)
  }

  @Test
  fun `only approved drivers can receive ride requests`() {
    assertTrue(DriverApprovalStatus.APPROVED.canReceiveRideRequests())
    assertFalse(DriverApprovalStatus.PENDING_REVIEW.canReceiveRideRequests())
    assertFalse(DriverApprovalStatus.REJECTED.canReceiveRideRequests())
    assertFalse(DriverApprovalStatus.SUSPENDED.canReceiveRideRequests())
    assertFalse(DriverApprovalStatus.NOT_APPLIED.canReceiveRideRequests())
  }

  @Test
  fun `fare estimator computes realistic EGP ranges and fairness levels`() {
    val tukEst = com.example.domain.model.FareEstimator.estimate(VehicleCategory.TUK_TUK, 3.0)
    val carEst = com.example.domain.model.FareEstimator.estimate(VehicleCategory.CAR, 3.0)
    assertTrue(carEst.suggestedEgp > tukEst.suggestedEgp)
    assertTrue(tukEst.recommendedMinEgp >= VehicleCategory.TUK_TUK.minFareEgp.toInt())
    assertTrue(tukEst.estimatedMinutes >= 3)

    assertEquals(
      com.example.domain.model.FareFairnessLevel.EXCELLENT,
      com.example.domain.model.FareEstimator.evaluateProposedFare(tukEst.suggestedEgp.toDouble(), tukEst),
    )
    assertEquals(
      com.example.domain.model.FareFairnessLevel.LOW,
      com.example.domain.model.FareEstimator.evaluateProposedFare(5.0, carEst),
    )
  }
}
