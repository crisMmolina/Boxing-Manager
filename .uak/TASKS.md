# TASKS · SIGEBOX v1 (spec `docs/specs/001-sigebox/`)

State lives on the `claims` branch (`uak status`); this file holds only tasks.
Prefijos: CORE (núcleo compartido `core/`), AND (app del juez `app/`, humano cris), DSK (central `BOXCENTRAL/`, humano zoe), QA.
Cada tarea empieza con una prueba que falla. Contratos en `core/schemas/` y `core/src/main/java/com/sigebox/core/protocol/`.
En Windows (zoe) los comandos `Verify` se corren en Git Bash.

## CORE-001 · Build de core y CI

- **Type:** setup
- **Priority:** P0
- **Estimate:** 1 h
- **Rubric / Spec:** ADR 0001 · `docs/specs/001-sigebox/plan.md`
- **Depends on:** None
- **Paths:** core/build.gradle.kts, core/settings.gradle.kts, core/gradle.properties, core/gradle/, core/gradlew, core/gradlew.bat, core/src/test/java/com/sigebox/core/SmokeTest.java, .github/workflows/ci.yml
- **Freeze-Allowed:** no
- **Acceptance:**
  - `core/` es un build Gradle independiente (wrapper Gradle 9.1+ propio), `java-library`, `group = com.sigebox`, `release = 17`, sin dependencias de Android ni JavaFX
  - Dependencias declaradas de una vez: Gson (JSON), JUnit 5, networknt json-schema-validator (solo test); JaCoCo con verificación de 80 % de líneas
  - CI de GitHub Actions corre `core/gradlew -p core build` en ubuntu y windows
- **Verify:** `core/gradlew -p core build`
- **Next step:** copiar el wrapper de Gradle a `core/` y crear `core/settings.gradle.kts` con `rootProject.name = "core"`

## CORE-002 · Contrato: modelo, mensajes WS y JSON Schema

- **Type:** contract
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** IDEA §6 · plan.md §Contracts
- **Depends on:** CORE-001
- **Paths:** core/src/main/java/com/sigebox/core/model/, core/src/main/java/com/sigebox/core/protocol/, core/schemas/, core/src/test/java/com/sigebox/core/protocol/, core/src/test/resources/, demo_data/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Records Java para Pelea, LugarJuez, Round, TarjetaRound, VersionTarjeta, TarjetaFinal, Terminacion, Resultado, EventoBitacora (IDEA §6) y enums de estado
  - Un DTO por mensaje (`JOIN` … `PONG`), todos con `v`, `id` (UUID) y `type`; `Messages.encode/decode` con Gson; `JoinUri` parsea y arma `sigebox://join?h=&p=&s=&seat=&t=`
  - Un `*.schema.json` por mensaje en `core/schemas/`; una prueba valida que cada DTO serializado pasa su Schema y que un mensaje con campo extra o faltante falla
  - `demo_data/pelea_referencia.json` con la pelea de IDEA §3 (resultado esperado: Roja, dividida 2–1) y fixtures de mensajes de ejemplo
- **Verify:** `core/gradlew -p core test`
- **Next step:** escribir `ProtocolSchemaTest` con un `SUBMIT_ROUND` de ejemplo que todavía no compila

## CORE-003 · Validación y cálculo del resultado

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** RC-xx · IDEA §5 · criterio "Resultado igual al cálculo a mano"
- **Depends on:** CORE-002
- **Paths:** core/src/main/java/com/sigebox/core/validation/, core/src/main/java/com/sigebox/core/scoring/, core/src/test/java/com/sigebox/core/validation/, core/src/test/java/com/sigebox/core/scoring/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `ScoreValidator`: solo enteros 0–10 por esquina, sin valor por defecto; rechaza juez/round/pelea que no corresponden a la sesión
  - `ResultCalculator`: total por juez, ganador por juez (igual = empate), resultado final por mayoría (igual = empate), tipo unánime / mayoritaria / dividida; terminación extraordinaria da el resultado por motivo; decisión técnica con rounds cerrados; cuenta enviadas, tardías, capturadas y corregidas (última versión)
  - Pruebas: 1 a 5 jueces, empates, cada tipo de decisión, cada motivo de terminación, pelea de referencia (`demo_data/pelea_referencia.json` → Roja dividida 2–1) y 12 rounds × 5 jueces contra cálculo a mano
  - Cobertura de `core` ≥ 80 %
