package com.htmake.reader.utils

object BookSourceTypeFilter {
    fun isSupportedTextType(type: Int?): Boolean = type == null || type == 0

    fun reason(type: Int?): String = when (type) {
        1 -> "audio source unsupported"
        2 -> "image source unsupported"
        3 -> "file source unsupported"
        else -> "unknown source type unsupported"
    }

    fun bucket(type: Int?): String = when (type) {
        1 -> "audio"
        2 -> "image"
        3 -> "file"
        else -> "unknown"
    }
}
