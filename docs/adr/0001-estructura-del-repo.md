# ADR 0001 · Estructura del repo: dos apps y un núcleo compartido
- Status: accepted · Date: 2026-10-24 · Decider: cris

## Context
IDEA.md §0 propone un monorepo Gradle `core/`, `desktop/`, `android/`, `tools/`, `docs/`. El equipo ya creó la app Android en la raíz (`app/`, proyecto de Android Studio) y la carpeta `BOXCENTRAL/` para la app de escritorio. La central necesita JDK 25 + JavaFX 25 (Gradle 9.1+), y Android usa AGP con su propia versión de Gradle. Zoe trabaja en Windows sin Android SDK; Cris en macOS con Android Studio.

## Decision
- `app/` (build raíz, `settings.gradle.kts` de la raíz) = app del juez Android, paquete `com.sigebox.juez`.
- `BOXCENTRAL/` = build Gradle independiente (wrapper propio) de la app central JavaFX. Incluye `BOXCENTRAL/judge-sim/` como subproyecto (simulador de jueces por línea de comandos).
- `core/` = build Gradle independiente (wrapper propio), Java 17 puro, sin dependencias de Android ni JavaFX. Contiene modelo, protocolo (DTO), validación y cálculo del resultado, y los JSON Schema en `core/schemas/`.
- Ambas apps consumen `core` como *composite build*: `includeBuild("core")` en la raíz y `includeBuild("../core")` en `BOXCENTRAL/`, con la coordenada `com.sigebox:core`.
- Datos de demo en `demo_data/`. Documentación en `docs/`.
- Desviación de IDEA.md: los esquemas viven en `core/schemas/` (no en `app/schemas/`, porque `app/` es el módulo Android).

## Alternatives
- Un solo build Gradle con `:android`, `:desktop`, `:core`: obliga a Zoe a instalar el Android SDK y ata la versión de Gradle de JavaFX a la de AGP.
- Copiar `core` en cada app: dos implementaciones del cálculo, rompe el principio 2.

## Consequences
Cada humano compila solo lo suyo más `core`. Un cambio en `core` afecta a las dos apps: va por PR con contrato (`uak msg impact:ID --kind contract`). Para revertir, se puede convertir en un solo build sin mover código.
