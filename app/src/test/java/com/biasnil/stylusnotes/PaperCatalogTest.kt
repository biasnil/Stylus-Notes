package com.biasnil.stylusnotes

import com.biasnil.stylusnotes.paper.PaperCatalog
import com.biasnil.stylusnotes.paper.PaperCategory
import com.biasnil.stylusnotes.paper.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperCatalogTest {

    @Test fun has69Backgrounds() = assertEquals(69, PaperCatalog.all.size)

    @Test fun idsAreUnique() = assertEquals(PaperCatalog.all.size, PaperCatalog.all.map { it.id }.toSet().size)

    @Test fun countsPerCategory() {
        val counts = PaperCategory.entries.associateWith { PaperCatalog.inCategory(it).size }
        assertEquals(8, counts[PaperCategory.STANDARD])
        assertEquals(13, counts[PaperCategory.GRID])
        assertEquals(13, counts[PaperCategory.MATH])
        assertEquals(5, counts[PaperCategory.MUSIC])
        assertEquals(18, counts[PaperCategory.SPORTS])
        assertEquals(12, counts[PaperCategory.PLANNERS])
    }

    @Test fun sportsEndWithPickleball() =
        assertEquals("Pickleball court", PaperCatalog.inCategory(PaperCategory.SPORTS).last().name)

    @Test fun unknownIdFallsBackToBlank() = assertEquals("blank", PaperCatalog.find("nope").id)

    @Test fun defaultExists() = assertTrue(PaperCatalog.contains(PaperCatalog.DEFAULT_ID))

    @Test fun sportsPagesAreLandscape() {
        val (w, h) = PaperCatalog.pageSize(PaperCatalog.find("tennis"), PaperSize.LETTER)
        assertEquals(792f, w)
        assertEquals(612f, h)
        val (pw, ph) = PaperCatalog.pageSize(PaperCatalog.find("college"), PaperSize.A4)
        assertTrue(ph > pw)
    }
}
