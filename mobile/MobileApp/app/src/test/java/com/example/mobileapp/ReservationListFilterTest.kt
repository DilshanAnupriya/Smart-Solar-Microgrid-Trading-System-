package com.example.mobileapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationListFilterTest {

    private val reservation = Reservation(
        id = "RES-2026-0142",
        node = GridNode("NODE-01", "Colombo Central Hub", "Galle Road, Colombo 03"),
        type = ReservationType.DROP_OFF,
        startMillis = 1_800_000_000_000,
        durationHours = 2,
        energyKwh = 12.5,
        status = ReservationStatus.APPROVED,
        qrToken = null
    )

    @Test
    fun searchMatchesVisibleBookingFieldsIgnoringCase() {
        assertTrue(matches(query = "res-2026"))
        assertTrue(matches(query = "CENTRAL"))
        assertTrue(matches(query = "galle road"))
        assertTrue(matches(query = "drop-off"))
        assertTrue(matches(query = "approved"))
        assertFalse(matches(query = "Kandy"))
    }

    @Test
    fun statusAndSearchFiltersAreCombined() {
        assertTrue(matches(status = ReservationStatus.APPROVED, query = "Colombo"))
        assertFalse(matches(status = ReservationStatus.PENDING, query = "Colombo"))
        assertFalse(matches(status = ReservationStatus.APPROVED, query = "Kandy"))
    }

    @Test
    fun emptySearchMatchesTheSelectedStatus() {
        assertTrue(matches(status = null, query = "  "))
        assertTrue(matches(status = ReservationStatus.APPROVED, query = ""))
        assertFalse(matches(status = ReservationStatus.CANCELLED, query = ""))
    }

    private fun matches(
        status: ReservationStatus? = null,
        query: String
    ): Boolean = ReservationListFilter.matches(
        reservation = reservation,
        selectedStatus = status,
        query = query,
        typeLabel = "Drop-off",
        statusLabel = "Approved"
    )
}
