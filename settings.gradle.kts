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
        // GeckoView (Mozilla's Gecko engine) - the non-Chromium rendering engine.
        maven("https://maven.mozilla.org/maven2/") {
            content { includeGroup("org.mozilla.geckoview") }
        }
    }
}

// This project is deliberately self-contained: no shared design-system module, no build-logic,
// no convention plugins. Each app in the suite is isolated, so redundancy between them is accepted
// rather than factored out into a dependency none of them would then be free of.
rootProject.name = "Zuno"
include(":app")
