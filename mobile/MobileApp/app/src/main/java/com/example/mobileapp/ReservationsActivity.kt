package com.example.mobileapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.sessions.SessionManager
import com.example.mobileapp.utils.BaseActivity
import kotlinx.coroutines.launch

/**
 * "My reservations": the prosumer's reservations, upcoming first, with a New button.
 */
class ReservationsActivity : BaseActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var filterAll: Button
    private lateinit var filterPending: Button
    private lateinit var filterApproved: Button
    private lateinit var filterCompleted: Button
    private lateinit var filterCancelled: Button

    // null represents the "All" filter.
    private var selectedStatus: ReservationStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        listContainer = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearchReservations)
        filterAll = findViewById(R.id.filterAll)
        filterPending = findViewById(R.id.filterPending)
        filterApproved = findViewById(R.id.filterApproved)
        filterCompleted = findViewById(R.id.filterCompleted)
        filterCancelled = findViewById(R.id.filterCancelled)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnNew).setOnClickListener {
            startActivity(ReservationFormActivity.newReservationIntent(this))
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                showReservations()
            }

            override fun afterTextChanged(text: Editable?) = Unit
        })

        filterAll.setOnClickListener { selectStatus(null) }
        filterPending.setOnClickListener { selectStatus(ReservationStatus.PENDING) }
        filterApproved.setOnClickListener { selectStatus(ReservationStatus.APPROVED) }
        filterCompleted.setOnClickListener { selectStatus(ReservationStatus.COMPLETED) }
        filterCancelled.setOnClickListener { selectStatus(ReservationStatus.CANCELLED) }
    }

    override fun onResume() {
        super.onResume()
        // Rebuild immediately from cache, then fetch live data from server
        showReservations()
        fetchLiveReservations()
    }

    private fun fetchLiveReservations() {
        val currentUser = SessionManager(this).getCurrentUser()
        val nic = currentUser?.nic
        uiScope.launch {
            val api = ReservationApi(this@ReservationsActivity)
            val result = api.getAll(nic = nic)
            if (result is ApiResult.Success) {
                val mapped = result.data.map { ReservationStore.fromApiReservation(it) }
                ReservationStore.setReservations(mapped)
                showReservations()
            }
        }
    }

    private fun showReservations() {
        listContainer.removeAllViews()

        val allReservations = ReservationStore.all()
        val visibleReservations = allReservations.filter(::matchesCurrentFilters)
        val (upcoming, past) = visibleReservations.partition { it.isActive }
        val hasResults = visibleReservations.isNotEmpty()

        tvEmpty.setText(
            if (allReservations.isEmpty()) R.string.reservations_empty
            else R.string.reservations_no_matches
        )
        tvEmpty.visibility = if (hasResults) View.GONE else View.VISIBLE

        // Soonest upcoming first; most recent past first
        addSection(R.string.reservations_upcoming, upcoming.sortedBy { it.startMillis })
        addSection(R.string.reservations_past, past.sortedByDescending { it.startMillis })
    }

    /** Applies the selected status and searches the fields visible to a prosumer. */
    private fun matchesCurrentFilters(reservation: Reservation): Boolean {
        return ReservationListFilter.matches(
            reservation = reservation,
            selectedStatus = selectedStatus,
            query = etSearch.text.toString(),
            typeLabel = getString(reservation.type.labelRes),
            statusLabel = getString(reservation.status.labelRes)
        )
    }

    private fun selectStatus(status: ReservationStatus?) {
        selectedStatus = status

        val selectedButton = when (status) {
            null -> filterAll
            ReservationStatus.PENDING -> filterPending
            ReservationStatus.APPROVED -> filterApproved
            ReservationStatus.COMPLETED -> filterCompleted
            ReservationStatus.CANCELLED -> filterCancelled
        }

        listOf(filterAll, filterPending, filterApproved, filterCompleted, filterCancelled).forEach { button ->
            val selected = button == selectedButton
            button.setBackgroundResource(
                if (selected) R.drawable.bg_role_tab_active else R.drawable.bg_btn_outline
            )
            button.setTextColor(getColor(if (selected) R.color.white else R.color.text_dark))
        }

        showReservations()
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
