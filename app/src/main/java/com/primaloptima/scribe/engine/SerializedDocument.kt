package com.primaloptima.scribe.engine

import kotlinx.serialization.Serializable

@Serializable
data class SerializedDocument(
    val version: Int = 2,
    val plainText: String,
    val spans: List<SerializedSpan> = emptyList()
)

@Serializable
data class SerializedSpan(
    val type: String,
    val start: Int,
    val end: Int
)

fun List<FormatSpan>.toSerialized(): List<SerializedSpan> = map {
    SerializedSpan(it.type.name, it.start, it.end)
}

fun List<SerializedSpan>.toFormatSpans(): List<FormatSpan> = mapNotNull {
    runCatching { FormatSpan(FormatType.valueOf(it.type), it.start, it.end) }.getOrNull()
}