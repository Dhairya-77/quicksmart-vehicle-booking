package com.quicksmart.android.reusable_methods

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.quicksmart.android.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class RouteResult(
    val points      : List<LatLng>,
    val distanceText: String?   = null,
    val durationText: String?   = null,
    val durationSec : Int       = 0,
    val errorMsg    : String?   = null
)

object DirectionsHelper {

    private const val TAG = "DirectionsHelper"

    private val executor    = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * @param onResult RouteResult containing points, distance, duration or error
     */
    fun getRoute(
        origin      : LatLng,
        destination : LatLng,
        onResult    : (RouteResult) -> Unit
    ) {
        executor.execute {
            val result = fetchRoute(origin, destination)
            mainHandler.post { onResult(result) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun fetchRoute(origin: LatLng, destination: LatLng): RouteResult {
        return try {
            val apiKey = BuildConfig.MAPS_API_KEY
            if (apiKey.isBlank()) {
                val msg = "API key is empty. Check local.properties → mapKey"
                return RouteResult(emptyList(), errorMsg = msg)
            }

            val urlStr = buildUrl(origin, destination, apiKey)
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout    = 15_000
            connection.requestMethod  = "GET"

            val httpCode = connection.responseCode
            val responseBody = if (httpCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            connection.disconnect()

            parseRoute(responseBody)
        } catch (e: Exception) {
            RouteResult(emptyList(), errorMsg = "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun buildUrl(origin: LatLng, destination: LatLng, apiKey: String): String =
        "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=driving" +
                "&key=$apiKey"

    private fun parseRoute(json: String): RouteResult {
        return try {
            if (json.isBlank()) return RouteResult(emptyList(), errorMsg = "Empty response from Directions API")

            val root   = JSONObject(json)
            val status = root.getString("status")

            if (status != "OK") {
                val errMsg = root.optString("error_message", "No error_message field")
                return RouteResult(emptyList(), errorMsg = "API status=$status | $errMsg")
            }

            val routes = root.getJSONArray("routes")
            if (routes.length() == 0) return RouteResult(emptyList(), errorMsg = "API returned 0 routes")

            val route = routes.getJSONObject(0)
            val leg   = route.getJSONArray("legs").getJSONObject(0)

            val distanceText = leg.getJSONObject("distance").getString("text")
            val durationText = leg.getJSONObject("duration").getString("text")
            val durationSec  = leg.getJSONObject("duration").getInt("value")

            val encodedPoly = route.getJSONObject("overview_polyline").getString("points")
            val points      = decodePolyline(encodedPoly)

            RouteResult(points, distanceText, durationText, durationSec)
        } catch (e: Exception) {
            RouteResult(emptyList(), errorMsg = "Parse error: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Standard Google encoded-polyline decoding algorithm. */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index  = 0
        val len    = encoded.length
        var lat    = 0
        var lng    = 0

        while (index < len) {
            var b: Int; var shift = 0; var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0; result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            points.add(LatLng(lat / 1e5, lng / 1e5))
        }
        return points
    }
}
