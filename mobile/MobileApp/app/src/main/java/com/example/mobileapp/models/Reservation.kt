/*
 * File:        Reservation.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — matched to the API's
 *              ReservationResponseDto: server ids, UTC times, the server's QR
 *              token and its canModify / canCancel decisions.
 * Description: An energy slot reservation, its type and its status.
 */

package com.example.mobileapp.models

import android.os.Bundle
import com.example.mobileapp.R

/**
 * Whether the prosumer is sending energy to the node or taking energy from it.
 * [apiValue] is the exact text the API uses.
 */
enum class ReservationType(val apiValue: String, val labelRes: Int, val helpRes: Int) {
    DROP_OFF("DropOff", R.string.reservation_type_drop_off, R.string.reservation_type_drop_off_help),
    CHARGING("Charging", R.string.reservation_type_charging, R.string.reservation_type_charging_help);

    companion object {
        /**
         * Reads the API's text; anything unknown is shown as a drop-off.
         */
        fun fromApi(value: String): ReservationType {
            // The API only ever sends the two values above
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: DROP_OFF
        }
    }
}

/**
 * Reservation lifecycle as decided by the API; each status has its own badge colours.
 */
enum class ReservationStatus(
    val apiValue: String,
    val labelRes: Int,
    val badgeRes: Int,
    val badgeTextColorRes: Int
) {
    PENDING("Pending", R.string.reservation_status_pending, R.drawable.bg_badge_pending, R.color.warning_text),
    APPROVED("Approved", R.string.reservation_status_approved, R.drawable.bg_badge_active, R.color.success_text),
    CANCELLED("Cancelled", R.string.reservation_status_cancelled, R.drawable.bg_badge_deactivated, R.color.danger_text),
    COMPLETED("Completed", R.string.reservation_status_completed, R.drawable.bg_badge_completed, R.color.secondary_text);

    companion object {
        /**
         * Reads the API's text; anything unknown is shown as pending.
         */
        fun fromApi(value: String): ReservationStatus {
            // The API only ever sends the four values above
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

/**
 * One energy slot reservation. Times are epoch milliseconds.
 *
 * Every business decision comes from the API: [qrToken] is empty until the reservation is
 * approved, and [canModify] / [canCancel] already include the 12-hour notice rule. A copy
 * read back from the SQLite cache has both flags false, because changes need the API.
 */
data class Reservation(
    val id: String,
    val reservationNumber: String,
    val nodeId: String,
    val nodeName: String,
    val type: ReservationType,
    val startMillis: Long,
    val endMillis: Long,
    val energyKwh: Double,
    val status: ReservationStatus,
    val qrToken: String,
    val canModify: Boolean,
    val canCancel: Boolean
) {

    /** Pending and approved reservations are still going to happen; the others are history. */
    val isActive: Boolean
        get() = status == ReservationStatus.PENDING || status == ReservationStatus.APPROVED

    /** The QR code is shown only for an approved reservation whose token the API released. */
    val hasQrCode: Boolean
        get() = status == ReservationStatus.APPROVED && qrToken.isNotEmpty()

    /**
     * Packs the reservation into a Bundle, to hand it to the next screen.
     */
    fun toBundle(): Bundle {
        // One key per field; fromBundle reads the same keys back
        return Bundle().apply {
            putString(KEY_ID, id)
            putString(KEY_NUMBER, reservationNumber)
            putString(KEY_NODE_ID, nodeId)
            putString(KEY_NODE_NAME, nodeName)
            putString(KEY_TYPE, type.apiValue)
            putLong(KEY_START, startMillis)
            putLong(KEY_END, endMillis)
            putDouble(KEY_ENERGY, energyKwh)
            putString(KEY_STATUS, status.apiValue)
            putString(KEY_QR_TOKEN, qrToken)
            putBoolean(KEY_CAN_MODIFY, canModify)
            putBoolean(KEY_CAN_CANCEL, canCancel)
        }
    }

    companion object {
        private const val KEY_ID = "reservation_id"
        private const val KEY_NUMBER = "reservation_number"
        private const val KEY_NODE_ID = "reservation_node_id"
        private const val KEY_NODE_NAME = "reservation_node_name"
        private const val KEY_TYPE = "reservation_type"
        private const val KEY_START = "reservation_start"
        private const val KEY_END = "reservation_end"
        private const val KEY_ENERGY = "reservation_energy"
        private const val KEY_STATUS = "reservation_status"
        private const val KEY_QR_TOKEN = "reservation_qr_token"
        private const val KEY_CAN_MODIFY = "reservation_can_modify"
        private const val KEY_CAN_CANCEL = "reservation_can_cancel"

        /**
         * Reads a reservation written by [toBundle]; returns null if the bundle doesn't hold one.
         */
        fun fromBundle(bundle: Bundle): Reservation? {
            // The id is required; without it this bundle is not a reservation
            val id = bundle.getString(KEY_ID) ?: return null
            return Reservation(
                id = id,
                reservationNumber = bundle.getString(KEY_NUMBER).orEmpty(),
                nodeId = bundle.getString(KEY_NODE_ID).orEmpty(),
                nodeName = bundle.getString(KEY_NODE_NAME).orEmpty(),
                type = ReservationType.fromApi(bundle.getString(KEY_TYPE).orEmpty()),
                startMillis = bundle.getLong(KEY_START),
                endMillis = bundle.getLong(KEY_END),
                energyKwh = bundle.getDouble(KEY_ENERGY),
                status = ReservationStatus.fromApi(bundle.getString(KEY_STATUS).orEmpty()),
                qrToken = bundle.getString(KEY_QR_TOKEN).orEmpty(),
                canModify = bundle.getBoolean(KEY_CAN_MODIFY),
                canCancel = bundle.getBoolean(KEY_CAN_CANCEL)
            )
        }
    }
}
