package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.EventStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

object EventRepository {

    private const val EVENTS_COLLECTION = "events"

    fun getAvailableEvents(): Task<QuerySnapshot> {
        return FirebaseServices.firestore
            .collection(EVENTS_COLLECTION)
            .whereEqualTo("status", EventStatus.PUBLISHED.firestoreValue)
            .get()
    }

    fun getEvent(eventId: String): Task<DocumentSnapshot> =
        FirebaseServices.firestore.collection(EVENTS_COLLECTION).document(eventId).get()
}
