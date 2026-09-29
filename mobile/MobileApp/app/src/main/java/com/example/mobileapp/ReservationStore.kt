package com.example.mobileapp

import java.util.Calendar
import java.util.UUID

/**
 * Temporary in-memory reservations so the screens can be used before the reservation
 * API exists. Everything resets when the app process restarts.
 *
 * Replace these calls with the API (and SQLite caching) when the backend is ready:
 *   create -> POST /api/reservations, update -> PUT /api/reservations/{id},
 *   cancel -> PATCH /api/reservations/{id}/cancel, all() -> GET /api/reservations?nic=...
 */
object ReservationStore {

    val nodes = listOf(
        GridNode("NODE-001", "Colombo South Solar Hub", "Galle Road, Colombo 03"),
        GridNode("NODE-002", "Kandy Hills Microgrid", "Peradeniya Road, Kandy"),
        GridNode("NODE-003", "Negombo Coastal Node", "Lewis Place, Negombo")
    )

    // Filled on first use, with times relative to "now" so every status and rule can be seen
    private val reservations: MutableList<Reservation> by lazy { createSampleReservations() }

    fun all(): List<Reservation> = reservations.toList()

    fun find(id: String): Reservation? = reservations.firstOrNull { it.id == id }

    /** New requests start as Pending until a grid operator approves them. */
    fun create(
        node: GridNode,
        type: ReservationType,
        startMillis: Long,
        durationHours: Int,
        energyKwh: Double
    ): Reservation {
        val reservation = Reservation(
            id = nextId(),
            node = node,
            type = type,
            startMillis = startMillis,
            durationHours = durationHours,
            energyKwh = energyKwh,
            status = ReservationStatus.PENDING,
            qrToken = null
        )
        reservations.add(reservation)
        return reservation
    }

    /** A modified reservation needs approval again, so its old QR code stops working. */
    fun update(
        id: String,
        node: GridNode,
        type: ReservationType,
        startMillis: Long,
        durationHours: Int,
        energyKwh: Double
    ): Reservation? = replace(id) {
        it.copy(
            node = node,
            type = type,
            startMillis = startMillis,
            durationHours = durationHours,
            energyKwh = energyKwh,
            status = ReservationStatus.PENDING,
            qrToken = null
        )
    }

    fun cancel(id: String): Reservation? =
        replace(id) { it.copy(status = ReservationStatus.CANCELLED, qrToken = null) }

    private fun replace(id: String, change: (Reservation) -> Reservation): Reservation? {
        val index = reservations.indexOfFirst { it.id == id }
        if (index == -1) return null

        val updated = change(reservations[index])
        reservations[index] = updated
        return updated
    }

    // Reservation ids look like RSV-1001; the next one is one higher than the largest so far
    private fun nextId(): String {
        val highest = reservations.maxOfOrNull { it.id.removePrefix("RSV-").toIntOrNull() ?: 0 } ?: 1000
        return "RSV-${highest + 1}"
    }

    private fun createSampleReservations(): MutableList<Reservation> {
        // Stand-in for the signed token the server will issue on approval
        fun sampleToken() = UUID.randomUUID().toString().replace("-", "")

        return mutableListOf(
            Reservation(
                "RSV-1001", nodes[0], ReservationType.DROP_OFF, atHour(daysFromToday = 2, hour = 10),
                2, 12.5, ReservationStatus.APPROVED, sampleToken()
            ),
            Reservation(
                "RSV-1002", nodes[1], ReservationType.CHARGING, atHour(daysFromToday = 4, hour = 14),
                1, 8.0, ReservationStatus.PENDING, null
            ),
            // Starts in about 6 hours, so the 12-hour rule locks it
            Reservation(
                "RSV-1003", nodes[2], ReservationType.CHARGING, hoursFromNow(6),
                1, 5.0, ReservationStatus.APPROVED, sampleToken()
            ),
            Reservation(
                "RSV-0998", nodes[0], ReservationType.DROP_OFF, atHour(daysFromToday = -3, hour = 9),
                3, 20.0, ReservationStatus.COMPLETED, null
            ),
            Reservation(
                "RSV-0999", nodes[1], ReservationType.CHARGING, atHour(daysFromToday = -1, hour = 16),
                2, 10.0, ReservationStatus.CANCELLED, null
            )
        )
    }

    private fun atHour(daysFromToday: Int, hour: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun hoursFromNow(hours: Int): Long = Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
