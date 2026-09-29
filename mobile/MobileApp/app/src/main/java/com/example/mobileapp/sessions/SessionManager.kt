/*
 * File:        SessionManager.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Session
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: The signed-in session, kept in SQLite through LocalUserDao so the
 *              user stays signed in after the app is closed. Gives the token to
 *              ApiClient and the role and NIC to the screens.
 */

package com.example.mobileapp.sessions

import android.content.Context
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.LocalUserDao
import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.models.Prosumer
import com.example.mobileapp.utils.DateTimeUtils

/**
 * Save, read and clear the session. Nothing here decides what the user may do;
 * the API checks the token on every request.
 */
class SessionManager(context: Context) {

    private val localUserDao = LocalUserDao(DbHelper.getInstance(context))

    /**
     * Stores the account returned by a successful login.
     */
    fun saveSession(user: LocalUser) {
        // Replaces any earlier session, so only one account is ever signed in
        localUserDao.insertOrReplace(user)
    }

    /**
     * The saved account (even if its token has expired), or null.
     */
    fun getCurrentUser(): LocalUser? {
        // Read straight from the local_user table
        return localUserDao.get()
    }

    /**
     * True when an account is saved and its token has not expired yet.
     */
    fun hasValidSession(): Boolean {
        // The API would refuse an expired token, so that counts as signed out
        val user = localUserDao.get() ?: return false
        return !isExpired(user.expiresAt, System.currentTimeMillis())
    }

    /**
     * Bearer token for API calls, or null when nobody is signed in or the token has expired.
     */
    fun getToken(): String? {
        // Sending an expired token is pointless, so it is treated as no token
        val user = localUserDao.get() ?: return null
        return if (isExpired(user.expiresAt, System.currentTimeMillis())) null else user.token
    }

    /**
     * Role of the signed-in account ("Prosumer"), which decides the home screen.
     */
    fun getRole(): String? {
        // Written from the login reply, so it is whatever role the API issued
        return localUserDao.get()?.role
    }

    /**
     * NIC of the signed-in prosumer, used in the profile URLs.
     */
    fun getNic(): String? {
        // The NIC is the prosumer's primary key on the server
        return localUserDao.get()?.nic
    }

    /**
     * Keeps the saved copy in step after the profile is loaded or edited.
     */
    fun updateProfile(prosumer: Prosumer) {
        // Only the profile fields change; the token stays the same
        localUserDao.updateProfile(
            prosumer.nic, prosumer.fullName, prosumer.email, prosumer.phone, prosumer.address
        )
    }

    /**
     * Signs out on this phone: removes the saved account and its token.
     */
    fun clearSession() {
        // The account itself is untouched on the server
        localUserDao.clear()
    }

    companion object {
        /**
         * True if the ISO-8601 expiry time has passed, or cannot be read.
         */
        fun isExpired(expiresAt: String, nowMillis: Long): Boolean {
            // An unreadable expiry counts as expired, so the user simply signs in again
            val expiresMillis = DateTimeUtils.parseIsoToMillis(expiresAt) ?: return true
            return expiresMillis <= nowMillis
        }
    }
}
