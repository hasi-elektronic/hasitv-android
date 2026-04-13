package de.hasi.hasitv.data.parser

import android.util.Xml
import de.hasi.hasitv.data.model.EpgProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object XmltvParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val dateFormats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    )

    // ─── Fetch + Parse ───────────────────────────────────────────
    suspend fun fetchAndParse(url: String): List<EpgProgram> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
            parse(body)
        }

    // ─── Parse XMLTV content ─────────────────────────────────────
    fun parse(content: String): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(content))

            var channelId = ""
            var startTime = 0L
            var endTime = 0L
            var title = ""
            var description: String? = null
            var category: String? = null
            var inProgramme = false
            var currentTag = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "programme" -> {
                                inProgramme = true
                                channelId   = parser.getAttributeValue(null, "channel") ?: ""
                                startTime   = parseDate(parser.getAttributeValue(null, "start") ?: "")
                                endTime     = parseDate(parser.getAttributeValue(null, "stop") ?: "")
                                title = ""; description = null; category = null
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inProgramme) {
                            val text = parser.text.trim()
                            when (currentTag) {
                                "title"    -> title += text
                                "desc"     -> description = text
                                "category" -> category = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme" && inProgramme) {
                            if (title.isNotBlank() && channelId.isNotBlank()) {
                                programs += EpgProgram(
                                    id          = "$channelId-$startTime",
                                    channelId   = channelId,
                                    title       = title,
                                    description = description,
                                    startTime   = startTime,
                                    endTime     = endTime,
                                    category    = category
                                )
                            }
                            inProgramme = false
                        }
                        currentTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return programs
    }

    // ─── Date parsing: "20240115183000 +0100" ────────────────────
    private fun parseDate(raw: String): Long {
        val clean = raw.trim()
        for (fmt in dateFormats) {
            try {
                return fmt.parse(clean)?.time ?: continue
            } catch (_: Exception) {}
        }
        // Try without timezone (prefix 14 chars)
        return try {
            dateFormats[1].parse(clean.take(14))?.time ?: 0L
        } catch (_: Exception) { 0L }
    }
}
