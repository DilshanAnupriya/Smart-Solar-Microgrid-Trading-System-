package com.example.mobileapp

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/**
 * Creates a new reservation, or modifies an existing one when started with a reservation id.
 * Enforces the 7-day window here for quick feedback; the API remains the real check.
 */
class ReservationFormActivity : Activity() {

    // Set when modifying; null when creating a new reservation
    private var editing: Reservation? = null

    // Picked values; null until the user chooses them
    private var selectedNode: GridNode? = null
    private var selectedDate: Calendar? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null
    private var durationHours = DEFAULT_DURATION_HOURS

    private lateinit var scrollView: ScrollView
    private lateinit var tvNode: TextView
    private lateinit var tvNodeError: TextView
    private lateinit var rgType: RadioGroup
    private lateinit var tvTypeHelp: TextView
    private lateinit var rowWhen: View
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvWhenError: TextView
    private lateinit var tvDuration: TextView
    private lateinit var etEnergy: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modify mode: only allowed while the reservation is active and 12+ hours away
        val editId = intent.getStringExtra(EXTRA_RESERVATION_ID)
        if (editId != null) {
            val reservation = ReservationStore.find(editId)
            if (reservation == null || !reservation.isActive ||
                !ReservationRules.canModifyOrCancel(reservation.startMillis, System.currentTimeMillis())
            ) {
                finish()
                return
            }
            editing = reservation
        }

        setContentView(R.layout.activity_reservation_form)

        scrollView = findViewById(R.id.scrollView)
        tvNode = findViewById(R.id.tvNode)
        tvNodeError = findViewById(R.id.tvNodeError)
        rgType = findViewById(R.id.rgType)
        tvTypeHelp = findViewById(R.id.tvTypeHelp)
        rowWhen = findViewById(R.id.rowWhen)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvWhenError = findViewById(R.id.tvWhenError)
        tvDuration = findViewById(R.id.tvDuration)
        etEnergy = findViewById(R.id.etEnergy)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        editing?.let { reservation ->
            findViewById<TextView>(R.id.tvTitle).setText(R.string.reservation_form_modify_title)
            findViewById<TextView>(R.id.tvModifyNote).apply {
                text = getString(R.string.reservation_form_modify_note, reservation.id)
                visibility = View.VISIBLE
            }
            btnSubmit.setText(R.string.reservation_submit_modify)
        }

        // Restore picked values after a rotation; otherwise pre-fill when modifying.
        // The type buttons and the energy field restore themselves.
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            editing?.let { prefill(it) }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        tvNode.setOnClickListener { pickNode() }
        tvDate.setOnClickListener { pickDate() }
        tvTime.setOnClickListener { pickTime() }
        tvDuration.setOnClickListener { pickDuration() }
        rgType.setOnCheckedChangeListener { _, _ -> showTypeHelp() }
        btnSubmit.setOnClickListener { attemptSubmit() }

