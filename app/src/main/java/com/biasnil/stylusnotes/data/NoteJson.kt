package com.biasnil.stylusnotes.data

import kotlinx.serialization.json.Json

object NoteJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(note: Note): String = json.encodeToString(Note.serializer(), note)

    /** Decodes and upgrades notes saved by older versions. */
    fun decode(text: String): Note = NoteMigration.migrate(json.decodeFromString(Note.serializer(), text))

    fun encodeIndex(index: LibraryIndex): String = json.encodeToString(LibraryIndex.serializer(), index)
    fun decodeIndex(text: String): LibraryIndex = json.decodeFromString(LibraryIndex.serializer(), text)
}
