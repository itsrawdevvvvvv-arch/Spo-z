/**
 * Spo~z — onboarding
 * Licensed under GPL-3.0
 *
 * Three steps on the Spo~z backdrop:
 *   0. sign-in      — [GlossyLoginScreen] (the Spo~z loading + login screen),
 *   1. nickname     — "What should we call you?",
 *   2. community    — "You're all set" + the project's channels.
 *
 * Rewritten for Spo~z: no Glossy branding, no "Jay, Chikku & Matrix" credit,
 * and a deliberately light, flat look (no blur / glass) so it stays smooth on
 * low-end devices. The entry point keeps its original signature so the
 * navigation graph is untouched.
 */

package com.jay.glossy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.glossy.constants.CommunityChannel
import com.jay.glossy.constants.GuestNameKey
import com.jay.glossy.constants.InnerTubeCookieKey
import com.jay.glossy.constants.PendingCommunityIntroKey
import com.jay.glossy.ui.theme.SpozAccents
import com.jay.glossy.ui.theme.SpozPalette
import com.jay.glossy.utils.rememberPreference
import com.jay.glossy.utils.safeDataStoreEdit
import kotlinx.coroutines.launch

/** Placeholder handle — swap for the real one when provided. */
private const val SPOZ_HANDLE = "@spo_z"
private const val SPOZ_CREDIT = "Crafted with \u2764 by rawde_vvvvvv"

private const val STEP_INTRO = 0
private const val STEP_GUEST = 1
private const val STEP_COMMUNITY = 2

@Composable
fun GlossyWelcomeScreen(
    onSetupComplete: (String) -> Unit,
    onGoogleLoginClick: () -> Unit,
    onTokenClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (cookie) = rememberPreference(InnerTubeCookieKey, defaultValue = "")
    val (pendingCommunityIntro, setPendingCommunityIntro) =
        rememberPreference(PendingCommunityIntroKey, defaultValue = false)

    var step by rememberSaveable { mutableStateOf(STEP_INTRO) }
    var guestName by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableStateOf(false) }

    val completeOnce: (String) -> Unit = { name ->
        if (!completed) {
            completed = true
            onSetupComplete(name)
        }
    }

    LaunchedEffect(cookie, pendingCommunityIntro) {
        if (cookie.isBlank()) return@LaunchedEffect
        if (pendingCommunityIntro) {
            if (step == STEP_INTRO) step = STEP_COMMUNITY
        } else {
            completeOnce("Google User")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpozPalette.DarkBackground),
    ) {
        when (step) {
            STEP_INTRO -> GlossyLoginScreen(
                onGoogleClick = onGoogleLoginClick,
                onGuestClick = { step = STEP_GUEST },
                onTokenClick = onTokenClick,
            )

            STEP_GUEST -> SpozGuestStep(
                name = guestName,
                onNameChange = { guestName = it },
                onBack = { step = STEP_INTRO },
                onContinue = {
                    val name = guestName.trim()
                    if (name.isNotBlank()) {
                        scope.launch {
                            context.safeDataStoreEdit { prefs -> prefs[GuestNameKey] = name }
                            step = STEP_COMMUNITY
                        }
                    }
                },
            )

            else -> SpozCommunityStep(
                onNext = {
                    scope.launch {
                        if (pendingCommunityIntro) setPendingCommunityIntro(false)
                        completeOnce(guestName.trim().ifBlank { "Google User" })
                    }
                },
            )
        }
    }
}

@Composable
private fun SpozGuestStep(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
            .padding(top = 40.dp, bottom = 28.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SpozPalette.DarkSurface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2039", color = SpozPalette.DarkInk, fontSize = 24.sp)
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SpozAccents.Purple.copy(alpha = 0.22f))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Text("\u263A", color = SpozAccents.Purple, fontSize = 40.sp)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "What should we call you?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SpozPalette.DarkInk,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pick a nickname for your Spo~z profile. You can change it later in Settings.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = SpozPalette.DarkMuted,
        )

        Spacer(Modifier.height(26.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SpozPalette.DarkSurface)
                .border(1.dp, SpozAccents.Purple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            if (name.isEmpty()) {
                Text("Your name or nickname", color = SpozPalette.DarkMuted, fontSize = 16.sp)
            }
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(color = SpozPalette.DarkInk, fontSize = 16.sp),
                cursorBrush = SolidColor(SpozAccents.Purple),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))

        SpozPrimaryButton("Start listening", onContinue)

        Spacer(Modifier.weight(1f))
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
private fun SpozCommunityStep(onNext: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 72.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SpozAccents.Purple.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Spo~z", color = SpozAccents.Purple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "You're all set",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = SpozPalette.DarkInk,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Spo~z is built in the open. Follow along for release news, support and early builds.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = SpozPalette.DarkMuted,
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SpozPalette.DarkSurface),
        ) {
            CommunityChannel.entries.forEachIndexed { index, channel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runCatching { uriHandler.openUri(channel.url) } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(channel.brandColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = channel.iconRes),
                            contentDescription = channel.label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = channel.label,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SpozPalette.DarkInk,
                        )
                        Text(
                            text = stringResource(channel.descriptionRes),
                            fontSize = 12.sp,
                            color = SpozPalette.DarkMuted,
                        )
                    }
                    Text("\u203A", color = SpozPalette.DarkMuted, fontSize = 20.sp)
                }
                if (index != CommunityChannel.entries.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 70.dp)
                            .height(1.dp)
                            .background(SpozPalette.DarkMuted.copy(alpha = 0.2f)),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = SPOZ_HANDLE,
            fontSize = 14.sp,
            color = SpozPalette.DarkMuted,
        )

        Spacer(Modifier.weight(1f))
        SpozPrimaryButton("Next", onNext)
        Spacer(Modifier.height(12.dp))
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
private fun SpozPrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SpozAccents.Purple)
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
