/*
 * File:        OperatorScanActivity.kt
 * Project:     Smart Solar Microgrid Trading System (SE4040)
 * Layer:       UI / Operator Mode
 * Author:      H. Bhathiya (IT22189530)
 * Created:     2026-09-30
 * Description: Live QR code scanner for grid site operators. Decodes prosumer transaction
 *              QR codes using ZXing and Camera preview frames, with manual entry and
 *              gallery image decoding fallbacks for emulators and viva tests.
 */

package com.example.mobileapp.operator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobileapp.R
import com.example.mobileapp.utils.BaseActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class OperatorScanActivity : BaseActivity(), TextureView.SurfaceTextureListener {

    private lateinit var textureCameraPreview: TextureView
    private lateinit var etManualReference: EditText
    private lateinit var btnVerifyManual: Button
    private lateinit var btnPickGallery: Button
    private lateinit var btnToggleTorch: Button
    private lateinit var btnBack: ImageButton
    private lateinit var progressVerifying: ProgressBar
    private lateinit var tvInstruction: TextView

    private lateinit var btnDemoColombo: Button
    private lateinit var btnDemoKandy: Button
    private lateinit var btnDemoPending: Button
    private lateinit var btnDemoGalle: Button

    private var camera: Camera? = null
    private var isTorchOn = false
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_scan)

        initViews()
        setupListeners()
        checkCameraPermission()
    }

    private fun initViews() {
        textureCameraPreview = findViewById(R.id.textureCameraPreview)
        etManualReference = findViewById(R.id.etManualReference)
        btnVerifyManual = findViewById(R.id.btnVerifyManual)
        btnPickGallery = findViewById(R.id.btnPickGallery)
        btnToggleTorch = findViewById(R.id.btnToggleTorch)
        btnBack = findViewById(R.id.btnBack)
        progressVerifying = findViewById(R.id.progressVerifying)
        tvInstruction = findViewById(R.id.tvInstruction)

        btnDemoColombo = findViewById(R.id.btnDemoColombo)
        btnDemoKandy = findViewById(R.id.btnDemoKandy)
        btnDemoPending = findViewById(R.id.btnDemoPending)
        btnDemoGalle = findViewById(R.id.btnDemoGalle)

        textureCameraPreview.surfaceTextureListener = this
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnToggleTorch.setOnClickListener { toggleTorch() }

        btnVerifyManual.setOnClickListener {
            val code = etManualReference.text.toString().trim()
            if (code.isNotEmpty()) {
                onQrCodeScanned(code)
            } else {
                Toast.makeText(this, "Please enter a reference code", Toast.LENGTH_SHORT).show()
            }
        }

        btnPickGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
        }

        // Quick demo test buttons for viva demonstrations
        btnDemoColombo.setOnClickListener { onQrCodeScanned("RES-20260922-A101") }
        btnDemoKandy.setOnClickListener { onQrCodeScanned("RES-20260922-B202") }
        btnDemoPending.setOnClickListener { onQrCodeScanned("RES-20260922-D404") }
        btnDemoGalle.setOnClickListener { onQrCodeScanned("RES-20260922-C303") }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission needed for live scanning. Manual input available.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        startCamera()
        startContinuousScanLoop()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopCamera()
        return true
    }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            stopCamera()
            camera = Camera.open()
            camera?.setDisplayOrientation(90)
            camera?.setPreviewTexture(textureCameraPreview.surfaceTexture)
            camera?.startPreview()
        } catch (_: Exception) {
            // Emulator or camera in use; manual entry is ready
        }
    }

    private fun stopCamera() {
        try {
            camera?.stopPreview()
            camera?.release()
            camera = null
        } catch (_: Exception) {}
    }

    private fun toggleTorch() {
        try {
            val params = camera?.parameters ?: return
            val modes = params.supportedFlashModes ?: return
            if (modes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                isTorchOn = !isTorchOn
                params.flashMode = if (isTorchOn) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                camera?.parameters = params
                btnToggleTorch.text = if (isTorchOn) "Torch ON" else "Torch"
            }
        } catch (_: Exception) {}
    }

    /**
     * Loops periodically grabbing frames from TextureView and running ZXing MultiFormatReader.
     */
    private fun startContinuousScanLoop() {
        isScanning = true
        uiScope.launch {
            while (isActive && isScanning) {
                delay(300)
                if (!isScanning) break

                val bitmap = textureCameraPreview.bitmap ?: continue
                val decodedText = withContext(Dispatchers.Default) {
                    decodeQrFromBitmap(bitmap)
                }

                if (decodedText != null && isScanning) {
                    isScanning = false
                    onQrCodeScanned(decodedText)
                    break
                }
            }
        }
    }

    /**
     * Uses ZXing core to decode QR barcodes from Bitmap pixels.
     */
    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            decodeFromUri(data.data!!)
        }
    }

    private fun decodeFromUri(uri: Uri) {
        progressVerifying.visibility = View.VISIBLE
        uiScope.launch {
            val decoded = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        decodeQrFromBitmap(bitmap)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            progressVerifying.visibility = View.GONE
            if (decoded != null) {
                onQrCodeScanned(decoded)
            } else {
                Toast.makeText(this@OperatorScanActivity, "Could not find a valid QR code in that image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Navigates to the verification screen once a QR payload is captured.
     */
    private fun onQrCodeScanned(qrPayload: String) {
        stopCamera()
        val intent = Intent(this, OperatorVerifyActivity::class.java).apply {
            putExtra(OperatorVerifyActivity.EXTRA_QR_CODE, qrPayload)
        }
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        isScanning = false
        stopCamera()
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 201
        private const val REQUEST_GALLERY_IMAGE = 202
    }
}