- **Verify:** `core/gradlew -p core test jacocoTestCoverageVerification`
- **Next step:** `ResultCalculatorTest.peleaDeReferencia()` leyendo `demo_data/pelea_referencia.json`

## CORE-004 · Entradas de las pruebas de core y plan con CHALLENGE

- **Type:** fix
- **Priority:** P1
- **Estimate:** 30 min
- **Rubric / Spec:** follow-up de CORE-001 y CORE-002 (revisión de seguridad) · `docs/specs/001-sigebox/plan.md`
- **Depends on:** CORE-001, CORE-002
- **Paths:** core/build.gradle.kts, docs/specs/001-sigebox/plan.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - `tasks.test` declara `core/schemas/` y `demo_data/` como entradas (ruta relativa): cambiar o romper un fixture o un Schema vuelve a correr las pruebas en lugar de quedar UP-TO-DATE o salir de la caché
  - `plan.md` §Contracts lista los 16 mensajes (con `CHALLENGE`) y la fila Spoofing del STRIDE describe la llave EC P-256 del teléfono, el nonce por conexión y `DeviceProof`
- **Verify:** `core/gradlew -p core build`
- **Next step:** romper `demo_data/mensajes/PING.json` y comprobar que `core/gradlew -p core test` ya no queda UP-TO-DATE

## AND-001 · Setup de la app del juez (Android)

- **Type:** setup
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** ADR 0001 · IDEA §9 (stack juez)
- **Depends on:** CORE-001
- **Paths:** build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/, gradlew, gradlew.bat, app/build.gradle.kts, app/proguard-rules.pro, app/src/main/AndroidManifest.xml, app/src/main/java/com/example/, app/src/main/java/com/sigebox/juez/MainActivity.java, app/src/main/java/com/sigebox/juez/SigeboxApp.java, app/src/main/res/values/strings.xml, app/src/main/res/values/themes.xml, app/src/main/res/values/colors.xml, app/src/main/res/values-night/, app/src/main/res/layout/activity_main.xml, app/src/test/java/com/example/, app/src/test/java/com/sigebox/juez/SmokeTest.java, app/src/androidTest/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Paquete y `applicationId` `com.sigebox.juez` (se borra `com.example.box`), minSdk 26, compile/targetSdk actuales, Java 17, Gradle y AGP actualizados
  - `settings.gradle.kts` hace `includeBuild("core")`; `app` depende de `com.sigebox:core`
  - Dependencias declaradas de una vez: Material, OkHttp, Room (+ annotationProcessor), CameraX, ML Kit Barcode Scanning (modelo incluido), JUnit, Robolectric/Mockito para pruebas locales
  - El Manifest declara permisos (INTERNET, CAMERA, VIBRATE, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, ACCESS_WIFI_STATE), `usesCleartextTraffic` solo para LAN y de antemano todas las Activities: `TermsActivity`, `JoinActivity`, `ScanActivity`, `WaitingActivity`, `ScoreActivity`, `LateScoreActivity`, `FinalCardActivity` (clases vacías creadas por cada tarea dueña)
  - Strings por feature en archivos separados (`strings_join.xml`, `strings_score.xml`, …); `values/strings.xml` solo con `app_name` = SIGEBOX Juez
- **Verify:** `./gradlew :app:assembleDebug :app:testDebugUnitTest`
- **Next step:** actualizar el wrapper de Gradle y AGP, luego renombrar el paquete en `app/build.gradle.kts`

## DSN-001 · Guía visual de las dos apps

- **Type:** contract
- **Priority:** P0
- **Estimate:** 1 h
- **Rubric / Spec:** criterios "Sin totales visibles", "Emparejar en < 2 min" · IDEA §0 (sin logotipo, espacio reservado)
- **Depends on:** None
- **Paths:** docs/DESIGN.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Paleta (rojo/azul de esquina accesibles en sol y en arena oscura, contraste AA), tipografía, tamaños mínimos de toque (≥ 64 dp en el teclado del juez)
  - Bocetos en texto de cada pantalla: juez (términos, unión, espera, puntuación, tardío, tarjeta final) y central (crear pelea, aprobación, rounds, terminación, resultado, bitácora)
  - Textos en español (México) para estados: Pendiente, Enviado, Tardío, Corregido, Conectado, Esperando aprobación
