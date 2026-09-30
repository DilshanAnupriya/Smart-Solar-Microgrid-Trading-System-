/*
 * File:        UiUtils.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Small UI helpers shared by the screens: toasts, the inline error
 *              banner, and signing out back to the login screen.
 */

package com.example.mobileapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.mobileapp.auth.LoginActivity
import com.example.mobileapp.sessions.SessionManager

/**
 * Helpers so every screen shows messages and signs out in the same way.
 */
object UiUtils {

    /**
     * Shows [message] at the bottom of the screen.
     */
    fun showToast(context: Context, message: String) {
        // Long duration, because API messages are often a full sentence
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows [message] in an inline banner, or hides the banner when [message] is null.
     */
    fun showBanner(banner: TextView, message: String?) {
        // The banner keeps its space only while it has something to say
        banner.text = message
        banner.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    /**
     * Clears the saved session and opens the login screen on a fresh task, so Back
     * cannot return to a signed-in screen. Used for logout, deactivation and expired sessions.
     */
    fun signOutToLogin(activity: Activity, message: String?) {
        // Remove the token first, so nothing can use it after this point
        SessionManager(activity).clearSession()
        message?.let { showToast(activity, it) }
        activity.startActivity(
            Intent(activity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
    }

    /**
     * Up to two uppercase initials for an avatar, e.g. "Operator One" -> "OO".
     */
    fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "OP"
        return parts.take(2).joinToString("") { it.first().uppercase() }
    }
}
