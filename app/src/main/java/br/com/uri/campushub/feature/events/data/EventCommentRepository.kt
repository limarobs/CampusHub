package br.com.uri.campushub.feature.events.data

import br.com.uri.campushub.core.firebase.FirebaseServices
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

object EventCommentRepository {

    private const val EVENTS_COLLECTION = "events"
    private const val COMMENTS_COLLECTION = "comments"

    fun getComments(eventId: String): Task<QuerySnapshot> {
        return commentsCollection(eventId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
    }

    fun addComment(
        eventId: String,
        authorId: String,
        authorName: String,
        content: String
    ): Task<DocumentReference> {
        val comment = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "content" to content,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return commentsCollection(eventId).add(comment)
    }

    fun updateComment(eventId: String, commentId: String, content: String): Task<Void> {
        return commentDocument(eventId, commentId).update(
            mapOf(
                "content" to content,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun deleteComment(eventId: String, commentId: String): Task<Void> {
        return commentDocument(eventId, commentId).delete()
    }

    private fun commentsCollection(eventId: String) = FirebaseServices.firestore
        .collection(EVENTS_COLLECTION)
        .document(eventId)
        .collection(COMMENTS_COLLECTION)

    private fun commentDocument(eventId: String, commentId: String) =
        commentsCollection(eventId).document(commentId)
}
