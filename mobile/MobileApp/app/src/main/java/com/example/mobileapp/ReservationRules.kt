package com.example.mobileapp

/**
 * Reservation business rules from the assignment brief:
 *  - a reservation must start within the next 7 days
 *  - modifying or cancelling needs at least 12 hours' notice before the start time
 *
 * The API is where these are actually enforced; checking here gives instant feedback
 * and hides actions that the server would reject anyway.
 * Validation functions return a string resource id for the error, or null when valid.
 */
object ReservationRules {

    const val HOUR_MILLIS = 60 * 60 * 1000L
    const val BOOKING_WINDOW_DAYS = 7
    const val CHANGE_NOTICE_HOURS = 12
    const val MIN_ENERGY_KWH = 1.0
    const val MAX_ENERGY_KWH = 100.0

    /** True if the start time is in the future and no more than 7 days away. */
    fun isWithinBookingWindow(startMillis: Long, nowMillis: Long): Boolean =
        startMillis > nowMillis && startMillis <= nowMillis + BOOKING_WINDOW_DAYS * 24 * HOUR_MILLIS

    /** True if there are still at least 12 hours before the reservation starts. */
    fun canModifyOrCancel(startMillis: Long, nowMillis: Long): Boolean =
        startMillis - nowMillis >= CHANGE_NOTICE_HOURS * HOUR_MILLIS

    /** Checks a chosen start time; null means the date or time hasn't been picked yet. */
    fun validateStart(startMillis: Long?, nowMillis: Long): Int? = when {
        startMillis == null -> R.string.error_start_required
        startMillis <= nowMillis -> R.string.error_start_in_past
        !isWithinBookingWindow(startMillis, nowMillis) -> R.string.error_start_too_far
        else -> null
    }

    /** Checks the energy amount typed by the user. */
    fun validateEnergy(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return R.string.error_energy_required

        val kwh = trimmed.toDoubleOrNull()
        return if (kwh == null || kwh < MIN_ENERGY_KWH || kwh > MAX_ENERGY_KWH) {
            R.string.error_energy_range
        } else {
            null
        }
    }
}
