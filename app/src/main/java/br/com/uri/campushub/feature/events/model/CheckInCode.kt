package br.com.uri.campushub.feature.events.model

data class CheckInQrPayload(
    val eventId: String,
    val attendeeId: String,
    val token: String
)

object CheckInCode {
    const val PAYLOAD_PREFIX = "CAMPUSHUB_CHECKIN"
    const val MANUAL_CODE_LENGTH = 8

    private val manualCodePattern = Regex("[0-9A-F]{$MANUAL_CODE_LENGTH}")

    fun createQrPayload(eventId: String, attendeeId: String, token: String): String =
        "$PAYLOAD_PREFIX|$eventId|$attendeeId|$token"

    fun parseQrPayload(value: String, expectedEventId: String): CheckInQrPayload? {
        val parts = value.split('|', limit = 4)
        if (
            parts.size != 4 ||
            parts[0] != PAYLOAD_PREFIX ||
            parts[1] != expectedEventId ||
            parts[2].isBlank() ||
            parts[3].isBlank()
        ) {
            return null
        }

        return CheckInQrPayload(
            eventId = parts[1],
            attendeeId = parts[2],
            token = parts[3]
        )
    }

    fun manualCodeFromToken(token: String): String =
        token.replace("-", "").take(MANUAL_CODE_LENGTH).uppercase()

    fun normalizeManualCode(value: String): String = value.trim().uppercase()

    fun isValidManualCode(value: String): Boolean =
        manualCodePattern.matches(normalizeManualCode(value))
}
