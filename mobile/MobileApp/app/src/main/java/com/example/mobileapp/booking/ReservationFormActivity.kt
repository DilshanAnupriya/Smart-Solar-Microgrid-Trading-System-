/*
 * File:        ReservationFormActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — sends new and modified
 *              reservations to the API, picks from the API's grid nodes (cached
 *              in SQLite) and leaves the 7-day and 12-hour rules to the API.
 * Description: Creates a new reservation, or modifies an existing one when it is
 *              started with a reservation.
 */

package com.example.mobileapp.booking

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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.db.DbHelper
import com.example.mobileapp.db.NodeCacheDao
import com.example.mobileapp.db.ReservationCacheDao
import com.example.mobileapp.models.GridNode
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.models.ReservationType
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.NodeApi
import com.example.mobileapp.network.ReservationApi
import com.example.mobileapp.utils.BaseActivity
import com.example.mobileapp.utils.DateTimeUtils
import com.example.mobileapp.utils.UiUtils
import com.example.mobileapp.utils.showServerFieldErrors
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Calendar

/**
 * The phone only checks that the form is filled in. Whether the slot is allowed (7-day window,
 * 12-hour notice, active node, energy range) is decided by the API, and its reason is shown.
 */
class ReservationFormActivity : BaseActivity() {

    // Set when modifying; null when creating a new reservation
    private var editing: Reservation? = null

    private lateinit var nodeCache: NodeCacheDao
    private lateinit var reservationCache: ReservationCacheDao

    // Nodes offered in the picker: from the API, or the copy in SQLite when offline
    private var nodes: List<GridNode> = emptyList()
    private var isLoadingNodes = false
    private var nodesFailureMessage: String? = null

    // Picked values; null until the user chooses them. Dates and times are Sri Lanka time.
    private var selectedNodeId: String? = null
    private var selectedNodeName: String? = null
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
    private lateinit var tvFormError: TextView
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        // Modify mode receives the reservation from the detail screen
        super.onCreate(savedInstanceState)
        editing = intent.getBundleExtra(EXTRA_RESERVATION)?.let { Reservation.fromBundle(it) }

        setContentView(R.layout.activity_reservation_form)

        nodeCache = NodeCacheDao(DbHelper.getInstance(this))
        reservationCache = ReservationCacheDao(DbHelper.getInstance(this))

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
        tvFormError = findViewById(R.id.tvFormError)
        btnSubmit = findViewById(R.id.btnSubmit)

        editing?.let { reservation ->
            findViewById<TextView>(R.id.tvTitle).setText(R.string.reservation_form_modify_title)
            findViewById<TextView>(R.id.tvModifyNote).apply {
                text = getString(R.string.reservation_form_modify_note, reservation.reservationNumber)
                visibility = View.VISIBLE
            }
            btnSubmit.setText(R.string.reservation_submit_modify)
            lockNodeAndType()
        }

        // Restore picked values after a rotation; otherwise pre-fill when modifying.
        // The type buttons and the energy field restore themselves.
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            editing?.let { prefill(it) }
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        if (editing == null) {
            tvNode.setOnClickListener { pickNode() }
        }
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

        // Fetch the nodes now, so the picker opens straight away (not needed when modifying)
        if (editing == null) {
            loadNodes()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Keep the picked values across rotation
        super.onSaveInstanceState(outState)
        selectedNodeId?.let { outState.putString(STATE_NODE_ID, it) }
        selectedNodeName?.let { outState.putString(STATE_NODE_NAME, it) }
        selectedDate?.let { outState.putLong(STATE_DATE_MILLIS, it.timeInMillis) }
        selectedHour?.let { outState.putInt(STATE_HOUR, it) }
        selectedMinute?.let { outState.putInt(STATE_MINUTE, it) }
        outState.putInt(STATE_DURATION, durationHours)
    }

    /**
     * Reads back the values saved by onSaveInstanceState.
     */
    private fun restoreState(state: Bundle) {
        // Each value is only present if the user had picked it
        selectedNodeId = state.getString(STATE_NODE_ID)
        selectedNodeName = state.getString(STATE_NODE_NAME)
        if (state.containsKey(STATE_DATE_MILLIS)) {
            selectedDate = sriLankaCalendar(state.getLong(STATE_DATE_MILLIS))
        }
        if (state.containsKey(STATE_HOUR)) selectedHour = state.getInt(STATE_HOUR)
        if (state.containsKey(STATE_MINUTE)) selectedMinute = state.getInt(STATE_MINUTE)
        durationHours = state.getInt(STATE_DURATION, DEFAULT_DURATION_HOURS)
    }

