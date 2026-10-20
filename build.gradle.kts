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

    // Spo~z rebrand + performance, applied at build time so the huge source
    // files never have to be rewritten by hand.
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

            // 3. Performance: drop every .blur(...) modifier. Blur is a
            //    full-screen GPU pass per frame and is the single biggest cost
            //    on low-end devices.
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
            //    by hand.
            val spotifyShareActivity =
                buildString {
                    append("\n")
                    append("        <activity\n")
                    append("            android:name=\".spotify.SpotifyShareImportActivity\"\n")
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

            // 8. Share sheet: the app used to show TWO "Spo~z" targets, because
            //    the main activity already had its own ACTION_SEND (text/plain)
            //    "Share filter" and the new share-import activity added another.
            //    Drop the old one so there is a single, unambiguous Spo~z entry
            //    (the one that imports a Spotify playlist).
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
        }
    }
}