- **Verify:** `test -s docs/DESIGN.md`
- **Next step:** escribir la tabla de colores y el boceto de `ScoreActivity`

## AND-002 · Spike: hotspot sin internet y bindProcessToNetwork

- **Type:** spike
- **Priority:** P0
- **Estimate:** 30 min
- **Rubric / Spec:** IDEA §7 (red local sin router, mantener Wi‑Fi sin internet)
- **Depends on:** None
- **Paths:** docs/spikes/AND-002.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Documenta si con el hotspot de un Galaxy A37 (sin SIM/datos) la laptop y 2 teléfonos se ven entre sí (ping + WebSocket de prueba al puerto 8765)
  - Documenta si `ConnectivityManager.bindProcessToNetwork` mantiene el socket en Android 16 cuando la red no tiene internet
  - Si falla: respaldo elegido (router de viaje) registrado con `uak decision`
- **Verify:** `test -s docs/spikes/AND-002.md`
- **Next step:** activar el hotspot y probar `ping` desde la laptop al teléfono cliente

## AND-003 · Cliente WebSocket, bandeja de salida y reconexión

- **Type:** feature
- **Priority:** P0
- **Estimate:** 5 h
- **Rubric / Spec:** criterio "Ninguna calificación enviada se pierde" · IDEA §5 confiabilidad
- **Depends on:** AND-001, CORE-002
- **Paths:** app/src/main/java/com/sigebox/juez/net/, app/src/main/java/com/sigebox/juez/data/, app/src/test/java/com/sigebox/juez/net/, app/src/test/java/com/sigebox/juez/data/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `JudgeConnection` (OkHttp WebSocket) con reconexión automática y backoff; al reconectar envía `HELLO` y aplica `STATE`
  - Room `outbox`: cada mensaje se guarda antes de enviarse y se reenvía hasta recibir `ACK` con su `id`; sobrevive a cerrar la app a la fuerza
  - `bindProcessToNetwork` a la red Wi‑Fi del hotspot (según AND-002); `PING/PONG` para detectar caída
  - Expone a la UI un `LiveData`/listener de estado (conectado, esperando aprobación, round abierto/cerrado, pendiente de ACK)
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de `Outbox` que encola un `SUBMIT_ROUND` y lo marca acked solo al recibir su `ACK`

## AND-004 · Términos, unión por QR o código y espera de aprobación

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** RJ-01, RJ-12 · demo paso 2 · criterio "Emparejar 3 jueces en < 2 min"
- **Depends on:** AND-001, CORE-002
- **Uses contract:** AND-003, DSN-001
- **Paths:** app/src/main/java/com/sigebox/juez/join/, app/src/main/java/com/sigebox/juez/terms/, app/src/main/res/layout/activity_terms.xml, app/src/main/res/layout/activity_join.xml, app/src/main/res/layout/activity_scan.xml, app/src/main/res/layout/activity_waiting.xml, app/src/main/res/values/strings_join.xml, app/src/test/java/com/sigebox/juez/join/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Primer uso: términos y aviso de privacidad simplificado; sin aceptar no se continúa
  - `ScanActivity` (CameraX + ML Kit, sin internet) lee `sigebox://join?...`; alternativa: código de 6 dígitos + host
  - El juez escribe su nombre → `JOIN` → "Esperando aprobación" → con `JOIN_APPROVED` abre `ScoreActivity`; `JOIN_REJECTED` muestra el motivo
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de `JoinUri` → `JoinRequest` con nombre vacío rechazado

## AND-005 · Pantalla de puntuación, corrección y tarjeta final

