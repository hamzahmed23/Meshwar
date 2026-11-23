package com.example.presentation.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.data.repository.DriverRepository
import com.example.data.repository.RideRepository
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.DriverApprovalStatus
import com.example.domain.model.OperatingMode
import com.example.domain.model.RideStatus
import com.example.presentation.admin.AdminDashboardScreen
import com.example.presentation.admin.AdminViewModel
import com.example.presentation.driver.DriverActiveRideScreen
import com.example.presentation.driver.DriverApprovalGateScreen
import com.example.presentation.driver.DriverEarningsScreen
import com.example.presentation.driver.DriverHomeScreen
import com.example.presentation.driver.DriverProfileDetailsScreen
import com.example.presentation.driver.DriverProfileUiState
import com.example.presentation.driver.DriverViewModel
import com.example.presentation.passenger.PassengerActiveRideScreen
import com.example.presentation.passenger.PassengerHistoryScreen
import com.example.presentation.passenger.PassengerHomeScreen
import com.example.presentation.passenger.PassengerMatchingScreen
import com.example.presentation.passenger.PassengerProfileScreen
import com.example.presentation.passenger.PassengerRideCompletionScreen
import com.example.presentation.passenger.PassengerRidesUiState
import com.example.presentation.passenger.PassengerViewModel
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideErrorRed
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideWhite
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DRideMainShell(
  userProfile: UserProfile,
  currentUserId: String,
  sessionViewModel: DRideSessionViewModel,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (userProfile.suspended) {
    SuspendedAccountScreen(onSignOut = onSignOut, modifier = modifier)
    return
  }

  val context = LocalContext.current
  val db = remember(context) {
    FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
  }

  val selectedTopMode by sessionViewModel.selectedTopMode.collectAsStateWithLifecycle()
  val activeTopMode =
    selectedTopMode
      ?: if (userProfile.resolvedMode() == OperatingMode.DRIVER_MODE) ShellTopMode.DRIVER
      else ShellTopMode.PASSENGER

  var passengerTab by remember { mutableIntStateOf(0) }
  var driverTab by remember { mutableIntStateOf(0) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Sub-tab BackHandler support
  if (activeTopMode == ShellTopMode.PASSENGER && passengerTab != 0) {
    BackHandler { passengerTab = 0 }
  } else if (activeTopMode == ShellTopMode.DRIVER && driverTab != 0) {
    BackHandler { driverTab = 0 }
  } else if (activeTopMode == ShellTopMode.ADMIN) {
    BackHandler { sessionViewModel.switchMode(ShellTopMode.PASSENGER, userProfile) }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Surface(color = DRideDeepBlue, shadowElevation = 4.dp) {
        Column(
          modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "D-RIDE • ديروط",
                style = MaterialTheme.typography.titleLarge,
                color = DRideWhite,
                fontWeight = FontWeight.ExtraBold,
              )
              Text(
                text = "أهلاً ${userProfile.fullName}",
                style = MaterialTheme.typography.labelSmall,
                color = DRideIceBlue,
              )
            }
            com.example.presentation.components.UserAvatarBadge(
              photoBase64 = userProfile.photoBase64,
              size = 40.dp,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ModeSwitcherChip(
              label = stringResource(R.string.mode_passenger),
              selected = activeTopMode == ShellTopMode.PASSENGER,
              onClick = { sessionViewModel.switchMode(ShellTopMode.PASSENGER, userProfile) },
              modifier = Modifier.testTag("mode_switch_passenger"),
            )
            ModeSwitcherChip(
              label = stringResource(R.string.mode_driver),
              selected = activeTopMode == ShellTopMode.DRIVER,
              onClick = { sessionViewModel.switchMode(ShellTopMode.DRIVER, userProfile) },
              modifier = Modifier.testTag("mode_switch_driver"),
            )
            if (sessionViewModel.isAdmin) {
              ModeSwitcherChip(
                label = stringResource(R.string.mode_admin),
                selected = activeTopMode == ShellTopMode.ADMIN,
                onClick = { sessionViewModel.switchMode(ShellTopMode.ADMIN, userProfile) },
                modifier = Modifier.testTag("mode_switch_admin"),
              )
            }
          }
        }
      }
    },
    bottomBar = {
      when (activeTopMode) {
        ShellTopMode.PASSENGER -> {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            NavigationBarItem(
              selected = passengerTab == 0,
              onClick = { passengerTab = 0 },
              icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_home)) },
              modifier = Modifier.testTag("nav_passenger_home"),
            )
            NavigationBarItem(
              selected = passengerTab == 1,
              onClick = { passengerTab = 1 },
              icon = { Icon(Icons.Rounded.History, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_history)) },
              modifier = Modifier.testTag("nav_passenger_history"),
            )
            NavigationBarItem(
              selected = passengerTab == 2,
              onClick = { passengerTab = 2 },
              icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_profile)) },
              modifier = Modifier.testTag("nav_passenger_profile"),
            )
          }
        }
        ShellTopMode.DRIVER -> {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            NavigationBarItem(
              selected = driverTab == 0,
              onClick = { driverTab = 0 },
              icon = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_driver_requests)) },
              modifier = Modifier.testTag("nav_driver_home"),
            )
            NavigationBarItem(
              selected = driverTab == 1,
              onClick = { driverTab = 1 },
              icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_driver_earnings)) },
              modifier = Modifier.testTag("nav_driver_earnings"),
            )
            NavigationBarItem(
              selected = driverTab == 2,
              onClick = { driverTab = 2 },
              icon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
              label = { Text(stringResource(R.string.nav_driver_profile)) },
              modifier = Modifier.testTag("nav_driver_profile"),
            )
          }
        }
        ShellTopMode.ADMIN -> Unit
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (activeTopMode) {
        ShellTopMode.PASSENGER -> {
          val passengerVm: PassengerViewModel = viewModel(
            key = "${currentUserId}_passenger",
            factory = viewModelFactory {
              initializer {
                PassengerViewModel(
                  rideRepository = RideRepository(db),
                  driverRepository = DriverRepository(db),
                  userProfileRepository = UserProfileRepository(db),
                  currentUserId = currentUserId,
                )
              }
            },
          )
          PassengerModeContent(
            userProfile = userProfile,
            selectedTab = passengerTab,
            viewModel = passengerVm,
            snackbarHostState = snackbarHostState,
            onNavigateToHome = { passengerTab = 0 },
            onSignOut = onSignOut,
          )
        }
        ShellTopMode.DRIVER -> {
          val driverVm: DriverViewModel = viewModel(
            key = "${currentUserId}_driver",
            factory = viewModelFactory {
              initializer {
                DriverViewModel(
                  driverRepository = DriverRepository(db),
                  rideRepository = RideRepository(db),
                  currentDriverId = currentUserId,
                )
              }
            },
          )
          DriverModeContent(
            userProfile = userProfile,
            selectedTab = driverTab,
            viewModel = driverVm,
            snackbarHostState = snackbarHostState,
            onSignOut = onSignOut,
          )
        }
        ShellTopMode.ADMIN -> {
          if (sessionViewModel.isAdmin) {
            val adminVm: AdminViewModel = viewModel(
              key = "${currentUserId}_admin",
              factory = viewModelFactory {
                initializer { AdminViewModel(AdminRepository(db)) }
              },
            )
            AdminDashboardScreen(viewModel = adminVm)
          }
        }
      }
    }
  }
}

