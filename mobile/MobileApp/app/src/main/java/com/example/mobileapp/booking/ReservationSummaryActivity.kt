/*
 * File:        ReservationSummaryActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — shows the reservation
 *              exactly as the API returned it, with the API's message.
 * Description: Summary page shown after every reservation action (create,
 *              modify, cancel), confirming what happened and showing the
 *              reservation as it now stands.
 */

package com.example.mobileapp.booking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.models.Reservation

/**
 * Receives the reservation the API returned after the action, so nothing is re-fetched.
 */
class ReservationSummaryActivity : Activity() {

    enum class Action { CREATED, UPDATED, CANCELLED }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Both the reservation and the action are required
        super.onCreate(savedInstanceState)
        val reservation = intent.getBundleExtra(EXTRA_RESERVATION)?.let { Reservation.fromBundle(it) }
        val action = intent.getStringExtra(EXTRA_ACTION)?.let { name -> Action.entries.firstOrNull { it.name == name } }
        if (reservation == null || action == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_reservation_summary)

        val ivResultIcon = findViewById<ImageView>(R.id.ivResultIcon)
        val tvResultTitle = findViewById<TextView>(R.id.tvResultTitle)
        val tvResultMessage = findViewById<TextView>(R.id.tvResultMessage)
        val btnViewReservation = findViewById<Button>(R.id.btnViewReservation)

        // The API's own message is shown when it sent one
        val apiMessage = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        when (action) {
            Action.CREATED -> {
                showResult(ivResultIcon, success = true)
                tvResultTitle.setText(R.string.summary_created_title)
                tvResultMessage.text = apiMessage.ifBlank { getString(R.string.summary_created_message) }
            }
            Action.UPDATED -> {
                showResult(ivResultIcon, success = true)
                tvResultTitle.setText(R.string.summary_updated_title)
                tvResultMessage.text = apiMessage.ifBlank { getString(R.string.summary_updated_message) }
            }
            Action.CANCELLED -> {
                showResult(ivResultIcon, success = false)
                tvResultTitle.setText(R.string.summary_cancelled_title)
                tvResultMessage.text = apiMessage.ifBlank { getString(R.string.summary_cancelled_message) }
                // Nothing more to do with a cancelled reservation
                btnViewReservation.visibility = View.GONE
            }
        }

        findViewById<TextView>(R.id.tvReservationId).text = reservation.reservationNumber
        bindStatusBadge(findViewById(R.id.tvStatus), reservation.status)
        bindReservationDetails(findViewById(R.id.details), reservation)

        btnViewReservation.setOnClickListener {
            // Go back to the list (closing the screens above it), then open the reservation
            startActivities(
                arrayOf(listIntent(), ReservationDetailActivity.createIntent(this, reservation.id))
            )
            finish()
        }
        findViewById<Button>(R.id.btnBackToList).setOnClickListener {
            startActivity(listIntent())
            finish()
        }
    }

    /**
     * Tick on green for created / updated, cross on red for cancelled.
     */
    private fun showResult(icon: ImageView, success: Boolean) {
        // Same icon style for every action
        icon.setImageResource(if (success) R.drawable.ic_check else R.drawable.ic_close)
        icon.setBackgroundResource(if (success) R.drawable.bg_circle_success else R.drawable.bg_circle_danger)
    }

    /**
     * Reuses the existing "My reservations" screen instead of stacking a new one on top.
     */
    private fun listIntent(): Intent {
        // CLEAR_TOP closes any screens above the list
        return Intent(this, ReservationsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    companion object {
        private const val EXTRA_RESERVATION = "reservation"
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_MESSAGE = "message"

        /**
         * Opens the summary for [reservation] after [action], with the API's [message].
         */
        fun createIntent(context: Context, reservation: Reservation, action: Action, message: String): Intent {
            // The reservation travels in a Bundle exactly as the API returned it
            return Intent(context, ReservationSummaryActivity::class.java)
                .putExtra(EXTRA_RESERVATION, reservation.toBundle())
                .putExtra(EXTRA_ACTION, action.name)
                .putExtra(EXTRA_MESSAGE, message)
        }
    }
}
