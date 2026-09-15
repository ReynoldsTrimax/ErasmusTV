package com.erasmustv.app.data.subtitle

data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
) {
    fun isActiveAt(positionMs: Long, offsetMs: Long = 0L): Boolean {
        val effectivePos = positionMs - offsetMs
        return effectivePos in startMs..endMs
    }
}