@Composable
private fun PassengerModeContent(
  userProfile: UserProfile,
  selectedTab: Int,
  viewModel: PassengerViewModel,
  snackbarHostState: SnackbarHostState,
  onNavigateToHome: () -> Unit,
  onSignOut: () -> Unit,
) {
  val draft by viewModel.draft.collectAsStateWithLifecycle()
  val ridesState by viewModel.ridesState.collectAsStateWithLifecycle()
  val onlineDrivers by viewModel.onlineDrivers.collectAsStateWithLifecycle()
  val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
  val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
  val activeOffers by viewModel.activeOffers.collectAsStateWithLifecycle()
  val activeRideContact by viewModel.activeRideContact.collectAsStateWithLifecycle()

  LaunchedEffect(draft.actionSuccess, draft.actionError) {
    val msg = draft.actionSuccess ?: draft.actionError
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  when (selectedTab) {
    0 -> {
      val ride = activeRide
      if (ride == null) {
        PassengerHomeScreen(
          userProfile = userProfile,
          draft = draft,
          onlineDrivers = onlineDrivers,
          savedPlaces = savedPlaces,
          viewModel = viewModel,
        )
      } else {
        when (ride.resolvedStatus()) {
          RideStatus.REQUESTED, RideStatus.SEARCHING -> {
            PassengerMatchingScreen(
              ride = ride,
              offers = activeOffers,
              onAcceptOffer = { offer -> viewModel.acceptOffer(ride.id, offer) },
              onRejectOffer = { offer -> viewModel.rejectOffer(ride.id, offer.id) },
              onCancelRide = { viewModel.cancelRide(ride) },
            )
          }
          RideStatus.OFFER_SELECTED,
          RideStatus.DRIVER_ARRIVING,
          RideStatus.DRIVER_ARRIVED,
          RideStatus.IN_PROGRESS -> {
            PassengerActiveRideScreen(
              ride = ride,
              contact = activeRideContact,
              userProfile = userProfile,
              onCancelRide = { viewModel.cancelRide(ride) },
              onSubmitComplaint = { reason ->
                viewModel.submitComplaint(ride.id, userProfile.fullName, reason)
              },
            )
          }
          RideStatus.COMPLETED -> {
            PassengerRideCompletionScreen(
              ride = ride,
              onConfirmRating = { rating, feedback ->
                viewModel.submitRating(ride, rating, feedback)
              },
            )
          }
          else -> {
            PassengerHomeScreen(
              userProfile = userProfile,
              draft = draft,
              onlineDrivers = onlineDrivers,
              savedPlaces = savedPlaces,
              viewModel = viewModel,
            )
          }
        }
      }
    }
    1 ->
      PassengerHistoryScreen(
        ridesState = ridesState,
        onRebookRide = { ride ->
          viewModel.rebookRide(ride)
          onNavigateToHome()
        },
      )
    2 -> {
      val totalCount = (ridesState as? PassengerRidesUiState.Success)?.rides?.size ?: 0
      PassengerProfileScreen(
        userProfile = userProfile,
        ridesCount = totalCount,
        onUpdateProfile = { name, phone, photo -> viewModel.updateUserProfile(name, phone, photo) },
        onSignOut = onSignOut,
      )
    }
  }
}

@Composable
private fun DriverModeContent(
  userProfile: UserProfile,
  selectedTab: Int,
  viewModel: DriverViewModel,
  snackbarHostState: SnackbarHostState,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val profileState by viewModel.driverProfileState.collectAsStateWithLifecycle()
  val actionState by viewModel.actionState.collectAsStateWithLifecycle()
  val searchingRides by viewModel.searchingRides.collectAsStateWithLifecycle()
  val activeRide by viewModel.activeAssignedRide.collectAsStateWithLifecycle()
  val activeContact by viewModel.activeRideContact.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()

  LaunchedEffect(actionState.successMessage, actionState.errorMessage) {
    val msg = actionState.successMessage ?: actionState.errorMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  when (val state = profileState) {
    DriverProfileUiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is DriverProfileUiState.Error -> {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(state.message, color = DRideErrorRed, textAlign = TextAlign.Center)
      }
    }
    is DriverProfileUiState.Ready -> {
      val driverProfile = state.profile
      if (driverProfile == null || driverProfile.resolvedApprovalStatus() != DriverApprovalStatus.APPROVED) {
        DriverApprovalGateScreen(
          existingProfile = driverProfile,
          userProfile = userProfile,
          isSubmitting = actionState.isSubmittingApplication,
          errorMessage = actionState.errorMessage,
          onSubmitApplication = { fullName, phone, nid, cat, model, color, plate, pPhoto, idPhoto, licPhoto, resubmit ->
            viewModel.submitApplication(fullName, phone, nid, cat, model, color, plate, pPhoto, idPhoto, licPhoto, resubmit)
          },
        )
      } else {
        when (selectedTab) {
          0 -> {
            val ride = activeRide
            if (ride != null) {
              DriverActiveRideScreen(
                ride = ride,
                driverProfile = driverProfile,
                contact = activeContact,
                actionState = actionState,
                onAdvanceStatus = { target ->
                  viewModel.advanceRideStatus(context, ride, driverProfile, target)
                },
              )
            } else {
              DriverHomeScreen(
                driverProfile = driverProfile,
                actionState = actionState,
                searchingRides = searchingRides,
                viewModel = viewModel,
              )
            }
          }
          1 -> DriverEarningsScreen(driverProfile = driverProfile, transactions = transactions)
          2 -> DriverProfileDetailsScreen(driverProfile = driverProfile, onSignOut = onSignOut)
        }
      }
    }
  }
}

@Composable
private fun ModeSwitcherChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(label, maxLines = 1) },
    colors =
      FilterChipDefaults.filterChipColors(
        selectedContainerColor = DRideWhite,
        selectedLabelColor = DRideDeepBlue,
        containerColor = DRideRoyalBlue,
        labelColor = DRideWhite,
      ),
    modifier = modifier,
  )
}

@Composable
private fun SuspendedAccountScreen(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Card(
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(Icons.Rounded.Block, contentDescription = null, tint = DRideErrorRed, modifier = Modifier.size(56.dp))
        Text("تم إيقاف الحساب", style = MaterialTheme.typography.headlineMedium, color = DRideErrorRed)
        Text(
          "تم تعليق هذا الحساب من قِبل الإدارة. يمكنك التواصل مع فريق الدعم في ديروط للمراجعة.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.btn_logout))
        }
      }
    }
  }
}
