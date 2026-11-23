package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DRideLightColorScheme =
  lightColorScheme(
    primary = DRideRoyalBlue,
    onPrimary = DRideWhite,
    primaryContainer = DRideIceBlue,
    onPrimaryContainer = DRideDeepBlue,
    secondary = DRideVividBlue,
    onSecondary = DRideWhite,
    secondaryContainer = DRideSurfaceTint,
    onSecondaryContainer = DRideDeepBlue,
    tertiary = DRideSkyBlue,
    onTertiary = DRideInk,
    background = DRideWhite,
    onBackground = DRideInk,
    surface = DRideWhite,
    onSurface = DRideInk,
    surfaceVariant = DRideSurfaceTint,
    onSurfaceVariant = DRideSlate,
    outline = DRideOutline,
    error = DRideErrorRed,
    onError = DRideWhite,
  )

private val DRideDarkColorScheme =
  darkColorScheme(
    primary = DRideSkyBlue,
    onPrimary = DRideDarkBackground,
    primaryContainer = DRideDeepBlue,
    onPrimaryContainer = DRideWhite,
    secondary = DRideVividBlue,
    onSecondary = DRideWhite,
    secondaryContainer = DRideDarkSurfaceVariant,
    onSecondaryContainer = DRideIceBlue,
    background = DRideDarkBackground,
    onBackground = DRideWhite,
    surface = DRideDarkSurface,
    onSurface = DRideWhite,
    surfaceVariant = DRideDarkSurfaceVariant,
    onSurfaceVariant = DRideIceBlue,
    outline = DRideRoyalBlue,
    error = DRideErrorRed,
    onError = DRideWhite,
  )

val DRideShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun DRideTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  layoutDirection: LayoutDirection = LayoutDirection.Rtl,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DRideDarkColorScheme else DRideLightColorScheme

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = DRideTypography,
      shapes = DRideShapes,
      content = content,
    )
  }
}
