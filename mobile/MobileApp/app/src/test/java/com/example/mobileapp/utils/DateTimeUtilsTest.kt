/*
 * File:        DateTimeUtilsTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Unit tests for reading the API's ISO-8601 times and showing them
 *              in Sri Lanka time.
 */

package com.example.mobileapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class DateTimeUtilsTest {

    // 2026-09-29T04:30:00Z in epoch milliseconds
    private val sampleMillis = 1_790_656_200_000L

    @Test
    fun parse_utcWithZ() {
        // The shape System.Text.Json uses for UTC DateTime values
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis("2026-09-29T04:30:00Z"))
    }

    @Test
    fun parse_keepsMillisecondsFromLongFractions() {
        // .NET can send seven fraction digits; only the first three (milliseconds) matter
        assertEquals(sampleMillis + 123, DateTimeUtils.parseIsoToMillis("2026-09-29T04:30:00.1234567Z"))
        assertEquals(sampleMillis + 500, DateTimeUtils.parseIsoToMillis("2026-09-29T04:30:00.5Z"))
    }

    @Test
    fun parse_appliesOffsets() {
        // 10:00 in Sri Lanka (+05:30) is 04:30 UTC
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis("2026-09-29T10:00:00+05:30"))
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis("2026-09-29T03:30:00-0100"))
    }

    @Test
    fun parse_noZoneMeansUtc() {
        // The API only leaves the zone off for values that are already UTC
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis("2026-09-29T04:30:00"))
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis("2026-09-29T04:30"))
    }

    @Test
    fun parse_rejectsTextThatIsNotATime() {
        // Anything unreadable gives null instead of throwing
        assertNull(DateTimeUtils.parseIsoToMillis(null))
        assertNull(DateTimeUtils.parseIsoToMillis(""))
        assertNull(DateTimeUtils.parseIsoToMillis("yesterday"))
        assertNull(DateTimeUtils.parseIsoToMillis("2026-13-01T00:00:00Z"))
    }

    @Test
    fun formatIsoUtc_isTheUtcFormTheApiExpects() {
        // Always UTC with a "Z", whatever the phone's time zone, and readable back
        assertEquals("2026-09-29T04:30:00Z", DateTimeUtils.formatIsoUtc(sampleMillis))
        assertEquals(sampleMillis, DateTimeUtils.parseIsoToMillis(DateTimeUtils.formatIsoUtc(sampleMillis)))
    }

    @Test
    fun formatDate_usesSriLankaTime() {
        // 20:00 UTC on 29 Sep is already 01:30 on 30 Sep in Sri Lanka; US English month names
        // keep the expected text the same on every computer
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val lateEveningUtc = DateTimeUtils.parseIsoToMillis("2026-09-29T20:00:00Z")!!
            assertEquals("30 Sep 2026", DateTimeUtils.formatDate(lateEveningUtc))
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }
}
