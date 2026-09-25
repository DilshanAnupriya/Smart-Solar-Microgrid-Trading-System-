/*
 * File:        NodeDao.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: SQLite Data Access Object for local persistence and offline reference
 *              data caching of microgrid station hubs in node_cache table.
 */

package com.example.mobileapp.db

import android.database.Cursor
import com.example.mobileapp.models.MicrogridStation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Manages local persistence for microgrid stations.
 */
class NodeDao(private val dbHelper: DbHelper) {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Caches or replaces a microgrid station node.
     */
    fun insertOrReplace(station: MicrogridStation) {
        val db = dbHelper.writableDatabase
        val syncedAt = isoFormat.format(Date())
        db.execSQL(
            "INSERT OR REPLACE INTO ${DbHelper.TABLE_NODE_CACHE} " +
                "(node_id, name, address, latitude, longitude, available_slots, synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(
                station.id,
                station.name,
                station.address,
                station.latitude,
                station.longitude,
                station.availableBatterySlots,
                syncedAt
            )
        )
    }

    /**
     * Inserts multiple stations in a single transaction.
     */
    fun insertAll(stations: List<MicrogridStation>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val syncedAt = isoFormat.format(Date())
            for (st in stations) {
                db.execSQL(
                    "INSERT OR REPLACE INTO ${DbHelper.TABLE_NODE_CACHE} " +
                        "(node_id, name, address, latitude, longitude, available_slots, synced_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        st.id,
                        st.name,
                        st.address,
                        st.latitude,
                        st.longitude,
                        st.availableBatterySlots,
                        syncedAt
                    )
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Returns all cached microgrid stations.
     */
    fun getAll(): List<MicrogridStation> {
        val list = mutableListOf<MicrogridStation>()
        dbHelper.readableDatabase.rawQuery(
            "SELECT node_id, name, address, latitude, longitude, available_slots FROM ${DbHelper.TABLE_NODE_CACHE}",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor))
            }
        }
        return list
    }

    /**
     * Gets a node by its ID.
     */
    fun getById(id: String): MicrogridStation? {
        dbHelper.readableDatabase.rawQuery(
            "SELECT node_id, name, address, latitude, longitude, available_slots FROM ${DbHelper.TABLE_NODE_CACHE} WHERE node_id = ? LIMIT 1",
            arrayOf(id)
        ).use { cursor ->
            return if (cursor.moveToFirst()) mapCursor(cursor) else null
        }
    }

    /**
     * Updates available battery slots for a station.
     */
    fun updateSlots(nodeId: String, availableSlots: Int) {
        dbHelper.writableDatabase.execSQL(
            "UPDATE ${DbHelper.TABLE_NODE_CACHE} SET available_slots = ? WHERE node_id = ?",
            arrayOf<Any>(availableSlots, nodeId)
        )
    }

    /**
     * Clears cached nodes.
     */
    fun clear() {
        dbHelper.writableDatabase.execSQL("DELETE FROM ${DbHelper.TABLE_NODE_CACHE}")
    }

    private fun mapCursor(cursor: Cursor): MicrogridStation {
        val id = cursor.getString(0) ?: ""
        return MicrogridStation(
            id = id,
            nodeCode = id,
            name = cursor.getString(1) ?: "Microgrid Hub",
            address = cursor.getString(2) ?: "",
            latitude = cursor.getDouble(3),
            longitude = cursor.getDouble(4),
            generationCapacityKw = 50.0,
            storageCapacityKWh = 100.0,
            totalBatterySlots = 10,
            availableBatterySlots = cursor.getInt(5),
            isActive = true,
            distanceKm = null
        )
    }
}
