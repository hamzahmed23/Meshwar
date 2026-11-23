package com.example.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricRickshaw
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideVividBlue
import com.example.ui.theme.DRideWhite

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    AuthRepository.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      scope = scope,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
    )
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(listOf(DRideDeepBlue, DRideRoyalBlue, DRideVividBlue))
        )
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = CircleShape,
        color = DRideWhite.copy(alpha = 0.16f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = stringResource(R.string.cd_region_icon),
            tint = DRideWhite,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = stringResource(R.string.launch_region_badge),
            style = MaterialTheme.typography.labelLarge,
            color = DRideWhite,
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(28.dp),
          color = DRideDeepBlue,
          shadowElevation = 12.dp,
          modifier = Modifier.size(104.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Image(
              painter = painterResource(id = R.drawable.img_dride_emblem),
              contentDescription = stringResource(R.string.cd_dride_logo),
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = DRideWhite,
        )
        Text(
          text = stringResource(R.string.splash_tagline),
          style = MaterialTheme.typography.headlineMedium,
          color = DRideIceBlue,
          textAlign = TextAlign.Center,
        )
      }

      Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DRideWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = stringResource(R.string.auth_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            color = DRideDeepBlue,
            textAlign = TextAlign.Center,
          )
          Text(
            text = stringResource(R.string.auth_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            Icon(Icons.Rounded.ElectricRickshaw, contentDescription = null, tint = DRideRoyalBlue)
            Icon(Icons.Rounded.TwoWheeler, contentDescription = null, tint = DRideRoyalBlue)
            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = DRideRoyalBlue)
          }

          errorMessage?.let { msg ->
            Surface(
              shape = MaterialTheme.shapes.small,
              color = DRideErrorRed.copy(alpha = 0.1f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = DRideErrorRed,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
              )
            }
          }

          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              AuthRepository.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                onAuthCancelled = {
                  isLoading = false
                },
              )
            },
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = DRideRoyalBlue),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = DRideWhite,
                strokeWidth = 2.5.dp,
              )
            } else {
              Icon(Icons.Rounded.Login, contentDescription = null)
              Spacer(Modifier.size(10.dp))
              Text(
                text = stringResource(R.string.auth_google_button),
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = null,
              tint = DRideRoyalBlue,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = stringResource(R.string.auth_security_note),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
