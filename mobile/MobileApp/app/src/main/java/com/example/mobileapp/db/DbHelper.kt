/*
 * File:        DbHelper.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Data
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Opens smartsolar.db, the app's SQLite database, and creates its
 *              three tables on first run. SQLite is only a session store and a
 *              cache here; the Web API is always the source of truth.
 */

package com.example.mobileapp.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The app's single SQLiteOpenHelper. The table definitions live here; the queries for
 * each table live in that table's DAO, so no screen ever touches the database directly.
 */
class DbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // First launch: create every table
        db.execSQL(CREATE_LOCAL_USER)
        db.execSQL(CREATE_RESERVATION_CACHE)
        db.execSQL(CREATE_NODE_CACHE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nothing stored here is the only copy (the API has it all), so an upgrade just
        // rebuilds the tables; the user signs in again afterwards
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCAL_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESERVATION_CACHE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NODE_CACHE")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "smartsolar.db"
        const val DATABASE_VERSION = 1

        // The account signed in on this phone: at most one row, written on login and
        // removed on logout or deactivation
        const val TABLE_LOCAL_USER = "local_user"

        // Reservations last fetched from the API; the rows are replaced on every fetch
        const val TABLE_RESERVATION_CACHE = "reservation_cache"

        // Grid nodes last fetched from the API
        const val TABLE_NODE_CACHE = "node_cache"

        private const val CREATE_LOCAL_USER = """
            CREATE TABLE local_user (
                id          TEXT PRIMARY KEY,
                nic         TEXT NOT NULL,
                full_name   TEXT,
                email       TEXT,
                phone       TEXT,
                address     TEXT,
                role        TEXT NOT NULL,
                token       TEXT NOT NULL,
                expires_at  TEXT NOT NULL
            )"""

        private const val CREATE_RESERVATION_CACHE = """
            CREATE TABLE reservation_cache (
                reservation_id      TEXT PRIMARY KEY,
                reservation_number  TEXT,
                node_id             TEXT,
                node_name           TEXT,
                reservation_type    TEXT,
                start_utc           TEXT,
                end_utc             TEXT,
                status              TEXT,
                energy_kwh          REAL,
                qr_token            TEXT,
                synced_at           TEXT
            )"""

        private const val CREATE_NODE_CACHE = """
            CREATE TABLE node_cache (
                node_id          TEXT PRIMARY KEY,
                name             TEXT,
                address          TEXT,
                latitude         REAL,
                longitude        REAL,
                available_slots  INTEGER,
                synced_at        TEXT
            )"""

        @Volatile
        private var instance: DbHelper? = null

        /**
         * The one shared helper for the whole app, so every screen uses the same connection.
         */
        fun getInstance(context: Context): DbHelper {
            // Created once; the application context is used so no screen is kept in memory
            return instance ?: synchronized(this) {
                instance ?: DbHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
