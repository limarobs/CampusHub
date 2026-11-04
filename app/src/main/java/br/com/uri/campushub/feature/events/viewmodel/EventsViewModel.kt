package br.com.uri.campushub.feature.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.uri.campushub.feature.events.model.CampusEvent

data class EventsUiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val hasEvents: Boolean = false,
    val events: List<CampusEvent> = emptyList(),
    val categories: List<String> = listOf(ALL_CATEGORIES),
    val selectedCategory: String = ALL_CATEGORIES
)

class EventsViewModel(
    private val dataSource: EventListDataSource = FirebaseEventListDataSource
) : ViewModel() {

    private val _uiState = MutableLiveData(EventsUiState())
    val uiState: LiveData<EventsUiState> = _uiState

    private var allEvents = emptyList<CampusEvent>()
    private var searchTerm = ""
    private var selectedCategory = ALL_CATEGORIES
    private var selectedStatus = EventStatusFilter.ALL
    private val eventFilter = EventFilter()

    fun loadEvents() {
        _uiState.value = currentState().copy(isLoading = true, hasError = false)

        dataSource.getAvailableEvents { result ->
            if (result.isFailure) {
                _uiState.value = currentState().copy(isLoading = false, hasError = true)
                return@getAvailableEvents
            }

            allEvents = result.getOrDefault(emptyList())
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
            events = eventFilter.filter(allEvents, searchTerm, selectedCategory, selectedStatus),
            categories = eventFilter.categories(allEvents),
            selectedCategory = selectedCategory
        )
    }

    private fun buildCategories(): List<String> {
        return eventFilter.categories(allEvents)
    }
}
