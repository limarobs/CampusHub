package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot

object EventFavoriteRepository {

    private const val USERS_COLLECTION = "users"
    private const val FAVORITES_COLLECTION = "favorites"

    fun getFavorite(userId: String, eventId: String): Task<DocumentSnapshot> {
        return favoriteDocument(userId, eventId).get()
    }

    fun favorite(userId: String, event: CampusEvent): Task<Void> {
        val favorite = mapOf(
            "eventId" to event.id,
            "title" to event.title,
            "description" to event.description,
            "date" to event.date,
            "location" to event.location,
            "category" to event.category,
            "startsAt" to event.startsAt,
            "endsAt" to event.endsAt,
            "favoritedAt" to FieldValue.serverTimestamp()
        )

        return favoriteDocument(userId, event.id).set(favorite)
    }

    fun unfavorite(userId: String, eventId: String): Task<Void> {
        return favoriteDocument(userId, eventId).delete()
    }

    fun getFavorites(userId: String): Task<QuerySnapshot> {
        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(FAVORITES_COLLECTION)
            .get()
    }

    private fun favoriteDocument(userId: String, eventId: String) =
        FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(FAVORITES_COLLECTION)
            .document(eventId)
}
