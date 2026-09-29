/*
 * File:        GridNode.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: A microgrid node a prosumer can book, as returned by GET /nodes
 *              (NodeResponseDto) and as stored in the node_cache SQLite table.
 */

package com.example.mobileapp.models

/**
 * A solar microgrid hub where prosumers drop off or draw energy.
 * [id] is the node's database id, which is what a reservation stores as its node.
 */
data class GridNode(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val availableSlots: Int
)
