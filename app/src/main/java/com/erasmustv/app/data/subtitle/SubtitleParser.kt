package com.erasmustv.app.data.subtitle

import java.util.regex.Pattern

object SubtitleParser {

    private val TIMESTAMP_PATTERN = Pattern.compile(
        """(?:(\d{1,2}):)?(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(?:(\d{1,2}):)?(\d{2}):(\d{2})[,.](\d{3})"""
    )

    private val HTML_TAG_PATTERN = Pattern.compile("""<[^>]*>|\{[^}]*\}""")

    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()

        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trimStart('\uFEFF') // Strip UTF-8 BOM if present

        val lines = normalized.split("\n")
        val cues = mutableListOf<SubtitleCue>()

        var cueIndex = 0
        var currentStartMs = -1L
        var currentEndMs = -1L
        val currentTextLines = mutableListOf<String>()

        fun commitCurrentCue() {
            if (currentStartMs >= 0 && currentEndMs > currentStartMs && currentTextLines.isNotEmpty()) {
                val rawText = currentTextLines.joinToString("\n").trim()
                val cleanText = HTML_TAG_PATTERN.matcher(rawText).replaceAll("").trim()
                if (cleanText.isNotBlank()) {
                    cues.add(
                        SubtitleCue(
                            index = cueIndex++,
                            startMs = currentStartMs,
                            endMs = currentEndMs,
                            text = cleanText
                        )
                    )
                }
            }
            currentStartMs = -1L
            currentEndMs = -1L
            currentTextLines.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Skip WebVTT signature, comments and empty lines
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE") || trimmed.startsWith("STYLE")) {
                continue
            }

            if (trimmed.isEmpty()) {
                commitCurrentCue()
                continue
            }

            val matcher = TIMESTAMP_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                // We reached a new timestamp line: commit previous cue first
                commitCurrentCue()

                val startH = matcher.group(1)?.toLongOrNull() ?: 0L
                val startM = matcher.group(2)?.toLongOrNull() ?: 0L
                val startS = matcher.group(3)?.toLongOrNull() ?: 0L
                val startMs = matcher.group(4)?.toLongOrNull() ?: 0L
                currentStartMs = (startH * 3600 + startM * 60 + startS) * 1000 + startMs

                val endH = matcher.group(5)?.toLongOrNull() ?: 0L
                val endM = matcher.group(6)?.toLongOrNull() ?: 0L
                val endS = matcher.group(7)?.toLongOrNull() ?: 0L
                val endMs = matcher.group(8)?.toLongOrNull() ?: 0L
                currentEndMs = (endH * 3600 + endM * 60 + endS) * 1000 + endMs
            } else {
                // If it's just a sequence number before a timestamp line, ignore if currentStartMs == -1
                if (currentStartMs == -1L && trimmed.all { it.isDigit() }) {
                    continue
                }
                // Text line for current cue
                if (currentStartMs >= 0) {
                    currentTextLines.add(trimmed)
                }
            }
        }

        // Commit any trailing cue
        commitCurrentCue()

        return cues
    }

    fun parseTimestampToMs(h: Long, m: Long, s: Long, ms: Long): Long {
        return (h * 3600 + m * 60 + s) * 1000 + ms
    }
}
