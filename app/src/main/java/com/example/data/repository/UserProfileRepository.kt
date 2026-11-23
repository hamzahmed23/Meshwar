package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.backend.OperationType
import com.example.backend.handleFirestoreError
import com.example.data.model.SavedFavoritePlace
import com.example.data.model.UserProfile
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
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

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeSavedPlaces(userId: String): Flow<List<SavedFavoritePlace>> = flow {
    require(userId.isNotBlank()) { "userId must not be blank" }
    val path = "users/$userId/saved_places"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("saved_places")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(SavedFavoritePlace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
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

  suspend fun saveFavoritePlace(
    placeKey: String,
    label: String,
    address: String,
    latitude: Double,
    longitude: Double,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val safeKey =
      placeKey
        .trim()
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .take(64)
        .ifBlank { "custom_${System.currentTimeMillis()}" }
    val docRef = db.collection("users").document(uid).collection("saved_places").document(safeKey)
    return try {
      val cleanLabel = label.trim().take(60)
      val cleanAddress = address.trim().take(200)
      require(cleanLabel.isNotEmpty()) { "يرجى إدخال اسم للمكان" }
      require(cleanAddress.length >= 2) { "العنوان غير صالح" }
      require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "إحداثيات غير صالحة" }

      db.runTransaction { tx ->
        val existing = tx.get(docRef)
        if (existing.exists()) {
          tx.update(
            docRef,
            mapOf(
              "placeKey" to safeKey,
              "label" to cleanLabel,
              "address" to cleanAddress,
              "latitude" to latitude,
              "longitude" to longitude,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          tx.set(
            docRef,
            mapOf(
              "userId" to uid,
              "placeKey" to safeKey,
              "label" to cleanLabel,
              "address" to cleanAddress,
              "latitude" to latitude,
              "longitude" to longitude,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteFavoritePlace(placeId: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val docRef = db.collection("users").document(uid).collection("saved_places").document(placeId)
    return try {
      require(placeId.isNotBlank())
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createProfile(
    fullName: String,
    phone: String,
    email: String?,
    photoBase64: String?,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val path = "users/$uid"
    return try {
      val trimmedName = fullName.trim()
      val trimmedPhone = phone.trim()
      require(trimmedName.length in 2..80) { "الاسم يجب أن يكون بين حرفين و 80 حرفاً" }
      require(trimmedPhone.length in 8..20) { "يرجى إدخال رقم هاتف مصري صحيح" }

      val payload =
        mapOf(
          "userId" to uid,
          "fullName" to trimmedName,
          "phone" to trimmedPhone,
          "email" to email?.take(120)?.takeIf { it.isNotBlank() },
          "photoBase64" to photoBase64?.take(180_000)?.takeIf { it.isNotBlank() },
          "activeMode" to "passenger",
          "roles" to listOf("passenger"),
          "ratingAvg" to 5.0,
          "ratingCount" to 0L,
          "completedRidesCount" to 0L,
          "suspended" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }

      db.collection("users").document(uid).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateProfile(
    fullName: String,
    phone: String,
    photoBase64: String?,
  ): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val path = "users/$uid"
    return try {
      requireUserId()
      val trimmedName = fullName.trim()
      val trimmedPhone = phone.trim()
      require(trimmedName.length in 2..80) { "الاسم يجب أن يكون بين حرفين و 80 حرفاً" }
      require(trimmedPhone.length in 8..20) { "يرجى إدخال رقم هاتف مصري صحيح" }

      val updates =
        mutableMapOf<String, Any>(
          "fullName" to trimmedName,
          "phone" to trimmedPhone,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!photoBase64.isNullOrBlank()) {
        updates["photoBase64"] = photoBase64.take(180_000)
      }
      db.collection("users").document(uid).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun switchMode(targetMode: String, currentRoles: List<String>): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("غير مسجل الدخول"))
    val path = "users/$uid"
    return try {
      require(targetMode in listOf("passenger", "driver")) { "وضع تشغيل غير صالح" }
      val nextRoles =
        (currentRoles.filter { it in listOf("passenger", "driver") } + targetMode).distinct()
      db.collection("users")
        .document(uid)
        .update(
          mapOf(
            "activeMode" to targetMode,
            "roles" to nextRoles,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }
}
