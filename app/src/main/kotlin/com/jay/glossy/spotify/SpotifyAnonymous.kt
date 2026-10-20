/**
 * Spo~z Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.spotify

import com.jay.glossy.spotifycore.models.SpotifySimpleArtist
import com.jay.glossy.spotifycore.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Raised when a shared Spotify link cannot be turned into a public collection. */
class SpotifyImportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Anonymous (no login, no sp_dc cookie) access to a public Spotify
 * playlist/album/track.
 *
 * Spotify's embed page server-renders the first slice of a collection into a
 * `__NEXT_DATA__` JSON blob and answers without any token, which is the only
 * public surface that does. That is what the share-import reads. It exposes at
 * most [MAX_EMBED_TRACKS] entries, so very large playlists import their first
 * hundred songs.
 */
object SpotifyAnonymous {
    /** The embed page server-renders at most this many entries per collection. */
    const val MAX_EMBED_TRACKS = 100

    private val client =
        OkHttpClient
            .Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"

    private val NEXT_DATA =
        Regex(
            """<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

    private val httpUrl =
        Regex(
            """https?://open\.spotify\.com/(?:intl-[a-z-]+/)?(playlist|album|track)/([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE,
        )

    private val spotifyUri =
        Regex("""spotify:(playlist|album|track):([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)

    private val shortLink =
        Regex("""https?://(?:www\.)?(?:spotify\.link|spotify\.app\.link)/\S+""", RegexOption.IGNORE_CASE)

    enum class Kind(val path: String) {
        PLAYLIST("playlist"),
        ALBUM("album"),
        TRACK("track"),
    }

    data class Ref(val kind: Kind, val id: String)

    data class PublicCollection(
        val name: String,
        val tracks: List<SpotifyTrack>,
        val truncated: Boolean,
    )

    sealed interface Target {
        data class Direct(val ref: Ref) : Target

        data class ShortLink(val url: String) : Target

        data object None : Target
    }

    /** Finds the Spotify target in arbitrary shared text (a share sheet's EXTRA_TEXT, a VIEW uri, ...). */
    fun extractTarget(raw: String?): Target {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return Target.None

        httpUrl.find(text)?.let { return Target.Direct(Ref(kindOf(it.groupValues[1]), it.groupValues[2])) }
        spotifyUri.find(text)?.let { return Target.Direct(Ref(kindOf(it.groupValues[1]), it.groupValues[2])) }
        shortLink.find(text)?.let { return Target.ShortLink(it.value.trimEnd('.', ',', ')', ']')) }
        return Target.None
    }

    private fun kindOf(value: String): Kind =
        when (value.lowercase()) {
            "album" -> Kind.ALBUM
            "track" -> Kind.TRACK
            else -> Kind.PLAYLIST
        }

    /** Follows a spotify.link / spotify.app.link short link to its real open.spotify.com reference. */
    fun resolveShortLink(url: String): Ref? =
        runCatching {
            client
                .newCall(Request.Builder().url(url).header("User-Agent", UA).build())
                .execute()
                .use { response ->
                    httpUrl.find(response.request.url.toString())?.let {
                        return@use Ref(kindOf(it.groupValues[1]), it.groupValues[2])
                    }
                    val body = response.body?.string().orEmpty()
                    httpUrl.find(body)?.let {
                        return@use Ref(kindOf(it.groupValues[1]), it.groupValues[2])
                    }
                    null
                }
        }.getOrNull()

    /** Fetches a public collection through the embed page — no token, no cookie. */
    suspend fun fetch(ref: Ref): PublicCollection =
        withContext(Dispatchers.IO) {
            val url = "https://open.spotify.com/embed/${ref.kind.path}/${ref.id}"
            val html =
                try {
                    client
                        .newCall(
                            Request
                                .Builder()
                                .url(url)
                                .header("User-Agent", UA)
                                .header("Accept-Language", "en")
                                .build(),
                        ).execute()
                        .use { response ->
                            if (!response.isSuccessful) throw SpotifyImportException("not_public")
                            response.body?.string().orEmpty()
                        }
                } catch (e: SpotifyImportException) {
                    throw e
                } catch (e: IOException) {
                    throw SpotifyImportException("network", e)
                }

            val nextData =
                NEXT_DATA.find(html)?.groupValues?.get(1)
                    ?: throw SpotifyImportException("not_public")

            val root = runCatching { json.parseToJsonElement(nextData) }.getOrNull()
                ?: throw SpotifyImportException("not_public")

            val entity =
                root
                    .obj()?.get("props")
                    ?.obj()?.get("pageProps")
                    ?.obj()?.get("state")
                    ?.obj()?.get("data")
                    ?.obj()?.get("entity")
                    ?.obj()
                    ?: throw SpotifyImportException("not_public")

            val name = entity.string("name") ?: entity.string("title") ?: "Spotify import"
            val tracks = parseTracks(ref.kind, entity)
            if (tracks.isEmpty()) throw SpotifyImportException("empty")

            PublicCollection(
                name = name.trim().ifBlank { "Spotify import" },
                tracks = tracks,
                truncated = tracks.size >= MAX_EMBED_TRACKS,
            )
        }

    private fun parseTracks(
        kind: Kind,
        entity: JsonObject,
    ): List<SpotifyTrack> {
        if (kind == Kind.TRACK) {
            return listOfNotNull(entityToTrack(entity))
        }
        val list =
            entity["trackList"]?.let { runCatching { it.jsonArray }.getOrNull() }
                ?: return emptyList()
        return list.mapNotNull { entry -> runCatching { entry.jsonObject }.getOrNull()?.let(::entryToTrack) }
    }

    private fun entryToTrack(entry: JsonObject): SpotifyTrack? {
        // Episodes and other non-track entities are skipped; only songs are imported.
        val entityType = entry.string("entityType")
        if (entityType != null && !entityType.equals("track", ignoreCase = true)) return null

        val title = entry.string("title") ?: return null
        val uri = entry.string("uri")
        return SpotifyTrack(
            id = uri?.substringAfterLast(':') ?: entry.string("uid").orEmpty(),
            name = title,
            artists = parseArtists(entry.string("subtitle")),
            durationMs = entry.int("duration") ?: 0,
            explicit = entry.bool("isExplicit") ?: false,
            uri = uri,
        )
    }

    private fun entityToTrack(entity: JsonObject): SpotifyTrack? {
        val title = entity.string("title") ?: entity.string("name") ?: return null
        val uri = entity.string("uri")
        return SpotifyTrack(
            id = uri?.substringAfterLast(':') ?: entity.string("id").orEmpty(),
            name = title,
            artists = parseArtists(entity.string("subtitle")),
            durationMs = entity.int("duration") ?: 0,
            explicit = entity.bool("isExplicit") ?: false,
            uri = uri,
        )
    }

    /** "A,\u00a0B" -> [A, B]; the embed separates artists with a comma plus a non-breaking space. */
    private fun parseArtists(subtitle: String?): List<SpotifySimpleArtist> =
        subtitle
            .orEmpty()
            .replace('\u00a0', ' ')
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { SpotifySimpleArtist(name = it) }

    private fun JsonElement.obj(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonObject.string(key: String): String? =
        this[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

    private fun JsonObject.int(key: String): Int? =
        this[key]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.let { runCatching { it.jsonPrimitive.booleanOrNull }.getOrNull() }
}
