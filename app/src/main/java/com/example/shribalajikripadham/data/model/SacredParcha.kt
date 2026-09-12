package com.example.shribalajikripadham.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class ParchaCategory(val displayNameHindi: String, val icon: String) {
    HAWAN("हवन पर्चा", "🪔"),
    UTARA("उतारा पर्चा", "🌺"),
    ARJI_ARDAS("अर्जी व अरदास", "🥥"),
    NIYAM_PARHEZ("नियम व परहेज", "🛡️"),
    AARTI_STUTI("आरती व चालीसा", "🔔"),
    OTHER("अन्य दस्तावेज", "📜");

    companion object {
        fun fromString(value: String): ParchaCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

/**
 * Sacred Parcha / Document Model.
 * Represents ritual guidelines, Hawan samagri slips, Maiya ke utare ka parcha, and rules.
 */
data class SacredParcha(
    val id: Long = 0,
    val parchaId: String = "PARCHA_" + System.currentTimeMillis(),
    val title: String,
    val category: ParchaCategory = ParchaCategory.OTHER,
    val subtitle: String = "",
    val samagriList: List<String> = emptyList(),
    val vidhiSteps: List<String> = emptyList(),
    val precautions: List<String> = emptyList(),
    val mantraText: String = "",
    val imageUri: String = "",
    val isPublished: Boolean = true,
    val isHidden: Boolean = false,
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val createdBy: String = "SUPER_ADMIN",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun samagriListToJson(): String {
        val arr = JSONArray()
        samagriList.forEach { arr.put(it) }
        return arr.toString()
    }

    fun vidhiStepsToJson(): String {
        val arr = JSONArray()
        vidhiSteps.forEach { arr.put(it) }
        return arr.toString()
    }

    fun precautionsToJson(): String {
        val arr = JSONArray()
        precautions.forEach { arr.put(it) }
        return arr.toString()
    }

    companion object {
        fun parseJsonList(jsonStr: String?): List<String> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i)
                    if (item.isNotBlank()) list.add(item)
                }
                list
            } catch (e: Exception) {
                jsonStr.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
            }
        }
    }
}