- **Type:** feature
- **Priority:** P0
- **Estimate:** 5 h
- **Rubric / Spec:** RJ-05 · demo pasos 3 y 6 · criterios "Solo enteros 0–10" y "Sin totales visibles"
- **Depends on:** AND-003, CORE-003
- **Paths:** app/src/main/java/com/sigebox/juez/score/, app/src/main/java/com/sigebox/juez/finalcard/, app/src/main/res/layout/activity_score.xml, app/src/main/res/layout/activity_final_card.xml, app/src/main/res/values/strings_score.xml, app/src/test/java/com/sigebox/juez/score/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Teclado numérico grande para una mano: Roja y Azul 0–10, sin valor por defecto; "Enviar round" deshabilitado hasta tener ambos válidos (`ScoreValidator` de core)
  - Muestra el round actual y el estado (enviado / pendiente de ACK); nunca totales ni ganador
  - Con el round abierto el juez puede corregir; cada corrección es un `SUBMIT_ROUND` nuevo con versión
  - Al final, `FinalCardActivity` muestra solo su propia tarjeta y envía `SUBMIT_FINAL_CARD`
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de `ScoreViewModel`: con solo Roja = 10 el botón Enviar está deshabilitado

## AND-006 · Round cerrado sin calificar: vibración y calificación obligatoria

- **Type:** feature
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** demo paso 4 · criterio "Round sin calificar: vibración y calificación exigida"
- **Depends on:** AND-005
- **Paths:** app/src/main/java/com/sigebox/juez/late/, app/src/main/res/layout/activity_late_score.xml, app/src/main/res/values/strings_late.xml, app/src/test/java/com/sigebox/juez/late/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al recibir `ROUND_CLOSED` de un round que el juez no envió: vibra y abre `LateScoreActivity` a pantalla completa, sin poder salir hasta enviar
  - El envío va marcado `late: true` y pasa por la bandeja de salida
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba: `ROUND_CLOSED` del round 3 sin envío propio → se pide `LateScoreActivity`

## DSK-001 · Setup de la central (BOXCENTRAL, JavaFX)

- **Type:** setup
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** ADR 0001 · IDEA §9 (stack central)
- **Depends on:** CORE-001
- **Paths:** BOXCENTRAL/build.gradle.kts, BOXCENTRAL/settings.gradle.kts, BOXCENTRAL/gradle.properties, BOXCENTRAL/gradle/, BOXCENTRAL/gradlew, BOXCENTRAL/gradlew.bat, BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/Screens.java, BOXCENTRAL/src/main/resources/com/sigebox/central/app.css, BOXCENTRAL/src/test/java/com/sigebox/central/SmokeTest.java, BOXCENTRAL/judge-sim/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - Build Gradle independiente (wrapper 9.1+), toolchain JDK 25, plugin `org.openjfx.javafxplugin` con JavaFX 25, `application` con `mainClass = com.sigebox.central.App`
  - `settings.gradle.kts` hace `includeBuild("../core")` e incluye el subproyecto `judge-sim`
  - Dependencias declaradas de una vez: Javalin 7, sqlite-jdbc, ZXing (core + javase), OpenPDF, SLF4J simple, JUnit 5, TestFX (opcional)
  - Tarea `jpackage` configurada (se ejecuta solo en Windows) para .msi/.exe con runtime incluido
  - `App` abre una ventana "SIGEBOX · Central" con navegación por `Screens` (registro de pantallas que agregan las demás tareas)
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** crear `BOXCENTRAL/settings.gradle.kts` con `includeBuild("../core")`

## DSK-002 · Spike: servidor WebSocket en Windows 11 + firewall + jpackage

- **Type:** spike
- **Priority:** P0
- **Estimate:** 30 min
- **Rubric / Spec:** IDEA §7 (servidor local, instalador sin Java)
- **Depends on:** None
- **Paths:** docs/spikes/DSK-002.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Documenta: Javalin 7 WS en el puerto 8765 accesible desde un teléfono en el hotspot; la regla de firewall de Windows necesaria (comando `netsh` exacto)
  - Documenta si jpackage (JDK 25) genera el .msi/.exe y qué requiere (WiX)
  - Si Javalin falla: respaldo Java-WebSocket registrado con `uak decision`
- **Verify:** `test -s docs/spikes/DSK-002.md`
- **Next step:** un `main` mínimo de Javalin con un endpoint `/ws` de eco

