/*
 * File:        ReservationsActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — loads the list from the
 *              API, caches it in SQLite and shows the saved copy when offline.
 * Description: "My reservations": the prosumer's reservations, upcoming first,
 *              with a New button.
 */

package com.example.mobileapp.booking

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.ReservationCacheDao
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch

/**
 * Shows the copy saved in SQLite straight away, then replaces it with the API's list.
 */
class ReservationsActivity : BaseActivity() {

    private lateinit var reservationCache: ReservationCacheDao

    private lateinit var progressLoading: ProgressBar
    private lateinit var tvOfflineBanner: TextView
    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Find the views and connect the buttons
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        reservationCache = ReservationCacheDao(DbHelper.getInstance(this))

        progressLoading = findViewById(R.id.progressLoading)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        listContainer = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNew).setOnClickListener {
            startActivity(ReservationFormActivity.newReservationIntent(this))
        }
        tvOfflineBanner.setOnClickListener { loadReservations() }
    }

    override fun onResume() {
        // Refresh every time, so changes made on other screens show up on return. The saved
        // copy is shown meanwhile, without the "no reservations" text until the API has answered.
        super.onResume()
        showReservations(reservationCache.getAll(), showEmptyText = false)
        loadReservations()
    }

    /**
     * GET /reservations/my; on success the SQLite cache is replaced with the API's list.
     */
    private fun loadReservations() {
        // The saved copy stays on screen while this runs
        progressLoading.visibility = View.VISIBLE
        uiScope.launch {
            val result = ReservationApi(this@ReservationsActivity).getMyReservations()
            progressLoading.visibility = View.GONE
            when (result) {
                is ApiResult.Success -> {
                    reservationCache.replaceAll(result.data)
                    UiUtils.showBanner(tvOfflineBanner, null)
                    showReservations(result.data)
                }
                is ApiResult.Failure -> {
                    if (result.isUnauthorized) {
                        UiUtils.signOutToLogin(this@ReservationsActivity, getString(R.string.session_expired))
                    } else {
                        // Offline path: show the saved copy and say why it wasn't refreshed
                        showReservations(reservationCache.getAll())
                        UiUtils.showBanner(
                            tvOfflineBanner,
                            getString(R.string.reservations_offline_banner, result.message)
                        )
                    }
                }
            }
        }
    }

    /**
     * Rebuilds the list: upcoming reservations first, then past and cancelled ones.
     * [showEmptyText] is false while waiting for the API, so "no reservations" doesn't flash up.
     */
    private fun showReservations(reservations: List<Reservation>, showEmptyText: Boolean = true) {
        // Grouping by status is only presentation; the statuses themselves come from the API
        listContainer.removeAllViews()

        val (upcoming, past) = reservations.partition { it.isActive }
        tvEmpty.visibility = if (reservations.isEmpty() && showEmptyText) View.VISIBLE else View.GONE

        // Soonest upcoming first; most recent past first
        addSection(R.string.reservations_upcoming, upcoming.sortedBy { it.startMillis })
        addSection(R.string.reservations_past, past.sortedByDescending { it.startMillis })
    }

    /**
     * Adds a section heading and one row per reservation; nothing when the section is empty.
     */
    private fun addSection(titleRes: Int, reservations: List<Reservation>) {
        // An empty section would only show a lonely heading
        if (reservations.isEmpty()) return

        val header = layoutInflater.inflate(R.layout.item_reservation_section, listContainer, false) as TextView
        header.setText(titleRes)
        listContainer.addView(header)

        for (reservation in reservations) {
            listContainer.addView(createRow(reservation))
        }
    }

    /**
     * Builds one list row; tapping it opens the reservation.
     */
    private fun createRow(reservation: Reservation): View {
        // Node, status, type and energy, date and time, and the reference number
        val row = layoutInflater.inflate(R.layout.item_reservation, listContainer, false)

        row.findViewById<TextView>(R.id.tvNodeName).text = reservation.nodeName
        bindStatusBadge(row.findViewById(R.id.tvStatus), reservation.status)
        row.findViewById<TextView>(R.id.tvTypeAndEnergy).text = getString(
            R.string.reservation_type_and_energy,
            getString(reservation.type.labelRes),
            formatEnergy(this, reservation.energyKwh)
        )
        row.findViewById<TextView>(R.id.tvWhen).text = getString(
            R.string.reservation_date_and_time,
            formatReservationDate(reservation.startMillis),
            formatReservationTime(this, reservation)
        )
        row.findViewById<TextView>(R.id.tvReservationId).text = reservation.reservationNumber

        row.setOnClickListener {
            startActivity(ReservationDetailActivity.createIntent(this, reservation.id))
        }
        return row
    }
}
