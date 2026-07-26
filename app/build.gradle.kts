import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.netpress.kotidy") version "0.1.0"
}

// Schedule endpoint resolution, highest priority first:
//   1. local.properties (gitignored) — per-developer override, e.g. to point at a
//      local hang/instant-fail test server. Never committed.
//   2. config.properties (committed) — the real production URL. If the
//      schedule data ever moves to a new home, edit and commit this file directly;
//      no source edit needed.
//   3. Hardcoded literal below — last-resort safety net if both files are missing.
// See docs/COWORK.md "Schedule data".
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}
val defaultProperties = Properties().apply {
    val defaultsFile = rootProject.file("config.properties")
    if (defaultsFile.exists()) {
        defaultsFile.inputStream().use { load(it) }
    }
}
val scheduleUrl: String = localProperties.getProperty("scheduleUrl")
    ?: defaultProperties.getProperty("scheduleUrl")
    ?: "https://next-caltrain-pwa.appspot.com/feed/schedule.json"

// Release signing. Keystore path + passwords are per-developer secrets that live only
// in local.properties (gitignored) — never committed. If any of the four are missing,
// the release build type compiles unsigned, which is fine for local testing but will
// be rejected by Play Console on upload. See docs/CLAUDE.md "Release signing".
val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "com.netpress.nextcaltrain"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = "com.netpress.nextcaltrain"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SCHEDULE_URL", "\"$scheduleUrl\"")
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)
    // Composite build via settings.gradle.kts's includeBuild -- no published
    // artifact yet, see kwick's own docs/COWORK.md "Packaging".
    testImplementation("com.netpress:kwick:0.1.1")
    // Android's own org.json.JSONObject is stubbed to throw "not mocked" in plain JVM unit
    // tests (no Robolectric here). The real org.json:json artifact shadows that stub on the
    // test classpath so Schedule.fromJson's parsing actually runs in ScheduleSpec.
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
tasks.withType<Test> {
    useJUnitPlatform()
}

// Replaces the custom RSpec/ginkgo-fd-style TestListener that used to live
// directly in this file (also copy-pasted into humane-kotlin/huck) -- see
// kotidy's own docs/COWORK.md for why it was extracted into a real plugin
// instead of staying a hand-synced block, and settings.gradle.kts's
// includeBuild comment for the composite-build mechanism. "fs" is the
// closest existing style to what this project's output looked like before
// (checkmark + gray name for passes) -- not byte-identical, since the old
// ad hoc block's fail/skip glyphs didn't actually match any single named
// style; see kotidy's README for the real Mocha-spec-format shape this now
// renders instead.
kotidy {
    style = "fs"
}
