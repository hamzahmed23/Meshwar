package com.example.presentation.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.config.LaunchConfig
import com.example.data.model.DriverPresence
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun DayroutMapView(
  centerLat: Double = LaunchConfig.initialCity.centerLatitude,
  centerLng: Double = LaunchConfig.initialCity.centerLongitude,
  pickupPoint: Pair<Double, Double>? = null,
  destinationPoint: Pair<Double, Double>? = null,
  driverPoint: Pair<Double, Double>? = null,
  onlineDrivers: List<DriverPresence> = emptyList(),
  onMapTapped: ((Double, Double) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val latestOnMapTapped by rememberUpdatedState(onMapTapped)

  val mapView = remember {
    Configuration.getInstance().apply {
      userAgentValue = context.packageName
      osmdroidBasePath = context.cacheDir
      osmdroidTileCache = context.cacheDir.resolve("osmdroid")
    }
    MapView(context).apply {
      setTileSource(TileSourceFactory.MAPNIK)
      setMultiTouchControls(true)
      controller.setZoom(15.0)
      controller.setCenter(GeoPoint(centerLat, centerLng))
    }
  }

  DisposableEffect(lifecycleOwner, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      mapView.onDetach()
    }
  }

  Box(modifier = modifier) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize(),
      update = { view ->
        view.overlays.clear()

        if (latestOnMapTapped != null) {
          view.overlays.add(
            MapEventsOverlay(
              object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                  if (p != null) {
                    latestOnMapTapped?.invoke(p.latitude, p.longitude)
                  }
                  return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
              }
            )
          )
        }

        if (pickupPoint != null && destinationPoint != null) {
          val line =
            Polyline(view).apply {
              setPoints(
                listOf(
                  GeoPoint(pickupPoint.first, pickupPoint.second),
                  GeoPoint(destinationPoint.first, destinationPoint.second),
                )
              )
              outlinePaint.color = AndroidColor.parseColor("#0D5BD6")
              outlinePaint.strokeWidth = 9f
              outlinePaint.strokeCap = Paint.Cap.ROUND
            }
          view.overlays.add(line)
        }

        onlineDrivers.forEach { d ->
          val marker =
            Marker(view).apply {
              position = GeoPoint(d.latitude, d.longitude)
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = createCircleBadgeDrawable(AndroidColor.parseColor("#3B82F6"), 28)
              title = "سائق متاح"
            }
          view.overlays.add(marker)
        }

        pickupPoint?.let { (lat, lng) ->
          val marker =
            Marker(view).apply {
              position = GeoPoint(lat, lng)
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = createCircleBadgeDrawable(AndroidColor.parseColor("#0D5BD6"), 40)
              title = "نقطة الانطلاق"
            }
          view.overlays.add(marker)
        }

        destinationPoint?.let { (lat, lng) ->
          val marker =
            Marker(view).apply {
              position = GeoPoint(lat, lng)
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = createCircleBadgeDrawable(AndroidColor.parseColor("#0F9D58"), 40)
              title = "الوجهة"
            }
          view.overlays.add(marker)
        }

        driverPoint?.let { (lat, lng) ->
          val marker =
            Marker(view).apply {
              position = GeoPoint(lat, lng)
              setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
              icon = createCircleBadgeDrawable(AndroidColor.parseColor("#064191"), 44)
              title = "موقع السائق"
            }
          view.overlays.add(marker)
        }

        view.controller.animateTo(GeoPoint(centerLat, centerLng))
        view.invalidate()
      },
    )
  }
}

private fun createCircleBadgeDrawable(fillColor: Int, sizePx: Int): GradientDrawable =
  GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(fillColor)
    setStroke(5, AndroidColor.WHITE)
    setSize(sizePx, sizePx)
  }
