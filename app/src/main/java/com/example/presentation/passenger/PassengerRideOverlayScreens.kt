package com.example.presentation.passenger

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Ride
import com.example.data.model.RideContact
import com.example.data.model.RideOffer
import com.example.data.model.UserProfile
import com.example.domain.model.RideStatus
import com.example.presentation.components.DayroutMapView
import com.example.presentation.components.PulsingBeaconDot
import com.example.presentation.components.RideStatusTimeline
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.formatEgp
import com.example.presentation.components.iconVector
import com.example.presentation.components.openDialer
import com.example.presentation.components.openWhatsApp
import com.example.presentation.components.shareRideSafetyDetails
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWarningAmber

@Composable
fun PassengerMatchingScreen(
  ride: Ride,
  offers: List<RideOffer>,
  onAcceptOffer: (RideOffer) -> Unit,
  onRejectOffer: (RideOffer) -> Unit,
  onCancelRide: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCancelConfirm by remember { mutableStateOf(false) }

  if (showCancelConfirm) {
    AlertDialog(
      onDismissRequest = { showCancelConfirm = false },
      title = { Text("إلغاء طلب الرحلة؟") },
      text = { Text("سيتم إلغاء البحث عن السائقين لهذه الرحلة.") },
      confirmButton = {
        Button(
          onClick = {
            showCancelConfirm = false
            onCancelRide()
          },
          colors = ButtonDefaults.buttonColors(containerColor = DRideErrorRed),
        ) {
          Text("تأكيد الإلغاء")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCancelConfirm = false }) { Text("تراجع") }
      },
    )
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = DRideIceBlue),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          PulsingBeaconDot(color = DRideRoyalBlue, size = 14.dp)
          Text(
            text = stringResource(R.string.matching_title),
            style = MaterialTheme.typography.titleLarge,
            color = DRideDeepBlue,
          )
        }
        Text(
          text = stringResource(R.string.matching_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DRideRoyalBlue)
        HorizontalDivider()
        Text("من: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
        Text("إلى: ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "الفئة: ${ride.resolvedVehicleCategory().arabicLabel()} (${ride.distanceKm} كم)",
            style = MaterialTheme.typography.labelLarge,
            color = DRideDeepBlue,
          )
          Text(
            text = "سعرك: ${formatEgp(ride.proposedFareEgp)}",
            style = MaterialTheme.typography.labelLarge,
            color = DRideRoyalBlue,
          )
        }
      }
    }

    val pendingOffers = offers.filter { it.status == "pending" }
    if (pendingOffers.isEmpty()) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp).fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          PulsingBeaconDot(color = DRideSuccessGreen, size = 18.dp)
          Text(
            text = "بانتظار عروض السائقين في ديروط…",
            style = MaterialTheme.typography.titleMedium,
            color = DRideDeepBlue,
          )
          Text(
            text = "يتم تنبيه السائقين المعتمدين القريبين منك الآن وستظهر عروضهم وأسعارهم فور إرسالها.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      pendingOffers.forEach { offer ->
        val diff = offer.offeredFareEgp - ride.proposedFareEgp
        val diffBadge =
          when {
            diff <= 0.0 -> "مطابق لسعرك" to DRideSuccessGreen
            else -> "+${diff.toInt()} ج.م عن سعرك" to DRideWarningAmber
          }

        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth().testTag("offer_card_${offer.id}"),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(offer.driverName, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Rounded.Star, contentDescription = null, tint = DRideWarningAmber, modifier = Modifier.size(16.dp))
                  Text("${offer.driverRating}", style = MaterialTheme.typography.labelLarge)
                  Text("• ${offer.vehicleSummary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
              Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                  shape = CircleShape,
                  color = diffBadge.second.copy(alpha = 0.14f),
                ) {
                  Text(
                    text = formatEgp(offer.offeredFareEgp),
                    style = MaterialTheme.typography.titleMedium,
                    color = diffBadge.second,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  )
                }
                Text(
                  text = diffBadge.first,
                  style = MaterialTheme.typography.labelSmall,
                  color = diffBadge.second,
                )
              }
            }

            Text(
              text = "لوحة المركبة: ${offer.vehiclePlate}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { onRejectOffer(offer) },
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_reject_offer))
              }
              Button(
                onClick = { onAcceptOffer(offer) },
                modifier = Modifier.weight(1.4f).height(48.dp).testTag("accept_offer_${offer.id}"),
              ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_accept_offer))
              }
            }
          }
        }
      }
    }

    OutlinedButton(
      onClick = { showCancelConfirm = true },
      colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
      modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cancel_searching_ride_button"),
    ) {
      Text(stringResource(R.string.btn_cancel_ride))
    }
  }
}

