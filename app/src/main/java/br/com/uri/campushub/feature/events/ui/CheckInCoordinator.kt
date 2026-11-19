package br.com.uri.campushub.feature.events.ui

import br.com.uri.campushub.feature.events.data.CheckInDataSource
import br.com.uri.campushub.feature.events.data.CheckInRepository
import br.com.uri.campushub.feature.events.data.toEventRegistration
import br.com.uri.campushub.feature.events.model.CheckInCandidate
import br.com.uri.campushub.feature.events.model.CheckInCode
import br.com.uri.campushub.feature.events.model.EventRegistration
import br.com.uri.campushub.feature.events.model.RegistrationStatus
import com.google.android.gms.tasks.Task

class CheckInCoordinator(
    private val dataSource: CheckInDataSource = CheckInRepository
) {
    fun candidateFromQr(rawValue: String, eventId: String): CheckInCandidate? =
        CheckInCode.parseQrPayload(rawValue, eventId)?.let { payload ->
            CheckInCandidate(payload.attendeeId, payload.token)
        }

    fun manualCode(value: String): String? =
        CheckInCode.normalizeManualCode(value).takeIf(CheckInCode::isValidManualCode)

    fun candidateFromManualCode(
        code: String,
        registrations: Iterable<EventRegistration>
    ): CheckInCandidate? = registrations.firstOrNull { registration ->
        registration.status == RegistrationStatus.CONFIRMED &&
            CheckInCode.manualCodeFromToken(registration.qrToken) == code
    }?.let { registration ->
        CheckInCandidate(registration.attendeeId, registration.qrToken)
    }

    fun resolveManualCandidate(eventId: String, code: String): Task<CheckInCandidate?> =
        dataSource.getEventRegistrations(eventId).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Não foi possível carregar as inscrições.")
            }
            candidateFromManualCode(
                code = code,
                registrations = task.result?.documents.orEmpty().map { it.toEventRegistration() }
            )
        }

    fun confirm(
        organizerId: String,
        eventId: String,
        candidate: CheckInCandidate
    ): Task<Boolean> = dataSource.checkIn(
        organizerId = organizerId,
        eventId = eventId,
        attendeeId = candidate.attendeeId,
        qrToken = candidate.qrToken
    )
}
