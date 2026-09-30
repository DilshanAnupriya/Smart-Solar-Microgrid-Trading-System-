package com.example.mobileapp

import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.models.MicrogridStation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * In-memory reservation store for prosumer UI state.
 * Synchronized with the SmartSolar MongoDB backend via ReservationApi.
 */
object ReservationStore {

    val defaultNodes = listOf(
        GridNode("NODE-COLOMBO-01", "Colombo Central Hub (120 kWh)", "Galle Road, Colombo 03, Western Province"),
        GridNode("NODE-KANDY-02", "Kandy Hillcrest Station (80 kWh)", "Peradeniya Road, Kandy, Central Province"),
        GridNode("NODE-GALLE-01", "Galle Coastal Solar Grid (150 kWh)", "Matara Road, Galle, Southern Province")
    )

    var nodes: List<GridNode> = defaultNodes

    fun updateNodesFromStations(stations: List<MicrogridStation>) {
        if (stations.isEmpty()) return
        nodes = stations.map {
            GridNode(it.nodeCode.ifBlank { it.id }, it.name, it.address)
        }
    }

    // Filled on first use with seed/cache
    private val reservations: MutableList<Reservation> by lazy { createSampleReservations() }

    fun all(): List<Reservation> = synchronized(reservations) { reservations.toList() }

    fun find(id: String): Reservation? = synchronized(reservations) {
        val clean = id.trim()
        reservations.firstOrNull { it.id.equals(clean, ignoreCase = true) }
    }

    fun addOrUpdate(reservation: Reservation) = synchronized(reservations) {
        val index = reservations.indexOfFirst { it.id.equals(reservation.id, ignoreCase = true) }
        if (index != -1) {
            reservations[index] = reservation
        } else {
            reservations.add(0, reservation)
        }
    }

    fun setReservations(list: List<Reservation>) = synchronized(reservations) {
        reservations.clear()
        reservations.addAll(list)
    }

    /**
     * Converts an API response DTO from MongoDB into the local domain model.
     */
    fun fromApiReservation(api: ApiReservation): Reservation {
        val startMillis = parseIsoToMillis(api.slotStartTime)
        val endMillis = parseIsoToMillis(api.slotEndTime)
        val durationHours = (((endMillis - startMillis) / (1000 * 60 * 60)).toInt()).coerceIn(1, 12)

        val node = nodes.firstOrNull { it.id.equals(api.nodeId, ignoreCase = true) }
            ?: GridNode(
                id = api.nodeId,
                name = api.nodeName.ifBlank { "Grid Node ${api.nodeId}" },
                location = ""
            )

        val type = if (api.isCharging) ReservationType.CHARGING else ReservationType.DROP_OFF

        val status = when (api.status.lowercase(Locale.ROOT)) {
            "approved" -> ReservationStatus.APPROVED
            "completed" -> ReservationStatus.COMPLETED
            "cancelled" -> ReservationStatus.CANCELLED
            else -> ReservationStatus.PENDING
        }

        val displayId = if (api.reservationNumber.isNotBlank()) api.reservationNumber else api.id

        return Reservation(
            id = displayId,
            node = node,
            type = type,
            startMillis = startMillis,
            durationHours = durationHours,
            energyKwh = api.energyAmountKWh,
            status = status,
            qrToken = api.transactionQrCode.ifBlank { null }
        )
    }

    fun parseIsoToMillis(isoString: String): Long {
        if (isoString.isBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(isoString)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

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
        addOrUpdate(reservation)
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

    private fun replace(id: String, change: (Reservation) -> Reservation): Reservation? = synchronized(reservations) {
        val index = reservations.indexOfFirst { it.id.equals(id, ignoreCase = true) }
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
                "RES-SAMPLE-01", nodes[0], ReservationType.DROP_OFF, atHour(daysFromToday = 2, hour = 10),
                2, 12.5, ReservationStatus.APPROVED, sampleToken()
            ),
            Reservation(
                "RES-SAMPLE-02", nodes[1], ReservationType.CHARGING, atHour(daysFromToday = 4, hour = 14),
                1, 8.0, ReservationStatus.PENDING, null
            ),
            // Starts in about 6 hours, so the 12-hour rule locks it
            Reservation(
                "RES-SAMPLE-03", nodes[2], ReservationType.CHARGING, hoursFromNow(6),
                1, 5.0, ReservationStatus.APPROVED, sampleToken()
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
