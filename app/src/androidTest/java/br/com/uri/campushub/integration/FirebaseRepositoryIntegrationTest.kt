package br.com.uri.campushub.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.data.EventFavoriteRepository
import br.com.uri.campushub.feature.events.data.EventRegistrationRepository
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.profile.data.UserProfileRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseRepositoryIntegrationTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var user: FirebaseUser

    @Before
    fun createAuthenticatedUser() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseServices.firestore

        val email = "integration-${UUID.randomUUID()}@campushub.test"
        await(auth.createUserWithEmailAndPassword(email, TEST_PASSWORD))
        user = requireNotNull(auth.currentUser)
    }

    @After
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun createsAnAuthenticatedFirebaseUser() {
        assertNotNull(user.uid)
        assertTrue(user.email?.endsWith("@campushub.test") == true)
    }

    @Test
    fun createsUserProfileInFirestore() {
        await(UserProfileRepository.create(user, "Ana"))

        val profile = await(firestore.collection("users").document(user.uid).get())

        assertEquals("Ana", profile.getString("displayName"))
        assertEquals(user.email, profile.getString("email"))
    }

    @Test
    fun updatesUserProfileInFirestore() {
        await(UserProfileRepository.create(user, "Ana"))
        await(UserProfileRepository.update(user, "Ana Clara"))

        val profile = await(firestore.collection("users").document(user.uid).get())

        assertEquals("Ana Clara", profile.getString("displayName"))
    }

    @Test
    fun savesAndReadsAnEventRegistration() {
        val event = testEvent("registration")
        await(EventRegistrationRepository.register(user.uid, event))

        val registration = await(EventRegistrationRepository.getRegistration(user.uid, event.id))

        assertTrue(registration.exists())
        assertEquals(event.title, registration.getString("title"))
    }

    @Test
    fun listsUserRegistrations() {
        val event = testEvent("registration-list")
        await(EventRegistrationRepository.register(user.uid, event))

        val registrations = await(EventRegistrationRepository.getRegistrations(user.uid))

        assertEquals(1, registrations.size())
        assertEquals(event.id, registrations.documents.single().id)
    }

    @Test
    fun cancelsAnEventRegistration() {
        val event = testEvent("registration-cancel")
        await(EventRegistrationRepository.register(user.uid, event))
        await(EventRegistrationRepository.cancel(user.uid, event.id))

        val registration = await(EventRegistrationRepository.getRegistration(user.uid, event.id))

        assertFalse(registration.exists())
    }

    @Test
    fun savesAndReadsAnEventFavorite() {
        val event = testEvent("favorite")
        await(EventFavoriteRepository.favorite(user.uid, event))

        val favorite = await(EventFavoriteRepository.getFavorite(user.uid, event.id))

        assertTrue(favorite.exists())
        assertEquals(event.title, favorite.getString("title"))
    }

    @Test
    fun listsAndRemovesEventFavorites() {
        val event = testEvent("favorite-list")
        await(EventFavoriteRepository.favorite(user.uid, event))

        val favorites = await(EventFavoriteRepository.getFavorites(user.uid))
        assertEquals(event.id, favorites.documents.single().id)

        await(EventFavoriteRepository.unfavorite(user.uid, event.id))
        assertFalse(await(EventFavoriteRepository.getFavorite(user.uid, event.id)).exists())
    }

    private fun testEvent(suffix: String) = CampusEvent(
        id = "event-$suffix-${UUID.randomUUID()}",
        title = "Evento de integração",
        description = "Evento usado somente pelos testes.",
        date = "03/11/2026",
        location = "Campus URI"
    )

    private fun <T> await(task: com.google.android.gms.tasks.Task<T>): T =
        Tasks.await(task, TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private companion object {
        const val TEST_PASSWORD = "123456"
        const val TASK_TIMEOUT_SECONDS = 15L
        const val EMULATOR_HOST = "10.0.2.2"
        const val AUTH_EMULATOR_PORT = 9099
        const val FIRESTORE_EMULATOR_PORT = 8080

        @JvmStatic
        @BeforeClass
        fun connectToFirebaseEmulators() {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
        }
    }
}
