package com.example.mobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProsumerValidatorTest {

    // NIC

    @Test
    fun nic_oldFormat_isValid() {
        assertNull(ProsumerValidator.validateNic("991234567V"))
        assertNull(ProsumerValidator.validateNic("991234567X"))
    }

    @Test
    fun nic_oldFormatLowercaseWithSpaces_isValid() {
        assertNull(ProsumerValidator.validateNic("  991234567v "))
    }

    @Test
    fun nic_newFormat_isValid() {
        assertNull(ProsumerValidator.validateNic("199912345678"))
    }

    @Test
    fun nic_empty_isRequired() {
        assertEquals(R.string.error_nic_required, ProsumerValidator.validateNic("   "))
    }

    @Test
    fun nic_wrongFormat_isRejected() {
        assertEquals(R.string.error_nic_format, ProsumerValidator.validateNic("12345678V"))
        assertEquals(R.string.error_nic_format, ProsumerValidator.validateNic("991234567A"))
        assertEquals(R.string.error_nic_format, ProsumerValidator.validateNic("19991234567"))
        assertEquals(R.string.error_nic_format, ProsumerValidator.validateNic("1999123456789"))
    }

    @Test
    fun normalizeNic_trimsAndUppercases() {
        assertEquals("991234567V", ProsumerValidator.normalizeNic(" 991234567v "))
    }

    // Full name

    @Test
    fun fullName_rules() {
        assertNull(ProsumerValidator.validateFullName("Nimal Perera"))
        assertEquals(R.string.error_full_name_required, ProsumerValidator.validateFullName(""))
        assertEquals(R.string.error_full_name_too_short, ProsumerValidator.validateFullName("Al"))
        assertEquals(R.string.error_full_name_too_long, ProsumerValidator.validateFullName("a".repeat(101)))
    }

    // Email

    @Test
    fun email_rules() {
        assertNull(ProsumerValidator.validateEmail("nimal@example.lk"))
        assertEquals(R.string.error_email_required, ProsumerValidator.validateEmail(" "))
        assertEquals(R.string.error_email_format, ProsumerValidator.validateEmail("nimal@example"))
        assertEquals(R.string.error_email_format, ProsumerValidator.validateEmail("nimal example.lk"))
    }

    // Phone

    @Test
    fun phone_rules() {
        assertNull(ProsumerValidator.validatePhone("0771234567"))
        assertEquals(R.string.error_phone_required, ProsumerValidator.validatePhone(""))
        assertEquals(R.string.error_phone_format, ProsumerValidator.validatePhone("771234567"))
        assertEquals(R.string.error_phone_format, ProsumerValidator.validatePhone("07712345678"))
        assertEquals(R.string.error_phone_format, ProsumerValidator.validatePhone("+94771234567"))
    }

    // Address

    @Test
    fun address_rules() {
        assertNull(ProsumerValidator.validateAddress("12 Galle Road, Colombo 03"))
        assertEquals(R.string.error_address_required, ProsumerValidator.validateAddress(""))
        assertEquals(R.string.error_address_too_long, ProsumerValidator.validateAddress("a".repeat(251)))
    }

    // Password

    @Test
    fun password_rules() {
        assertNull(ProsumerValidator.validatePassword("12345678"))
        assertEquals(R.string.error_password_required, ProsumerValidator.validatePassword(""))
        assertEquals(R.string.error_password_too_short, ProsumerValidator.validatePassword("1234567"))
    }

    @Test
    fun confirmPassword_rules() {
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