    /**
     * Fills the form from the reservation being modified.
     */
    private fun prefill(reservation: Reservation) {
        // Split the start time into Sri Lanka date, hour and minute for the pickers
        val start = sriLankaCalendar(reservation.startMillis)
        selectedNodeId = reservation.nodeId
        selectedNodeName = reservation.nodeName
        selectedDate = sriLankaCalendar(reservation.startMillis).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedHour = start.get(Calendar.HOUR_OF_DAY)
        selectedMinute = start.get(Calendar.MINUTE)
        durationHours = ((reservation.endMillis - reservation.startMillis + HOUR_MILLIS / 2) / HOUR_MILLIS)
            .toInt()
            .coerceAtLeast(1)
        rgType.check(if (reservation.type == ReservationType.CHARGING) R.id.rbCharging else R.id.rbDropOff)
        etEnergy.setText(DECIMAL_FORMAT.format(reservation.energyKwh))
    }

    /**
     * The API does not allow the node or the type of a reservation to change, so both are
     * shown read-only when modifying.
     */
    private fun lockNodeAndType() {
        // Grey the node field and remove its drop-down arrow; disable both type buttons
        tvNode.isEnabled = false
        tvNode.setBackgroundResource(R.drawable.bg_input_disabled)
        tvNode.setCompoundDrawablesRelative(null, null, null, null)
        findViewById<RadioButton>(R.id.rbDropOff).isEnabled = false
        findViewById<RadioButton>(R.id.rbCharging).isEnabled = false
    }

    /**
     * Writes the picked values into the picker fields (empty ones show their hint).
     */
    private fun showPickedValues() {
        // Dates and times are shown in Sri Lanka time
        tvNode.text = selectedNodeName
        tvDate.text = selectedDate?.let { formatReservationDate(it.timeInMillis) }
        tvTime.text = formatSelectedTime()
        tvDuration.text = resources.getQuantityString(R.plurals.reservation_duration, durationHours, durationHours)
    }

    /**
     * Formats just the picked hour and minute, so the time shows even before a date is chosen.
     */
    private fun formatSelectedTime(): String? {
        // Uses the phone's 12- or 24-hour setting
        val hour = selectedHour ?: return null
        val minute = selectedMinute ?: return null
        val time = Calendar.getInstance(DateTimeUtils.SRI_LANKA).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val format = DateFormat.getTimeFormat(this)
        format.timeZone = DateTimeUtils.SRI_LANKA
        return format.format(time.time)
    }

    /**
     * Explains the selected reservation type under the radio buttons.
     */
    private fun showTypeHelp() {
        // Drop-off sends energy to the node; charging takes energy from it
        tvTypeHelp.setText(selectedType().helpRes)
    }

    /**
     * The reservation type chosen with the radio buttons.
     */
    private fun selectedType(): ReservationType {
        // Drop-off is the default
        return if (rgType.checkedRadioButtonId == R.id.rbCharging) ReservationType.CHARGING else ReservationType.DROP_OFF
    }

    /**
     * GET /nodes into [nodes]; when the API can't be reached, the copy in SQLite is used instead.
     */
    private fun loadNodes() {
        // Runs in the background while the user fills in the rest of the form
        isLoadingNodes = true
        uiScope.launch {
            val result = NodeApi(this@ReservationFormActivity).getNodes()
            isLoadingNodes = false
            when (result) {
                is ApiResult.Success -> {
                    nodeCache.replaceAll(result.data)
                    nodes = result.data.sortedBy { it.name }
                    nodesFailureMessage = null
                }
                is ApiResult.Failure -> {
                    if (result.isUnauthorized) {
                        sessionExpired()
                    } else {
                        nodes = nodeCache.getAll()
                        nodesFailureMessage = result.message
                    }
                }
            }
        }
    }

