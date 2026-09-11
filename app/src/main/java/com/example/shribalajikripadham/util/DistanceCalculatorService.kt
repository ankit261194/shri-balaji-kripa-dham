package com.example.shribalajikripadham.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

/**
 * Result data class for calculated road distance
 */
data class DistanceResult(
    val distanceKm: Float,
    val isEstimated: Boolean = false,
    val origin: String = "",
    val destination: String = DistanceCalculatorService.DESTINATION_NAME
)

/**
 * Service to calculate real road distance in kilometers between devotee's origin location
 * and Shri Balaji Kripa Dham, Dungra Jaat (Bulandshahr, UP).
 *
 * Fixed Destination Coordinates:
 * Latitude: 28.4089 N, Longitude: 77.8789 E
 */
object DistanceCalculatorService {
    private const val TAG = "DistanceCalculator"

    const val DESTINATION_NAME = "श्री बालाजी कृपा धाम, डुंगरा जाट (बुलंदशहर)"
    const val DESTINATION_LAT = 28.4089
    const val DESTINATION_LNG = 77.8789

    // Pre-mapped driving distances (km) for instant offline calculation and fallback
    private val PRE_MAPPED_ROAD_DISTANCES: Map<String, Float> = mapOf(
        "डुंगरा जाट" to 0f,
        "डूँगरा जाट" to 0f,
        "डूँगरा जाट (स्थानीय)" to 0f,
        "dungra jaat" to 0f,
        "dungra" to 0f,
        "शिकारपुर" to 14f,
        "shikarpur" to 14f,
        "बुलंदशहर" to 22f,
        "bulandshahr" to 22f,
        "बुलन्दशहर" to 22f,
        "jahangirabad" to 24f,
        "जहाँगीराबाद" to 24f,
        "खुर्जा" to 38f,
        "khurja" to 38f,
        "अनूपशहर" to 36f,
        "anupshahr" to 36f,
        "स्याना" to 35f,
        "syana" to 35f,
        "गुलावठी" to 42f,
        "gulawati" to 42f,
        "dibai" to 44f,
        "डिबाई" to 44f,
        "अतरौली" to 48f,
        "atrauli" to 48f,
        "सिकंदराबाद" to 45f,
        "sikandrabad" to 45f,
        "हापुड़" to 58f,
        "hapur" to 58f,
        "अलीगढ़" to 55f,
        "aligarh" to 55f,
        "मेरठ" to 85f,
        "meerut" to 85f,
        "ग्रेटर नोएडा" to 68f,
        "greater noida" to 68f,
        "नोएडा" to 82f,
        "noida" to 82f,
        "गाजियाबाद" to 78f,
        "ghaziabad" to 78f,
        "दिल्ली" to 95f,
        "delhi" to 95f,
        "नई दिल्ली" to 98f,
        "new delhi" to 98f,
        "फरीदाबाद" to 85f,
        "faridabad" to 85f,
        "पलवल" to 75f,
        "palwal" to 75f,
        "मथुरा" to 110f,
        "mathura" to 110f,
        "वृंदावन" to 105f,
        "vrindavan" to 105f,
        "हाथरस" to 82f,
        "hathras" to 82f,
        "कासगंज" to 95f,
        "kasganj" to 95f,
        "बदायूं" to 125f,
        "budaun" to 125f,
        "मुरादाबाद" to 115f,
        "moradabad" to 115f,
        "संभल" to 85f,
        "sambhal" to 85f,
        "आगरा" to 155f,
        "agra" to 155f,
        "बरेली" to 175f,
        "bareilly" to 175f,
        "सहारनपुर" to 195f,
        "saharanpur" to 195f,
        "मुजफ्फरनगर" to 135f,
        "muzaffarnagar" to 135f,
        "बिजनौर" to 140f,
        "bijnor" to 140f,
        "जयपुर" to 310f,
        "jaipur" to 310f,
        "लखनऊ" to 420f,
        "lucknow" to 420f,
        "कानपुर" to 380f,
        "kanpur" to 380f,
        "हरिद्वार" to 220f,
        "haridwar" to 220f,
        "ऋषिकेश" to 245f,
        "rishikesh" to 245f,
        "देहरादून" to 275f,
        "dehradun" to 275f,
        "गुरुग्राम" to 110f,
        "gurugram" to 110f,
        "गुड़गांव" to 110f,
        "gurgaon" to 110f
    )

    private val customCityDistancesCache = java.util.concurrent.ConcurrentHashMap<String, Float>()

    fun registerCustomDistance(cityName: String, distanceKm: Float) {
        customCityDistancesCache[cityName.trim().lowercase()] = distanceKm
    }

    fun clearCustomDistances() {
        customCityDistancesCache.clear()
    }

    fun loadCustomDistances(list: List<Pair<String, Float>>) {
        for ((name, dist) in list) {
            customCityDistancesCache[name.trim().lowercase()] = dist
        }
    }

