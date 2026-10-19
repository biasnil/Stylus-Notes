package com.biasnil.stylusnotes.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun prefixAndMissingParts() {
        assertEquals(0, Versions.compare("v1.0", "1.0"))
        assertEquals(0, Versions.compare("1", "1.0.0"))
    }

    @Test
    fun numericNotAlphabetic() {
        assertTrue(Versions.isNewer("1.10", "1.9"))
        assertTrue(Versions.isNewer("v2.0", "1.99.9"))
        assertFalse(Versions.isNewer("1.0", "1.0.1"))
    }

    @Test
    fun suffixIsOlderThanRelease() {
        assertTrue(Versions.isNewer("1.1", "1.1-beta"))
        assertFalse(Versions.isNewer("v1.1-beta", "1.1"))
        assertTrue(Versions.isNewer("1.1-beta", "1.0"))
    }

    @Test
    fun junkTagsDoNotCrash() {
        assertFalse(Versions.isNewer("release", "1.0"))
        assertTrue(Versions.isNewer("1.0", "release"))
    }
}
