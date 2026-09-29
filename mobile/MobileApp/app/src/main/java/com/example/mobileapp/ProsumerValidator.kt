package com.example.mobileapp

/**
 * Client-side checks for prosumer registration and profile forms.
 *
 * The rules mirror ProsumerCreateRequest and NicValidator on the API, which is still
 * where they are actually enforced; checking here just gives the user instant feedback.
 * Every function returns the string resource id of the error, or null when the value is valid.
 */
object ProsumerValidator {

    // Old NIC: 9 digits followed by V or X (e.g. 991234567V)
    private val OLD_NIC_PATTERN = Regex("^[0-9]{9}[VX]$")

    // New NIC: 12 digits (e.g. 199912345678)
    private val NEW_NIC_PATTERN = Regex("^[0-9]{12}$")

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    // Sri Lankan phone number: 10 digits starting with 0
    private val PHONE_PATTERN = Regex("^0[0-9]{9}$")

    const val FULL_NAME_MIN_LENGTH = 3
    const val FULL_NAME_MAX_LENGTH = 100
    const val ADDRESS_MAX_LENGTH = 250
    const val PASSWORD_MIN_LENGTH = 8

    /** Trims spaces and uppercases v/x, the same way the API normalises a NIC before saving it. */
    fun normalizeNic(nic: String): String = nic.trim().uppercase()

    fun validateNic(nic: String): Int? {
        val normalized = normalizeNic(nic)
        return when {
            normalized.isEmpty() -> R.string.error_nic_required
            !OLD_NIC_PATTERN.matches(normalized) && !NEW_NIC_PATTERN.matches(normalized) ->
                R.string.error_nic_format
            else -> null
        }
    }

    fun validateFullName(fullName: String): Int? {
        val trimmed = fullName.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_full_name_required
            trimmed.length < FULL_NAME_MIN_LENGTH -> R.string.error_full_name_too_short
            trimmed.length > FULL_NAME_MAX_LENGTH -> R.string.error_full_name_too_long
            else -> null
        }
    }

    fun validateEmail(email: String): Int? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_email_required
            !EMAIL_PATTERN.matches(trimmed) -> R.string.error_email_format
            else -> null
        }
    }

    fun validatePhone(phone: String): Int? {
        val trimmed = phone.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_phone_required
            !PHONE_PATTERN.matches(trimmed) -> R.string.error_phone_format
            else -> null
        }
    }

    fun validateAddress(address: String): Int? {
        val trimmed = address.trim()
        return when {
            trimmed.isEmpty() -> R.string.error_address_required
            trimmed.length > ADDRESS_MAX_LENGTH -> R.string.error_address_too_long
            else -> null
        }
    }

    fun validatePassword(password: String): Int? = when {
        password.isEmpty() -> R.string.error_password_required
        password.length < PASSWORD_MIN_LENGTH -> R.string.error_password_too_short
        else -> null
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): Int? = when {
        confirmPassword.isEmpty() -> R.string.error_confirm_password_required
        password != confirmPassword -> R.string.error_passwords_do_not_match
        else -> null
    }
}
