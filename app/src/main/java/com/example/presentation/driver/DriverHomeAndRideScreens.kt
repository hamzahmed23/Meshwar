package com.example.presentation.driver

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.location.DayroutGeoService
import com.example.data.model.DriverProfile
import com.example.data.model.Ride
import com.example.data.model.RideContact
import com.example.domain.model.FareEstimator
import com.example.domain.model.RideStatus
import com.example.presentation.components.DayroutMapView
import com.example.presentation.components.PulsingBeaconDot
import com.example.presentation.components.RideStatusTimeline
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.formatEgp
import com.example.presentation.components.openDialer
import com.example.presentation.components.openNavigationMaps
import com.example.presentation.components.openWhatsApp
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWhite

@Composable
fun DriverHomeScreen(
  driverProfile: DriverProfile,
  actionState: DriverActionState,
  searchingRides: List<Ride>,
  viewModel: DriverViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var counterOfferTargetRide by remember { mutableStateOf<Ride?>(null) }
  var counterOfferFareText by remember { mutableStateOf("") }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      val granted =
        result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        viewModel.refreshDriverGps(context, driverProfile)
      }
    }

  counterOfferTargetRide?.let { ride ->
    val rec = FareEstimator.estimate(ride.resolvedVehicleCategory(), ride.distanceKm)
    AlertDialog(
      onDismissRequest = { counterOfferTargetRide = null },
      title = { Text("تقديم سعر بديل") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("سعر الراكب المقترح: ${formatEgp(ride.proposedFareEgp)}", style = MaterialTheme.typography.bodyMedium)
          Text(
            "النطاق العادل لهذه المسافة (${ride.distanceKm} كم): ${rec.recommendedMinEgp} – ${rec.recommendedMaxEgp} ج.م",
            style = MaterialTheme.typography.labelSmall,
            color = DRideRoyalBlue,
          )
          OutlinedTextField(
            value = counterOfferFareText,
            onValueChange = { counterOfferFareText = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
            label = { Text("سعرك المقترح (ج.م)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("driver_counteroffer_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val fare = counterOfferFareText.toDoubleOrNull() ?: 0.0
            if (fare >= 10.0) {
              viewModel.submitCounterOffer(ride, driverProfile, fare)
              counterOfferTargetRide = null
            }
          }
        ) {
          Text("إرسال العرض")
        }
      },
      dismissButton = {
        TextButton(onClick = { counterOfferTargetRide = null }) { Text("إلغاء") }
      },
    )
  }

  val visibleRides = searchingRides.filterNot { it.id in actionState.declinedRideIds }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Online Status Header Card with Live Beacon
    Surface(
      color = if (driverProfile.online) DRideSuccessGreen.copy(alpha = 0.12f) else DRideIceBlue,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (driverProfile.online) {
            PulsingBeaconDot(color = DRideSuccessGreen, size = 14.dp)
          }
          Column {
            Text(
              text =
                if (driverProfile.online) stringResource(R.string.driver_online)
                else stringResource(R.string.driver_offline),
              style = MaterialTheme.typography.titleMedium,
              color = if (driverProfile.online) DRideSuccessGreen else DRideDeepBlue,
            )
            Text(
              text = "${driverProfile.resolvedVehicleCategory().arabicLabel()} • ${driverProfile.vehiclePlate}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (actionState.isTogglingOnline) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        } else {
          Switch(
            checked = driverProfile.online,
            onCheckedChange = { checked ->
              viewModel.toggleOnline(context, driverProfile, checked)
            },
            modifier = Modifier.testTag("driver_online_switch"),
          )
        }
      }
    }

    // Driver Map in Dayrout
    Box(modifier = Modifier.fillMaxWidth().height(215.dp)) {
      val firstRide = visibleRides.firstOrNull()
      DayroutMapView(
        centerLat = actionState.currentLat,
        centerLng = actionState.currentLng,
        driverPoint = actionState.currentLat to actionState.currentLng,
        pickupPoint = firstRide?.let { it.pickupLat to it.pickupLng },
        destinationPoint = firstRide?.let { it.destinationLat to it.destinationLng },
        modifier = Modifier.fillMaxSize(),
      )

      FloatingActionButton(
        onClick = {
          locationPermissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_COARSE_LOCATION,
              Manifest.permission.ACCESS_FINE_LOCATION,
            )
          )
        },
        containerColor = DRideWhite,
        contentColor = DRideRoyalBlue,
        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).size(48.dp),
      ) {
        Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.cd_my_location))
      }
    }

    Column(
      modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      actionState.errorMessage?.let { err ->
        Text(err, color = DRideErrorRed, style = MaterialTheme.typography.bodyMedium)
      }

      if (!driverProfile.online) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = DRideDeepBlue.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Text("أنت غير متصل حالياً", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
            Text(
              "فعّل زر الاتصال بالأعلى لعرض طلبات الرحلات القريبة في ديروط وتقديم عروضك.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else if (visibleRides.isEmpty()) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = DRideRoyalBlue, modifier = Modifier.size(44.dp))
            Text("لا توجد طلبات قريبة حالياً", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
            Text(
              "ستظهر هنا طلبات الركاب المناسبة لفئة ${driverProfile.resolvedVehicleCategory().arabicLabel()} لحظة إرسالها.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        Text(
          text = "الطلبات المتاحة في ديروط (${visibleRides.size})",
          style = MaterialTheme.typography.titleMedium,
          color = DRideDeepBlue,
        )
        visibleRides.forEach { ride ->
          val alreadyOffered = ride.id in actionState.offeredRideIds
          val distanceToPickupKm =
            DayroutGeoService.calculateDistanceKm(
              actionState.currentLat,
              actionState.currentLng,
              ride.pickupLat,
              ride.pickupLng,
            )
          val estimate = FareEstimator.estimate(ride.resolvedVehicleCategory(), ride.distanceKm)

          Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth().testTag("driver_ride_request_${ride.id}"),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(ride.passengerName, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "يبعد عنك $distanceToPickupKm كم • المشوار ${ride.distanceKm} كم (~${estimate.estimatedMinutes}د)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(shape = CircleShape, color = DRideIceBlue) {
                  Text(
                    text = formatEgp(ride.proposedFareEgp),
                    style = MaterialTheme.typography.titleMedium,
                    color = DRideDeepBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  )
                }
              }

              HorizontalDivider()
              Text("الانطلاق: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
              Text("الوجهة: ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)
              Text(
                "التقدير العادل للمشوار: ${estimate.recommendedMinEgp} – ${estimate.recommendedMaxEgp} ج.م",
                style = MaterialTheme.typography.labelSmall,
                color = DRideRoyalBlue,
              )

              if (alreadyOffered) {
                Surface(shape = MaterialTheme.shapes.small, color = DRideSuccessGreen.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                  Text(
                    "تم إرسال عرضك لهذا الطلب، بانتظار موافقة الراكب",
                    style = MaterialTheme.typography.labelLarge,
                    color = DRideSuccessGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = { viewModel.acceptProposedFare(ride, driverProfile) },
                  enabled = !actionState.isSubmittingOffer,
                  modifier = Modifier.weight(1.2f).height(48.dp).testTag("driver_accept_fare_${ride.id}"),
                ) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.size(4.dp))
                  Text(stringResource(R.string.btn_accept_proposed_fare), maxLines = 1)
                }
                OutlinedButton(
                  onClick = {
                    counterOfferFareText = (ride.proposedFareEgp + 5).toInt().toString()
                    counterOfferTargetRide = ride
                  },
                  enabled = !actionState.isSubmittingOffer,
                  modifier = Modifier.weight(1.2f).height(48.dp).testTag("driver_counteroffer_${ride.id}"),
                ) {
                  Icon(Icons.Rounded.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.size(4.dp))
                  Text(stringResource(R.string.btn_counteroffer), maxLines = 1)
                }
                OutlinedButton(
                  onClick = { viewModel.declineRideLocally(ride.id) },
                  modifier = Modifier.height(48.dp),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.btn_decline), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DriverActiveRideScreen(
  ride: Ride,
  driverProfile: DriverProfile,
  contact: RideContact?,
  actionState: DriverActionState,
  onAdvanceStatus: (RideStatus) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val currentStatus = ride.resolvedStatus()

  val navTarget =
    if (currentStatus == RideStatus.IN_PROGRESS) {
      Triple(ride.destinationLat, ride.destinationLng, ride.destinationAddress)
    } else {
      Triple(ride.pickupLat, ride.pickupLng, ride.pickupAddress)
    }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    DayroutMapView(
      centerLat = actionState.currentLat,
      centerLng = actionState.currentLng,
      pickupPoint = ride.pickupLat to ride.pickupLng,
      destinationPoint = ride.destinationLat to ride.destinationLng,
      driverPoint = actionState.currentLat to actionState.currentLng,
      modifier = Modifier.fillMaxWidth().height(230.dp),
    )

    Column(
      modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DRideIceBlue),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = currentStatus.arabicLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = DRideDeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
          RideStatusTimeline(currentStatus = currentStatus)
        }
      }

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("الراكب: ${ride.passengerName}", style = MaterialTheme.typography.titleMedium)
              Text("${ride.distanceKm} كم • دفع نقدي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
              text = formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp),
              style = MaterialTheme.typography.titleLarge,
              color = DRideRoyalBlue,
              fontWeight = FontWeight.Bold,
            )
          }

          HorizontalDivider()
          Text("نقطة الانطلاق: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
          Text("الوجهة: ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)

          OutlinedButton(
            onClick = { openNavigationMaps(context, navTarget.first, navTarget.second, navTarget.third) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("driver_open_navigation_button"),
          ) {
            Icon(Icons.Rounded.Directions, contentDescription = null, tint = DRideRoyalBlue)
            Spacer(Modifier.size(8.dp))
            Text(
              if (currentStatus == RideStatus.IN_PROGRESS) "التوجه للوجهة عبر الخرائط"
              else "التوجه لموقع الراكب عبر الخرائط"
            )
          }

          val passengerPhone = contact?.passengerPhone
          if (!passengerPhone.isNullOrBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { openDialer(context, passengerPhone) },
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Rounded.Call, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_call))
              }
              OutlinedButton(
                onClick = { openWhatsApp(context, passengerPhone) },
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Rounded.Chat, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_whatsapp))
              }
            }
          }
        }
      }

      actionState.errorMessage?.let { err ->
        Text(err, color = DRideErrorRed, style = MaterialTheme.typography.bodyMedium)
      }

      val nextAction: Pair<RideStatus, String>? =
        when (currentStatus) {
          RideStatus.OFFER_SELECTED -> RideStatus.DRIVER_ARRIVING to "أنا في طريقي للراكب"
          RideStatus.DRIVER_ARRIVING -> RideStatus.DRIVER_ARRIVED to "وصلت لموقع الراكب"
          RideStatus.DRIVER_ARRIVED -> RideStatus.IN_PROGRESS to "بدء الرحلة الآن"
          RideStatus.IN_PROGRESS -> RideStatus.COMPLETED to "إنهاء الرحلة وتحصيل الأجرة نقداً"
          else -> null
        }

      nextAction?.let { (targetStatus, label) ->
        Button(
          onClick = { onAdvanceStatus(targetStatus) },
          enabled = !actionState.isUpdatingRide,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (targetStatus == RideStatus.COMPLETED) DRideSuccessGreen else DRideRoyalBlue,
            ),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("driver_advance_ride_button"),
        ) {
          if (actionState.isUpdatingRide) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = DRideWhite, strokeWidth = 2.5.dp)
          } else {
            Icon(
              imageVector = if (targetStatus == RideStatus.COMPLETED) Icons.Rounded.Payments else Icons.Rounded.Navigation,
              contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
          }
        }
      }

      if (currentStatus != RideStatus.IN_PROGRESS) {
        OutlinedButton(
          onClick = { onAdvanceStatus(RideStatus.CANCELLED) },
          enabled = !actionState.isUpdatingRide,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Text(stringResource(R.string.btn_cancel_ride))
        }
      }
    }
  }
}
