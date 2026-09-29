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

    // Android emulator: 10.0.2.2 is the computer running the emulator, so this reaches the API
    // started with `dotnet run` (http profile, port 5262 in launchSettings.json).
    // On a real phone: use the computer's LAN IP instead (e.g. http://192.168.1.20:5262/api),
    // start the API with `dotnet run --urls http://0.0.0.0:5262`, and add the same IP to
    // res/xml/network_security_config.xml.
    const val BASE_URL = "http://10.0.2.2:5262/api"

    // Give up connecting after 10 seconds, and waiting for a reply after 15
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 15_000
}
