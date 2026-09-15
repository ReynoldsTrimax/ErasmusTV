package com.erasmustv.app.data.subtitle

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SubtitleSyncEngine(
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "SubtitleSyncEngine"
        private const val BUCKET_MS = 100L
        private const val MAX_ANALYSIS_WINDOW_MS = 90_000L // Analyze first 90 seconds
        private const val MAX_SEARCH_OFFSET_MS = 5_000L    // -5s to +5s sync offset search
    }

    private val _currentOffsetMs = MutableStateFlow(0L)
    val currentOffsetMs: StateFlow<Long> = _currentOffsetMs.asStateFlow()

    private val _isAutoSynced = MutableStateFlow(false)
    val isAutoSynced: StateFlow<Boolean> = _isAutoSynced.asStateFlow()

    private val _statusText = MutableStateFlow("Auto-sync: Active (±0.0s)")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    // Real-time audio energy buckets (100ms each)
    private val audioEnergyBuckets = ArrayList<Float>(900)
    private var sampleRate: Int = 48000
    private var channelCount: Int = 2
    private var samplesAccumulated: Long = 0
    private var sumSquareAccumulated: Double = 0.0
    private var samplesPerBucket: Long = (48000 * 100) / 1000L

    private var activeCues: List<SubtitleCue> = emptyList()
    private var referenceCues: List<SubtitleCue>? = null
    private var analysisJob: Job? = null
    private var hasAppliedAutoSync = false

    fun reset() {
        analysisJob?.cancel()
        _currentOffsetMs.value = 0L
        _isAutoSynced.value = false
        _statusText.value = "Auto-sync: Active (±0.0s)"
        hasAppliedAutoSync = false
        synchronized(audioEnergyBuckets) {
            audioEnergyBuckets.clear()
        }
        samplesAccumulated = 0
        sumSquareAccumulated = 0.0
    }

    fun setActiveCues(cues: List<SubtitleCue>, reference: List<SubtitleCue>? = null) {
        activeCues = cues
        referenceCues = reference

        // Immediate Phase 1: If a reference track exists, align immediately against reference!
        if (reference != null && reference.isNotEmpty() && cues.isNotEmpty()) {
            val offset = computeOffsetAgainstReference(cues, reference)
            if (abs(offset) >= 300L) {
                applyOffset(offset, "Reference Aligned")
                return
            }
        }
    }

    fun nudgeOffset(deltaMs: Long) {
        val newOffset = _currentOffsetMs.value + deltaMs
        _currentOffsetMs.value = newOffset
        val seconds = newOffset / 1000.0
        val sign = if (newOffset >= 0) "+" else ""
        _statusText.value = "Manual Sync: ${sign}%.1fs".format(seconds)
    }

    fun applyOffset(offsetMs: Long, reason: String = "Auto-synced") {
        _currentOffsetMs.value = offsetMs
        _isAutoSynced.value = true
        hasAppliedAutoSync = true
        val seconds = offsetMs / 1000.0
        val sign = if (offsetMs >= 0) "+" else ""
        _statusText.value = "$reason (${sign}%.1fs)".format(seconds)
        Log.i(TAG, "Subtitle offset applied: ${offsetMs}ms ($reason)")
    }

    // Audio Buffer Sink Callback from ExoPlayer TeeAudioProcessor
    fun onAudioFormatChanged(rate: Int, channels: Int) {
        sampleRate = if (rate > 0) rate else 48000
        channelCount = if (channels > 0) channels else 2
        samplesPerBucket = (sampleRate.toLong() * BUCKET_MS) / 1000L
    }

    fun onAudioBuffer(buffer: ByteBuffer) {
        if (hasAppliedAutoSync || activeCues.isEmpty()) return

        val duplicate = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val shortBuffer = duplicate.asShortBuffer()
        val numShorts = shortBuffer.remaining()
        if (numShorts <= 0) return

        var sumSq = 0.0
        var count = 0
        // Downsample by channel count to process mono channel power
        val step = max(1, channelCount)
        var i = 0
        while (i < numShorts) {
            val sample = shortBuffer.get(i).toDouble() / 32768.0
            sumSq += sample * sample
            count++
            i += step
        }

        sumSquareAccumulated += sumSq
        samplesAccumulated += count

        if (samplesAccumulated >= samplesPerBucket) {
            val rms = sqrt(sumSquareAccumulated / samplesAccumulated.coerceAtLeast(1L)).toFloat()
            synchronized(audioEnergyBuckets) {
                if (audioEnergyBuckets.size < 900) { // Up to 90 seconds
                    audioEnergyBuckets.add(rms)
                }
            }
            sumSquareAccumulated = 0.0
            samplesAccumulated = 0

            // Trigger analysis once we have at least 25 seconds of audio
            val currentBuckets = synchronized(audioEnergyBuckets) { audioEnergyBuckets.size }
            if (currentBuckets in 250..300 && !hasAppliedAutoSync && analysisJob == null) {
                scheduleAudioCrossCorrelation()
            }
        }
    }

    private fun scheduleAudioCrossCorrelation() {
        analysisJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                val energyList = synchronized(audioEnergyBuckets) { audioEnergyBuckets.toList() }
                if (energyList.size < 200 || activeCues.isEmpty()) return@launch

                // 1. Calculate dynamic noise floor & threshold
                val sorted = energyList.sorted()
                val noiseFloor = sorted[sorted.size / 4] // 25th percentile
                val speechThreshold = max(noiseFloor * 2.2f, 0.025f)

                // 2. Build binary speech activity array for audio
                val audioSpeech = BooleanArray(energyList.size) { idx ->
                    energyList[idx] >= speechThreshold
                }

                // Check if audio has meaningful speech bursts
                val speechBucketCount = audioSpeech.count { it }
                if (speechBucketCount < 10) return@launch // Mostly silence/quiet

                // 3. Build binary subtitle activity array for same window
                val maxMs = energyList.size * BUCKET_MS
                val subSpeech = BooleanArray(energyList.size) { idx ->
                    val t = idx * BUCKET_MS
                    activeCues.any { cue -> t in cue.startMs..cue.endMs && cue.startMs < maxMs }
                }

                // 4. Compute cross-correlation over lag range [-5s, +5s]
                val maxLagBuckets = (MAX_SEARCH_OFFSET_MS / BUCKET_MS).toInt()
                var bestLag = 0
                var maxCorrelation = 0

                for (lag in -maxLagBuckets..maxLagBuckets) {
                    var corr = 0
                    for (k in 0 until energyList.size) {
                        val subIdx = k - lag
                        if (subIdx in 0 until energyList.size) {
                            if (audioSpeech[k] && subSpeech[subIdx]) {
                                corr++
                            }
                        }
                    }
                    if (corr > maxCorrelation) {
                        maxCorrelation = corr
                        bestLag = lag
                    }
                }

                val detectedOffsetMs = bestLag * BUCKET_MS
                val zeroCorr = run {
                    var c = 0
                    for (k in 0 until energyList.size) {
                        if (audioSpeech[k] && subSpeech[k]) c++
                    }
                    c
                }

                // Apply offset only if significantly better than zero and |offset| >= 400ms
                if (maxCorrelation > zeroCorr * 1.35 && abs(detectedOffsetMs) >= 400L) {
                    applyOffset(detectedOffsetMs, "Auto-synced")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio sync cross-correlation error: ${e.message}")
            }
        }
    }

    fun computeOffsetAgainstReference(
        targetCues: List<SubtitleCue>,
        referenceCues: List<SubtitleCue>,
        searchWindowMs: Long = 5000L,
        stepMs: Long = 50L
    ): Long {
        if (targetCues.isEmpty() || referenceCues.isEmpty() || targetCues === referenceCues) return 0L

        // Quick check: if target and reference have identical timestamps, offset is strictly 0
        if (targetCues.size >= 3 && referenceCues.size >= 3) {
            val identicalCount = (0 until min(targetCues.size, referenceCues.size)).count { i ->
                abs(targetCues[i].startMs - referenceCues[i].startMs) <= 50L &&
                        targetCues[i].text.equals(referenceCues[i].text, ignoreCase = true)
            }
            if (identicalCount >= 3) return 0L
        }

        // Method 1: Text Token / Dialogue Matching & Delta Clustering (Industrial Standard)
        fun cleanText(s: String): String =
            s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()

        val deltas = mutableListOf<Long>()
        val targetWindow = targetCues.take(100)
        val refWindow = referenceCues.take(120)

        for (t in targetWindow) {
            val tClean = cleanText(t.text)
            if (tClean.length < 4) continue

            var bestMatchRef: SubtitleCue? = null
            var bestDistance = Long.MAX_VALUE

            for (r in refWindow) {
                // Search within [-searchWindowMs - 1000, +searchWindowMs + 1000]
                val diff = abs(r.startMs - t.startMs)
                if (diff > searchWindowMs + 1000L) continue

                val rClean = cleanText(r.text)
                if (rClean.length < 4) continue

                // Check text equality or significant substring overlap
                val isMatch = tClean == rClean ||
                        (tClean.length > 12 && rClean.contains(tClean)) ||
                        (rClean.length > 12 && tClean.contains(rClean))

                if (isMatch && diff < bestDistance) {
                    bestDistance = diff
                    bestMatchRef = r
                }
            }

            if (bestMatchRef != null) {
                // How much target needs to shift to match reference:
                // target.startMs + offset = reference.startMs => offset = reference.startMs - target.startMs
                val offset = bestMatchRef.startMs - t.startMs
                deltas.add(offset)
            }
        }

        // If we found matching dialogue lines, find the cluster
        if (deltas.size >= 3) {
            // Bucket by 100ms using true floor
            val buckets = deltas.groupBy {
                (kotlin.math.floor((it.toDouble() + 50.0) / 100.0) * 100.0).toLong()
            }
            val dominant = buckets.maxByOrNull { it.value.size }
            if (dominant != null && dominant.value.size >= 3) {
                val ratio = dominant.value.size.toFloat() / deltas.size.toFloat()
                if (ratio >= 0.5f) {
                    val clusterOffset = dominant.value.average().toLong()
                    if (abs(clusterOffset) in 300L..searchWindowMs) {
                        return clusterOffset
                    } else if (abs(clusterOffset) < 300L) {
                        return 0L // Already aligned within tolerance
                    }
                }
            }
        }

        // Method 2: Mathematical Interval Overlap Correlation (Fixed break condition)
        val subTarget = targetCues.take(80)
        val subRef = referenceCues.take(80).sortedBy { it.startMs }

        fun scoreAt(delta: Long): Long {
            var score = 0L
            for (t in subTarget) {
                val sStart = t.startMs + delta
                val sEnd = t.endMs + delta
                for (r in subRef) {
                    if (r.startMs > sEnd) break
                    if (r.endMs < sStart) continue
                    val overlap = min(sEnd, r.endMs) - max(sStart, r.startMs)
                    if (overlap > 0) score += overlap
                }
            }
            return score
        }

        val zeroScore = scoreAt(0L)
        var bestScore = zeroScore
        var bestDelta = 0L

        var delta = -searchWindowMs
        while (delta <= searchWindowMs) {
            val s = scoreAt(delta)
            if (s > bestScore) {
                bestScore = s
                bestDelta = delta
            }
            delta += stepMs
        }

        val threshold = if (zeroScore > 0) (zeroScore * 1.3).toLong() else 0L
        return if (bestScore > threshold && abs(bestDelta) >= 400L) {
            bestDelta
        } else {
            0L
        }
    }
}
