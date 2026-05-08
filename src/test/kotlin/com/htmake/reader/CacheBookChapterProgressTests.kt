package com.htmake.reader

import com.htmake.reader.api.controller.cacheChapterWithAccounting
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CacheBookChapterProgressTests {

    @Test
    fun chapterFailureDoesNotBlockLaterSuccesses() = runBlocking {
        val cachedChapterContentSet = ConcurrentHashMap.newKeySet<Int>()
        val successCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)
        val executedChapters = mutableListOf<Int>()

        listOf(0, 1, 2).forEach { chapterIndex ->
            cacheChapterWithAccounting(
                chapterIndex,
                cachedChapterContentSet,
                successCount,
                failedCount
            ) {
                executedChapters.add(chapterIndex)
                if (chapterIndex == 1) {
                    throw IllegalStateException("chapter cache failed")
                }
            }
        }

        assertEquals(listOf(0, 1, 2), executedChapters)
        assertEquals(setOf(0, 2), cachedChapterContentSet)
        assertEquals(2, successCount.get())
        assertEquals(1, failedCount.get())
    }

    @Test
    fun cachedChapterIsSkippedWithoutChangingCounters() = runBlocking {
        val cachedChapterContentSet = ConcurrentHashMap.newKeySet<Int>().apply {
            add(3)
        }
        val successCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)
        var executed = false

        cacheChapterWithAccounting(3, cachedChapterContentSet, successCount, failedCount) {
            executed = true
        }

        assertTrue(cachedChapterContentSet.contains(3))
        assertEquals(0, successCount.get())
        assertEquals(0, failedCount.get())
        assertEquals(false, executed)
    }
}
