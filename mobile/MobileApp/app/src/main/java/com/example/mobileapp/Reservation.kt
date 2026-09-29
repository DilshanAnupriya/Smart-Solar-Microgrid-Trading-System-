package com.example.mobileapp

/** A solar microgrid hub where prosumers drop off or draw energy. */
data class GridNode(
    val id: String,
    val name: String,
    val location: String
)

/** Whether the prosumer is sending energy to the node or taking energy from it. */
enum class ReservationType(val labelRes: Int, val helpRes: Int) {
    DROP_OFF(R.string.reservation_type_drop_off, R.string.reservation_type_drop_off_help),
    CHARGING(R.string.reservation_type_charging, R.string.reservation_type_charging_help)
}

/** Reservation lifecycle; each status has its own badge colours. */
enum class ReservationStatus(val labelRes: Int, val badgeRes: Int, val badgeTextColorRes: Int) {
    PENDING(R.string.reservation_status_pending, R.drawable.bg_badge_pending, R.color.warning_text),
    APPROVED(R.string.reservation_status_approved, R.drawable.bg_badge_active, R.color.success_text),
    CANCELLED(R.string.reservation_status_cancelled, R.drawable.bg_badge_deactivated, R.color.danger_text),
    COMPLETED(R.string.reservation_status_completed, R.drawable.bg_badge_completed, R.color.secondary_text)
}

/**
 * One energy slot reservation. Times are epoch milliseconds.
 * [qrToken] is only set once a grid operator approves the reservation.
 */
data class Reservation(
    val id: String,
    val node: GridNode,
    val type: ReservationType,
    val startMillis: Long,
    val durationHours: Int,
    val energyKwh: Double,
    val status: ReservationStatus,
    val qrToken: String?
) {

    val endMillis: Long
        get() = startMillis + durationHours * ReservationRules.HOUR_MILLIS

    /** Pending and approved reservations are still going to happen; the others are history. */
    val isActive: Boolean
        get() = status == ReservationStatus.PENDING || status == ReservationStatus.APPROVED

    /**
     * Text encoded in the transaction QR code, or null if the reservation isn't approved.
     * The token is issued by the server, so the operator app can verify it there.
     */
    fun qrPayload(): String? =
        if (status == ReservationStatus.APPROVED && qrToken != null) "SSM-RSV|$id|$qrToken" else null
}
