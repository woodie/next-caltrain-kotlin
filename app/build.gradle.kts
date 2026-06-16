import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Schedule endpoint resolution, highest priority first:
//   1. local.properties (gitignored) — per-developer override, e.g. to point at a
//      local hang/instant-fail test server. Never committed.
//   2. schedule-endpoint.properties (committed) — the real production URL. If the
//      schedule data ever moves to a new home, edit and commit this file directly;
//      no source edit needed.
//   3. Hardcoded literal below — last-resort safety net if both files are missing.
// See docs/CLAUDE.md "Schedule data".
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}
val defaultProperties = Properties().apply {
    val defaultsFile = rootProject.file("schedule-endpoint.properties")
    if (defaultsFile.exists()) {
        defaultsFile.inputStream().use { load(it) }
    }
}
val scheduleUrl: String = localProperties.getProperty("scheduleUrl")
    ?: defaultProperties.getProperty("scheduleUrl")
    ?: "https://next-caltrain-pwa.appspot.com/schedule.json"

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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SCHEDULE_URL", "\"$scheduleUrl\"")
    }
    buildTypes {
        release {
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
