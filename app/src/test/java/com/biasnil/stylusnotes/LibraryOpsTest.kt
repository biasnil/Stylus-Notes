package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.data.Folder
import com.biasnil.stylusnotes.data.LibraryIndex
import com.biasnil.stylusnotes.data.LibraryOps
import com.biasnil.stylusnotes.data.NoteMeta
import com.biasnil.stylusnotes.data.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOpsTest {

    private fun folder(id: String, name: String, parent: String? = null, created: Long = 0) = Folder(id, name, parent, created)
    private fun note(id: String, title: String, folder: String? = null, modified: Long = 0, opened: Long = 0, trashed: Long? = null, assets: List<String> = emptyList()) =
        NoteMeta(id, title, folder, created = 0, modified = modified, opened = opened, trashedAt = trashed, assets = assets)

    // School > Physics > Labs, and Personal
    private val index = LibraryIndex(
        folders = listOf(
            folder("school", "School"),
            folder("physics", "Physics", "school"),
            folder("labs", "Labs", "physics"),
            folder("personal", "personal"),
        ),
        notes = listOf(
            note("n1", "Waveguides", "physics", modified = 30),
            note("n2", "", "physics", modified = 50),
            note("n3", "Lab 3", "labs", modified = 10, assets = listOf("a.pdf", "shared.png")),
            note("n4", "Groceries", null, modified = 20, opened = 99, assets = listOf("shared.png")),
            note("n5", "Old", null, modified = 5, trashed = 100),
        ),
    )

    @Test fun childFoldersSortedByNameIgnoringCase() {
        assertEquals(listOf("personal", "School"), LibraryOps.childFolders(index, null, SortMode.NAME).map { it.name })
        assertEquals(listOf("Physics"), LibraryOps.childFolders(index, "school", SortMode.NAME).map { it.name })
    }

    @Test fun notesInFolderExcludeTrashAndSort() {
        assertEquals(listOf("n2", "n1"), LibraryOps.notesIn(index, "physics", SortMode.MODIFIED).map { it.id })
        // A note with no title sorts as "Untitled".
        assertEquals(listOf("n2", "n1"), LibraryOps.notesIn(index, "physics", SortMode.NAME).map { it.id })
        assertEquals(listOf("n4"), LibraryOps.notesIn(index, null, SortMode.NAME).map { it.id })
    }

    @Test fun recentUsesOpenedOrModifiedAndSkipsTrash() {
        assertEquals(listOf("n4", "n2", "n1", "n3"), LibraryOps.recent(index).map { it.id })
    }

    @Test fun trashList() = assertEquals(listOf("n5"), LibraryOps.trash(index).map { it.id })

    @Test fun pathFromTopToFolder() {
        assertEquals(listOf("School", "Physics", "Labs"), LibraryOps.path(index, "labs").map { it.name })
        assertTrue(LibraryOps.path(index, null).isEmpty())
    }

    @Test fun pathSurvivesACycle() {
        val broken = index.copy(folders = listOf(folder("a", "A", "b"), folder("b", "B", "a")))
        assertEquals(2, LibraryOps.path(broken, "a").size)
    }

    @Test fun descendantsIncludeSelfAndChildren() {
        assertEquals(setOf("school", "physics", "labs"), LibraryOps.descendants(index, "school"))
        assertEquals(setOf("labs"), LibraryOps.descendants(index, "labs"))
    }

    @Test fun folderCannotMoveIntoItself() {
        assertFalse(LibraryOps.canMoveFolder(index, "school", "labs"))
        assertFalse(LibraryOps.canMoveFolder(index, "school", "school"))
        assertTrue(LibraryOps.canMoveFolder(index, "labs", "personal"))
        assertTrue(LibraryOps.canMoveFolder(index, "labs", null))
    }

    @Test fun deletingFolderTrashesNotesInsideRecursively() {
        val after = LibraryOps.deleteFolder(index, "physics", now = 500)
        assertEquals(listOf("school", "personal"), after.folders.map { it.id })
        assertEquals(500L, after.notes.first { it.id == "n1" }.trashedAt)
        assertEquals(500L, after.notes.first { it.id == "n3" }.trashedAt)
        assertNull(after.notes.first { it.id == "n4" }.trashedAt)
        assertEquals(100L, after.notes.first { it.id == "n5" }.trashedAt) // already trashed, untouched
    }

    @Test fun restoreGoesToTopLevelWhenFolderIsGone() {
        val deleted = LibraryOps.deleteFolder(index, "physics", now = 500)
        val restored = LibraryOps.restore(deleted, "n1")
        val n1 = restored.notes.first { it.id == "n1" }
        assertNull(n1.trashedAt)
        assertNull(n1.folderId)
    }

    @Test fun restoreKeepsFolderWhenItExists() {
        val trashed = index.copy(notes = index.notes.map { if (it.id == "n1") it.copy(trashedAt = 1) else it })
        assertEquals("physics", LibraryOps.restore(trashed, "n1").notes.first { it.id == "n1" }.folderId)
    }

    @Test fun sharedAssetsAreKept() {
        assertEquals(setOf("a.pdf"), LibraryOps.unusedAssets(index, setOf("n3")))
        assertEquals(setOf("a.pdf", "shared.png"), LibraryOps.unusedAssets(index, setOf("n3", "n4")))
    }

    @Test fun folderTreeIsDepthFirstAndRespectsExclude() {
        val tree = LibraryOps.folderTree(index)
        assertEquals(listOf("personal" to 0, "School" to 0, "Physics" to 1, "Labs" to 2), tree.map { it.first.name to it.second })
        val excluded = LibraryOps.folderTree(index, LibraryOps.descendants(index, "physics"))
        assertEquals(listOf("personal", "School"), excluded.map { it.first.name })
    }
}
