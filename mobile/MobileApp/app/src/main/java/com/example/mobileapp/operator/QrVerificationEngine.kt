/*
 * File:        QrVerificationEngine.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Operator / Business Logic
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Verifies scanned prosumer QR transaction codes against live server data.
 *              Decodes multiple formats (base64, pipe tokens, reservation IDs), validates
 *              prosumer status, and determines whether energy transfer business logic can proceed.
 */

package com.example.mobileapp.operator

import android.content.Context
import android.util.Base64
import com.example.mobileapp.models.ApiReservation
import com.example.mobileapp.models.MicrogridStation
import com.example.mobileapp.models.Prosumer
import com.example.mobileapp.models.QrVerificationResult
import com.example.mobileapp.network.ApiResult
import com.example.mobileapp.network.NodeApi
import com.example.mobileapp.network.ProsumerApi
import com.example.mobileapp.network.ReservationApi

class QrVerificationEngine(private val context: Context) {

    private val reservationApi = ReservationApi(context)
    private val prosumerApi = ProsumerApi(context)
    private val nodeApi = NodeApi(context)

    fun extractReference(rawQr: String): String = Companion.extractReference(rawQr)

    /**
     * Executes the complete server verification workflow for a scanned QR payload.
     */
    suspend fun verifyServerData(rawQr: String): QrVerificationResult {
        val reference = extractReference(rawQr)
        if (reference.isBlank()) {
            return QrVerificationResult(
                rawPayload = rawQr,
                extractedReference = "",
                isVerified = false,
                canFinalizeTransfer = false,
                reservation = null,
                prosumer = null,
                station = null,
                statusTitle = "Invalid QR Code",
                statusMessage = "Could not decode any valid reservation reference from the scanned QR code.",
                warningOrError = "Format not recognized"
            )
        }

        // 1. Fetch reservation from server
        val resResult = reservationApi.findByReference(reference)
        if (resResult is ApiResult.Failure) {
            return QrVerificationResult(
                rawPayload = rawQr,
                extractedReference = reference,
                isVerified = false,
                canFinalizeTransfer = false,
                reservation = null,
                prosumer = null,
                station = null,
                statusTitle = "Reservation Not Found",
                statusMessage = resResult.message,
                warningOrError = "Server check failed (HTTP ${resResult.statusCode})"
            )
        }

        val reservation = (resResult as ApiResult.Success).data

        // 2. Check Prosumer Profile & Active Status
        var prosumer: Prosumer? = null
        if (reservation.prosumerNic.isNotBlank()) {
            val prosumerResult = prosumerApi.getProfile(reservation.prosumerNic)
            if (prosumerResult is ApiResult.Success) {
                prosumer = prosumerResult.data
            }
        }

        // 3. Check Microgrid Station / Node
        var station: MicrogridStation? = null
        if (reservation.nodeId.isNotBlank()) {
            val nodeResult = nodeApi.getById(reservation.nodeId)
            if (nodeResult is ApiResult.Success) {
                station = nodeResult.data
            }
        }

        // 4. Verify Business Logic Rules:
        val isCompleted = reservation.isCompleted
        val isCancelled = reservation.isCancelled
        val isProsumerDeactivated = prosumer?.status?.equals("Deactivated", ignoreCase = true) == true

        val (canFinalize, title, message, warning) = when {
            isCompleted -> Quad(
                false,
                "Transfer Already Finalized",
                "This energy transfer has already been completed.",
                "Completed on ${reservation.updatedAt.ifBlank { "record" }}"
            )
            isCancelled -> Quad(
                false,
                "Reservation Cancelled",
                "This booking was cancelled: ${reservation.cancellationReason ?: "No reason specified"}.",
                "Cancelled booking cannot be transferred"
            )
            isProsumerDeactivated -> Quad(
                false,
                "Prosumer Deactivated",
                "The prosumer account (${prosumer?.nic}) is currently deactivated by the backoffice.",
                "Action blocked: account inactive"
            )
            reservation.isPending -> Quad(
                true,
                "Pending Reservation",
                "Booking is pending approval. You can verify prosumer details and finalize transfer.",
                null
            )
            else -> Quad(
                true,
                "Verified & Ready for Transfer",
                "Valid approved booking verified against central API. Ready to finalize energy transfer.",
                null
            )
        }

        return QrVerificationResult(
            rawPayload = rawQr,
            extractedReference = reference,
            isVerified = true,
            canFinalizeTransfer = canFinalize,
            reservation = reservation,
            prosumer = prosumer,
            station = station,
            statusTitle = title,
            statusMessage = message,
            warningOrError = warning
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        fun extractReference(rawQr: String): String {
            val trimmed = rawQr.trim().removeSurrounding("\"").removeSurrounding("'")

            // 1. Check for SSM-RSV format: SSM-RSV|<id>|<token>
            if (trimmed.startsWith("SSM-RSV|")) {
                val parts = trimmed.split("|")
                if (parts.size >= 2 && parts[1].isNotBlank()) {
                    return parts[1].trim()
                }
            }

            // 2. Check if already a Mongo ObjectId hex (24 hex characters)
            if (trimmed.matches(Regex("^[0-9a-fA-F]{24}$"))) {
                return trimmed
            }

            // 3. Check for standard reservation reference prefixes (including RSV)
            if (trimmed.matches(Regex("^(RES|BK|SSM|RSV)-[A-Za-z0-9-]+$"))) {
                return trimmed
            }

            // 4. Check for pipe-separated format: RES-xxx|nic|nodeId...
            if (trimmed.contains("|")) {
                val parts = trimmed.split("|")
                if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                    return parts[0].trim()
                }
            }

            // 5. Try Base64 decoding (e.g. encoded reservation token or encoded pipe format)
            val decodedString = decodeBase64Safe(trimmed)
            if (!decodedString.isNullOrBlank() && decodedString.all { it in ' '..'~' || it == '\n' || it == '\r' }) {
                if (decodedString.contains("|")) {
                    val parts = decodedString.split("|")
                    if (parts.size >= 2 && decodedString.startsWith("SSM-RSV|")) {
                        return parts[1].trim()
                    }
                    if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                        return parts[0].trim()
                    }
                } else if (decodedString.matches(Regex("^(RES|BK|SSM|RSV)-[A-Za-z0-9-]+$"))) {
                    return decodedString.trim()
                } else if (decodedString.length in 5..50) {
                    return decodedString.trim()
                }
            }

            // 6. Return as raw reference ID/number
            return trimmed
        }

        fun decodeBase64Safe(text: String): String? {
            return try {
                val decoded = java.util.Base64.getDecoder().decode(text)
                String(decoded, Charsets.UTF_8)
            } catch (_: Throwable) {
                try {
                    val decoded = android.util.Base64.decode(text, android.util.Base64.DEFAULT)
                    String(decoded, Charsets.UTF_8)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}
