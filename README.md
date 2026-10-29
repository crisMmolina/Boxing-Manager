# SIGEBOX · Tarjetas de box sin papel ni internet

Los jueces califican cada round en su teléfono Android y la central (una laptop con Windows) recibe todo en ≤ 1 s por una red local sin router ni internet: el hotspot de un teléfono. Al cerrar la pelea, la central calcula el resultado y exporta PDF/CSV.

| Carpeta | Qué es |
|---|---|
| `core/` | Modelo, protocolo WebSocket/JSON y cálculo del resultado. Java puro, compartido por las dos apps |
| `android/` | App del juez (Android, Java + vistas XML, Material 3) |
| `central/` | Central del organizador (JavaFX 25, Javalin, SQLite) y `judge-sim/`, el simulador de jueces |
| `demo_data/` | Pelea de referencia con el resultado calculado a mano |
| `docs/` | `DESIGN.md`, specs, ADR, spikes, `runbook.md` (día del evento) y `demo.md` |

## Requisitos

- **JDK 25** para `core` y la central. Si `JAVA_HOME` apunta a otro JDK, ajústalo antes de compilar, p. ej. en Git Bash: `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5"`.
- **Android SDK** (Android Studio) para la app. Crea `local.properties` con `sdk.dir=...` si Android Studio no lo hizo.
- Los wrappers de Gradle descargan el resto la primera vez (eso sí pide internet).

## Compilar y probar

### core

```bash
core/gradlew -p core build
```

### App del juez

```bash
android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest
```

La APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`. Instalar: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk` (más opciones en `docs/runbook.md` §3).

### Central

```bash
central/gradlew -p central build
```

Correrla desde el código:

```bash
central/gradlew -p central run
```

Escucha en `0.0.0.0:8765`. La prueba de integración `com.sigebox.central.it.LiveFightTest` (5 jueces × 12 rounds, corte de 60 s y reinicio del servidor) tarda ≈ 1 min.

### Empaquetar la central para Windows

```bash
central/gradlew -p central jpackage -PinstallerType=app-image
```

Genera `central/build/jpackage/SIGEBOX Central/` con su propio Java: se copia a la laptop y se abre `SIGEBOX Central.exe`. Para un `.msi` (opción por defecto, `jpackage` sin `-PinstallerType`) hace falta **WiX** v3 (`candle`/`light`) o v4+ (`wix`) en el `PATH` de quien construye; quien instala no necesita nada. El `.msi` todavía no se ha generado en este proyecto (ver `docs/spikes/DSK-002.md` §3).

### Simulador de jueces

```bash
central/gradlew -p central :judge-sim:installDist
central/judge-sim/build/install/judge-sim/bin/judge-sim --judges 3 --script demo_data/pelea_referencia.json --tokens CODIGO1,CODIGO2,CODIGO3 --fight ID_PELEA --host IP_CENTRAL
```

- `--tokens`: el código de 6 dígitos de cada lugar que muestra la central, en orden de lugar.
- `--fight`: el id de la pelea. La central lo muestra como **Código de la pelea** en la tarjeta Pelea (botón **Copiar** al lado).
- Opcionales: `--port 8765`, `--drop 60s` (corta al juez 1 tras su primer envío), `--timeout 30m`. Al terminar imprime la latencia envío → ACK (p50/p95).

### Smoke de todo

```bash
bash tools/smoke.sh
```

## Documentos

- `docs/runbook.md`: montar el hotspot, instalar sin internet, firewall, qué hacer si falla, volver a papel.
- `docs/demo.md`: los 6 pasos de la demo y el plan B.
- `docs/DESIGN.md`: guía visual v2 de las dos apps.
- `docs/specs/001-sigebox/`: spec y plan. `docs/adr/`: decisiones de arquitectura.
