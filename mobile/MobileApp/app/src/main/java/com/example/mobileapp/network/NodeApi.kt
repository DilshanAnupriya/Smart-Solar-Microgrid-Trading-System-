/*
 * File:        NodeApi.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: Reads the grid nodes a prosumer can book from GET /nodes.
 */

package com.example.mobileapp.network

import android.content.Context
import com.example.mobileapp.models.GridNode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grid node calls used by the booking screen.
 */
class NodeApi(context: Context) {

    private val client = ApiClient(context)

    /**
     * GET /nodes. For a prosumer the API returns active nodes only, so whether a node
     * can take reservations is never decided on the phone.
     */
    suspend fun getNodes(): ApiResult<List<GridNode>> {
        // The list is inside the "data" field of the reply
        return client.get("/nodes").map { parseNodes(it.getJSONArray("data")) }
    }

    companion object {
        /**
         * Reads one NodeResponseDto.
         */
        fun parseNode(json: JSONObject): GridNode {
            // The id is required; it is what a reservation stores as its node
            return GridNode(
                id = json.getString("id"),
                name = json.optText("name"),
                address = json.optText("address"),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                availableSlots = json.optInt("availableBatterySlots", 0)
            )
        }

        /**
         * Reads a list of NodeResponseDto.
         */
        fun parseNodes(array: JSONArray): List<GridNode> {
            // Keeps the order the API sent
            return (0 until array.length()).map { parseNode(array.getJSONObject(it)) }
        }
    }
}
