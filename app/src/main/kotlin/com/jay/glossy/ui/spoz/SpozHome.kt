/**
 * Spo~z — Home screen
 * Licensed under GPL-3.0
 *
 * Ported from the Move Sync prototype's R.home: greeting header, the big
 * "Daily Mix" hero, the week strip, the "Your Plan" tile grid, the Wrapped
 * banner and the recently-played list.
 *
 * Self-contained: it takes plain data and callbacks, so it compiles and renders
 * without depending on the player/viewmodel layer. The wiring layer feeds it
 * real data later.
 */

package com.jay.glossy.ui.spoz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.ui.theme.SpozAccents

/** Minimal view models for the Home screen. */
data class SpozTrack(
    val title: String,
    val artist: String,
    val accentIndex: Int,
)

data class SpozPlanTile(
    val label: String,
    val title: String,
    val time: String?,
    val accentIndex: Int,
)

/** Accent-cycling gradient used for the placeholder cover art. */
@Composable
private fun coverBrush(index: Int): Brush {
    val a = SpozAccents.forIndex(index)
    val b = SpozAccents.forIndex(index + 2)
    return Brush.linearGradient(listOf(a, b))
}

@Composable
private fun Cover(index: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(coverBrush(index)),
    )
}

@Composable
private fun IconBadge(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 18.sp)
    }
}

/**
 * The full Home screen.
 */
@Composable
fun SpozHomeScreen(
    userName: String = "Sandra",
    dateLabel: String = "Today",
    week: List<String> = listOf("S", "M", "T", "W", "T", "F", "S"),
    todayIndex: Int = 0,
    plan: List<SpozPlanTile> = emptyList(),
    recent: List<SpozTrack> = emptyList(),
    onSearch: () -> Unit = {},
    onAccount: () -> Unit = {},
    onOpenMix: () -> Unit = {},
    onOpenPlan: (SpozPlanTile) -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onPlay: (SpozTrack) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hello, $userName",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        dateLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconBadge("S", onSearch)
                Spacer(Modifier.width(8.dp))
                IconBadge("A", onAccount)
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SpozAccents.Purple, SpozAccents.Blue),
                        ),
                    )
                    .clickable(onClick = onOpenMix)
                    .padding(20.dp),
            ) {
                Column {
                    Text("Daily", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Mix", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Play your mix before 08:00 AM",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEachIndexed { i, d ->
                    val on = i == todayIndex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            d,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Your Plan",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(plan) { tile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(SpozAccents.forIndex(tile.accentIndex))
                    .clickable { onOpenPlan(tile) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tile.label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        tile.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    tile.time?.let {
                        Text(it, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7B5CFF), SpozAccents.Pink),
                        ),
                    )
                    .clickable(onClick = onOpenWrapped)
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        "Your 2026 Wrapped is ready",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Tap to relive your year",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        item {
            Text(
                "Recently played",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(recent) { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPlay(track) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(track.accentIndex, Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        track.artist,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(90.dp)) }
    }
}
