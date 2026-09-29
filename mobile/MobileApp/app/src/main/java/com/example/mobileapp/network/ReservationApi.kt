/*
 * File:        ReservationApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: The signed-in prosumer's reservation calls (list, details,
 *              create, modify, cancel) against /reservations/my. The API
 *              enforces every rule; this class only sends and reads.
 */

package com.example.mobileapp.network

import android.content.Context
import android.net.Uri
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.models.ReservationStatus
import com.example.mobileapp.models.ReservationType
import com.example.mobileapp.utils.DateTimeUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Wraps the /reservations/my endpoints. The API takes the NIC from the token, so there is
 * no way to send, read or change another prosumer's reservation from here.
 */
class ReservationApi(context: Context) {

    private val client = ApiClient(context)

    /**
     * GET /reservations/my — every reservation of the signed-in prosumer, newest slot first.
     */
    suspend fun getMyReservations(): ApiResult<List<Reservation>> {
        // The list is inside the "data" field of the reply
        return client.get(BASE_PATH).map { parseReservations(it.getJSONArray("data")) }
    }

    /**
     * GET /reservations/my/{id} — one reservation, with its QR token if it is approved.
     */
    suspend fun getReservation(id: String): ApiResult<Reservation> {
        // The reservation is inside the "data" field of the reply
        return client.get(reservationPath(id)).map { parseReservation(it.getJSONObject("data")) }
    }

    /**
     * POST /reservations/my — requests a new slot. The API checks the 7-day window, the node
     * and the type, and the new reservation starts as Pending.
     */
    suspend fun createReservation(
        nodeId: String,
        type: ReservationType,
        startMillis: Long,
        endMillis: Long,
        energyKwh: Double
    ): ApiResult<Reservation> {
        // Field names match ProsumerReservationRequestDto; times are sent as UTC
        val body = JSONObject()
            .put("nodeId", nodeId)
            .put("reservationType", type.apiValue)
            .put("slotStartTime", DateTimeUtils.formatIsoUtc(startMillis))
            .put("slotEndTime", DateTimeUtils.formatIsoUtc(endMillis))
            .put("energyAmountKWh", energyKwh)
        return client.post(BASE_PATH, body).map { parseReservation(it.getJSONObject("data")) }
    }

    /**
     * PUT /reservations/my/{id} — changes the slot or energy. The API checks the 12-hour
     * notice and the 7-day window, and the reservation needs approval again afterwards.
     */
    suspend fun updateReservation(
        id: String,
        startMillis: Long,
        endMillis: Long,
        energyKwh: Double
    ): ApiResult<Reservation> {
        // Field names match UpdateReservationDto; the node and type cannot be changed
        val body = JSONObject()
            .put("slotStartTime", DateTimeUtils.formatIsoUtc(startMillis))
            .put("slotEndTime", DateTimeUtils.formatIsoUtc(endMillis))
            .put("energyAmountKWh", energyKwh)
        return client.put(reservationPath(id), body).map { parseReservation(it.getJSONObject("data")) }
    }

    /**
     * PATCH /reservations/my/{id}/cancel — the API checks the 12-hour notice rule.
     */
    suspend fun cancelReservation(id: String): ApiResult<Reservation> {
        // No reason is asked for in the app; the API records a standard one
        return client.patch(reservationPath(id) + "/cancel").map { parseReservation(it.getJSONObject("data")) }
    }

    /**
     * URL path of one reservation.
     */
    private fun reservationPath(id: String): String {
        // Encode the id so it is always safe inside a URL path
        return "$BASE_PATH/" + Uri.encode(id)
    }

    companion object {
        private const val BASE_PATH = "/reservations/my"

        /**
         * Reads one ReservationResponseDto.
         */
        fun parseReservation(json: JSONObject): Reservation {
            // The id and both slot times are required; a reply without them is unreadable
            return Reservation(
                id = json.getString("id"),
                reservationNumber = json.optText("reservationNumber"),
                nodeId = json.optText("nodeId"),
                nodeName = json.optText("nodeName"),
                type = ReservationType.fromApi(json.optText("reservationType")),
                startMillis = readTime(json, "slotStartTime"),
                endMillis = readTime(json, "slotEndTime"),
                energyKwh = json.optDouble("energyAmountKWh", 0.0),
                status = ReservationStatus.fromApi(json.optText("status")),
                qrToken = json.optText("transactionQrCode"),
                canModify = json.optBoolean("canModify", false),
                canCancel = json.optBoolean("canCancel", false)
            )
        }

        /**
         * Reads a list of ReservationResponseDto.
         */
        fun parseReservations(array: JSONArray): List<Reservation> {
            // Keeps the order the API sent (newest slot first)
            return (0 until array.length()).map { parseReservation(array.getJSONObject(it)) }
        }

        /**
         * Reads a required ISO-8601 time field as epoch milliseconds.
         */
        private fun readTime(json: JSONObject, key: String): Long {
            // A missing or unreadable time makes the whole reply unreadable
            return DateTimeUtils.parseIsoToMillis(json.optText(key))
                ?: throw JSONException("$key is missing or is not a valid time")
        }
    }
}
