package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.model.CampusEvent
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class EventFilterTest {

    private val now = 1_000_000L
    private val filter = EventFilter { now }

    @Test
    fun `builds categories without blanks duplicates and in alphabetical order`() {
        val categories = filter.categories(
            listOf(
                event(id = "1", category = "Esporte"),
                event(id = "2", category = "Acadêmico"),
                event(id = "3", category = "Esporte"),
                event(id = "4", category = "")
            )
        )

        assertEquals(listOf(ALL_CATEGORIES, "Acadêmico", "Esporte"), categories)
    }

    @Test
    fun `filters title without case sensitivity`() {
        val result = filter.filter(
            events = listOf(event(id = "1", title = "Semana Acadêmica"), event(id = "2", title = "Feira")),
            searchTerm = "ACADÊMICA",
            selectedCategory = ALL_CATEGORIES,
            selectedStatus = EventStatusFilter.ALL
        )

        assertEquals(listOf("1"), result.map(CampusEvent::id))
    }

    @Test
    fun `filters by selected category`() {
        val result = filter.filter(
            events = listOf(event(id = "1", category = "Acadêmico"), event(id = "2", category = "Esporte")),
            searchTerm = "",
            selectedCategory = "Esporte",
            selectedStatus = EventStatusFilter.ALL
        )

        assertEquals(listOf("2"), result.map(CampusEvent::id))
    }

    @Test
    fun `keeps events without an end date among upcoming events`() {
        val result = filter.filter(
            events = listOf(
                event(id = "future", endsAt = timestamp(now + 1)),
                event(id = "without-end"),
                event(id = "ended", endsAt = timestamp(now - 1))
            ),
            searchTerm = "",
            selectedCategory = ALL_CATEGORIES,
            selectedStatus = EventStatusFilter.UPCOMING
        )

        assertEquals(listOf("future", "without-end"), result.map(CampusEvent::id))
    }

    @Test
    fun `returns only ended events when requested`() {
        val result = filter.filter(
            events = listOf(
                event(id = "future", endsAt = timestamp(now + 1)),
                event(id = "ended", endsAt = timestamp(now - 1))
            ),
            searchTerm = "",
            selectedCategory = ALL_CATEGORIES,
            selectedStatus = EventStatusFilter.ENDED
        )

        assertEquals(listOf("ended"), result.map(CampusEvent::id))
    }

    @Test
    fun `combines title category and status filters`() {
        val result = filter.filter(
            events = listOf(
                event(id = "1", title = "Palestra Kotlin", category = "Acadêmico", endsAt = timestamp(now + 1)),
                event(id = "2", title = "Palestra encerrada", category = "Acadêmico", endsAt = timestamp(now - 1)),
                event(id = "3", title = "Oficina Kotlin", category = "Tecnologia", endsAt = timestamp(now + 1))
            ),
            searchTerm = "kotlin",
            selectedCategory = "Acadêmico",
            selectedStatus = EventStatusFilter.UPCOMING
        )

        assertEquals(listOf("1"), result.map(CampusEvent::id))
    }

    private fun event(
        id: String,
        title: String = "Evento",
        category: String = "Acadêmico",
        endsAt: Timestamp? = null
    ) = CampusEvent(
        id = id,
        title = title,
        description = "Descrição",
        date = "10/11/2026",
        location = "Campus",
        category = category,
        endsAt = endsAt
    )

    private fun timestamp(time: Long) = Timestamp(Date(time))
}
