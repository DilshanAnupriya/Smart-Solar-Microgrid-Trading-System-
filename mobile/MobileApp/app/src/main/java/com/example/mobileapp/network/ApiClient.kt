/*
 * File:        ApiClient.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Sends GET, POST, PUT and PATCH requests to the SmartSolar Web API
 *              over HttpURLConnection, attaches the signed-in user's token, and
 *              turns every reply into an ApiResult.
 */

package com.example.mobileapp.network

import android.content.Context
import com.example.mobileapp.sessions.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal HTTP client for the Web API.
 *
 * Every request runs on Dispatchers.IO. The functions suspend, so a screen that calls them
 * from its main-thread coroutine scope gets the result back on the main thread.
 * The token is read from SQLite and sent as "Authorization: Bearer <token>".
 */
class ApiClient(context: Context) {

    private val sessionManager = SessionManager(context)

    /** GET [path], e.g. "/prosumers/991234567V". */
    suspend fun get(path: String): ApiResult<JSONObject> {
        // A GET request never has a body
        return send("GET", path, null)
    }

    /** POST [body] to [path], e.g. a new registration. */
    suspend fun post(path: String, body: JSONObject? = null): ApiResult<JSONObject> {
        // Creates something on the server or signs the user in
        return send("POST", path, body)
    }

    /** PUT [body] to [path], replacing the editable fields of a record. */
    suspend fun put(path: String, body: JSONObject): ApiResult<JSONObject> {
        // Updates an existing record as a whole
        return send("PUT", path, body)
    }

    /** PATCH [path], changing one thing about a record, e.g. deactivating it. */
    suspend fun patch(path: String, body: JSONObject? = null): ApiResult<JSONObject> {
        // Partial change of an existing record
        return send("PATCH", path, body)
    }

    /**
     * Performs the request on Dispatchers.IO and reads the reply.
     */
    private suspend fun send(method: String, path: String, body: JSONObject?): ApiResult<JSONObject> {
        // Everything below (network and the SQLite token read) runs off the main thread
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(ApiConfig.BASE_URL + path).openConnection() as HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = ApiConfig.CONNECT_TIMEOUT_MS
                connection.readTimeout = ApiConfig.READ_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")

                // Sign-in and registration run before there is a token, so it is optional
                sessionManager.getToken()?.let { token ->
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                // POST, PUT and PATCH always send a JSON body ("{}" when there is nothing to
                // send), because Android's HTTP stack rejects those methods without one
                if (method != "GET") {
                    val bytes = (body ?: JSONObject()).toString().toByteArray(Charsets.UTF_8)
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }

                // Error replies (4xx/5xx) are read from the error stream
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (statusCode in 200..299) parseSuccess(text) else parseFailure(statusCode, text)
            } catch (e: IOException) {
                // No network, the API is not running, cleartext blocked, or a timeout
                ApiResult.Failure(NO_CONNECTION_MESSAGE, ApiResult.NO_RESPONSE)
            } finally {
                connection?.disconnect()
            }
        }
    }

    companion object {
        const val NO_CONNECTION_MESSAGE =
            "Can't reach the server. Check your connection and that the SmartSolar API is running, then try again."

        /**
         * Reads a 2xx reply. Most endpoints wrap their data as { success, message, data };
         * login and the health check answer with plain objects. An empty body
         * (e.g. 204 No Content) becomes an empty object.
         */
        fun parseSuccess(body: String): ApiResult<JSONObject> {
            // The whole object is returned; each *Api class picks out what it needs
            return try {
                val json = if (body.isBlank()) JSONObject() else JSONObject(body)
                ApiResult.Success(json, json.optText("message"))
            } catch (e: JSONException) {
                ApiResult.Failure(ApiResult.UNREADABLE_MESSAGE, ApiResult.UNREADABLE_RESPONSE)
            }
        }

        /**
         * Builds a Failure from an error reply, which the API sends as
         * { success: false, message, errors: { field: [messages] } }.
         * The API's message is preferred, because that is where the business rules are
         * explained. ASP.NET answers its own 401/403 checks with an empty body, so those
         * get a readable fallback.
         */
        fun parseFailure(statusCode: Int, body: String): ApiResult.Failure {
            // A body that is empty or not JSON simply has no message to offer
            val json = try {
                if (body.isBlank()) null else JSONObject(body)
            } catch (e: JSONException) {
                null
            }
            val message = json?.optText("message").orEmpty().ifBlank { fallbackMessage(statusCode) }
            return ApiResult.Failure(message, statusCode, readFieldErrors(json))
        }

        /**
         * Reads model validation errors, e.g. { "email": ["Email address is not valid."] },
         * keeping the first message for each field.
         */
        private fun readFieldErrors(json: JSONObject?): Map<String, String> {
            // Other replies either have no "errors" or use a plain list, which has no fields
            val errors = json?.optJSONObject("errors") ?: return emptyMap()
            val result = LinkedHashMap<String, String>()
            for (field in errors.keys()) {
                val first = errors.optJSONArray(field)?.optString(0).orEmpty()
                if (first.isNotBlank()) result[field] = first
            }
            return result
        }

        /**
         * Message used only when the API did not explain the problem itself.
         */
        private fun fallbackMessage(statusCode: Int): String {
            // Worded for the user, not for a developer
            return when (statusCode) {
                400 -> "Please check the details you entered."
                401 -> "Your session has expired. Please sign in again."
                403 -> "You are not allowed to do that."
                404 -> "That record could not be found."
                409 -> "That conflicts with an existing record."
                in 500..599 -> "The server ran into a problem. Please try again later."
                else -> "The request failed (HTTP $statusCode)."
            }
        }
    }
}

/**
 * Like optString, but a missing key or a JSON null gives "" (Android's optString gives "null").
 */
fun JSONObject.optText(key: String): String {
    // isNull is true both for a missing key and for an explicit null
    return if (isNull(key)) "" else optString(key, "")
}
