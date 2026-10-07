package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot

object EventRatingRepository {

    private const val EVENTS_COLLECTION = "events"
    private const val RATINGS_COLLECTION = "ratings"

    fun getRating(eventId: String, userId: String): Task<DocumentSnapshot> {
        return ratingDocument(eventId, userId).get()
    }

    fun getRatings(eventId: String): Task<QuerySnapshot> {
        return ratingsCollection(eventId).get()
    }

    fun saveRating(eventId: String, userId: String, rating: Int): Task<Void> {
        val ratingData = mapOf(
            "userId" to userId,
            "rating" to rating,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return ratingDocument(eventId, userId).set(ratingData)
    }

    private fun ratingsCollection(eventId: String) = FirebaseServices.firestore
        .collection(EVENTS_COLLECTION)
        .document(eventId)
        .collection(RATINGS_COLLECTION)

    private fun ratingDocument(eventId: String, userId: String) =
        ratingsCollection(eventId).document(userId)
}