## DSK-003 · Persistencia SQLite WAL y recuperación

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** criterio "Ninguna calificación se pierde" · plan.md §Data
- **Depends on:** DSK-001, CORE-002
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/db/, BOXCENTRAL/src/main/resources/db/, BOXCENTRAL/src/test/java/com/sigebox/central/db/
- **Freeze-Allowed:** no
- **Acceptance:**
  - SQLite en modo WAL en la carpeta de datos del usuario; migraciones `V1__init.sql` con todas las tablas de plan.md §Data
  - Repositorios para cada entidad + `ProcessedMessages` (idempotencia por `id`) + `EventLog.append(...)`
  - `RecoveryService.load()` reconstruye la pelea activa y su round actual tras un cierre forzado
  - Pruebas contra un archivo SQLite real temporal, incluida una escritura seguida de "kill" simulado y reapertura
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.db.*'`
- **Next step:** prueba que guarda una `TarjetaRound`, cierra la conexión y la vuelve a leer

## DSK-004 · Servidor WebSocket: unión, rounds, ACK idempotente y STATE por juez

- **Type:** feature
- **Priority:** P0
- **Estimate:** 6 h
- **Rubric / Spec:** demo pasos 2–5 · criterios "≤ 1 s", "Un juez no ve la tarjeta de otro", "servidor rechaza mensajes manipulados"
- **Depends on:** DSK-003, CORE-003
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/server/, BOXCENTRAL/src/test/java/com/sigebox/central/server/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Javalin 7 WS en el puerto 8765 (configurable); `JOIN` con token de un solo uso → `JOIN_PENDING`; la central aprueba/rechaza → `JOIN_APPROVED`/`JOIN_REJECTED`
  - `SUBMIT_ROUND`/`SUBMIT_FINAL_CARD`: valida con `ScoreValidator`, persiste (DSK-003) y solo después envía `ACK`; mensaje repetido → mismo `ACK` sin duplicar
  - `HELLO` → `STATE` filtrado: solo los datos del propio juez; nunca totales
  - Difunde `ROUND_OPENED`, `ROUND_CLOSED`, `FIGHT_ENDED`, `FIGHT_CLOSED`; `PING/PONG`; límite de tamaño de mensaje; ningún mensaje de control se acepta desde un juez
  - API interna (`ServerEvents`) para que la UI vea conexiones y envíos
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.server.*'`
- **Next step:** prueba: dos `SUBMIT_ROUND` con el mismo `id` producen un solo registro y dos `ACK`

## DSK-005 · Crear pelea, QR por lugar y aprobación de jueces

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** RO-xx · demo pasos 1–2
- **Depends on:** DSK-004, DSK-011
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/setup/, BOXCENTRAL/src/main/java/com/sigebox/central/service/FightService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/FightServiceTest.java, BOXCENTRAL/src/main/java/com/sigebox/central/server/SeatTokens.java, BOXCENTRAL/src/test/java/com/sigebox/central/server/SeatTokensTest.java (ampliado por zoe, 2026-10-25: código manual del contrato AND-004)
- **Freeze-Allowed:** no
- **Acceptance:**
  - Formulario: esquina roja, esquina azul, rounds (1–12), jueces (1–5)
  - Un QR (ZXing) + código de 6 dígitos por lugar con la URI `sigebox://join?...` (IP local detectada)
  - Lista de lugares: "Juez 2 · María López · Conectado / Esperando aprobación" con botones Aprobar/Rechazar
  - Contrato AND-004: `fightId` corto (≤ 8 caracteres), huella corta del teléfono (8 hex, "AB12 CD34") junto a Aprobar; `SeatTokens` acepta el código de 6 dígitos además del token del QR y limita los intentos con código por conexión
  - Pantalla según `docs/DESIGN.md` v2 §7.1 (componentes de DSK-011)
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.FightServiceTest' --tests 'com.sigebox.central.server.*'`
- **Next step:** prueba de `FightService.create` que rechaza 13 rounds

## DSK-006 · Control de rounds, faltantes, cierre forzado y resumen parcial

- **Type:** feature
- **Priority:** P0
- **Estimate:** 5 h
- **Rubric / Spec:** RO-07 · demo pasos 3–4 · criterios "Sin totales visibles", "Cierre forzado con doble confirmación"
- **Depends on:** DSK-004
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/rounds/, BOXCENTRAL/src/main/java/com/sigebox/central/service/RoundService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/RoundServiceTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Abrir/cerrar round; casilla por juez: Pendiente / Enviado / Tardío / Corregido, actualizada en ≤ 1 s
  - Cerrar con faltantes: confirmar + escribir AVANZAR; queda registrado como cierre forzado
  - Nunca muestra totales ni ganador; "Ver resumen parcial" discreto con confirmación, visible 15 s y registrado en bitácora
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.RoundServiceTest'`
- **Next step:** prueba: `closeRound` con un juez faltante sin la palabra AVANZAR lanza error

