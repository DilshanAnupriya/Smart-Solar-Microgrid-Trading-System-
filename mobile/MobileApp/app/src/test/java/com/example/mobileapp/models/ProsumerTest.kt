/*
 * File:        ProsumerTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Unit tests for the Prosumer model's avatar initials and status.
 */

package com.example.mobileapp.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProsumerTest {

    /**
     * A sample profile with the given name and status.
     */
    private fun prosumer(fullName: String = "Nimal Perera", status: String = Prosumer.STATUS_ACTIVE): Prosumer {
        // Only the name and status matter to these tests
        return Prosumer(
            nic = "991234567V",
            fullName = fullName,
            email = "nimal@example.lk",
            phone = "0771234567",
            address = "12 Galle Road, Colombo 03",
            status = status,
            createdAt = "2026-09-29T04:30:00Z"
        )
    }

    @Test
    fun initials_takesFirstLetterOfFirstTwoNames() {
        // Upper-cased, and a third name is ignored
        assertEquals("NP", prosumer("Nimal Perera").initials())
        assertEquals("NP", prosumer("nimal perera silva").initials())
    }

    @Test
    fun initials_handlesSingleNameAndExtraSpaces() {
        // Runs of spaces between or around names are ignored
        assertEquals("N", prosumer("  Nimal  ").initials())
        assertEquals("NP", prosumer("Nimal    Perera").initials())
    }

    @Test
    fun initials_emptyNameGivesEmptyString() {
        // A blank name has no initials at all
        assertEquals("", prosumer("   ").initials())
    }

    @Test
    fun isActive_followsStatus() {
        // Uses the same status text as the API's ProsumerStatus enum
        assertTrue(prosumer(status = Prosumer.STATUS_ACTIVE).isActive)
        assertFalse(prosumer(status = Prosumer.STATUS_DEACTIVATED).isActive)
    }
}
