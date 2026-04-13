package com.hasielectronic.hasitv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// ─── Channel ────────────────────────────────────────────────────
@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String = "General",
    val epgId: String? = null,
    val isFavorite: Boolean = false,
    val lastWatched: Long? = null,        // timestamp millis
    val playlistId: String = ""
)

// ─── Playlist ────────────────────────────────────────────────────
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val type: PlaylistType,
    val m3uUrl: String? = null,
    val xtreamHost: String? = null,
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null,
    val channelCount: Int = 0,
    val lastRefreshed: Long? = null
)

enum class PlaylistType { M3U_URL, M3U_LOCAL, XTREAM }

// ─── EPG Program ────────────────────────────────────────────────
@Entity(tableName = "epg_programs")
data class EpgProgram(
    @PrimaryKey val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,          // epoch millis
    val endTime: Long,
    val category: String? = null
) {
    val isLive: Boolean get() {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }
    val progressFraction: Float get() {
        val now = System.currentTimeMillis()
        if (now < startTime) return 0f
        val total = (endTime - startTime).toFloat()
        val elapsed = (now - startTime).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }
    val durationMin: Int get() = ((endTime - startTime) / 60_000).toInt()
}

// ─── Stream Info (for player) ────────────────────────────────────
data class StreamInfo(
    val channel: Channel,
    val currentProgram: EpgProgram? = null,
    val nextProgram: EpgProgram? = null
)
