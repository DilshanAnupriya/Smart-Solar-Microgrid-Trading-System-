package com.example.mobileapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

/**
 * One reservation. Modify and Cancel appear in the top bar only while the reservation is
 * active and at least 12 hours away; the transaction QR code appears once it's approved.
 */
class ReservationDetailActivity : Activity() {

    private lateinit var reservationId: String

    private lateinit var btnModify: Button
    private lateinit var btnCancel: Button
    private lateinit var tvReservationId: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLockedBanner: TextView
    private lateinit var tvPendingBanner: TextView
    private lateinit var cardQr: View
    private lateinit var ivQr: ImageView
    private lateinit var tvQrCode: TextView
    private lateinit var details: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getStringExtra(EXTRA_RESERVATION_ID)
        if (id == null) {
            finish()
            return
        }
        reservationId = id

        setContentView(R.layout.activity_reservation_detail)

        btnModify = findViewById(R.id.btnModify)
        btnCancel = findViewById(R.id.btnCancel)
        tvReservationId = findViewById(R.id.tvReservationId)
        tvStatus = findViewById(R.id.tvStatus)
        tvLockedBanner = findViewById(R.id.tvLockedBanner)
        tvPendingBanner = findViewById(R.id.tvPendingBanner)
        cardQr = findViewById(R.id.cardQr)
        ivQr = findViewById(R.id.ivQr)
        tvQrCode = findViewById(R.id.tvQrCode)
        details = findViewById(R.id.details)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnModify.setOnClickListener {
            startActivity(ReservationFormActivity.modifyReservationIntent(this, reservationId))
        }
        btnCancel.setOnClickListener { confirmCancel() }
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from Modify or the summary page
        showReservation()
    }

    private fun showReservation() {
        val reservation = ReservationStore.find(reservationId)
        if (reservation == null) {
            finish()
            return
        }

        tvReservationId.text = reservation.id
        bindStatusBadge(tvStatus, reservation.status)
        bindReservationDetails(details, reservation)

        // 12-hour rule: only offer changes the server would accept
        val canChange = reservation.isActive &&
            ReservationRules.canModifyOrCancel(reservation.startMillis, System.currentTimeMillis())
        val actionsVisibility = if (canChange) View.VISIBLE else View.GONE
        btnModify.visibility = actionsVisibility
        btnCancel.visibility = actionsVisibility
        tvLockedBanner.visibility = if (reservation.isActive && !canChange) View.VISIBLE else View.GONE

        tvPendingBanner.visibility =
            if (reservation.status == ReservationStatus.PENDING) View.VISIBLE else View.GONE

        val qrPayload = reservation.qrPayload()
        if (qrPayload != null) {
            val sizePx = resources.getDimensionPixelSize(R.dimen.qr_code_size)
            ivQr.setImageBitmap(QrCodeGenerator.generate(qrPayload, sizePx))
            ivQr.contentDescription = getString(R.string.reservation_qr_description, reservation.id)
            tvQrCode.text = reservation.id
            cardQr.visibility = View.VISIBLE
        } else {
            cardQr.visibility = View.GONE
        }
    }

    private fun confirmCancel() {
        val reservation = ReservationStore.find(reservationId) ?: return

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.reservation_cancel_title)
            .setMessage(
                getString(
                    R.string.reservation_cancel_message,
                    reservation.node.name,
                    formatReservationDate(reservation.startMillis)
                )
            )
            .setNegativeButton(R.string.reservation_cancel_keep, null)
            .setPositiveButton(R.string.reservation_cancel_confirm) { _, _ -> cancelReservation() }
            .create()
        dialog.show()

        // Colour the destructive action red so it stands out
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.danger))
    }

    private fun cancelReservation() {
        val reservation = ReservationStore.find(reservationId) ?: return

        // Check the 12-hour rule again: the screen may have been open for a while
        if (!ReservationRules.canModifyOrCancel(reservation.startMillis, System.currentTimeMillis())) {
            Toast.makeText(this, R.string.reservation_change_locked, Toast.LENGTH_LONG).show()
            showReservation()
            return
        }

        // UI only for now: PATCH /api/reservations/{id}/cancel goes here
        ReservationStore.cancel(reservationId)
        startActivity(
            ReservationSummaryActivity.createIntent(this, reservationId, ReservationSummaryActivity.Action.CANCELLED)
        )
    }

    companion object {
        private const val EXTRA_RESERVATION_ID = "reservation_id"

        fun createIntent(context: Context, reservationId: String): Intent =
            Intent(context, ReservationDetailActivity::class.java).putExtra(EXTRA_RESERVATION_ID, reservationId)
    }
}
