package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverRepository
import com.example.data.repository.UserProfileRepository
import com.example.presentation.auth.AuthScreen
import com.example.presentation.auth.ProfileOnboardingScreen
import com.example.presentation.shell.DRideMainShell
import com.example.presentation.shell.DRideSessionViewModel
import com.example.presentation.shell.SessionUiState
import com.example.presentation.splash.SplashRoute
import com.example.presentation.splash.SplashUiState
import com.example.presentation.splash.SplashViewModel
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      DRideTheme {
        DRideRootApp()
      }
    }
  }
}

@Composable
private fun DRideRootApp() {
  val splashViewModel: SplashViewModel = viewModel()
  val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

  if (splashState is SplashUiState.Initializing) {
    SplashRoute(
      viewModel = splashViewModel,
      modifier = Modifier.fillMaxSize(),
    )
    return
  }

  val authStateFlow = remember { AuthRepository.authStateFlow() }
  val currentUser by authStateFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = {},
      modifier = Modifier.fillMaxSize(),
    )
  } else {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    key(user.uid) {
      val db = remember(context) {
        FirebaseFirestore.getInstance(
          context.applicationContext.getString(R.string.firestore_database_id)
        )
      }
      val sessionViewModel: DRideSessionViewModel = viewModel(
        key = "${user.uid}_session",
        factory = viewModelFactory {
          initializer {
            DRideSessionViewModel(
              userProfileRepository = UserProfileRepository(db),
              driverRepository = DriverRepository(db),
              currentUserId = user.uid,
            )
          }
        },
      )

      val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
      val isSavingOnboarding by sessionViewModel.isSavingOnboarding.collectAsStateWithLifecycle()
      val onboardingError by sessionViewModel.onboardingError.collectAsStateWithLifecycle()

      val handleSignOut: () -> Unit = {
        AuthRepository.signOut(credentialManager = credentialManager, scope = scope)
      }

      when (val state = sessionState) {
        SessionUiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        SessionUiState.NeedsOnboarding -> {
          ProfileOnboardingScreen(
            initialName = user.displayName.orEmpty(),
            initialEmail = user.email,
            isSaving = isSavingOnboarding,
            errorMessage = onboardingError,
            onSaveProfile = { fullName, phone, photoBase64 ->
              sessionViewModel.createInitialProfile(
                fullName = fullName,
                phone = phone,
                email = user.email,
                photoBase64 = photoBase64,
              )
            },
            onSignOut = handleSignOut,
          )
        }
        is SessionUiState.Ready -> {
          DRideMainShell(
            userProfile = state.userProfile,
            currentUserId = user.uid,
            sessionViewModel = sessionViewModel,
            onSignOut = handleSignOut,
          )
        }
        is SessionUiState.Error -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            androidx.compose.foundation.layout.Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
              Text(state.message, color = DRideErrorRed, textAlign = TextAlign.Center)
              Button(onClick = handleSignOut) {
                Text("تسجيل الخروج والمحاولة مجدداً")
              }
            }
          }
        }
      }
    }
  }
}
