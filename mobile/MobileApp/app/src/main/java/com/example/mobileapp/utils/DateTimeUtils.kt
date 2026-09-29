/*
 * File:        DateTimeUtils.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Reads the ISO-8601 UTC times sent by the API and formats them in
 *              Sri Lanka time, whatever time zone the phone is set to.
 */

package com.example.mobileapp.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date and time helpers shared by every screen.
 */
object DateTimeUtils {

    /** Sri Lanka Standard Time (UTC+05:30). */
    val SRI_LANKA: TimeZone = TimeZone.getTimeZone("Asia/Colombo")

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    // e.g. 2026-09-29T10:15:30Z, 2026-09-29T10:15:30.1234567Z, 2026-09-29T15:45:30+05:30,
    // or 2026-09-29T10:15:30 with no zone (the API only sends that for UTC values)
    private val ISO_PATTERN = Regex(
        """^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?(Z|[+-]\d{2}:?\d{2})?$"""
    )

    /**
     * Converts an ISO-8601 time from the API into epoch milliseconds, or null if it can't be read.
     */
    fun parseIsoToMillis(text: String?): Long? {
        // Build the moment field by field in UTC, then apply any offset the text carries
        val match = ISO_PATTERN.matchEntire(text?.trim().orEmpty()) ?: return null
        val parts = match.groupValues
        return try {
            val calendar = Calendar.getInstance(UTC).apply {
                clear()
                // Reject impossible dates such as month 13 instead of rolling them over
                isLenient = false
                set(
                    parts[1].toInt(), parts[2].toInt() - 1, parts[3].toInt(),
                    parts[4].toInt(), parts[5].toInt(), parts[6].ifEmpty { "0" }.toInt()
                )
            }
            // Only milliseconds matter; .NET can send up to seven fraction digits
            val millisOfSecond = parts[7].padEnd(3, '0').take(3).toInt()
            calendar.timeInMillis + millisOfSecond - offsetMillis(parts[8])
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * e.g. "29 Sep 2026", in Sri Lanka time.
     */
    fun formatDate(millis: Long): String {
        // A new formatter each call, because SimpleDateFormat is not thread-safe
        val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        format.timeZone = SRI_LANKA
        return format.format(Date(millis))
    }

    /**
     * e.g. "Tue, 30 Sep 2026", in Sri Lanka time.
     */
    fun formatDayDate(millis: Long): String {
        // Same as formatDate, with the day of the week in front
        val format = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        format.timeZone = SRI_LANKA
        return format.format(Date(millis))
    }

    /**
     * e.g. "2026-10-02T04:30:00Z": the UTC form the API expects in request bodies.
     */
    fun formatIsoUtc(millis: Long): String {
        // Always UTC with a "Z", so the API never has to guess the time zone
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = UTC
        return format.format(Date(millis))
    }

    /**
     * Offset of a "+05:30", "-0100" or "Z" suffix in milliseconds; no suffix means UTC.
     */
    private fun offsetMillis(zone: String): Long {
        // "Z" and a missing zone are both UTC
        if (zone.isEmpty() || zone == "Z") return 0L
        val digits = zone.substring(1).replace(":", "")
        val minutes = digits.substring(0, 2).toInt() * 60 + digits.substring(2, 4).toInt()
        val sign = if (zone[0] == '-') -1 else 1
        return sign * minutes * 60_000L
    }
}
