/*
 * File:        AuthApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Prosumer sign-in against POST /auth/prosumer-login, turning the
 *              reply into the LocalUser row that is saved in SQLite.
 */

package com.example.mobileapp.network

import android.content.Context
import com.example.mobileapp.models.LocalUser
import org.json.JSONObject

/**
 * Authentication calls.
 */
class AuthApi(context: Context) {

    private val client = ApiClient(context)

    /**
     * Signs a prosumer in with their NIC (or email) and password. Wrong details come back
     * as a 401 failure and a deactivated account as a 403, each with the API's message.
     */
    suspend fun prosumerLogin(identifier: String, password: String): ApiResult<LocalUser> {
        // The API decides whether the identifier is a NIC or an email
        val body = JSONObject()
            .put("identifier", identifier)
            .put("password", password)
        return client.post("/auth/prosumer-login", body).map { parseLoginResponse(it) }
    }

    companion object {
        /**
         * Reads the login reply. Unlike most endpoints it is not wrapped in
         * { success, message, data }: it is { token, expiresAt, role, user } at the top level.
         */
        fun parseLoginResponse(json: JSONObject): LocalUser {
            // For a prosumer the account id is the NIC
            val user = json.getJSONObject("user")
            val nic = user.getString("nic")
            return LocalUser(
                id = nic,
                nic = nic,
                fullName = user.optText("fullName"),
                email = user.optText("email"),
                phone = user.optText("phone"),
                address = user.optText("address"),
                role = json.optText("role").ifBlank { LocalUser.ROLE_PROSUMER },
                token = json.getString("token"),
                expiresAt = json.getString("expiresAt")
            )
        }
    }
}
