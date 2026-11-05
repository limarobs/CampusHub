package br.com.uri.campushub.feature.events.ui

import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import br.com.uri.campushub.feature.events.data.EventCommentRepository
import br.com.uri.campushub.feature.events.model.EventComment
import br.com.uri.campushub.feature.events.viewmodel.EventDetailsPolicy
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.DateFormat
import java.util.Date

internal class EventCommentsController(
    private val activity: AppCompatActivity,
    private val eventId: String,
    private val userId: String,
    private val authorName: String,
    private val policy: EventDetailsPolicy,
    private val input: TextInputEditText,
    private val addButton: MaterialButton,
    private val progress: ProgressBar,
    private val container: LinearLayout,
    private val emptyMessage: View,
    private val errorMessage: View
) {
    fun load() {
        progress.visibility = View.VISIBLE
        container.removeAllViews()
        emptyMessage.visibility = View.GONE
        errorMessage.visibility = View.GONE
        EventCommentRepository.getComments(eventId).addOnCompleteListener { task ->
            progress.visibility = View.GONE
            if (!task.isSuccessful) { errorMessage.visibility = View.VISIBLE; return@addOnCompleteListener }
            val comments = task.result?.documents.orEmpty().map(::toComment)
            if (comments.isEmpty()) emptyMessage.visibility = View.VISIBLE else comments.forEach(::addCard)
        }
    }

    fun add() {
        val content = policy.commentContent(input.text)
        if (content == null) { input.error = "Digite um comentário antes de publicar."; return }
        addButton.isEnabled = false
        addButton.text = "Publicando comentário..."
        EventCommentRepository.addComment(eventId, userId, authorName, content).addOnCompleteListener { task ->
            addButton.isEnabled = true
            addButton.text = "Publicar comentário"
            if (task.isSuccessful) { input.text?.clear(); load() }
            else Toast.makeText(activity, "Não foi possível publicar o comentário.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toComment(document: DocumentSnapshot) = EventComment(
        document.id, document.getString("authorId").orEmpty(),
        document.getString("authorName") ?: "Aluno", document.getString("content").orEmpty(),
        document.getTimestamp("createdAt")
    )

    private fun addCard(comment: EventComment) {
        val card = activity.layoutInflater.inflate(R.layout.item_event_comment, container, false)
        card.findViewById<android.widget.TextView>(R.id.tvCommentAuthor).text = comment.authorName
        card.findViewById<android.widget.TextView>(R.id.tvCommentDate).text = formatDate(comment.createdAt)
        card.findViewById<android.widget.TextView>(R.id.tvCommentContent).text = comment.content
        if (policy.canManagePublication(comment.authorId, userId)) {
            card.findViewById<LinearLayout>(R.id.commentOwnerActions).visibility = View.VISIBLE
            card.findViewById<MaterialButton>(R.id.btnEditComment).setOnClickListener { edit(comment) }
            card.findViewById<MaterialButton>(R.id.btnDeleteComment).setOnClickListener { delete(comment) }
        }
        container.addView(card)
    }

    private fun edit(comment: EventComment) {
        val editText = EditText(activity).apply {
            setText(comment.content)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(48, 0, 48, 0)
        }
        AlertDialog.Builder(activity).setTitle("Editar comentário").setView(editText)
            .setNegativeButton("Cancelar", null).setPositiveButton("Salvar") { _, _ ->
                val content = policy.commentContent(editText.text)
                if (content == null) { Toast.makeText(activity, "O comentário não pode ficar vazio.", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                EventCommentRepository.updateComment(eventId, comment.id, content).addOnCompleteListener { task ->
                    if (task.isSuccessful) load() else Toast.makeText(activity, "Não foi possível editar o comentário.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun delete(comment: EventComment) {
        AlertDialog.Builder(activity).setTitle("Excluir comentário?")
            .setMessage("Esta ação não pode ser desfeita.").setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                EventCommentRepository.deleteComment(eventId, comment.id).addOnCompleteListener { task ->
                    if (task.isSuccessful) load() else Toast.makeText(activity, "Não foi possível excluir o comentário.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun formatDate(timestamp: Timestamp?) = timestamp?.toDate()?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it.time))
    } ?: "Agora"
}
