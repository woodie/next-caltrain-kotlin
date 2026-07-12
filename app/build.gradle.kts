import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
tasks.withType<Test> {
    useJUnitPlatform()

    // Custom RSpec/ginkgo-fd-style console reporter, replacing gradle-test-logger-plugin.
    // The plugin's mocha theme gave genuine nested indentation with checkmarks (the
    // shape we want) but inserts a blank line between every describe/context group,
    // hardcoded into its theme with no config flag to disable. This hooks Gradle's own
    // TestListener API directly -- the same mechanism the plugin itself uses under the
    // hood -- to walk the real nested TestDescriptor.parent chain and print a dense
    // tree with no blank-line padding. No final summary line of our own -- Gradle's
    // own "BUILD SUCCESSFUL"/"BUILD FAILED" already closes out the run.
    //
    // Gradle's tree has two synthetic wrapper suites above the real top-level describe()
    // ("Gradle Test Run :app:..." and "Gradle Test Executor N"); ancestry() filters those
    // out by name prefix, which is the standard trick for custom Gradle test listeners.
    var lastPath: List<String> = emptyList()

    // Respect the NO_COLOR convention (https://no-color.org/) for anyone piping
    // this into a log file or a terminal that mangles escape codes.
    val colorEnabled = System.getenv("NO_COLOR") == null
    val RESET = "[0m"
    val GREEN = "[32m"
    val RED = "[31m"
    val CYAN = "[36m"
    val GRAY = "[90m"
    fun ansi(code: String, text: String) = if (colorEnabled) "$code$text$RESET" else text

    fun ancestry(descriptor: TestDescriptor): List<String> {
        val names = mutableListOf<String>()
        var d = descriptor.parent
        while (d != null) {
            if (!d.name.startsWith("Gradle Test")) names.add(0, d.name)
            d = d.parent
        }
        return names
    }

    // Reset dedupe state at actual task-execution time, not here at
    // configuration time. With org.gradle.configuration-cache=true (see
    // gradle.properties), a cache hit skips re-running this whole
    // tasks.withType<Test> block, so state captured directly in a `val`
    // above would carry over stale from whenever the cache entry was first
    // written. doFirst always re-runs on every invocation regardless of
    // config-cache state, so this is the one place safe to reset from.
    doFirst {
        lastPath = emptyList()
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            val ancestors = ancestry(testDescriptor)
            val path = ancestors + testDescriptor.name

            // Print only the part of the path not already printed for the previous
            // test -- the "dedupe shared prefix" trick that produces a real nested
            // tree from a flat stream of leaf-test callbacks, with no blank lines.
            val shared = path.zip(lastPath).takeWhile { (a, b) -> a == b }.count()
            for (depth in shared until ancestors.size) {
                // depth == 0 here means ancestors[0] -- the fully-qualified spec class
                // name (e.g. com.netpress.nextcaltrain.CaltrainScheduleSpec) -- is about
                // to be printed for a new top-level suite. A blank line goes before every
                // one of those, unconditionally (including the first), so each suite's
                // block visually stands apart from whatever came before it.
                if (depth == 0) println()
                println("  ".repeat(depth) + ancestors[depth])
            }

            // Mocha's own spec reporter colors the checkmark green and dims the title
            // for passes; failures and pending get a single solid color instead.
            val line = when (result.resultType) {
                TestResult.ResultType.SUCCESS ->
                    "${ansi(GREEN, "✔")} ${ansi(GRAY, testDescriptor.name)}"

                TestResult.ResultType.SKIPPED ->
                    ansi(CYAN, "○ ${testDescriptor.name}")

                else ->
                    ansi(RED, "✖ ${testDescriptor.name}")
            }
            println("  ".repeat(ancestors.size) + line)
            if (result.resultType == TestResult.ResultType.FAILURE) {
                result.exceptions.forEach { e ->
                    println("  ".repeat(ancestors.size + 1) + ansi(RED, e.message ?: e.toString()))
                }
            }

            lastPath = path
        }
    })
}
