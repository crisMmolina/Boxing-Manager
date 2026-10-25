// BOXCENTRAL/ es un build independiente (ADR 0001): app central JavaFX + simulador de jueces.
rootProject.name = "BOXCENTRAL"

include(":judge-sim")

// Núcleo compartido como composite build, coordenada com.sigebox:core.
includeBuild("../core")
