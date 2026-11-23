package com.example.domain.model

/**
 * Controlled ride lifecycle state machine.
 * Enforces valid transitions before backend RPC validation in later phases.
 */
enum class RideStatus(val wireValue: String) {
  REQUESTED("requested"),
  SEARCHING("searching"),
  OFFER_SELECTED("offer_selected"),
  DRIVER_ARRIVING("driver_arriving"),
  DRIVER_ARRIVED("driver_arrived"),
  IN_PROGRESS("in_progress"),
  COMPLETED("completed"),
  CANCELLED("cancelled"),
  DISPUTED("disputed");

  fun allowedNextStatuses(): Set<RideStatus> =
    when (this) {
      REQUESTED -> setOf(SEARCHING, CANCELLED)
      SEARCHING -> setOf(OFFER_SELECTED, CANCELLED)
      OFFER_SELECTED -> setOf(DRIVER_ARRIVING, CANCELLED, DISPUTED)
      DRIVER_ARRIVING -> setOf(DRIVER_ARRIVED, CANCELLED, DISPUTED)
      DRIVER_ARRIVED -> setOf(IN_PROGRESS, CANCELLED, DISPUTED)
      IN_PROGRESS -> setOf(COMPLETED, DISPUTED)
      COMPLETED -> setOf(DISPUTED)
      CANCELLED -> emptySet()
      DISPUTED -> emptySet()
    }

  fun canTransitionTo(next: RideStatus): Boolean = next in allowedNextStatuses()
}
