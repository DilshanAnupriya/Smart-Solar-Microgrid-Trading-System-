/*
 * File:        LocalUser.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: The account signed in on this phone, exactly as it is stored in
 *              the local_user SQLite table.
 */

package com.example.mobileapp.models

/**
 * One row of local_user. For a prosumer, [id] and [nic] hold the same value.
 * [expiresAt] is the token's expiry time exactly as the API sent it (ISO-8601, UTC).
 */
data class LocalUser(
    val id: String,
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val role: String,
    val token: String,
    val expiresAt: String
) {
    companion object {
        // Role the API writes into a prosumer's token and login reply
        const val ROLE_PROSUMER = "Prosumer"
    }
}
