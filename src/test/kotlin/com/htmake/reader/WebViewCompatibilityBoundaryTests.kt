package com.htmake.reader

import io.legado.app.data.entities.BaseSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.JsExtensions
import io.legado.app.model.analyzeRule.AnalyzeUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class WebViewCompatibilityBoundaryTests {

    @Test
    fun urlOptionWebViewFailsClearly() {
        assertWebViewUnsupported {
            AnalyzeUrl("https://example.invalid/search,{\"webView\":true}").getStrResponse()
        }
    }

    @Test
    fun javaWebViewFailsClearly() {
        val jsExtensions = object : JsExtensions {
            override fun getSource(): BaseSource? = null
        }

        assertWebViewUnsupported {
            jsExtensions.webView(null, "https://example.invalid", null)
        }
    }

    private fun assertWebViewUnsupported(block: () -> Unit) {
        try {
            block()
            fail("Expected WebView unsupported exception")
        } catch (e: NoStackTraceException) {
            assertEquals(AnalyzeUrl.WEB_VIEW_UNSUPPORTED_MSG, e.message)
        }
    }
}
