/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.db.entities

import com.jay.glossy.R

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
        Junction(
            value = PlaylistSongMapPreview::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songThumbnails: List<String?>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
    override val title: String
        get() = playlist.name

    /**
     * Was hardcoded `null`, so any card that draws a playlist's single cover
     * (the Library grid, the widget, the mini-player) came out blank for local
     * playlists — including freshly imported Spotify ones — even though the
     * songs and their art were right there. Fall back to the first song's
     * thumbnail (and to the playlist's own cover if it has one) so there is
     * always something to draw.
     */
    override val thumbnailUrl: String?
        get() = playlist.thumbnailUrl ?: songThumbnails.firstOrNull { !it.isNullOrBlank() }

    val thumbnails: List<String>
        get() {
            return if (playlist.thumbnailUrl != null)
                listOf(playlist.thumbnailUrl)
            else songThumbnails.filterNotNull()
        }
}
