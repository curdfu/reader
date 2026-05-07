package com.htmake.reader

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.model.webBook.BookChapterList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterUrlOptionCompatibilityTests {

    @Test
    fun tocChapterUrlKeepsUrlOptionThroughAbsoluteUrl() = runBlocking {
        val html = """
            <ul>
                <li class="chapter"><a href="chapter-1.html">第一章</a></li>
            </ul>
        """.trimIndent()
        val book = Book(
            bookUrl = "http://example.test/book/1",
            tocUrl = "http://example.test/book/toc.html"
        )
        val bookSource = BookSource(
            bookSourceUrl = "http://example.test",
            ruleToc = TocRule(
                chapterList = ".chapter",
                chapterName = "a@text",
                chapterUrl = "a@href@js:result+',{\"webView\":true}'"
            )
        )

        val chapters = BookChapterList.analyzeChapterList(
            book = book,
            body = html,
            bookSource = bookSource,
            baseUrl = book.tocUrl,
            redirectUrl = book.tocUrl
        )

        assertEquals(1, chapters.size)
        val chapter = chapters.single()
        assertTrue(chapter.url.contains("\"webView\":true"))
        assertEquals(
            "http://example.test/book/chapter-1.html,{\"webView\":true}",
            chapter.getAbsoluteURL()
        )
    }

    @Test
    fun bookChapterAbsoluteUrlKeepsUrlOption() {
        val chapter = BookChapter(
            url = "chapter-2.html,{\"webView\":true}",
            baseUrl = "http://example.test/book/toc.html"
        )

        assertEquals(
            "http://example.test/book/chapter-2.html,{\"webView\":true}",
            chapter.getAbsoluteURL()
        )
    }
}
