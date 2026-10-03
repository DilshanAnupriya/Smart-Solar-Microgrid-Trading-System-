/*
 * File:        ReservationDao.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: SQLite Data Access Object for local persistence and caching of
 *              power trading reservations in reservation_cache table.
 */

package com.example.mobileapp.db

import android.database.Cursor
import com.example.mobileapp.models.ApiReservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Manages local persistence for reservations.
 */
class ReservationDao(private val dbHelper: DbHelper) {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Caches or updates a single reservation.
     */
    fun insertOrReplace(reservation: ApiReservation) {
        val db = dbHelper.writableDatabase
        val syncedAt = isoFormat.format(Date())
        db.execSQL(
            "INSERT OR REPLACE INTO ${DbHelper.TABLE_RESERVATION_CACHE} " +
                "(reservation_id, reservation_number, node_id, node_name, reservation_type, " +
                "start_utc, end_utc, status, energy_kwh, qr_token, synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(
                reservation.id,
                reservation.reservationNumber,
                reservation.nodeId,
                reservation.nodeName,
                reservation.reservationType,
                reservation.slotStartTime,
                reservation.slotEndTime,
                reservation.status,
                reservation.energyAmountKWh,
                reservation.transactionQrCode,
                syncedAt
            )
        )
    }

    /**
     * Caches a batch of reservations within a transaction.
     */
    fun insertAll(reservations: List<ApiReservation>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val syncedAt = isoFormat.format(Date())
            for (res in reservations) {
                db.execSQL(
                    "INSERT OR REPLACE INTO ${DbHelper.TABLE_RESERVATION_CACHE} " +
                        "(reservation_id, reservation_number, node_id, node_name, reservation_type, " +
                        "start_utc, end_utc, status, energy_kwh, qr_token, synced_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        res.id,
                        res.reservationNumber,
                        res.nodeId,
                        res.nodeName,
                        res.reservationType,
                        res.slotStartTime,
                        res.slotEndTime,
                        res.status,
                        res.energyAmountKWh,
                        res.transactionQrCode,
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
     * Reads all cached reservations, most recent first.
     */
    fun getAll(): List<ApiReservation> {
        val list = mutableListOf<ApiReservation>()
        dbHelper.readableDatabase.rawQuery(
            "SELECT reservation_id, reservation_number, node_id, node_name, reservation_type, " +
                "start_utc, end_utc, status, energy_kwh, qr_token FROM ${DbHelper.TABLE_RESERVATION_CACHE} " +
                "ORDER BY start_utc DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(mapCursor(cursor))
            }
        }
        return list
    }

    /**
     * Finds a reservation by its ID or reservation number.
     */
    fun findByIdOrNumber(idOrNumber: String): ApiReservation? {
        dbHelper.readableDatabase.rawQuery(
            "SELECT reservation_id, reservation_number, node_id, node_name, reservation_type, " +
                "start_utc, end_utc, status, energy_kwh, qr_token FROM ${DbHelper.TABLE_RESERVATION_CACHE} " +
                "WHERE reservation_id = ? OR reservation_number = ? LIMIT 1",
            arrayOf(idOrNumber, idOrNumber)
        ).use { cursor ->
            return if (cursor.moveToFirst()) mapCursor(cursor) else null
        }
    }

    /**
     * Updates the status of a cached reservation (e.g. after finalizing energy transfer).
     */
    fun updateStatus(id: String, newStatus: String) {
        dbHelper.writableDatabase.execSQL(
            "UPDATE ${DbHelper.TABLE_RESERVATION_CACHE} SET status = ? WHERE reservation_id = ? OR reservation_number = ?",
            arrayOf<Any>(newStatus, id, id)
        )
    }

    /**
     * Clears cached reservations.
     */
    fun clear() {
        dbHelper.writableDatabase.execSQL("DELETE FROM ${DbHelper.TABLE_RESERVATION_CACHE}")
    }

    private fun mapCursor(cursor: Cursor): ApiReservation {
        return ApiReservation(
            id = cursor.getString(0) ?: "",
            reservationNumber = cursor.getString(1) ?: "",
            prosumerNic = "",
            prosumerName = "Solar Prosumer",
            nodeId = cursor.getString(2) ?: "",
            nodeName = cursor.getString(3) ?: "",
            reservationType = cursor.getString(4) ?: "DropOff",
            slotStartTime = cursor.getString(5) ?: "",
            slotEndTime = cursor.getString(6) ?: "",
            status = cursor.getString(7) ?: "Pending",
            energyAmountKWh = cursor.getDouble(8),
            transactionQrCode = cursor.getString(9) ?: "",
            cancellationReason = null,
            cancelledAt = null,
            notes = null,
            createdAt = "",
            updatedAt = "",
            createdBy = "",
            canModify = false,
            canCancel = false,
            hoursUntilSlot = 0.0
        )
    }
}
