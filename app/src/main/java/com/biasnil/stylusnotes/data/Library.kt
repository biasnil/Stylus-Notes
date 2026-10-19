package com.biasnil.stylusnotes.data

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val created: Long,
)

/**
 * What the library screen needs to know about a note without opening it.
 * The library index is the only place folder and trash state live.
 */
@Serializable
data class NoteMeta(
    val id: String,
    val title: String = "",
    val folderId: String? = null,
    val created: Long,
    val modified: Long,
    val opened: Long = 0,
    val trashedAt: Long? = null,
    val pageCount: Int = 1,
    /** Page that was open when the note was last closed. */
    val lastPage: Int = 0,
    /** Asset files (PDFs, images) this note uses, so they can be cleaned up when it's deleted. */
    val assets: List<String> = emptyList(),
) {
    val displayTitle: String get() = title.ifBlank { "Untitled" }
}

@Serializable
data class LibraryIndex(
    val folders: List<Folder> = emptyList(),
    val notes: List<NoteMeta> = emptyList(),
)

enum class SortMode { NAME, MODIFIED }

/** Pure library logic, kept free of Android so it runs in plain JVM tests. */
object LibraryOps {

    fun childFolders(index: LibraryIndex, parentId: String?, sort: SortMode): List<Folder> {
        val list = index.folders.filter { it.parentId == parentId }
        return when (sort) {
            SortMode.NAME -> list.sortedBy { it.name.lowercase() }
            SortMode.MODIFIED -> list.sortedByDescending { it.created }
        }
    }

    fun notesIn(index: LibraryIndex, folderId: String?, sort: SortMode): List<NoteMeta> =
        sortNotes(index.notes.filter { it.trashedAt == null && it.folderId == folderId }, sort)

    fun sortNotes(list: List<NoteMeta>, sort: SortMode): List<NoteMeta> = when (sort) {
        SortMode.NAME -> list.sortedWith(compareBy<NoteMeta> { it.displayTitle.lowercase() }.thenByDescending { it.modified })
        SortMode.MODIFIED -> list.sortedByDescending { it.modified }
    }

    fun recent(index: LibraryIndex, limit: Int = 30): List<NoteMeta> =
        index.notes.filter { it.trashedAt == null }
            .sortedByDescending { maxOf(it.opened, it.modified) }
            .take(limit)

    fun trash(index: LibraryIndex): List<NoteMeta> =
        index.notes.filter { it.trashedAt != null }.sortedByDescending { it.trashedAt }

    /** Folders from the top level down to [folderId]. Empty for the top level. */
    fun path(index: LibraryIndex, folderId: String?): List<Folder> {
        val byId = index.folders.associateBy { it.id }
        val out = ArrayList<Folder>()
        val seen = HashSet<String>()
        var cur = folderId?.let { byId[it] }
        while (cur != null && seen.add(cur.id)) {
            out.add(0, cur)
            cur = cur.parentId?.let { byId[it] }
        }
        return out
    }

    /** The folder itself plus everything inside it. */
    fun descendants(index: LibraryIndex, folderId: String): Set<String> {
        val children = index.folders.groupBy { it.parentId }
        val out = LinkedHashSet<String>()
        val queue = ArrayDeque(listOf(folderId))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!out.add(id)) continue
            children[id]?.forEach { queue.addLast(it.id) }
        }
        return out
    }

    fun canMoveFolder(index: LibraryIndex, folderId: String, newParent: String?): Boolean =
        newParent == null || newParent !in descendants(index, folderId)

    /** Removes the folder and its subfolders. Notes inside go to the trash. */
    fun deleteFolder(index: LibraryIndex, folderId: String, now: Long): LibraryIndex {
        val gone = descendants(index, folderId)
        return index.copy(
            folders = index.folders.filterNot { it.id in gone },
            notes = index.notes.map {
                if (it.trashedAt == null && it.folderId != null && it.folderId in gone) it.copy(trashedAt = now) else it
            },
        )
    }

    /** Takes a note out of the trash. If its folder was deleted, it goes to the top level. */
    fun restore(index: LibraryIndex, noteId: String): LibraryIndex {
        val folderIds = index.folders.map { it.id }.toSet()
        return index.copy(notes = index.notes.map {
            if (it.id != noteId) it
            else it.copy(trashedAt = null, folderId = it.folderId?.takeIf { f -> f in folderIds })
        })
    }

    /** Asset files used only by the notes being removed. */
    fun unusedAssets(index: LibraryIndex, removing: Set<String>): Set<String> {
        val removed = index.notes.filter { it.id in removing }.flatMap { it.assets }.toSet()
        val kept = index.notes.filter { it.id !in removing }.flatMap { it.assets }.toSet()
        return removed - kept
    }

    /** Every folder with its depth, in tree order, for "Move to" pickers. */
    fun folderTree(index: LibraryIndex, exclude: Set<String> = emptySet()): List<Pair<Folder, Int>> {
        val children = index.folders.groupBy { it.parentId }
        val out = ArrayList<Pair<Folder, Int>>()
        fun walk(parent: String?, depth: Int) {
            children[parent].orEmpty().sortedBy { it.name.lowercase() }.forEach { f ->
                if (f.id in exclude) return@forEach
                out += f to depth
                walk(f.id, depth + 1)
            }
        }
        walk(null, 0)
        return out
    }
}
