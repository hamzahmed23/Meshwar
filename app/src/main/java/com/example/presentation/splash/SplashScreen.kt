package com.example.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricRickshaw
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TwoWheeler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.VehicleCategory
import com.example.ui.theme.DRideDeepBlue
import com.example.ui.theme.DRideIceBlue
import com.example.ui.theme.DRideRoyalBlue
import com.example.ui.theme.DRideSuccessGreen
import com.example.ui.theme.DRideVividBlue
import com.example.ui.theme.DRideWhite

@Composable
fun SplashRoute(
  viewModel: SplashViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  SplashScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun SplashScreen(
  uiState: SplashUiState,
  modifier: Modifier = Modifier,
) {
  var revealed by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { revealed = true }

  val logoScale by
    animateFloatAsState(
      targetValue = if (revealed) 1f else 0.84f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
      label = "logo_scale",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("splash_screen_root")
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(DRideDeepBlue, DRideRoyalBlue, DRideVividBlue),
            ),
        ),
  ) {
    SplashRouteAtmosphereCanvas(modifier = Modifier.fillMaxSize())

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top region launch pill
      AnimatedVisibility(
        visible = revealed,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
      ) {
        Surface(
          shape = CircleShape,
          color = DRideWhite.copy(alpha = 0.16f),
          modifier = Modifier.testTag("splash_region_badge"),
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
      }

      // Center brand identity hero
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 24.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(32.dp),
          color = DRideDeepBlue,
          shadowElevation = 16.dp,
          tonalElevation = 8.dp,
          modifier =
            Modifier
              .size(124.dp)
              .scale(logoScale)
              .testTag("splash_logo"),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .background(
                  brush =
                    Brush.linearGradient(
                      colors = listOf(DRideDeepBlue, DRideRoyalBlue),
                    ),
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_dride_emblem),
              contentDescription = stringResource(R.string.cd_dride_logo),
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        AnimatedVisibility(
          visible = revealed,
          enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
              color = DRideWhite,
              modifier = Modifier.testTag("splash_app_name"),
            )
            Text(
              text = stringResource(R.string.brand_name_arabic),
              style = MaterialTheme.typography.titleMedium,
              color = DRideIceBlue,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.splash_tagline),
              style = MaterialTheme.typography.headlineMedium,
              color = DRideWhite,
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag("splash_tagline"),
            )
            Text(
              text = stringResource(R.string.splash_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = DRideWhite.copy(alpha = 0.86f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      // Bottom white brand summary & readiness card
      AnimatedVisibility(
        visible = revealed,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
      ) {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = DRideWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
          modifier =
            Modifier
              .widthIn(max = 540.dp)
              .fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            // Vehicle categories preview row
            Row(
              modifier = Modifier.fillMaxWidth().testTag("splash_vehicle_categories"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              VehicleBadgeItem(
                icon = Icons.Rounded.ElectricRickshaw,
                label = stringResource(R.string.cd_vehicle_tuktuk),
                modifier = Modifier.weight(1f),
              )
              VehicleBadgeItem(
                icon = Icons.Rounded.TwoWheeler,
                label = stringResource(R.string.cd_vehicle_motorcycle),
                modifier = Modifier.weight(1f),
              )
              VehicleBadgeItem(
                icon = Icons.Rounded.DirectionsCar,
                label = stringResource(R.string.cd_vehicle_car),
                modifier = Modifier.weight(1f),
              )
            }

            // Currency & Cash info row
            Surface(
              shape = MaterialTheme.shapes.small,
              color = DRideIceBlue,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = null,
                    tint = DRideDeepBlue,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = stringResource(R.string.payment_cash_only),
                    style = MaterialTheme.typography.labelLarge,
                    color = DRideDeepBlue,
                  )
                }
                Text(
                  text = stringResource(R.string.currency_egp),
                  style = MaterialTheme.typography.labelLarge,
                  color = DRideRoyalBlue,
                )
              }
            }

            // Live Phase 1 readiness status indicator
            SplashStatusFooter(uiState = uiState)
          }
        }
      }
    }
  }
}

@Composable
private fun VehicleBadgeItem(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = DRideRoyalBlue,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun SplashStatusFooter(uiState: SplashUiState) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .testTag("splash_status_indicator"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    when (uiState) {
      SplashUiState.Initializing -> {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.5.dp,
          color = DRideRoyalBlue,
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
          text = stringResource(R.string.splash_loading_status),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      is SplashUiState.Ready -> {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = DRideSuccessGreen,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = stringResource(R.string.splash_ready_status),
          style = MaterialTheme.typography.labelLarge,
          color = DRideDeepBlue,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun SplashRouteAtmosphereCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Soft atmospheric radial glow behind logo
    drawCircle(
      color = Color.White.copy(alpha = 0.08f),
      radius = w * 0.55f,
      center = Offset(w * 0.5f, h * 0.36f),
    )
    drawCircle(
      color = Color.White.copy(alpha = 0.05f),
      radius = w * 0.35f,
      center = Offset(w * 0.15f, h * 0.15f),
    )

    // Stylized curved transit route lines
    val routePathPrimary =
      Path().apply {
        moveTo(-w * 0.1f, h * 0.62f)
        cubicTo(
          w * 0.25f,
          h * 0.52f,
          w * 0.65f,
          h * 0.72f,
          w * 1.15f,
          h * 0.48f,
        )
      }
    drawPath(
      path = routePathPrimary,
      color = Color.White.copy(alpha = 0.12f),
      style = Stroke(width = 6f, cap = StrokeCap.Round),
    )

    val routePathSecondary =
      Path().apply {
        moveTo(-w * 0.05f, h * 0.22f)
        cubicTo(
          w * 0.35f,
          h * 0.14f,
          w * 0.72f,
          h * 0.30f,
          w * 1.1f,
          h * 0.18f,
        )
      }
    drawPath(
      path = routePathSecondary,
      color = Color.White.copy(alpha = 0.09f),
      style = Stroke(width = 4f, cap = StrokeCap.Round),
    )
  }
}
