package de.hasi.hasitv.data.parser

import de.hasi.hasitv.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

object M3UParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Fetch from URL ──────────────────────────────────────────
    suspend fun fetchAndParse(url: String, playlistId: String): List<Channel> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            parse(body, playlistId)
        }

    // ─── Parse raw M3U content ───────────────────────────────────
    fun parse(content: String, playlistId: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var name = ""
        var logoUrl: String? = null
        var group = "General"
        var epgId: String? = null
        var channelId = ""

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF") -> {
                    name     = extractName(trimmed)
                    logoUrl  = extractAttr("tvg-logo", trimmed)
                    group    = extractAttr("group-title", trimmed) ?: "General"
                    channelId = extractAttr("tvg-id", trimmed) ?: UUID.randomUUID().toString()
                    epgId    = extractAttr("tvg-id", trimmed)
                }
                trimmed.startsWith("http") ||
                trimmed.startsWith("rtmp") ||
                trimmed.startsWith("rtsp") -> {
                    if (name.isNotBlank()) {
                        channels += Channel(
                            id         = channelId,
                            name       = name,
                            url        = trimmed,
                            logoUrl    = logoUrl,
                            group      = group,
                            epgId      = epgId,
                            playlistId = playlistId
                        )
                    }
                    name = ""; logoUrl = null; group = "General"; epgId = null; channelId = ""
                }
            }
        }
        return channels
    }

    // ─── Helpers ─────────────────────────────────────────────────
    private fun extractName(line: String): String =
        line.substringAfterLast(",").trim()

    private fun extractAttr(attr: String, line: String): String? {
        val regex = Regex("""$attr="([^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }
}
