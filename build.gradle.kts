plugins {
    alias(libs.plugins.hilt) apply (false)
    alias(libs.plugins.kotlin.ksp) apply (false)
    alias(libs.plugins.kotlin.serialization) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
    dependencies {
        classpath(libs.gradle)
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                arrayOf("reports", "metrics").forEach {
                    freeCompilerArgs.add("-P")
                    freeCompilerArgs.add("plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${project.layout.buildDirectory}/compose_metrics")
                }
            }
        }
    }

    // Spo~z rebrand + performance + fixes, applied at build time so the huge
    // source files never have to be rewritten by hand.
    tasks.matching { it.name == "preBuild" }.configureEach {
        doFirst {
            fun patch(relative: String, from: String, to: String) {
                val f = project.file(relative)
                if (!f.exists()) return
                val text = f.readText()
                // Idempotent: nothing to do if the anchor is gone (a replacement
                // already applied), or — for an insertion that keeps its anchor —
                // if the inserted text is already present.
                if (from.isNotEmpty() && !text.contains(from)) return
                if (to.contains(from) && text.contains(to)) return
                val patched = text.replace(from, to)
                if (patched != text) f.writeText(patched)
            }

            // 1. Home top bar wordmark: "Glossy" -> "Spo~z".
            patch("src/main/kotlin/com/jay/glossy/MainActivity.kt", "\"Glossy\"", "\"Spo~z\"")

            // 2. Design-system accent: teal 1D9E75 -> Spo~z purple B79CFF.
            patch("src/main/kotlin/com/jay/glossy/ui/theme/GlossyDesign.kt", "1D9E75", "B79CFF")

            // 3. Performance: drop every .blur(...) modifier.
            patch("src/main/kotlin/com/jay/glossy/ui/component/LyricsImageCard.kt", ".blur(80.dp)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/component/LyricsLine.kt", ".blur(lineBlur)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/component/OriginalLyrics.kt", ".blur(lineBlur)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt", ".blur(GlowBlurRadius)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/Player.kt", ".blur(60.dp)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/applemusic/NowPlayingContentAppleMusic.kt", ".blur(180.dp)", "")

            // 4. Remove the Equalizer entry from the player menu.
            patch(
                "src/main/kotlin/com/jay/glossy/ui/menu/PlayerMenu.kt",
                "                            Material3MenuItemData(\n" +
                    "                                icon = { PlayerMenuTonalTile(iconRes = R.drawable.equalizer) },\n" +
                    "                                title = { Text(text = stringResource(R.string.equalizer)) },\n" +
                    "                                description = { Text(text = stringResource(R.string.equalizer_desc)) },\n" +
                    "                                cardColors = menuRowColors(),\n" +
                    "                                onClick = {\n" +
                    "                                    navController.navigate(\"equalizer\")\n" +
                    "                                    onDismiss()\n" +
                    "                                },\n" +
                    "                            ),\n",
                "",
            )

            // 5. Performance: pin the Home header aurora wash static.
            patch(
                "src/main/kotlin/com/jay/glossy/ui/component/GreetingSection.kt",
                "if (rememberAmbientMotionEnabled()) {",
                "if (false) {",
            )

            // 6. Splash: install the system SplashScreen API on cold start.
            patch(
                "src/main/kotlin/com/jay/glossy/MainActivity.kt",
                "import androidx.activity.compose.setContent",
                "import androidx.activity.compose.setContent\n" +
                    "import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen",
            )
            patch(
                "src/main/kotlin/com/jay/glossy/MainActivity.kt",
                "        super.onCreate(savedInstanceState)",
                "        installSplashScreen()\n        super.onCreate(savedInstanceState)",
            )

            // 7. Spotify share import: declare the share/VIEW activity in the
            //    manifest by string replace, so the manifest is never rewritten
            //    by hand. The activity carries its own label so the share sheet
            //    says "Import from Spotify" instead of a second, identical
            //    "Spo~z" — you can always tell which entry imports a playlist.
            val spotifyShareActivity =
                buildString {
                    append("\n")
                    append("        <activity\n")
                    append("            android:name=\".spotify.SpotifyShareImportActivity\"\n")
                    append("            android:label=\"Import from Spotify\"\n")
                    append("            android:exported=\"true\"\n")
                    append("            android:excludeFromRecents=\"true\"\n")
                    append("            android:theme=\"@style/Theme.SpoZ\">\n")
                    append("            <intent-filter>\n")
                    append("                <action android:name=\"android.intent.action.SEND\" />\n")
                    append("                <category android:name=\"android.intent.category.DEFAULT\" />\n")
                    append("                <data android:mimeType=\"text/plain\" />\n")
                    append("            </intent-filter>\n")
                    append("            <intent-filter>\n")
                    append("                <action android:name=\"android.intent.action.VIEW\" />\n")
                    append("                <category android:name=\"android.intent.category.DEFAULT\" />\n")
                    append("                <category android:name=\"android.intent.category.BROWSABLE\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"open.spotify.com\" android:pathPrefix=\"/playlist/\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"open.spotify.com\" android:pathPrefix=\"/album/\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"open.spotify.com\" android:pathPrefix=\"/track/\" />\n")
                    append("            </intent-filter>\n")
                    append("            <intent-filter>\n")
                    append("                <action android:name=\"android.intent.action.VIEW\" />\n")
                    append("                <category android:name=\"android.intent.category.DEFAULT\" />\n")
                    append("                <category android:name=\"android.intent.category.BROWSABLE\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"spotify.link\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"www.spotify.link\" />\n")
                    append("                <data android:scheme=\"https\" android:host=\"spotify.app.link\" />\n")
                    append("            </intent-filter>\n")
                    append("            <intent-filter>\n")
                    append("                <action android:name=\"android.intent.action.VIEW\" />\n")
                    append("                <category android:name=\"android.intent.category.DEFAULT\" />\n")
                    append("                <category android:name=\"android.intent.category.BROWSABLE\" />\n")
                    append("                <data android:scheme=\"spotify\" android:host=\"playlist\" />\n")
                    append("                <data android:scheme=\"spotify\" android:host=\"album\" />\n")
                    append("                <data android:scheme=\"spotify\" android:host=\"track\" />\n")
                    append("            </intent-filter>\n")
                    append("        </activity>")
                }
            patch(
                "src/main/AndroidManifest.xml",
                "    </application>",
                spotifyShareActivity + "\n    </application>",
            )

            // 8. Share sheet: drop the main activity's own ACTION_SEND
            //    (text/plain) "Share filter" so there is a single share target.
            patch(
                "src/main/AndroidManifest.xml",
                "            <!-- Share filter -->\n" +
                    "            <intent-filter>\n" +
                    "                <action android:name=\"android.intent.action.SEND\" />\n" +
                    "\n" +
                    "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                    "\n" +
                    "                <data android:mimeType=\"text/plain\" />\n" +
                    "            </intent-filter>\n\n",
                "",
            )

            // 9. Custom playlist cover: open the gallery picker on a local
            //    playlist even when it has no custom cover yet.
            patch(
                "src/main/kotlin/com/jay/glossy/ui/screens/playlist/LocalPlaylistScreen.kt",
                "if (isCustomThumbnail) {",
                "if (isCustomThumbnail || playlist.playlist.browseId == null) {",
            )

            // 10. Recognise a custom cover saved under the current app id.
            patch(
                "src/main/kotlin/com/jay/glossy/ui/screens/playlist/LocalPlaylistScreen.kt",
                "it.contains(\"content://com.jay.glossy\")",
                "it.contains(\"content://\")",
            )

            // 11. Keep the custom cover: files dir, not the wipeable cache dir.
            patch(
                "src/main/kotlin/com/jay/glossy/ui/screens/playlist/LocalPlaylistScreen.kt",
                "context.cacheDir, \"playlist_cover_crop_",
                "context.filesDir, \"playlist_cover_crop_",
            )
        }
    }
}
