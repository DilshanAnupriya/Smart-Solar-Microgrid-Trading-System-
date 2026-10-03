/*
 * File:        MicrogridStation.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Model representing a physical microgrid solar station hub
 *              with GPS coordinates, energy capacities, and available battery slots.
 */

package com.example.mobileapp.models

import org.json.JSONObject

/**
 * Solar microgrid station hub used for mapping and battery slot management.
 */
data class MicrogridStation(
    val id: String,
    val nodeCode: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val generationCapacityKw: Double,
    val storageCapacityKWh: Double,
    val totalBatterySlots: Int,
    val availableBatterySlots: Int,
    val isActive: Boolean,
    val distanceKm: Double?
) {
    val occupancyPercentage: Int
        get() = if (totalBatterySlots > 0) {
            val used = totalBatterySlots - availableBatterySlots
            ((used.toDouble() / totalBatterySlots.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

    companion object {
        fun fromJson(json: JSONObject): MicrogridStation {
            return MicrogridStation(
                id = json.optString("id", ""),
                nodeCode = json.optString("nodeCode", ""),
                name = json.optString("name", "Microgrid Hub"),
                address = json.optString("address", ""),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                generationCapacityKw = json.optDouble("generationCapacityKw", 0.0),
                storageCapacityKWh = json.optDouble("storageCapacityKWh", 0.0),
                totalBatterySlots = json.optInt("totalBatterySlots", 0),
                availableBatterySlots = json.optInt("availableBatterySlots", 0),
                isActive = json.optBoolean("isActive", true),
                distanceKm = if (json.isNull("distanceKm")) null else json.optDouble("distanceKm")
            )
        }
    }
}
