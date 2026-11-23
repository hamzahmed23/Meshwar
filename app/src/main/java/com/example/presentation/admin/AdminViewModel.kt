package com.example.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.toArabicFriendlyMessage
import com.example.data.model.AdminAuditLog
import com.example.data.model.CommissionRule
import com.example.data.model.Complaint
import com.example.data.model.DriverProfile
import com.example.data.model.FinancialTransaction
import com.example.data.model.Ride
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.domain.model.DriverApprovalStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminActionState(
  val isBusy: Boolean = false,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

class AdminViewModel(
  private val adminRepository: AdminRepository,
) : ViewModel() {

  private val _actionState = MutableStateFlow(AdminActionState())
  val actionState: StateFlow<AdminActionState> = _actionState.asStateFlow()

  val users: StateFlow<List<UserProfile>> =
    adminRepository
      .observeAllUsers()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val drivers: StateFlow<List<DriverProfile>> =
    adminRepository
      .observeAllDrivers()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val rides: StateFlow<List<Ride>> =
    adminRepository
      .observeAllRides()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val complaints: StateFlow<List<Complaint>> =
    adminRepository
      .observeAllComplaints()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val transactions: StateFlow<List<FinancialTransaction>> =
    adminRepository
      .observeAllTransactions()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val auditLogs: StateFlow<List<AdminAuditLog>> =
    adminRepository
      .observeAuditLogs()
      .withAdminFallback(emptyList())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val commissionRule: StateFlow<CommissionRule> =
    adminRepository
      .observeCommissionRule()
      .withAdminFallback(CommissionRule())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CommissionRule())

  private fun <T> kotlinx.coroutines.flow.Flow<T>.withAdminFallback(
    fallback: T,
  ): kotlinx.coroutines.flow.Flow<T> =
    catch { error ->
      val firestoreError =
        generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      _actionState.update { it.copy(errorMessage = (firestoreError ?: error).toArabicFriendlyMessage()) }
      emit(fallback)
    }

  fun approveDriver(driverId: String) {
    runAdminAction("تم اعتماد حساب السائق بنجاح") {
      adminRepository.reviewDriverApplication(driverId, DriverApprovalStatus.APPROVED)
    }
  }

  fun rejectDriver(driverId: String, reason: String) {
    runAdminAction("تم رفض طلب السائق") {
      adminRepository.reviewDriverApplication(driverId, DriverApprovalStatus.REJECTED, reason)
    }
  }

  fun suspendDriver(driverId: String, reason: String) {
    runAdminAction("تم إيقاف حساب السائق") {
      adminRepository.reviewDriverApplication(driverId, DriverApprovalStatus.SUSPENDED, reason)
    }
  }

  fun toggleUserSuspended(userId: String, suspended: Boolean) {
    runAdminAction(if (suspended) "تم إيقاف المستخدم" else "تم إعادة تفعيل المستخدم") {
      adminRepository.setUserSuspended(userId, suspended)
    }
  }

  fun updateCommissionRate(ratePercent: Double) {
    runAdminAction("تم تحديث نسبة العمولة إلى $ratePercent%") {
      adminRepository.updateCommissionRate(ratePercent)
    }
  }

  fun resolveComplaint(complaintId: String, status: String, note: String) {
    runAdminAction("تم تحديث حالة الشكوى") {
      adminRepository.resolveComplaint(complaintId, status, note)
    }
  }

  private fun runAdminAction(successMsg: String, block: suspend () -> Result<Unit>) {
    _actionState.update { it.copy(isBusy = true, errorMessage = null) }
    viewModelScope.launch {
      block()
        .onSuccess { _actionState.update { it.copy(isBusy = false, successMessage = successMsg) } }
        .onFailure { err ->
          _actionState.update { it.copy(isBusy = false, errorMessage = err.toArabicFriendlyMessage()) }
        }
    }
  }

  fun clearMessages() {
    _actionState.update { it.copy(errorMessage = null, successMessage = null) }
  }
}
