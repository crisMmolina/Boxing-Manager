pluginManagement {
    repositories {
        google()
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

rootProject.name = "SIGEBOX"
include(":app")
// Núcleo compartido (ADR 0001): composite build con la coordenada com.sigebox:core.
includeBuild("core")
