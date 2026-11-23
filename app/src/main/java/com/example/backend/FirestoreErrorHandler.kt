package com.example.backend

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val currentUser = Firebase.auth.currentUser

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

fun Throwable.toArabicFriendlyMessage(): String {
  val firestoreError =
    generateSequence(this) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
  return when (firestoreError?.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
      "لا تملك صلاحية تنفيذ هذا الإجراء حالياً."
    FirebaseFirestoreException.Code.UNAVAILABLE ->
      "تعذر الاتصال بالخادم، يرجى التحقق من الإنترنت."
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
      "تم تحديث هذه الرحلة مسبقاً، يرجى تحديث الشاشة."
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
      "تم تجاوز حد الاستخدام المؤقت، يرجى المحاولة لاحقاً."
    else -> localizedMessage?.takeIf { it.isNotBlank() } ?: "حدث خطأ غير متوقع، يرجى المحاولة مرة أخرى."
  }
}
