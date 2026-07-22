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

    // No published artifact for kotidy yet -- this is a Gradle composite
    // build, not a version dependency, and specifically needs to be included
    // from inside pluginManagement (not the main settings.gradle.kts body,
    // unlike a regular includeBuild) since it supplies a plugin ID rather
    // than a library dependency. Requires kotidy checked out as a sibling
    // directory (../kotidy relative to this file). See its own docs/COWORK.md.
    includeBuild("../kotidy")
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
