package br.com.uri.campushub.feature.events.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CheckInScannerActivity : AppCompatActivity() {

    private lateinit var eventId: String
    private lateinit var organizerId: String
    private lateinit var previewView: PreviewView
    private lateinit var feedback: TextView
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private var cameraProvider: ProcessCameraProvider? = null
    private var handlingResult = false

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showCameraPermissionError()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        organizerId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (eventId.isBlank() || organizerId.isBlank()) {
            finish()
            return
        }

        setContentView(R.layout.activity_check_in_scanner)
        previewView = findViewById(R.id.previewCheckInCamera)
        feedback = findViewById(R.id.tvCheckInFeedback)
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnEnterCheckInCode).setOnClickListener {
            showManualCodeDialog()
        }
        findViewById<MaterialButton>(R.id.btnScanAgain).setOnClickListener {
            handlingResult = false
            feedback.text = "Aponte a câmera para o QR Code do aluno."
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get().also { provider ->
                provider.unbindAll()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor, ::analyzeImage)
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || handlingResult) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.rawValue != null }?.rawValue?.let(::validateQrCode)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun validateQrCode(rawValue: String) {
        if (handlingResult) return
        handlingResult = true
        val payload = rawValue.split('|', limit = 4)
        if (payload.size != 4 || payload[0] != RegistrationQrActivity.PAYLOAD_PREFIX || payload[1] != eventId) {
            showResult("Este QR Code não pertence a este evento.", retry = true)
            return
        }

        checkIn(payload[2], payload[3])
    }

    private fun showManualCodeDialog() {
        val input = EditText(this).apply {
            hint = "Ex.: A1B2C3D4"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Digitar codigo de check-in")
            .setMessage("Use o codigo alternativo mostrado junto ao QR Code do aluno.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Validar") { _, _ -> validateManualCode(input.text.toString()) }
            .show()
    }

    private fun validateManualCode(value: String) {
        if (handlingResult) return
        val code = value.trim().uppercase()
        if (code.length != RegistrationQrActivity.MANUAL_CODE_LENGTH) {
            showResult("Informe o codigo alternativo com 8 caracteres.", retry = true)
            return
        }

        handlingResult = true
        feedback.text = "Validando check-in..."
        EventRegistrationRepository.getEventRegistrations(eventId).addOnCompleteListener { task ->
            val registration = task.result?.documents?.firstOrNull { document ->
                document.getString("status") == "CONFIRMED"
                    && RegistrationQrActivity.manualCodeFromToken(document.getString("qrToken").orEmpty()) == code
            }
            if (!task.isSuccessful || registration == null) {
                showResult("Codigo alternativo invalido.", retry = true)
                return@addOnCompleteListener
            }
            checkIn(registration.getString("attendeeId").orEmpty(), registration.getString("qrToken").orEmpty())
        }
    }

    private fun checkIn(attendeeId: String, qrToken: String) {
        runOnUiThread { feedback.text = "Validando check-in..." }
        EventRegistrationRepository.checkIn(organizerId, eventId, attendeeId, qrToken)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showResult("Check-in registrado com sucesso.", retry = false)
                } else {
                    showResult(task.exception?.message ?: "Não foi possível validar o QR Code.", retry = true)
                }
            }
    }

    private fun showResult(message: String, retry: Boolean) {
        runOnUiThread {
            feedback.text = message
            findViewById<MaterialButton>(R.id.btnScanAgain).visibility = if (retry) View.VISIBLE else View.GONE
            if (!retry) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCameraPermissionError() {
        feedback.text = "Permita o uso da câmera para ler o QR Code."
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        scanner.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"

        fun createIntent(context: Context, eventId: String): Intent =
            Intent(context, CheckInScannerActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}
