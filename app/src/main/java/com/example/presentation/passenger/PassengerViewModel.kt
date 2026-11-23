package com.example.presentation.passenger

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.toArabicFriendlyMessage
import com.example.core.config.LaunchConfig
import com.example.data.location.DayroutGeoService
import com.example.data.location.DayroutPlace
import com.example.data.model.DriverPresence
import com.example.data.model.Ride
import com.example.data.model.RideContact
import com.example.data.model.RideOffer
import com.example.data.model.SavedFavoritePlace
import com.example.data.model.UserProfile
import com.example.data.repository.DriverRepository
import com.example.data.repository.RideRepository
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.FareValidationResult
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

enum class MapPickTarget {
  PICKUP,
  DESTINATION,
}

data class PassengerBookingDraft(
  val pickupAddress: String = LaunchConfig.initialCity.let { "محطة قطار ديروط" },
  val pickupLat: Double = 27.5561,
  val pickupLng: Double = 30.8075,
  val destinationAddress: String = "",
  val destinationLat: Double? = null,
  val destinationLng: Double? = null,
  val selectedVehicle: VehicleCategory = VehicleCategory.TUK_TUK,
  val proposedFareText: String = "25",
  val pickTarget: MapPickTarget = MapPickTarget.DESTINATION,
  val searchQuery: String = "",
  val searchResults: List<DayroutPlace> = DayroutGeoService.curatedLandmarks,
  val isSearchingLocation: Boolean = false,
  val isSubmittingRide: Boolean = false,
  val actionError: String? = null,
  val actionSuccess: String? = null,
) {
  fun distanceKm(): Double? {
    val dLat = destinationLat ?: return null
    val dLng = destinationLng ?: return null
    return DayroutGeoService.calculateDistanceKm(pickupLat, pickupLng, dLat, dLng).coerceAtLeast(0.3)
  }

  fun fareEstimate(): com.example.domain.model.FareEstimate? {
    val km = distanceKm() ?: return null
    return com.example.domain.model.FareEstimator.estimate(selectedVehicle, km)
  }
}

sealed interface PassengerRidesUiState {
  data object Loading : PassengerRidesUiState

  data class Success(val rides: List<Ride>) : PassengerRidesUiState

