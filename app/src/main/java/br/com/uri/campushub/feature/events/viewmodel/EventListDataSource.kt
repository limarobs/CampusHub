package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.data.EventRepository
import br.com.uri.campushub.feature.events.data.toCampusEvent
import br.com.uri.campushub.feature.events.model.CampusEvent

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
            callback(Result.success(task.result!!.documents.map { it.toCampusEvent() }))
        }
    }
}