    /**
     * Lets the user choose one of the active grid nodes returned by the API.
     */
    private fun pickNode() {
        // The API only returns nodes that are taking reservations
        when {
            isLoadingNodes -> UiUtils.showToast(this, getString(R.string.reservation_nodes_loading))
            nodes.isEmpty() -> {
                UiUtils.showToast(this, nodesFailureMessage ?: getString(R.string.reservation_nodes_empty))
                loadNodes()
            }
            else -> {
                if (nodesFailureMessage != null) {
                    UiUtils.showToast(this, getString(R.string.reservation_nodes_saved))
                }
                showNodeDialog()
            }
        }
    }

    /**
     * Single-choice list of the nodes, each with its address and free slots.
     */
    private fun showNodeDialog() {
        // Tapping a node selects it and closes the dialog
        val labels = nodes.map { node ->
            getString(
                R.string.reservation_node_option,
                node.name,
                node.address,
                resources.getQuantityString(R.plurals.reservation_node_slots, node.availableSlots, node.availableSlots)
            )
        }.toTypedArray()
        val checked = nodes.indexOfFirst { it.id == selectedNodeId }

        AlertDialog.Builder(this)
            .setTitle(R.string.reservation_node_label)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                selectedNodeId = nodes[which].id
                selectedNodeName = nodes[which].name
                tvNodeError.visibility = View.GONE
                showPickedValues()
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Date picker. Past days are greyed out; the 7-day limit is left to the API, which
     * explains it if a later date is chosen.
     */
    private fun pickDate() {
        // The picked day is taken as a Sri Lanka date
        val initial = selectedDate ?: Calendar.getInstance(DateTimeUtils.SRI_LANKA)
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = Calendar.getInstance(DateTimeUtils.SRI_LANKA).apply {
                    clear()
                    set(year, month, day)
                }
                tvWhenError.visibility = View.GONE
                showPickedValues()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    /**
     * Time picker, defaulting to the next full hour.
     */
    private fun pickTime() {
        // Hours and minutes are Sri Lanka time
        val nextHour = (Calendar.getInstance(DateTimeUtils.SRI_LANKA).get(Calendar.HOUR_OF_DAY) + 1) % 24
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

    /**
     * Lets the user choose how many hours the slot lasts.
     */
    private fun pickDuration() {
        // One to four hours; the API caps a slot at 12 hours
        val labels = DURATION_CHOICES
            .map { resources.getQuantityString(R.plurals.reservation_duration, it, it) }
            .toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.reservation_duration_label)
            .setSingleChoiceItems(labels, DURATION_CHOICES.indexOf(durationHours)) { dialog, which ->
                durationHours = DURATION_CHOICES[which]
                showPickedValues()
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Combines the picked date and time into epoch milliseconds, or returns null if either is missing.
     */
    private fun buildStartMillis(): Long? {
        // The date's calendar is in Sri Lanka time, so the hour and minute are too
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

    /**
     * Checks the form is filled in, then sends it to the API.
     */
    private fun attemptSubmit() {
        // Only "is it filled in" is checked here; the API decides whether the slot is allowed
        val nodeId = selectedNodeId
        val startMillis = buildStartMillis()
        val energyText = etEnergy.text.toString().trim()
        val energyKwh = energyText.toDoubleOrNull()

        val nodeError = if (nodeId == null) R.string.error_node_required else null
        val startError = if (startMillis == null) R.string.error_start_required else null
        val energyError = when {
            energyText.isEmpty() -> R.string.error_energy_required
            energyKwh == null || energyKwh <= 0 -> R.string.error_energy_positive
            else -> null
        }

        showPickerError(tvNodeError, nodeError)
        showPickerError(tvWhenError, startError)
        etEnergy.error = energyError?.let { getString(it) }

        // Bring the first problem into view, top to bottom
        when {
            nodeError != null -> scrollTo(tvNode)
            startError != null -> scrollTo(rowWhen)
            energyError != null -> etEnergy.requestFocus()
        }
        if (nodeId == null || startMillis == null || energyKwh == null || energyError != null) return

        val endMillis = startMillis + durationHours * HOUR_MILLIS
        UiUtils.showBanner(tvFormError, null)
        setLoading(true)

        uiScope.launch {
            val api = ReservationApi(this@ReservationFormActivity)
            val reservation = editing
            val result = if (reservation == null) {
                api.createReservation(nodeId, selectedType(), startMillis, endMillis, energyKwh)
            } else {
                api.updateReservation(reservation.id, startMillis, endMillis, energyKwh)
            }

            when (result) {
                is ApiResult.Success -> {
                    // Saved in SQLite only now, after the API has accepted it
                    reservationCache.upsert(result.data)
                    val action = if (reservation == null) {
                        ReservationSummaryActivity.Action.CREATED
                    } else {
                        ReservationSummaryActivity.Action.UPDATED
                    }
                    startActivity(
                        ReservationSummaryActivity.createIntent(this@ReservationFormActivity, result.data, action, result.message)
                    )
                    finish()
                }
                is ApiResult.Failure -> {
                    setLoading(false)
                    if (result.isUnauthorized) {
                        sessionExpired()
                    } else {
                        showSubmitError(result)
                    }
                }
            }
        }
    }

    /**
     * Shows why the API refused the reservation, e.g. "within 7 days" or "12 hours' notice".
     */
    private fun showSubmitError(failure: ApiResult.Failure) {
        // An energy validation message goes on the energy field; the banner always has the API's reason
        showServerFieldErrors(mapOf("energyAmountKWh" to etEnergy), failure.fieldErrors)
        UiUtils.showBanner(tvFormError, failure.message)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    /**
     * Locks the submit button while the request is running.
     */
    private fun setLoading(loading: Boolean) {
        // Prevents a second tap from sending the reservation twice
        btnSubmit.isEnabled = !loading
        btnSubmit.setText(
            when {
                loading -> R.string.reservation_submitting
                editing == null -> R.string.reservation_submit_new
                else -> R.string.reservation_submit_modify
            }
        )
    }

    /**
     * Shows or hides the red message under a picker field.
     */
    private fun showPickerError(errorView: TextView, errorRes: Int?) {
        // The picker fields can't show an EditText-style error, so a separate line is used
        if (errorRes == null) {
            errorView.visibility = View.GONE
        } else {
            errorView.setText(errorRes)
            errorView.visibility = View.VISIBLE
        }
    }

    /**
     * Scrolls a picker field into view (picker fields can't take focus).
     */
    private fun scrollTo(view: View) {
        // A small margin keeps the field's label visible above it
        scrollView.post { scrollView.smoothScrollTo(0, (view.top - SCROLL_MARGIN_PX).coerceAtLeast(0)) }
    }

    /**
     * A calendar in Sri Lanka time set to [millis].
     */
    private fun sriLankaCalendar(millis: Long): Calendar {
        // Every date and time on this form is Sri Lanka time
        return Calendar.getInstance(DateTimeUtils.SRI_LANKA).apply { timeInMillis = millis }
    }

    /**
     * The API no longer accepts the saved token.
     */
    private fun sessionExpired() {
        // The only way forward is to sign in again
        UiUtils.signOutToLogin(this, getString(R.string.session_expired))
    }

    companion object {
        private const val EXTRA_RESERVATION = "reservation"

        private const val STATE_NODE_ID = "node_id"
        private const val STATE_NODE_NAME = "node_name"
        private const val STATE_DATE_MILLIS = "date_millis"
        private const val STATE_HOUR = "hour"
        private const val STATE_MINUTE = "minute"
        private const val STATE_DURATION = "duration"

        private const val HOUR_MILLIS = 60 * 60 * 1000L
        private const val DEFAULT_DURATION_HOURS = 1
        private val DURATION_CHOICES = listOf(1, 2, 3, 4)
        private const val SCROLL_MARGIN_PX = 120
        private val DECIMAL_FORMAT = DecimalFormat("0.#")

        /**
         * Opens an empty form for a new reservation.
         */
        fun newReservationIntent(context: Context): Intent {
            // No extras: create mode
            return Intent(context, ReservationFormActivity::class.java)
        }

        /**
         * Opens the form filled in with [reservation], to modify it.
         */
        fun modifyReservationIntent(context: Context, reservation: Reservation): Intent {
            // The reservation travels in a Bundle, exactly as the API last returned it
            return Intent(context, ReservationFormActivity::class.java)
                .putExtra(EXTRA_RESERVATION, reservation.toBundle())
        }
    }
}
