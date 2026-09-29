/*
 * File:        ApiResult.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: The outcome of one API call, so every screen handles success and
 *              failure the same way and always shows the API's own message.
 */

package com.example.mobileapp.network

import org.json.JSONException

/**
 * Result of an API call: [Success] with the parsed data, or [Failure] with a message
 * the screen can show to the user as it is.
 */
sealed class ApiResult<out T> {

    /** The API accepted the request. [message] is the API's own message, or empty. */
    data class Success<out T>(val data: T, val message: String) : ApiResult<T>()

    /**
     * The request failed.
     *
     * @property message the API's explanation when it sent one (business rules live there),
     *   otherwise a readable fallback
     * @property statusCode the HTTP status, or [NO_RESPONSE] / [UNREADABLE_RESPONSE]
     * @property fieldErrors first validation message per request field, keyed by the field
     *   name the API uses (camelCase, e.g. "email")
     */
    data class Failure(
        val message: String,
        val statusCode: Int,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : ApiResult<Nothing>() {

        /** 401: the token is missing, has expired or is no longer accepted. */
        val isUnauthorized: Boolean
            get() = statusCode == 401
    }

    companion object {
        // statusCode when no HTTP response arrived at all (no network, API not running, timeout)
        const val NO_RESPONSE = 0

        // statusCode when the API answered with a body the app could not read
        const val UNREADABLE_RESPONSE = -1

        const val UNREADABLE_MESSAGE = "The server sent a reply the app could not read. Please try again."
    }
}

/**
 * Converts the data inside a [ApiResult.Success]; a [ApiResult.Failure] passes through unchanged.
 * A reply that is missing an expected field becomes a failure instead of crashing the screen.
 */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
    // Only a successful result carries data to convert
    return when (this) {
        is ApiResult.Success -> try {
            ApiResult.Success(transform(data), message)
        } catch (e: JSONException) {
            ApiResult.Failure(ApiResult.UNREADABLE_MESSAGE, ApiResult.UNREADABLE_RESPONSE)
        }
        is ApiResult.Failure -> this
    }
}
