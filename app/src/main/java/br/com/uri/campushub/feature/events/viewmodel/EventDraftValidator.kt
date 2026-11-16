package br.com.uri.campushub.feature.events.viewmodel

import br.com.uri.campushub.feature.events.model.EventDraft
import java.net.URI

data class EventDraftInput(
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val address: String,
    val startsAt: String,
    val endsAt: String,
    val capacity: String,
    val coverImageUrl: String
)

data class EventDraftValidation(
    val draft: EventDraft? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val locationError: String? = null,
    val addressError: String? = null,
    val startsAtError: String? = null,
    val endsAtError: String? = null,
    val capacityError: String? = null,
    val coverImageUrlError: String? = null
) {
    val isValid: Boolean
        get() = draft != null
}

class EventDraftValidator {

    fun validate(input: EventDraftInput): EventDraftValidation {
        val title = input.title.trim()
        val description = input.description.trim()
        val category = input.category.trim()
        val location = input.location.trim()
        val address = input.address.trim()
        val coverImageUrl = input.coverImageUrl.trim()

        val start = parseDate(input.startsAt)
        val end = if (input.endsAt.isBlank()) null else parseDate(input.endsAt)
        val capacity = input.capacity.trim().toIntOrNull()

        val validation = EventDraftValidation(
            titleError = requiredError(title, "Informe o título."),
            descriptionError = requiredError(description, "Informe a descrição."),
            categoryError = requiredError(category, "Informe a categoria."),
            locationError = requiredError(location, "Informe o local."),
            addressError = requiredError(address, "Informe o endereço."),
            startsAtError = if (start == null) "Use o formato dd/MM/aaaa HH:mm." else null,
            endsAtError = when {
                input.endsAt.isNotBlank() && end == null -> "Use o formato dd/MM/aaaa HH:mm."
                start != null && end != null && !end.toDate().after(start.toDate()) ->
                    "O encerramento deve ser posterior ao início."
                else -> null
            },
            capacityError = if (capacity == null || capacity <= 0) "Informe uma capacidade maior que zero." else null,
            coverImageUrlError = if (isPublicImageUrl(coverImageUrl)) null else "Informe uma URL pública de imagem válida."
        )

        if (
            validation.titleError != null || validation.descriptionError != null || validation.categoryError != null ||
            validation.locationError != null || validation.addressError != null || validation.startsAtError != null ||
            validation.endsAtError != null || validation.capacityError != null || validation.coverImageUrlError != null
        ) {
            return validation
        }

        return validation.copy(
            draft = EventDraft(
                title = title,
                description = description,
                category = category,
                location = location,
                address = address,
                startsAt = requireNotNull(start),
                endsAt = end,
                capacity = requireNotNull(capacity),
                coverImageUrl = coverImageUrl
            )
        )
    }

    private fun requiredError(value: String, error: String): String? =
        if (value.isBlank()) error else null

    private fun parseDate(value: String) = runCatching { EventDraft.parseDate(value) }.getOrNull()

    private fun isPublicImageUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
