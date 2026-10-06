package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot

object EventRegistrationRepository {

    private const val USERS_COLLECTION = "users"
    private const val REGISTRATIONS_COLLECTION = "registrations"

    fun getRegistration(userId: String, eventId: String): Task<DocumentSnapshot> {
        return registrationDocument(userId, eventId).get()
    }

    fun register(userId: String, event: CampusEvent): Task<Void> {
        val registration = mapOf(
            "eventId" to event.id,
            "title" to event.title,
            "description" to event.description,
            "date" to event.date,
            "location" to event.location,
            "registeredAt" to FieldValue.serverTimestamp()
        )

        return registrationDocument(userId, event.id).set(registration)
    }

    fun cancel(userId: String, eventId: String): Task<Void> {
        return registrationDocument(userId, eventId).delete()
    }

    fun getRegistrations(userId: String): Task<QuerySnapshot> {
        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_COLLECTION)
            .get()
    }

    private fun registrationDocument(userId: String, eventId: String) =
        FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_COLLECTION)
            .document(eventId)
}
