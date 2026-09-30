/*
 * File:        ApiReservation.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Data transfer and persistence model representing an energy slot reservation
 *              received from the SmartSolar Web API and cached in SQLite.
 */

package com.example.mobileapp.models

import org.json.JSONObject

/**
 * Representation of a power trading booking on the microgrid system.
 */
data class ApiReservation(
    val id: String,
    val reservationNumber: String,
    val prosumerNic: String,
    val prosumerName: String,
    val nodeId: String,
    val nodeName: String,
    val slotStartTime: String,
    val slotEndTime: String,
    val energyAmountKWh: Double,
    val reservationType: String,
    val status: String,
    val transactionQrCode: String,
    val cancellationReason: String?,
    val cancelledAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val canModify: Boolean,
    val canCancel: Boolean,
    val hoursUntilSlot: Double
) {
    val isPending: Boolean
        get() = status.equals("Pending", ignoreCase = true)

    val isApproved: Boolean
        get() = status.equals("Approved", ignoreCase = true)

    val isCompleted: Boolean
        get() = status.equals("Completed", ignoreCase = true)

    val isCancelled: Boolean
        get() = status.equals("Cancelled", ignoreCase = true)

    val isDropOff: Boolean
        get() = reservationType.equals("DropOff", ignoreCase = true)

    val isCharging: Boolean
        get() = reservationType.equals("Charging", ignoreCase = true)

    companion object {
        /**
         * Parses a JSON object from the Web API into an ApiReservation model.
         */
        fun fromJson(json: JSONObject): ApiReservation {
            return ApiReservation(
                id = json.optString("id", ""),
                reservationNumber = json.optString("reservationNumber", ""),
                prosumerNic = json.optString("prosumerNic", ""),
                prosumerName = json.optString("prosumerName", "Solar Prosumer"),
                nodeId = json.optString("nodeId", ""),
                nodeName = json.optString("nodeName", "Grid Node"),
                slotStartTime = json.optString("slotStartTime", ""),
                slotEndTime = json.optString("slotEndTime", ""),
                energyAmountKWh = json.optDouble("energyAmountKWh", 0.0),
                reservationType = json.optString("reservationType", "DropOff"),
                status = json.optString("status", "Pending"),
                transactionQrCode = json.optString("transactionQrCode", ""),
                cancellationReason = if (json.isNull("cancellationReason")) null else json.optString("cancellationReason"),
                cancelledAt = if (json.isNull("cancelledAt")) null else json.optString("cancelledAt"),
                notes = if (json.isNull("notes")) null else json.optString("notes"),
                createdAt = json.optString("createdAt", ""),
                updatedAt = json.optString("updatedAt", ""),
                createdBy = json.optString("createdBy", ""),
                canModify = json.optBoolean("canModify", false),
                canCancel = json.optBoolean("canCancel", false),
                hoursUntilSlot = json.optDouble("hoursUntilSlot", 0.0)
            )
        }
    }
}
