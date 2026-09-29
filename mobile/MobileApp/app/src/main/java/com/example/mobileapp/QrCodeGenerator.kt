package com.example.mobileapp

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Draws QR codes using the ZXing core library (https://github.com/zxing/zxing).
 * ZXing works out which squares are dark; this class just paints them into a Bitmap.
 */
object QrCodeGenerator {

    /** Returns a square black-on-white QR code of [sizePx] x [sizePx] pixels. */
    fun generate(content: String, sizePx: Int): Bitmap {
        val hints = mapOf(
            // Level M still scans if about 15% of the code is damaged or glared
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            // Quiet zone around the code, in QR modules
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
