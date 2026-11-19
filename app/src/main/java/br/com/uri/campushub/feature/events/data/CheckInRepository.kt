package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import java.util.UUID

interface CheckInDataSource {
    fun getEventRegistrations(eventId: String): Task<QuerySnapshot>
    fun ensureQrToken(userId: String, eventId: String): Task<String>
    fun checkIn(organizerId: String, eventId: String, attendeeId: String, qrToken: String): Task<Boolean>
}

object CheckInRepository : CheckInDataSource {
    private const val EVENTS_COLLECTION = "events"
    private const val REGISTRATIONS_COLLECTION = "registrations"

    override fun getEventRegistrations(eventId: String): Task<QuerySnapshot> =
        eventDocument(eventId).collection(REGISTRATIONS_COLLECTION).get()

    override fun ensureQrToken(userId: String, eventId: String): Task<String> =
        FirebaseServices.firestore.runTransaction { transaction ->
            val registration = eventRegistrationDocument(eventId, userId)
            val snapshot = transaction.get(registration)
            require(snapshot.exists()) { "Inscrição não encontrada." }
            require(snapshot.getString("attendeeId") == userId) { "Inscrição inválida." }
            require(snapshot.getString("status") == RegistrationStatus.CONFIRMED.firestoreValue) {
                "Somente inscrições confirmadas possuem QR Code."
            }

            val existingToken = snapshot.getString("qrToken").orEmpty()
            if (existingToken.isNotBlank()) return@runTransaction existingToken

            UUID.randomUUID().toString().also { token ->
                transaction.update(registration, "qrToken", token)
            }
        }

    override fun checkIn(
        organizerId: String,
        eventId: String,
        attendeeId: String,
        qrToken: String
    ): Task<Boolean> = FirebaseServices.firestore.runTransaction { transaction ->
        val event = transaction.get(eventDocument(eventId))
        require(event.exists() && event.getString("organizerId") == organizerId) {
            "Você não pode registrar presença neste evento."
        }
        val registration = eventRegistrationDocument(eventId, attendeeId)
        val registrationSnapshot = transaction.get(registration)
        require(registrationSnapshot.exists()) { "Inscrição não encontrada." }
        require(registrationSnapshot.getString("status") == RegistrationStatus.CONFIRMED.firestoreValue) {
            "Somente inscrições confirmadas podem fazer check-in."
        }
        require(registrationSnapshot.getString("qrToken") == qrToken) { "QR Code inválido." }
        require(registrationSnapshot.getTimestamp("checkedInAt") == null) { "Este QR Code já foi utilizado." }

        transaction.update(registration, "checkedInAt", FieldValue.serverTimestamp())
        true
    }

    private fun eventDocument(eventId: String) =
        FirebaseServices.firestore.collection(EVENTS_COLLECTION).document(eventId)

    private fun eventRegistrationDocument(eventId: String, userId: String) =
        eventDocument(eventId).collection(REGISTRATIONS_COLLECTION).document(userId)
}
