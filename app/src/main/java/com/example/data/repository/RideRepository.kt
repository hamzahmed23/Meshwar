package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.backend.OperationType
import com.example.backend.handleFirestoreError
import com.example.core.config.LaunchConfig
import com.example.data.model.DriverProfile
import com.example.data.model.Ride
import com.example.data.model.RideContact
import com.example.data.model.RideOffer
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RideRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

  fun observeUserRides(userId: String): Flow<List<Ride>> = flow {
    require(userId.isNotBlank())
    val path = "rides"
    emitAll(
      db.collection(path)
        .whereArrayContains("participantUids", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Ride::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeSearchingRides(vehicleCategory: VehicleCategory? = null): Flow<List<Ride>> = flow {
    requireUserId()
    val path = "rides"
    emitAll(
      db.collection(path)
        .whereEqualTo("status", RideStatus.SEARCHING.wireValue)
        .whereEqualTo("cityId", LaunchConfig.initialCity.id)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(Ride::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { vehicleCategory == null || it.vehicleCategory == vehicleCategory.id }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeRideOffers(rideId: String, passengerId: String): Flow<List<RideOffer>> = flow {
    require(rideId.isNotBlank())
    val uid = requireUserId()
    val path = "rides/$rideId/offers"
    emitAll(
      db.collection("rides")
        .document(rideId)
        .collection("offers")
        .whereEqualTo("passengerId", passengerId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(RideOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.offeredFareEgp }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeRideContact(rideId: String): Flow<RideContact?> = flow {
    require(rideId.isNotBlank())
    val uid = requireUserId()
    val path = "rides/$rideId/private/contact"
    emitAll(
      db.collection("rides")
        .document(rideId)
        .collection("private")
        .document("contact")
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(RideContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  suspend fun createRideRequest(
    passengerName: String,
    passengerPhone: String,
    pickupAddress: String,
    pickupLat: Double,
    pickupLng: Double,
    destinationAddress: String,
    destinationLat: Double,
    destinationLng: Double,
    distanceKm: Double,
    vehicleCategory: VehicleCategory,
    proposedFareEgp: Double,
  ): Result<String> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val rideRef = db.collection("rides").document()
    return try {
      require(pickupAddress.trim().length >= 2) { "يرجى تحديد نقطة الانطلاق" }
      require(destinationAddress.trim().length >= 2) { "يرجى تحديد الوجهة" }
      require(proposedFareEgp >= vehicleCategory.minFareEgp && proposedFareEgp <= 10000.0) {
        "الحد الأدنى للأجرة لهذه الفئة ${vehicleCategory.minFareEgp.toInt()} ج.م"
      }

      val ridePayload =
        mapOf(
          "passengerId" to uid,
          "passengerName" to passengerName.trim().take(80),
          "cityId" to LaunchConfig.initialCity.id,
          "pickupAddress" to pickupAddress.trim().take(200),
          "pickupLat" to pickupLat,
          "pickupLng" to pickupLng,
          "destinationAddress" to destinationAddress.trim().take(200),
          "destinationLat" to destinationLat,
          "destinationLng" to destinationLng,
          "distanceKm" to distanceKm.coerceIn(0.1, 500.0),
          "vehicleCategory" to vehicleCategory.id,
          "paymentMethod" to "cash",
          "proposedFareEgp" to proposedFareEgp,
          "status" to RideStatus.SEARCHING.wireValue,
          "participantUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      val contactPayload =
        mapOf(
          "passengerId" to uid,
          "passengerPhone" to passengerPhone.trim().take(20),
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      val historyRef = rideRef.collection("status_history").document()
      val historyPayload =
        mapOf(
          "rideId" to rideRef.id,
          "fromStatus" to RideStatus.REQUESTED.wireValue,
          "toStatus" to RideStatus.SEARCHING.wireValue,
          "actorId" to uid,
          "participantUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
        )

      db.runBatch { batch ->
        batch.set(rideRef, ridePayload)
        batch.set(rideRef.collection("private").document("contact"), contactPayload)
        batch.set(historyRef, historyPayload)
      }.await()

      Result.success(rideRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, rideRef.path)
      Result.failure(e)
    }
  }

  suspend fun submitDriverOffer(
    ride: Ride,
    driverProfile: DriverProfile,
    offeredFareEgp: Double,
    isCounterOffer: Boolean,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val offerRef =
      db.collection("rides").document(ride.id).collection("offers").document()
    return try {
      require(driverProfile.resolvedApprovalStatus().canReceiveRideRequests()) {
        "حساب السائق بانتظار موافقة الإدارة"
      }
      require(offeredFareEgp >= 10.0 && offeredFareEgp <= 10000.0) {
        "قيمة العرض غير صالحة"
      }

      val payload =
        mapOf(
          "rideId" to ride.id,
          "passengerId" to ride.passengerId,
          "driverId" to uid,
          "driverName" to driverProfile.fullName,
          "driverRating" to driverProfile.ratingAvg,
          "vehicleCategory" to driverProfile.vehicleCategory,
          "vehicleSummary" to "${driverProfile.vehicleModel} • ${driverProfile.vehicleColor}".take(100),
          "vehiclePlate" to driverProfile.vehiclePlate,
          "offeredFareEgp" to offeredFareEgp,
          "counterOffer" to isCounterOffer,
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      offerRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, offerRef.path)
      Result.failure(e)
    }
  }

  suspend fun rejectOffer(rideId: String, offerId: String): Result<Unit> {
    val offerRef = db.collection("rides").document(rideId).collection("offers").document(offerId)
    return try {
      requireUserId()
      offerRef
        .update(
          mapOf(
            "status" to "rejected",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, offerRef.path)
      Result.failure(e)
    }
  }

  /**
   * Atomic offer acceptance inside a Firestore Transaction to guarantee
   * that only one driver can ever win a ride.
   */
  suspend fun acceptOfferAtomically(rideId: String, offer: RideOffer): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val rideRef = db.collection("rides").document(rideId)
    val offerRef = rideRef.collection("offers").document(offer.id)
    val historyRef = rideRef.collection("status_history").document()
    return try {
      db.runTransaction { tx ->
        val rideSnap = tx.get(rideRef)
        val currentStatus = rideSnap.getString("status")
        val existingDriver = rideSnap.getString("assignedDriverId")
        val passengerId = rideSnap.getString("passengerId")
        check(passengerId == uid) { "غير مصرح بقبول العرض" }
        check(currentStatus == RideStatus.SEARCHING.wireValue && existingDriver == null) {
          "تم اختيار سائق لهذه الرحلة بالفعل"
        }

        tx.update(
          rideRef,
          mapOf(
            "status" to RideStatus.OFFER_SELECTED.wireValue,
            "assignedDriverId" to offer.driverId,
            "assignedDriverName" to offer.driverName,
            "assignedDriverVehicle" to offer.vehicleSummary,
            "assignedDriverPlate" to offer.vehiclePlate,
            "assignedDriverRating" to offer.driverRating,
            "agreedFareEgp" to offer.offeredFareEgp,
            "participantUids" to listOf(uid, offer.driverId),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.update(
          offerRef,
          mapOf(
            "status" to "accepted",
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          historyRef,
          mapOf(
            "rideId" to rideId,
            "fromStatus" to RideStatus.SEARCHING.wireValue,
            "toStatus" to RideStatus.OFFER_SELECTED.wireValue,
            "actorId" to uid,
            "participantUids" to listOf(uid, offer.driverId),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, rideRef.path)
      Result.failure(e)
    }
  }

  suspend fun attachDriverContactToRide(rideId: String, driverPhone: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val contactRef = db.collection("rides").document(rideId).collection("private").document("contact")
    return try {
      contactRef
        .update(
          mapOf(
            "driverId" to uid,
            "driverPhone" to driverPhone.trim().take(20),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, contactRef.path)
      Result.failure(e)
    }
  }

  suspend fun advanceRideStatus(
    ride: Ride,
    targetStatus: RideStatus,
    driverLat: Double? = null,
    driverLng: Double? = null,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val currentStatus = ride.resolvedStatus()
    if (!currentStatus.canTransitionTo(targetStatus)) {
      return Result.failure(IllegalStateException("انتقال حالة غير مسموح من $currentStatus إلى $targetStatus"))
    }
    val rideRef = db.collection("rides").document(ride.id)
    val historyRef = rideRef.collection("status_history").document()
    return try {
      val updates =
        mutableMapOf<String, Any?>(
          "status" to targetStatus.wireValue,
          "driverLat" to driverLat,
          "driverLng" to driverLng,
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      db.runBatch { batch ->
        batch.update(rideRef, updates)
        batch.set(
          historyRef,
          mapOf(
            "rideId" to ride.id,
            "fromStatus" to currentStatus.wireValue,
            "toStatus" to targetStatus.wireValue,
            "actorId" to uid,
            "participantUids" to ride.participantUids,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, rideRef.path)
      Result.failure(e)
    }
  }

  /**
   * Atomically completes the ride, calculates platform commission from `config/commission_rules`,
   * updates driver financial totals, and records a FinancialTransaction entry.
   */
  suspend fun completeRideAndRecordCommission(ride: Ride): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val rideRef = db.collection("rides").document(ride.id)
    val configRef = db.collection("config").document("commission_rules")
    val driverRef = db.collection("driver_profiles").document(ride.assignedDriverId ?: uid)
    val txRef = db.collection("financial_transactions").document("tx_${ride.id}")
    val historyRef = rideRef.collection("status_history").document()

    return try {
      db.runTransaction { tx ->
        val rideSnap = tx.get(rideRef)
        val statusStr = rideSnap.getString("status") ?: ""
        check(statusStr == RideStatus.IN_PROGRESS.wireValue) { "الرحلة ليست قيد التنفيذ حالياً" }

        val configSnap = tx.get(configRef)
        val ratePercent = (configSnap.getDouble("commissionRatePercent") ?: 10.0).coerceIn(0.0, 50.0)
        val grossFare = rideSnap.getDouble("agreedFareEgp") ?: rideSnap.getDouble("proposedFareEgp") ?: 0.0
        val commissionEgp = ((grossFare * ratePercent / 100.0) * 100.0).roundToInt() / 100.0
        val netEgp = ((grossFare - commissionEgp) * 100.0).roundToInt() / 100.0

        val driverSnap = tx.get(driverRef)
        val prevCount = driverSnap.getLong("completedRidesCount") ?: 0L
        val prevGross = driverSnap.getDouble("totalGrossEgp") ?: 0.0
        val prevComm = driverSnap.getDouble("totalCommissionEgp") ?: 0.0
        val prevNet = driverSnap.getDouble("totalNetEgp") ?: 0.0

        tx.update(
          rideRef,
          mapOf(
            "status" to RideStatus.COMPLETED.wireValue,
            "commissionRatePercent" to ratePercent,
            "commissionAmountEgp" to commissionEgp,
            "driverNetEgp" to netEgp,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.update(
          driverRef,
          mapOf(
            "completedRidesCount" to (prevCount + 1L),
            "totalGrossEgp" to (prevGross + grossFare),
            "totalCommissionEgp" to (prevComm + commissionEgp),
            "totalNetEgp" to (prevNet + netEgp),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          txRef,
          mapOf(
            "rideId" to ride.id,
            "driverId" to (ride.assignedDriverId ?: uid),
            "passengerId" to ride.passengerId,
            "cityId" to LaunchConfig.initialCity.id,
            "grossFareEgp" to grossFare,
            "commissionRatePercent" to ratePercent,
            "commissionAmountEgp" to commissionEgp,
            "driverNetEgp" to netEgp,
            "paymentMethod" to "cash",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          historyRef,
          mapOf(
            "rideId" to ride.id,
            "fromStatus" to RideStatus.IN_PROGRESS.wireValue,
            "toStatus" to RideStatus.COMPLETED.wireValue,
            "actorId" to uid,
            "participantUids" to ride.participantUids,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, rideRef.path)
      Result.failure(e)
    }
  }

  suspend fun rateCompletedRide(
    ride: Ride,
    rating: Int,
    feedback: String,
  ): Result<Unit> {
    val rideRef = db.collection("rides").document(ride.id)
    val driverId = ride.assignedDriverId
    return try {
      requireUserId()
      require(rating in 1..5) { "التقييم يجب أن يكون من 1 إلى 5" }
      val trimmedFeedback = feedback.trim().take(400)

      db.runTransaction { tx ->
        val driverSnap =
          if (!driverId.isNullOrBlank()) tx.get(db.collection("driver_profiles").document(driverId))
          else null

        val rideUpdates =
          mutableMapOf<String, Any>(
            "status" to RideStatus.COMPLETED.wireValue,
            "passengerRating" to rating.toLong(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (trimmedFeedback.isNotBlank()) {
          rideUpdates["passengerFeedback"] = trimmedFeedback
        }
        tx.update(rideRef, rideUpdates)

        if (driverSnap != null && driverSnap.exists()) {
          val oldAvg = driverSnap.getDouble("ratingAvg") ?: 5.0
          val oldCount = driverSnap.getLong("ratingCount") ?: 0L
          val newCount = oldCount + 1L
          val newAvg = (((oldAvg * oldCount) + rating) / newCount * 10.0).roundToInt() / 10.0
          tx.update(
            driverSnap.reference,
            mapOf(
              "ratingAvg" to newAvg.coerceIn(1.0, 5.0),
              "ratingCount" to newCount,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, rideRef.path)
      Result.failure(e)
    }
  }

  suspend fun submitComplaint(
    rideId: String,
    reporterName: String,
    reason: String,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val docRef = db.collection("complaints").document()
    return try {
      val trimmedReason = reason.trim()
      require(trimmedReason.length in 5..400) { "يرجى كتابة تفاصيل الشكوى (5 أحرف على الأقل)" }
      docRef
        .set(
          mapOf(
            "rideId" to rideId,
            "reporterId" to uid,
            "reporterName" to reporterName.trim().take(80),
            "reason" to trimmedReason,
            "status" to "open",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
