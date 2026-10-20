plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.orionisli.ovl0ad3r"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.orionisli.ovl0ad3r"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("gz", "zst", "tgz")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("RELEASE_STORE_FILE") as? String
                ?: System.getenv("RELEASE_STORE_FILE")
            val keystoreFile = storeFilePath?.let { file(it) }

            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as? String
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as? String
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as? String
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}

val generateBootstrapWhiteout = tasks.register("generateBootstrapWhiteout") {
    description = "Automatically generate whiteout manifest for bootstrap package"
    val bootstrapArchive = file("src/main/assets/bootstrap/0vl0ad3r-bootstrap-aarch64.tar.zst")
    val whiteoutFile = file("src/main/assets/bootstrap/whiteout")
    inputs.file(bootstrapArchive)
    outputs.file(whiteoutFile)

    doLast {
        if (bootstrapArchive.exists()) {
            val process = ProcessBuilder(
                "sh", "-c",
                "zstd -dc '${bootstrapArchive.absolutePath}' | tar -tf - | grep -v '/\$' | sed 's|^\\./||' | sort -u"
            ).start()
            val entries = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            if (entries.isNotEmpty()) {
                val allEntries = (entries + "whiteout").distinct().sorted()
                whiteoutFile.writeText(allEntries.joinToString("\n") + "\n")
                logger.lifecycle("Bootstrap whiteout generated with ${allEntries.size} entries.")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(generateBootstrapWhiteout)
}
