/*
 * File:        ApiClientTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Unit tests for how ApiClient reads the API's replies: the API's
 *              own message is always preferred, field errors are kept, and
 *              empty or broken bodies still give a readable message.
 */

package com.example.mobileapp.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {

    @Test
    fun parseSuccess_readsTheEnvelopeMessage() {
        // Most endpoints answer { success, message, data }
        val result = ApiClient.parseSuccess(
            """{"success":true,"message":"Registration successful.","data":{"nic":"991234567V"}}"""
        )

        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals("Registration successful.", result.message)
        assertEquals("991234567V", result.data.getJSONObject("data").getString("nic"))
    }

    @Test
    fun parseSuccess_emptyBodyIsAnEmptyObject() {
        // 204 No Content has no body at all
        val result = ApiClient.parseSuccess("")

        assertTrue(result is ApiResult.Success)
        assertEquals(0, (result as ApiResult.Success).data.length())
    }

    @Test
    fun parseSuccess_bodyThatIsNotJsonIsAFailure() {
        // e.g. an HTML page from the wrong server
        val result = ApiClient.parseSuccess("<html>Not the API</html>")

        assertTrue(result is ApiResult.Failure)
        assertEquals(ApiResult.UNREADABLE_RESPONSE, (result as ApiResult.Failure).statusCode)
    }

    @Test
    fun parseFailure_usesTheApisBusinessRuleMessage() {
        // The API explains its rules, e.g. a duplicate NIC
        val failure = ApiClient.parseFailure(
            409,
            """{"success":false,"message":"A prosumer with this NIC is already registered.","errors":{}}"""
        )

        assertEquals("A prosumer with this NIC is already registered.", failure.message)
        assertEquals(409, failure.statusCode)
        assertTrue(failure.fieldErrors.isEmpty())
    }

    @Test
    fun parseFailure_keepsTheFirstMessagePerField() {
        // Model validation errors arrive as { field: [messages] }
        val failure = ApiClient.parseFailure(
            400,
            """{"success":false,"message":"One or more fields are invalid.",
               "errors":{"email":["Email address is not valid."],
                         "password":["Password must be at least 8 characters.","Second message"]}}"""
        )

        assertEquals("One or more fields are invalid.", failure.message)
        assertEquals("Email address is not valid.", failure.fieldErrors["email"])
        assertEquals("Password must be at least 8 characters.", failure.fieldErrors["password"])
    }

    @Test
    fun parseFailure_emptyBodyGetsAReadableFallback() {
        // ASP.NET answers its own [Authorize] checks with a 401 and no body
        val failure = ApiClient.parseFailure(401, "")

        assertEquals("Your session has expired. Please sign in again.", failure.message)
        assertTrue(failure.isUnauthorized)
    }

    @Test
    fun parseFailure_serverErrorWithoutJsonGetsAFallback() {
        // A crash page or proxy error is not JSON
        val failure = ApiClient.parseFailure(502, "Bad Gateway")

        assertEquals("The server ran into a problem. Please try again later.", failure.message)
    }

    @Test
    fun optText_givesEmptyForNullAndMissing() {
        // Android's own optString would give the text "null" for a JSON null
        val json = JSONObject("""{"name":"Nimal","deactivatedBy":null}""")

        assertEquals("Nimal", json.optText("name"))
        assertEquals("", json.optText("deactivatedBy"))
        assertEquals("", json.optText("missing"))
    }
}
