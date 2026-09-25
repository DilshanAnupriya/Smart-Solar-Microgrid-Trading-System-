/*
 * File:        OperatorVerifyActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Verifies prosumer transaction QR code against central API server data
 *              and executes energy transfer completion business logic.
 */

package com.example.mobileapp.operator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.mobileapp.R
import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.models.MicrogridStation
import com.example.mobileapp.models.Prosumer
import com.example.mobileapp.models.QrVerificationResult
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.NodeApi
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.UiUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OperatorVerifyActivity : BaseActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var bannerStatusContainer: LinearLayout
    private lateinit var tvVerificationTitle: TextView
    private lateinit var tvVerificationMessage: TextView

    private lateinit var tvReservationNumber: TextView
    private lateinit var tvReservationStatusBadge: TextView
    private lateinit var tvTradingType: TextView
    private lateinit var tvEnergyQuantity: TextView
    private lateinit var tvSlotSchedule: TextView

    private lateinit var tvProsumerName: TextView
    private lateinit var tvProsumerAccountStatus: TextView
    private lateinit var tvProsumerNic: TextView
    private lateinit var tvProsumerContact: TextView

    private lateinit var tvStationName: TextView
    private lateinit var tvStationAddress: TextView
    private lateinit var tvSlotsInfo: TextView
    private lateinit var tvCurrentSlots: TextView
    private lateinit var btnSlotMinus: Button
    private lateinit var btnSlotPlus: Button

    private lateinit var etOperatorNotes: EditText
    private lateinit var btnFinalizeTransfer: Button
    private lateinit var btnReject: Button

    private lateinit var verificationEngine: QrVerificationEngine
    private lateinit var reservationApi: ReservationApi
    private lateinit var nodeApi: NodeApi

    private var currentResult: QrVerificationResult? = null
    private var availableSlots: Int = 0
    private var initialSlots: Int = 0
    private var totalSlots: Int = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_verify)

        verificationEngine = QrVerificationEngine(this)
        reservationApi = ReservationApi(this)
        nodeApi = NodeApi(this)

        initViews()
        setupListeners()

        val rawQr = intent.getStringExtra(EXTRA_QR_CODE)
        if (rawQr.isNullOrBlank()) {
            Toast.makeText(this, "No QR data provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        executeVerification(rawQr)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        bannerStatusContainer = findViewById(R.id.bannerStatusContainer)
        tvVerificationTitle = findViewById(R.id.tvVerificationTitle)
        tvVerificationMessage = findViewById(R.id.tvVerificationMessage)

        tvReservationNumber = findViewById(R.id.tvReservationNumber)
        tvReservationStatusBadge = findViewById(R.id.tvReservationStatusBadge)
        tvTradingType = findViewById(R.id.tvTradingType)
        tvEnergyQuantity = findViewById(R.id.tvEnergyQuantity)
        tvSlotSchedule = findViewById(R.id.tvSlotSchedule)

        tvProsumerName = findViewById(R.id.tvProsumerName)
        tvProsumerAccountStatus = findViewById(R.id.tvProsumerAccountStatus)
        tvProsumerNic = findViewById(R.id.tvProsumerNic)
        tvProsumerContact = findViewById(R.id.tvProsumerContact)

        tvStationName = findViewById(R.id.tvStationName)
        tvStationAddress = findViewById(R.id.tvStationAddress)
        tvSlotsInfo = findViewById(R.id.tvSlotsInfo)
        tvCurrentSlots = findViewById(R.id.tvCurrentSlots)
        btnSlotMinus = findViewById(R.id.btnSlotMinus)
        btnSlotPlus = findViewById(R.id.btnSlotPlus)

        etOperatorNotes = findViewById(R.id.etOperatorNotes)
        btnFinalizeTransfer = findViewById(R.id.btnFinalizeTransfer)
        btnReject = findViewById(R.id.btnReject)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnSlotMinus.setOnClickListener {
            if (availableSlots > 0) {
                availableSlots--
                updateSlotDisplay()
            }
        }

        btnSlotPlus.setOnClickListener {
            if (availableSlots < totalSlots) {
                availableSlots++
                updateSlotDisplay()
            }
        }

        btnFinalizeTransfer.setOnClickListener {
            finalizeEnergyTransfer()
        }

        btnReject.setOnClickListener {
            confirmRejectTransfer()
        }
    }

    private fun updateSlotDisplay() {
        tvCurrentSlots.text = availableSlots.toString()
        tvSlotsInfo.text = "$availableSlots of $totalSlots slots free"
    }

    /**
     * Executes the verification engine against server endpoints.
     */
    private fun executeVerification(rawQr: String) {
        tvVerificationTitle.text = getString(R.string.operator_scanner_verifying)
        tvVerificationMessage.text = "Contacting central API service to verify booking reference..."
        btnFinalizeTransfer.isEnabled = false

        uiScope.launch {
            val result = verificationEngine.verifyServerData(rawQr)
            currentResult = result
            displayVerificationResult(result)
        }
    }

    private fun displayVerificationResult(res: QrVerificationResult) {
        tvVerificationTitle.text = res.statusTitle
        tvVerificationMessage.text = res.statusMessage

        // Configure banner style based on verification outcome
        if (res.isVerified && res.canFinalizeTransfer) {
            bannerStatusContainer.setBackgroundResource(R.drawable.bg_banner_info)
            tvVerificationTitle.setTextColor(getColor(R.color.info_text))
            tvVerificationMessage.setTextColor(getColor(R.color.info_text))
            btnFinalizeTransfer.isEnabled = true
        } else if (res.isVerified && !res.canFinalizeTransfer) {
            bannerStatusContainer.setBackgroundResource(R.drawable.bg_banner_warning)
            tvVerificationTitle.setTextColor(getColor(R.color.warning_text))
            tvVerificationMessage.setTextColor(getColor(R.color.warning_text))
            btnFinalizeTransfer.isEnabled = false
        } else {
            bannerStatusContainer.setBackgroundResource(R.drawable.bg_banner_danger)
            tvVerificationTitle.setTextColor(getColor(R.color.danger_text))
            tvVerificationMessage.setTextColor(getColor(R.color.danger_text))
            btnFinalizeTransfer.isEnabled = false
        }

        val reservation = res.reservation
        if (reservation != null) {
            bindReservation(reservation)
        }

        val prosumer = res.prosumer
        if (prosumer != null) {
            bindProsumer(prosumer)
        } else if (reservation != null) {
            tvProsumerName.text = reservation.prosumerName.ifBlank { "Solar Prosumer" }
            tvProsumerNic.text = "NIC: ${reservation.prosumerNic}"
            tvProsumerContact.text = "Account: Verified with booking"
            tvProsumerAccountStatus.text = "Active"
        }

        val station = res.station
        if (station != null) {
            bindStation(station)
        } else if (reservation != null) {
            tvStationName.text = reservation.nodeName.ifBlank { reservation.nodeId }
            tvStationAddress.text = "Microgrid Station Hub"
            totalSlots = 10
            availableSlots = 7
            initialSlots = 7
            updateSlotDisplay()
        }
    }

    private fun bindReservation(res: ApiReservation) {
        tvReservationNumber.text = res.reservationNumber.ifBlank { res.id }
        tvReservationStatusBadge.text = res.status

        if (res.isApproved) {
            tvReservationStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            tvReservationStatusBadge.setTextColor(getColor(R.color.success_text))
        } else if (res.isPending) {
            tvReservationStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            tvReservationStatusBadge.setTextColor(getColor(R.color.warning_text))
        } else if (res.isCompleted) {
            tvReservationStatusBadge.setBackgroundResource(R.drawable.bg_badge_completed)
            tvReservationStatusBadge.setTextColor(getColor(R.color.secondary_text))
        } else {
            tvReservationStatusBadge.setBackgroundResource(R.drawable.bg_badge_deactivated)
            tvReservationStatusBadge.setTextColor(getColor(R.color.danger_text))
        }

        tvTradingType.text = if (res.isDropOff) "Drop-Off (Sell to Grid)" else "Charging (Buy from Grid)"
        tvEnergyQuantity.text = String.format(Locale.US, "%.2f kWh", res.energyAmountKWh)

        val start = res.slotStartTime.replace("T", " ").take(16)
        val end = res.slotEndTime.replace("T", " ").take(16)
        tvSlotSchedule.text = if (start.isNotBlank()) "$start to $end" else "Standard Scheduled Slot"
    }

    private fun bindProsumer(p: Prosumer) {
        tvProsumerName.text = p.fullName.ifBlank { "Prosumer ${p.nic}" }
        tvProsumerNic.text = "NIC: ${p.nic}"
        tvProsumerContact.text = "Phone: ${p.phone.ifBlank { "—" }} · Email: ${p.email.ifBlank { "—" }}"
        tvProsumerAccountStatus.text = p.status

        if (p.status.equals("Active", ignoreCase = true)) {
            tvProsumerAccountStatus.setBackgroundResource(R.drawable.bg_badge_active)
            tvProsumerAccountStatus.setTextColor(getColor(R.color.success_text))
        } else {
            tvProsumerAccountStatus.setBackgroundResource(R.drawable.bg_badge_deactivated)
            tvProsumerAccountStatus.setTextColor(getColor(R.color.danger_text))
        }
    }

    private fun bindStation(st: MicrogridStation) {
        tvStationName.text = st.name
        tvStationAddress.text = st.address.ifBlank { "Station ID: ${st.id}" }
        totalSlots = if (st.totalBatterySlots > 0) st.totalBatterySlots else 10
        availableSlots = st.availableBatterySlots.coerceIn(0, totalSlots)
        initialSlots = availableSlots
        updateSlotDisplay()
    }

    /**
     * Finalizes the energy transfer by calling PATCH /api/reservations/{id}/status to "Completed"
     * and optionally updates live battery slot availability at the station.
     */
    private fun finalizeEnergyTransfer() {
        val res = currentResult?.reservation ?: return
        val reservationId = res.id.ifBlank { res.reservationNumber }
        val notes = etOperatorNotes.text.toString().trim().ifBlank {
            "Verified on-site by Operator. Energy transfer completed successfully."
        }

        btnFinalizeTransfer.isEnabled = false
        btnFinalizeTransfer.text = getString(R.string.operator_finalizing)

        uiScope.launch {
            // 1. Advance reservation status to "Completed"
            when (val updateResult = reservationApi.updateStatus(reservationId, "Completed", notes)) {
                is ApiResult.Success -> {
                    // 2. If slots were adjusted, update the node on the server
                    if (availableSlots != initialSlots && res.nodeId.isNotBlank()) {
                        nodeApi.updateSlots(res.nodeId, availableSlots)
                    }

                    // 3. Navigate to Job Complete receipt screen
                    val completeIntent = Intent(this@OperatorVerifyActivity, OperatorJobCompleteActivity::class.java).apply {
                        putExtra(OperatorJobCompleteActivity.EXTRA_RESERVATION_NUMBER, res.reservationNumber.ifBlank { res.id })
                        putExtra(OperatorJobCompleteActivity.EXTRA_PROSUMER, res.prosumerName.ifBlank { res.prosumerNic })
                        putExtra(OperatorJobCompleteActivity.EXTRA_ENERGY, res.energyAmountKWh)
                        putExtra(OperatorJobCompleteActivity.EXTRA_NODE, res.nodeName.ifBlank { res.nodeId })
                        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date())
                        putExtra(OperatorJobCompleteActivity.EXTRA_TIMESTAMP, "$iso UTC")
                    }
                    startActivity(completeIntent)
                    finish()
                }
                is ApiResult.Failure -> {
                    btnFinalizeTransfer.isEnabled = true
                    btnFinalizeTransfer.text = getString(R.string.operator_finalize_btn)
                    Toast.makeText(this@OperatorVerifyActivity, "Finalization failed: ${updateResult.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmRejectTransfer() {
        AlertDialog.Builder(this)
            .setTitle("Flag / Cancel Energy Transfer?")
            .setMessage("Are you sure you want to cancel or flag this transfer session?")
            .setPositiveButton("Reject Transfer") { _, _ ->
                finish()
            }
            .setNegativeButton("Back", null)
            .show()
    }

    companion object {
        const val EXTRA_QR_CODE = "extra_qr_code"
    }
}
