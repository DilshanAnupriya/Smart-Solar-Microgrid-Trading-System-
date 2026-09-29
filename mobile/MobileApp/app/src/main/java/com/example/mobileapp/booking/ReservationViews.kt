/*
 * File:        ReservationViews.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — times shown in Sri Lanka
 *              time and the duration worked out from the API's start and end.
 * Description: Display helpers shared by the reservation screens, so a
 *              reservation looks the same in the list, the detail page and the
 *              summary page.
 */

package com.example.mobileapp.booking

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import com.example.mobileapp.R
import com.example.mobileapp.models.Reservation
import com.example.mobileapp.models.ReservationStatus
import com.example.mobileapp.utils.DateTimeUtils
import java.text.DecimalFormat
import java.util.Date

/**
 * e.g. "Tue, 30 Sep 2026", in Sri Lanka time.
 */
fun formatReservationDate(millis: Long): String {
    // Shared with the rest of the app, so every date uses the same time zone
    return DateTimeUtils.formatDayDate(millis)
}

/**
 * e.g. "10:00 – 12:00", in Sri Lanka time and the phone's 12- or 24-hour setting.
 */
fun formatReservationTime(context: Context, reservation: Reservation): String {
    // The phone may be set to any time zone, but slots are always shown in Sri Lanka time
    val timeFormat = DateFormat.getTimeFormat(context)
    timeFormat.timeZone = DateTimeUtils.SRI_LANKA
    return context.getString(
        R.string.reservation_time_range,
        timeFormat.format(Date(reservation.startMillis)),
        timeFormat.format(Date(reservation.endMillis))
    )
}

/**
 * e.g. "12.5 kWh" or "8 kWh".
 */
fun formatEnergy(context: Context, kwh: Double): String {
    // At most one decimal place, and none for whole numbers
    return context.getString(R.string.reservation_energy, DecimalFormat("0.#").format(kwh))
}

/**
 * e.g. "2 hours" for whole hours, otherwise the length in minutes (a web booking may be 90 minutes).
 */
fun formatDuration(context: Context, reservation: Reservation): String {
    // Worked out from the API's start and end times
    val minutes = ((reservation.endMillis - reservation.startMillis) / 60_000L).toInt()
    return if (minutes % 60 == 0) {
        context.resources.getQuantityString(R.plurals.reservation_duration, minutes / 60, minutes / 60)
    } else {
        context.getString(R.string.reservation_duration_minutes, minutes)
    }
}

/**
 * Sets a status pill's text and colours.
 */
fun bindStatusBadge(badge: TextView, status: ReservationStatus) {
    // Each status carries its own label and colours
    badge.setText(status.labelRes)
    badge.setBackgroundResource(status.badgeRes)
    badge.setTextColor(badge.context.getColor(status.badgeTextColorRes))
}

/**
 * Fills the rows of an included view_reservation_details layout.
 */
fun bindReservationDetails(root: View, reservation: Reservation) {
    // Every value comes from the API's reply (or its copy in SQLite)
    val context = root.context
    root.findViewById<TextView>(R.id.tvDetailNode).text = reservation.nodeName
    root.findViewById<TextView>(R.id.tvDetailType).setText(reservation.type.labelRes)
    root.findViewById<TextView>(R.id.tvDetailDate).text = formatReservationDate(reservation.startMillis)
    root.findViewById<TextView>(R.id.tvDetailTime).text = formatReservationTime(context, reservation)
    root.findViewById<TextView>(R.id.tvDetailDuration).text = formatDuration(context, reservation)
    root.findViewById<TextView>(R.id.tvDetailEnergy).text = formatEnergy(context, reservation.energyKwh)
}
