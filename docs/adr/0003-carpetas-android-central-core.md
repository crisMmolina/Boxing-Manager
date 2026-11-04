# ADR 0003 · Carpetas hermanas: `android/`, `central/` y `core/`
- Status: accepted · Date: 2026-10-29 · Decider: cris · Supersedes: la parte de carpetas de ADR 0001

## Context
ADR 0001 dejó la app del juez como build raíz (`settings.gradle.kts` en la raíz con `:app`) y la central en `BOXCENTRAL/`. Así la central y `core/` quedan dentro del proyecto de Android: Android Studio abre todo el repo, la raíz mezcla archivos de Gradle de Android con los de las otras dos apps y no se ve a primera vista qué carpeta es de quién.

## Decision
- El proyecto de Android completo (`settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew*`, `gradle/` con `libs.versions.toml`, y `app/`) pasa a `android/`. Android Studio abre `android/`.
- `BOXCENTRAL/` pasa a `central/` (incluye `judge-sim/`). Se conserva el nombre de la app "SIGEBOX Central" en el instalador.
- `core/`, `demo_data/`, `docs/` y `tools/` se quedan en la raíz.
- Cada app sigue consumiendo `core` como composite build: `includeBuild("../core")` en `android/` y en `central/`, coordenada `com.sigebox:core`.
- Paquetes Java, protocolo y datos no cambian; solo se mueven carpetas.

## Alternatives
- Dejar todo como está: es lo que se quiere corregir.
- Mover solo Android y conservar `BOXCENTRAL/`: menos cambios, pero nombres inconsistentes (`android/` y `BOXCENTRAL/`).
- Un solo build con los tres módulos: ya se descartó en ADR 0001 (zoe sin Android SDK, versiones de Gradle distintas).

## Consequences
- Cambian rutas en CI/smoke (`tools/smoke.sh`), `.github/CODEOWNERS`, `.uak/OWNERS.md`, `.uak/PROJECT.md`, README y docs. Los `Paths` de tareas ya integradas en `.uak/TASKS.md` se quedan como estaban (historial).
- Cada persona copia su `local.properties` (git-ignored) a `android/` y vuelve a abrir el proyecto en Android Studio desde `android/`. zoe usa `central/gradlew -p central …` en lugar de `BOXCENTRAL/gradlew -p BOXCENTRAL …`.
- `.idea/` de la raíz deja de servir para Android; no se mueve en este cambio.
- Revertir: mover las carpetas de vuelta; no hay cambios de código ni de datos.
