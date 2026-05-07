package com.htmake.reader

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.model.webBook.BookContent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookContentRuleCompatibilityTests {

    @Test
    fun contentRuleExtractsParagraphTextAndAppliesReplaceRegex() = runBlocking {
        val content = analyzeContent(
            html = """
                <div id="content">
                    <p>第一段广告</p>
                    <p>第二段正文</p>
                </div>
            """.trimIndent(),
            rule = ContentRule(
                content = "#content@p@text",
                replaceRegex = "##广告##"
            )
        )

        assertTrue(content.contains("第一段"))
        assertTrue(content.contains("第二段正文"))
        assertFalse(content.contains("广告"))
    }

    @Test
    fun contentRuleSupportsAtJsPostProcessing() = runBlocking {
        val content = analyzeContent(
            html = """
                <div id="content">
                    <p>正文</p>
                </div>
            """.trimIndent(),
            rule = ContentRule(
                content = "#content@text@js:result+'追加'"
            )
        )

        assertTrue(content.contains("正文追加"))
    }

    @Test
    fun contentRuleSupportsBlockJsPostProcessing() = runBlocking {
        val content = analyzeContent(
            html = """
                <div id="content">
                    <p>正文</p>
                </div>
            """.trimIndent(),
            rule = ContentRule(
                content = "#content@text<js>result+'块追加'</js>"
            )
        )

        assertTrue(content.contains("正文块追加"))
    }

    private suspend fun analyzeContent(html: String, rule: ContentRule): String {
        val baseUrl = "http://example.test/book/chapter-1.html"
        val book = Book(
            bookUrl = "http://example.test/book/1",
            tocUrl = "http://example.test/book/toc.html",
            origin = "http://example.test",
            name = "测试书籍"
        )
        val chapter = BookChapter(
            url = baseUrl,
            title = "第一章",
            baseUrl = book.tocUrl,
            bookUrl = book.bookUrl,
            index = 0
        )
        val source = BookSource(
            bookSourceName = "离线正文规则测试",
            bookSourceUrl = "http://example.test",
            ruleContent = rule
        )

        return BookContent.analyzeContent(
            body = html,
            book = book,
            bookChapter = chapter,
            bookSource = source,
            baseUrl = baseUrl,
            redirectUrl = baseUrl
        )
    }
}
