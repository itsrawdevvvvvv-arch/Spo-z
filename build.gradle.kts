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

    // Spo~z: rebrand the app wordmark at build time.
    //
    // The Home top bar's "Glossy" wordmark is one hardcoded string inside
    // MainActivity.kt, which is ~1900 lines long. Rewriting a file that size to
    // change one line is risky, so instead we patch the string here, right
    // before compilation. It only ever swaps the exact literal "Glossy" for
    // "Spo~z", and does nothing if the file is absent or already patched.
    tasks.matching { it.name == "preBuild" }.configureEach {
        doFirst {
            val source = project.file("src/main/kotlin/com/jay/glossy/MainActivity.kt")
            if (source.exists()) {
                val text = source.readText()
                val patched = text.replace("\"Glossy\"", "\"Spo~z\"")
                if (patched != text) source.writeText(patched)
            }
        }
    }
}
