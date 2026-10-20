/**
 * Spo~z Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.jay.glossy.spotify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jay.glossy.MainActivity
import com.jay.glossy.db.MusicDatabase
import com.jay.glossy.ui.theme.SpozAccents
import com.jay.glossy.ui.theme.SpozPalette
import com.jay.glossy.widget.PlaylistWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The share/VIEW target for Spotify links.
 *
 * It appears in the Spotify app's share sheet (ACTION_SEND text/plain) and in
 * the system chooser for open.spotify.com / spotify.link / spotify: links. It
 * reads the shared link, pulls the collection anonymously through
 * [SpotifyAnonymous], resolves every track through the same scoring path
 * playback uses ([SpotifyPlaylistImporter]), then hands off to [MainActivity] to
 * open the freshly created local playlist. No Spotify login is involved.
 */
@AndroidEntryPoint
class SpotifyShareImportActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    private val state = MutableStateFlow<ImportState>(ImportState.Working("Starting…"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme =
                    darkColorScheme(
                        primary = SpozAccents.Purple,
                        onPrimary = SpozPalette.LightInk,
                        background = SpozPalette.DarkBackground,
                        onBackground = SpozPalette.DarkInk,
                        surface = SpozPalette.DarkSurface,
                        onSurface = SpozPalette.DarkInk,
                    ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val current by state.collectAsState()
                    ImportScreen(current, onClose = { finish() })
                }
            }
        }
        lifecycleScope.launch { runImport() }
    }

    private suspend fun runImport() {
        val ref = resolveRef()
        if (ref == null) {
            state.value = ImportState.Error("This doesn't look like a Spotify playlist link.")
            return
        }

        state.value = ImportState.Working("Reading playlist…")
        val collection =
            try {
                SpotifyAnonymous.fetch(ref)
            } catch (e: SpotifyImportException) {
                state.value = ImportState.Error(friendly(e))
                return
            } catch (e: Exception) {
                state.value = ImportState.Error("Couldn't read this playlist. Try again.")
                return
            }

        state.value = ImportState.Working("Importing ${collection.tracks.size} songs…")
        val result =
            try {
                SpotifyPlaylistImporter.importTracks(database, collection.name, collection.tracks)
            } catch (e: Exception) {
                state.value = ImportState.Error("Import failed. ${e.message.orEmpty()}".trim())
                return
            }

        if (result.importedCount == 0) {
            runCatching { database.transaction { delete(result.entity) } }
            state.value = ImportState.Error("Couldn't match any songs from this playlist.")
            return
        }

        openPlaylist(result.playlistId)
        finish()
    }

    private fun resolveRef(): SpotifyAnonymous.Ref? =
        when (val target = SpotifyAnonymous.extractTarget(sharedText())) {
            is SpotifyAnonymous.Target.Direct -> target.ref
            is SpotifyAnonymous.Target.ShortLink -> SpotifyAnonymous.resolveShortLink(target.url)
            SpotifyAnonymous.Target.None -> null
        }

    private fun friendly(e: SpotifyImportException): String =
        when (e.message) {
            "not_public" -> "This playlist isn't public, so it can't be imported."
            "empty" -> "No playable songs were found in this playlist."
            "network" -> "Couldn't reach Spotify. Check your connection and try again."
            else -> "Couldn't import this playlist."
        }

    private fun sharedText(): String? {
        intent?.getStringExtra(Intent.EXTRA_TEXT)?.let { return it }
        return intent?.dataString
    }

    /** Reopens MainActivity on the new local playlist via its existing widget-target route. */
    private fun openPlaylist(playlistId: String) {
        val open =
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_WIDGET_TARGET
                putExtra(MainActivity.EXTRA_WIDGET_TARGET_TYPE, PlaylistWidgetReceiver.TARGET_TYPE_LOCAL)
                putExtra(MainActivity.EXTRA_WIDGET_TARGET_ID, playlistId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        startActivity(open)
    }

    sealed interface ImportState {
        data class Working(val message: String) : ImportState

        data class Error(val message: String) : ImportState
    }
}

@Composable
private fun ImportScreen(
    state: SpotifyShareImportActivity.ImportState,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is SpotifyShareImportActivity.ImportState.Working -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Importing from Spotify",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            is SpotifyShareImportActivity.ImportState.Error -> {
                Text(
                    "Couldn't import",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
