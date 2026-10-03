/*
 * File:        QrVerificationResult.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Models
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Encapsulates the complete server verification state of a scanned
 *              prosumer transaction QR code, including cross-checked prosumer
 *              and microgrid node data.
 */

package com.example.mobileapp.models

/**
 * Result of validating a scanned QR code against the Web API.
 */
data class QrVerificationResult(
    val rawPayload: String,
    val extractedReference: String,
    val isVerified: Boolean,
    val canFinalizeTransfer: Boolean,
    val reservation: ApiReservation?,
    val prosumer: Prosumer?,
    val station: MicrogridStation?,
    val statusTitle: String,
    val statusMessage: String,
    val warningOrError: String? = null
)
