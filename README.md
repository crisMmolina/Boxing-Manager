# SIGEBOX · Tarjetas de box sin papel ni internet

Los jueces califican cada round en su teléfono Android y la central (una laptop con Windows) recibe todo en ≤ 1 s por una red local sin router ni internet: el hotspot de un teléfono. Al cerrar la pelea, la central calcula el resultado y exporta PDF/CSV.

| Carpeta | Qué es |
|---|---|
| `core/` | Librería Java compartida por las dos apps |
| `android/` | App del juez (Android) |
| `central/` | Central de escritorio (JavaFX, Windows) y simulador de jueces (`central/judge-sim`) |

Además, `demo_data/` trae la pelea de referencia del simulador y `docs/` las specs, el diseño y las guías.

## Requisitos

- JDK 25.
- Android SDK, para la app del juez.

Los comandos son para Git Bash y se corren desde la raíz del repo. Cada proyecto trae su Gradle Wrapper, que se descarga la primera vez.

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
```

## Construir y probar

Cada comando compila y corre las pruebas de su proyecto.

```bash
core/gradlew -p core build
android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest
central/gradlew -p central build
```

La APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`. La prueba `LiveFightTest` de la central tarda cerca de un minuto cuando Gradle la corre.

Los tres proyectos de una vez:

```bash
bash tools/smoke.sh
```

## Correr las apps

**Central.** Escucha a los jueces en `0.0.0.0:8765`.

```bash
central/gradlew -p central run
```

**App del juez.** Con un teléfono conectado y la depuración USB activa:

```bash
"$ANDROID_HOME/platform-tools/adb" install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## Simulador de jueces

Hace de teléfonos: se une con los códigos de la pantalla **Jueces** y juega un guion de pelea contra la central. Cada código es un juez, de 1 a 5.

```bash
central/gradlew -p central :judge-sim:installDist
central/judge-sim/build/install/judge-sim/bin/judge-sim --script demo_data/pelea_referencia.json --codes CODIGO1,CODIGO2,CODIGO3
```

Cambia `CODIGO1,CODIGO2,CODIGO3` por el código de 6 dígitos de cada lugar. Luego aprueba a cada juez en **Jueces**, inicia la pelea y maneja los rounds desde **En vivo**. Al terminar imprime la latencia envío→ACK (p50 y p95).

Opciones: `--host` (por defecto `localhost`), `--port` (por defecto `8765`), `--drop 60s` (el juez 1 pierde la conexión ese tiempo tras su primera tarjeta; debe volver antes de que termine la pelea, o no manda su tarjeta final) y `--timeout 30m`.

## Empaquetar la central

```bash
central/gradlew -p central jpackage -PinstallerType=app-image
```

Deja la app en `central/build/jpackage/SIGEBOX Central/`, con su propio Java; se abre con `SIGEBOX Central.exe`. El instalador `.msi` necesita WiX y aún no se ha generado: ver `docs/spikes/DSK-002.md` §3.

## Guía corta del evento

1. **Inicio** → **Nuevo evento**: rounds por pelea, jueces y número de peleas.
2. **Evento**: arma la cartelera y pulsa **Iniciar pelea**.
3. **Jueces**: cada juez se une con su QR o su código; apruébalos e inicia la pelea.
4. **En vivo**: abre y cierra cada round. Aquí también se aplican los descuentos.
5. **Resultado**: al cerrar el último round llegan las tarjetas finales; cierra la pelea y sigue con la siguiente.

La red, los descuentos y qué hacer si algo falla están en el [runbook](docs/runbook.md).

## Documentos

- [Runbook del evento](docs/runbook.md)
- [Ensayo y demo](docs/demo.md)
- [Diseño](docs/DESIGN.md)
- [Spec 001](docs/specs/001-sigebox/spec.md) y [spec 004](docs/specs/004-eventos/spec.md)
- [Decisiones de arquitectura](docs/adr/)
