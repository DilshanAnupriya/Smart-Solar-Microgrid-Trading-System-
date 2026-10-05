package com.example.mobileapp

import java.util.Locale

/** Pure filtering logic shared by the reservations screen and its unit tests. */
object ReservationListFilter {

    fun matches(
        reservation: Reservation,
        selectedStatus: ReservationStatus?,
        query: String,
        typeLabel: String,
        statusLabel: String
    ): Boolean {
        if (selectedStatus != null && reservation.status != selectedStatus) return false

        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) return true

        return listOf(
            reservation.id,
            reservation.node.name,
            reservation.node.location,
            typeLabel,
            statusLabel
        ).any { value -> value.lowercase(Locale.ROOT).contains(normalizedQuery) }
    }
}