## DSK-007 · Terminación extraordinaria y corrección/captura desde papel

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** RO-08, RO-09
- **Depends on:** DSK-003, CORE-003
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/termination/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/correction/, BOXCENTRAL/src/main/java/com/sigebox/central/service/TerminationService.java, BOXCENTRAL/src/main/java/com/sigebox/central/service/CorrectionService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/TerminationServiceTest.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/CorrectionServiceTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Motivos: KO, KO técnico, detenida por el médico, abandono, descalificación, decisión técnica, sin decisión, Otro (razón obligatoria); esquina ganadora, round, tiempo, observaciones; doble confirmación
  - Corrección o captura desde papel por el organizador con motivo obligatorio; cada cambio crea una `VersionTarjeta`
  - Todo queda en bitácora
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.*'`
- **Next step:** prueba: terminación "Otro" sin razón es rechazada

## DSK-008 · Cierre de pelea, resultado a pantalla completa y exportar PDF/CSV

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** demo paso 6 · criterio "Resultado a pantalla completa con nombres y exportación"
- **Depends on:** DSK-003, CORE-003
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/result/, BOXCENTRAL/src/main/java/com/sigebox/central/service/ResultService.java, BOXCENTRAL/src/main/java/com/sigebox/central/export/, BOXCENTRAL/src/test/java/com/sigebox/central/service/ResultServiceTest.java, BOXCENTRAL/src/test/java/com/sigebox/central/export/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Cerrar pelea solo con todas las tarjetas finales (o terminación); `ResultService.compute` usa `ResultCalculator` de core
  - Pantalla completa solo para el operador: "Gana Roja por decisión dividida 2–1", tarjeta y total de cada juez con su nombre; espacio reservado para logotipo
  - `ExportService.pdf` (OpenPDF) y `ExportService.csv`; la pelea de referencia exporta los totales 39–37, 37–39, 39–37
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.ResultServiceTest' --tests 'com.sigebox.central.export.*'`
- **Next step:** prueba de `ResultService` con `demo_data/pelea_referencia.json`

## DSK-009 · Bitácora y términos en el primer uso

- **Type:** feature
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** RO-13, RO-14
- **Depends on:** DSK-003
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/log/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/terms/, BOXCENTRAL/src/test/java/com/sigebox/central/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Primer uso: términos, aviso de privacidad y prohibición de apuestas; sin aceptar no se continúa (queda registrado)
  - Vista de bitácora filtrable por pelea y tipo
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test`
- **Next step:** prueba: sin aceptación guardada, `App` muestra la pantalla de términos

## DSK-010 · Simulador de jueces (judge-sim)

- **Type:** feature
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** IDEA §4 plan B · criterio "≤ 1 s" (medición)
- **Depends on:** DSK-001, CORE-002
- **Uses contract:** DSK-004
- **Paths:** BOXCENTRAL/judge-sim/src/
- **Freeze-Allowed:** no
- **Acceptance:**
  - CLI: `--host --port --judges 1..5 --script demo_data/pelea_referencia.json`; une, envía rounds, reintenta hasta `ACK` y reporta latencia p50/p95
  - Modo `--drop 60s` para simular el corte de Wi‑Fi
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL :judge-sim:test`
- **Next step:** prueba del parser de argumentos con `--judges 6` rechazado

## DSN-002 · Guía visual v2 (cristal, fondo blanco, esquinas)

