/**
 * Spo~z — loading + sign-in screen
 * Licensed under GPL-3.0
 *
 * Replaces the old Glossy glass/holographic onboarding with a new, deliberately
 * light design: an animated Spo~z wordmark + equaliser while the app settles,
 * then the sign-in card. Everything here is cheap to draw — no blur, no glass
 * layer, no full-screen infinite shader — so it stays smooth on low-end devices
 * and the very first thing a user sees is not the thing that stutters.
 *
 * Public API is unchanged (same [GlossyLoginScreen] entry point) so the
 * onboarding host keeps working untouched.
 */

package com.jay.glossy.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.ui.theme.SpozAccents
import com.jay.glossy.ui.theme.SpozPalette
import kotlinx.coroutines.delay

private const val SPOZ_WORDMARK = "Spo~z"
private const val SPOZ_TAGLINE = "Your music, in rhythm with your day."
private const val SPOZ_CREDIT = "Crafted with \u2764 by rawde_vvvvvv"

@Composable
fun GlossyLoginScreen(
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1900)
        loading = false
    }

    Box(
        modifier
            .fillMaxSize()
            .background(SpozPalette.DarkBackground),
    ) {
        Crossfade(targetState = loading, animationSpec = tween(450), label = "spoz_enter") { isLoading ->
            if (isLoading) {
                SpozLoading()
            } else {
                SpozSignIn(onGoogleClick, onGuestClick, onTokenClick)
            }
        }
    }
}

/** Wordmark rising in, with a light equaliser pulsing underneath. */
@Composable
private fun SpozLoading() {
    val enter by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "enter",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = SPOZ_WORDMARK,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold,
            color = SpozPalette.DarkInk,
            modifier = Modifier.graphicsLayer {
                alpha = enter
                val s = 0.9f + 0.1f * enter
                scaleX = s
                scaleY = s
            },
        )
        Spacer(Modifier.height(26.dp))
        SpozEqualiser()
    }
}

@Composable
private fun SpozEqualiser() {
    val transition = rememberInfiniteTransition(label = "spoz_eq")
    val bars = List(5) { i ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520 + i * 90, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$i",
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEachIndexed { i, h ->
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height((34f * h.value).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpozAccents.forIndex(i)),
            )
        }
    }
}

@Composable
private fun SpozSignIn(
    onGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    onTokenClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
            .padding(top = 104.dp, bottom = 30.dp),
    ) {
        Text(
            text = SPOZ_WORDMARK,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = SpozPalette.DarkInk,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = SPOZ_TAGLINE,
            fontSize = 15.sp,
            color = SpozPalette.DarkMuted,
        )

        Spacer(Modifier.weight(1f))

        SpozActionButton("Continue with Google", SpozAccents.Purple, onGoogleClick)
        Spacer(Modifier.height(12.dp))
        SpozActionButton("Continue as a guest", SpozAccents.Blue, onGuestClick)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Use a cookie / token instead",
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTokenClick)
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = SpozPalette.DarkMuted,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = SPOZ_CREDIT,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = SpozPalette.DarkMuted,
        )
    }
}

@Composable
private fun SpozActionButton(
    text: String,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = SpozPalette.DarkInk,
        )
    }
}
