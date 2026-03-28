package com.openlinker

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths

object OpenLinkerResolvedUrl {
    private val schemeOnlyPattern = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:(//?)?$")
    private val fileSchemePattern = Regex("^file://", RegexOption.IGNORE_CASE)
    private val windowsDrivePattern = Regex("^[A-Za-z]:([/\\\\].*)?$")

    fun canOpen(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.isNotBlank() && !schemeOnlyPattern.matches(trimmed)
    }

    fun toLocalPath(url: String): Path? {
        val trimmed = url.trim()
        if (!fileSchemePattern.containsMatchIn(trimmed)) {
            return null
        }

        parseFileUri(trimmed)?.let { return it }

        val rawPath = fileSchemePattern.replace(trimmed, "")
        if (rawPath.isBlank()) {
            return null
        }

        val decodedPath = runCatching {
            URLDecoder.decode(rawPath, StandardCharsets.UTF_8)
        }.getOrDefault(rawPath)

        val normalizedPath = when {
            decodedPath.startsWith("/") -> decodedPath
            windowsDrivePattern.matches(decodedPath) -> decodedPath
            else -> return null
        }

        return runCatching { Paths.get(normalizedPath) }.getOrNull()
    }

    private fun parseFileUri(url: String): Path? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!uri.scheme.equals("file", ignoreCase = true)) {
            return null
        }
        return runCatching { Paths.get(uri) }.getOrNull()
    }
}
