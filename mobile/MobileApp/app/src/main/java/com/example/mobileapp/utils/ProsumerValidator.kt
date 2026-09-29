/*
 * File:        ProsumerValidator.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Light client-side checks for the registration and profile forms:
 *              only that required fields are filled in and the two passwords match.
 */

package com.example.mobileapp.utils

import com.example.mobileapp.R

/**
 * Form checks that do not involve any business rule.
 *
 * The NIC format, email and phone formats, password length and uniqueness are all decided
 * by the API (FAT service), and its messages are shown on the matching fields. Checking
 * here only saves a round trip for an obviously empty form.
 * Every function returns the string resource id of the error, or null when the value is fine.
 */
object ProsumerValidator {

    /**
     * The NIC must be entered; the API checks the old and new NIC formats.
     */
    fun validateNic(nic: String): Int? {
        // Only emptiness is checked on the phone
        return if (nic.isBlank()) R.string.error_nic_required else null
    }

    /**
     * The full name must be entered.
     */
    fun validateFullName(fullName: String): Int? {
        // The API limits the length
        return if (fullName.isBlank()) R.string.error_full_name_required else null
    }

    /**
     * The email must be entered; the API checks the format and that it is not taken.
     */
    fun validateEmail(email: String): Int? {
        // Only emptiness is checked on the phone
        return if (email.isBlank()) R.string.error_email_required else null
    }

    /**
     * The phone number must be entered; the API checks it is 10 digits starting with 0.
     */
    fun validatePhone(phone: String): Int? {
        // Only emptiness is checked on the phone
        return if (phone.isBlank()) R.string.error_phone_required else null
    }

    /**
     * The address must be entered.
     */
    fun validateAddress(address: String): Int? {
        // The API limits the length
        return if (address.isBlank()) R.string.error_address_required else null
    }

    /**
     * The password must be entered; the API checks its minimum length.
     */
    fun validatePassword(password: String): Int? {
        // Spaces are allowed in a password, so isEmpty rather than isBlank
        return if (password.isEmpty()) R.string.error_password_required else null
    }

    /**
     * The confirmation must be entered and match the password. The API never sees this
     * field, so it can only be checked here.
     */
    fun validateConfirmPassword(password: String, confirmPassword: String): Int? {
        // Catch a typo in the password before the account is created with it
        return when {
            confirmPassword.isEmpty() -> R.string.error_confirm_password_required
            password != confirmPassword -> R.string.error_passwords_do_not_match
            else -> null
        }
    }
}
