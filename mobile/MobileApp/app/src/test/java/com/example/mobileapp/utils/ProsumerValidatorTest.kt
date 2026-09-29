/*
 * File:        ProsumerValidatorTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Unit tests for the light form checks: required fields and the
 *              password confirmation. Formats are the API's job, so a badly
 *              formatted value must pass through to the API unchanged.
 */

package com.example.mobileapp.utils

import com.example.mobileapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProsumerValidatorTest {

    @Test
    fun nic_isRequired() {
        // Blank means not entered
        assertEquals(R.string.error_nic_required, ProsumerValidator.validateNic(""))
        assertEquals(R.string.error_nic_required, ProsumerValidator.validateNic("   "))
    }

    @Test
    fun nic_formatIsLeftToTheApi() {
        // A wrongly formatted NIC is not rejected here; the API returns its own message
        assertNull(ProsumerValidator.validateNic("991234567V"))
        assertNull(ProsumerValidator.validateNic("12345"))
    }

    @Test
    fun otherFields_areRequired() {
        // Each required field reports its own message when blank
        assertEquals(R.string.error_full_name_required, ProsumerValidator.validateFullName(" "))
        assertEquals(R.string.error_email_required, ProsumerValidator.validateEmail(""))
        assertEquals(R.string.error_phone_required, ProsumerValidator.validatePhone(""))
        assertEquals(R.string.error_address_required, ProsumerValidator.validateAddress("  "))
        assertEquals(R.string.error_password_required, ProsumerValidator.validatePassword(""))
    }

    @Test
    fun otherFields_formatsAreLeftToTheApi() {
        // Values the API would reject still pass the phone's checks
        assertNull(ProsumerValidator.validateFullName("Al"))
        assertNull(ProsumerValidator.validateEmail("nimal@example"))
        assertNull(ProsumerValidator.validatePhone("771234567"))
        assertNull(ProsumerValidator.validateAddress("12 Galle Road"))
        assertNull(ProsumerValidator.validatePassword("short"))
    }

    @Test
    fun confirmPassword_mustBeEnteredAndMatch() {
        // The API never sees this field, so only the phone can check it
        assertNull(ProsumerValidator.validateConfirmPassword("secret123", "secret123"))
        assertEquals(
            R.string.error_confirm_password_required,
            ProsumerValidator.validateConfirmPassword("secret123", "")
        )
        assertEquals(
            R.string.error_passwords_do_not_match,
            ProsumerValidator.validateConfirmPassword("secret123", "secret124")
        )
    }
}
