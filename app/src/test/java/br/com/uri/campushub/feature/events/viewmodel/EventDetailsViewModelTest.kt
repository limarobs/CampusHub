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

class EventDetailsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `start shows available actions for a future event`() {
        val dataSource = FakeEventDetailsDataSource()
        val viewModel = EventDetailsViewModel(dataSource)

        viewModel.start(USER_ID, futureEvent())

        assertEquals("Inscrever-se", viewModel.uiState.value?.registrationText)
        assertEquals("Adicionar aos favoritos", viewModel.uiState.value?.favoriteText)
        assertEquals("Ainda não há avaliações para este evento.", viewModel.uiState.value?.ratingSummary)
    }

    @Test
    fun `registers a student in a future event`() {
        val dataSource = FakeEventDetailsDataSource()
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, futureEvent())

        viewModel.toggleRegistration()

        assertEquals(1, dataSource.registerCalls)
        assertEquals("Cancelar inscrição", viewModel.uiState.value?.registrationText)
        assertEquals("Inscrição realizada.", viewModel.message.value)
    }

    @Test
    fun `does not register a student in an ended event`() {
        val dataSource = FakeEventDetailsDataSource()
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, endedEvent())

        viewModel.toggleRegistration()

        assertEquals(0, dataSource.registerCalls)
        assertFalse(viewModel.uiState.value?.registrationEnabled == true)
        assertEquals("Não é possível se inscrever em um evento encerrado.", viewModel.message.value)
    }

    @Test
    fun `cancels an existing registration`() {
        val dataSource = FakeEventDetailsDataSource(registration = true)
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, futureEvent())

        viewModel.toggleRegistration()

        assertEquals(1, dataSource.cancelCalls)
        assertEquals("Inscrever-se", viewModel.uiState.value?.registrationText)
    }

    @Test
    fun `retries registration lookup after a failure`() {
        val dataSource = FakeEventDetailsDataSource(
            registrationResult = Result.failure(IllegalStateException())
        )
        val viewModel = EventDetailsViewModel(dataSource)

        viewModel.start(USER_ID, futureEvent())

        assertEquals("Tentar novamente", viewModel.uiState.value?.registrationText)
        assertEquals("Não foi possível verificar sua inscrição.", viewModel.message.value)
    }

    @Test
    fun `adds and removes a favorite`() {
        val dataSource = FakeEventDetailsDataSource()
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, futureEvent())

        viewModel.toggleFavorite()
        assertEquals(1, dataSource.favoriteCalls)
        assertEquals("Remover dos favoritos", viewModel.uiState.value?.favoriteText)

        viewModel.toggleFavorite()
        assertEquals(1, dataSource.unfavoriteCalls)
        assertEquals("Adicionar aos favoritos", viewModel.uiState.value?.favoriteText)
    }

    @Test
    fun `shows ratings average using valid ratings only`() {
        val dataSource = FakeEventDetailsDataSource(ratings = listOf(5, 3, 0, 8))
        val viewModel = EventDetailsViewModel(dataSource)

        viewModel.start(USER_ID, futureEvent())

        val summary = viewModel.uiState.value?.ratingSummary.orEmpty()
        assertTrue(summary.contains("4,0") || summary.contains("4.0"))
        assertTrue(summary.contains("(2"))
    }

    @Test
    fun `allows a registered student to rate an ended event`() {
        val dataSource = FakeEventDetailsDataSource(registration = true)
        val viewModel = EventDetailsViewModel(dataSource)

        viewModel.start(USER_ID, endedEvent())

        assertTrue(viewModel.uiState.value?.ratingEnabled == true)
        assertEquals("Publicar avaliação", viewModel.uiState.value?.ratingButtonText)
    }

    @Test
    fun `rejects invalid ratings before saving`() {
        val dataSource = FakeEventDetailsDataSource(registration = true)
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, endedEvent())

        viewModel.saveRating(6)

        assertEquals(0, dataSource.saveRatingCalls)
        assertEquals("Escolha uma nota de 1 a 5.", viewModel.message.value)
    }

    @Test
    fun `saves a valid rating and refreshes its summary`() {
        val dataSource = FakeEventDetailsDataSource(registration = true, ratings = listOf(4))
        val viewModel = EventDetailsViewModel(dataSource)
        viewModel.start(USER_ID, endedEvent())

        viewModel.saveRating(4)

        assertEquals(1, dataSource.saveRatingCalls)
        assertEquals(4, dataSource.lastSavedRating)
        assertEquals("Atualizar avaliação", viewModel.uiState.value?.ratingButtonText)
    }

    private fun futureEvent() = event(endsAt = null)

    private fun endedEvent() = event(endsAt = Timestamp(Date(0)))

    private fun event(endsAt: Timestamp?) = CampusEvent(
        id = "event-1",
        title = "Evento",
        description = "Descrição",
        date = "04/11/2026",
        location = "URI",
        endsAt = endsAt
    )

    private class FakeEventDetailsDataSource(
        registration: Boolean = false,
        favorite: Boolean = false,
        ratings: List<Int> = emptyList(),
        registrationResult: Result<Boolean> = Result.success(registration),
        favoriteResult: Result<Boolean> = Result.success(favorite)
    ) : EventDetailsDataSource {

        var registrationResult = registrationResult
        var favoriteResult = favoriteResult
        var ratingsResult: Result<List<Int>> = Result.success(ratings)
        var userRatingResult: Result<Int?> = Result.success(null)
        var registerResult: Result<Unit> = Result.success(Unit)
        var cancelResult: Result<Unit> = Result.success(Unit)
        var favoriteActionResult: Result<Unit> = Result.success(Unit)
        var unfavoriteResult: Result<Unit> = Result.success(Unit)
        var saveRatingResult: Result<Unit> = Result.success(Unit)
        var registerCalls = 0
        var cancelCalls = 0
        var favoriteCalls = 0
        var unfavoriteCalls = 0
        var saveRatingCalls = 0
        var lastSavedRating: Int? = null

        override fun getRegistration(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit) = callback(registrationResult)

        override fun register(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit) {
            registerCalls++
            if (registerResult.isSuccess) registrationResult = Result.success(true)
            callback(registerResult)
        }

        override fun cancelRegistration(userId: String, eventId: String, callback: (Result<Unit>) -> Unit) {
            cancelCalls++
            if (cancelResult.isSuccess) registrationResult = Result.success(false)
            callback(cancelResult)
        }

        override fun getFavorite(userId: String, eventId: String, callback: (Result<Boolean>) -> Unit) = callback(favoriteResult)

        override fun favorite(userId: String, event: CampusEvent, callback: (Result<Unit>) -> Unit) {
            favoriteCalls++
            if (favoriteActionResult.isSuccess) favoriteResult = Result.success(true)
            callback(favoriteActionResult)
        }

        override fun unfavorite(userId: String, eventId: String, callback: (Result<Unit>) -> Unit) {
            unfavoriteCalls++
            if (unfavoriteResult.isSuccess) favoriteResult = Result.success(false)
            callback(unfavoriteResult)
        }

        override fun getRatings(eventId: String, callback: (Result<List<Int>>) -> Unit) = callback(ratingsResult)

        override fun getRating(eventId: String, userId: String, callback: (Result<Int?>) -> Unit) = callback(userRatingResult)

        override fun saveRating(eventId: String, userId: String, rating: Int, callback: (Result<Unit>) -> Unit) {
            saveRatingCalls++
            lastSavedRating = rating
            if (saveRatingResult.isSuccess) userRatingResult = Result.success(rating)
            callback(saveRatingResult)
        }
    }

    private companion object {
        const val USER_ID = "user-1"
    }
}
