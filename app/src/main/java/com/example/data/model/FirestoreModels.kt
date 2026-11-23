package com.example.data.model

import androidx.annotation.Keep
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.OperatingMode
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val fullName: String = "",
  val phone: String? = null,
  val email: String? = null,
  val photoBase64: String? = null,
  val activeMode: String = "passenger",
  val roles: List<String> = listOf("passenger"),
  val ratingAvg: Double = 5.0,
  val ratingCount: Long = 0L,
  val completedRidesCount: Long = 0L,
  val suspended: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedMode(): OperatingMode =
    when (activeMode) {
      "driver" -> OperatingMode.DRIVER_MODE
      else -> OperatingMode.PASSENGER_MODE
    }
}

@Keep
@IgnoreExtraProperties
data class SavedFavoritePlace(
  @DocumentId val id: String = "",
  val userId: String = "",
  val placeKey: String = "home",
  val label: String = "",
  val address: String = "",
  val latitude: Double = 27.5561,
  val longitude: Double = 30.8075,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DriverProfile(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val fullName: String = "",
  val phone: String = "",
  val nationalId: String = "",
  val vehicleCategory: String = VehicleCategory.TUK_TUK.id,
  val vehicleModel: String = "",
  val vehicleColor: String = "",
  val vehiclePlate: String = "",
  val profilePhotoBase64: String? = null,
  val nationalIdPhotoBase64: String? = null,
  val licensePhotoBase64: String? = null,
  val approvalStatus: String = DriverApprovalStatus.PENDING_REVIEW.id,
  val rejectionReason: String? = null,
  val online: Boolean = false,
  val completedRidesCount: Long = 0L,
  val totalGrossEgp: Double = 0.0,
  val totalCommissionEgp: Double = 0.0,
  val totalNetEgp: Double = 0.0,
  val ratingAvg: Double = 5.0,
  val ratingCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedApprovalStatus(): DriverApprovalStatus =
    DriverApprovalStatus.entries.firstOrNull { it.id == approvalStatus }
      ?: DriverApprovalStatus.PENDING_REVIEW

  fun resolvedVehicleCategory(): VehicleCategory =
    VehicleCategory.entries.firstOrNull { it.id == vehicleCategory }
      ?: VehicleCategory.TUK_TUK
}

@Keep
@IgnoreExtraProperties
data class DriverPresence(
  @DocumentId val id: String = "",
  val driverId: String = "",
  val cityId: String = "dayrout",
  val vehicleCategory: String = VehicleCategory.TUK_TUK.id,
  val online: Boolean = false,
  val latitude: Double = 27.5561,
  val longitude: Double = 30.8075,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Ride(
  @DocumentId val id: String = "",
  val passengerId: String = "",
  val passengerName: String = "",
  val cityId: String = "dayrout",
  val pickupAddress: String = "",
  val pickupLat: Double = 27.5561,
  val pickupLng: Double = 30.8075,
  val destinationAddress: String = "",
  val destinationLat: Double = 27.5590,
  val destinationLng: Double = 30.8120,
  val distanceKm: Double = 0.0,
  val vehicleCategory: String = VehicleCategory.TUK_TUK.id,
  val paymentMethod: String = "cash",
  val proposedFareEgp: Double = 0.0,
  val agreedFareEgp: Double? = null,
  val status: String = RideStatus.SEARCHING.wireValue,
  val assignedDriverId: String? = null,
  val assignedDriverName: String? = null,
  val assignedDriverVehicle: String? = null,
  val assignedDriverPlate: String? = null,
  val assignedDriverRating: Double? = null,
  val driverLat: Double? = null,
  val driverLng: Double? = null,
  val commissionRatePercent: Double? = null,
  val commissionAmountEgp: Double? = null,
  val driverNetEgp: Double? = null,
  val passengerRating: Long? = null,
  val passengerFeedback: String? = null,
  val participantUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedStatus(): RideStatus =
    RideStatus.entries.firstOrNull { it.wireValue == status } ?: RideStatus.SEARCHING

  fun resolvedVehicleCategory(): VehicleCategory =
    VehicleCategory.entries.firstOrNull { it.id == vehicleCategory } ?: VehicleCategory.TUK_TUK
}

@Keep
@IgnoreExtraProperties
data class RideOffer(
  @DocumentId val id: String = "",
  val rideId: String = "",
  val passengerId: String = "",
  val driverId: String = "",
  val driverName: String = "",
  val driverRating: Double = 5.0,
  val vehicleCategory: String = VehicleCategory.TUK_TUK.id,
  val vehicleSummary: String = "",
  val vehiclePlate: String = "",
  val offeredFareEgp: Double = 0.0,
  val counterOffer: Boolean = false,
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedVehicleCategory(): VehicleCategory =
    VehicleCategory.entries.firstOrNull { it.id == vehicleCategory } ?: VehicleCategory.TUK_TUK
}

@Keep
@IgnoreExtraProperties
data class RideContact(
  @DocumentId val id: String = "",
  val passengerId: String = "",
  val passengerPhone: String = "",
  val driverId: String? = null,
  val driverPhone: String? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FinancialTransaction(
  @DocumentId val id: String = "",
  val rideId: String = "",
  val driverId: String = "",
  val passengerId: String = "",
  val cityId: String = "dayrout",
  val grossFareEgp: Double = 0.0,
  val commissionRatePercent: Double = 10.0,
  val commissionAmountEgp: Double = 0.0,
  val driverNetEgp: Double = 0.0,
  val paymentMethod: String = "cash",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CommissionRule(
  @DocumentId val id: String = "",
  val commissionRatePercent: Double = 10.0,
  val updatedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Complaint(
  @DocumentId val id: String = "",
  val rideId: String = "",
  val reporterId: String = "",
  val reporterName: String = "",
  val reason: String = "",
  val status: String = "open",
  val adminNote: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
  @DocumentId val id: String = "",
  val adminId: String = "",
  val adminEmail: String = "",
  val action: String = "",
  val targetId: String = "",
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
