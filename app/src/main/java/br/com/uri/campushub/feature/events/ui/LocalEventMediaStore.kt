package br.com.uri.campushub.feature.events.ui

import android.content.Context
import java.io.File
import java.util.UUID
import org.json.JSONArray

/** Persiste os metadados e arquivos de mídia local de um evento. */
internal class LocalEventMediaStore(
    private val context: Context,
    private val eventId: String
) {
    fun createFile(directoryName: String, extension: String): File {
        val directory = File(context.filesDir, directoryName).apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.$extension")
    }

    fun read(preferencesName: String, entryName: String): JSONArray =
        JSONArray(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
                .getString("${entryName}_$eventId", "[]")
        )

    fun save(preferencesName: String, entryName: String, entries: JSONArray) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString("${entryName}_$eventId", entries.toString())
            .apply()
    }
}
