/*
 * File:        LocalUserDao.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: All SQL for the local_user table: save the signed-in account,
 *              read it back, keep its profile fields current and clear it.
 */

package com.example.mobileapp.db

import android.database.Cursor
import com.example.mobileapp.models.LocalUser

/**
 * Reads and writes the one account signed in on this phone.
 */
class LocalUserDao(private val dbHelper: DbHelper) {

    /**
     * Saves the signed-in account, replacing whoever was signed in before.
     */
    fun insertOrReplace(user: LocalUser) {
        // Only one account may be signed in, so empty the table and insert in one transaction
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM ${DbHelper.TABLE_LOCAL_USER}")
            db.execSQL(
                "INSERT OR REPLACE INTO ${DbHelper.TABLE_LOCAL_USER} " +
                    "(id, nic, full_name, email, phone, address, role, token, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any>(
                    user.id, user.nic, user.fullName, user.email, user.phone,
                    user.address, user.role, user.token, user.expiresAt
                )
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * The signed-in account, or null when nobody is signed in.
     */
    fun get(): LocalUser? {
        // There is at most one row, so the first row is the signed-in account
        dbHelper.readableDatabase.rawQuery(
            "SELECT id, nic, full_name, email, phone, address, role, token, expires_at " +
                "FROM ${DbHelper.TABLE_LOCAL_USER} LIMIT 1",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) readUser(cursor) else null
        }
    }

    /**
     * Copies profile edits into the saved account, so the next launch shows them.
     */
    fun updateProfile(nic: String, fullName: String, email: String, phone: String, address: String) {
        // The token, role and expiry are not affected by a profile edit
        dbHelper.writableDatabase.execSQL(
            "UPDATE ${DbHelper.TABLE_LOCAL_USER} " +
                "SET full_name = ?, email = ?, phone = ?, address = ? WHERE nic = ?",
            arrayOf<Any>(fullName, email, phone, address, nic)
        )
    }

    /**
     * Removes the saved account (logout, deactivation or an expired session).
     */
    fun clear() {
        // Deleting the row also deletes the token, so no later call can use it
        dbHelper.writableDatabase.execSQL("DELETE FROM ${DbHelper.TABLE_LOCAL_USER}")
    }

    /**
     * Builds a LocalUser from the current cursor row.
     */
    private fun readUser(cursor: Cursor): LocalUser {
        // Column positions follow the SELECT list in get()
        return LocalUser(
            id = cursor.getString(0),
            nic = cursor.getString(1),
            fullName = cursor.getString(2).orEmpty(),
            email = cursor.getString(3).orEmpty(),
            phone = cursor.getString(4).orEmpty(),
            address = cursor.getString(5).orEmpty(),
            role = cursor.getString(6),
            token = cursor.getString(7),
            expiresAt = cursor.getString(8)
        )
    }
}
