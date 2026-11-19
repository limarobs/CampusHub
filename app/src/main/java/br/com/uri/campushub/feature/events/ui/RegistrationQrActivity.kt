package br.com.uri.campushub.feature.events.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.model.CheckInCode
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class RegistrationQrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        val user = FirebaseAuth.getInstance().currentUser
        if (eventId.isBlank() || user == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_registration_qr)
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        loadQrCode(user.uid, eventId)
    }

    private fun loadQrCode(userId: String, eventId: String) {
        val progress = findViewById<ProgressBar>(R.id.progressQrCode)
        val code = findViewById<ImageView>(R.id.ivQrCode)
        val manualCode = findViewById<TextView>(R.id.tvManualCheckInCode)
        val error = findViewById<TextView>(R.id.tvQrCodeError)
        EventRegistrationRepository.getRegistration(userId, eventId).addOnCompleteListener { task ->
            val registration = task.result
            val token = registration?.getString("qrToken").orEmpty()
            val isConfirmed = registration?.getString("status") == RegistrationStatus.CONFIRMED.firestoreValue
            if (!task.isSuccessful || !isConfirmed) {
                progress.visibility = View.GONE
                error.visibility = View.VISIBLE
                return@addOnCompleteListener
            }

            if (token.isNotBlank()) {
                showQrCode(code, manualCode, token, eventId, userId)
                progress.visibility = View.GONE
                return@addOnCompleteListener
            }

            EventRegistrationRepository.ensureQrToken(userId, eventId).addOnCompleteListener { tokenTask ->
                progress.visibility = View.GONE
                val newToken = tokenTask.result.orEmpty()
                if (!tokenTask.isSuccessful || newToken.isBlank()) {
                    error.visibility = View.VISIBLE
                    return@addOnCompleteListener
                }
                showQrCode(code, manualCode, newToken, eventId, userId)
            }
        }
    }

    private fun showQrCode(
        code: ImageView,
        manualCode: TextView,
        token: String,
        eventId: String,
        userId: String
    ) {
        code.setImageBitmap(createQrBitmap(CheckInCode.createQrPayload(eventId, userId, token)))
        code.visibility = View.VISIBLE
        manualCode.text = "Codigo alternativo: ${CheckInCode.manualCodeFromToken(token)}"
        manualCode.visibility = View.VISIBLE
    }

    private fun createQrBitmap(value: String): Bitmap {
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)
        return Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_EVENT_ID = "event_id"
        private const val QR_SIZE = 720

        fun createIntent(context: Context, eventId: String): Intent =
            Intent(context, RegistrationQrActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}
