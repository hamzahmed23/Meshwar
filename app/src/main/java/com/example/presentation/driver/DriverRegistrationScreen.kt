package com.example.presentation.driver

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.ImageEncodingHelper
import com.example.data.model.DriverProfile
import com.example.data.model.UserProfile
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.VehicleCategory
import com.example.presentation.components.arabicLabel
import com.example.presentation.components.iconVector
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideWarningAmber
import kotlinx.coroutines.launch

@Composable
fun DriverApprovalGateScreen(
  existingProfile: DriverProfile?,
  userProfile: UserProfile,
  isSubmitting: Boolean,
  errorMessage: String?,
  onSubmitApplication: (
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
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val status = existingProfile?.resolvedApprovalStatus() ?: DriverApprovalStatus.NOT_APPLIED

  when (status) {
    DriverApprovalStatus.PENDING_REVIEW -> {
      DriverStatusNoticeCard(
        icon = Icons.Rounded.HourglassTop,
        tint = DRideWarningAmber,
        title = stringResource(R.string.driver_pending_title),
        body = stringResource(R.string.driver_pending_desc),
        profile = existingProfile,
        modifier = modifier,
      )
    }
    DriverApprovalStatus.SUSPENDED -> {
      DriverStatusNoticeCard(
        icon = Icons.Rounded.Block,
        tint = DRideErrorRed,
        title = "حساب السائق موقوف",
        body = existingProfile?.rejectionReason ?: "تم إيقاف حساب السائق من قبل الإدارة. تواصل مع الدعم لمراجعة الحساب.",
        profile = existingProfile,
        modifier = modifier,
      )
    }
    DriverApprovalStatus.REJECTED, DriverApprovalStatus.NOT_APPLIED -> {
      DriverRegistrationForm(
        existingProfile = existingProfile,
        userProfile = userProfile,
        isResubmit = status == DriverApprovalStatus.REJECTED,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        onSubmitApplication = onSubmitApplication,
        modifier = modifier,
      )
    }
    DriverApprovalStatus.APPROVED -> Unit
  }
}

@Composable
private fun DriverStatusNoticeCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: androidx.compose.ui.graphics.Color,
  title: String,
  body: String,
  profile: DriverProfile?,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = Modifier.fillMaxWidth().testTag("driver_approval_gate_card"),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = DRideDeepBlue, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

        profile?.let { p ->
          Surface(shape = MaterialTheme.shapes.medium, color = DRideIceBlue, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("الاسم: ${p.fullName}", style = MaterialTheme.typography.labelLarge, color = DRideDeepBlue)
              Text("المركبة: ${p.resolvedVehicleCategory().arabicLabel()} • ${p.vehicleModel} (${p.vehicleColor})", style = MaterialTheme.typography.bodyMedium)
              Text("اللوحة: ${p.vehiclePlate}", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DriverRegistrationForm(
  existingProfile: DriverProfile?,
  userProfile: UserProfile,
  isResubmit: Boolean,
  isSubmitting: Boolean,
  errorMessage: String?,
  onSubmitApplication: (
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
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var fullName by remember(existingProfile?.fullName, userProfile.fullName) {
    mutableStateOf(existingProfile?.fullName ?: userProfile.fullName)
  }
  var phone by remember(existingProfile?.phone, userProfile.phone) {
    mutableStateOf(existingProfile?.phone ?: userProfile.phone.orEmpty())
  }
  var nationalId by remember(existingProfile?.nationalId) { mutableStateOf(existingProfile?.nationalId.orEmpty()) }
  var vehicleCategory by remember(existingProfile?.vehicleCategory) {
    mutableStateOf(existingProfile?.resolvedVehicleCategory() ?: VehicleCategory.TUK_TUK)
  }
  var vehicleModel by remember(existingProfile?.vehicleModel) { mutableStateOf(existingProfile?.vehicleModel.orEmpty()) }
  var vehicleColor by remember(existingProfile?.vehicleColor) { mutableStateOf(existingProfile?.vehicleColor.orEmpty()) }
  var vehiclePlate by remember(existingProfile?.vehiclePlate) { mutableStateOf(existingProfile?.vehiclePlate.orEmpty()) }

  var profilePhotoBase64 by remember(existingProfile?.profilePhotoBase64) {
    mutableStateOf(existingProfile?.profilePhotoBase64 ?: userProfile.photoBase64)
  }
  var nationalIdPhotoBase64 by remember(existingProfile?.nationalIdPhotoBase64) {
    mutableStateOf(existingProfile?.nationalIdPhotoBase64)
  }
  var licensePhotoBase64 by remember(existingProfile?.licensePhotoBase64) {
    mutableStateOf(existingProfile?.licensePhotoBase64)
  }
  var localError by remember { mutableStateOf<String?>(null) }

  val pickProfilePhoto =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          ImageEncodingHelper.encodeUriToCompactBase64(context, uri, 240, 68)?.let {
            profilePhotoBase64 = it
            localError = null
          } ?: run { localError = "تعذر ضغط الصورة الشخصية" }
        }
      }
    }

  val pickIdPhoto =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          ImageEncodingHelper.encodeUriToCompactBase64(context, uri, 320, 65)?.let {
            nationalIdPhotoBase64 = it
            localError = null
          } ?: run { localError = "تعذر ضغط صورة البطاقة" }
        }
      }
    }

  val pickLicensePhoto =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          ImageEncodingHelper.encodeUriToCompactBase64(context, uri, 320, 65)?.let {
            licensePhotoBase64 = it
            localError = null
          } ?: run { localError = "تعذر ضغط صورة الرخصة" }
        }
      }
    }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (isResubmit) {
      Surface(
        shape = MaterialTheme.shapes.medium,
        color = DRideErrorRed.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("تم رفض الطلب السابق", style = MaterialTheme.typography.titleMedium, color = DRideErrorRed)
          Text(
            existingProfile?.rejectionReason ?: "يرجى مراجعة البيانات والمستندات وإعادة الإرسال.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = stringResource(R.string.driver_reg_title),
          style = MaterialTheme.typography.headlineMedium,
          color = DRideDeepBlue,
        )
        Text(
          text = "سجّل بياناتك ومركبتك في ديروط لتتم مراجعتها من الإدارة وتفعيل استقبال الرحلات.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text(stringResource(R.string.label_full_name)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("driver_reg_name_input"),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15) },
          label = { Text(stringResource(R.string.label_phone_number)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("driver_reg_phone_input"),
        )
        OutlinedTextField(
          value = nationalId,
          onValueChange = { nationalId = it.filter { c -> c.isDigit() }.take(14) },
          label = { Text("الرقم القومي (14 رقم)") },
          leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("driver_reg_nid_input"),
        )

        Text("نوع المركبة", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR).forEach { cat ->
            val selected = vehicleCategory == cat
            Card(
              onClick = { vehicleCategory = cat },
              shape = MaterialTheme.shapes.medium,
              colors = CardDefaults.cardColors(containerColor = if (selected) DRideIceBlue else MaterialTheme.colorScheme.surface),
              border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) DRideRoyalBlue else MaterialTheme.colorScheme.outline),
              modifier = Modifier.weight(1f),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(cat.iconVector(), contentDescription = cat.arabicLabel(), tint = if (selected) DRideDeepBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(cat.arabicLabel(), style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }

        OutlinedTextField(
          value = vehicleModel,
          onValueChange = { vehicleModel = it },
          label = { Text("نوع وموديل المركبة (مثال: باجاج 2023)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("driver_reg_model_input"),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = vehicleColor,
            onValueChange = { vehicleColor = it },
            label = { Text("اللون") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = vehiclePlate,
            onValueChange = { vehiclePlate = it },
            label = { Text("رقم اللوحة") },
            singleLine = true,
            modifier = Modifier.weight(1.2f).testTag("driver_reg_plate_input"),
          )
        }

        Text("المستندات والصور المطلوبة", style = MaterialTheme.typography.titleMedium, color = DRideDeepBlue)
        DocumentPickerRow(
          label = "صورة شخصية للسائق",
          base64 = profilePhotoBase64,
          onPick = {
            pickProfilePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
        )
        DocumentPickerRow(
          label = "صورة البطاقة الشخصية",
          base64 = nationalIdPhotoBase64,
          onPick = {
            pickIdPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
        )
        DocumentPickerRow(
          label = "صورة رخصة القيادة / تسيير المركبة",
          base64 = licensePhotoBase64,
          onPick = {
            pickLicensePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          },
        )

        val displayError = localError ?: errorMessage
        displayError?.let { err ->
          Text(err, style = MaterialTheme.typography.bodyMedium, color = DRideErrorRed)
        }

        Button(
          onClick = {
            val nid = nationalId.trim()
            when {
              fullName.trim().length < 2 -> localError = "الاسم الكامل مطلوب"
              phone.trim().length < 10 -> localError = "رقم الهاتف المصري مطلوب"
              nid.length != 14 -> localError = "الرقم القومي يجب أن يتكون من 14 رقماً"
              vehicleModel.trim().length < 2 -> localError = "موديل المركبة مطلوب"
              vehicleColor.trim().length < 2 -> localError = "لون المركبة مطلوب"
              vehiclePlate.trim().length < 3 -> localError = "رقم اللوحة مطلوب"
              nationalIdPhotoBase64 == null || licensePhotoBase64 == null ->
                localError = "يرجى إرفاق صورة البطاقة الشخصية ورخصة القيادة"
              else -> {
                localError = null
                onSubmitApplication(
                  fullName,
                  phone,
                  nid,
                  vehicleCategory,
                  vehicleModel,
                  vehicleColor,
                  vehiclePlate,
                  profilePhotoBase64,
                  nationalIdPhotoBase64,
                  licensePhotoBase64,
                  isResubmit,
                )
              }
            }
          },
          enabled = !isSubmitting,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("driver_reg_submit_button"),
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
          } else {
            Icon(Icons.Rounded.UploadFile, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(if (isResubmit) "إعادة إرسال الطلب" else "إرسال طلب التسجيل للمراجعة")
          }
        }
      }
    }
  }
}

@Composable
private fun DocumentPickerRow(
  label: String,
  base64: String?,
  onPick: () -> Unit,
) {
  val preview = remember(base64) { ImageEncodingHelper.decodeBase64ToImageBitmap(base64) }
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (preview != null) DRideIceBlue else MaterialTheme.colorScheme.surfaceVariant,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        if (preview != null) {
          Image(
            bitmap = preview,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small),
          )
        } else {
          Icon(Icons.Rounded.AddAPhoto, contentDescription = null, tint = DRideDeepBlue)
        }
        Column {
          Text(label, style = MaterialTheme.typography.labelLarge)
          Text(
            if (preview != null) "تم الإرفاق بنجاح" else "اضغط للاختيار من المعرض",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      OutlinedButton(onClick = onPick) {
        if (preview != null) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.size(4.dp))
          Text("تغيير")
        } else {
          Text("اختيار")
        }
      }
    }
  }
}
