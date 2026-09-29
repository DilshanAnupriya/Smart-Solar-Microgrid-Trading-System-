/*
 * File:        Prosumer.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: A prosumer's profile as returned by the API (ProsumerResponse),
 *              plus helpers to pass it between screens in a Bundle.
 */

package com.example.mobileapp.models

import android.os.Bundle

/**
 * A prosumer's profile, matching ProsumerResponse on the API (the password hash is never sent).
 * The NIC is the primary key, so it never changes after registration.
 * [createdAt] is the registration time exactly as the API sent it (ISO-8601, UTC).
 */
data class Prosumer(
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val status: String,
    val createdAt: String
) {

    /** True while the account is active; a deactivated account cannot trade energy. */
    val isActive: Boolean
        get() = status == STATUS_ACTIVE

    /**
     * Up to two initials for the profile avatar, e.g. "Nimal Perera" -> "NP".
     */
    fun initials(): String {
        // Split on any run of spaces and take the first letter of the first two names
        return fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    /**
     * Packs the profile into a Bundle, to pass it between screens or keep it across rotation.
     */
    fun toBundle(): Bundle {
        // One key per field; fromBundle reads the same keys back
        return Bundle().apply {
            putString(KEY_NIC, nic)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_ADDRESS, address)
            putString(KEY_STATUS, status)
            putString(KEY_CREATED_AT, createdAt)
        }
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
        private const val KEY_CREATED_AT = "prosumer_created_at"

        /**
         * Reads a profile written by [toBundle]; returns null if the bundle doesn't hold one.
         */
        fun fromBundle(bundle: Bundle): Prosumer? {
            // The NIC is required; without it this bundle is not a profile
            val nic = bundle.getString(KEY_NIC) ?: return null
            return Prosumer(
                nic = nic,
                fullName = bundle.getString(KEY_FULL_NAME).orEmpty(),
                email = bundle.getString(KEY_EMAIL).orEmpty(),
                phone = bundle.getString(KEY_PHONE).orEmpty(),
                address = bundle.getString(KEY_ADDRESS).orEmpty(),
                status = bundle.getString(KEY_STATUS) ?: STATUS_ACTIVE,
                createdAt = bundle.getString(KEY_CREATED_AT).orEmpty()
            )
        }
    }
}