    /**
     * Resolve driving distance with origin query and optional GPS device coordinates fallback.
     */
    suspend fun resolveDrivingDistance(
        origin: String,
        deviceLat: Double? = null,
        deviceLng: Double? = null
    ): DistanceResult = withContext(Dispatchers.IO) {
        val trimmed = origin.trim()
        val lower = trimmed.lowercase()

        // 0. Check Super Admin Custom Added Villages / Cities first
        for ((customKey, customDist) in customCityDistancesCache) {
            if (lower == customKey || lower.contains(customKey) || customKey.contains(lower)) {
                return@withContext DistanceResult(
                    distanceKm = customDist,
                    isEstimated = false,
                    origin = trimmed,
                    destination = DESTINATION_NAME
                )
            }
        }

        // 1. Direct match or substring match in pre-mapped road table
        for ((key, dist) in PRE_MAPPED_ROAD_DISTANCES) {
            if (lower == key || lower.contains(key) || key.contains(lower)) {
                return@withContext DistanceResult(
                    distanceKm = dist,
                    isEstimated = false,
                    origin = trimmed,
                    destination = DESTINATION_NAME
                )
            }
        }

        // 2. Online Geocoding + Road Routing calculation
        if (trimmed.isNotEmpty()) {
            try {
                val onlineDist = queryOnlineRoadDistance(trimmed)
                if (onlineDist > 0) {
                    return@withContext DistanceResult(
                        distanceKm = onlineDist,
                        isEstimated = false,
                        origin = trimmed,
                        destination = DESTINATION_NAME
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Online distance query failed: ${e.message}")
            }
        }

        // 3. Fallback to Device GPS Coordinates (if valid and not at origin)
        if (deviceLat != null && deviceLng != null && deviceLat > 0 && deviceLng > 0) {
            val straightKm = haversineDistanceKm(deviceLat, deviceLng, DESTINATION_LAT, DESTINATION_LNG)
            val roadEstimated = straightKm * 1.28f
            val rounded = (round(roadEstimated * 10) / 10)
            return@withContext DistanceResult(
                distanceKm = rounded,
                isEstimated = true,
                origin = if (trimmed.isNotEmpty()) trimmed else "GPS स्थान",
                destination = DESTINATION_NAME
            )
        }

        return@withContext DistanceResult(
            distanceKm = -1f,
            isEstimated = false,
            origin = trimmed,
            destination = DESTINATION_NAME
        )
    }

    suspend fun getRoadDistanceKm(originAddress: String): Float {
        return resolveDrivingDistance(originAddress).distanceKm
    }

    private fun queryOnlineRoadDistance(query: String): Float {
        val encodedQuery = URLEncoder.encode(query + ", Uttar Pradesh, India", "UTF-8")
        val geocodeUrl = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=1&countrycodes=in"

        val conn = URL(geocodeUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.1")
        conn.connectTimeout = 3000
        conn.readTimeout = 3000

        if (conn.responseCode != 200) return -1f
        val response = conn.inputStream.bufferedReader().readText()
        val jsonArray = JSONArray(response)
        if (jsonArray.length() == 0) return -1f

        val firstMatch = jsonArray.getJSONObject(0)
        val originLat = firstMatch.getDouble("lat")
        val originLng = firstMatch.getDouble("lon")

        // Call OSRM public route service for driving road distance
        try {
            val osrmUrl = "https://router.project-osrm.org/route/v1/driving/" +
                    originLng + "," + originLat + ";" +
                    DESTINATION_LNG + "," + DESTINATION_LAT + "?overview=false"

            val osrmConn = URL(osrmUrl).openConnection() as HttpURLConnection
            osrmConn.requestMethod = "GET"
            osrmConn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.1")
            osrmConn.connectTimeout = 3000
            osrmConn.readTimeout = 3000

            if (osrmConn.responseCode == 200) {
                val osrmResp = osrmConn.inputStream.bufferedReader().readText()
                val osrmJson = JSONObject(osrmResp)
                val routes = osrmJson.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val distanceMeters = routes.getJSONObject(0).getDouble("distance")
                    val distanceKm = (distanceMeters / 1000.0).toFloat()
                    return (round(distanceKm * 10) / 10)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM route failed, using Haversine fallback: ${e.message}")
        }

        // Fallback: Haversine distance * road tortuosity factor (1.28)
        val straightKm = haversineDistanceKm(originLat, originLng, DESTINATION_LAT, DESTINATION_LNG)
        val roadEstimated = straightKm * 1.28f
        return (round(roadEstimated * 10) / 10)
    }

    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    /**
     * Format distance string for UI display.
     */
    fun formatDistance(distanceKm: Float): String {
        return when {
            distanceKm < 0 -> "स्थान दर्ज करें (Enter Location)"
            distanceKm == 0f -> "स्थान पर ही (At Ashram)"
            distanceKm < 1f -> "${(distanceKm * 1000).toInt()} मीटर (${distanceKm} km)"
            else -> "लगभग ${distanceKm.toInt()} किमी (${distanceKm} KM)"
        }
    }
}
