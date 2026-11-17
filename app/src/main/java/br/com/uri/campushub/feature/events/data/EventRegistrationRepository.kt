package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.events.model.CampusEvent
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction

object EventRegistrationRepository {

    private const val USERS_COLLECTION = "users"
    private const val EVENTS_COLLECTION = "events"
    private const val REGISTRATIONS_COLLECTION = "registrations"

    fun getRegistration(userId: String, eventId: String): Task<DocumentSnapshot> {
        return registrationDocument(userId, eventId).get()
    }

    fun register(userId: String, event: CampusEvent): Task<RegistrationStatus> =
        FirebaseServices.firestore.runTransaction { transaction ->
            val eventDocument = eventDocument(event.id)
            val latestEvent = transaction.get(eventDocument)
            require(latestEvent.exists()) { "Evento não encontrado." }
            require(latestEvent.getString("status") == "PUBLISHED") { "Evento indisponível para inscrição." }

            val userRegistration = registrationDocument(userId, event.id)
            val existingRegistration = transaction.get(userRegistration)
            if (existingRegistration.exists()) {
                return@runTransaction RegistrationStatus.fromFirestore(existingRegistration.getString("status"))
            }

            val capacity = latestEvent.getLong("capacity")?.toInt()
            val confirmedCount = latestEvent.getLong("confirmedCount")?.toInt() ?: 0
            val waitlistCount = latestEvent.getLong("waitlistCount")?.toInt() ?: 0
            val status = if (capacity == null || confirmedCount < capacity) {
                RegistrationStatus.CONFIRMED
            } else {
                RegistrationStatus.WAITLISTED
            }
            val registration = registrationData(userId, event, status)

            transaction.set(eventRegistrationDocument(event.id, userId), registration)
            transaction.set(userRegistration, registration)
            transaction.update(
                eventDocument,
                mapOf(
                    "confirmedCount" to if (status == RegistrationStatus.CONFIRMED) confirmedCount + 1 else confirmedCount,
                    "waitlistCount" to if (status == RegistrationStatus.WAITLISTED) waitlistCount + 1 else waitlistCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            status
        }

    fun cancel(userId: String, eventId: String): Task<Void> =
        eventDocument(eventId).collection(REGISTRATIONS_COLLECTION)
            .whereEqualTo("status", RegistrationStatus.WAITLISTED.firestoreValue)
            .orderBy("waitlistedAt")
            .limit(1)
            .get()
            .continueWithTask { waitlistTask ->
                if (!waitlistTask.isSuccessful) {
                    throw waitlistTask.exception ?: IllegalStateException("Could not load waitlist")
                }
                val nextWaitlistedRegistration = waitlistTask.result?.documents?.firstOrNull()
                FirebaseServices.firestore.runTransaction { transaction ->
            val userRegistration = registrationDocument(userId, eventId)
            val existingRegistration = transaction.get(userRegistration)
            if (!existingRegistration.exists()) return@runTransaction null

            val eventDocument = eventDocument(eventId)
            val latestEvent = transaction.get(eventDocument)
            val eventRegistration = eventRegistrationDocument(eventId, userId)
            val eventRegistrationSnapshot = transaction.get(eventRegistration)
            if (!eventRegistrationSnapshot.exists()) {
                transaction.delete(userRegistration)
                return@runTransaction null
            }

            val status = RegistrationStatus.fromFirestore(eventRegistrationSnapshot.getString("status"))
            val confirmedCount = latestEvent.getLong("confirmedCount")?.toInt() ?: 0
            val waitlistCount = latestEvent.getLong("waitlistCount")?.toInt() ?: 0
            transaction.delete(userRegistration)
            transaction.delete(eventRegistration)

            when (status) {
                RegistrationStatus.WAITLISTED -> transaction.update(
                    eventDocument,
                    mapOf(
                        "waitlistCount" to (waitlistCount - 1).coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                RegistrationStatus.CONFIRMED -> promoteFirstWaitlisted(
                    transaction = transaction,
                    eventId = eventId,
                    eventDocument = eventDocument,
                    confirmedCount = confirmedCount,
                    waitlistCount = waitlistCount,
                    nextWaitlistedRegistration = nextWaitlistedRegistration
                )

                RegistrationStatus.CANCELLED -> Unit
            }
            null
                }
            }

    fun getRegistrations(userId: String): Task<QuerySnapshot> {
        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_COLLECTION)
            .get()
    }

    private fun registrationDocument(userId: String, eventId: String) =
        FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_COLLECTION)
            .document(eventId)

    private fun eventDocument(eventId: String) =
        FirebaseServices.firestore.collection(EVENTS_COLLECTION).document(eventId)

    private fun eventRegistrationDocument(eventId: String, userId: String) =
        eventDocument(eventId).collection(REGISTRATIONS_COLLECTION).document(userId)

    private fun registrationData(
        userId: String,
        event: CampusEvent,
        status: RegistrationStatus
    ): Map<String, Any?> = mapOf(
        "eventId" to event.id,
        "title" to event.title,
        "description" to event.description,
        "date" to event.date,
        "location" to event.location,
        "category" to event.category,
        "startsAt" to event.startsAt,
        "endsAt" to event.endsAt,
        "attendeeId" to userId,
        "status" to status.firestoreValue,
        "registeredAt" to FieldValue.serverTimestamp(),
        "confirmedAt" to if (status == RegistrationStatus.CONFIRMED) FieldValue.serverTimestamp() else null,
        "waitlistedAt" to if (status == RegistrationStatus.WAITLISTED) FieldValue.serverTimestamp() else null
    )

    private fun promoteFirstWaitlisted(
        transaction: Transaction,
        eventId: String,
        eventDocument: com.google.firebase.firestore.DocumentReference,
        confirmedCount: Int,
        waitlistCount: Int,
        nextWaitlistedRegistration: DocumentSnapshot?
    ) {
        val nextRegistration = nextWaitlistedRegistration?.let { candidate ->
            transaction.get(candidate.reference)
        }

        if (nextRegistration == null || nextRegistration.getString("status") != RegistrationStatus.WAITLISTED.firestoreValue) {
            transaction.update(
                eventDocument,
                mapOf(
                    "confirmedCount" to (confirmedCount - 1).coerceAtLeast(0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            return
        }

        val promotedUserId = nextRegistration.getString("attendeeId") ?: return
        val promotedData = mapOf(
            "status" to RegistrationStatus.CONFIRMED.firestoreValue,
            "confirmedAt" to FieldValue.serverTimestamp(),
            "waitlistedAt" to null
        )
        transaction.update(nextRegistration.reference, promotedData)
        transaction.update(registrationDocument(promotedUserId, eventId), promotedData)
        transaction.update(
            eventDocument,
            mapOf(
                "waitlistCount" to (waitlistCount - 1).coerceAtLeast(0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }
}
