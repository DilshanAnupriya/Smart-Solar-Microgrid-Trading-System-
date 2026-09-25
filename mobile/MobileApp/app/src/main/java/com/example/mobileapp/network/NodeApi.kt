/*
 * File:        NodeApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: API client for microgrid nodes and solar stations. Fetches locations,
 *              GPS coordinates, battery slot capacities, and performs operator slot updates.
 */

package com.example.mobileapp.network

import android.content.Context
import android.net.Uri
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.NodeDao
import com.example.mobileapp.models.MicrogridStation
import org.json.JSONObject

class NodeApi(context: Context) {

    private val client = ApiClient(context)
    private val nodeDao = NodeDao(DbHelper.getInstance(context))

    /**
     * Lists microgrid stations, optionally filtered by search text or active state.
     */
    suspend fun getAll(search: String? = null, isActive: Boolean? = true): ApiResult<List<MicrogridStation>> {
        val queryBuilder = StringBuilder("/nodes?")
        search?.let { queryBuilder.append("search=").append(Uri.encode(it)).append("&") }
        isActive?.let { queryBuilder.append("isActive=").append(it).append("&") }

        val path = queryBuilder.toString().removeSuffix("&").removeSuffix("?")
        val result = client.get(path)

        return when (result) {
            is ApiResult.Success -> {
                val list = parseNodeList(result.data)
                nodeDao.insertAll(list)
                ApiResult.Success(list, result.message)
            }
            is ApiResult.Failure -> {
                val cached = nodeDao.getAll()
                if (cached.isNotEmpty()) {
                    ApiResult.Success(cached, "Loaded from offline cache.")
                } else {
                    result
                }
            }
        }
    }

    /**
     * Fetches nodes near a given latitude and longitude.
     */
    suspend fun getNearby(latitude: Double, longitude: Double, radiusKm: Double = 15.0): ApiResult<List<MicrogridStation>> {
        val path = "/nodes/nearby?latitude=$latitude&longitude=$longitude&radiusKm=$radiusKm"
        val result = client.get(path)

        return when (result) {
            is ApiResult.Success -> {
                val list = parseNodeList(result.data)
                nodeDao.insertAll(list)
                ApiResult.Success(list, result.message)
            }
            is ApiResult.Failure -> {
                // If nearby fails or is offline, fall back to cached nodes
                val cached = nodeDao.getAll()
                if (cached.isNotEmpty()) {
                    ApiResult.Success(cached, "Loaded from offline cache.")
                } else {
                    result
                }
            }
        }
    }

    /**
     * Gets details for a specific node by ID.
     */
    suspend fun getById(id: String): ApiResult<MicrogridStation> {
        val result = client.get("/nodes/" + Uri.encode(id))
        return when (result) {
            is ApiResult.Success -> {
                val dataObj = result.data.optJSONObject("data") ?: result.data
                val station = MicrogridStation.fromJson(dataObj)
                nodeDao.insertOrReplace(station)
                ApiResult.Success(station, result.message)
            }
            is ApiResult.Failure -> {
                val cached = nodeDao.getById(id)
                if (cached != null) {
                    ApiResult.Success(cached, "Loaded from offline cache.")
                } else {
                    result
                }
            }
        }
    }

    /**
     * Updates live battery slot availability at a station (Operator operational task).
     */
    suspend fun updateSlots(id: String, availableSlots: Int): ApiResult<MicrogridStation> {
        val body = JSONObject().put("availableBatterySlots", availableSlots)
        val result = client.patch("/nodes/" + Uri.encode(id) + "/slots", body)

        return when (result) {
            is ApiResult.Success -> {
                val dataObj = result.data.optJSONObject("data") ?: result.data
                val station = MicrogridStation.fromJson(dataObj)
                nodeDao.updateSlots(id, availableSlots)
                ApiResult.Success(station, result.message)
            }
            is ApiResult.Failure -> result
        }
    }

    companion object {
        fun parseNodeList(json: JSONObject): List<MicrogridStation> {
            val list = mutableListOf<MicrogridStation>()
            val array = json.optJSONArray("data") ?: org.json.JSONArray()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                list.add(MicrogridStation.fromJson(item))
            }
            return list
        }
    }
}
