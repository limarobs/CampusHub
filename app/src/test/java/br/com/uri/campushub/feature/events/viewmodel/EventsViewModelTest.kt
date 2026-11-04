package br.com.uri.campushub.feature.events.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

class EventsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `loads events with sorted categories`() {
        val viewModel = EventsViewModel(FakeEventListDataSource(events = listOf(
            event("1", "Mostra", "Cultural"),
            event("2", "Oficina", "Acadêmico"),
            event("3", "Palestra", "Acadêmico")
        )))

        viewModel.loadEvents()

        val state = requireNotNull(viewModel.uiState.value)
        assertTrue(state.hasEvents)
        assertEquals(listOf(ALL_CATEGORIES, "Acadêmico", "Cultural"), state.categories)
        assertEquals(3, state.events.size)
    }

    @Test
    fun `shows an error when events cannot be loaded`() {
        val viewModel = EventsViewModel(
            FakeEventListDataSource(result = Result.failure(IllegalStateException()))
        )

        viewModel.loadEvents()

        val state = requireNotNull(viewModel.uiState.value)
        assertTrue(state.hasError)
        assertFalse(state.isLoading)
        assertFalse(state.hasEvents)
    }

    @Test
    fun `filters loaded events by trimmed search term`() {
        val viewModel = EventsViewModel(FakeEventListDataSource(events = listOf(
            event("1", "Semana Acadêmica", "Acadêmico"),
            event("2", "Cinema no Campus", "Cultural")
        )))
        viewModel.loadEvents()

        viewModel.updateSearchTerm("  cinema ")

        assertEquals(listOf("Cinema no Campus"), viewModel.uiState.value?.events?.map(CampusEvent::title))
    }

    @Test
    fun `filters loaded events by category`() {
        val viewModel = EventsViewModel(FakeEventListDataSource(events = listOf(
            event("1", "Semana Acadêmica", "Acadêmico"),
            event("2", "Cinema no Campus", "Cultural")
        )))
        viewModel.loadEvents()

        viewModel.updateCategory("Cultural")

        assertEquals("Cultural", viewModel.uiState.value?.selectedCategory)
        assertEquals(listOf("Cinema no Campus"), viewModel.uiState.value?.events?.map(CampusEvent::title))
    }

    @Test
    fun `filters events by upcoming and ended status`() {
        val viewModel = EventsViewModel(FakeEventListDataSource(events = listOf(
            event("1", "Futuro", "Acadêmico", endsAt = null),
            event("2", "Encerrado", "Acadêmico", endsAt = Timestamp(Date(0)))
        )))
        viewModel.loadEvents()

        viewModel.updateStatus(EventStatusFilter.UPCOMING)
        assertEquals(listOf("Futuro"), viewModel.uiState.value?.events?.map(CampusEvent::title))

        viewModel.updateStatus(EventStatusFilter.ENDED)
        assertEquals(listOf("Encerrado"), viewModel.uiState.value?.events?.map(CampusEvent::title))
    }

    @Test
    fun `resets a category that no longer exists after reload`() {
        val dataSource = FakeEventListDataSource(events = listOf(event("1", "Cinema", "Cultural")))
        val viewModel = EventsViewModel(dataSource)
        viewModel.loadEvents()
        viewModel.updateCategory("Cultural")

        dataSource.result = Result.success(listOf(event("2", "Oficina", "Acadêmico")))
        viewModel.loadEvents()

        assertEquals(ALL_CATEGORIES, viewModel.uiState.value?.selectedCategory)
        assertEquals(listOf("Oficina"), viewModel.uiState.value?.events?.map(CampusEvent::title))
    }

    private fun event(id: String, title: String, category: String, endsAt: Timestamp? = null) = CampusEvent(
        id = id,
        title = title,
        description = "Descrição",
        date = "04/11/2026",
        location = "URI",
        category = category,
        endsAt = endsAt
    )

    private class FakeEventListDataSource(
        events: List<CampusEvent> = emptyList(),
        var result: Result<List<CampusEvent>> = Result.success(events)
    ) : EventListDataSource {
        override fun getAvailableEvents(callback: (Result<List<CampusEvent>>) -> Unit) = callback(result)
    }
}
