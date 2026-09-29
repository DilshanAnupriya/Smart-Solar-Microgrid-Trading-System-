/*
 * File:        ReservationCacheDao.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-30
 * Description: All SQL for the reservation_cache table: the reservations last
 *              returned by the API, kept so the app can still show them (and an
 *              approved QR code) when the server can't be reached.
 */

package com.example.mobileapp.db

import android.database.Cursor
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.models.ReservationStatus
import com.example.mobileapp.models.ReservationType
import com.example.mobileapp.utils.DateTimeUtils

/**
 * A cache only: rows are written only from API replies, never from what the user typed,
 * so SQLite never holds a booking the API has not accepted.
 */
class ReservationCacheDao(private val dbHelper: DbHelper) {

    /**
     * Replaces the whole cache with the list the API just returned.
     */
    fun replaceAll(reservations: List<Reservation>) {
        // Rows are replaced, not appended, so a reservation removed on the server disappears here too
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM ${DbHelper.TABLE_RESERVATION_CACHE}")
            val syncedAt = DateTimeUtils.formatIsoUtc(System.currentTimeMillis())
            for (reservation in reservations) {
                insertOrReplace(reservation, syncedAt)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Saves or refreshes one reservation, e.g. after the API accepted a change to it.
     */
    fun upsert(reservation: Reservation) {
        // The primary key is the reservation id, so an existing row is replaced
        insertOrReplace(reservation, DateTimeUtils.formatIsoUtc(System.currentTimeMillis()))
    }

    /**
     * Every cached reservation, newest slot first (the same order as the API).
     */
    fun getAll(): List<Reservation> {
        // ISO-8601 UTC text sorts in time order
        dbHelper.readableDatabase.rawQuery(
            "SELECT $COLUMNS FROM ${DbHelper.TABLE_RESERVATION_CACHE} ORDER BY start_utc DESC",
            null
        ).use { cursor ->
            val reservations = ArrayList<Reservation>(cursor.count)
            while (cursor.moveToNext()) {
                readReservation(cursor)?.let { reservations.add(it) }
            }
            return reservations
        }
    }

    /**
     * One cached reservation, or null if it isn't in the cache.
     */
    fun get(id: String): Reservation? {
        // Primary key lookup
        dbHelper.readableDatabase.rawQuery(
            "SELECT $COLUMNS FROM ${DbHelper.TABLE_RESERVATION_CACHE} WHERE reservation_id = ?",
            arrayOf(id)
        ).use { cursor ->
            return if (cursor.moveToFirst()) readReservation(cursor) else null
        }
    }

    /**
     * Empties the cache (on sign-out, so the next user never sees these reservations).
     */
    fun clear() {
        // Removes every row
        dbHelper.writableDatabase.execSQL("DELETE FROM ${DbHelper.TABLE_RESERVATION_CACHE}")
    }

    /**
     * Writes one row, replacing any row with the same reservation id.
     */
    private fun insertOrReplace(reservation: Reservation, syncedAt: String) {
        // Times are stored as ISO-8601 UTC text, exactly as the API uses them
        dbHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO ${DbHelper.TABLE_RESERVATION_CACHE} " +
                "(reservation_id, reservation_number, node_id, node_name, reservation_type, " +
                "start_utc, end_utc, status, energy_kwh, qr_token, synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(
                reservation.id,
                reservation.reservationNumber,
                reservation.nodeId,
                reservation.nodeName,
                reservation.type.apiValue,
                DateTimeUtils.formatIsoUtc(reservation.startMillis),
                DateTimeUtils.formatIsoUtc(reservation.endMillis),
                reservation.status.apiValue,
                reservation.energyKwh,
                reservation.qrToken,
                syncedAt
            )
        )
    }

    /**
     * Builds a Reservation from the current cursor row; null if the row's times are unreadable.
     */
    private fun readReservation(cursor: Cursor): Reservation? {
        // Column positions follow COLUMNS. Changes need the API, so a cached copy allows none.
        val start = DateTimeUtils.parseIsoToMillis(cursor.getString(5)) ?: return null
        val end = DateTimeUtils.parseIsoToMillis(cursor.getString(6)) ?: return null
        return Reservation(
            id = cursor.getString(0),
            reservationNumber = cursor.getString(1).orEmpty(),
            nodeId = cursor.getString(2).orEmpty(),
            nodeName = cursor.getString(3).orEmpty(),
            type = ReservationType.fromApi(cursor.getString(4).orEmpty()),
            startMillis = start,
            endMillis = end,
            status = ReservationStatus.fromApi(cursor.getString(7).orEmpty()),
            energyKwh = cursor.getDouble(8),
            qrToken = cursor.getString(9).orEmpty(),
            canModify = false,
            canCancel = false
        )
    }

    companion object {
        // Read in this order by readReservation()
        private const val COLUMNS = "reservation_id, reservation_number, node_id, node_name, " +
            "reservation_type, start_utc, end_utc, status, energy_kwh, qr_token"
    }
}
