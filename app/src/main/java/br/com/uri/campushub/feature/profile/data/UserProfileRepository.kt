package br.com.uri.campushub.feature.profile.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import br.com.uri.campushub.feature.profile.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

object UserProfileRepository {

    private const val USERS_COLLECTION = "users"

    fun create(user: FirebaseUser, displayName: String): Task<Void> {
        val profile = mapOf(
            "displayName" to displayName,
            "email" to (user.email ?: ""),
            "role" to UserRole.STUDENT.firestoreValue,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(user.uid)
            .set(profile)
    }

    fun update(user: FirebaseUser, displayName: String): Task<Void> {
        val profile = mapOf(
            "displayName" to displayName,
            "email" to (user.email ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(user.uid)
            .set(profile, SetOptions.merge())
    }

    fun getRole(userId: String): Task<DocumentSnapshot> {
        return FirebaseServices.firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .get()
    }

    fun roleFrom(profile: DocumentSnapshot?): UserRole =
        UserRole.fromFirestore(profile?.getString("role"))
}
