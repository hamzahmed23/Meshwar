package com.example.presentation.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.media.ImageEncodingHelper
import com.example.data.model.DriverProfile
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.RideStatus
import com.example.domain.model.VehicleCategory
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.formatEgp
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen

@Composable
fun AdminDashboardScreen(
  viewModel: AdminViewModel,
  modifier: Modifier = Modifier,
) {
  val users by viewModel.users.collectAsStateWithLifecycle()
  val drivers by viewModel.drivers.collectAsStateWithLifecycle()
  val rides by viewModel.rides.collectAsStateWithLifecycle()
  val complaints by viewModel.complaints.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()
  val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
  val commissionRule by viewModel.commissionRule.collectAsStateWithLifecycle()
  val actionState by viewModel.actionState.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) }
  var driverFilterStatus by remember { mutableStateOf<DriverApprovalStatus?>(null) }
  var driverSearchQuery by remember { mutableStateOf("") }
  var inspectImagePreview by remember { mutableStateOf<Pair<String, ImageBitmap>?>(null) }
  var rejectDriverTarget by remember { mutableStateOf<DriverProfile?>(null) }
  var rejectReasonText by remember { mutableStateOf("") }

  inspectImagePreview?.let { (title, bmp) ->
    AlertDialog(
      onDismissRequest = { inspectImagePreview = null },
      title = { Text(title) },
      text = {
        Image(
          bitmap = bmp,
          contentDescription = title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxWidth().height(320.dp).clip(MaterialTheme.shapes.medium),
        )
      },
      confirmButton = {
        TextButton(onClick = { inspectImagePreview = null }) { Text("إغلاق") }
      },
    )
  }

  rejectDriverTarget?.let { driver ->
    AlertDialog(
      onDismissRequest = { rejectDriverTarget = null },
      title = { Text("سبب رفض طلب ${driver.fullName}") },
      text = {
        OutlinedTextField(
          value = rejectReasonText,
          onValueChange = { rejectReasonText = it },
          label = { Text("السبب (مثال: صورة البطاقة غير واضحة)") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.rejectDriver(driver.id, rejectReasonText.ifBlank { "بيانات غير مكتملة" })
            rejectReasonText = ""
            rejectDriverTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = DRideErrorRed),
        ) { Text("تأكيد الرفض") }
      },
      dismissButton = {
        TextButton(onClick = { rejectDriverTarget = null }) { Text("إلغاء") }
      },
    )
  }

  val tabs = listOf("نظرة عامة", "السائقون (${drivers.size})", "الرحلات والشكاوى", "العمولة والسجل")

  Column(modifier = modifier.fillMaxSize()) {
    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(title) },
          modifier = Modifier.testTag("admin_tab_$index"),
        )
      }
    }

    actionState.errorMessage?.let { err ->
      Text(err, color = DRideErrorRed, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
    }

    when (selectedTab) {
      0 -> {
        val totalGross = transactions.sumOf { it.grossFareEgp }
        val totalComm = transactions.sumOf { it.commissionAmountEgp }
        val pendingDrivers = drivers.count { it.resolvedApprovalStatus() == DriverApprovalStatus.PENDING_REVIEW }
        val completedCount = rides.count { it.resolvedStatus() == RideStatus.COMPLETED }
        val completionRatio = if (rides.isEmpty()) 0f else completedCount.toFloat() / rides.size.toFloat()

        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AdminKpiCard("الركاب والمسجلين", "${users.size}", Modifier.weight(1f))
              AdminKpiCard("طلبات السائقين المعلقة", "$pendingDrivers", Modifier.weight(1f))
            }
          }
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              AdminKpiCard("إجمالي الرحلات", "${rides.size}", Modifier.weight(1f))
              AdminKpiCard("عمولة المنصة", formatEgp(totalComm), Modifier.weight(1f))
            }
          }
          item {
            AdminKpiCard("إجمالي مبالغ الرحلات المكتملة (ديروط)", formatEgp(totalGross), Modifier.fillMaxWidth())
          }
          item {
            Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تحليلات التشغيل في ديروط", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("معدل إتمام الرحلات", style = MaterialTheme.typography.bodyMedium)
                  Text("${(completionRatio * 100).toInt()}% ($completedCount/${rides.size})", style = MaterialTheme.typography.labelLarge, color = DRideSuccessGreen)
                }
                LinearProgressIndicator(
                  progress = { completionRatio },
                  color = DRideSuccessGreen,
                  modifier = Modifier.fillMaxWidth(),
                )
                val tukCount = rides.count { it.resolvedVehicleCategory() == VehicleCategory.TUK_TUK }
                val motoCount = rides.count { it.resolvedVehicleCategory() == VehicleCategory.MOTORCYCLE }
                val carCount = rides.count { it.resolvedVehicleCategory() == VehicleCategory.CAR }
                Text(
                  "توزيع الطلبات: توك توك ($tukCount) • موتوسيكل ($motoCount) • سيارة ($carCount)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          item {
            Text("إدارة حسابات المستخدمين (${users.size})", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
          }
          items(users, key = { it.id }) { user ->
            Card(
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${user.phone.orEmpty()} • ${if (user.suspended) "موقوف" else "نشط"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.suspended) DRideErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = { viewModel.toggleUserSuspended(user.id, !user.suspended) },
                  enabled = !actionState.isBusy,
                ) {
                  Text(if (user.suspended) "تفعيل" else "إيقاف")
                }
              }
            }
          }
        }
      }
      1 -> {
        val filteredDrivers =
          drivers.filter { d ->
            val statusMatches = driverFilterStatus == null || d.resolvedApprovalStatus() == driverFilterStatus
            val queryMatches =
              driverSearchQuery.isBlank() ||
                d.fullName.contains(driverSearchQuery, ignoreCase = true) ||
                d.phone.contains(driverSearchQuery) ||
                d.vehiclePlate.contains(driverSearchQuery, ignoreCase = true)
            statusMatches && queryMatches
          }

        Column(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = driverSearchQuery,
              onValueChange = { driverSearchQuery = it },
              placeholder = { Text("ابحث بالاسم، الهاتف، أو رقم اللوحة…") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = driverFilterStatus == null,
                onClick = { driverFilterStatus = null },
                label = { Text("الكل (${drivers.size})") },
              )
              listOf(
                DriverApprovalStatus.PENDING_REVIEW,
                DriverApprovalStatus.APPROVED,
                DriverApprovalStatus.REJECTED,
                DriverApprovalStatus.SUSPENDED,
              ).forEach { statusOption ->
                val count = drivers.count { it.resolvedApprovalStatus() == statusOption }
                FilterChip(
                  selected = driverFilterStatus == statusOption,
                  onClick = { driverFilterStatus = statusOption },
                  label = { Text("${statusOption.arabicLabel()} ($count)") },
                )
              }
            }
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(filteredDrivers, key = { it.id }) { driver ->
              val st = driver.resolvedApprovalStatus()
              Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().testTag("admin_driver_card_${driver.id}"),
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(driver.fullName, style = MaterialTheme.typography.titleMedium)
                    Surface(shape = MaterialTheme.shapes.small, color = DRideIceBlue) {
                      Text(
                        st.arabicLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = DRideDeepBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }
                  }
                  Text("الهاتف: ${driver.phone} • قومي: ${driver.nationalId}", style = MaterialTheme.typography.bodyMedium)
                  Text(
                    "${driver.resolvedVehicleCategory().arabicLabel()} • ${driver.vehicleModel} (${driver.vehicleColor}) • ${driver.vehiclePlate}",
                    style = MaterialTheme.typography.bodyMedium,
                  )

                  val nidBmp = remember(driver.nationalIdPhotoBase64) {
                    ImageEncodingHelper.decodeBase64ToImageBitmap(driver.nationalIdPhotoBase64)
                  }
                  val licBmp = remember(driver.licensePhotoBase64) {
                    ImageEncodingHelper.decodeBase64ToImageBitmap(driver.licensePhotoBase64)
                  }
                  if (nidBmp != null || licBmp != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      nidBmp?.let { bmp ->
                        Image(
                          bitmap = bmp,
                          contentDescription = "صورة البطاقة",
                          contentScale = ContentScale.Crop,
                          modifier =
                            Modifier
                              .weight(1f)
                              .height(84.dp)
                              .clip(MaterialTheme.shapes.small)
                              .clickable { inspectImagePreview = "البطاقة الشخصية • ${driver.fullName}" to bmp },
                        )
                      }
                      licBmp?.let { bmp ->
                        Image(
                          bitmap = bmp,
                          contentDescription = "صورة الرخصة",
                          contentScale = ContentScale.Crop,
                          modifier =
                            Modifier
                              .weight(1f)
                              .height(84.dp)
                              .clip(MaterialTheme.shapes.small)
                              .clickable { inspectImagePreview = "رخصة القيادة • ${driver.fullName}" to bmp },
                        )
                      }
                    }
                    Text("اضغط على أي مستند للتكبير", style = MaterialTheme.typography.labelSmall, color = DRideRoyalBlue)
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (st != DriverApprovalStatus.APPROVED) {
                      Button(
                        onClick = { viewModel.approveDriver(driver.id) },
                        enabled = !actionState.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = DRideSuccessGreen),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("admin_approve_driver_${driver.id}"),
                      ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("اعتماد")
                      }
                    }
                    if (st == DriverApprovalStatus.PENDING_REVIEW) {
                      OutlinedButton(
                        onClick = { rejectDriverTarget = driver },
                        enabled = !actionState.isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
                        modifier = Modifier.weight(1f).height(48.dp),
                      ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("رفض")
                      }
                    }
                    if (st == DriverApprovalStatus.APPROVED) {
                      OutlinedButton(
                        onClick = { viewModel.suspendDriver(driver.id, "إيقاف إداري") },
                        enabled = !actionState.isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DRideErrorRed),
                        modifier = Modifier.weight(1f).height(48.dp),
                      ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("إيقاف")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Text("الشكاوى (${complaints.size})", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
          }
          items(complaints, key = { it.id }) { complaint ->
            Card(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${complaint.reporterName} • ${complaint.status}", style = MaterialTheme.typography.titleMedium)
                Text(complaint.reason, style = MaterialTheme.typography.bodyMedium)
                if (complaint.status == "open") {
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { viewModel.resolveComplaint(complaint.id, "resolved", "تمت المعالجة") },
                      modifier = Modifier.weight(1f),
                    ) { Text("تم الحل") }
                    OutlinedButton(
                      onClick = { viewModel.resolveComplaint(complaint.id, "dismissed", "تم الإغلاق") },
                      modifier = Modifier.weight(1f),
                    ) { Text("رفض الشكوى") }
                  }
                }
              }
            }
          }
          item {
            Text("أحدث الرحلات (${rides.size})", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
          }
          items(rides.take(25), key = { it.id }) { ride ->
            Card(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(ride.passengerName, style = MaterialTheme.typography.titleMedium)
                  Text(ride.resolvedStatus().arabicLabel(), style = MaterialTheme.typography.labelLarge, color = DRideRoyalBlue)
                }
                Text("من: ${ride.pickupAddress} → ${ride.destinationAddress}", style = MaterialTheme.typography.bodyMedium)
                Text("الأجرة: ${formatEgp(ride.agreedFareEgp ?: ride.proposedFareEgp)}", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
      3 -> {
        var rateInput by remember(commissionRule.commissionRatePercent) {
          mutableStateOf(commissionRule.commissionRatePercent.toString())
        }
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("نسبة عمولة المنصة (%)", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
                OutlinedTextField(
                  value = rateInput,
                  onValueChange = { rateInput = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("admin_commission_input"),
                )
                Button(
                  onClick = {
                    val v = rateInput.toDoubleOrNull() ?: return@Button
                    viewModel.updateCommissionRate(v)
                  },
                  enabled = !actionState.isBusy,
                  modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("حفظ نسبة العمولة") }
              }
            }
          }
          item {
            Text("سجل الإجراءات الإدارية (${auditLogs.size})", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
          }
          items(auditLogs.take(30), key = { it.id }) { log ->
            Card(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(log.action, style = MaterialTheme.typography.labelLarge, color = DRideDeepBlue)
                Text(log.details, style = MaterialTheme.typography.bodyMedium)
                Text(log.adminEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = DRideIceBlue),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(value, style = MaterialTheme.typography.headlineMedium, color = DRideDeepBlue, fontWeight = FontWeight.ExtraBold)
      Text(title, style = MaterialTheme.typography.labelLarge, color = DRideRoyalBlue)
    }
  }
}
