package com.example.mobileapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView

/**
 * "My reservations": the prosumer's reservations, upcoming first, with a New button.
 */
class ReservationsActivity : Activity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        listContainer = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNew).setOnClickListener {
            startActivity(ReservationFormActivity.newReservationIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        // Rebuild every time, so changes made on other screens show up on return
        showReservations()
    }

    private fun showReservations() {
        listContainer.removeAllViews()

        val (upcoming, past) = ReservationStore.all().partition { it.isActive }
        tvEmpty.visibility = if (upcoming.isEmpty() && past.isEmpty()) View.VISIBLE else View.GONE

        // Soonest upcoming first; most recent past first
        addSection(R.string.reservations_upcoming, upcoming.sortedBy { it.startMillis })
        addSection(R.string.reservations_past, past.sortedByDescending { it.startMillis })
    }

    private fun addSection(titleRes: Int, reservations: List<Reservation>) {
        if (reservations.isEmpty()) return

        val header = layoutInflater.inflate(R.layout.item_reservation_section, listContainer, false) as TextView
        header.setText(titleRes)
        listContainer.addView(header)

        for (reservation in reservations) {
            listContainer.addView(createRow(reservation))
        }
    }

    private fun createRow(reservation: Reservation): View {
        val row = layoutInflater.inflate(R.layout.item_reservation, listContainer, false)

        row.findViewById<TextView>(R.id.tvNodeName).text = reservation.node.name
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
        row.findViewById<TextView>(R.id.tvReservationId).text = reservation.id

        row.setOnClickListener {
            startActivity(ReservationDetailActivity.createIntent(this, reservation.id))
        }
        return row
    }
}
