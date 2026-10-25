// BOXCENTRAL/ es un build independiente (ADR 0001): app central JavaFX + simulador de jueces.
plugins {
    // Descarga el JDK 25 del toolchain si la máquina no lo tiene (CI usa 21).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "BOXCENTRAL"

include(":judge-sim")

// Núcleo compartido como composite build, coordenada com.sigebox:core.
includeBuild("../core")
