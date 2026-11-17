package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.EventDraft
import br.com.uri.campushub.feature.events.model.EventStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot

object EventEditorRepository {
    private const val EVENTS_COLLECTION = "events"

    fun createDraft(organizerId: String, draft: EventDraft): Task<String> {
        val document = FirebaseServices.firestore.collection(EVENTS_COLLECTION).document()
        val data = draft.toFirestoreData(organizerId, includeOrganizer = true).toMutableMap().apply {
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }

        return document.set(data).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Could not create event draft")
            }
            document.id
        }
    }

    fun updateDraft(eventId: String, organizerId: String, draft: EventDraft): Task<Void> {
        val data = draft.toFirestoreData(organizerId, includeOrganizer = false).toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return FirebaseServices.firestore.collection(EVENTS_COLLECTION).document(eventId).update(data)
    }

    fun updateStatus(eventId: String, status: EventStatus): Task<Void> =
        FirebaseServices.firestore.collection(EVENTS_COLLECTION).document(eventId).update(
            mapOf(
                "status" to status.firestoreValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

    fun getEvent(eventId: String): Task<DocumentSnapshot> = EventRepository.getEvent(eventId)

    fun getOrganizedEvents(organizerId: String): Task<QuerySnapshot> =
        FirebaseServices.firestore
            .collection(EVENTS_COLLECTION)
            .whereEqualTo("organizerId", organizerId)
            .get()
}
