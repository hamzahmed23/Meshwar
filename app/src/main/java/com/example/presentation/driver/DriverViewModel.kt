package com.example.presentation.driver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.toArabicFriendlyMessage
import com.example.core.config.LaunchConfig
import com.example.data.location.DayroutGeoService
import com.example.data.model.DriverProfile
import com.example.data.model.FinancialTransaction
import com.example.data.model.Ride
import com.example.data.model.RideContact
import com.example.data.repository.DriverRepository
import com.example.data.repository.RideRepository
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverActionState(
  val isSubmittingApplication: Boolean = false,
  val isTogglingOnline: Boolean = false,
  val isSubmittingOffer: Boolean = false,
  val isUpdatingRide: Boolean = false,
  val declinedRideIds: Set<String> = emptySet(),
  val offeredRideIds: Set<String> = emptySet(),
  val currentLat: Double = LaunchConfig.initialCity.centerLatitude,
  val currentLng: Double = LaunchConfig.initialCity.centerLongitude,
  val errorMessage: String? = null,
  val successMessage: String? = null,
)

sealed interface DriverProfileUiState {
  data object Loading : DriverProfileUiState

  data class Ready(val profile: DriverProfile?) : DriverProfileUiState

  data class Error(val message: String) : DriverProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriverViewModel(
  private val driverRepository: DriverRepository,
  private val rideRepository: RideRepository,
  private val currentDriverId: String,
) : ViewModel() {

  private val _actionState = MutableStateFlow(DriverActionState())
  val actionState: StateFlow<DriverActionState> = _actionState.asStateFlow()

  val driverProfileState: StateFlow<DriverProfileUiState> =
    driverRepository
      .observeDriverProfile(currentDriverId)
      .map<DriverProfile?, DriverProfileUiState> { profile ->
        DriverProfileUiState.Ready(profile)
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(DriverProfileUiState.Error((firestoreError ?: error).toArabicFriendlyMessage()))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DriverProfileUiState.Loading,
      )

  val searchingRides: StateFlow<List<Ride>> =
    driverProfileState
      .flatMapLatest { state ->
        val profile = (state as? DriverProfileUiState.Ready)?.profile
        if (profile != null &&
          profile.resolvedApprovalStatus() == DriverApprovalStatus.APPROVED &&
          profile.online
        ) {
          rideRepository.observeSearchingRides(profile.resolvedVehicleCategory()).catch { error ->
            val firestoreError =
              generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(emptyList())
          }
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )

  val driverRides: StateFlow<List<Ride>> =
    rideRepository
      .observeUserRides(currentDriverId)
      .map { rides -> rides.filter { it.assignedDriverId == currentDriverId } }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )

  val activeAssignedRide: StateFlow<Ride?> =
    driverRides
      .map { rides ->
        rides.firstOrNull { ride ->
          ride.resolvedStatus() in
            listOf(
              RideStatus.OFFER_SELECTED,
              RideStatus.DRIVER_ARRIVING,
              RideStatus.DRIVER_ARRIVED,
              RideStatus.IN_PROGRESS,
            )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )

  val activeRideContact: StateFlow<RideContact?> =
    activeAssignedRide
      .flatMapLatest { ride ->
        if (ride != null) {
          rideRepository.observeRideContact(ride.id).catch { error ->
            val firestoreError =
              generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
          }
        } else {
          flowOf(null)
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )

  val transactions: StateFlow<List<FinancialTransaction>> =
    driverRepository
      .observeDriverTransactions(currentDriverId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )

  fun submitApplication(
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
  ) {
    _actionState.update { it.copy(isSubmittingApplication = true, errorMessage = null) }
    viewModelScope.launch {
      driverRepository
        .submitDriverApplication(
          fullName = fullName,
          phone = phone,
          nationalId = nationalId,
          vehicleCategory = vehicleCategory,
          vehicleModel = vehicleModel,
          vehicleColor = vehicleColor,
          vehiclePlate = vehiclePlate,
          profilePhotoBase64 = profilePhotoBase64,
          nationalIdPhotoBase64 = nationalIdPhotoBase64,
          licensePhotoBase64 = licensePhotoBase64,
          isResubmit = isResubmit,
        )
        .onSuccess {
          _actionState.update {
            it.copy(
              isSubmittingApplication = false,
              successMessage = "تم إرسال طلب تسجيل السائق للمراجعة",
            )
          }
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(isSubmittingApplication = false, errorMessage = err.toArabicFriendlyMessage())
          }
        }
    }
  }

  fun toggleOnline(context: Context, profile: DriverProfile, online: Boolean) {
    if (online && !profile.resolvedApprovalStatus().canReceiveRideRequests()) {
      _actionState.update {
        it.copy(errorMessage = "لا يمكن تفعيل وضع الاتصال قبل موافقة الإدارة على حسابك.")
      }
      return
    }
    _actionState.update { it.copy(isTogglingOnline = true, errorMessage = null) }
    viewModelScope.launch {
      val gps = DayroutGeoService.getCurrentLocation(context)
      val lat = gps?.first ?: _actionState.value.currentLat
      val lng = gps?.second ?: _actionState.value.currentLng
      driverRepository
        .setDriverOnlineStatus(
          online = online,
          vehicleCategory = profile.resolvedVehicleCategory(),
          latitude = lat,
          longitude = lng,
        )
        .onSuccess {
          _actionState.update {
            it.copy(
              isTogglingOnline = false,
              currentLat = lat,
              currentLng = lng,
              successMessage = if (online) "أنت متاح الآن لاستقبال طلبات ديروط" else "تم التحويل إلى غير متاح",
            )
          }
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(isTogglingOnline = false, errorMessage = err.toArabicFriendlyMessage())
          }
        }
    }
  }

  fun refreshDriverGps(context: Context, profile: DriverProfile) {
    viewModelScope.launch {
      val gps = DayroutGeoService.getCurrentLocation(context) ?: return@launch
      _actionState.update { it.copy(currentLat = gps.first, currentLng = gps.second) }
      if (profile.online && profile.resolvedApprovalStatus() == DriverApprovalStatus.APPROVED) {
        driverRepository.setDriverOnlineStatus(
          online = true,
          vehicleCategory = profile.resolvedVehicleCategory(),
          latitude = gps.first,
          longitude = gps.second,
        )
      }
    }
  }

  fun acceptProposedFare(ride: Ride, driverProfile: DriverProfile) {
    submitOfferInternal(
      ride = ride,
      driverProfile = driverProfile,
      fareEgp = ride.proposedFareEgp,
      isCounterOffer = false,
    )
  }

  fun submitCounterOffer(ride: Ride, driverProfile: DriverProfile, fareEgp: Double) {
    submitOfferInternal(
      ride = ride,
      driverProfile = driverProfile,
      fareEgp = fareEgp,
      isCounterOffer = true,
    )
  }

  private fun submitOfferInternal(
    ride: Ride,
    driverProfile: DriverProfile,
    fareEgp: Double,
    isCounterOffer: Boolean,
  ) {
    _actionState.update { it.copy(isSubmittingOffer = true, errorMessage = null) }
    viewModelScope.launch {
      rideRepository
        .submitDriverOffer(
          ride = ride,
          driverProfile = driverProfile,
          offeredFareEgp = fareEgp,
          isCounterOffer = isCounterOffer,
        )
        .onSuccess {
          _actionState.update { current ->
            current.copy(
              isSubmittingOffer = false,
              offeredRideIds = current.offeredRideIds + ride.id,
              successMessage =
                if (isCounterOffer) "تم إرسال سعرك البديل (${fareEgp.toInt()} ج.م) للراكب"
                else "تم إرسال موافقتك على سعر الراكب",
            )
          }
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(isSubmittingOffer = false, errorMessage = err.toArabicFriendlyMessage())
          }
        }
    }
  }

