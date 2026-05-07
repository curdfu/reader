package com.htmake.reader.api.response

data class BookSourceImportReport(
    val imported: Int = 0,
    val skipped: Int = 0,
    val skippedByType: Map<String, Int> = emptyMap(),
    val skippedSources: List<SkippedBookSource> = emptyList()
)

data class SkippedBookSource(
    val name: String?,
    val url: String?,
    val type: Int?,
    val reason: String
)
