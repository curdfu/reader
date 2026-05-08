package com.htmake.reader

import com.htmake.reader.api.controller.resolveUploadAssetFileName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UploadAssetFileNameTests {

    @Test
    fun keepsOriginalNameForNonFontUploads() {
        val fileName = resolveUploadAssetFileName("background", "cover.jpg") { true }

        assertEquals("cover.jpg", fileName)
    }

    @Test
    fun keepsOriginalNameForNewFontUploads() {
        val fileName = resolveUploadAssetFileName("fonts", "reader.ttf") { false }

        assertEquals("reader.ttf", fileName)
    }

    @Test
    fun createsUniqueNameForExistingFontUploads() {
        val fileName = resolveUploadAssetFileName("fonts", "reader.ttf") {
            it == "reader.ttf"
        }

        assertNotEquals("reader.ttf", fileName)
        assertTrue(fileName.startsWith("reader-"))
        assertTrue(fileName.endsWith(".ttf"))
    }
}
