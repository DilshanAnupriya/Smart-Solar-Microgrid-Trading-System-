/*
 * File:        ProsumerApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Prosumer account calls: self-registration, reading and editing the
 *              signed-in prosumer's profile, and deactivating the account.
 */

package com.example.mobileapp.network

import android.content.Context
import android.net.Uri
import com.example.mobileapp.models.Prosumer
import org.json.JSONObject

/**
 * Wraps the /prosumers endpoints. Each reply is { success, message, data: ProsumerResponse }.
 */
class ProsumerApi(context: Context) {

    private val client = ApiClient(context)

    /**
     * POST /prosumers/register. No token is needed; the API checks the NIC format and that
     * the NIC and email are not already registered.
     */
    suspend fun register(
        nic: String,
        fullName: String,
        email: String,
        phone: String,
        address: String,
        password: String
    ): ApiResult<Prosumer> {
        // Field names match ProsumerCreateRequest on the API
        val body = JSONObject()
            .put("nic", nic)
            .put("fullName", fullName)
            .put("email", email)
            .put("phone", phone)
            .put("address", address)
            .put("password", password)
        return client.post("/prosumers/register", body).map { parseProsumer(it.getJSONObject("data")) }
    }

    /**
     * GET /prosumers/{nic}. The API only lets a prosumer read the profile whose NIC is in their token.
     */
    suspend fun getProfile(nic: String): ApiResult<Prosumer> {
        // The profile is inside the "data" field of the reply
        return client.get(profilePath(nic)).map { parseProsumer(it.getJSONObject("data")) }
    }

    /**
     * PUT /prosumers/{nic}. Saves name, email, phone and address; the NIC itself cannot change.
     */
    suspend fun updateProfile(
        nic: String,
        fullName: String,
        email: String,
        phone: String,
        address: String
    ): ApiResult<Prosumer> {
        // Field names match ProsumerUpdateRequest on the API
        val body = JSONObject()
            .put("fullName", fullName)
            .put("email", email)
            .put("phone", phone)
            .put("address", address)
        return client.put(profilePath(nic), body).map { parseProsumer(it.getJSONObject("data")) }
    }

    /**
     * PATCH /prosumers/{nic}/deactivate. Afterwards the API refuses to sign this prosumer in
     * until a Backoffice officer reactivates the account.
     */
    suspend fun deactivate(nic: String): ApiResult<Prosumer> {
        // The reply is the profile with its new "Deactivated" status
        return client.patch(profilePath(nic) + "/deactivate").map { parseProsumer(it.getJSONObject("data")) }
    }

    /**
     * URL path of one prosumer's profile.
     */
    private fun profilePath(nic: String): String {
        // Encode the NIC so it is always safe inside a URL path
        return "/prosumers/" + Uri.encode(nic)
    }

    companion object {
        /**
         * Reads a ProsumerResponse. The API never sends the password hash.
         */
        fun parseProsumer(json: JSONObject): Prosumer {
            // The NIC is required; the other text fields fall back to empty
            return Prosumer(
                nic = json.getString("nic"),
                fullName = json.optText("fullName"),
                email = json.optText("email"),
                phone = json.optText("phone"),
                address = json.optText("address"),
                status = json.optText("status").ifBlank { Prosumer.STATUS_ACTIVE },
                createdAt = json.optText("createdAt")
            )
        }
    }
}
