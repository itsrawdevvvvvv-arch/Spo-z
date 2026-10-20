/**
 * Spo~z — About screen
 * Licensed under GPL-3.0
 *
 * Rewritten for Spo~z: no Glossy wordmark, no "Jay, Chikku & Matrix" credits,
 * and a light, flat layout. The original project is still credited in one line
 * (GPL-3.0 requires attribution) and the licence is one tap away.
 *
 * Entry point keeps its original signature so the navigation graph is untouched.
 */

package com.jay.glossy.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jay.glossy.BuildConfig
import com.jay.glossy.ui.theme.SpozAccents

@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(SpozAccents.Purple, SpozAccents.Blue))),
            contentAlignment = Alignment.Center,
        ) {
            Text("Spo~z", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "Spo~z",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Crafted with \u2764 by rawde_vvvvvv",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        AboutRow("Licenses (GPL-3.0)") {
            runCatching { uriHandler.openUri("https://github.com/itsrawdevvvvvv-arch/Spo-z/blob/main/LICENSE") }
        }
        Spacer(Modifier.height(10.dp))
        AboutRow("View source") {
            runCatching { uriHandler.openUri("https://github.com/itsrawdevvvvvv-arch/Spo-z") }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Based on the open-source Glossy / Metrolist project.",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AboutRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
