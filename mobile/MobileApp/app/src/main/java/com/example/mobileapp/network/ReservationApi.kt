/*
 * File:        ReservationApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: API client for power trading reservations endpoints. Handles query
 *              filters, live stats, status updates, and automatic SQLite caching.
 */

package com.example.mobileapp.network

import android.content.Context
import android.net.Uri
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.ReservationDao
import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.models.ReservationStats
import org.json.JSONArray
import org.json.JSONObject

class ReservationApi(context: Context) {

    private val client = ApiClient(context)
    private val reservationDao = ReservationDao(DbHelper.getInstance(context))

    /**
     * Lists reservations from GET /reservations with optional filters.
     * Caches successful responses in SQLite.
     */
    suspend fun getAll(
        nic: String? = null,
        nodeId: String? = null,
        status: String? = null,
        search: String? = null
    ): ApiResult<List<ApiReservation>> {
        val queryBuilder = StringBuilder("/reservations?")
        nic?.let { queryBuilder.append("nic=").append(Uri.encode(it)).append("&") }
        nodeId?.let { queryBuilder.append("nodeId=").append(Uri.encode(it)).append("&") }
        status?.let { queryBuilder.append("status=").append(Uri.encode(it)).append("&") }
        search?.let { queryBuilder.append("search=").append(Uri.encode(it)).append("&") }

        val path = queryBuilder.toString().removeSuffix("&").removeSuffix("?")

        val result = client.get(path)
        return when (result) {
            is ApiResult.Success -> {
                val list = parseReservationList(result.data)
                // Cache to SQLite for offline access and instant refresh
                reservationDao.insertAll(list)
                ApiResult.Success(list, result.message)
            }
            is ApiResult.Failure -> {
                // If network fails, fall back to SQLite cache
                val cached = reservationDao.getAll()
                if (cached.isNotEmpty()) {
                    ApiResult.Success(cached, "Loaded from offline cache.")
                } else {
                    result
                }
            }
        }
    }

    /**
     * Retrieves details of a single reservation by ID.
     */
    suspend fun getById(id: String): ApiResult<ApiReservation> {
        val result = client.get("/reservations/" + Uri.encode(id))
        return when (result) {
            is ApiResult.Success -> {
                val reservation = ApiReservation.fromJson(result.data)
                reservationDao.insertOrReplace(reservation)
                ApiResult.Success(reservation, result.message)
            }
            is ApiResult.Failure -> {
                val cached = reservationDao.findByIdOrNumber(id)
                if (cached != null) {
                    ApiResult.Success(cached, "Loaded from offline cache.")
                } else {
                    result
                }
            }
        }
    }

