package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.model.CampusEvent

const val ALL_CATEGORIES = "Todas as categorias"

enum class EventStatusFilter {
    ALL,
    UPCOMING,
    ENDED
}

class EventFilter(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    fun filter(
        events: List<CampusEvent>,
        searchTerm: String,
        selectedCategory: String,
        selectedStatus: EventStatusFilter
    ): List<CampusEvent> {
        return events.filter { event ->
            event.title.contains(searchTerm, ignoreCase = true) &&
                (selectedCategory == ALL_CATEGORIES || event.category == selectedCategory) &&
                matchesStatus(event, selectedStatus)
        }
    }

    fun categories(events: List<CampusEvent>): List<String> {
        return listOf(ALL_CATEGORIES) + events
            .map(CampusEvent::category)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun matchesStatus(event: CampusEvent, status: EventStatusFilter): Boolean {
        return when (status) {
            EventStatusFilter.ALL -> true
            EventStatusFilter.UPCOMING -> !isEnded(event)
            EventStatusFilter.ENDED -> isEnded(event)
        }
    }

    private fun isEnded(event: CampusEvent): Boolean {
        return event.endsAt?.toDate()?.time?.let { it <= currentTimeMillis() } == true
    }
}
