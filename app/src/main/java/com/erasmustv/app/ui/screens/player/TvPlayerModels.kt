package com.erasmustv.app.ui.screens.player

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi

enum class PlayerActiveMenu {
    None,
    Audio,
    Subtitles,
    Quality,
    Servers,
    Episodes
}

@OptIn(UnstableApi::class)
data class TvAudioTrack(
    val id: String,
    val mediaTrackGroup: TrackGroup,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
data class TvSubtitleTrack(
    val id: String,
    val mediaTrackGroup: TrackGroup? = null,
    val trackIndex: Int = -1,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val isOff: Boolean = false
)

@OptIn(UnstableApi::class)
data class TvQualityTrack(
    val id: String,
    val mediaTrackGroup: TrackGroup? = null,
    val trackIndex: Int = -1,
    val label: String,
    val width: Int = 0,
    val height: Int = 0,
    val bitrate: Int = 0,
    val isAuto: Boolean = false,
    val isSelected: Boolean = false
)

data class TvScrubState(
    val isScrubbing: Boolean = false,
    val scrubPositionMs: Long = 0L,
    val anchorPositionMs: Long = 0L,
    val previewBitmap: Bitmap? = null,
    val isFetchingFrame: Boolean = false
)
