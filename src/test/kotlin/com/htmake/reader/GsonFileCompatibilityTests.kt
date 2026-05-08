package com.htmake.reader

import io.legado.app.utils.GSON
import com.htmake.reader.utils.gson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class GsonFileCompatibilityTests {

    @Test
    fun legadoGsonSerializesFileAsPathString() {
        val file = File("storage/cache/test.txt")
        val json = GSON.toJson(file)

        assertEquals(file.path, GSON.fromJson(json, String::class.java))
        assertEquals(file.path, GSON.fromJson(json, File::class.java).path)
    }

    @Test
    fun apiGsonSerializesFileAsPathString() {
        val file = File("storage/cache/test.txt")
        val json = gson.toJson(file)

        assertEquals(file.path, gson.fromJson(json, String::class.java))
        assertEquals(file.path, gson.fromJson(json, File::class.java).path)
    }
}
