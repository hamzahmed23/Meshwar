package com.example.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricRickshaw
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.ImageEncodingHelper
import com.example.data.model.Ride
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.FareFairnessLevel
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideOutline
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWarningAmber
import com.example.ui.theme.DRideWhite
import java.util.Locale

fun VehicleCategory.arabicLabel(): String =
  when (this) {
    VehicleCategory.TUK_TUK -> "توك توك"
    VehicleCategory.MOTORCYCLE -> "موتوسيكل"
    VehicleCategory.CAR -> "سيارة"
  }

fun VehicleCategory.arabicSubtitle(): String =
  when (this) {
    VehicleCategory.TUK_TUK -> "مناسب للشوارع الداخلية في ديروط"
    VehicleCategory.MOTORCYCLE -> "أسرع وصول للمشاوير الفردية"
    VehicleCategory.CAR -> "راحة وتكييف ومساحة للعائلات"
  }

fun VehicleCategory.iconVector(): ImageVector =
  when (this) {
    VehicleCategory.TUK_TUK -> Icons.Rounded.ElectricRickshaw
    VehicleCategory.MOTORCYCLE -> Icons.Rounded.TwoWheeler
    VehicleCategory.CAR -> Icons.Rounded.DirectionsCar
  }

fun RideStatus.arabicLabel(): String =
  when (this) {
    RideStatus.REQUESTED -> "تم إنشاء الطلب"
    RideStatus.SEARCHING -> "جاري البحث عن سائقين"
    RideStatus.OFFER_SELECTED -> "تم اختيار السائق"
    RideStatus.DRIVER_ARRIVING -> "السائق في الطريق إليك"
    RideStatus.DRIVER_ARRIVED -> "السائق وصل لنقطة الانطلاق"
    RideStatus.IN_PROGRESS -> "الرحلة جارية الآن"
    RideStatus.COMPLETED -> "اكتملت الرحلة"
    RideStatus.CANCELLED -> "تم إلغاء الرحلة"
    RideStatus.DISPUTED -> "قيد مراجعة النزاع"
  }

fun DriverApprovalStatus.arabicLabel(): String =
  when (this) {
    DriverApprovalStatus.NOT_APPLIED -> "غير مسجل"
    DriverApprovalStatus.PENDING_REVIEW -> "قيد المراجعة"
    DriverApprovalStatus.APPROVED -> "معتمد"
    DriverApprovalStatus.REJECTED -> "مرفوض"
    DriverApprovalStatus.SUSPENDED -> "موقوف"
  }

fun FareFairnessLevel.arabicSummary(): Pair<String, Color> =
  when (this) {
    FareFairnessLevel.EXCELLENT -> "سعر ممتاز • فرصة قبول سريعة" to DRideSuccessGreen
    FareFairnessLevel.FAIR -> "سعر مناسب للمسافة" to DRideRoyalBlue
    FareFairnessLevel.LOW -> "أقل من المعتاد • قد يتأخر القبول" to DRideWarningAmber
  }

fun formatEgp(amount: Double): String {
  val formatted =
    if (amount % 1.0 == 0.0) amount.toInt().toString()
    else String.format(Locale.US, "%.2f", amount)
  return "$formatted ج.م"
}

@Composable
fun UserAvatarBadge(
  photoBase64: String?,
  size: Dp = 56.dp,
  modifier: Modifier = Modifier,
) {
  val bitmap = remember(photoBase64) { ImageEncodingHelper.decodeBase64ToImageBitmap(photoBase64) }
  Surface(
    shape = CircleShape,
    color = DRideIceBlue,
    modifier = modifier.size(size),
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = stringResource(R.string.cd_user_avatar),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().clip(CircleShape),
      )
    } else {
      Box(
        modifier = Modifier.fillMaxSize().background(DRideIceBlue),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Person,
          contentDescription = stringResource(R.string.cd_user_avatar),
          tint = DRideDeepBlue,
          modifier = Modifier.size(size * 0.55f),
        )
      }
    }
  }
}