    /**
     * Creates a new power trading reservation via POST /reservations.
     * Caches the created reservation in SQLite.
     */
    suspend fun createReservation(
        nodeId: String,
        nodeName: String,
        slotStartTimeUtc: String,
        slotEndTimeUtc: String,
        energyAmountKWh: Double,
        reservationType: String,
        prosumerNic: String? = null,
        prosumerName: String? = null,
        notes: String? = null
    ): ApiResult<ApiReservation> {
        val body = JSONObject().apply {
            put("nodeId", nodeId)
            put("nodeName", nodeName)
            put("slotStartTime", slotStartTimeUtc)
            put("slotEndTime", slotEndTimeUtc)
            put("energyAmountKWh", energyAmountKWh)
            put("reservationType", reservationType)
            if (!prosumerNic.isNullOrBlank()) put("prosumerNic", prosumerNic)
            if (!prosumerName.isNullOrBlank()) put("prosumerName", prosumerName)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }

        val result = client.post("/reservations", body)
        return when (result) {
            is ApiResult.Success -> {
                val created = ApiReservation.fromJson(result.data)
                reservationDao.insertOrReplace(created)
                ApiResult.Success(created, result.message)
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Updates an existing reservation slot time and energy amount via PUT /reservations/{id}.
     */
    suspend fun updateReservation(
        id: String,
        slotStartTimeUtc: String,
        slotEndTimeUtc: String,
        energyAmountKWh: Double,
        notes: String? = null
    ): ApiResult<ApiReservation> {
        val body = JSONObject().apply {
            put("slotStartTime", slotStartTimeUtc)
            put("slotEndTime", slotEndTimeUtc)
            put("energyAmountKWh", energyAmountKWh)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }

        val result = client.put("/reservations/" + Uri.encode(id), body)
        return when (result) {
            is ApiResult.Success -> {
                val updated = ApiReservation.fromJson(result.data)
                reservationDao.insertOrReplace(updated)
                ApiResult.Success(updated, result.message)
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Cancels an existing reservation via PATCH /reservations/{id}/cancel.
     */
    suspend fun cancelReservation(id: String, reason: String = "Cancelled by user via mobile app"): ApiResult<ApiReservation> {
        val body = JSONObject().apply {
            put("reason", reason)
        }

        val result = client.patch("/reservations/" + Uri.encode(id) + "/cancel", body)
        return when (result) {
            is ApiResult.Success -> {
                val cancelled = ApiReservation.fromJson(result.data)
                reservationDao.insertOrReplace(cancelled)
                ApiResult.Success(cancelled, result.message)
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Retrieves aggregated counts for the Grid Operator operational dashboard.
     */
    suspend fun getStats(): ApiResult<ReservationStats> {
        return client.get("/reservations/stats").map { json ->
            ReservationStats.fromJson(json)
        }
    }

    /**
     * Updates the status of a reservation (e.g. to "Completed" to finalize energy transfer).
     */
    suspend fun updateStatus(id: String, status: String, notes: String? = null): ApiResult<ApiReservation> {
        val body = JSONObject()
            .put("status", status)
        if (!notes.isNullOrBlank()) {
            body.put("notes", notes)
        }

        val result = client.patch("/reservations/" + Uri.encode(id) + "/status", body)
        return when (result) {
            is ApiResult.Success -> {
                val updated = ApiReservation.fromJson(result.data)
                reservationDao.insertOrReplace(updated)
                ApiResult.Success(updated, result.message)
            }
            is ApiResult.Failure -> result
        }
    }

    /**
     * Searches for a reservation using either MongoDB ObjectId, reservation number, or QR token key.
     */
    suspend fun findByReference(reference: String): ApiResult<ApiReservation> {
        val clean = reference.trim().removeSurrounding("\"").removeSurrounding("'")
        if (clean.isBlank()) {
            return ApiResult.Failure("Reference code cannot be empty.", 400)
        }

        // 1. Try direct ID or reservation number lookup via GET /reservations/{clean}
        val direct = getById(clean)
        if (direct is ApiResult.Success) {
            return direct
        }

        // 2. Search local SQLite cache as an immediate fallback
        val cached = reservationDao.findByIdOrNumber(clean)
        if (cached != null) {
            return ApiResult.Success(cached, "Loaded from offline cache.")
        }

        // 3. Search by query string (matches reservation number, NIC, or node name)
        val searchResult = getAll(search = clean)
        return when (searchResult) {
            is ApiResult.Success -> {
                val match = searchResult.data.firstOrNull {
                    it.reservationNumber.equals(clean, ignoreCase = true) ||
                        it.id.equals(clean, ignoreCase = true) ||
                        it.transactionQrCode.contains(clean, ignoreCase = true)
                }

                if (match != null) {
                    ApiResult.Success(match, "Reservation located.")
                } else {
                    ApiResult.Failure("No reservation found matching reference '$clean'.", 404)
                }
            }
            is ApiResult.Failure -> {
                ApiResult.Failure("No reservation found matching reference '$clean'.", 404)
            }
        }
    }

    companion object {
        fun parseReservationList(json: JSONObject): List<ApiReservation> {
            val list = mutableListOf<ApiReservation>()
            val array = json.optJSONArray("data") ?: org.json.JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                list.add(ApiReservation.fromJson(item))
            }
            return list
        }
    }
}
