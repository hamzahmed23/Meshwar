package com.example.domain.model

enum class UserRole(val id: String) {
  PASSENGER("passenger"),
  DRIVER("driver"),
  ADMIN("admin"),
}

enum class OperatingMode {
  PASSENGER_MODE,
  DRIVER_MODE,
}

enum class DriverApprovalStatus(val id: String) {
  NOT_APPLIED("not_applied"),
  PENDING_REVIEW("pending_review"),
  APPROVED("approved"),
  REJECTED("rejected"),
  SUSPENDED("suspended");

  fun canReceiveRideRequests(): Boolean = this == APPROVED
}
