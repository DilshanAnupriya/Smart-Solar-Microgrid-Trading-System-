/*
 * File:        NodeCacheDao.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: All SQL for the node_cache table: the grid nodes last returned
 *              by the API, shown in the node picker when the server can't be reached.
 */

package com.example.mobileapp.db

import android.database.Cursor
import com.example.mobileapp.models.GridNode
import com.example.mobileapp.utils.DateTimeUtils

/**
 * A cache only: it is rewritten from every node list the API returns.
 */
class NodeCacheDao(private val dbHelper: DbHelper) {

    /**
     * Replaces the whole cache with the list the API just returned.
     */
    fun replaceAll(nodes: List<GridNode>) {
        // Rows are replaced, not appended, so a node switched off on the server disappears here too
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM ${DbHelper.TABLE_NODE_CACHE}")
            val syncedAt = DateTimeUtils.formatIsoUtc(System.currentTimeMillis())
            for (node in nodes) {
                db.execSQL(
                    "INSERT OR REPLACE INTO ${DbHelper.TABLE_NODE_CACHE} " +
                        "(node_id, name, address, latitude, longitude, available_slots, synced_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        node.id, node.name, node.address, node.latitude,
                        node.longitude, node.availableSlots, syncedAt
                    )
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Every cached node, in name order.
     */
    fun getAll(): List<GridNode> {
        // Same order the picker shows them in
        dbHelper.readableDatabase.rawQuery(
            "SELECT node_id, name, address, latitude, longitude, available_slots " +
                "FROM ${DbHelper.TABLE_NODE_CACHE} ORDER BY name",
            null
        ).use { cursor ->
            val nodes = ArrayList<GridNode>(cursor.count)
            while (cursor.moveToNext()) {
                nodes.add(readNode(cursor))
            }
            return nodes
        }
    }

    /**
     * Empties the cache (on sign-out).
     */
    fun clear() {
        // Removes every row
        dbHelper.writableDatabase.execSQL("DELETE FROM ${DbHelper.TABLE_NODE_CACHE}")
    }

    /**
     * Builds a GridNode from the current cursor row.
     */
    private fun readNode(cursor: Cursor): GridNode {
        // Column positions follow the SELECT list in getAll()
        return GridNode(
            id = cursor.getString(0),
            name = cursor.getString(1).orEmpty(),
            address = cursor.getString(2).orEmpty(),
            latitude = cursor.getDouble(3),
            longitude = cursor.getDouble(4),
            availableSlots = cursor.getInt(5)
        )
    }
}
