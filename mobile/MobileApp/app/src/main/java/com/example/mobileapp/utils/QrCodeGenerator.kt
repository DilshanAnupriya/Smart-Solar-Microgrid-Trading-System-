/*
 * File:        QrCodeGenerator.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       Utils
 * Author:      N. Jayasinghe (IT2XXXXXXX)
 * Created:     2026-09-29
 * Modified:    2026-09-30 by Cooray B.D.A (IT22189530) — moved to utils; it now
 *              only ever draws the token issued by the API.
 * Description: Draws a QR code bitmap with the ZXing core library
 *              (https://github.com/zxing/zxing, Apache License 2.0).
 */

package com.example.mobileapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * ZXing works out which squares are dark; this class just paints them into a Bitmap.
 * The content is always the server's token, exactly as received: the QR is never built
 * on the phone, so the grid operator's scan can be verified against the API.
 */
object QrCodeGenerator {

    /**
     * Returns a square black-on-white QR code of [sizePx] x [sizePx] pixels.
     */
    fun generate(content: String, sizePx: Int): Bitmap {
        // Level M still scans if about 15% of the code is damaged or glared; a 1-module quiet zone
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height) { index ->
            if (matrix.get(index % width, index / width)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
