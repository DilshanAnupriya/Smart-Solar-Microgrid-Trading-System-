/*
 * File:        SessionManagerTest.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Tests
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Unit tests for deciding whether a saved token has expired.
 */

package com.example.mobileapp.sessions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    // 2026-09-29T04:30:00Z in epoch milliseconds
    private val now = 1_790_656_200_000L

    @Test
    fun isExpired_falseBeforeTheExpiryTime() {
        // The API issues tokens that last 120 minutes
        assertFalse(SessionManager.isExpired("2026-09-29T06:30:00Z", now))
    }

    @Test
    fun isExpired_trueAtAndAfterTheExpiryTime() {
        // The API has no grace period (ClockSkew = 0), so neither does the phone
        assertTrue(SessionManager.isExpired("2026-09-29T04:30:00Z", now))
        assertTrue(SessionManager.isExpired("2026-09-29T04:29:59Z", now))
    }

    @Test
    fun isExpired_trueWhenTheTimeCannotBeRead() {
        // Better to ask the user to sign in again than to keep a token of unknown age
        assertTrue(SessionManager.isExpired("", now))
        assertTrue(SessionManager.isExpired("not a date", now))
    }
}
