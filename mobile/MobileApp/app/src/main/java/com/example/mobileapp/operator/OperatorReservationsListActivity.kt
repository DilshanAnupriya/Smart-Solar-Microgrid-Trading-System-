/*
 * File:        OperatorReservationsListActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Operational screen for monitoring and filtering all power trading
 *              reservations across microgrid hubs with search and status filters.
 */

package com.example.mobileapp.operator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.utils.BaseActivity
import kotlinx.coroutines.launch
import java.util.Locale

class OperatorReservationsListActivity : BaseActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnScanFromList: Button
    private lateinit var etSearchReservations: EditText

    private lateinit var filterAll: Button
    private lateinit var filterPending: Button
    private lateinit var filterApproved: Button
    private lateinit var filterCompleted: Button
    private lateinit var filterCancelled: Button

    private lateinit var progressLoading: ProgressBar
    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmptyList: TextView

    private lateinit var reservationApi: ReservationApi
    private val allReservations = mutableListOf<ApiReservation>()
    private var activeFilterStatus: String? = null // null means All

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_reservations_list)

        reservationApi = ReservationApi(this)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadReservations()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnScanFromList = findViewById(R.id.btnScanFromList)
        etSearchReservations = findViewById(R.id.etSearchReservations)

        filterAll = findViewById(R.id.filterAll)
        filterPending = findViewById(R.id.filterPending)
        filterApproved = findViewById(R.id.filterApproved)
        filterCompleted = findViewById(R.id.filterCompleted)
        filterCancelled = findViewById(R.id.filterCancelled)

        progressLoading = findViewById(R.id.progressLoading)
        listContainer = findViewById(R.id.listContainer)
        tvEmptyList = findViewById(R.id.tvEmptyList)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnScanFromList.setOnClickListener {
            startActivity(Intent(this, OperatorScanActivity::class.java))
        }

        filterAll.setOnClickListener { setFilter(null, filterAll) }
        filterPending.setOnClickListener { setFilter("Pending", filterPending) }
        filterApproved.setOnClickListener { setFilter("Approved", filterApproved) }
        filterCompleted.setOnClickListener { setFilter("Completed", filterCompleted) }
        filterCancelled.setOnClickListener { setFilter("Cancelled", filterCancelled) }

        etSearchReservations.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyLocalFilterAndRender()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setFilter(status: String?, selectedButton: Button) {
        activeFilterStatus = status

        val buttons = listOf(filterAll, filterPending, filterApproved, filterCompleted, filterCancelled)
        for (btn in buttons) {
            if (btn == selectedButton) {
                btn.setBackgroundResource(R.drawable.bg_role_tab_active)
                btn.setTextColor(getColor(R.color.white))
            } else {
                btn.setBackgroundResource(R.drawable.bg_btn_outline)
                btn.setTextColor(getColor(R.color.text_dark))
            }
        }

        applyLocalFilterAndRender()
    }

    /**
     * Loads live reservations from Web API.
     */
    private fun loadReservations() {
        progressLoading.visibility = View.VISIBLE

        uiScope.launch {
            when (val result = reservationApi.getAll()) {
                is ApiResult.Success -> {
                    progressLoading.visibility = View.GONE
                    allReservations.clear()
                    allReservations.addAll(result.data)
                    applyLocalFilterAndRender()
                }
                is ApiResult.Failure -> {
                    progressLoading.visibility = View.GONE
                    applyLocalFilterAndRender()
                }
            }
        }
    }

    private fun applyLocalFilterAndRender() {
        val searchQuery = etSearchReservations.text.toString().trim().lowercase(Locale.US)

        val filtered = allReservations.filter { res ->
            val matchesFilter = activeFilterStatus == null || res.status.equals(activeFilterStatus, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() ||
                res.reservationNumber.lowercase(Locale.US).contains(searchQuery) ||
                res.prosumerNic.lowercase(Locale.US).contains(searchQuery) ||
                res.prosumerName.lowercase(Locale.US).contains(searchQuery) ||
                res.nodeName.lowercase(Locale.US).contains(searchQuery)
            matchesFilter && matchesSearch
        }

        listContainer.removeAllViews()

        if (filtered.isEmpty()) {
            tvEmptyList.visibility = View.VISIBLE
            return
        }
        tvEmptyList.visibility = View.GONE

        for (res in filtered) {
            val item = layoutInflater.inflate(R.layout.item_reservation, listContainer, false)

            val tvNodeName = item.findViewById<TextView>(R.id.tvNodeName)
            val tvStatus = item.findViewById<TextView>(R.id.tvStatus)
            val tvTypeAndEnergy = item.findViewById<TextView>(R.id.tvTypeAndEnergy)
            val tvWhen = item.findViewById<TextView>(R.id.tvWhen)
            val tvReservationId = item.findViewById<TextView>(R.id.tvReservationId)

            tvNodeName.text = res.nodeName.ifBlank { res.nodeId }
            tvReservationId.text = res.reservationNumber.ifBlank { res.id }
            tvTypeAndEnergy.text = "${res.reservationType} · ${String.format(Locale.US, "%.1f kWh", res.energyAmountKWh)}"
            tvWhen.text = "${res.prosumerName} (${res.prosumerNic})"

            if (res.isApproved) {
                tvStatus.text = "Approved"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_active)
                tvStatus.setTextColor(getColor(R.color.success_text))
            } else if (res.isPending) {
                tvStatus.text = "Pending"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                tvStatus.setTextColor(getColor(R.color.warning_text))
            } else if (res.isCompleted) {
                tvStatus.text = "Completed"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_completed)
                tvStatus.setTextColor(getColor(R.color.secondary_text))
            } else {
                tvStatus.text = res.status
                tvStatus.setBackgroundResource(R.drawable.bg_badge_deactivated)
                tvStatus.setTextColor(getColor(R.color.danger_text))
            }

            item.setOnClickListener {
                val intent = Intent(this, OperatorVerifyActivity::class.java).apply {
                    putExtra(OperatorVerifyActivity.EXTRA_QR_CODE, res.reservationNumber.ifBlank { res.id })
                }
                startActivity(intent)
            }

            listContainer.addView(item)
        }
    }
}
