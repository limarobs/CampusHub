package br.com.uri.campushub.feature.profile.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun `maps organizer role from firestore`() {
        assertEquals(UserRole.ORGANIZER, UserRole.fromFirestore("ORGANIZER"))
    }

    @Test
    fun `treats missing or unknown roles as student`() {
        assertEquals(UserRole.STUDENT, UserRole.fromFirestore(null))
        assertEquals(UserRole.STUDENT, UserRole.fromFirestore("ADMIN"))
    }
}
