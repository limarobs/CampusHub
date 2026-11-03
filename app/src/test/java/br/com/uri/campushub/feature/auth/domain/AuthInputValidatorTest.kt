package br.com.uri.campushub.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInputValidatorTest {

    private val validator = AuthInputValidator()

    @Test
    fun `login rejects invalid email before password`() {
        assertEquals(
            InputError(InputField.EMAIL, "Informe um e-mail válido."),
            validator.login("email-invalido", "")
        )
    }

    @Test
    fun `login rejects empty password`() {
        assertEquals(
            InputError(InputField.PASSWORD, "Informe sua senha."),
            validator.login("aluno@uri.edu.br", " ")
        )
    }

    @Test
    fun `login accepts valid credentials`() {
        assertNull(validator.login("aluno@uri.edu.br", "123456"))
    }

    @Test
    fun `signup requires a nickname first`() {
        assertEquals(
            InputError(InputField.NAME, "Informe seu apelido."),
            validator.signup("", "email-invalido", "123")
        )
    }

    @Test
    fun `signup rejects passwords shorter than six characters`() {
        assertEquals(
            InputError(InputField.PASSWORD, "A senha deve ter pelo menos 6 caracteres."),
            validator.signup("Ana", "ana@uri.edu.br", "12345")
        )
    }

    @Test
    fun `signup accepts a complete form`() {
        assertNull(validator.signup("Ana", "ana@uri.edu.br", "123456"))
    }

    @Test
    fun `password recovery accepts only a valid email`() {
        assertEquals(
            InputError(InputField.EMAIL, "Informe um e-mail válido."),
            validator.passwordRecovery("ana.uri.edu.br")
        )
        assertNull(validator.passwordRecovery("ana@uri.edu.br"))
    }

    @Test
    fun `profile requires a display name`() {
        assertEquals(
            InputError(InputField.NAME, "Informe seu nome."),
            validator.profile("  ")
        )
    }
}
