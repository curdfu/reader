package com.htmake.reader

import com.sun.net.httpserver.HttpServer
import io.legado.app.model.analyzeRule.AnalyzeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class SearchPostCharsetCompatibilityTests {

    @Test
    fun gb2312PostSearchOptionSendsEncodedBodyAndHeaders() {
        val captured = CapturedRequest()
        val executor = Executors.newSingleThreadExecutor()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/search.asp") { exchange ->
            captured.method = exchange.requestMethod
            captured.body = exchange.requestBody.readBytes().toString(StandardCharsets.ISO_8859_1)
            captured.testHeader = exchange.requestHeaders.getFirst("X-Reader-Test")

            val response = "ok".toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }

        try {
            server.start()
            val port = server.address.port
            AnalyzeUrl(
                mUrl = "/search.asp,{\"charset\":\"GB2312\",\"method\":\"POST\",\"headers\":{\"X-Reader-Test\":\"ok\"},\"body\":\"word={{key}}&m=2&ChannelID=0&page={{page}}\"}",
                key = "测试",
                page = 3,
                baseUrl = "http://127.0.0.1:$port"
            ).getStrResponse()

            val expectedKeyword = URLEncoder.encode("测试", "GB2312")
            assertEquals("POST", captured.method)
            assertEquals("ok", captured.testHeader)
            assertTrue(captured.body.contains("word=$expectedKeyword"))
            assertTrue(captured.body.contains("page=3"))
            assertTrue(captured.body.contains("m=2"))
            assertTrue(captured.body.contains("ChannelID=0"))
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    private class CapturedRequest {
        var method: String? = null
        var body: String = ""
        var testHeader: String? = null
    }
}