@Composable
fun PassengerActiveRideScreen(
  ride: Ride,
  contact: RideContact?,
  userProfile: UserProfile,
  onCancelRide: () -> Unit,
  onSubmitComplaint: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showComplaintDialog by remember { mutableStateOf(false) }
  var complaintText by remember { mutableStateOf("") }

  if (showComplaintDialog) {
    AlertDialog(
      onDismissRequest = { showComplaintDialog = false },
      title = { Text(stringResource(R.string.btn_report_complaint)) },
      text = {
        OutlinedTextField(
          value = complaintText,
          onValueChange = { complaintText = it },
          label = { Text("تفاصيل الشكوى") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (complaintText.trim().length >= 5) {
              onSubmitComplaint(complaintText)
              complaintText = ""
              showComplaintDialog = false
            }
          }
        ) { Text("إرسال") }
      },
      dismissButton = {
        TextButton(onClick = { showComplaintDialog = false }) { Text("إلغاء") }
      },
    )
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    DayroutMapView(
      centerLat = ride.driverLat ?: ride.pickupLat,
      centerLng = ride.driverLng ?: ride.pickupLng,
      pickupPoint = ride.pickupLat to ride.pickupLng,
      destinationPoint = ride.destinationLat to ride.destinationLng,
      driverPoint =
        if (ride.driverLat != null && ride.driverLng != null) ride.driverLat to ride.driverLng else null,
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
            text = ride.resolvedStatus().arabicLabel(),
            style = MaterialTheme.typography.titleMedium,
            color = DRideDeepBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
          RideStatusTimeline(currentStatus = ride.resolvedStatus())
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(
                imageVector = ride.resolvedVehicleCategory().iconVector(),
                contentDescription = null,
                tint = DRideRoyalBlue,
                modifier = Modifier.size(30.dp),
              )
              Column {
                Text(ride.assignedDriverName ?: "السائق", style = MaterialTheme.typography.titleMedium)
                Text(
                  "${ride.assignedDriverVehicle.orEmpty()} • ${ride.assignedDriverPlate.orEmpty()}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Rounded.Star, contentDescription = null, tint = DRideWarningAmber, modifier = Modifier.size(18.dp))
              Text("${ride.assignedDriverRating ?: 5.0}", style = MaterialTheme.typography.titleMedium)
            }
          }

          HorizontalDivider()
          Text("الانطلاق: ${ride.pickupAddress}", style = MaterialTheme.typography.bodyMedium)
          Text("الوجهة: ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(stringResource(R.string.label_agreed_fare), style = MaterialTheme.typography.titleMedium)
            Text(
              text = formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp),
              style = MaterialTheme.typography.titleLarge,
              color = DRideRoyalBlue,
              fontWeight = FontWeight.Bold,
            )
          }

          val driverPhone = contact?.driverPhone
          if (!driverPhone.isNullOrBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { openDialer(context, driverPhone) },
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Rounded.Call, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_call))
              }
              OutlinedButton(
                onClick = { openWhatsApp(context, driverPhone) },
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Icon(Icons.Rounded.Chat, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.btn_whatsapp))
              }
            }
          }

          // Safety trip share card button
          OutlinedButton(
            onClick = { shareRideSafetyDetails(context, ride) },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_ride_safety_button"),
          ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = DRideRoyalBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("مشاركة تفاصيل الرحلة مع العائلة للأمان")
            Spacer(Modifier.size(6.dp))
            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (ride.resolvedStatus() != RideStatus.IN_PROGRESS) {
          OutlinedButton(
            onClick = onCancelRide,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Text(stringResource(R.string.btn_cancel_ride))
          }
        }
        OutlinedButton(
          onClick = { showComplaintDialog = true },
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(6.dp))
          Text(stringResource(R.string.btn_report_complaint))
        }
      }
    }
  }
}

@Composable
fun PassengerRideCompletionScreen(
  ride: Ride,
  onConfirmRating: (rating: Int, feedback: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var rating by remember { mutableIntStateOf(5) }
  var feedback by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = DRideSuccessGreen,
          modifier = Modifier.size(56.dp),
        )
        Text(
          text = stringResource(R.string.completion_title),
          style = MaterialTheme.typography.headlineMedium,
          color = DRideDeepBlue,
        )
        Text(
          text = "الأجرة النهائية (نقداً)",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp),
          style = MaterialTheme.typography.displayLarge,
          color = DRideRoyalBlue,
          fontWeight = FontWeight.ExtraBold,
        )
        HorizontalDivider()
        Text(
          text = "قيّم السائق ${ride.assignedDriverName.orEmpty()}",
          style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (star in 1..5) {
            IconButton(
              onClick = { rating = star },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "$star نجوم",
                tint = DRideWarningAmber,
                modifier = Modifier.size(34.dp),
              )
            }
          }
        }
        OutlinedTextField(
          value = feedback,
          onValueChange = { feedback = it },
          label = { Text("ملاحظاتك عن الرحلة (اختياري)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = { onConfirmRating(rating, feedback) },
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_rating_button"),
        ) {
          Text(stringResource(R.string.completion_confirm_btn))
        }
      }
    }
  }
}
