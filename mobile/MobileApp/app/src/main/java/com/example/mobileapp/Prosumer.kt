package com.example.mobileapp

import android.os.Bundle

/**
 * A prosumer's profile, matching ProsumerResponse on the API (the password hash is never sent).
 * The NIC is the primary key, so it never changes after registration.
 */
data class Prosumer(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val status: String,
    val memberSince: String
) {

    val isActive: Boolean
        get() = status == STATUS_ACTIVE

    /** Up to two initials for the profile avatar, e.g. "Nimal Perera" -> "NP". */
    fun initials(): String =
        fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    /** Packs the profile into a Bundle, to pass it between screens or keep it across rotation. */
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_NIC, nic)
        putString(KEY_FULL_NAME, fullName)
        putString(KEY_EMAIL, email)
        putString(KEY_PHONE, phone)
        putString(KEY_ADDRESS, address)
        putString(KEY_STATUS, status)
        putString(KEY_MEMBER_SINCE, memberSince)
    }

    companion object {
        // Same values as the API's ProsumerStatus enum
        const val STATUS_ACTIVE = "Active"
        const val STATUS_DEACTIVATED = "Deactivated"

        private const val KEY_NIC = "prosumer_nic"
        private const val KEY_FULL_NAME = "prosumer_full_name"
        private const val KEY_EMAIL = "prosumer_email"
        private const val KEY_PHONE = "prosumer_phone"
        private const val KEY_ADDRESS = "prosumer_address"
        private const val KEY_STATUS = "prosumer_status"
        private const val KEY_MEMBER_SINCE = "prosumer_member_since"

        /** Reads a profile written by [toBundle]; returns null if the bundle doesn't hold one. */
        fun fromBundle(bundle: Bundle): Prosumer? {
            val nic = bundle.getString(KEY_NIC) ?: return null
            return Prosumer(
                nic = nic,
                fullName = bundle.getString(KEY_FULL_NAME).orEmpty(),
                email = bundle.getString(KEY_EMAIL).orEmpty(),
                phone = bundle.getString(KEY_PHONE).orEmpty(),
                address = bundle.getString(KEY_ADDRESS).orEmpty(),
                status = bundle.getString(KEY_STATUS) ?: STATUS_ACTIVE,
                memberSince = bundle.getString(KEY_MEMBER_SINCE).orEmpty()
            )
        }
    }
}
