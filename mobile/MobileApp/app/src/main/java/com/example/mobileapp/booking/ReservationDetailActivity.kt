/*
 * File:        ReservationDetailActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — loads the reservation
 *              from the API, draws the server's QR token once it is approved,
 *              cancels through the API and follows the API's canModify and
 *              canCancel instead of checking the 12-hour rule on the phone.
 * Description: One reservation, with Modify / Cancel and the transaction QR code.
 */

package com.example.mobileapp.booking

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.ReservationCacheDao
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.models.ReservationStatus
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.QrCodeGenerator
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch

/**
 * Modify and Cancel appear only when the API says the change is still allowed; the
 * transaction QR code appears only once the API has approved the reservation.
 */
class ReservationDetailActivity : BaseActivity() {

    private lateinit var reservationId: String
    private lateinit var reservationCache: ReservationCacheDao

    // What is on screen, and whether it is the SQLite copy rather than the API's answer
    private var reservation: Reservation? = null
    private var isSavedCopy = false

    private lateinit var progressLoading: ProgressBar
    private lateinit var tvOfflineBanner: TextView
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
        // The reservation id is required to load anything
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_RESERVATION_ID)
        if (id == null) {
            finish()
            return
        }
        reservationId = id
        reservationCache = ReservationCacheDao(DbHelper.getInstance(this))

        setContentView(R.layout.activity_reservation_detail)

        progressLoading = findViewById(R.id.progressLoading)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
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

        // Nothing can be changed until the API has said so for this reservation
        btnModify.visibility = View.GONE
        btnCancel.visibility = View.GONE

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnModify.setOnClickListener {
            reservation?.let { startActivity(ReservationFormActivity.modifyReservationIntent(this, it)) }
        }
        btnCancel.setOnClickListener { confirmCancel() }
        tvOfflineBanner.setOnClickListener { loadReservation() }
    }

    override fun onResume() {
        // Refresh after returning from Modify or the summary page
        super.onResume()
        reservationCache.get(reservationId)?.let { saved ->
            isSavedCopy = true
            showReservation(saved)
        }
        loadReservation()
    }

    /**
     * GET /reservations/my/{id}; on success the SQLite copy is refreshed too.
     */
    private fun loadReservation() {
        // Any saved copy stays on screen while this runs
        progressLoading.visibility = View.VISIBLE
        uiScope.launch {
            val result = ReservationApi(this@ReservationDetailActivity).getReservation(reservationId)
            progressLoading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> {
                    reservationCache.upsert(result.data)
                    isSavedCopy = false
                    UiUtils.showBanner(tvOfflineBanner, null)
                    showReservation(result.data)
                }
                is ApiResult.Failure -> showLoadFailure(result)
            }
        }
    }

    /**
     * Handles a failed load: expired session, a reservation that is gone, or no connection.
     */
    private fun showLoadFailure(failure: ApiResult.Failure) {
        // Only a connection problem keeps the screen open with the saved copy
        when {
            failure.isUnauthorized ->
                UiUtils.signOutToLogin(this, getString(R.string.session_expired))
            failure.statusCode == 403 || failure.statusCode == 404 -> {
                UiUtils.showToast(this, failure.message)
                finish()
            }
            else -> UiUtils.showBanner(
                tvOfflineBanner,
                getString(R.string.reservation_offline_banner, failure.message)
            )
        }
    }

    /**
     * Writes [shown] into the views and decides which actions and banners appear.
     */
    private fun showReservation(shown: Reservation) {
        // Every decision below comes from the API's reply, not from rules on the phone
        reservation = shown

        tvReservationId.text = shown.reservationNumber
        bindStatusBadge(tvStatus, shown.status)
        bindReservationDetails(details, shown)

        // The API's canModify / canCancel already include the 12-hour notice rule
        btnModify.visibility = if (shown.canModify) View.VISIBLE else View.GONE
        btnCancel.visibility = if (shown.canCancel) View.VISIBLE else View.GONE

        // Still active but the API refuses changes: it starts within 12 hours. A saved copy
        // never allows changes, so it doesn't count as locked.
        tvLockedBanner.visibility =
            if (!isSavedCopy && shown.isActive && !shown.canModify && !shown.canCancel) View.VISIBLE else View.GONE

        tvPendingBanner.visibility =
            if (shown.status == ReservationStatus.PENDING) View.VISIBLE else View.GONE

        showQrCode(shown)
    }

    /**
     * Draws the QR code for an approved reservation, or hides the card.
     */
    private fun showQrCode(shown: Reservation) {
        // The content is the server's token exactly as received, so the operator's scan can be
        // verified against the API; nothing about it is built on the phone
        if (shown.hasQrCode) {
            val sizePx = resources.getDimensionPixelSize(R.dimen.qr_code_size)
            ivQr.setImageBitmap(QrCodeGenerator.generate(shown.qrToken, sizePx))
            ivQr.contentDescription = getString(R.string.reservation_qr_description, shown.reservationNumber)
            tvQrCode.text = shown.reservationNumber
            cardQr.visibility = View.VISIBLE
        } else {
            cardQr.visibility = View.GONE
        }
    }

    /**
     * Asks for confirmation before cancelling.
     */
    private fun confirmCancel() {
        // Cancelling can't be undone, so make sure it's intended
        val shown = reservation ?: return

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.reservation_cancel_title)
            .setMessage(
                getString(
                    R.string.reservation_cancel_message,
                    shown.nodeName,
                    formatReservationDate(shown.startMillis)
                )
            )
            .setNegativeButton(R.string.reservation_cancel_keep, null)
            .setPositiveButton(R.string.reservation_cancel_confirm) { _, _ -> cancelReservation() }
            .create()
        dialog.show()

        // Colour the destructive action red so it stands out
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.danger))
    }

    /**
     * PATCH /reservations/my/{id}/cancel. The API checks the 12-hour rule; if it refuses,
     * its reason is shown and the screen is refreshed.
     */
    private fun cancelReservation() {
        // Lock the actions while the request runs
        progressLoading.visibility = View.VISIBLE
        btnModify.isEnabled = false
        btnCancel.isEnabled = false

        uiScope.launch {
            val result = ReservationApi(this@ReservationDetailActivity).cancelReservation(reservationId)
            progressLoading.visibility = View.GONE
            btnModify.isEnabled = true
            btnCancel.isEnabled = true

            when (result) {
                is ApiResult.Success -> {
                    reservationCache.upsert(result.data)
                    startActivity(
                        ReservationSummaryActivity.createIntent(
                            this@ReservationDetailActivity,
                            result.data,
                            ReservationSummaryActivity.Action.CANCELLED,
                            result.message
                        )
                    )
                }
                is ApiResult.Failure -> {
                    if (result.isUnauthorized) {
                        UiUtils.signOutToLogin(this@ReservationDetailActivity, getString(R.string.session_expired))
                    } else {
                        UiUtils.showToast(this@ReservationDetailActivity, result.message)
                        loadReservation()
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_RESERVATION_ID = "reservation_id"

        /**
         * Opens the reservation with this database id.
         */
        fun createIntent(context: Context, reservationId: String): Intent {
            // Only the id travels; the screen loads the rest from the API
            return Intent(context, ReservationDetailActivity::class.java).putExtra(EXTRA_RESERVATION_ID, reservationId)
        }
    }
}
