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
    const val DESTINATION_LAT = 28.3972915
    const val DESTINATION_LNG = 78.1460410

    // Pre-mapped driving distances (km) for instant offline calculation and fallback
    // Pre-mapped driving distances (km) for instant offline calculation and fallback
    private val PRE_MAPPED_ROAD_DISTANCES: Map<String, Float> = mapOf(
        // Local / Dungra Jat
        "डुंगरा जाट" to 0f,
        "डूँगरा जाट" to 0f,
        "डूँगरा" to 0f,
        "डुंगरा" to 0f,
        "डूँगरा जाट (स्थानीय)" to 0f,
        "dungra jaat" to 0f,
        "dungra" to 0f,
        "स्थानीय" to 0f,
        "local" to 0f,

        // Bulandshahr District Villages & Towns
        "सलेमपुर" to 8f,
        "salampur" to 8f,
        "बावन" to 6f,
        "bavan" to 6f,
        "छोटाबांस" to 5f,
        "chhotabans" to 5f,
        "बड़ाबांस" to 7f,
        "badabans" to 7f,
        "शिकारपुर" to 14f,
        "shikarpur" to 14f,
        "वलीपुरा" to 20f,
        "walipura" to 20f,
        "बुलंदशहर" to 22f,
        "bulandshahr" to 22f,
        "बुलन्दशहर" to 22f,
        "jahangirabad" to 24f,
        "जहाँगीराबाद" to 24f,
        "जहांगीराबाद" to 24f,
        "औरंगाबाद" to 26f,
        "aurangabad" to 26f,
        "पहासू" to 28f,
        "pahasu" to 28f,
        "खानपुर" to 28f,
        "khanpur" to 28f,
        "छतारी" to 32f,
        "chhatari" to 32f,
        "दानपुर" to 34f,
        "danpur" to 34f,
        "स्याना" to 35f,
        "syana" to 35f,
        "चोला" to 35f,
        "chola" to 35f,
        "अनूपशहर" to 36f,
        "anupshahr" to 36f,
        "खुर्जा" to 38f,
        "khurja" to 38f,
        "गुलावठी" to 42f,
        "gulawati" to 42f,
        "gulaothi" to 42f,
        "बुगरासी" to 42f,
        "bugrasi" to 42f,
        "जवां" to 42f,
        "jawan" to 42f,
        "dibai" to 44f,
        "डिबाई" to 44f,
        "दिबाई" to 44f,
        "सिकंदराबाद" to 45f,
        "sikandrabad" to 45f,
        "अरनिया" to 45f,
        "arnia" to 45f,
        "ककोड़" to 45f,
        "kakore" to 45f,
        "ककोड" to 45f,
        "अतरौली" to 48f,
        "atrauli" to 48f,
        "खैर" to 48f,
        "khair" to 48f,
        "बीबीनगर" to 48f,
        "bb nagar" to 48f,
        "bibinagar" to 48f,
        "भवन बहादुर नगर" to 48f,
        "झाझर" to 50f,
        "jhajhar" to 50f,
        "अलीगढ़" to 55f,
        "aligarh" to 55f,
        "जेवर" to 58f,
        "jewar" to 58f,
        "हापुड़" to 58f,
        "hapur" to 58f,
        "दादरी" to 62f,
        "dadri" to 62f,
        "हसनपुर" to 65f,
        "hasanpur" to 65f,
        "गढ़मुक्तेश्वर" to 65f,
        "garhmukteshwar" to 65f,
        "ग्रेटर नोएडा" to 68f,
        "greater noida" to 68f,
        "बहजोई" to 72f,
        "bahjoi" to 72f,
        "गजरौला" to 75f,
        "gajraula" to 75f,
        "पलवल" to 75f,
        "palwal" to 75f,
        "गाजियाबाद" to 78f,
        "ghaziabad" to 78f,
        "हाथरस" to 82f,
        "hathras" to 82f,
        "नोएडा" to 82f,
        "noida" to 82f,
        "फरीदाबाद" to 85f,
        "faridabad" to 85f,
        "मेरठ" to 85f,
        "meerut" to 85f,
        "संभल" to 85f,
        "sambhal" to 85f,
        "अमरोहा" to 90f,
        "amroha" to 90f,
        "दिल्ली" to 95f,
        "delhi" to 95f,
        "कासगंज" to 95f,
        "kasganj" to 95f,
        "चंदौसी" to 95f,
        "chandausi" to 95f,
        "नई दिल्ली" to 98f,
        "new delhi" to 98f,
        "वृंदावन" to 105f,
        "vrindavan" to 105f,
        "गुरुग्राम" to 110f,
        "gurugram" to 110f,
        "गुड़गांव" to 110f,
        "gurgaon" to 110f,
        "मथुरा" to 110f,
        "mathura" to 110f,
        "मुरादाबाद" to 115f,
        "moradabad" to 115f,
        "बदायूं" to 125f,
        "budaun" to 125f,
        "मुजफ्फरनगर" to 135f,
        "muzaffarnagar" to 135f,
        "सोनीपत" to 140f,
        "sonipat" to 140f,
        "बिजनौर" to 140f,
        "bijnor" to 140f,
        "आगरा" to 155f,
        "agra" to 155f,
        "रोहतक" to 165f,
        "rohtak" to 165f,
        "पानीपत" to 170f,
        "panipat" to 170f,
        "बरेली" to 175f,
        "bareilly" to 175f,
        "सहारनपुर" to 195f,
        "saharanpur" to 195f,
        "करनाल" to 205f,
        "karnal" to 205f,
        "हरिद्वार" to 220f,
        "haridwar" to 220f,
        "ऋषिकेश" to 245f,
        "rishikesh" to 245f,
        "देहरादून" to 275f,
        "dehradun" to 275f,
        "जयपुर" to 310f,
        "jaipur" to 310f,
        "कानपुर" to 380f,
        "kanpur" to 380f,
        "लखनऊ" to 420f,
        "lucknow" to 420f
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

    private fun cleanLocationQuery(query: String): String {
        return query.lowercase()
            .replace(Regex("\\b(ग्राम|गांव|गाँव|vill|village|तहसील|जिला|dist|district|पोस्ट|post|थाना|thana|श्री|shri)\\b"), " ")
            .replace(Regex("[,.-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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
        val clean = cleanLocationQuery(lower)
        val cleanWords = clean.split(" ").filter { it.isNotBlank() }

        // 0. Check Super Admin Custom Added Villages / Cities first
        for ((customKey, customDist) in customCityDistancesCache) {
            if (lower == customKey || clean == customKey || (customKey.length >= 4 && clean.contains(customKey))) {
                return@withContext DistanceResult(
                    distanceKm = customDist,
                    isEstimated = false,
                    origin = trimmed,
                    destination = DESTINATION_NAME
                )
            }
        }

        // 1. Direct match or word match in pre-mapped road table (prevents false substring hits)
        for ((key, dist) in PRE_MAPPED_ROAD_DISTANCES) {
            if (lower == key || clean == key) {
                return@withContext DistanceResult(
                    distanceKm = dist,
                    isEstimated = false,
                    origin = trimmed,
                    destination = DESTINATION_NAME
                )
            }
        }

        // 1b. Check if individual words in query match known town (e.g. "गांव शिकारपुर" -> "शिकारपुर")
        for (w in cleanWords) {
            if (w.length >= 3 && PRE_MAPPED_ROAD_DISTANCES.containsKey(w)) {
                return@withContext DistanceResult(
                    distanceKm = PRE_MAPPED_ROAD_DISTANCES[w] ?: 0f,
                    isEstimated = false,
                    origin = trimmed,
                    destination = DESTINATION_NAME
                )
            }
        }

        // 1c. Substring match only when key length >= 4 and query contains the full key
        for ((key, dist) in PRE_MAPPED_ROAD_DISTANCES) {
            if (key.length >= 4 && (lower.contains(key) || clean.contains(key))) {
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
                val onlineDist = queryOnlineRoadDistance(clean.ifEmpty { trimmed })
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

    suspend fun calculateRoadDistance(originAddress: String): DistanceResult {
        return resolveDrivingDistance(originAddress)
    }

    suspend fun getRoadDistanceKm(originAddress: String): Float {
        return resolveDrivingDistance(originAddress).distanceKm
    }

    private fun geocodeNominatim(queryWithContext: String): Pair<Double, Double>? {
        return try {
            val encodedQuery = URLEncoder.encode(queryWithContext, "UTF-8")
            val geocodeUrl = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=1&countrycodes=in"
            val conn = URL(geocodeUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "ShriBalajiKripaDhamApp/2.2")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode != 200) return null
            val response = conn.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(response)
            if (jsonArray.length() == 0) return null
            val firstMatch = jsonArray.getJSONObject(0)
            Pair(firstMatch.getDouble("lat"), firstMatch.getDouble("lon"))
        } catch (e: Exception) {
            null
        }
    }

    private fun queryOnlineRoadDistance(query: String): Float {
        // First try searching specifically around Bulandshahr / West UP
        var coords = geocodeNominatim("$query, Bulandshahr, Uttar Pradesh, India")
        if (coords == null) {
            coords = geocodeNominatim("$query, Uttar Pradesh, India")
        }
        if (coords == null) {
            coords = geocodeNominatim("$query, India")
        }
        if (coords == null) return -1f

        val (originLat, originLng) = coords

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