@Composable
fun RideStatusTimeline(
  currentStatus: RideStatus,
  modifier: Modifier = Modifier,
) {
  val steps =
    listOf(
      RideStatus.OFFER_SELECTED to "تم الاتفاق",
      RideStatus.DRIVER_ARRIVING to "في الطريق",
      RideStatus.DRIVER_ARRIVED to "وصل",
      RideStatus.IN_PROGRESS to "الرحلة",
      RideStatus.COMPLETED to "انتهت",
    )
  val activeIndex =
    when (currentStatus) {
      RideStatus.OFFER_SELECTED -> 0
      RideStatus.DRIVER_ARRIVING -> 1
      RideStatus.DRIVER_ARRIVED -> 2
      RideStatus.IN_PROGRESS -> 3
      RideStatus.COMPLETED -> 4
      else -> 0
    }

  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    steps.forEachIndexed { index, (_, label) ->
      val reached = index <= activeIndex
      val current = index == activeIndex
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.weight(1f),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(3.dp)
                .background(
                  when {
                    index == 0 -> Color.Transparent
                    reached -> DRideRoyalBlue
                    else -> DRideOutline
                  }
                )
          )
          Surface(
            shape = CircleShape,
            color =
              when {
                current -> DRideRoyalBlue
                reached -> DRideSuccessGreen
                else -> DRideOutline
              },
            modifier = Modifier.size(if (current) 24.dp else 18.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (reached) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = DRideWhite,
                  modifier = Modifier.size(12.dp),
                )
              }
            }
          }
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(3.dp)
                .background(
                  when {
                    index == steps.lastIndex -> Color.Transparent
                    index < activeIndex -> DRideSuccessGreen
                    else -> DRideOutline
                  }
                )
          )
        }
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = if (reached) DRideDeepBlue else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
fun PulsingBeaconDot(
  color: Color,
  size: Dp = 14.dp,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "pulse")
  val scale by
    transition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.45f,
      animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "pulse_scale",
    )
  Box(modifier = modifier.size(size * 1.6f), contentAlignment = Alignment.Center) {
    Surface(
      shape = CircleShape,
      color = color.copy(alpha = 0.25f),
      modifier = Modifier.size(size).scale(scale),
      content = {},
    )
    Surface(
      shape = CircleShape,
      color = color,
      modifier = Modifier.size(size * 0.65f),
      content = {},
    )
  }
}

fun openDialer(context: Context, phone: String) {
  val clean = phone.trim()
  if (clean.isEmpty()) return
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
  runCatching { context.startActivity(intent) }
}

fun openWhatsApp(context: Context, phone: String) {
  val digits = phone.filter { it.isDigit() }
  val intl = if (digits.startsWith("0")) "2$digits" else digits
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$intl"))
  runCatching { context.startActivity(intent) }
}

fun openNavigationMaps(context: Context, lat: Double, lng: Double, label: String) {
  val encoded = Uri.encode(label)
  val navUri = Uri.parse("google.navigation:q=$lat,$lng")
  val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply { setPackage("com.google.android.apps.maps") }
  val fallbackUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encoded)")
  val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
  runCatching { context.startActivity(navIntent) }.onFailure {
    runCatching { context.startActivity(fallbackIntent) }
  }
}

fun shareRideSafetyDetails(context: Context, ride: Ride) {
  val msg = buildString {
    appendLine("تفاصيل رحلتي عبر D-RIDE (ديروط):")
    appendLine("السائق: ${ride.assignedDriverName ?: "غير محدد"}")
    if (!ride.assignedDriverVehicle.isNullOrBlank()) {
      appendLine("المركبة: ${ride.assignedDriverVehicle} • لوحة: ${ride.assignedDriverPlate.orEmpty()}")
    }
    appendLine("من: ${ride.pickupAddress}")
    appendLine("إلى: ${ride.destinationAddress}")
    appendLine("الأجرة المتفق عليها: ${formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp)}")
  }
  val sendIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, msg)
    }
  runCatching {
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفاصيل الرحلة للأمان"))
  }
}
