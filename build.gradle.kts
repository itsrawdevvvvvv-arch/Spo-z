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
                val patched = text.replace(from, to)
                if (patched != text) f.writeText(patched)
            }

            // 1. Home top bar wordmark: "Glossy" -> "Spo~z".
            patch("src/main/kotlin/com/jay/glossy/MainActivity.kt", "\"Glossy\"", "\"Spo~z\"")

            // 2. Design-system accent: teal 1D9E75 -> Spo~z purple B79CFF.
            patch("src/main/kotlin/com/jay/glossy/ui/theme/GlossyDesign.kt", "1D9E75", "B79CFF")

            // 3. Performance: drop every .blur(...) modifier. Blur is a
            //    full-screen GPU pass per frame and is the single biggest cost
            //    on low-end devices; the app reads fine without it.
            patch("src/main/kotlin/com/jay/glossy/ui/component/LyricsImageCard.kt", ".blur(80.dp)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/component/LyricsLine.kt", ".blur(lineBlur)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/component/OriginalLyrics.kt", ".blur(lineBlur)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/MiniPlayer.kt", ".blur(GlowBlurRadius)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/Player.kt", ".blur(60.dp)", "")
            patch("src/main/kotlin/com/jay/glossy/ui/player/applemusic/NowPlayingContentAppleMusic.kt", ".blur(180.dp)", "")

            // 4. Remove the Equalizer entry from the player menu, so the feature
            //    is no longer reachable. PlayerMenu.kt is ~96 KB, far too large
            //    to rewrite, so the exact menu-item block is dropped here.
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
        }
    }
}
