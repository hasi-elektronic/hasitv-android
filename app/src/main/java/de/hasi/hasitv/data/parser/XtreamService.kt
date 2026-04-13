package de.hasi.hasitv.data.parser

import de.hasi.hasitv.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class XtreamConfig(
    val host: String,
    val username: String,
    val password: String
) {
    val baseUrl get() = "$host/player_api.php?username=$username&password=$password"
    val streamBaseUrl get() = "$host/live/$username/$password"
}

object XtreamService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Verify credentials ──────────────────────────────────────
    suspend fun verify(config: XtreamConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = get(config.baseUrl)
            val json = JSONObject(resp)
            json.optJSONObject("user_info")?.optString("status") == "Active"
        } catch (e: Exception) { false }
    }

    // ─── Fetch live streams ──────────────────────────────────────
    suspend fun fetchLiveStreams(config: XtreamConfig, playlistId: String): List<Channel> =
        withContext(Dispatchers.IO) {
            try {
                val resp = get("${config.baseUrl}&action=get_live_streams")
                val arr = JSONArray(resp)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optInt("stream_id")
                    Channel(
                        id          = streamId.toString(),
                        name        = obj.optString("name"),
                        url         = "${config.streamBaseUrl}/$streamId.m3u8",
                        logoUrl     = obj.optString("stream_icon").takeIf { it.isNotBlank() },
                        group       = obj.optString("category_name", "General"),
                        epgId       = obj.optString("epg_channel_id").takeIf { it.isNotBlank() },
                        playlistId  = playlistId
                    )
                }
            } catch (e: Exception) { emptyList() }
        }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