  data class Error(val message: String) : PassengerRidesUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PassengerViewModel(
  private val rideRepository: RideRepository,
  private val driverRepository: DriverRepository,
  private val userProfileRepository: UserProfileRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _draft = MutableStateFlow(PassengerBookingDraft())
  val draft: StateFlow<PassengerBookingDraft> = _draft.asStateFlow()

  val ridesState: StateFlow<PassengerRidesUiState> =
    rideRepository
      .observeUserRides(currentUserId)
      .map<List<Ride>, PassengerRidesUiState> { rides ->
        PassengerRidesUiState.Success(rides.filter { it.passengerId == currentUserId })
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(PassengerRidesUiState.Error((firestoreError ?: error).toArabicFriendlyMessage()))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PassengerRidesUiState.Loading,
      )

  val onlineDrivers: StateFlow<List<DriverPresence>> =
    driverRepository
      .observeOnlineDrivers()
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

  val savedPlaces: StateFlow<List<SavedFavoritePlace>> =
    userProfileRepository
      .observeSavedPlaces(currentUserId)
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

  val activeRide: StateFlow<Ride?> =
    ridesState
      .map { state ->
        (state as? PassengerRidesUiState.Success)?.rides?.firstOrNull { ride ->
          val st = ride.resolvedStatus()
          st in
            listOf(
              RideStatus.REQUESTED,
              RideStatus.SEARCHING,
              RideStatus.OFFER_SELECTED,
              RideStatus.DRIVER_ARRIVING,
              RideStatus.DRIVER_ARRIVED,
              RideStatus.IN_PROGRESS,
            ) || (st == RideStatus.COMPLETED && ride.passengerRating == null)
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )

  val activeOffers: StateFlow<List<RideOffer>> =
    activeRide
      .flatMapLatest { ride ->
        if (ride != null && ride.resolvedStatus() == RideStatus.SEARCHING) {
          rideRepository.observeRideOffers(ride.id, currentUserId).catch { error ->
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

  val activeRideContact: StateFlow<RideContact?> =
    activeRide
      .flatMapLatest { ride ->
        if (ride != null && ride.assignedDriverId != null) {
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

  fun setPickTarget(target: MapPickTarget) {
    _draft.update { it.copy(pickTarget = target, actionError = null) }
  }

  fun selectVehicleCategory(category: VehicleCategory) {
    _draft.update { current ->
      val nextBase = current.copy(selectedVehicle = category, actionError = null)
      val est = nextBase.fareEstimate()
      val updatedFare =
        if (est != null) est.suggestedEgp.toString()
        else {
          val currentFare = current.proposedFareText.toDoubleOrNull() ?: 0.0
          if (currentFare < category.minFareEgp) category.minFareEgp.toInt().toString()
          else current.proposedFareText
        }
      nextBase.copy(proposedFareText = updatedFare)
    }
  }

  fun swapPickupAndDestination() {
    _draft.update { current ->
      val dLat = current.destinationLat ?: return@update current
      val dLng = current.destinationLng ?: return@update current
      val dAddr = current.destinationAddress.ifBlank { return@update current }
      current.copy(
        pickupAddress = dAddr,
        pickupLat = dLat,
        pickupLng = dLng,
        destinationAddress = current.pickupAddress,
        destinationLat = current.pickupLat,
        destinationLng = current.pickupLng,
        actionError = null,
      )
    }
  }

  fun rebookRide(ride: Ride) {
    _draft.update {
      PassengerBookingDraft(
        pickupAddress = ride.pickupAddress,
        pickupLat = ride.pickupLat,
        pickupLng = ride.pickupLng,
        destinationAddress = ride.destinationAddress,
        destinationLat = ride.destinationLat,
        destinationLng = ride.destinationLng,
        selectedVehicle = ride.resolvedVehicleCategory(),
        proposedFareText = (ride.agreedFareEgp ?: ride.proposedFareEgp).toInt().toString(),
        pickTarget = MapPickTarget.DESTINATION,
        actionSuccess = "تم تحميل بيانات المسار السابق • يمكنك التعديل أو الإرسال فوراً",
      )
    }
  }

  fun updateProposedFare(text: String) {
    val clean = text.filter { it.isDigit() || it == '.' }.take(6)
    _draft.update { it.copy(proposedFareText = clean, actionError = null) }
  }

  fun adjustProposedFare(deltaEgp: Int) {
    _draft.update { current ->
      val base = current.proposedFareText.toDoubleOrNull() ?: current.selectedVehicle.minFareEgp
      val next = (base + deltaEgp).coerceIn(current.selectedVehicle.minFareEgp, 5000.0)
      current.copy(proposedFareText = next.toInt().toString(), actionError = null)
    }
  }

  fun searchLocation(query: String) {
    _draft.update { it.copy(searchQuery = query, isSearchingLocation = true) }
    viewModelScope.launch {
      val results = DayroutGeoService.searchPlaces(query)
      _draft.update { it.copy(searchResults = results, isSearchingLocation = false) }
    }
  }

  fun selectPlace(place: DayroutPlace) {
    _draft.update { current ->
      val next =
        if (current.pickTarget == MapPickTarget.PICKUP) {
          current.copy(
            pickupAddress = place.title,
            pickupLat = place.latitude,
            pickupLng = place.longitude,
            pickTarget = MapPickTarget.DESTINATION,
            actionError = null,
          )
        } else {
          current.copy(
            destinationAddress = place.title,
            destinationLat = place.latitude,
            destinationLng = place.longitude,
            actionError = null,
          )
        }
      val est = next.fareEstimate()
      if (est != null) next.copy(proposedFareText = est.suggestedEgp.toString()) else next
    }
  }

  fun onMapTapped(context: Context, lat: Double, lng: Double) {
    viewModelScope.launch {
      val label = DayroutGeoService.reverseGeocode(context, lat, lng)
      _draft.update { current ->
        val next =
          if (current.pickTarget == MapPickTarget.PICKUP) {
            current.copy(
              pickupAddress = label,
              pickupLat = lat,
              pickupLng = lng,
              pickTarget = MapPickTarget.DESTINATION,
              actionError = null,
            )
          } else {
            current.copy(
              destinationAddress = label,
              destinationLat = lat,
              destinationLng = lng,
              actionError = null,
            )
          }
        val est = next.fareEstimate()
        if (est != null) next.copy(proposedFareText = est.suggestedEgp.toString()) else next
      }
    }
  }

  fun useCurrentGpsLocation(context: Context) {
    viewModelScope.launch {
      val coords = DayroutGeoService.getCurrentLocation(context)
      if (coords != null) {
        val label = DayroutGeoService.reverseGeocode(context, coords.first, coords.second)
        _draft.update {
          it.copy(
            pickupAddress = label,
            pickupLat = coords.first,
            pickupLng = coords.second,
            actionError = null,
          )
        }
      } else {
        _draft.update {
          it.copy(actionError = "تعذر تحديد موقعك بدقة، يمكنك اختيار نقطة الانطلاق من الخريطة.")
        }
      }
    }
  }

  fun submitRideRequest(profile: UserProfile) {
    val current = _draft.value
    val destLat = current.destinationLat
    val destLng = current.destinationLng
    if (destLat == null || destLng == null || current.destinationAddress.isBlank()) {
      _draft.update { it.copy(actionError = "يرجى تحديد الوجهة على الخريطة أو من البحث") }
      return
    }
    val fare = current.proposedFareText.toDoubleOrNull() ?: -1.0
    when (val validation = current.selectedVehicle.validateProposedFare(fare)) {
      FareValidationResult.InvalidAmount -> {
        _draft.update { it.copy(actionError = "يرجى إدخال سعر مقترح صحيح بالجنيه") }
        return
      }
      is FareValidationResult.BelowMinimum -> {
        _draft.update {
          it.copy(
            actionError =
              "الحد الأدنى لهذه الفئة ${validation.minimumEgp.toInt()} ج.م"
          )
        }
        return
      }
      is FareValidationResult.Valid -> Unit
    }

    val phone = profile.phone?.takeIf { it.length >= 8 } ?: "01000000000"
    val distKm = current.distanceKm() ?: 1.5

    _draft.update { it.copy(isSubmittingRide = true, actionError = null) }
    viewModelScope.launch {
      val result =
        rideRepository.createRideRequest(
          passengerName = profile.fullName,
          passengerPhone = phone,
          pickupAddress = current.pickupAddress,
          pickupLat = current.pickupLat,
          pickupLng = current.pickupLng,
          destinationAddress = current.destinationAddress,
          destinationLat = destLat,
          destinationLng = destLng,
          distanceKm = distKm,
          vehicleCategory = current.selectedVehicle,
          proposedFareEgp = fare,
        )
      result
        .onSuccess {
          _draft.update { it.copy(isSubmittingRide = false, actionError = null) }
        }
        .onFailure { err ->
          _draft.update {
            it.copy(isSubmittingRide = false, actionError = err.toArabicFriendlyMessage())
          }
        }
    }
  }

  fun acceptOffer(rideId: String, offer: RideOffer) {
    viewModelScope.launch {
      val result = rideRepository.acceptOfferAtomically(rideId, offer)
      result.onFailure { err ->
        _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
      }
    }
  }

  fun rejectOffer(rideId: String, offerId: String) {
    viewModelScope.launch {
      rideRepository.rejectOffer(rideId, offerId).onFailure { err ->
        _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
      }
    }
  }

  fun cancelRide(ride: Ride) {
    viewModelScope.launch {
      rideRepository.advanceRideStatus(ride, RideStatus.CANCELLED).onFailure { err ->
        _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
      }
    }
  }

  fun submitRating(ride: Ride, rating: Int, feedback: String) {
    viewModelScope.launch {
      rideRepository.rateCompletedRide(ride, rating, feedback)
        .onSuccess {
          _draft.update { it.copy(actionSuccess = "تم حفظ تقييمك بنجاح") }
        }
        .onFailure { err ->
          _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
        }
    }
  }

  fun submitComplaint(rideId: String, reporterName: String, reason: String) {
    viewModelScope.launch {
      rideRepository.submitComplaint(rideId, reporterName, reason)
        .onSuccess {
          _draft.update { it.copy(actionSuccess = "تم إرسال الشكوى للإدارة للمراجعة") }
        }
        .onFailure { err ->
          _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
        }
    }
  }

  fun updateUserProfile(fullName: String, phone: String, photoBase64: String?) {
    viewModelScope.launch {
      userProfileRepository.updateProfile(fullName, phone, photoBase64)
        .onSuccess { _draft.update { it.copy(actionSuccess = "تم تحديث البيانات بنجاح") } }
        .onFailure { err -> _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) } }
    }
  }

  fun saveFavoritePlace(
    placeKey: String,
    label: String,
    address: String,
    latitude: Double,
    longitude: Double,
  ) {
    viewModelScope.launch {
      userProfileRepository
        .saveFavoritePlace(placeKey, label, address, latitude, longitude)
        .onSuccess {
          _draft.update { it.copy(actionSuccess = "تم حفظ المكان في حسابك السحابي بنجاح") }
        }
        .onFailure { err ->
          _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
        }
    }
  }

  fun deleteFavoritePlace(placeId: String) {
    viewModelScope.launch {
      userProfileRepository
        .deleteFavoritePlace(placeId)
        .onSuccess {
          _draft.update { it.copy(actionSuccess = "تم حذف المكان المحفوظ") }
        }
        .onFailure { err ->
          _draft.update { it.copy(actionError = err.toArabicFriendlyMessage()) }
        }
    }
  }

  fun clearMessages() {
    _draft.update { it.copy(actionError = null, actionSuccess = null) }
  }
}
