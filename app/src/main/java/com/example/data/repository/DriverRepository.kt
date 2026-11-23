package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.backend.OperationType
import com.example.backend.handleFirestoreError
import com.example.core.config.LaunchConfig
import com.example.data.model.DriverPresence
import com.example.data.model.DriverProfile
import com.example.data.model.FinancialTransaction
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.VehicleCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DriverRepository(
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

  fun observeDriverProfile(driverId: String): Flow<DriverProfile?> = flow {
    require(driverId.isNotBlank())
    val path = "driver_profiles/$driverId"
    emitAll(
      db.collection("driver_profiles")
        .document(driverId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
          if (exceptionToLog != null && auth.currentUser?.uid == driverId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeOnlineDrivers(): Flow<List<DriverPresence>> = flow {
    requireUserId()
    val path = "driver_presence"
    emitAll(
      db.collection(path)
        .whereEqualTo("online", true)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(DriverPresence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

  fun observeDriverTransactions(driverId: String): Flow<List<FinancialTransaction>> = flow {
    require(driverId.isNotBlank())
    val path = "financial_transactions"
    emitAll(
      db.collection(path)
        .whereEqualTo("driverId", driverId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == driverId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun submitDriverApplication(
    fullName: String,
    phone: String,
    nationalId: String,
    vehicleCategory: VehicleCategory,
    vehicleModel: String,
    vehicleColor: String,
    vehiclePlate: String,
    profilePhotoBase64: String?,
    nationalIdPhotoBase64: String?,
    licensePhotoBase64: String?,
    isResubmit: Boolean,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val path = "driver_profiles/$uid"
    return try {
      val name = fullName.trim()
      val phoneTrim = phone.trim()
      val nid = nationalId.trim()
      val model = vehicleModel.trim()
      val color = vehicleColor.trim()
      val plate = vehiclePlate.trim()

      require(name.length in 2..80) { "الاسم الكامل مطلوب (2-80 حرف)" }
      require(phoneTrim.length in 8..20) { "رقم الهاتف غير صالح" }
      require(nid.length == 14 && nid.all { it.isDigit() }) { "الرقم القومي يجب أن يكون 14 رقماً" }
      require(model.length in 2..60) { "موديل المركبة مطلوب" }
      require(color.length in 2..30) { "لون المركبة مطلوب" }
      require(plate.length in 3..20) { "رقم اللوحة مطلوب" }

      if (isResubmit) {
        val updates =
          mutableMapOf<String, Any?>(
            "fullName" to name,
            "phone" to phoneTrim,
            "nationalId" to nid,
            "vehicleCategory" to vehicleCategory.id,
            "vehicleModel" to model,
            "vehicleColor" to color,
            "vehiclePlate" to plate,
            "profilePhotoBase64" to profilePhotoBase64?.take(180_000),
            "nationalIdPhotoBase64" to nationalIdPhotoBase64?.take(180_000),
            "licensePhotoBase64" to licensePhotoBase64?.take(180_000),
            "approvalStatus" to DriverApprovalStatus.PENDING_REVIEW.id,
            "online" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        db.collection("driver_profiles").document(uid).update(updates).await()
      } else {
        val payload =
          mapOf(
            "driverId" to uid,
            "fullName" to name,
            "phone" to phoneTrim,
            "nationalId" to nid,
            "vehicleCategory" to vehicleCategory.id,
            "vehicleModel" to model,
            "vehicleColor" to color,
            "vehiclePlate" to plate,
            "profilePhotoBase64" to profilePhotoBase64?.take(180_000),
            "nationalIdPhotoBase64" to nationalIdPhotoBase64?.take(180_000),
            "licensePhotoBase64" to licensePhotoBase64?.take(180_000),
            "approvalStatus" to DriverApprovalStatus.PENDING_REVIEW.id,
            "online" to false,
            "completedRidesCount" to 0L,
            "totalGrossEgp" to 0.0,
            "totalCommissionEgp" to 0.0,
            "totalNetEgp" to 0.0,
            "ratingAvg" to 5.0,
            "ratingCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        db.collection("driver_profiles").document(uid).set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun setDriverOnlineStatus(
    online: Boolean,
    vehicleCategory: VehicleCategory,
    latitude: Double,
    longitude: Double,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val profileRef = db.collection("driver_profiles").document(uid)
    val presenceRef = db.collection("driver_presence").document(uid)
    return try {
      db.runBatch { batch ->
        batch.update(
          profileRef,
          mapOf(
            "online" to online,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          presenceRef,
          mapOf(
            "driverId" to uid,
            "cityId" to LaunchConfig.initialCity.id,
            "vehicleCategory" to vehicleCategory.id,
            "online" to online,
            "latitude" to latitude,
            "longitude" to longitude,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "driver_profiles/$uid")
      Result.failure(e)
    }
  }
}
