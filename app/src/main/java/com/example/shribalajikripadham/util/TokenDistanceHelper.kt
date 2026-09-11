package com.example.shribalajikripadham.util

import android.location.Location
import com.example.shribalajikripadham.data.model.Token
import java.util.Locale

enum class DistanceFilter(val labelHindi: String, val labelEnglish: String) {
    ALL("सभी (All)", "All"),
    WITHIN_10_KM("निकट (< 10 किमी)", "Local (<10 km)"),
    BETWEEN_10_AND_50_KM("मध्यम (10-50 किमी)", "Medium (10-50 km)"),
    BEYOND_50_KM("दूर-दराज़ (> 50 किमी)", "Far (>50 km)")
}

enum class TokenSortOrder(val labelHindi: String, val labelEnglish: String) {
    TOKEN_NUMBER("टोकन क्रम (1,2,3)", "Token Number (1,2,3)"),
    DISTANCE_DESC("सर्वाधिक दूरी (दूर से पास)", "Furthest Distance First"),
    DISTANCE_ASC("निकटतम दूरी (पास से दूर)", "Nearest Distance First"),
    DARSHAN_PENDING_FIRST("दर्शन प्रतीक्षारत पहले", "Pending Darshan First")
}

object TokenDistanceHelper {

    /**
     * Calculates distance in kilometers between token coordinates and Ashram coordinates.
     * Returns -1f if token coordinates are invalid/missing (e.g. counter registration).
     */
    fun calculateDistanceKm(
        ashramLat: Double,
        ashramLong: Double,
        tokenLat: Double,
        tokenLng: Double
    ): Float {
        if (tokenLat == 0.0 && tokenLng == 0.0) return -1f
        if (ashramLat == 0.0 && ashramLong == 0.0) return -1f

        val results = FloatArray(1)
        return try {
            Location.distanceBetween(ashramLat, ashramLong, tokenLat, tokenLng, results)
            results[0] / 1000f
        } catch (e: Exception) {
            -1f
        }
    }

    /**
     * Formats distance with intuitive badges.
     */
    fun formatDistance(distanceKm: Float, isHindi: Boolean = true): String {
        return when {
            distanceKm < 0f -> if (isHindi) "आश्रम काउंटर" else "Desk Booking"
            distanceKm < 0.2f -> if (isHindi) "📍 आश्रम परिसर (<200m)" else "📍 Inside Ashram (<200m)"
            distanceKm < 1.0f -> {
                val meters = (distanceKm * 1000).toInt()
                if (isHindi) "📍 ${meters} मीटर" else "📍 ${meters} meters"
            }
            distanceKm < 10.0f -> {
                val distStr = String.format(Locale.getDefault(), "%.1f", distanceKm)
                if (isHindi) "🏘️ $distStr किमी (स्थानीय)" else "🏘️ $distStr km (Local)"
            }
            else -> {
                val distStr = String.format(Locale.getDefault(), "%.1f", distanceKm)
                if (isHindi) "🛣️ $distStr किमी दूर" else "🛣️ $distStr km away"
            }
        }
    }

    /**
     * Convenience overload accepting Token directly.
     */
    fun formatDistance(
        token: Token,
        ashramLat: Double,
        ashramLong: Double,
        isHindi: Boolean = true
    ): String {
        val distKm = calculateDistanceKm(ashramLat, ashramLong, token.latitude, token.longitude)
        return formatDistance(distKm, isHindi)
    }

    /**
     * Filters and sorts list of tokens based on query, distance, and selected sort order.
     */
    fun filterAndSortTokens(
        tokens: List<Token>,
        ashramLat: Double,
        ashramLong: Double,
        searchQuery: String = "",
        distanceFilter: DistanceFilter = DistanceFilter.ALL,
        sortOrder: TokenSortOrder = TokenSortOrder.TOKEN_NUMBER
    ): List<Token> {
        val query = searchQuery.trim().lowercase()

        // 1. Search Query Filter (Name, Phone, City, Token #)
        val filteredByQuery = if (query.isEmpty()) {
            tokens
        } else {
            tokens.filter { token ->
                token.patientName.lowercase().contains(query) ||
                token.phoneNumber.contains(query) ||
                token.city.lowercase().contains(query) ||
                token.tokenNumber.toString() == query
            }
        }

        // 2. Distance Filter
        val filteredByDistance = when (distanceFilter) {
            DistanceFilter.ALL -> filteredByQuery
            DistanceFilter.WITHIN_10_KM -> filteredByQuery.filter { token ->
                val dist = calculateDistanceKm(ashramLat, ashramLong, token.latitude, token.longitude)
                dist in 0f..10f || dist < 0f // Include desk as local
            }
            DistanceFilter.BETWEEN_10_AND_50_KM -> filteredByQuery.filter { token ->
                val dist = calculateDistanceKm(ashramLat, ashramLong, token.latitude, token.longitude)
                dist in 10f..50f
            }
            DistanceFilter.BEYOND_50_KM -> filteredByQuery.filter { token ->
                val dist = calculateDistanceKm(ashramLat, ashramLong, token.latitude, token.longitude)
                dist > 50f
            }
        }

        // 3. Sorting
        return when (sortOrder) {
            TokenSortOrder.TOKEN_NUMBER -> filteredByDistance.sortedBy { it.tokenNumber }
            TokenSortOrder.DISTANCE_DESC -> filteredByDistance.sortedByDescending {
                calculateDistanceKm(ashramLat, ashramLong, it.latitude, it.longitude)
            }
            TokenSortOrder.DISTANCE_ASC -> filteredByDistance.sortedBy {
                val d = calculateDistanceKm(ashramLat, ashramLong, it.latitude, it.longitude)
                if (d < 0f) 0f else d
            }
            TokenSortOrder.DARSHAN_PENDING_FIRST -> filteredByDistance.sortedWith(
                compareBy<Token> { it.isDarshanCompleted }.thenBy { it.tokenNumber }
            )
        }
    }
}
