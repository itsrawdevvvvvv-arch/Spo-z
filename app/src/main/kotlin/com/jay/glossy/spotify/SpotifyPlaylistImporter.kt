/**
 * Spo~z Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.spotify

import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.db.entities.PlaylistEntity
import com.jay.glossy.spotifycore.Spotify
import com.jay.glossy.spotifycore.SpotifyMapper
import com.jay.glossy.spotifycore.models.SpotifyTrack
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Imports a Spotify playlist into a local playlist.
 *
 * The old implementation took the *first* YouTube search hit for
 * "song name + artist". That pulled in the wrong version (live, remix, cover)
 * whenever the top hit was not the studio track, and silently dropped anything
 * the search missed.
 *
 * This version resolves every track through [SpotifyPlaybackResolver], the same
 * scoring path playback uses: candidates are compared on title, artist and
 * duration, and anything scoring below [MIN_MATCH_THRESHOLD] is rejected rather
 * than imported. A scored fallback query covers tracks the primary query
 * misses, without ever lowering the quality bar.
 */
object SpotifyPlaylistImporter {
    private const val MAX_CONCURRENT_RESOLUTIONS = 8

    /** Kept in step with [SpotifyPlaybackResolver]. */
    private const val MIN_MATCH_THRESHOLD = 0.35

    suspend fun importPlaylist(
        database: MusicDatabase,
        playlistId: String,
        playlistName: String,
    ): Int = withContext(Dispatchers.IO) {
        val page = Spotify.playlistTracks(playlistId, limit = 100).getOrThrow()
        val tracks = page.items.mapNotNull { it.track }
        if (tracks.isEmpty()) error("Playlist has no playable tracks")

        val entity = PlaylistEntity(name = playlistName)
        database.transaction { insert(entity) }
        val localPlaylist = database.playlist(entity.id).firstOrNull() ?: error("Could not create local playlist")

        val songIds = mutableListOf<String>()
        tracks.chunked(MAX_CONCURRENT_RESOLUTIONS).forEach { chunk ->
            val resolved = coroutineScope {
                chunk.map { track -> async { resolveTrack(database, track) } }.awaitAll().filterNotNull()
            }
            songIds.addAll(resolved)
        }

        database.addSongsToPlaylist(localPlaylist, songIds.map { it to null })
        songIds.size
    }

    /**
     * Resolves one Spotify track to a local song id, best match first.
     */
    private suspend fun resolveTrack(database: MusicDatabase, track: SpotifyTrack): String? {
        val song = SpotifyPlaybackResolver.resolveToSongItem(track) ?: fallbackSearch(track) ?: return null
        val metadata = song.toMediaMetadata()
        database.transaction { insert(metadata) }
        return metadata.id
    }

    /**
     * Title-first fallback for tracks the scorer's artist-first query misses.
     * It still has to clear the same match bar, so it never trades quality for
     * coverage.
     */
    private suspend fun fallbackSearch(track: SpotifyTrack): SongItem? {
        val artist = track.artists.firstOrNull()?.name ?: return null
        val result = YouTube.search(
            query = "${track.name} $artist",
            filter = YouTube.SearchFilter.FILTER_SONG,
        ).getOrNull() ?: return null

        val candidates = result.items.filterIsInstance<SongItem>().distinctBy { it.id }
        if (candidates.isEmpty()) return null

        val precomputed = SpotifyMapper.precompute(
            title = track.name,
            artist = track.artists.joinToString(" ") { it.name },
            durationMs = track.durationMs,
        )

        val best = candidates.map { candidate ->
            candidate to SpotifyMapper.matchScorePrecomputed(
                precomputed = precomputed,
                candidateTitle = candidate.title,
                candidateArtist = candidate.artists.joinToString(" ") { it.name },
                candidateDurationSec = candidate.duration,
            )
        }.maxByOrNull { it.second } ?: return null

        return if (best.second < MIN_MATCH_THRESHOLD) null else best.first
    }
}
