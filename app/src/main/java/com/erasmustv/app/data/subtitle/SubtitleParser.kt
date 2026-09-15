package com.erasmustv.app.data.subtitle

import java.util.regex.Pattern

object SubtitleParser {

    private val TIMESTAMP_PATTERN = Pattern.compile(
        """(?:(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[,.](\d{2,3}))\s*-->\s*(?:(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[,.](\d{2,3}))"""
    )

    private val HTML_TAG_PATTERN = Pattern.compile("""<[^>]*>|\{[^}]*\}""")
    private val ASS_DRAWING_PATTERN = Regex("""\{\\p[1-9]\}.*?(\{\\p0\}|$)""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()

        val normalized = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trimStart('\uFEFF') // Strip UTF-8 BOM if present

        // Guard against HTML error pages or connection failure responses
        val firstLines = normalized.lineSequence().take(5).joinToString(" ").lowercase()
        if (firstLines.contains("<!doctype") || firstLines.contains("<html") ||
            firstLines.contains("problem with network connection") || firstLines.contains("502 bad gateway")
        ) {
            return emptyList()
        }

        // Format detection: SSA / ASS format
        if (normalized.contains("[Script Info]", ignoreCase = true) ||
            normalized.contains("[Events]", ignoreCase = true) ||
            normalized.contains("[V4+ Styles]", ignoreCase = true) ||
            normalized.contains("[V4 Styles]", ignoreCase = true)
        ) {
            val assCues = parseAss(normalized)
            if (assCues.isNotEmpty()) return assCues
        }

        return parseSrtOrVtt(normalized)
    }

    private fun parseAss(content: String): List<SubtitleCue> {
        val lines = content.split("\n")
        val cues = mutableListOf<SubtitleCue>()
        var inEvents = false
        var formatColumns: List<String>? = null
        var startIndex = 1
        var endIndex = 2
        var textIndex = 9
        var cueIndex = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(";")) continue

            if (trimmed.equals("[Events]", ignoreCase = true)) {
                inEvents = true
                continue
            } else if (trimmed.startsWith("[") && inEvents) {
                // Another section header encountered after [Events]
                inEvents = false
                continue
            }

            if (!inEvents) continue

            if (trimmed.startsWith("Format:", ignoreCase = true)) {
                val cols = trimmed.substring(7).split(",").map { it.trim().lowercase() }
                formatColumns = cols
                val sIdx = cols.indexOf("start")
                val eIdx = cols.indexOf("end")
                val tIdx = cols.indexOf("text")
                if (sIdx >= 0) startIndex = sIdx
                if (eIdx >= 0) endIndex = eIdx
                if (tIdx >= 0) textIndex = tIdx
                continue
            }

            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val afterPrefix = trimmed.substring(9).trim()
                val totalColumns = maxOf((formatColumns?.size ?: 10), textIndex + 1)
                val parts = afterPrefix.split(",", limit = totalColumns).map { it.trim() }
                if (parts.size <= maxOf(startIndex, endIndex)) continue

                val startMs = parseAssTimestamp(parts[startIndex]) ?: continue
                val endMs = parseAssTimestamp(parts[endIndex]) ?: continue
                if (endMs <= startMs) continue

                val rawText = if (parts.size > textIndex) parts[textIndex] else parts.last()
                val cleanText = sanitizeAssText(rawText)
                if (cleanText.isNotBlank()) {
                    cues.add(
                        SubtitleCue(
                            index = cueIndex++,
                            startMs = startMs,
                            endMs = endMs,
                            text = cleanText
                        )
                    )
                }
            }
        }

        cues.sortBy { it.startMs }
        return cues
    }

    private fun sanitizeAssText(raw: String): String {
        // Strip vector drawing mode blocks like {\p1}...{\p0}
        var text = ASS_DRAWING_PATTERN.replace(raw, "")
        // Handle line break tokens in ASS
        text = text.replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ")
        // Strip ASS style overrides like {\an8}, {\b1}, {\pos(...)}, {\c&H...&}
        text = text.replace(Regex("""\{[^}]*\}"""), "")
        // Strip any HTML markup
        text = text.replace(Regex("""<[^>]*>"""), "")
        return text.trim()
    }

    private fun parseAssTimestamp(ts: String): Long? {
        val parts = ts.trim().split(":")
        if (parts.size != 3) return null
        val h = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull() ?: return null
        val secAndFrac = parts[2].split(".", ",")
        val s = secAndFrac[0].toLongOrNull() ?: return null
        val fracStr = if (secAndFrac.size > 1) secAndFrac[1] else "0"
        val ms = when (fracStr.length) {
            1 -> (fracStr.toLongOrNull() ?: 0L) * 100
            2 -> (fracStr.toLongOrNull() ?: 0L) * 10
            3 -> fracStr.toLongOrNull() ?: 0L
            else -> fracStr.take(3).toLongOrNull() ?: 0L
        }
        return (h * 3600 + m * 60 + s) * 1000 + ms
    }

    private fun parseSrtOrVtt(content: String): List<SubtitleCue> {
        val lines = content.split("\n")
        val cues = mutableListOf<SubtitleCue>()

        var cueIndex = 0
        var currentStartMs = -1L
        var currentEndMs = -1L
        val currentTextLines = mutableListOf<String>()
        var inStyleOrRegion = false

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

            // Skip WebVTT header and comments
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE")) {
                continue
            }

            // Skip STYLE or REGION blocks
            if (trimmed.startsWith("STYLE", ignoreCase = true) || trimmed.startsWith("REGION", ignoreCase = true)) {
                inStyleOrRegion = true
                continue
            }
            if (inStyleOrRegion) {
                if (trimmed.isEmpty()) {
                    inStyleOrRegion = false
                }
                continue
            }

            if (trimmed.isEmpty()) {
                commitCurrentCue()
                continue
            }

            val matcher = TIMESTAMP_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                commitCurrentCue()

                val startH = matcher.group(1)?.toLongOrNull() ?: 0L
                val startM = matcher.group(2)?.toLongOrNull() ?: 0L
                val startS = matcher.group(3)?.toLongOrNull() ?: 0L
                val startMsRaw = matcher.group(4) ?: "0"
                val startMs = if (startMsRaw.length == 2) (startMsRaw.toLongOrNull() ?: 0L) * 10 else (startMsRaw.toLongOrNull() ?: 0L)
                currentStartMs = (startH * 3600 + startM * 60 + startS) * 1000 + startMs

                val endH = matcher.group(5)?.toLongOrNull() ?: 0L
                val endM = matcher.group(6)?.toLongOrNull() ?: 0L
                val endS = matcher.group(7)?.toLongOrNull() ?: 0L
                val endMsRaw = matcher.group(8) ?: "0"
                val endMs = if (endMsRaw.length == 2) (endMsRaw.toLongOrNull() ?: 0L) * 10 else (endMsRaw.toLongOrNull() ?: 0L)
                currentEndMs = (endH * 3600 + endM * 60 + endS) * 1000 + endMs
            } else {
                // If it's just a sequence number or cue id before a timestamp line, ignore if currentStartMs == -1
                if (currentStartMs == -1L && (trimmed.all { it.isDigit() } || trimmed.startsWith("cue-", ignoreCase = true))) {
                    continue
                }
                // Text line for current cue
                if (currentStartMs >= 0) {
                    currentTextLines.add(trimmed)
                }
            }
        }

        // Commit trailing cue
        commitCurrentCue()

        return cues
    }

    fun parseTimestampToMs(h: Long, m: Long, s: Long, ms: Long): Long {
        return (h * 3600 + m * 60 + s) * 1000 + ms
    }
}

