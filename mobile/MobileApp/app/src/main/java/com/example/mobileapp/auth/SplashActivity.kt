/*
 * File:        SplashActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Description: Launcher screen. Reads the session saved in SQLite and opens the
 *              prosumer's home screen, or the login screen when nobody is signed in.
 */

package com.example.mobileapp.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.profile.ProfileActivity
import com.example.mobileapp.sessions.SessionManager

/**
 * Routes by the saved session and closes itself; it has no layout of its own.
 */
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // A saved, unexpired prosumer session goes straight to the profile (the prosumer's home)
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        val next = if (sessionManager.hasValidSession() && sessionManager.getRole() == LocalUser.ROLE_PROSUMER) {
            ProfileActivity::class.java
        } else {
            // Drop an expired session so the login screen starts clean
            sessionManager.clearSession()
            LoginActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
