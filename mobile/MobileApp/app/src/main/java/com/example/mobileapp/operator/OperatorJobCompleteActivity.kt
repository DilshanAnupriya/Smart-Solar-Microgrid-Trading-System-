/*
 * File:        OperatorJobCompleteActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Receipt and summary screen presented upon successful finalization
 *              of an energy transfer business transaction.
 */

package com.example.mobileapp.operator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.mobileapp.R
import java.util.Locale

class OperatorJobCompleteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_job_complete)

        val resNumber = intent.getStringExtra(EXTRA_RESERVATION_NUMBER) ?: "RES-UNKNOWN"
        val prosumer = intent.getStringExtra(EXTRA_PROSUMER) ?: "Prosumer"
        val energy = intent.getDoubleExtra(EXTRA_ENERGY, 0.0)
        val node = intent.getStringExtra(EXTRA_NODE) ?: "Microgrid Hub"
        val timestamp = intent.getStringExtra(EXTRA_TIMESTAMP) ?: "Completed"

        findViewById<TextView>(R.id.tvReceiptReservationNumber).text = resNumber
        findViewById<TextView>(R.id.tvReceiptProsumer).text = prosumer
        findViewById<TextView>(R.id.tvReceiptEnergy).text = String.format(Locale.US, "%.2f kWh", energy)
        findViewById<TextView>(R.id.tvReceiptNode).text = node
        findViewById<TextView>(R.id.tvReceiptTimestamp).text = timestamp

        findViewById<Button>(R.id.btnScanNext).setOnClickListener {
            startActivity(Intent(this, OperatorScanActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnReturnDashboard).setOnClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_RESERVATION_NUMBER = "extra_res_number"
        const val EXTRA_PROSUMER = "extra_prosumer"
        const val EXTRA_ENERGY = "extra_energy"
        const val EXTRA_NODE = "extra_node"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
}
