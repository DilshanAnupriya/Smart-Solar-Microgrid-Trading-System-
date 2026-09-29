package com.example.mobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProsumerTest {

    private fun prosumer(fullName: String = "Nimal Perera", status: String = Prosumer.STATUS_ACTIVE) =
        Prosumer(
            nic = "991234567V",
            fullName = fullName,
            email = "nimal@example.lk",
            phone = "0771234567",
            address = "12 Galle Road, Colombo 03",
            status = status,
            memberSince = "29 Sep 2026"
        )

    @Test
    fun initials_takesFirstLetterOfFirstTwoNames() {
        assertEquals("NP", prosumer("Nimal Perera").initials())
        assertEquals("NP", prosumer("nimal perera silva").initials())
    }

    @Test
    fun initials_handlesSingleNameAndExtraSpaces() {
        assertEquals("N", prosumer("  Nimal  ").initials())
        assertEquals("NP", prosumer("Nimal    Perera").initials())
    }

    @Test
    fun initials_emptyNameGivesEmptyString() {
        assertEquals("", prosumer("   ").initials())
    }

    @Test
    fun isActive_followsStatus() {
        assertTrue(prosumer(status = Prosumer.STATUS_ACTIVE).isActive)
        assertFalse(prosumer(status = Prosumer.STATUS_DEACTIVATED).isActive)
    }
}
