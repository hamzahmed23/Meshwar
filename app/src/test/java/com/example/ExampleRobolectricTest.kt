package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.LaunchConfig
import com.example.core.config.PaymentMethod
import com.example.domain.model.VehicleCategory
import com.example.presentation.splash.SplashScreen
import com.example.presentation.splash.SplashUiState
import com.example.ui.theme.DRideTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app name resource matches D-RIDE`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("D-RIDE", appName)
  }

  @Test
  fun `splash screen renders brand identity and launch region in ready state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val readyState =
      SplashUiState.Ready(
        launchCity = LaunchConfig.initialCity,
        supportedVehicles = listOf(VehicleCategory.TUK_TUK, VehicleCategory.MOTORCYCLE, VehicleCategory.CAR),
        paymentMethods = listOf(PaymentMethod.CASH),
      )

    composeTestRule.setContent {
      DRideTheme {
        SplashScreen(uiState = readyState)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("splash_screen_root").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_app_name").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_tagline").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_region_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_vehicle_categories").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splash_status_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText(context.getString(R.string.splash_ready_status)).assertIsDisplayed()
  }
}
