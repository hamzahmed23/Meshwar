package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.backend.OperationType
import com.example.backend.handleFirestoreError
import com.example.data.model.AdminAuditLog
import com.example.data.model.CommissionRule
import com.example.data.model.Complaint
import com.example.data.model.DriverProfile
import com.example.data.model.FinancialTransaction
import com.example.data.model.Ride
import com.example.data.model.UserProfile
import com.example.domain.model.DriverApprovalStatus
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

class AdminRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireAdminUser() =
    auth.currentUser ?: throw IllegalStateException("Admin must be signed in.")

  fun observeAllUsers(): Flow<List<UserProfile>> = observeCollection("users") {
    it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
  }

  fun observeAllDrivers(): Flow<List<DriverProfile>> = observeCollection("driver_profiles") {
    it.toObjects(DriverProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
  }

  fun observeAllRides(): Flow<List<Ride>> = observeCollection("rides") {
    it.toObjects(Ride::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      .sortedByDescending { ride -> ride.createdAt?.seconds ?: 0L }
  }

  fun observeAllComplaints(): Flow<List<Complaint>> = observeCollection("complaints") {
    it.toObjects(Complaint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      .sortedByDescending { c -> c.createdAt?.seconds ?: 0L }
  }

  fun observeAllTransactions(): Flow<List<FinancialTransaction>> =
    observeCollection("financial_transactions") {
      it.toObjects(
        FinancialTransaction::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      ).sortedByDescending { tx -> tx.createdAt?.seconds ?: 0L }
    }

  fun observeAuditLogs(): Flow<List<AdminAuditLog>> = observeCollection("admin_audit_logs") {
    it.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      .sortedByDescending { log -> log.createdAt?.seconds ?: 0L }
  }

  fun observeCommissionRule(): Flow<CommissionRule> = flow {
    requireAdminUser()
    val docRef = db.collection("config").document("commission_rules")
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(CommissionRule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?: CommissionRule()
          } else {
            CommissionRule()
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
          }
          throw error
        }
    )
  }

  private fun <T> observeCollection(
    collectionPath: String,
    mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>,
  ): Flow<List<T>> = flow {
    requireAdminUser()
    emitAll(
      db.collection(collectionPath)
        .snapshots()
        .map(mapper)
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, collectionPath)
          }
          throw error
        }
    )
  }

  suspend fun reviewDriverApplication(
    driverId: String,
    newStatus: DriverApprovalStatus,
    rejectionReason: String? = null,
  ): Result<Unit> {
    val admin = auth.currentUser ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val driverRef = db.collection("driver_profiles").document(driverId)
    val logRef = db.collection("admin_audit_logs").document()
    return try {
      val updates =
        mutableMapOf<String, Any?>(
          "approvalStatus" to newStatus.id,
          "online" to if (newStatus == DriverApprovalStatus.APPROVED) null else false,
          "rejectionReason" to rejectionReason?.trim()?.take(300)?.takeIf { it.isNotBlank() },
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      db.runBatch { batch ->
        batch.update(driverRef, updates)
        batch.set(
          logRef,
          mapOf(
            "adminId" to admin.uid,
            "adminEmail" to (admin.email ?: "admin"),
            "action" to "driver_${newStatus.id}",
            "targetId" to driverId,
            "details" to "تغيير حالة السائق إلى ${newStatus.id}",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, driverRef.path)
      Result.failure(e)
    }
  }

  suspend fun setUserSuspended(userId: String, suspended: Boolean): Result<Unit> {
    val admin = auth.currentUser ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val userRef = db.collection("users").document(userId)
    val logRef = db.collection("admin_audit_logs").document()
    return try {
      db.runBatch { batch ->
        batch.update(
          userRef,
          mapOf(
            "suspended" to suspended,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          logRef,
          mapOf(
            "adminId" to admin.uid,
            "adminEmail" to (admin.email ?: "admin"),
            "action" to if (suspended) "suspend_user" else "reactivate_user",
            "targetId" to userId,
            "details" to if (suspended) "إيقاف حساب المستخدم" else "إعادة تفعيل حساب المستخدم",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateCommissionRate(ratePercent: Double): Result<Unit> {
    val admin = auth.currentUser ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val configRef = db.collection("config").document("commission_rules")
    val logRef = db.collection("admin_audit_logs").document()
    return try {
      require(ratePercent in 0.0..50.0) { "نسبة العمولة يجب أن تكون بين 0% و 50%" }
      db.runBatch { batch ->
        batch.set(
          configRef,
          mapOf(
            "commissionRatePercent" to ratePercent,
            "updatedBy" to admin.uid,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          logRef,
          mapOf(
            "adminId" to admin.uid,
            "adminEmail" to (admin.email ?: "admin"),
            "action" to "update_commission",
            "targetId" to "commission_rules",
            "details" to "تحديث نسبة العمولة إلى $ratePercent%",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, configRef.path)
      Result.failure(e)
    }
  }

  suspend fun resolveComplaint(complaintId: String, status: String, note: String): Result<Unit> {
    val admin = auth.currentUser ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val ref = db.collection("complaints").document(complaintId)
    val logRef = db.collection("admin_audit_logs").document()
    return try {
      require(status in listOf("resolved", "dismissed"))
      db.runBatch { batch ->
        batch.update(
          ref,
          mapOf(
            "status" to status,
            "adminNote" to note.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          logRef,
          mapOf(
            "adminId" to admin.uid,
            "adminEmail" to (admin.email ?: "admin"),
            "action" to "complaint_$status",
            "targetId" to complaintId,
            "details" to "معالجة الشكوى: $status",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      Result.failure(e)
    }
  }
}
