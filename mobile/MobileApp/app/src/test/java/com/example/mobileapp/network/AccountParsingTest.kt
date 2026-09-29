/*
 * File:        AccountParsingTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Unit tests for reading the prosumer login reply and a
 *              ProsumerResponse, using the exact JSON shapes the API sends.
 */

package com.example.mobileapp.network

import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.models.Prosumer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountParsingTest {

    // ProsumerResponse as serialised by the API (camelCase, nulls included)
    private val prosumerJson = """
        {"nic":"991234567V","fullName":"Nimal Perera","email":"nimal@example.lk",
         "phone":"0771234567","address":"12 Galle Road, Colombo 03","status":"Active",
         "deactivatedAt":null,"deactivatedBy":null,
         "createdAt":"2026-09-29T04:30:00.123Z","updatedAt":"2026-09-29T04:30:00.123Z"}
    """

    @Test
    fun parseLoginResponse_buildsTheLocalUserRow() {
        // POST /auth/prosumer-login answers at the top level, without the data wrapper
        val json = JSONObject(
            """{"token":"header.payload.signature","expiresAt":"2026-09-29T06:30:00Z",
                "role":"Prosumer","user":$prosumerJson}"""
        )

        val user = AuthApi.parseLoginResponse(json)

        assertEquals("991234567V", user.id)
        assertEquals("991234567V", user.nic)
        assertEquals("Nimal Perera", user.fullName)
        assertEquals("12 Galle Road, Colombo 03", user.address)
        assertEquals(LocalUser.ROLE_PROSUMER, user.role)
        assertEquals("header.payload.signature", user.token)
        assertEquals("2026-09-29T06:30:00Z", user.expiresAt)
    }

    @Test
    fun parseLoginResponse_missingRoleMeansProsumer() {
        // Only prosumers use this endpoint
        val json = JSONObject(
            """{"token":"t","expiresAt":"2026-09-29T06:30:00Z","user":$prosumerJson}"""
        )

        assertEquals(LocalUser.ROLE_PROSUMER, AuthApi.parseLoginResponse(json).role)
    }

    @Test
    fun parseProsumer_readsEveryField() {
        // The same object is the "data" of register, get, update and deactivate
        val prosumer = ProsumerApi.parseProsumer(JSONObject(prosumerJson))

        assertEquals("991234567V", prosumer.nic)
        assertEquals("nimal@example.lk", prosumer.email)
        assertEquals("0771234567", prosumer.phone)
        assertEquals(Prosumer.STATUS_ACTIVE, prosumer.status)
        assertEquals("2026-09-29T04:30:00.123Z", prosumer.createdAt)
        assertTrue(prosumer.isActive)
    }

    @Test
    fun parseProsumer_readsDeactivatedStatus() {
        // The deactivate endpoint returns the profile with its new status
        val json = JSONObject(prosumerJson).put("status", "Deactivated")

        assertEquals(false, ProsumerApi.parseProsumer(json).isActive)
    }

    @Test
    fun map_turnsAMissingFieldIntoAFailure() {
        // A reply without the expected "data" must not crash the screen
        val reply: ApiResult<JSONObject> = ApiResult.Success(JSONObject("""{"success":true}"""), "")

        val result = reply.map { ProsumerApi.parseProsumer(it.getJSONObject("data")) }

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiResult.UNREADABLE_RESPONSE, (result as ApiResult.Failure).statusCode)
    }
}
