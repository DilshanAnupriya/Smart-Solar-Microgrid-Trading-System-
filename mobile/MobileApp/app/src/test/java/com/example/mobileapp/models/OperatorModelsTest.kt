/*
 * File:        OperatorModelsTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Unit tests validating JSON mapping, status checks, and calculations
 *              for ApiReservation, MicrogridStation, and LocalUser roles.
 */

package com.example.mobileapp.models

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperatorModelsTest {

    @Test
    fun localUser_roleChecks() {
        val operator = LocalUser(
            id = "op-01",
            nic = "199212345678",
            fullName = "Site Operator",
            email = "operator@smartsolar.lk",
            phone = "0771234567",
            address = "",
            role = LocalUser.ROLE_OPERATOR,
            token = "jwt-operator",
            expiresAt = "2026-10-01T00:00:00Z"
        )

        assertTrue(operator.isOperator)
        assertFalse(operator.isProsumer)

        val prosumer = LocalUser(
            id = "200012345678",
            nic = "200012345678",
            fullName = "Kasun Perera",
            email = "kasun@example.com",
            phone = "0771234567",
            address = "Colombo",
            role = LocalUser.ROLE_PROSUMER,
            token = "jwt-prosumer",
            expiresAt = "2026-10-01T00:00:00Z"
        )

        assertFalse(prosumer.isOperator)
        assertTrue(prosumer.isProsumer)
    }

    @Test
    fun apiReservation_fromJson_andStatusFlags() {
        val json = JSONObject(
            """{
                "id": "66f7f0a8b9a1c2d3e4f5a6b7",
                "reservationNumber": "RES-20260922-A101",
                "prosumerNic": "200012345678",
                "prosumerName": "Kasun Perera",
                "nodeId": "NODE-COLOMBO-01",
                "nodeName": "Colombo Central Hub (120 kWh)",
                "slotStartTime": "2026-10-02T10:00:00Z",
                "slotEndTime": "2026-10-02T12:00:00Z",
                "energyAmountKWh": 25.5,
                "reservationType": "DropOff",
                "status": "Approved",
                "transactionQrCode": "token-xyz",
                "canModify": true,
                "canCancel": true,
                "hoursUntilSlot": 48.0
            }"""
        )

        val res = ApiReservation.fromJson(json)
        assertEquals("66f7f0a8b9a1c2d3e4f5a6b7", res.id)
        assertEquals("RES-20260922-A101", res.reservationNumber)
        assertEquals("200012345678", res.prosumerNic)
        assertEquals("Kasun Perera", res.prosumerName)
        assertEquals(25.5, res.energyAmountKWh, 0.001)
        assertTrue(res.isApproved)
        assertFalse(res.isPending)
        assertFalse(res.isCompleted)
        assertTrue(res.isDropOff)
        assertFalse(res.isCharging)
    }

    @Test
    fun microgridStation_fromJson_andCalculations() {
        val json = JSONObject(
            """{
                "id": "NODE-COLOMBO-01",
                "nodeCode": "NODE-COLOMBO-01",
                "name": "Colombo Central Hub (120 kWh)",
                "address": "Galle Road, Colombo 03",
                "latitude": 6.9271,
                "longitude": 79.8612,
                "generationCapacityKw": 50.0,
                "storageCapacityKWh": 120.0,
                "totalBatterySlots": 10,
                "availableBatterySlots": 7,
                "isActive": true
            }"""
        )

        val station = MicrogridStation.fromJson(json)
        assertEquals("NODE-COLOMBO-01", station.id)
        assertEquals(6.9271, station.latitude, 0.0001)
        assertEquals(79.8612, station.longitude, 0.0001)
        assertEquals(10, station.totalBatterySlots)
        assertEquals(7, station.availableBatterySlots)
        // 3 of 10 used = 30% occupancy
        assertEquals(30, station.occupancyPercentage)
    }

    @Test
    fun reservationStats_fromJson() {
        val json = JSONObject(
            """{
                "totalReservations": 14,
                "pendingReservations": 3,
                "approvedReservations": 6,
                "completedReservations": 4,
                "cancelledReservations": 1,
                "totalEnergyKWh": 245.5
            }"""
        )

        val stats = ReservationStats.fromJson(json)
        assertEquals(14, stats.totalReservations)
        assertEquals(3, stats.pendingReservations)
        assertEquals(6, stats.approvedReservations)
        assertEquals(4, stats.completedReservations)
        assertEquals(1, stats.cancelledReservations)
        assertEquals(245.5, stats.totalEnergyKWh, 0.001)
    }
}