        // "Done" on the keyboard in the energy field submits the form
        etEnergy.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSubmit()
                true
            } else {
                false
            }
        }

        showPickedValues()
        showTypeHelp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedNode?.let { outState.putString(STATE_NODE_ID, it.id) }
        selectedDate?.let { outState.putLong(STATE_DATE_MILLIS, it.timeInMillis) }
        selectedHour?.let { outState.putInt(STATE_HOUR, it) }
        selectedMinute?.let { outState.putInt(STATE_MINUTE, it) }
        outState.putInt(STATE_DURATION, durationHours)
    }

    private fun restoreState(state: Bundle) {
        selectedNode = state.getString(STATE_NODE_ID)?.let { id -> ReservationStore.nodes.firstOrNull { it.id == id } }
        if (state.containsKey(STATE_DATE_MILLIS)) {
            selectedDate = Calendar.getInstance().apply { timeInMillis = state.getLong(STATE_DATE_MILLIS) }
        }
        if (state.containsKey(STATE_HOUR)) selectedHour = state.getInt(STATE_HOUR)
        if (state.containsKey(STATE_MINUTE)) selectedMinute = state.getInt(STATE_MINUTE)
        durationHours = state.getInt(STATE_DURATION, DEFAULT_DURATION_HOURS)
    }

    private fun prefill(reservation: Reservation) {
        val start = Calendar.getInstance().apply { timeInMillis = reservation.startMillis }
        selectedNode = reservation.node
        selectedDate = start
        selectedHour = start.get(Calendar.HOUR_OF_DAY)
        selectedMinute = start.get(Calendar.MINUTE)
        durationHours = reservation.durationHours
        rgType.check(if (reservation.type == ReservationType.CHARGING) R.id.rbCharging else R.id.rbDropOff)
        etEnergy.setText(DECIMAL_FORMAT.format(reservation.energyKwh))
    }

    /** Writes the picked values into the picker fields (empty ones show their hint). */
    private fun showPickedValues() {
        tvNode.text = selectedNode?.let { "${it.name}, ${it.location}" }
        tvDate.text = selectedDate?.let { formatReservationDate(it.timeInMillis) }
        tvTime.text = formatSelectedTime()
        tvDuration.text = formatDuration(this, durationHours)
    }

    // Formats just the picked hour and minute, so the time shows even before a date is chosen
    private fun formatSelectedTime(): String? {
        val hour = selectedHour ?: return null
        val minute = selectedMinute ?: return null
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(this).format(time.time)
    }

    private fun showTypeHelp() {
        tvTypeHelp.setText(selectedType().helpRes)
    }

    private fun selectedType(): ReservationType =
        if (rgType.checkedRadioButtonId == R.id.rbCharging) ReservationType.CHARGING else ReservationType.DROP_OFF

    private fun pickNode() {
        val nodes = ReservationStore.nodes
        val labels = nodes.map { "${it.name}\n${it.location}" }.toTypedArray()
        val checked = nodes.indexOf(selectedNode)

        AlertDialog.Builder(this)
            .setTitle(R.string.reservation_node_label)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                selectedNode = nodes[which]
                tvNodeError.visibility = View.GONE
                showPickedValues()
                dialog.dismiss()
            }
            .show()
    }

    private fun pickDate() {
        val initial = selectedDate ?: Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                tvWhenError.visibility = View.GONE
                showPickedValues()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        )

        // Only offer today to 7 days from now; the exact time is checked on submit
        val now = System.currentTimeMillis()
        dialog.datePicker.minDate = now
        dialog.datePicker.maxDate = now + ReservationRules.BOOKING_WINDOW_DAYS * 24 * ReservationRules.HOUR_MILLIS
        dialog.show()
    }

    private fun pickTime() {
        // Default to the next full hour
        val nextHour = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                tvWhenError.visibility = View.GONE
                showPickedValues()
            },
            selectedHour ?: nextHour,
            selectedMinute ?: 0,
            DateFormat.is24HourFormat(this)
        ).show()
    }

    private fun pickDuration() {
        val labels = DURATION_CHOICES.map { formatDuration(this, it) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.reservation_duration_label)
            .setSingleChoiceItems(labels, DURATION_CHOICES.indexOf(durationHours)) { dialog, which ->
                durationHours = DURATION_CHOICES[which]
                showPickedValues()
                dialog.dismiss()
            }
            .show()
    }

    /** Combines the picked date and time, or returns null if either is missing. */
    private fun buildStartMillis(): Long? {
        val date = selectedDate ?: return null
        val hour = selectedHour ?: return null
        val minute = selectedMinute ?: return null

        return (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun attemptSubmit() {
        val node = selectedNode
        val startMillis = buildStartMillis()
        val energyText = etEnergy.text.toString()

        val nodeError = if (node == null) R.string.error_node_required else null
        val startError = ReservationRules.validateStart(startMillis, System.currentTimeMillis())
        val energyError = ReservationRules.validateEnergy(energyText)

        showPickerError(tvNodeError, nodeError)
        showPickerError(tvWhenError, startError)
        etEnergy.error = energyError?.let { getString(it) }

        // Bring the first problem into view, top to bottom
        when {
            nodeError != null -> scrollTo(tvNode)
            startError != null -> scrollTo(rowWhen)
            energyError != null -> etEnergy.requestFocus()
        }
        if (node == null || startMillis == null || startError != null || energyError != null) return

        val energyKwh = energyText.trim().toDouble()
        val reservation = editing

        // UI only for now: POST /api/reservations or PUT /api/reservations/{id} goes here
        val saved: Reservation?
        val action: ReservationSummaryActivity.Action
        if (reservation == null) {
            saved = ReservationStore.create(node, selectedType(), startMillis, durationHours, energyKwh)
            action = ReservationSummaryActivity.Action.CREATED
        } else {
            saved = ReservationStore.update(reservation.id, node, selectedType(), startMillis, durationHours, energyKwh)
            action = ReservationSummaryActivity.Action.UPDATED
        }

        if (saved != null) {
            startActivity(ReservationSummaryActivity.createIntent(this, saved.id, action))
        }
        finish()
    }

    private fun showPickerError(errorView: TextView, errorRes: Int?) {
        if (errorRes == null) {
            errorView.visibility = View.GONE
        } else {
            errorView.setText(errorRes)
            errorView.visibility = View.VISIBLE
        }
    }

    // The picker fields can't take focus, so scroll to them instead
    private fun scrollTo(view: View) {
        scrollView.post { scrollView.smoothScrollTo(0, (view.top - SCROLL_MARGIN_PX).coerceAtLeast(0)) }
    }

    companion object {
        private const val EXTRA_RESERVATION_ID = "reservation_id"

        private const val STATE_NODE_ID = "node_id"
        private const val STATE_DATE_MILLIS = "date_millis"
        private const val STATE_HOUR = "hour"
        private const val STATE_MINUTE = "minute"
        private const val STATE_DURATION = "duration"

        private const val DEFAULT_DURATION_HOURS = 1
        private val DURATION_CHOICES = listOf(1, 2, 3, 4)
        private const val SCROLL_MARGIN_PX = 120
        private val DECIMAL_FORMAT = java.text.DecimalFormat("0.#")

        fun newReservationIntent(context: Context): Intent =
            Intent(context, ReservationFormActivity::class.java)

        fun modifyReservationIntent(context: Context, reservationId: String): Intent =
            Intent(context, ReservationFormActivity::class.java).putExtra(EXTRA_RESERVATION_ID, reservationId)
    }
}
