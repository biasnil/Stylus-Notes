package com.biasnil.stylusnotes.input

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.penStore by preferencesDataStore(name = "pen")

data class InputSettings(
    val pens: List<PenIdentity> = emptyList(),
    val mode: InputMode = InputMode.PEN_ONLY,
    val policy: PenPolicy = PenPolicy.REGISTERED_ONLY,
)

/** Stores registered pens and input settings in DataStore. */
class PenRepository(private val context: Context) {

    private val pensKey = stringPreferencesKey("registered_pens")
    /** Single-pen key from the first version. Read once and migrated. */
    private val legacyPenKey = stringPreferencesKey("registered_pen")
    private val modeKey = stringPreferencesKey("input_mode")
    private val policyKey = stringPreferencesKey("pen_policy")

    private val listSerializer = ListSerializer(PenIdentity.serializer())

    val settings: Flow<InputSettings> = context.penStore.data.map { prefs ->
        InputSettings(
            pens = decodePens(prefs),
            mode = prefs[modeKey]?.let { runCatching { InputMode.valueOf(it) }.getOrNull() } ?: InputMode.PEN_ONLY,
            policy = prefs[policyKey]?.let { runCatching { PenPolicy.valueOf(it) }.getOrNull() } ?: PenPolicy.REGISTERED_ONLY,
        )
    }

    /** Adds a pen, or refreshes it if it's already registered (keeping its label). */
    suspend fun addPen(pen: PenIdentity) {
        context.penStore.edit { prefs ->
            val list = decodePens(prefs).toMutableList()
            val i = list.indexOfFirst { StylusFilter.samePen(it, pen) }
            if (i >= 0) {
                list[i] = pen.copy(label = list[i].label)
            } else {
                list += pen.copy(label = pen.label.ifBlank { nextLabel(list) })
            }
            writePens(prefs, list)
        }
    }

    suspend fun removePen(pen: PenIdentity) {
        context.penStore.edit { prefs ->
            writePens(prefs, decodePens(prefs).filterNot { StylusFilter.samePen(it, pen) })
        }
    }

    suspend fun setMode(mode: InputMode) {
        context.penStore.edit { it[modeKey] = mode.name }
    }

    suspend fun setPolicy(policy: PenPolicy) {
        context.penStore.edit { it[policyKey] = policy.name }
    }

    private fun decodePens(prefs: Preferences): List<PenIdentity> {
        prefs[pensKey]?.let { raw ->
            return runCatching { Json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
        }
        prefs[legacyPenKey]?.let { raw ->
            val old = runCatching { Json.decodeFromString(PenIdentity.serializer(), raw) }.getOrNull()
            if (old != null) return listOf(old.copy(label = old.label.ifBlank { "Pen 1" }))
        }
        return emptyList()
    }

    private fun writePens(prefs: MutablePreferences, list: List<PenIdentity>) {
        prefs[pensKey] = Json.encodeToString(listSerializer, list)
        prefs.remove(legacyPenKey)
    }

    private fun nextLabel(existing: List<PenIdentity>): String {
        var n = existing.size + 1
        while (existing.any { it.label == "Pen $n" }) n++
        return "Pen $n"
    }
}
