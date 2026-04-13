package com.hasielectronic.hasitv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.hasielectronic.hasitv.data.model.Channel
import com.hasielectronic.hasitv.data.model.EpgProgram
import com.hasielectronic.hasitv.data.model.Playlist
import com.hasielectronic.hasitv.data.model.PlaylistType
import com.hasielectronic.hasitv.data.parser.M3UParser
import com.hasielectronic.hasitv.data.parser.XmltvParser
import com.hasielectronic.hasitv.data.parser.XtreamConfig
import com.hasielectronic.hasitv.data.parser.XtreamService
import kotlinx.coroutines.flow.Flow

class IptvRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hasitv_prefs", Context.MODE_PRIVATE)

    // ─── Preferences ─────────────────────────────────────────────
    var epgUrl: String
        get() = prefs.getString("epg_url", "") ?: ""
        set(v) = prefs.edit().putString("epg_url", v).apply()

    var epgLastFetched: Long
        get() = prefs.getLong("epg_last_fetched", 0L)
        set(v) = prefs.edit().putLong("epg_last_fetched", v).apply()

    val epgCacheValid: Boolean
        get() = System.currentTimeMillis() - epgLastFetched < 12 * 60 * 60 * 1000L

    // ─── Channels ────────────────────────────────────────────────
    fun getAllChannelsFlow(): Flow<List<Channel>> = db.channelDao().getAllFlow()
    fun getFavoritesFlow(): Flow<List<Channel>>  = db.channelDao().getFavoritesFlow()
    fun getRecentFlow(): Flow<List<Channel>>     = db.channelDao().getRecentFlow()

    suspend fun getGroups(): List<String>        = db.channelDao().getGroups()

    suspend fun toggleFavorite(channel: Channel) {
        db.channelDao().setFavorite(channel.id, !channel.isFavorite)
    }

    suspend fun markWatched(channel: Channel) {
        db.channelDao().setLastWatched(channel.id, System.currentTimeMillis())
    }

    // ─── Playlists ───────────────────────────────────────────────
    fun getPlaylistsFlow(): Flow<List<Playlist>> = db.playlistDao().getAllFlow()

    suspend fun addPlaylist(playlist: Playlist) {
        db.playlistDao().upsert(playlist)
        refreshPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        db.playlistDao().delete(playlist)
        db.channelDao().deleteByPlaylist(playlist.id)
    }

    suspend fun refreshPlaylist(playlist: Playlist) {
        val channels = when (playlist.type) {
            PlaylistType.M3U_URL -> {
                playlist.m3uUrl?.let { M3UParser.fetchAndParse(it, playlist.id) } ?: emptyList()
            }
            PlaylistType.XTREAM -> {
                val config = XtreamConfig(
                    host     = playlist.xtreamHost ?: "",
                    username = playlist.xtreamUsername ?: "",
                    password = playlist.xtreamPassword ?: ""
                )
                XtreamService.fetchLiveStreams(config, playlist.id)
            }
            PlaylistType.M3U_LOCAL -> emptyList() // loaded separately
        }
        if (channels.isNotEmpty()) {
            db.channelDao().deleteByPlaylist(playlist.id)
            db.channelDao().upsertAll(channels)
            db.playlistDao().upsert(
                playlist.copy(
                    channelCount  = channels.size,
                    lastRefreshed = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun refreshAllPlaylists() {
        db.playlistDao().getAll().forEach { refreshPlaylist(it) }
    }

    suspend fun loadLocalM3U(content: String, playlist: Playlist) {
        val channels = M3UParser.parse(content, playlist.id)
        db.channelDao().deleteByPlaylist(playlist.id)
        db.channelDao().upsertAll(channels)
        db.playlistDao().upsert(playlist.copy(channelCount = channels.size))
    }

    // ─── EPG ─────────────────────────────────────────────────────
    suspend fun refreshEpg(forceRefresh: Boolean = false) {
        if (!forceRefresh && epgCacheValid && db.epgDao().count() > 0) return
        val url = epgUrl.takeIf { it.isNotBlank() } ?: return
        val programs = XmltvParser.fetchAndParse(url)
        if (programs.isNotEmpty()) {
            db.epgDao().deleteAll()
            db.epgDao().upsertAll(programs)
            epgLastFetched = System.currentTimeMillis()
        }
    }

    suspend fun getCurrentProgram(channelId: String): EpgProgram? =
        db.epgDao().getCurrentProgram(channelId)

    suspend fun getNextProgram(channelId: String): EpgProgram? =
        db.epgDao().getNextProgram(channelId)

    suspend fun getProgramsForChannel(channelId: String): List<EpgProgram> =
        db.epgDao().getByChannel(channelId)
}