  fun declineRideLocally(rideId: String) {
    _actionState.update { it.copy(declinedRideIds = it.declinedRideIds + rideId) }
  }

  fun advanceRideStatus(
    context: Context,
    ride: Ride,
    driverProfile: DriverProfile,
    targetStatus: RideStatus,
  ) {
    _actionState.update { it.copy(isUpdatingRide = true, errorMessage = null) }
    viewModelScope.launch {
      val gps = DayroutGeoService.getCurrentLocation(context)
      val lat = gps?.first ?: _actionState.value.currentLat
      val lng = gps?.second ?: _actionState.value.currentLng

      if (targetStatus == RideStatus.DRIVER_ARRIVING) {
        rideRepository.attachDriverContactToRide(ride.id, driverProfile.phone)
      }

      val result =
        if (targetStatus == RideStatus.COMPLETED) {
          rideRepository.completeRideAndRecordCommission(ride)
        } else {
          rideRepository.advanceRideStatus(
            ride = ride,
            targetStatus = targetStatus,
            driverLat = lat,
            driverLng = lng,
          )
        }

      result
        .onSuccess {
          _actionState.update {
            it.copy(
              isUpdatingRide = false,
              currentLat = lat,
              currentLng = lng,
              successMessage =
                if (targetStatus == RideStatus.COMPLETED) "تم إنهاء الرحلة واحتساب العمولة بنجاح"
                else null,
            )
          }
        }
        .onFailure { err ->
          _actionState.update {
            it.copy(isUpdatingRide = false, errorMessage = err.toArabicFriendlyMessage())
          }
        }
    }
  }

  fun clearMessages() {
    _actionState.update { it.copy(errorMessage = null, successMessage = null) }
  }
}
