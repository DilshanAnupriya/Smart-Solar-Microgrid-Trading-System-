/*
 * File:        OperatorDashboardActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: The primary mobile console for Grid Site Operators. Displays live
 *              operational KPIs, quick access to the prosumer QR scanner,
 *              microgrid stations map, and active power trading booking dispatch.
 */

package com.example.mobileapp.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.auth.LoginActivity
import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.models.LocalUser
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch
import java.util.Locale

class OperatorDashboardActivity : BaseActivity() {

    private lateinit var tvOperatorAvatar: TextView
    private lateinit var tvOperatorName: TextView
    private lateinit var tvOperatorSubtitle: TextView
    private lateinit var tvOfflineBanner: TextView
    private lateinit var btnSignOut: Button
    private lateinit var btnRefresh: TextView

    private lateinit var cardScanQr: View
    private lateinit var cardStationsMap: View
    private lateinit var cardBookingsList: View

    private lateinit var tvCountPending: TextView
    private lateinit var tvCountApproved: TextView
    private lateinit var tvCountCompleted: TextView
    private lateinit var tvTotalEnergy: TextView

    private lateinit var progressLoading: ProgressBar
    private lateinit var containerRecentReservations: LinearLayout
    private lateinit var tvEmptyRecent: TextView

    private lateinit var sessionManager: SessionManager
    private lateinit var reservationApi: ReservationApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        val user = sessionManager.getCurrentUser()
        if (user == null || !user.isOperator) {
            signOut()
            return
        }

        setContentView(R.layout.activity_operator_dashboard)
        reservationApi = ReservationApi(this)

        initViews(user)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun initViews(user: LocalUser) {
        tvOperatorAvatar = findViewById(R.id.tvOperatorAvatar)
        tvOperatorName = findViewById(R.id.tvOperatorName)
        tvOperatorSubtitle = findViewById(R.id.tvOperatorSubtitle)
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnRefresh = findViewById(R.id.btnRefresh)

        cardScanQr = findViewById(R.id.cardScanQr)
        cardStationsMap = findViewById(R.id.cardStationsMap)
        cardBookingsList = findViewById(R.id.cardBookingsList)

        tvCountPending = findViewById(R.id.tvCountPending)
        tvCountApproved = findViewById(R.id.tvCountApproved)
        tvCountCompleted = findViewById(R.id.tvCountCompleted)
        tvTotalEnergy = findViewById(R.id.tvTotalEnergy)

        progressLoading = findViewById(R.id.progressLoading)
        containerRecentReservations = findViewById(R.id.containerRecentReservations)
        tvEmptyRecent = findViewById(R.id.tvEmptyRecent)

        tvOperatorName.text = user.fullName.ifBlank { "Grid Operator" }
        tvOperatorAvatar.text = UiUtils.initials(user.fullName.ifBlank { "OP" })
        tvOperatorSubtitle.text = if (user.email.isNotBlank()) user.email else "Operator Dispatch Terminal"
    }

    private fun setupListeners() {
        btnSignOut.setOnClickListener { signOut() }
        btnRefresh.setOnClickListener { loadDashboardData() }
        tvOfflineBanner.setOnClickListener { loadDashboardData() }

        cardScanQr.setOnClickListener {
            startActivity(Intent(this, OperatorScanActivity::class.java))
        }

        cardStationsMap.setOnClickListener {
            startActivity(Intent(this, OperatorStationsMapActivity::class.java))
        }

        cardBookingsList.setOnClickListener {
            startActivity(Intent(this, OperatorReservationsListActivity::class.java))
        }
    }

    /**
     * Loads live stats and active bookings from the Web API with offline fallback.
     */
    private fun loadDashboardData() {
        progressLoading.visibility = View.VISIBLE
        UiUtils.showBanner(tvOfflineBanner, null)

        uiScope.launch {
            // 1. Fetch live metrics
            when (val statsResult = reservationApi.getStats()) {
                is ApiResult.Success -> {
                    val s = statsResult.data
                    tvCountPending.text = s.pendingReservations.toString()
                    tvCountApproved.text = s.approvedReservations.toString()
                    tvCountCompleted.text = s.completedReservations.toString()
                    tvTotalEnergy.text = String.format(Locale.US, "%.1f", s.totalEnergyKWh)
                }
                is ApiResult.Failure -> {
                    UiUtils.showBanner(tvOfflineBanner, statsResult.message)
                }
            }

            // 2. Fetch active reservations (pending + approved) for operator review
            when (val resResult = reservationApi.getAll()) {
                is ApiResult.Success -> {
                    progressLoading.visibility = View.GONE
                    val activeList = resResult.data.filter { it.isApproved || it.isPending }
                    populateRecentBookings(activeList)
                }
                is ApiResult.Failure -> {
                    progressLoading.visibility = View.GONE
                    UiUtils.showBanner(tvOfflineBanner, resResult.message)
                }
            }
        }
    }

    private fun populateRecentBookings(reservations: List<ApiReservation>) {
        containerRecentReservations.removeAllViews()

        if (reservations.isEmpty()) {
            tvEmptyRecent.visibility = View.VISIBLE
            return
        }
        tvEmptyRecent.visibility = View.GONE

        // Show up to 5 most urgent bookings
        for (res in reservations.take(5)) {
            val item = layoutInflater.inflate(R.layout.item_reservation, containerRecentReservations, false)

            val tvNodeName = item.findViewById<TextView>(R.id.tvNodeName)
            val tvStatus = item.findViewById<TextView>(R.id.tvStatus)
            val tvTypeAndEnergy = item.findViewById<TextView>(R.id.tvTypeAndEnergy)
            val tvWhen = item.findViewById<TextView>(R.id.tvWhen)
            val tvReservationId = item.findViewById<TextView>(R.id.tvReservationId)

            tvNodeName.text = res.nodeName.ifBlank { res.nodeId }
            tvReservationId.text = res.reservationNumber.ifBlank { res.id }
            tvTypeAndEnergy.text = "${res.reservationType} · ${String.format(Locale.US, "%.1f kWh", res.energyAmountKWh)}"
            tvWhen.text = res.prosumerName.ifBlank { res.prosumerNic }

            if (res.isApproved) {
                tvStatus.text = "Approved"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
                tvStatus.setTextColor(getColor(R.color.success_text))
            } else if (res.isPending) {
                tvStatus.text = "Pending"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                tvStatus.setTextColor(getColor(R.color.warning_text))
            } else {
                tvStatus.text = res.status
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed)
                tvStatus.setTextColor(getColor(R.color.secondary_text))
            }

            // Clicking opens verification screen with this reservation
            item.setOnClickListener {
                val intent = Intent(this, OperatorVerifyActivity::class.java)
                intent.putExtra(OperatorVerifyActivity.EXTRA_QR_CODE, res.reservationNumber.ifBlank { res.id })
                startActivity(intent)
            }

            containerRecentReservations.addView(item)
        }
    }

    private fun signOut() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
