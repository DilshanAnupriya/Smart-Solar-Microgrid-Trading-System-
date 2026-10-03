/*
 * File:        ReservationStats.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Model representing aggregated reservation counts and energy metrics
 *              for the Grid Operator live dashboard.
 */

package com.example.mobileapp.models

import org.json.JSONObject

data class ReservationStats(
    val totalReservations: Int,
    val pendingReservations: Int,
    val approvedReservations: Int,
    val completedReservations: Int,
    val cancelledReservations: Int,
    val totalEnergyKWh: Double
) {
    companion object {
        fun fromJson(json: JSONObject): ReservationStats {
            return ReservationStats(
                totalReservations = json.optInt("totalReservations", 0),
                pendingReservations = json.optInt("pendingReservations", 0),
                approvedReservations = json.optInt("approvedReservations", 0),
                completedReservations = json.optInt("completedReservations", 0),
                cancelledReservations = json.optInt("cancelledReservations", 0),
                totalEnergyKWh = json.optDouble("totalEnergyKWh", 0.0)
            )
        }
    }
}
