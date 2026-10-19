package com.biasnil.stylusnotes.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * App-private storage:
 *   files/library.json      folders + note list (folder, trash, opened time)
 *   files/notes/<id>.json   one file per note (pages, strokes)
 *   files/assets/<id>.<ext> imported PDFs and images
 *   files/thumbs/<id>.png   first-page thumbnails
 */
class LibraryRepository(private val context: Context) {

    private val root = context.filesDir
    private val notesDir = File(root, "notes").apply { mkdirs() }
    private val assetsDir = File(root, "assets").apply { mkdirs() }
    private val thumbsDir = File(root, "thumbs").apply { mkdirs() }
    private val indexFile = File(root, "library.json")

    /** Guards the index. */
    private val indexLock = Mutex()
    /** Guards note files, so a load waits for a save that's already running. */
    private val noteLock = Mutex()
    private var loaded = false

    private val _state = MutableStateFlow(LibraryIndex())
    val state: StateFlow<LibraryIndex> = _state.asStateFlow()

    suspend fun ensureLoaded() = indexLock.withLock {
        if (loaded) return@withLock
        val index = withContext(Dispatchers.IO) {
            val stored = runCatching { NoteJson.decodeIndex(indexFile.readText()) }.getOrNull() ?: LibraryIndex()
            val present = stored.notes.filter { noteFile(it.id).exists() }
            val known = present.map { it.id }.toSet()
            // Notes on disk the index doesn't know about (e.g. saved by version 1) go to the top level.
            val extra = notesDir.listFiles { f -> f.extension == "json" }.orEmpty()
                .filter { it.nameWithoutExtension !in known }
                .mapNotNull { f -> readNote(f)?.let { metaFor(it, null) } }
            val merged = stored.copy(notes = present + extra)
            if (merged != stored || !indexFile.exists()) writeIndex(merged)
            merged
        }
        _state.value = index
        loaded = true
    }

    // ------------------------------------------------------------------ notes

    suspend fun load(id: String): Note? = noteLock.withLock {
        withContext(Dispatchers.IO) { readNote(noteFile(id)) }
    }

    suspend fun createNote(title: String, folderId: String?, pages: List<Page>): Note {
        val now = System.currentTimeMillis()
        val note = Note(id = newId(), title = title, created = now, modified = now, pages = pages)
        writeNote(note)
        mutate { it.copy(notes = it.notes + metaFor(note, null, folderId).copy(opened = now)) }
        return note
    }

    suspend fun save(note: Note) {
        writeNote(note)
        mutate { idx ->
            val old = idx.notes.find { it.id == note.id }
            if (old == null) idx.copy(notes = idx.notes + metaFor(note, null))
            else idx.copy(notes = idx.notes.map { if (it.id == note.id) metaFor(note, old) else it })
        }
    }

    suspend fun markOpened(id: String) = updateMeta(id) { it.copy(opened = System.currentTimeMillis()) }

    suspend fun setLastPage(id: String, page: Int) {
        if (_state.value.notes.find { it.id == id }?.lastPage == page) return
        updateMeta(id) { it.copy(lastPage = page) }
    }

    suspend fun renameNote(id: String, title: String) {
        updateMeta(id) { it.copy(title = title) }
        noteLock.withLock {
            withContext(Dispatchers.IO) {
                readNote(noteFile(id))?.let { atomicWrite(noteFile(id), NoteJson.encode(it.copy(title = title))) }
            }
        }
    }

    suspend fun moveNote(id: String, folderId: String?) = updateMeta(id) { it.copy(folderId = folderId) }

    suspend fun trashNote(id: String) = updateMeta(id) { it.copy(trashedAt = System.currentTimeMillis()) }

    suspend fun restoreNote(id: String) = mutate { LibraryOps.restore(it, id) }

    suspend fun deleteForever(ids: Set<String>) {
        if (ids.isEmpty()) return
        var unused = emptySet<String>()
        mutate { idx ->
            unused = LibraryOps.unusedAssets(idx, ids)
            idx.copy(notes = idx.notes.filterNot { it.id in ids })
        }
        withContext(Dispatchers.IO) {
            ids.forEach { noteFile(it).delete(); thumbFile(it).delete() }
            unused.forEach { assetFile(it).delete() }
        }
    }

    suspend fun emptyTrash() = deleteForever(LibraryOps.trash(_state.value).map { it.id }.toSet())

    // ------------------------------------------------------------------ folders

    suspend fun createFolder(name: String, parentId: String?): Folder {
        val folder = Folder(newId(), name, parentId, System.currentTimeMillis())
        mutate { it.copy(folders = it.folders + folder) }
        return folder
    }

    suspend fun renameFolder(id: String, name: String) =
        mutate { idx -> idx.copy(folders = idx.folders.map { if (it.id == id) it.copy(name = name) else it }) }

    suspend fun moveFolder(id: String, parentId: String?) = mutate { idx ->
        if (!LibraryOps.canMoveFolder(idx, id, parentId)) idx
        else idx.copy(folders = idx.folders.map { if (it.id == id) it.copy(parentId = parentId) else it })
    }

    suspend fun deleteFolder(id: String) = mutate { LibraryOps.deleteFolder(it, id, System.currentTimeMillis()) }

    // ------------------------------------------------------------------ assets & thumbnails

    /** Copies a picked file into app storage and returns its asset id. */
    suspend fun importAsset(uri: Uri, ext: String): String = withContext(Dispatchers.IO) {
        val id = "${newId()}.$ext"
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open the file.")
        input.use { src -> assetFile(id).outputStream().use { src.copyTo(it) } }
        id
    }

    fun assetFile(id: String) = File(assetsDir, id)

    fun deleteAsset(id: String) {
        assetFile(id).delete()
    }

    fun thumbFile(id: String) = File(thumbsDir, "$id.png")

    suspend fun writeThumb(id: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val target = thumbFile(id)
        val tmp = File(thumbsDir, "$id.png.tmp")
        FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }

    // ------------------------------------------------------------------ internals

    private fun noteFile(id: String) = File(notesDir, "$id.json")

    private fun readNote(f: File): Note? = runCatching { NoteJson.decode(f.readText()) }.getOrNull()

    private suspend fun writeNote(note: Note) = noteLock.withLock {
        withContext(Dispatchers.IO) { atomicWrite(noteFile(note.id), NoteJson.encode(note)) }
    }

    private fun metaFor(n: Note, old: NoteMeta?, folderId: String? = old?.folderId) = NoteMeta(
        id = n.id,
        title = n.title,
        folderId = folderId,
        created = n.created,
        modified = n.modified,
        opened = old?.opened ?: 0,
        trashedAt = old?.trashedAt,
        pageCount = n.pages.size,
        lastPage = (old?.lastPage ?: 0).coerceIn(0, (n.pages.size - 1).coerceAtLeast(0)),
        assets = n.assetIds,
    )

    private suspend fun updateMeta(id: String, block: (NoteMeta) -> NoteMeta) =
        mutate { idx -> idx.copy(notes = idx.notes.map { if (it.id == id) block(it) else it }) }

    private suspend fun mutate(block: (LibraryIndex) -> LibraryIndex) = indexLock.withLock {
        val next = block(_state.value)
        _state.value = next
        withContext(Dispatchers.IO) { writeIndex(next) }
    }

    private fun writeIndex(index: LibraryIndex) = atomicWrite(indexFile, NoteJson.encodeIndex(index))

    /** Write to a temp file, then rename, so a crash mid-write can't corrupt anything. */
    private fun atomicWrite(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}
