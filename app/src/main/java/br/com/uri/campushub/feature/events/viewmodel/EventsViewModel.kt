package br.com.uri.campushub.feature.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.feature.events.data.EventRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.firebase.firestore.DocumentSnapshot

private const val ALL_CATEGORIES = "Todas as categorias"

enum class EventStatusFilter {
    ALL,
    UPCOMING,
    ENDED
}

data class EventsUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val hasEvents: Boolean = false,
    val events: List<CampusEvent> = emptyList(),
    val categories: List<String> = listOf(ALL_CATEGORIES),
    val selectedCategory: String = ALL_CATEGORIES
)

class EventsViewModel : ViewModel() {

    private val _uiState = MutableLiveData(EventsUiState())
    val uiState: LiveData<EventsUiState> = _uiState

    private var allEvents = emptyList<CampusEvent>()
    private var searchTerm = ""
    private var selectedCategory = ALL_CATEGORIES
    private var selectedStatus = EventStatusFilter.ALL

    fun loadEvents() {
        _uiState.value = currentState().copy(isLoading = true, hasError = false)

        EventRepository.getAvailableEvents().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                _uiState.value = currentState().copy(isLoading = false, hasError = true)
                return@addOnCompleteListener
            }

            allEvents = task.result?.documents.orEmpty().map(::toCampusEvent)
            val categories = buildCategories()
            if (selectedCategory !in categories) {
                selectedCategory = ALL_CATEGORIES
            }
            publishState()
        }
    }

    fun updateSearchTerm(value: String) {
        searchTerm = value.trim()
        publishState()
    }

    fun updateCategory(value: String) {
        selectedCategory = value
        publishState()
    }

    fun updateStatus(value: EventStatusFilter) {
        selectedStatus = value
        publishState()
    }

    private fun publishState() {
        _uiState.value = currentState().copy(isLoading = false, hasError = false)
    }

    private fun currentState(): EventsUiState {
        return EventsUiState(
            hasEvents = allEvents.isNotEmpty(),
            events = allEvents.filter(::matchesFilters),
            categories = buildCategories(),
            selectedCategory = selectedCategory
        )
    }

    private fun buildCategories(): List<String> {
        return listOf(ALL_CATEGORIES) + allEvents
            .map(CampusEvent::category)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    private fun matchesFilters(event: CampusEvent): Boolean {
        val matchesTitle = event.title.contains(searchTerm, ignoreCase = true)
        val matchesCategory = selectedCategory == ALL_CATEGORIES || event.category == selectedCategory
        val matchesStatus = when (selectedStatus) {
            EventStatusFilter.ALL -> true
            EventStatusFilter.UPCOMING -> !isEventEnded(event)
            EventStatusFilter.ENDED -> isEventEnded(event)
        }

        return matchesTitle && matchesCategory && matchesStatus
    }

    private fun isEventEnded(event: CampusEvent): Boolean {
        return event.endsAt?.toDate()?.time?.let { it <= System.currentTimeMillis() } == true
    }

    private fun toCampusEvent(document: DocumentSnapshot): CampusEvent {
        return CampusEvent(
            id = document.id,
            title = document.getString("title").orEmpty(),
            description = document.getString("description").orEmpty(),
            date = document.getString("date").orEmpty(),
            location = document.getString("location").orEmpty(),
            category = document.getString("category") ?: "Sem categoria",
            startsAt = document.getTimestamp("startsAt"),
            endsAt = document.getTimestamp("endsAt")
        )
    }
}
