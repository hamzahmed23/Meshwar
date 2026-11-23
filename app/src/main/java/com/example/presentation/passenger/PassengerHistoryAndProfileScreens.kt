package com.example.presentation.passenger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.ImageEncodingHelper
import com.example.data.model.Ride
import com.example.data.model.UserProfile
import com.example.domain.model.RideStatus
import com.example.presentation.components.UserAvatarBadge
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.formatEgp
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWarningAmber
import kotlinx.coroutines.launch

@Composable
fun PassengerHistoryScreen(
  ridesState: PassengerRidesUiState,
  onRebookRide: (Ride) -> Unit,
  modifier: Modifier = Modifier,
) {
  var filterIndex by remember { mutableIntStateOf(0) }

  when (ridesState) {
    PassengerRidesUiState.Loading -> {
      Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        CircularProgressIndicator()
      }
    }
    is PassengerRidesUiState.Error -> {
      Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(ridesState.message, color = DRideErrorRed)
      }
    }
    is PassengerRidesUiState.Success -> {
      val allRides = ridesState.rides
      val filteredRides =
        when (filterIndex) {
          1 -> allRides.filter { it.resolvedStatus() == RideStatus.COMPLETED }
          2 -> allRides.filter { it.resolvedStatus() == RideStatus.CANCELLED }
          else -> allRides
        }

      if (allRides.isEmpty()) {
        Column(
          modifier = modifier.fillMaxSize().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = DRideDeepBlue.copy(alpha = 0.45f),
            modifier = Modifier.size(64.dp),
          )
          Spacer(Modifier.height(12.dp))
          Text("لا توجد رحلات سابقة حتى الآن", style = MaterialTheme.typography.titleMedium)
          Text(
            "عند إتمام أول رحلة لك في ديروط ستظهر هنا كاملة.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Column(modifier = modifier.fillMaxSize()) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf("الكل (${allRides.size})", "مكتملة", "ملغاة").forEachIndexed { idx, label ->
              FilterChip(
                selected = filterIndex == idx,
                onClick = { filterIndex = idx },
                label = { Text(label) },
              )
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filteredRides, key = { it.id }) { ride ->
              PassengerRideHistoryCard(ride = ride, onRebookRide = { onRebookRide(ride) })
            }
            item { Spacer(Modifier.height(12.dp)) }
          }
        }
      }
    }
  }
}

@Composable
private fun PassengerRideHistoryCard(
  ride: Ride,
  onRebookRide: () -> Unit,
) {
  val st = ride.resolvedStatus()
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color =
            when (st) {
              RideStatus.COMPLETED -> DRideSuccessGreen.copy(alpha = 0.14f)
              RideStatus.CANCELLED -> DRideErrorRed.copy(alpha = 0.12f)
              else -> DRideIceBlue
            },
        ) {
          Text(
            text = st.arabicLabel(),
            style = MaterialTheme.typography.labelLarge,
            color =
              when (st) {
                RideStatus.COMPLETED -> DRideSuccessGreen
                RideStatus.CANCELLED -> DRideErrorRed
                else -> DRideDeepBlue
              },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        Text(
          text = formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp),
          style = MaterialTheme.typography.titleMedium,
          color = DRideDeepBlue,
        )
      }
      Text("من: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
      Text("إلى: ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${ride.resolvedVehicleCategory().arabicLabel()} • ${ride.assignedDriverName ?: "بدون سائق"}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ride.passengerRating?.let { stars ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = DRideWarningAmber, modifier = Modifier.size(16.dp))
            Text(" $stars/5", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
      OutlinedButton(
        onClick = onRebookRide,
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text("إعادة طلب نفس المسار")
      }
    }
  }
}

@Composable
fun PassengerProfileScreen(
  userProfile: UserProfile,
  ridesCount: Int,
  onUpdateProfile: (fullName: String, phone: String, photoBase64: String?) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var fullName by remember(userProfile.fullName) { mutableStateOf(userProfile.fullName) }
  var phone by remember(userProfile.phone) { mutableStateOf(userProfile.phone.orEmpty()) }
  var photoBase64 by remember(userProfile.photoBase64) { mutableStateOf(userProfile.photoBase64) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val encoded = ImageEncodingHelper.encodeUriToCompactBase64(context, uri, 220, 70)
          if (encoded != null) photoBase64 = encoded
        }
      }
    }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        UserAvatarBadge(photoBase64 = photoBase64, size = 84.dp)
        OutlinedButton(
          onClick = {
            photoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }
        ) {
          Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("تغيير الصورة الشخصية")
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$ridesCount", style = MaterialTheme.typography.titleLarge, color = DRideDeepBlue)
            Text("الرحلات", style = MaterialTheme.typography.labelSmall)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${userProfile.ratingAvg}", style = MaterialTheme.typography.titleLarge, color = DRideDeepBlue)
            Text("التقييم", style = MaterialTheme.typography.labelSmall)
          }
        }

        HorizontalDivider()

        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text(stringResource(R.string.label_full_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15) },
          label = { Text(stringResource(R.string.label_phone_number)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        userProfile.email?.let { email ->
          Text(email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
          onClick = { onUpdateProfile(fullName, phone, photoBase64) },
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          Icon(Icons.Rounded.Save, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("حفظ التعديلات")
        }
      }
    }

    OutlinedButton(
      onClick = onSignOut,
      colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
      modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
      Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
      Spacer(Modifier.size(8.dp))
      Text(stringResource(R.string.btn_logout))
    }
  }
}
