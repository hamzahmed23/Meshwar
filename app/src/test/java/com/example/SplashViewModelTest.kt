package com.example

import com.example.core.config.PaymentMethod
import com.example.data.repository.LocalLaunchConfigRepository
import com.example.domain.model.VehicleCategory
import com.example.presentation.splash.SplashUiState
import com.example.presentation.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `splash viewModel transitions from Initializing to Ready with Dayrout config`() =
    runTest(testDispatcher) {
      val viewModel =
        SplashViewModel(
          configRepository = LocalLaunchConfigRepository(),
          splashInitDelayMs = 100L,
        )

      assertEquals(SplashUiState.Initializing, viewModel.uiState.value)

      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertTrue(state is SplashUiState.Ready)
      val ready = state as SplashUiState.Ready
      assertEquals("dayrout", ready.launchCity.id)
      assertEquals(listOf(PaymentMethod.CASH), ready.paymentMethods)
      assertEquals(
        listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR),
        ready.supportedVehicles,
      )
    }
}
