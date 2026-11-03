package br.com.uri.campushub.feature.auth.domain

enum class InputField {
    NAME,
    EMAIL,
    PASSWORD
}

data class InputError(
    val field: InputField,
    val message: String
)

class AuthInputValidator {

    fun login(email: String, password: String): InputError? =
        emailError(email) ?: passwordError(password, allowEmpty = false)

    fun signup(name: String, email: String, password: String): InputError? =
        nameError(name) ?: emailError(email) ?: passwordError(password, allowEmpty = true)

    fun passwordRecovery(email: String): InputError? = emailError(email)

    fun profile(name: String): InputError? = nameError(name, "Informe seu nome.")

    private fun nameError(name: String, message: String = "Informe seu apelido."): InputError? =
        if (name.isBlank()) InputError(InputField.NAME, message) else null

    private fun emailError(email: String): InputError? =
        if (!EMAIL_REGEX.matches(email)) InputError(InputField.EMAIL, "Informe um e-mail válido.") else null

    private fun passwordError(password: String, allowEmpty: Boolean): InputError? = when {
        !allowEmpty && password.isBlank() -> InputError(InputField.PASSWORD, "Informe sua senha.")
        allowEmpty && password.length < MINIMUM_PASSWORD_LENGTH ->
            InputError(InputField.PASSWORD, "A senha deve ter pelo menos 6 caracteres.")
        else -> null
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
