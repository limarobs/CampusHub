package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRatingRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.model.CampusEvent

interface EventDetailsDataSource {
    fun getRegistration(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit)
    fun register(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit)
    fun cancelRegistration(userId: String, eventId: String, callback: (Result<Unit>) -> Unit)

    fun getFavorite(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit)
    fun favorite(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit)
    fun unfavorite(userId: String, eventId: String, callback: (Result<Unit>) -> Unit)

    fun getRatings(eventId: String, callback: (Result<List<Int>>) -> Unit)
    fun getRating(eventId: String, userId: String, callback: (Result<Int?>) -> Unit)
    fun saveRating(eventId: String, userId: String, rating: Int, callback: (Result<Unit>) -> Unit)
}

object FirebaseEventDetailsDataSource : EventDetailsDataSource {

    override fun getRegistration(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit) {
        EventRegistrationRepository.getRegistration(userId, eventId).addOnCompleteListener { task ->
            callback(task.toResult { it.exists() })
        }
    }

    override fun register(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit) {
        EventRegistrationRepository.register(userId, event).addOnCompleteListener { task ->
            callback(task.toUnitResult())
        }
    }

    override fun cancelRegistration(userId: String, eventId: String, callback: (Result<Unit>) -> Unit) {
        EventRegistrationRepository.cancel(userId, eventId).addOnCompleteListener { task ->
            callback(task.toUnitResult())
        }
    }

    override fun getFavorite(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit) {
        EventFavoriteRepository.getFavorite(userId, eventId).addOnCompleteListener { task ->
            callback(task.toResult { it.exists() })
        }
    }

    override fun favorite(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit) {
        EventFavoriteRepository.favorite(userId, event).addOnCompleteListener { task ->
            callback(task.toUnitResult())
        }
    }

    override fun unfavorite(userId: String, eventId: String, callback: (Result<Unit>) -> Unit) {
        EventFavoriteRepository.unfavorite(userId, eventId).addOnCompleteListener { task ->
            callback(task.toUnitResult())
        }
    }

    override fun getRatings(eventId: String, callback: (Result<List<Int>>) -> Unit) {
        EventRatingRepository.getRatings(eventId).addOnCompleteListener { task ->
            callback(task.toResult { snapshot ->
                snapshot.documents.mapNotNull { it.getLong("rating")?.toInt() }.filter { it in 1..5 }
            })
        }
    }

    override fun getRating(eventId: String, userId: String, callback: (Result<Int?>) -> Unit) {
        EventRatingRepository.getRating(eventId, userId).addOnCompleteListener { task ->
            callback(task.toResult { it.getLong("rating")?.toInt()?.takeIf { rating -> rating in 1..5 } })
        }
    }

    override fun saveRating(eventId: String, userId: String, rating: Int, callback: (Result<Unit>) -> Unit) {
        EventRatingRepository.saveRating(eventId, userId, rating).addOnCompleteListener { task ->
            callback(task.toUnitResult())
        }
    }

    private fun <T, R> com.google.android.gms.tasks.Task<T>.toResult(transform: (T) -> R): Result<R> =
        if (isSuccessful && result != null) Result.success(transform(result!!))
        else Result.failure(exception ?: IllegalStateException("Firebase request failed"))

    private fun com.google.android.gms.tasks.Task<Void>.toUnitResult(): Result<Unit> =
        if (isSuccessful) Result.success(Unit)
        else Result.failure(exception ?: IllegalStateException("Firebase request failed"))
}
