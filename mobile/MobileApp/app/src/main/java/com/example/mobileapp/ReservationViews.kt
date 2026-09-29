package com.example.mobileapp

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Display helpers shared by the reservation screens, so a reservation is formatted
 * the same way in the list, the detail page and the summary page.
 */

/** e.g. "Tue, 30 Sep 2026" */
fun formatReservationDate(millis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

/** e.g. "10:00 – 12:00", in the phone's 12- or 24-hour setting */
fun formatReservationTime(context: Context, reservation: Reservation): String {
    val timeFormat = DateFormat.getTimeFormat(context)
    return context.getString(
        R.string.reservation_time_range,
        timeFormat.format(Date(reservation.startMillis)),
        timeFormat.format(Date(reservation.endMillis))
    )
}

/** e.g. "12.5 kWh" or "8 kWh" */
fun formatEnergy(context: Context, kwh: Double): String =
    context.getString(R.string.reservation_energy, DecimalFormat("0.#").format(kwh))

fun formatDuration(context: Context, hours: Int): String =
    context.resources.getQuantityString(R.plurals.reservation_duration, hours, hours)

/** Sets a status pill's text and colours. */
fun bindStatusBadge(badge: TextView, status: ReservationStatus) {
    badge.setText(status.labelRes)
    badge.setBackgroundResource(status.badgeRes)
    badge.setTextColor(badge.context.getColor(status.badgeTextColorRes))
}

/** Fills the rows of an included view_reservation_details layout. */
fun bindReservationDetails(root: View, reservation: Reservation) {
    val context = root.context
    root.findViewById<TextView>(R.id.tvDetailNode).text = reservation.node.name
    root.findViewById<TextView>(R.id.tvDetailNodeLocation).text = reservation.node.location
    root.findViewById<TextView>(R.id.tvDetailType).setText(reservation.type.labelRes)
    root.findViewById<TextView>(R.id.tvDetailDate).text = formatReservationDate(reservation.startMillis)
    root.findViewById<TextView>(R.id.tvDetailTime).text = formatReservationTime(context, reservation)
    root.findViewById<TextView>(R.id.tvDetailDuration).text = formatDuration(context, reservation.durationHours)
    root.findViewById<TextView>(R.id.tvDetailEnergy).text = formatEnergy(context, reservation.energyKwh)
}