- **Type:** docs
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** spec 001 enmienda 2026-10-25 · propuesta "Gong: guía de diseño de interfaz (v1)" de zoe
- **Depends on:** DSN-001
- **Paths:** docs/DESIGN.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Reescribe `docs/DESIGN.md` como v2 con la propuesta de zoe adaptada a SIGEBOX: nombre SIGEBOX (no "Gong"), Java con vistas XML + Material (juez) y JavaFX + CSS (central), sin Flutter
  - Tokens (color, tipografía Barlow Condensed + Inter empaquetadas, radios, sombra única gris) y la receta de cristal con su modo sencillo como base (JavaFX no difumina el fondo y Android lo hace solo desde API 31; minSdk 26)
  - Ajustes al producto: el juez no ve su total ("Mi total" fuera) ni tarjetas ajenas; la central sí ve en vivo los puntos y "Va ganando" (spec enmendado); sin "Escribiendo…", "reabrió el round" ni "Expulsar" (el protocolo no los tiene); puerto 8765; código de 6 dígitos, `fightId` corto y huella de 8 hex (contrato AND-004)
  - Lista de revisión visual y bocetos de cada pantalla del juez y de la central
- **Verify:** `test -s docs/DESIGN.md && grep -q "v2" docs/DESIGN.md`
- **Next step:** tabla de tokens de color con los valores de la propuesta

## DSK-011 · Sistema visual de la central y pantallas conectadas

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/DESIGN.md` v2 §3–5 y §7.3 · spec 001
- **Depends on:** DSN-002, DSK-009
- **Paths:** BOXCENTRAL/src/main/resources/com/sigebox/central/, BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/Screens.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/theme/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/termination/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/correction/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/log/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/terms/, BOXCENTRAL/src/main/java/com/sigebox/central/ui/result/, BOXCENTRAL/src/test/java/com/sigebox/central/ui/theme/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `app.css` con los tokens de DESIGN v2 (sin valores sueltos en las pantallas); Inter y Barlow Condensed empaquetadas con su licencia OFL (sin descarga en ejecución); dependencia registrada con `uak decision`
  - Componentes en `ui/theme/`: superficie de cristal, píldora, botón principal y secundario, punto de conexión, barra de pasos (Pelea · Jueces · En vivo · Resultado)
  - `App` arranca con los términos si no están aceptados (`TermsGate.firstScreen`) y registra Términos, Rounds, Terminación, Corrección, Bitácora y Resultado
  - Terminación, corrección, bitácora, términos y resultado con el estilo v2; foco visible y uso con teclado
  - Evidencia: captura de la app-image en 1280×800
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** prueba de que cada color de DESIGN v2 aparece como variable en `app.css`

## DSK-012 · Tablero en vivo del operador: tarjetas por juez y "Va ganando"

- **Type:** feature
- **Priority:** P0
- **Estimate:** 5 h
- **Rubric / Spec:** spec 001 enmienda 2026-10-25 (reemplaza al resumen parcial de 15 s) · `docs/DESIGN.md` v2 §7.2
- **Depends on:** DSK-006, DSK-011
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/rounds/, BOXCENTRAL/src/main/java/com/sigebox/central/service/RoundService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/RoundServiceTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tabla juez × round con `rojo – azul`, estado del round abierto por juez (Pendiente / Enviado / Tardío / Corregido) y fila de totales de los rounds cerrados
  - "Va ganando" con `DecisionRules.fallo` de `core` sobre los totales de los rounds cerrados (sin reglas nuevas en la central); "Empate" cuando corresponda
  - Se quita el resumen parcial de 15 s; ver el tablero se registra una vez por pelea en la bitácora
  - Nada del tablero se envía a los teléfonos (el `STATE` no cambia)
  - Evidencia: captura del tablero con la pelea de referencia
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.RoundServiceTest'`
- **Next step:** prueba: con la pelea de referencia hasta el round 3, "Va ganando" es Roja

