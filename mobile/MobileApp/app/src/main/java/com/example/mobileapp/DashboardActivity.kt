/*
 * File:        DashboardActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      Vidura Hewaduwa
 * Created:     2026-10-05
 * Description: Prosumer home screen. Uses the signed-in SQLite session and live
 *              reservation data to show pending bookings, approved future
 *              bookings and the next scheduled energy transfer.
 */

package com.example.mobileapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.profile.ProfileActivity
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Landing screen for a signed-in prosumer. SessionManager is the single source
 * for the displayed account and for the NIC used to filter reservation calls.
 */
class DashboardActivity : BaseActivity() {

    private lateinit var currentUser: LocalUser
    private lateinit var reservationApi: ReservationApi

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvProsumerNic: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView
    private lateinit var tvDashboardError: TextView
    private lateinit var btnRefresh: TextView
    private lateinit var progressDashboard: ProgressBar

    private lateinit var cardNextBooking: View
    private lateinit var tvNextEmpty: TextView
    private lateinit var tvNextNode: TextView
    private lateinit var tvNextStatus: TextView
    private lateinit var tvNextTypeEnergy: TextView
    private lateinit var tvNextWhen: TextView

    private lateinit var cardMyBookings: LinearLayout
    private lateinit var cardNearbyNodes: LinearLayout
    private lateinit var cardProfile: LinearLayout

    private var nextBooking: Reservation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val savedUser = sessionManager.getCurrentUser()
        if (!sessionManager.hasValidSession() || savedUser == null || !savedUser.isProsumer) {
            UiUtils.signOutToLogin(this, null)
            return
        }
        currentUser = savedUser

        setContentView(R.layout.activity_dashboard)
        reservationApi = ReservationApi(this)

        initViews()
        bindSessionUser()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Profile edits update the SQLite session, so read it again when returning.
        val sessionManager = SessionManager(this)
        val refreshedUser = sessionManager.getCurrentUser()
        if (!sessionManager.hasValidSession() || refreshedUser == null || !refreshedUser.isProsumer) {
            UiUtils.signOutToLogin(this, null)
            return
        }
        currentUser = refreshedUser
        bindSessionUser()
        loadDashboard()
    }

    /** Connects the XML views once so refreshes only update their values. */
    private fun initViews() {
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvProsumerNic = findViewById(R.id.tvProsumerNic)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
        tvDashboardError = findViewById(R.id.tvDashboardError)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressDashboard = findViewById(R.id.progressDashboard)

        cardNextBooking = findViewById(R.id.cardNextBooking)
        tvNextEmpty = findViewById(R.id.tvNextEmpty)
        tvNextNode = findViewById(R.id.tvNextNode)
        tvNextStatus = findViewById(R.id.tvNextStatus)
        tvNextTypeEnergy = findViewById(R.id.tvNextTypeEnergy)
        tvNextWhen = findViewById(R.id.tvNextWhen)

        cardMyBookings = findViewById(R.id.cardMyBookings)
        cardNearbyNodes = findViewById(R.id.cardNearbyNodes)
        cardProfile = findViewById(R.id.cardProfile)
    }

    /** Shows only data belonging to the saved signed-in prosumer. */
    private fun bindSessionUser() {
        val firstName = currentUser.fullName.trim().substringBefore(" ").ifBlank {
            getString(R.string.dashboard_default_name)
        }
        tvWelcomeName.text = getString(R.string.dashboard_welcome, firstName)
        tvProsumerNic.text = getString(R.string.dashboard_nic, currentUser.nic)
    }

    /** Wires the three required dashboard destinations and the refresh action. */
    private fun setupNavigation() {
        btnRefresh.setOnClickListener { loadDashboard() }

        cardMyBookings.setOnClickListener {
            startActivity(Intent(this, ReservationsActivity::class.java))
        }
        cardNearbyNodes.setOnClickListener {
            startActivity(Intent(this, ProsumerNodesMapActivity::class.java))
        }
        cardProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        cardNextBooking.setOnClickListener {
            nextBooking?.let {
                startActivity(ReservationDetailActivity.createIntent(this, it.id))
            }
        }
        tvDashboardError.setOnClickListener { loadDashboard() }
    }

    /**
     * Gets this prosumer's bookings from the API (or SQLite fallback), refreshes
     * ReservationStore for the existing booking screens, then calculates KPIs.
     */
    private fun loadDashboard() {
        progressDashboard.visibility = View.VISIBLE
        btnRefresh.isEnabled = false
        UiUtils.showBanner(tvDashboardError, null)

        uiScope.launch {
            when (val result = reservationApi.getAll(nic = currentUser.nic)) {
                is ApiResult.Success -> {
                    progressDashboard.visibility = View.GONE
                    btnRefresh.isEnabled = true

                    // Cache fallback may contain other accounts, so filter again locally.
                    val ownedReservations = result.data.filter {
                        it.prosumerNic.equals(currentUser.nic, ignoreCase = true)
                    }
                    val reservations = ownedReservations.map(ReservationStore::fromApiReservation)
                    ReservationStore.setReservations(reservations)
                    showDashboard(reservations)
                }
                is ApiResult.Failure -> {
                    progressDashboard.visibility = View.GONE
                    btnRefresh.isEnabled = true
                    if (result.isUnauthorized) {
                        UiUtils.signOutToLogin(this@DashboardActivity, getString(R.string.session_expired))
                    } else {
                        UiUtils.showBanner(
                            tvDashboardError,
                            getString(R.string.dashboard_load_error, result.message)
                        )
                    }
                }
            }
        }
    }

    /** Calculates counts and selects the earliest active future booking. */
    private fun showDashboard(reservations: List<Reservation>) {
        val now = System.currentTimeMillis()
        val pendingCount = reservations.count { it.status == ReservationStatus.PENDING }
        val approvedFutureCount = reservations.count {
            it.status == ReservationStatus.APPROVED && it.startMillis > now
        }

        tvPendingCount.text = String.format(Locale.getDefault(), "%d", pendingCount)
        tvApprovedCount.text = String.format(Locale.getDefault(), "%d", approvedFutureCount)

        nextBooking = reservations
            .asSequence()
            .filter { it.isActive && it.startMillis > now }
            .minByOrNull { it.startMillis }

        bindNextBooking(nextBooking)
    }

    /** Fills the next-booking card, or displays a clear empty state. */
    private fun bindNextBooking(reservation: Reservation?) {
        val hasNext = reservation != null
        tvNextEmpty.visibility = if (hasNext) View.GONE else View.VISIBLE
        tvNextNode.visibility = if (hasNext) View.VISIBLE else View.GONE
        tvNextStatus.visibility = if (hasNext) View.VISIBLE else View.GONE
        tvNextTypeEnergy.visibility = if (hasNext) View.VISIBLE else View.GONE
        tvNextWhen.visibility = if (hasNext) View.VISIBLE else View.GONE
        cardNextBooking.isClickable = hasNext

        reservation ?: return
        tvNextNode.text = reservation.node.name
        bindStatusBadge(tvNextStatus, reservation.status)
        tvNextTypeEnergy.text = getString(
            R.string.reservation_type_and_energy,
            getString(reservation.type.labelRes),
            formatEnergy(this, reservation.energyKwh)
        )
        tvNextWhen.text = getString(
            R.string.reservation_date_and_time,
            formatReservationDate(reservation.startMillis),
            formatReservationTime(this, reservation)
        )
    }
}
