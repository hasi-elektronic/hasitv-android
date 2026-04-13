package com.hasielectronic.hasitv.data.repository

import android.content.Context
import androidx.room.*
import com.hasielectronic.hasitv.data.model.Channel
import com.hasielectronic.hasitv.data.model.EpgProgram
import com.hasielectronic.hasitv.data.model.Playlist
import com.hasielectronic.hasitv.data.model.PlaylistType
import kotlinx.coroutines.flow.Flow

// ─── Type Converters ─────────────────────────────────────────────
class Converters {
    @TypeConverter
    fun fromPlaylistType(type: PlaylistType): String = type.name
    @TypeConverter
    fun toPlaylistType(value: String): PlaylistType = PlaylistType.valueOf(value)
}

// ─── DAOs ────────────────────────────────────────────────────────
@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllFlow(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoritesFlow(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE lastWatched IS NOT NULL ORDER BY lastWatched DESC LIMIT 20")
    fun getRecentFlow(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE `group` = :group ORDER BY name ASC")
    fun getByGroupFlow(group: String): Flow<List<Channel>>

    @Query("SELECT DISTINCT `group` FROM channels ORDER BY `group` ASC")
    suspend fun getGroups(): List<String>

    @Upsert
    suspend fun upsertAll(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE channels SET lastWatched = :ts WHERE id = :id")
    suspend fun setLastWatched(id: String, ts: Long)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun countByPlaylist(playlistId: String): Int
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllFlow(): Flow<List<Playlist>>

    @Upsert
    suspend fun upsert(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("SELECT * FROM playlists")
    suspend fun getAll(): List<Playlist>
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY startTime ASC")
    suspend fun getByChannel(channelId: String): List<EpgProgram>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime <= :now AND endTime >= :now LIMIT 1")
    suspend fun getCurrentProgram(channelId: String, now: Long = System.currentTimeMillis()): EpgProgram?

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime > :now ORDER BY startTime ASC LIMIT 1")
    suspend fun getNextProgram(channelId: String, now: Long = System.currentTimeMillis()): EpgProgram?

    @Upsert
    suspend fun upsertAll(programs: List<EpgProgram>)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM epg_programs")
    suspend fun count(): Int
}

// ─── Database ────────────────────────────────────────────────────
@Database(
    entities = [Channel::class, Playlist::class, EpgProgram::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun epgDao(): EpgDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "hasitv.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
