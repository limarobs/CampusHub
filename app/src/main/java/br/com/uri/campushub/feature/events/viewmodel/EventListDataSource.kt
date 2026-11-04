package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.data.EventRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.firebase.firestore.DocumentSnapshot

interface EventListDataSource {
    fun getAvailableEvents(callback: (Result<List<CampusEvent>>) -> Unit)
}

object FirebaseEventListDataSource : EventListDataSource {

    override fun getAvailableEvents(callback: (Result<List<CampusEvent>>) -> Unit) {
        EventRepository.getAvailableEvents().addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                callback(Result.failure(task.exception ?: IllegalStateException("Firebase request failed")))
                return@addOnCompleteListener
            }
            callback(Result.success(task.result!!.documents.map(::toCampusEvent)))
        }
    }

    private fun toCampusEvent(document: DocumentSnapshot): CampusEvent = CampusEvent(
        id = document.id,
        title = document.getString("title").orEmpty(),
        description = document.getString("description").orEmpty(),
        date = document.getString("date").orEmpty(),
        location = document.getString("location").orEmpty(),
        address = document.getString("address").orEmpty(),
        latitude = (document.get("latitude") as? Number)?.toDouble(),
        longitude = (document.get("longitude") as? Number)?.toDouble(),
        category = document.getString("category") ?: "Sem categoria",
        startsAt = document.getTimestamp("startsAt"),
        endsAt = document.getTimestamp("endsAt")
    )
}
