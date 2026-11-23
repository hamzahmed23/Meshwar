package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlin.coroutines.resume

data class DayroutPlace(
  val title: String,
  val subtitle: String,
  val latitude: Double,
  val longitude: Double,
)

object DayroutGeoService {
  private val httpClient = OkHttpClient()

  // Real geographical coordinates for major Dayrout, Assiut landmarks and villages
  val curatedLandmarks: List<DayroutPlace> =
    listOf(
      DayroutPlace("محطة قطار ديروط", "وسط مدينة ديروط • أسيوط", 27.5561, 30.8075),
      DayroutPlace("ميدان المحطة", "شارع المحطة • ديروط", 27.5553, 30.8082),
      DayroutPlace("مستشفى ديروط المركزي", "طريق أسيوط الزراعي • ديروط", 27.5608, 30.8114),
      DayroutPlace("مجلس مدينة ديروط", "شارع بورسعيد • ديروط", 27.5579, 30.8096),
      DayroutPlace("كوبري ديروط على ترعة الإبراهيمية", "مدخل ديروط الشرقي", 27.5540, 30.8142),
      DayroutPlace("ديروط الشريف", "مركز ديروط • أسيوط", 27.5432, 30.7858),
      DayroutPlace("قرية صنبو", "مركز ديروط • أسيوط", 27.4986, 30.8184),
      DayroutPlace("قرية مسارة", "مركز ديروط • أسيوط", 27.5812, 30.7931),
      DayroutPlace("قرية دشلوط", "مركز ديروط • أسيوط", 27.5715, 30.7350),
      DayroutPlace("قرية أمشول", "مركز ديروط • أسيوط", 27.5210, 30.8240),
      DayroutPlace("نزلة عبد الله", "ديروط • أسيوط", 27.5655, 30.8215),
    )

  @SuppressLint("MissingPermission")
  suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
    return withContext(Dispatchers.IO) {
      try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()
        suspendCancellableCoroutine { cont ->
          client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { loc ->
              if (cont.isActive) {
                cont.resume(loc?.let { it.latitude to it.longitude })
              }
            }
            .addOnFailureListener {
              if (cont.isActive) cont.resume(null)
            }
          cont.invokeOnCancellation { tokenSource.cancel() }
        }
      } catch (_: Exception) {
        null
      }
    }
  }

  suspend fun searchPlaces(query: String): List<DayroutPlace> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.isEmpty()) return@withContext curatedLandmarks

      val localMatches =
        curatedLandmarks.filter {
          it.title.contains(trimmed, ignoreCase = true) ||
            it.subtitle.contains(trimmed, ignoreCase = true)
        }

      val remoteMatches =
        try {
          val encoded = URLEncoder.encode("$trimmed ديروط أسيوط", "UTF-8")
          val url =
            "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=6&countrycodes=eg&accept-language=ar"
          val req =
            Request.Builder()
              .url(url)
              .header("User-Agent", "D-RIDE-Dayrout-Android/1.0")
              .build()
          httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) emptyList()
            else {
              val body = response.body?.string().orEmpty()
              val arr = JSONArray(body)
              buildList {
                for (i in 0 until arr.length()) {
                  val obj = arr.getJSONObject(i)
                  val display = obj.optString("display_name")
                  val name = display.substringBefore(",").ifBlank { trimmed }
                  val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                  val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                  add(
                    DayroutPlace(
                      title = name,
                      subtitle = display.substringAfter(",", "ديروط • أسيوط").trim(),
                      latitude = lat,
                      longitude = lon,
                    )
                  )
                }
              }
            }
          }
        } catch (_: Exception) {
          emptyList()
        }

      (localMatches + remoteMatches).distinctBy { "${it.title}_${(it.latitude * 1000).roundToInt()}" }
    }

  suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
      // Check if close to a known Dayrout landmark first (< 250m)
      val nearest =
        curatedLandmarks.minByOrNull { calculateDistanceKm(lat, lng, it.latitude, it.longitude) }
      if (nearest != null && calculateDistanceKm(lat, lng, nearest.latitude, nearest.longitude) < 0.25) {
        return@withContext nearest.title
      }

      try {
        val geocoder = Geocoder(context, Locale("ar", "EG"))
        val addressText =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
              geocoder.getFromLocation(lat, lng, 1) { list ->
                val line =
                  list.firstOrNull()?.let { addr ->
                    listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality)
                      .filter { it.isNotBlank() }
                      .joinToString("، ")
                  }
                if (cont.isActive) cont.resume(line)
              }
            }
          } else {
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lng, 1)
            list?.firstOrNull()?.let { addr ->
              listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality)
                .filter { it.isNotBlank() }
                .joinToString("، ")
            }
          }
        if (!addressText.isNullOrBlank()) return@withContext addressText
      } catch (_: Exception) {
        // Fall through to coordinate label
      }

      "موقع محدد في ديروط (${String.format(Locale.US, "%.4f, %.4f", lat, lng)})"
    }

  fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
      sin(dLat / 2).pow(2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return ((r * c) * 10.0).roundToInt() / 10.0
  }
}
