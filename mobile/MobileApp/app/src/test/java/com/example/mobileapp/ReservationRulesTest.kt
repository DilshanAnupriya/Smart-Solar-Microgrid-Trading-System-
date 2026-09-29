package com.example.mobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationRulesTest {

    private val hour = ReservationRules.HOUR_MILLIS
    private val now = 1_790_000_000_000L

    // 7-day booking window

    @Test
    fun bookingWindow_acceptsFutureStartUpToSevenDays() {
        assertTrue(ReservationRules.isWithinBookingWindow(now + hour, now))
        assertTrue(ReservationRules.isWithinBookingWindow(now + 7 * 24 * hour, now))
    }

    @Test
    fun bookingWindow_rejectsPastAndMoreThanSevenDays() {
        assertFalse(ReservationRules.isWithinBookingWindow(now, now))
        assertFalse(ReservationRules.isWithinBookingWindow(now - hour, now))
        assertFalse(ReservationRules.isWithinBookingWindow(now + 7 * 24 * hour + 1, now))
    }

    @Test
    fun validateStart_returnsTheRightError() {
        assertEquals(R.string.error_start_required, ReservationRules.validateStart(null, now))
        assertEquals(R.string.error_start_in_past, ReservationRules.validateStart(now - hour, now))
        assertEquals(R.string.error_start_too_far, ReservationRules.validateStart(now + 8 * 24 * hour, now))
        assertNull(ReservationRules.validateStart(now + 2 * hour, now))
    }

    // 12-hour notice for modify and cancel

    @Test
    fun modifyOrCancel_allowedWithTwelveHoursOrMore() {
        assertTrue(ReservationRules.canModifyOrCancel(now + 12 * hour, now))
        assertTrue(ReservationRules.canModifyOrCancel(now + 48 * hour, now))
    }

    @Test
    fun modifyOrCancel_blockedWithLessThanTwelveHours() {
        assertFalse(ReservationRules.canModifyOrCancel(now + 12 * hour - 1, now))
        assertFalse(ReservationRules.canModifyOrCancel(now + hour, now))
        assertFalse(ReservationRules.canModifyOrCancel(now - hour, now))
    }

    // Energy amount

    @Test
    fun energy_rules() {
        assertNull(ReservationRules.validateEnergy("10"))
        assertNull(ReservationRules.validateEnergy(" 12.5 "))
        assertNull(ReservationRules.validateEnergy("1"))
        assertNull(ReservationRules.validateEnergy("100"))
        assertEquals(R.string.error_energy_required, ReservationRules.validateEnergy(""))
        assertEquals(R.string.error_energy_range, ReservationRules.validateEnergy("0.5"))
        assertEquals(R.string.error_energy_range, ReservationRules.validateEnergy("100.1"))
        assertEquals(R.string.error_energy_range, ReservationRules.validateEnergy("abc"))
    }

    // QR payload

    @Test
    fun qrPayload_onlyForApprovedReservations() {
        val node = GridNode("NODE-001", "Colombo South Solar Hub", "Galle Road, Colombo 03")
        val approved = Reservation(
            "RSV-1001", node, ReservationType.DROP_OFF, now + 24 * hour, 2, 12.5,
            ReservationStatus.APPROVED, "token123"
        )
        assertEquals("SSM-RSV|RSV-1001|token123", approved.qrPayload())
        assertNull(approved.copy(status = ReservationStatus.PENDING).qrPayload())
        assertNull(approved.copy(qrToken = null).qrPayload())
    }
}
