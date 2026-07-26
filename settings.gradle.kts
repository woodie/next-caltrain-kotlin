pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Next Caltrain"
include(":app")

// Composite build, not pluginManagement -- kwick is a plain library
// (testImplementation), not a Gradle plugin like kotidy, so a regular
// dependency substitution in the main body is all Gradle needs to resolve
// app/build.gradle.kts's testImplementation("com.netpress:kwick:...")
// against the local checkout instead of Maven Central. No published
// artifact yet -- see that repo's own docs/COWORK.md "Packaging". Requires
// ../kwick to exist as a sibling checkout.
includeBuild("../kwick")
