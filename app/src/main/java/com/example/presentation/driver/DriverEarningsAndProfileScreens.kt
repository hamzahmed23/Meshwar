package com.example.presentation.driver

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.ImageEncodingHelper
import com.example.data.model.DriverProfile
import com.example.data.model.FinancialTransaction
import com.example.presentation.components.UserAvatarBadge
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.formatEgp
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideWarningAmber

@Composable
fun DriverEarningsScreen(
  driverProfile: DriverProfile,
  transactions: List<FinancialTransaction>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DRideDeepBlue),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = DRideIceBlue)
            Text("ملخص أرباح السائق", style = MaterialTheme.typography.titleMedium, color = DRideIceBlue)
          }
          Text(
            text = formatEgp(driverProfile.totalNetEgp),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = "صافي الأرباح بعد خصم عمولة المنصة",
            style = MaterialTheme.typography.labelSmall,
            color = DRideIceBlue.copy(alpha = 0.85f),
          )
          HorizontalDivider(color = DRideIceBlue.copy(alpha = 0.25f))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricColumn("الرحلات المكتملة", "${driverProfile.completedRidesCount}")
            MetricColumn("إجمالي التحصيل", formatEgp(driverProfile.totalGrossEgp))
            MetricColumn("عمولة المنصة", formatEgp(driverProfile.totalCommissionEgp))
          }
        }
      }
    }

    item {
      Text(
        text = "سجل المعاملات المالية (${transactions.size})",
        style = MaterialTheme.typography.titleMedium,
        color = DRideDeepBlue,
      )
    }

    if (transactions.isEmpty()) {
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "لم تكتمل أي رحلات مسجلة بعد. عند إنهاء كل رحلة سيتم احتساب العمولة وصافي الربح هنا تلقائياً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      }
    } else {
      items(transactions, key = { it.id }) { tx ->
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("رحلة #${tx.rideId.take(6)}", style = MaterialTheme.typography.labelLarge, color = DRideDeepBlue)
              Text(formatEgp(tx.driverNetEgp), style = MaterialTheme.typography.titleMedium, color = DRideSuccessGreen)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("الإجمالي: ${formatEgp(tx.grossFareEgp)}", style = MaterialTheme.typography.bodyMedium)
              Text(
                "العمولة (${tx.commissionRatePercent}%): ${formatEgp(tx.commissionAmountEgp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricColumn(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    Text(label, style = MaterialTheme.typography.labelSmall, color = DRideIceBlue.copy(alpha = 0.85f))
  }
}

@Composable
fun DriverProfileDetailsScreen(
  driverProfile: DriverProfile,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        UserAvatarBadge(photoBase64 = driverProfile.profilePhotoBase64, size = 84.dp)
        Text(driverProfile.fullName, style = MaterialTheme.typography.headlineMedium, color = DRideDeepBlue)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Verified, contentDescription = null, tint = DRideSuccessGreen, modifier = Modifier.size(18.dp))
          Text(driverProfile.resolvedApprovalStatus().arabicLabel(), style = MaterialTheme.typography.labelLarge, color = DRideSuccessGreen)
          Icon(Icons.Rounded.Star, contentDescription = null, tint = DRideWarningAmber, modifier = Modifier.size(18.dp))
          Text("${driverProfile.ratingAvg} (${driverProfile.ratingCount})", style = MaterialTheme.typography.labelLarge)
        }

        HorizontalDivider()
        Surface(shape = MaterialTheme.shapes.medium, color = DRideIceBlue, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("الهاتف: ${driverProfile.phone}", style = MaterialTheme.typography.bodyMedium)
            Text("الرقم القومي: ${driverProfile.nationalId}", style = MaterialTheme.typography.bodyMedium)
            Text(
              "المركبة: ${driverProfile.resolvedVehicleCategory().arabicLabel()} • ${driverProfile.vehicleModel} (${driverProfile.vehicleColor})",
              style = MaterialTheme.typography.bodyMedium,
            )
            Text("رقم اللوحة: ${driverProfile.vehiclePlate}", style = MaterialTheme.typography.bodyMedium)
          }
        }

        val idBitmap = remember(driverProfile.nationalIdPhotoBase64) {
          ImageEncodingHelper.decodeBase64ToImageBitmap(driverProfile.nationalIdPhotoBase64)
        }
        val licenseBitmap = remember(driverProfile.licensePhotoBase64) {
          ImageEncodingHelper.decodeBase64ToImageBitmap(driverProfile.licensePhotoBase64)
        }

        if (idBitmap != null || licenseBitmap != null) {
          Text("المستندات الموثقة", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            idBitmap?.let { bmp ->
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                  bitmap = bmp,
                  contentDescription = "البطاقة الشخصية",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().height(90.dp).clip(MaterialTheme.shapes.small),
                )
                Text("البطاقة", style = MaterialTheme.typography.labelSmall)
              }
            }
            licenseBitmap?.let { bmp ->
              Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                  bitmap = bmp,
                  contentDescription = "رخصة القيادة",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().height(90.dp).clip(MaterialTheme.shapes.small),
                )
                Text("الرخصة", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
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
