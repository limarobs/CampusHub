package br.com.uri.campushub.feature.events.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date
import org.json.JSONArray
import org.json.JSONObject

internal data class LocalEventMedia(
    val id: String,
    val authorId: String,
    val authorName: String,
    val createdAt: Long,
    val path: String
)

internal fun LocalEventMediaStore.readMedia(
    preferencesName: String,
    entryName: String
): MutableList<LocalEventMedia> = try {
    val json = read(preferencesName, entryName)
    MutableList(json.length()) { index -> json.getJSONObject(index).toLocalEventMedia() }
} catch (_: Exception) {
    mutableListOf()
}

internal fun LocalEventMediaStore.saveMedia(
    preferencesName: String,
    entryName: String,
    media: List<LocalEventMedia>
) {
    val json = JSONArray()
    media.forEach { item -> json.put(item.toJson()) }
    save(preferencesName, entryName, json)
}

private fun JSONObject.toLocalEventMedia() = LocalEventMedia(
    getString("id"),
    getString("authorId"),
    getString("authorName"),
    getLong("createdAt"),
    getString("path")
)

private fun LocalEventMedia.toJson() = JSONObject()
    .put("id", id)
    .put("authorId", authorId)
    .put("authorName", authorName)
    .put("createdAt", createdAt)
    .put("path", path)

internal class EventMediaItemFactory(private val activity: AppCompatActivity) {
    fun item() = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(activity.getColor(R.color.surface))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
    }

    fun authorText(author: String) = TextView(activity).apply {
        text = author
        textSize = 16f
        setTextColor(activity.getColor(R.color.text_primary))
    }

    fun dateText(createdAt: Long) = TextView(activity).apply {
        text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(createdAt))
        textSize = 12f
        setTextColor(activity.getColor(R.color.text_secondary))
    }

    fun deleteButton(onDelete: () -> Unit) = MaterialButton(activity).apply {
        text = "Excluir"
        setOnClickListener { onDelete() }
    }

    fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
