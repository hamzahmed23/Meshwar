package com.example.presentation.passenger

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.location.DayroutGeoService
import com.example.data.location.DayroutPlace
import com.example.data.model.DriverPresence
import com.example.data.model.SavedFavoritePlace
import com.example.data.model.UserProfile
import com.example.domain.model.FareEstimator
import com.example.domain.model.VehicleCategory
import com.example.presentation.components.DayroutMapView
import com.example.presentation.components.PulsingBeaconDot
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.arabicSubtitle
import com.example.presentation.components.arabicSummary
import com.example.presentation.components.iconVector
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWhite

@Composable
fun PassengerHomeScreen(
  userProfile: UserProfile,
  draft: PassengerBookingDraft,
  onlineDrivers: List<DriverPresence>,
  savedPlaces: List<SavedFavoritePlace>,
  viewModel: PassengerViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showSearchDropdown by remember { mutableStateOf(false) }
  var showSavePlaceDialog by remember { mutableStateOf(false) }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      val granted =
        result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        viewModel.useCurrentGpsLocation(context)
      }
    }

  if (showSavePlaceDialog) {
    val targetLat = draft.destinationLat ?: draft.pickupLat
    val targetLng = draft.destinationLng ?: draft.pickupLng
    val targetAddr = draft.destinationAddress.ifBlank { draft.pickupAddress }
    AlertDialog(
      onDismissRequest = { showSavePlaceDialog = false },
      title = { Text("حفظ المكان في حسابك السحابي") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(targetAddr, style = MaterialTheme.typography.bodyMedium, color = DRideDeepBlue)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                viewModel.saveFavoritePlace("home", "المنزل", targetAddr, targetLat, targetLng)
                showSavePlaceDialog = false
              },
              modifier = Modifier.weight(1f).testTag("save_favorite_home_button"),
            ) {
              Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(4.dp))
              Text("المنزل")
            }
            OutlinedButton(
              onClick = {
                viewModel.saveFavoritePlace("work", "العمل", targetAddr, targetLat, targetLng)
                showSavePlaceDialog = false
              },
              modifier = Modifier.weight(1f).testTag("save_favorite_work_button"),
            ) {
              Icon(Icons.Rounded.Work, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(4.dp))
              Text("العمل")
            }
          }
          if (savedPlaces.isNotEmpty()) {
            Text(
              "الأماكن المحفوظة (${savedPlaces.size}):",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            savedPlaces.forEach { fav ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  "${fav.label}: ${fav.address}",
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.deleteFavoritePlace(fav.id) }) {
                  Text("حذف", color = DRideErrorRed)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSavePlaceDialog = false }) { Text("إغلاق") }
      },
    )
  }

  val mapCenterLat =
    if (draft.pickTarget == MapPickTarget.DESTINATION && draft.destinationLat != null) {
      draft.destinationLat
    } else {
      draft.pickupLat
    }
  val mapCenterLng =
    if (draft.pickTarget == MapPickTarget.DESTINATION && draft.destinationLng != null) {
      draft.destinationLng
    } else {
      draft.pickupLng
    }

  val fareEstimate = draft.fareEstimate()
  val enteredFare = draft.proposedFareText.toDoubleOrNull() ?: 0.0

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Interactive Dayrout Map Card
    Box(
      modifier = Modifier.fillMaxWidth().height(280.dp),
    ) {
      DayroutMapView(
        centerLat = mapCenterLat,
        centerLng = mapCenterLng,
        pickupPoint = draft.pickupLat to draft.pickupLng,
        destinationPoint =
          if (draft.destinationLat != null && draft.destinationLng != null) {
            draft.destinationLat to draft.destinationLng
          } else null,
        onlineDrivers = onlineDrivers,
        onMapTapped = { lat, lng -> viewModel.onMapTapped(context, lat, lng) },
        modifier = Modifier.fillMaxSize(),
      )

      // Live Online Drivers & Tap Hint Pill
      Row(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = DRideWhite.copy(alpha = 0.95f),
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PulsingBeaconDot(
              color = if (onlineDrivers.isNotEmpty()) DRideSuccessGreen else DRideRoyalBlue,
              size = 10.dp,
            )
            Text(
              text =
                if (onlineDrivers.isNotEmpty()) "${onlineDrivers.size} سائق متاح"
                else if (draft.pickTarget == MapPickTarget.PICKUP) "حدد نقطة الانطلاق"
                else "اضغط لتحديد الوجهة",
              style = MaterialTheme.typography.labelSmall,
              color = DRideDeepBlue,
            )
          }
        }
      }

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
        modifier =
          Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
            .size(48.dp)
            .testTag("gps_my_location_button"),
      ) {
        Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.cd_my_location))
      }
    }

    Column(
      modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Search & Pick target card
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = draft.pickTarget == MapPickTarget.PICKUP,
              onClick = { viewModel.setPickTarget(MapPickTarget.PICKUP) },
              label = { Text(stringResource(R.string.label_pickup)) },
              leadingIcon = {
                Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(18.dp))
              },
              modifier = Modifier.weight(1f).testTag("pick_pickup_chip"),
            )
            FilterChip(
              selected = draft.pickTarget == MapPickTarget.DESTINATION,
              onClick = { viewModel.setPickTarget(MapPickTarget.DESTINATION) },
              label = { Text(stringResource(R.string.label_destination)) },
              leadingIcon = {
                Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
              },
              modifier = Modifier.weight(1f).testTag("pick_destination_chip"),
            )
            if (draft.destinationLat != null) {
              IconButton(
                onClick = { viewModel.swapPickupAndDestination() },
                modifier = Modifier.size(48.dp).testTag("swap_route_button"),
              ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = "تبديل الانطلاق والوجهة", tint = DRideRoyalBlue)
              }
            }
          }

          // Pickup summary
          Surface(
            shape = MaterialTheme.shapes.small,
            color = DRideIceBlue,
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setPickTarget(MapPickTarget.PICKUP) },
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.Place, contentDescription = null, tint = DRideRoyalBlue)
              Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.label_pickup), style = MaterialTheme.typography.labelSmall, color = DRideDeepBlue)
                Text(
                  text = draft.pickupAddress,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          // Destination summary
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setPickTarget(MapPickTarget.DESTINATION) },
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.Flag, contentDescription = null, tint = DRideSuccessGreen)
              Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.label_destination), style = MaterialTheme.typography.labelSmall, color = DRideSuccessGreen)
                Text(
                  text = draft.destinationAddress.ifBlank { "اختر الوجهة من الخريطة أو البحث بالأسفل" },
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (fareEstimate != null) {
                Column(horizontalAlignment = Alignment.End) {
                  Text("${draft.distanceKm()} كم", style = MaterialTheme.typography.labelLarge, color = DRideRoyalBlue)
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = DRideDeepBlue, modifier = Modifier.size(13.dp))
                    Text("${fareEstimate.estimatedMinutes} د", style = MaterialTheme.typography.labelSmall, color = DRideDeepBlue)
                  }
                }
              }
            }
          }

          // Search box
          OutlinedTextField(
            value = draft.searchQuery,
            onValueChange = {
              showSearchDropdown = true
              viewModel.searchLocation(it)
            },
            placeholder = { Text(stringResource(R.string.search_place_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (draft.isSearchingLocation) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                IconButton(
                  onClick = { showSavePlaceDialog = true },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(Icons.Rounded.BookmarkAdd, contentDescription = "حفظ المكان", tint = DRideRoyalBlue)
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("location_search_input"),
          )

          // Saved Favorites + Quick Dayrout landmarks strip
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            savedPlaces.forEach { fav ->
              AssistChip(
                onClick = {
                  showSearchDropdown = false
                  viewModel.selectPlace(
                    DayroutPlace(fav.label, fav.address, fav.latitude, fav.longitude)
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (fav.placeKey == "home") Icons.Rounded.Home else Icons.Rounded.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DRideRoyalBlue,
                  )
                },
                label = { Text(fav.label, maxLines = 1) },
              )
            }
            DayroutGeoService.curatedLandmarks.take(6).forEach { landmark ->
              FilterChip(
                selected = false,
                onClick = {
                  showSearchDropdown = false
                  viewModel.selectPlace(landmark)
                },
                label = { Text(landmark.title, maxLines = 1) },
              )
            }
          }

          AnimatedVisibility(visible = showSearchDropdown && draft.searchQuery.isNotBlank() && draft.searchResults.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              draft.searchResults.take(4).forEach { place ->
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable {
                        showSearchDropdown = false
                        viewModel.selectPlace(place)
                      },
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(place.title, style = MaterialTheme.typography.labelLarge)
                    Text(
                      place.subtitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Vehicle Selection
      Text(
        text = "اختر وسيلة المواصلات",
        style = MaterialTheme.typography.titleMedium,
        color = DRideDeepBlue,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR).forEach { cat ->
          val selected = draft.selectedVehicle == cat
          val catEstimate = draft.distanceKm()?.let { FareEstimator.estimate(cat, it) }
          Card(
            onClick = { viewModel.selectVehicleCategory(cat) },
            shape = MaterialTheme.shapes.medium,
            colors =
              CardDefaults.cardColors(
                containerColor = if (selected) DRideIceBlue else MaterialTheme.colorScheme.surface,
              ),
            border =
              BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) DRideRoyalBlue else MaterialTheme.colorScheme.outline,
              ),
            modifier = Modifier.weight(1f).testTag("vehicle_${cat.id}"),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = cat.iconVector(),
                contentDescription = cat.arabicLabel(),
                tint = if (selected) DRideDeepBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
              )
              Text(
                text = cat.arabicLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) DRideDeepBlue else MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (catEstimate != null) "~${catEstimate.suggestedEgp} ج.م • ${catEstimate.estimatedMinutes}د"
                  else "من ${cat.minFareEgp.toInt()} ج.م",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) DRideRoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            }
          }
        }
      }
      Text(
        text = draft.selectedVehicle.arabicSubtitle(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Proposed Fare EGP Input with Smart Guidance
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.label_proposed_fare),
              style = MaterialTheme.typography.titleMedium,
              color = DRideDeepBlue,
            )
            Surface(shape = CircleShape, color = DRideIceBlue) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.Rounded.Payments, contentDescription = null, tint = DRideDeepBlue, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.payment_cash_only), style = MaterialTheme.typography.labelSmall, color = DRideDeepBlue)
              }
            }
          }

          if (fareEstimate != null) {
            val fairness = FareEstimator.evaluateProposedFare(enteredFare, fareEstimate)
            val (fairnessText, fairnessColor) = fairness.arabicSummary()
            Surface(
              shape = MaterialTheme.shapes.small,
              color = fairnessColor.copy(alpha = 0.12f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(fairnessText, style = MaterialTheme.typography.labelLarge, color = fairnessColor)
                Text(
                  "السعر التقديري العادل في ديروط: ${fareEstimate.recommendedMinEgp} – ${fareEstimate.recommendedMaxEgp} ج.م",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            IconButton(
              onClick = { viewModel.adjustProposedFare(-5) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = "إنقاص 5 جنيه", tint = DRideDeepBlue)
            }

            OutlinedTextField(
              value = draft.proposedFareText,
              onValueChange = { viewModel.updateProposedFare(it) },
              singleLine = true,
              suffix = { Text(stringResource(R.string.currency_egp), fontWeight = FontWeight.Bold) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("proposed_fare_input"),
            )

            IconButton(
              onClick = { viewModel.adjustProposedFare(5) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "زيادة 5 جنيه", tint = DRideDeepBlue)
            }
          }

          // Quick fare chips
          if (fareEstimate != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(
                fareEstimate.recommendedMinEgp,
                fareEstimate.suggestedEgp,
                fareEstimate.recommendedMaxEgp,
              ).distinct().forEach { preset ->
                FilterChip(
                  selected = enteredFare.toInt() == preset,
                  onClick = { viewModel.updateProposedFare(preset.toString()) },
                  label = { Text("$preset ج.م") },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          draft.actionError?.let { err ->
            Text(
              text = err,
              style = MaterialTheme.typography.bodyMedium,
              color = DRideErrorRed,
            )
          }

          Button(
            onClick = { viewModel.submitRideRequest(userProfile) },
            enabled = !draft.isSubmittingRide,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("request_ride_button"),
          ) {
            if (draft.isSubmittingRide) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
              )
            } else {
              Text(
                text = stringResource(R.string.btn_request_ride),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }
    }
  }
}
