/*
 * File:        ApiConfig.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Network
 * Author:      Cooray B.D.A (IT22189530)
 * Created:     2026-09-29
 * Description: Where the app finds the SmartSolar Web API, plus the network
 *              timeouts. The only place the server address is written down.
 */

package com.example.mobileapp.network

/**
 * Connection settings shared by every API call.
 */
object ApiConfig {

    // Default candidates for emulator and USB ADB reverse
    val CANDIDATE_URLS = listOf(
        "http://127.0.0.1:5262/api",
        "http://10.0.2.2:5262/api"
    )

    @Volatile
    var BASE_URL = "http://127.0.0.1:5262/api"

    // Give up connecting after 5 seconds, and waiting for a reply after 15
    const val CONNECT_TIMEOUT_MS = 5_000
    const val READ_TIMEOUT_MS = 15_000
}