## AND-007 · Rediseño de la app del juez y bordes de pantalla

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/DESIGN.md` v2 §3–6 · humano: cris (revisa)
- **Depends on:** DSN-002, AND-006
- **Paths:** app/src/main/res/, app/src/main/java/com/sigebox/juez/, app/src/test/java/com/sigebox/juez/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tema Material con los tokens de DESIGN v2: fondo blanco, tarjetas de cristal (modo sencillo por defecto; desenfoque solo con API 31+), rojo y azul solo para esquinas, punto verde solo para la conexión; Inter y Barlow Condensed en `res/font` con licencia OFL
  - Puntuación: tarjeta por esquina con botones − y + de 76 dp y el número en grande, progreso de rounds, píldora "Juez N" con punto de conexión, hoja de confirmación antes de enviar; sin "Mi total"
  - Todas las pantallas respetan los bordes del sistema (edge-to-edge de targetSdk 37: nada queda bajo la barra de estado ni la de navegación)
  - Textos en minúscula con mayúscula inicial; zonas táctiles ≥ 48 dp
  - Evidencia: captura de cada pantalla en el emulador (`adb exec-out screencap`)
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de que `ScoreActivity` aplica los insets del sistema a su raíz

## DSK-013 · Ajustes visuales de zoe: contenido centrado y sin cursivas

- **Type:** feature
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** pedido de zoe 2026-10-25 sobre las capturas de DSK-011 · `docs/DESIGN.md` v2
- **Depends on:** DSK-011, DSK-005
- **Paths:** BOXCENTRAL/src/main/resources/com/sigebox/central/, BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/theme/, BOXCENTRAL/src/test/java/com/sigebox/central/ui/theme/, docs/DESIGN.md, BOXCENTRAL/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - El contenido de cada pantalla va en una columna centrada (ancho máximo 1100 px), con títulos y tarjetas centrados
  - Ningún texto en cursiva: Barlow Condensed recta (ExtraBold y Bold, OFL) para números y títulos; se quitan los TTF cursivos
  - `docs/DESIGN.md` v2 dice "recta" en lugar de "cursiva" y describe la columna centrada (también rige para AND-007)
  - `App` registra la pantalla de pelea y jueces de DSK-005 (`SetupScreen`) en los pasos Pelea y Jueces, con un solo `SeatTokens` compartido con `JudgeHub`
  - `BOXCENTRAL/build.gradle.kts`: `filter.isFailOnNoMatchingTests = false` en todos los `Test`, para que `test --tests X` no falle en `judge-sim` (los Verify de DSK-005 y DSK-012 lo usan)
  - Evidencia: capturas 1280×800 de la ventana real
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** prueba de que `app.css` no tiene `-fx-font-style: italic`

## DSK-014 · judge-sim: orden estable de rounds (prueba inestable)

- **Type:** fix
- **Priority:** P1
- **Estimate:** 30 min
- **Rubric / Spec:** seguimiento de DSK-010
- **Depends on:** DSK-010
- **Paths:** BOXCENTRAL/judge-sim/src/main/java/com/sigebox/judgesim/SimJudge.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - `SimJudge` guarda los planes en un `TreeMap`: el catch-up tras reconectar envía los rounds en orden aunque reciba un `Map.of`
  - `SimJudgeTest.afterReconnectStateCatchesUpMissedRounds` pasa 10 veces seguidas con `--rerun-tasks`
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL :judge-sim:test`
- **Next step:** `this.plans = new TreeMap<>(plans)` en el constructor

## QA-001 · Integración: corte de 60 s, cierre forzado y latencia ≤ 1 s

- **Type:** integration
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** demo paso 5 · criterios "Ninguna calificación se pierde" y "≤ 1 s"
- **Depends on:** DSK-004, DSK-010
- **Paths:** BOXCENTRAL/src/test/java/com/sigebox/central/it/, BOXCENTRAL/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - Servidor real + judge-sim con 5 jueces × 12 rounds: corte de 60 s de un juez y reinicio del servidor a mitad de pelea → 120 calificaciones presentes, sin duplicados
  - p95 `SUBMIT_ROUND` → `ACK` ≤ 1 s en localhost
  - `BOXCENTRAL/build.gradle.kts`: `testImplementation(project(":judge-sim"))` para usar `SimJudge` en la prueba
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.it.*'`
- **Next step:** prueba que arranca el servidor en un puerto libre y une 5 jueces simulados

## QA-002 · Runbook del evento y READMEs

- **Type:** docs
- **Priority:** P1
- **Estimate:** 2 h
- **Rubric / Spec:** criterio "Instalación sin tienda ni internet, y todo sin router"
- **Depends on:** AND-002, DSK-002
- **Paths:** docs/runbook.md, README.md, docs/demo.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Runbook: montar el hotspot, instalar el .msi y el APK sin internet, regla de firewall, qué hacer si cae la red o una app, volver a papel
  - README con cómo compilar cada app y `core`; `docs/demo.md` con los 6 pasos y el plan B (video + judge-sim)
- **Verify:** `test -s docs/runbook.md`
- **Next step:** copiar los resultados de `docs/spikes/AND-002.md` y `DSK-002.md` al runbook
