/*
 * File:        ReservationParsingTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: Unit tests for reading the API's reservation and node replies,
 *              and for the rule that a QR code is shown only for an approved
 *              reservation whose token the API released.
 */

package com.example.mobileapp.network

import com.example.mobileapp.models.ReservationStatus
import com.example.mobileapp.models.ReservationType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationParsingTest {

    /**
     * A ReservationResponseDto as the API serialises it (camelCase, UTC times).
     */
    private fun reservationJson(status: String = "Approved", qr: String = "UkVTLTIwMjYwOTMw"): JSONObject {
        // Only the status and the QR token vary between the tests
        return JSONObject(
            """
            {"id":"66f9a1b2c3d4e5f601234567","reservationNumber":"RES-20260930-8F2A1C",
             "prosumerNic":"991234567V","prosumerName":"Nimal Perera",
             "nodeId":"66f9a1b2c3d4e5f607654321","nodeName":"Colombo Central Hub",
             "slotStartTime":"2026-10-02T04:30:00Z","slotEndTime":"2026-10-02T06:30:00Z",
             "energyAmountKWh":12.5,"reservationType":"Charging","status":"$status",
             "transactionQrCode":"$qr","cancellationReason":null,"cancelledAt":null,"notes":null,
             "createdAt":"2026-09-30T03:00:00.123Z","updatedAt":"2026-09-30T03:00:00.123Z",
             "createdBy":"991234567V","canModify":true,"canCancel":true,"hoursUntilSlot":49.5}
            """
        )
    }

    @Test
    fun parseReservation_readsEveryField() {
        // Times become epoch milliseconds; the type and status become enums
        val reservation = ReservationApi.parseReservation(reservationJson())

        assertEquals("66f9a1b2c3d4e5f601234567", reservation.id)
        assertEquals("RES-20260930-8F2A1C", reservation.reservationNumber)
        assertEquals("66f9a1b2c3d4e5f607654321", reservation.nodeId)
        assertEquals("Colombo Central Hub", reservation.nodeName)
        assertEquals(ReservationType.CHARGING, reservation.type)
        assertEquals(1_790_915_400_000L, reservation.startMillis)
        assertEquals(2 * 60 * 60 * 1000L, reservation.endMillis - reservation.startMillis)
        assertEquals(12.5, reservation.energyKwh, 0.0)
        assertEquals(ReservationStatus.APPROVED, reservation.status)
        assertEquals("UkVTLTIwMjYwOTMw", reservation.qrToken)
        assertTrue(reservation.canModify)
        assertTrue(reservation.canCancel)
    }

    @Test
    fun hasQrCode_onlyForApprovedWithAToken() {
        // A pending reservation arrives without a token; an approved one with it
        assertTrue(ReservationApi.parseReservation(reservationJson("Approved")).hasQrCode)
        assertFalse(ReservationApi.parseReservation(reservationJson("Pending", qr = "")).hasQrCode)
        assertFalse(ReservationApi.parseReservation(reservationJson("Approved", qr = "")).hasQrCode)
    }

    @Test
    fun isActive_forPendingAndApprovedOnly() {
        // Cancelled and completed reservations are history
        assertTrue(ReservationApi.parseReservation(reservationJson("Pending", qr = "")).isActive)
        assertTrue(ReservationApi.parseReservation(reservationJson("Approved")).isActive)
        assertFalse(ReservationApi.parseReservation(reservationJson("Cancelled", qr = "")).isActive)
        assertFalse(ReservationApi.parseReservation(reservationJson("Completed", qr = "")).isActive)
    }

    @Test
    fun enums_matchTheApisText() {
        // Exactly the spellings ReservationStatus and ReservationType use on the API
        assertEquals(ReservationStatus.CANCELLED, ReservationStatus.fromApi("Cancelled"))
        assertEquals(ReservationType.DROP_OFF, ReservationType.fromApi("DropOff"))
        assertEquals("DropOff", ReservationType.DROP_OFF.apiValue)
        assertEquals("Charging", ReservationType.CHARGING.apiValue)
    }

    @Test
    fun missingSlotTime_makesTheReplyUnreadable() {
        // Wrapped in map(), a broken reply becomes a failure instead of a crash
        val broken = reservationJson().apply { remove("slotStartTime") }
        val reply: ApiResult<JSONObject> = ApiResult.Success(JSONObject().put("data", broken), "")

        val result = reply.map { ReservationApi.parseReservation(it.getJSONObject("data")) }

        assertTrue(result is ApiResult.Failure)
    }

    @Test
    fun parseNodes_readsIdNameAddressAndFreeSlots() {
        // NodeResponseDto as returned by GET /nodes
        val nodes = NodeApi.parseNodes(
            JSONArray(
                """
                [{"id":"66f9a1b2c3d4e5f607654321","nodeCode":"NODE-01","name":"Colombo Central Hub",
                  "address":"Galle Road, Colombo 03","latitude":6.9,"longitude":79.85,
                  "availableBatterySlots":3,"totalBatterySlots":8,"isActive":true}]
                """
            )
        )

        assertEquals(1, nodes.size)
        assertEquals("66f9a1b2c3d4e5f607654321", nodes[0].id)
        assertEquals("Colombo Central Hub", nodes[0].name)
        assertEquals("Galle Road, Colombo 03", nodes[0].address)
        assertEquals(3, nodes[0].availableSlots)
    }
}
