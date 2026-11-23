package com.example.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.media.ImageEncodingHelper
import com.example.presentation.components.UserAvatarBadge
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideSurfaceTint
import kotlinx.coroutines.launch

@Composable
fun ProfileOnboardingScreen(
  initialName: String,
  initialEmail: String?,
  isSaving: Boolean,
  errorMessage: String?,
  onSaveProfile: (fullName: String, phone: String, photoBase64: String?) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var fullName by remember(initialName) { mutableStateOf(initialName) }
  var phone by remember { mutableStateOf("") }
  var photoBase64 by remember { mutableStateOf<String?>(null) }
  var localError by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val encoded = ImageEncodingHelper.encodeUriToCompactBase64(context, uri, 220, 70)
          if (encoded != null) {
            photoBase64 = encoded
            localError = null
          } else {
            localError = "تعذر ضغط الصورة المختارة، اختر صورة أصغر."
          }
        }
      }
    }

  Surface(
    color = DRideSurfaceTint,
    modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DRideDeepBlue,
            textAlign = TextAlign.Center,
          )
          Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          UserAvatarBadge(photoBase64 = photoBase64, size = 88.dp)

          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.testTag("onboarding_pick_photo_button"),
          ) {
            Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.btn_pick_photo))
          }

          OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.label_full_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
          )

          OutlinedTextField(
            value = phone,
            onValueChange = { input ->
              phone = input.filter { it.isDigit() || it == '+' }.take(15)
            },
            label = { Text(stringResource(R.string.label_phone_number)) },
            placeholder = { Text("01012345678") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("onboarding_phone_input"),
          )

          initialEmail?.takeIf { it.isNotBlank() }?.let { email ->
            Text(
              text = email,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          val displayError = localError ?: errorMessage
          displayError?.let { err ->
            Text(
              text = err,
              style = MaterialTheme.typography.bodyMedium,
              color = DRideErrorRed,
              textAlign = TextAlign.Center,
            )
          }

          Button(
            onClick = {
              val trimmedName = fullName.trim()
              val trimmedPhone = phone.trim()
              when {
                trimmedName.length < 2 -> localError = "يرجى إدخال الاسم الكامل"
                trimmedPhone.length < 10 -> localError = "يرجى إدخال رقم هاتف مصري صحيح"
                else -> {
                  localError = null
                  onSaveProfile(trimmedName, trimmedPhone, photoBase64)
                }
              }
            },
            enabled = !isSaving,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_save_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
              )
            } else {
              Icon(Icons.Rounded.Check, contentDescription = null)
              Spacer(Modifier.size(8.dp))
              Text(stringResource(R.string.btn_save_and_continue))
            }
          }

          TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.btn_logout))
          }
        }
      }
    }
  }
}
