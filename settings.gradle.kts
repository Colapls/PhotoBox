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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PhotoBox"

include(":app")
include(":core:ui")
include(":core:data")
include(":core:common")
include(":core:media")
include(":feature:onboarding")
include(":feature:stream")
include(":feature:profile")
include(":feature:settings")
include(":feature:day")