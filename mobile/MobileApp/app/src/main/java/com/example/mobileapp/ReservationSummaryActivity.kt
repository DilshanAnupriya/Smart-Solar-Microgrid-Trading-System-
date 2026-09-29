package com.example.mobileapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

/**
 * Summary page shown after every reservation action (create, modify, cancel),
 * confirming what happened and showing the reservation as it now stands.
 */
class ReservationSummaryActivity : Activity() {

    enum class Action { CREATED, UPDATED, CANCELLED }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reservation = intent.getStringExtra(EXTRA_RESERVATION_ID)?.let { ReservationStore.find(it) }
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

        when (action) {
            Action.CREATED -> {
                showResult(ivResultIcon, success = true)
                tvResultTitle.setText(R.string.summary_created_title)
                tvResultMessage.setText(R.string.summary_created_message)
            }
            Action.UPDATED -> {
                showResult(ivResultIcon, success = true)
                tvResultTitle.setText(R.string.summary_updated_title)
                tvResultMessage.setText(R.string.summary_updated_message)
            }
            Action.CANCELLED -> {
                showResult(ivResultIcon, success = false)
                tvResultTitle.setText(R.string.summary_cancelled_title)
                tvResultMessage.setText(R.string.summary_cancelled_message)
                // Nothing more to do with a cancelled reservation
                btnViewReservation.visibility = View.GONE
            }
        }

        findViewById<TextView>(R.id.tvReservationId).text = reservation.id
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

    // Tick on green for created / updated, cross on red for cancelled
    private fun showResult(icon: ImageView, success: Boolean) {
        icon.setImageResource(if (success) R.drawable.ic_check else R.drawable.ic_close)
        icon.setBackgroundResource(if (success) R.drawable.bg_circle_success else R.drawable.bg_circle_danger)
    }

    // Reuses the existing "My reservations" screen instead of stacking a new one on top
    private fun listIntent(): Intent =
        Intent(this, ReservationsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    companion object {
        private const val EXTRA_RESERVATION_ID = "reservation_id"
        private const val EXTRA_ACTION = "action"

        fun createIntent(context: Context, reservationId: String, action: Action): Intent =
            Intent(context, ReservationSummaryActivity::class.java)
                .putExtra(EXTRA_RESERVATION_ID, reservationId)
                .putExtra(EXTRA_ACTION, action.name)
    }
}
