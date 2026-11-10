# TASKS · SIGEBOX v1 (spec `docs/specs/001-sigebox/`)

State lives on the `claims` branch (`uak status`); this file holds only tasks.
Prefijos: CORE (núcleo compartido `core/`), AND (app del juez `android/`, humano cris), DSK (central `central/`, humano zoe), DSN (diseño), QA, DOC. Specs: `docs/specs/001…005`.
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

## DSK-015 · Atajos del tablero en vivo: Ctrl+Enter, Corregir y Cerrar pelea

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 h
- **Rubric / Spec:** `docs/DESIGN.md` v2 §7.2 (control del round), seguimiento de DSK-012
- **Depends on:** DSK-012
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/rounds/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `Ctrl+Enter` en el tablero abre o cierra el round según el estado (con la misma confirmación AVANZAR si faltan jueces)
  - Botón secundario "Corregir un round" lleva a la pantalla de corrección
  - Tras el último round, el botón principal es "Cerrar pelea" y lleva a Resultado
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** `RoundsScreen.register` recibe los callbacks `onCorrect` y `onFinish` desde `App`

## DSK-016 · Mostrar el id de la pelea en la central

- **Type:** fix
- **Priority:** P1
- **Estimate:** 1 h
- **Rubric / Spec:** hallazgo de QA-002, que resultó falso: la central ya mostraba el id como "Código de la pelea" en la tarjeta Pelea. DSK-016 agregó el botón Copiar y corrigió los docs
- **Depends on:** DSK-005, QA-002
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/setup/SetupScreen.java, docs/runbook.md, docs/demo.md, README.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Las pantallas Pelea y Jueces muestran el id de la pelea junto a la dirección (IP:puerto), en texto que se puede leer y copiar
  - Con esos datos y el código de 6 dígitos, un juez se une por la entrada manual de la app sin escanear el QR
  - `docs/runbook.md`, `docs/demo.md` y `README.md` dejan de decir que el id solo va en el QR
  - Evidencia: captura de la pantalla Jueces y unión manual (teléfono o `judge-sim --fight`)
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** agregar el id al pie de `SetupScreen` (`footer`), junto a "Dirección para los jueces"

## AND-008 · CI dice qué prueba de la app falla

- **Type:** fix
- **Priority:** P1
- **Estimate:** 30 min
- **Rubric / Spec:** en `uak / gates`, `:app:testDebugUnitTest` falla a veces con "64 tests completed, 1 failed" y pasa al relanzar (main 2026-10-25 22:37; PR #38 intento 1). CI no muestra qué prueba es. Dividida el 2026-10-26: el arreglo de la prueba pasa a AND-009
- **Depends on:** AND-003
- **Paths:** app/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - `app/build.gradle.kts`: las pruebas unitarias imprimen el nombre y la excepción de cada prueba fallida (`testLogging` con `events("failed")` y `exceptionFormat = FULL`)
  - `./gradlew :app:testDebugUnitTest` sigue en verde y `uak / gates` pasa en el PR
  - El PR documenta lo intentado para reproducir la falla
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** PR #40

## AND-010 · Seguimiento de AND-008: testLogging también con `gradle -q`

- **Type:** fix
- **Priority:** P1
- **Estimate:** 15 min
- **Rubric / Spec:** seguimiento de AND-008. En `uak / gates`, `tools/smoke.sh` corre `:app:testDebugUnitTest -q`, y el modo silencioso oculta el `testLogging` de AND-008. Prueba: la corrida de main del 2026-10-26 03:46 (run 40265713052) falló otra vez con "64 tests completed, 1 failed" y sin nombre
- **Depends on:** AND-008
- **Paths:** app/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - `testLogging.quiet` muestra `failed` con `exceptionFormat = FULL`
  - Evidencia: con `-q`, una prueba temporal que falla a propósito imprime su nombre y la aserción; sin el cambio no imprime nada. La prueba temporal no se sube
  - `./gradlew :app:testDebugUnitTest` sigue en verde
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** bloque `quiet { ... }` dentro de `testLogging`

## AND-009 · Arreglar la prueba inestable de la app

- **Type:** fix
- **Priority:** P2
- **Estimate:** 2 h
- **Rubric / Spec:** seguimiento de AND-008. Empieza cuando un log de `uak / gates` (con el `testLogging` de AND-008) nombre la prueba que falla. Sin ese log, la tarea queda BLOCKED
- **Depends on:** AND-008
- **Paths:** app/src/test/java/com/sigebox/juez/, app/src/main/java/com/sigebox/juez/net/
- **Freeze-Allowed:** no
- **Acceptance:**
  - El PR cita el log de CI con el nombre y la excepción de la prueba que falló
  - Se corrige la causa sin esperas fijas que dependan de la velocidad de la máquina. Si la causa está en el código de la app, se arregla el código, no se relaja la prueba
  - Cambiar una prueba existente se justifica con la causa encontrada
  - La prueba pasa 10 veces seguidas localmente y `uak / gates` pasa en el PR
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** esperar un fallo de `uak / gates` con nombre (relanzar o revisar corridas de main)

## AND-011 · Al abrir la app, reconectar y volver a la pantalla correcta

- **Type:** fix
- **Priority:** P0
- **Estimate:** 1 h
- **Rubric / Spec:** demo paso 5 · criterio "Ninguna calificación enviada se pierde (corte de 60 s, cierre forzado de apps)". Hoy, tras un cierre forzado, `MainActivity` siempre abre `JoinActivity` y nada llama a `JudgeRuntime.get()` hasta tocar "Unirme": el teléfono no se reconecta (`HELLO` → `STATE`), no vacía la bandeja de salida y no vuelve a la puntuación aunque el lugar esté aprobado
- **Depends on:** None
- **Paths:** app/src/main/java/com/sigebox/juez/MainActivity.java, app/src/test/java/com/sigebox/juez/MainActivityTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - `MainActivity` llama a `JudgeRuntime.get(this)` al abrir la app (reconecta al lugar guardado y reenvía la bandeja de salida)
  - Lee `local_state` fuera del hilo principal: lugar aprobado → `ScoreActivity`; lugar pendiente de aprobación → `WaitingActivity`; sin lugar → `JoinActivity` (que pide términos si faltan)
  - Prueba Robolectric del enrutamiento para los tres casos
  - Tras un cierre forzado, lo que quedó en la bandeja de salida se envía al reconectar, sin tocar "Unirme"
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** `MainActivityTest` con `local_state` aprobado que espera `ScoreActivity`

## DSK-017 · Principio 3: tablero en vivo sin totales ni "Va ganando"

- **Type:** fix
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** decisión de zoe del 2026-10-26, tras el aviso de cris: manda `PROJECT.md` principio 3 ("nadie ve totales ni ganador durante la pelea, salvo 'Ver resumen parcial', registrado en bitácora"). Se revierte la enmienda del spec 001 que introdujo DSK-012. Regla del empate mayoritario: se queda como está (una esquina con votos y el resto empates = MAYORITARIA, IDEA §5)
- **Depends on:** DSK-012, DSK-015
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/service/RoundService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/RoundServiceTest.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/rounds/, docs/specs/001-sigebox/spec.md, docs/DESIGN.md, docs/runbook.md, docs/demo.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - El tablero en vivo muestra por juez y round solo el estado (Pendiente / Enviado / Tardío / Corregido), sin puntos, sin fila de totales y sin "Va ganando"
  - Vuelve "Ver resumen parcial": botón discreto, confirmación, se muestra 15 s y cada vista queda en la bitácora (spec 001 original, RO-07)
  - `RoundService` no expone totales ni líder fuera de `partialSummary` con confirmación; se quitan `Leader`, totales y puntos de `live()`. Las pruebas de DSK-012 que afirmaban totales o líder se cambian a la regla nueva, citando esta decisión
  - Se conservan los atajos de DSK-015 (Ctrl+Enter, Corregir un round, Cerrar pelea)
  - `docs/specs/001-sigebox/spec.md` y `docs/DESIGN.md` §7.2 vuelven a decir "sin totales"; `docs/runbook.md` y `docs/demo.md` dejan de mencionar "Va ganando"
  - Evidencia: captura del tablero con la pelea de referencia y del resumen parcial con su registro en la bitácora
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.RoundServiceTest'`
- **Next step:** recuperar `partialSummary` y su prueba de antes de DSK-012 (commit anterior a 128723f)

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

## DSN-003 · Guía visual v3: rediseño total con los colores de la Ciudad de México

- **Type:** contract
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** pedido de cris (2026-10-26): rediseño total que siga las tendencias actuales de diseño de apps, use la paleta de colores de la Ciudad de México y haga que SIGEBOX se sienta distinta. Reemplaza a la v2 (DSN-002)
- **Depends on:** DSK-017
- **Paths:** docs/DESIGN.md, docs/design/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Paleta basada en los colores de la identidad visual de la Ciudad de México, con la fuente oficial citada en el documento (URL y fecha). Solo se toman los colores: nada de escudo, logotipos, lemas ni tipografías con licencia del gobierno, y nada que haga parecer la app oficial del gobierno
  - Tokens completos para claro y oscuro (arena con poca luz): color, tipografía empaquetable con licencia libre (OFL), espaciado, radios, elevación y movimiento, con contraste AA medido para cada par texto/fondo. Rojo y azul de esquina siguen distinguibles entre sí y frente a la paleta CDMX (incluido daltonismo)
  - Tendencias actuales justificadas una por una (por ejemplo: tipografía expresiva y numerales grandes, layouts tipo bento en la central, componentes Material 3 Expressive en Android, microinteracciones con háptica); cada una dice qué se adopta y qué no, y por qué sirve en la arena
  - Pantallas nuevas descritas para el juez (términos, unión/QR, espera, puntuación, tardío, tarjeta final) y la central (crear pelea, aprobación, tablero en vivo sin totales, terminación, resultado, bitácora), respetando PROJECT.md principio 3 (sin totales ni ganador durante la pelea, salvo "Ver resumen parcial") y que el juez solo ve lo suyo
  - Sección "0. Qué cambió respecto a la v2" y lista de revisión visual actualizada
- **Verify:** `test -s docs/DESIGN.md && grep -q "v3" docs/DESIGN.md`
- **Next step:** investigar y citar la paleta oficial de la CDMX; escribir la tabla de tokens de color con su contraste

## AND-012 · Rediseño v3 de la app del juez

- **Type:** feature
- **Priority:** P0
- **Estimate:** 6 h
- **Rubric / Spec:** `docs/DESIGN.md` v3 (DSN-003) · criterios "Solo enteros 0–10" y "Round sin calificar: vibración y calificación exigida"
- **Depends on:** DSN-003, AND-011
- **Paths:** app/src/main/res/, app/src/main/java/com/sigebox/juez/, app/src/test/java/com/sigebox/juez/ui/, app/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tema, colores, tipografía (empaquetada, sin descargas) y componentes según DESIGN v3, en claro y oscuro
  - Todas las pantallas del juez rediseñadas sin cambiar el comportamiento: mismas validaciones, mismo protocolo, misma bandeja de salida
  - Las pruebas existentes siguen en verde; pruebas Robolectric nuevas de que cada pantalla infla con el tema v3
  - Evidencia en el PR: capturas del emulador de cada pantalla en claro y oscuro
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** pasar los tokens de DESIGN v3 a `res/values/colors.xml`, `themes.xml` y `values-night/`

## DSK-018 · Rediseño v3 de la central

- **Type:** feature
- **Priority:** P0
- **Estimate:** 6 h
- **Rubric / Spec:** `docs/DESIGN.md` v3 (DSN-003) · criterios "Sin totales visibles" y "Resultado a pantalla completa"
- **Depends on:** DSN-003, DSK-017
- **Paths:** BOXCENTRAL/src/main/resources/com/sigebox/central/, BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/main/java/com/sigebox/central/ui/, BOXCENTRAL/src/test/java/com/sigebox/central/ui/, BOXCENTRAL/build.gradle.kts
- **Freeze-Allowed:** no
- **Acceptance:**
  - Hoja de estilos JavaFX, tipografía empaquetada y componentes según DESIGN v3, en claro y oscuro
  - Todas las pantallas de la central rediseñadas sin cambiar servicios ni protocolo; el tablero en vivo sigue sin totales (principio 3)
  - Las pruebas existentes siguen en verde; prueba de que el tema v3 carga sin errores de CSS
  - Evidencia en el PR: capturas de cada pantalla con la pelea de referencia
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** reemplazar los tokens de `ui/theme/` por los de DESIGN v3

## CORE-005 · Mensaje NEXT_FIGHT (contrato del cambio de pelea)

- **Type:** contract
- **Priority:** P0
- **Estimate:** 2 h
- **Rubric / Spec:** `docs/specs/002-funcion/` · ADR 0002
- **Depends on:** None
- **Paths:** core/src/main/java/com/sigebox/core/protocol/, core/src/test/java/com/sigebox/core/protocol/, core/schemas/, demo_data/mensajes/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `NextFight` record `{v, id, type, fightId, nextFightId, seat|null, redCorner, blueCorner, rounds}` con validación de `Fields` (seat 1–5 o nulo, rounds 1–12, textos 1–64 sin controles)
  - `core/schemas/next_fight.schema.json` (`additionalProperties: false`) y fixture `demo_data/mensajes/NEXT_FIGHT.json`; `ProtocolSchemaTest` lo cubre como a los otros 16
  - `MessageType.NEXT_FIGHT` y `Messages.decode` lo reconocen; mensajes manipulados se rechazan
  - Visto bueno de cris al contrato antes del merge (ADR 0002)
- **Verify:** `core/gradlew -p core build`
- **Next step:** prueba de ida y vuelta del fixture `NEXT_FIGHT.json` que falla porque el tipo no existe

## DSK-019 · Función y cartelera en la central (datos y servicio)

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/specs/002-funcion/` (cartelera, base, cambios por pelea, saltar)
- **Depends on:** DSK-017
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/db/, BOXCENTRAL/src/main/resources/db/, BOXCENTRAL/src/main/java/com/sigebox/central/service/FuncionService.java, BOXCENTRAL/src/test/java/com/sigebox/central/service/FuncionServiceTest.java, BOXCENTRAL/src/test/java/com/sigebox/central/db/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tablas `funcion` y `cartelera` (solo expand; las peleas sueltas siguen igual) y su repositorio
  - `FuncionService`: crear función con base (rounds 1–12, jueces 1–5); agregar, quitar, reordenar y saltar peleas no empezadas; cambiar rounds o jueces de una pelea no empezada; cada cambio persiste antes de devolverse y queda en la bitácora
  - `siguiente(funcionId)` crea la `Pelea` de la siguiente fila PROGRAMADA (base o sus cambios) con `FightService`, la marca EN_CURSO y respeta "una sola pelea activa"
  - Cerrar a la fuerza la central a mitad de cartelera no pierde la cartelera (prueba reabriendo la base)
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.service.FuncionServiceTest'`
- **Next step:** prueba: función con base 4 rounds × 3 jueces y 3 peleas; la tercera cambia a 6 rounds; `siguiente` crea las peleas en orden con esa configuración

## DSK-020 · Llevar a los jueces a la siguiente pelea (central y judge-sim)

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/specs/002-funcion/` · ADR 0002
- **Depends on:** CORE-005, DSK-019
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/server/, BOXCENTRAL/src/main/java/com/sigebox/central/service/FuncionService.java, BOXCENTRAL/src/test/java/com/sigebox/central/server/, BOXCENTRAL/src/test/java/com/sigebox/central/service/FuncionServiceTest.java, BOXCENTRAL/judge-sim/src/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al iniciar la siguiente pelea se copian en una transacción los lugares aprobados 1..N (nombre, dispositivo, huella) y después se manda `NEXT_FIGHT` a cada teléfono conectado; los lugares sobrantes reciben `seat: null`; los lugares nuevos quedan para QR
  - Un `HELLO` de la pelea cerrada con una llave que está en la pelea activa de la misma función recibe `NEXT_FIGHT`; con otra llave se rechaza como hoy
  - `JudgeHub` rechaza un `NEXT_FIGHT` que llegue de un teléfono
  - `SimJudge` entiende `NEXT_FIGHT`: guarda la pelea nueva y reconecta con `HELLO`
  - Bitácora `SIGUIENTE_PELEA` en ambas peleas
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.server.*'`
- **Next step:** prueba de `JudgeHub`: dos jueces aprobados en la pelea 1; al iniciar la 2, ambos reciben `NEXT_FIGHT` con su lugar y su `HELLO` a la pelea 2 se acepta sin aprobación

## AND-013 · La app del juez pasa sola a la siguiente pelea

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/specs/002-funcion/` · ADR 0002
- **Depends on:** CORE-005, AND-012, AND-014
- **Paths:** app/src/main/java/com/sigebox/juez/net/, app/src/main/java/com/sigebox/juez/data/, app/src/main/java/com/sigebox/juez/join/, app/src/main/java/com/sigebox/juez/score/, app/src/test/java/com/sigebox/juez/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al recibir `NEXT_FIGHT` con lugar: guarda la pelea nueva y el lugar en `local_state` (sigue aprobado), reconecta con `HELLO` firmado con la misma llave y abre directo la puntuación de la nueva pelea, sin confirmación
  - Con `seat: null`: pantalla "No participas en esta pelea" y espera el siguiente `NEXT_FIGHT`
  - Lo que quedaba en la bandeja de la pelea anterior se entrega a esa pelea antes de cambiar (principio 1)
  - Pruebas Robolectric con un servidor de prueba
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de `JudgeSession`: tras `NEXT_FIGHT` el siguiente `HELLO` lleva `nextFightId` y el mismo `deviceId`

## AND-014 · La app del juez no pierde avisos de la central

- **Type:** fix
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** demo pasos 5 y 6 · hallazgo de cris-2 en el emulador al cerrar AND-012. `ScoreActivity` y `LateScoreActivity` solo escuchan entre `onStart` y `onStop` (`connection.addListener`/`removeListener`): un `STATE` o `FIGHT_ENDED` que llega con la captura tardía abierta, o justo al reabrir la app antes de `onStart`, no lo ve nadie y la pantalla queda en un round o pelea que ya cambió
- **Depends on:** None
- **Paths:** app/src/main/java/com/sigebox/juez/net/JudgeSession.java, app/src/main/java/com/sigebox/juez/net/JudgeConnection.java, app/src/main/java/com/sigebox/juez/net/ConnectionState.java, app/src/main/java/com/sigebox/juez/score/ScoreActivity.java, app/src/main/java/com/sigebox/juez/score/ScoreViewModel.java, app/src/main/java/com/sigebox/juez/late/LateScoreActivity.java, app/src/test/java/com/sigebox/juez/net/, app/src/test/java/com/sigebox/juez/score/, app/src/test/java/com/sigebox/juez/late/
- **Freeze-Allowed:** no
- **Acceptance:**
  - La conexión guarda el último `STATE` y si la pelea terminó (`FIGHT_ENDED`); al registrar un listener se le entrega ese último estado de inmediato
  - Con la captura tardía abierta llega `FIGHT_ENDED`: al volver, la pantalla de puntuar muestra la pelea terminada sin esperar otro mensaje
  - Al reabrir la app, un `STATE` que llegó antes de `onStart` se aplica a la pantalla de puntuar
  - El juez sigue sin ver tarjetas de otros ni totales (principios 2 y 3): solo se guarda lo que ya llega a este teléfono
  - Pruebas Robolectric de ambos casos; las pruebas actuales siguen en verde sin editarlas
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba de `JudgeConnection`: `FIGHT_ENDED` sin listeners, luego `addListener` recibe el fin de pelea

## AND-015 · La tarjeta final no olvida que ya se envió

- **Type:** fix
- **Priority:** P1
- **Estimate:** 2 h
- **Rubric / Spec:** demo paso 6 · hallazgo de cris-2 en el emulador al cerrar AND-012. `FinalCardActivity` guarda el envío solo en memoria (`sentId`): si la pantalla se recrea (rotación, cambio de tema, proceso muerto) vuelve a mostrar "Enviar tarjeta final" habilitado. No se pierde nada (la central responde `ALREADY_FINAL`), pero el juez ve un estado falso y puede reenviar
- **Depends on:** None
- **Paths:** app/src/main/java/com/sigebox/juez/finalcard/, app/src/test/java/com/sigebox/juez/finalcard/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tras enviar, al recrear la pantalla (rotación o proceso muerto) se muestra "Enviando…" mientras la tarjeta siga en la bandeja de salida y "Tarjeta final enviada" con el botón deshabilitado después del `ACK`
  - El estado sale de lo que ya guarda el teléfono (bandeja de salida o `local_state`), no solo de `onSaveInstanceState`, para sobrevivir al cierre forzado
  - Un `ACK` con error que no sea `ALREADY_FINAL` sigue habilitando el botón para reintentar
  - Pruebas Robolectric de recreación antes y después del `ACK`; las pruebas actuales siguen en verde sin editarlas
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba Robolectric: enviar, `recreate()`, el botón sigue deshabilitado

## DSK-021 · Pantalla de función y "Siguiente pelea" en la central

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** `docs/specs/002-funcion/` · DESIGN v3
- **Depends on:** DSK-018, DSK-020
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/ui/, BOXCENTRAL/src/main/java/com/sigebox/central/App.java, BOXCENTRAL/src/test/java/com/sigebox/central/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Pantalla Función: base, cartelera ordenada con estado (Programada, En curso, Cerrada, Saltada), agregar, quitar, reordenar, saltar y cambiar rounds o jueces de las no empezadas
  - En Resultado, "Siguiente pelea: Roja vs Azul" inicia la siguiente; los jueces conectados aparecen ya aprobados y solo los lugares nuevos muestran QR
  - Evidencia: capturas con una función de 3 peleas y judge-sim
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL build`
- **Next step:** registrar la pantalla Función en `App` y mostrar la cartelera de `FuncionService`

## QA-003 · Integración: función de 3 peleas sin volver a emparejar

- **Type:** integration
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** `docs/specs/002-funcion/` (NFR cambio ≤ 5 s, durabilidad)
- **Depends on:** DSK-020
- **Paths:** BOXCENTRAL/src/test/java/com/sigebox/central/it/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Servidor real + 5 jueces de judge-sim: función de 3 peleas (la segunda con 3 jueces y la tercera con 5); todos califican las 3 sin `JOIN` nuevo salvo los lugares agregados
  - Cambio de pelea con 5 jueces: todos en la nueva en ≤ 5 s
  - Reinicio del servidor a mitad de la cartelera: no se pierden cartelera ni tarjetas
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.it.*'`
- **Next step:** prueba con 2 peleas y 2 jueces simulados que pasan de una a otra

## DSK-022 · Tarjetas pendientes de la pelea cerrada no se quedan en el teléfono

- **Type:** fix
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** principio 1 · ADR 0002 · revisión de AND-013 (cris-2). Si un teléfono se reconecta con `HELLO` a la pelea ya cerrada y todavía tiene envíos sin `ACK` de esa pelea, la central (DSK-020) contesta `NEXT_FIGHT` y cierra el socket: el teléfono cambia de pelea y esos envíos se quedan en su bandeja sin llegar nunca a la central
- **Depends on:** DSK-020, AND-013
- **Paths:** BOXCENTRAL/src/main/java/com/sigebox/central/server/JudgeHub.java, BOXCENTRAL/src/test/java/com/sigebox/central/server/JudgeHubNextFightTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Un `HELLO` válido a la pelea cerrada (misma llave que un lugar aprobado de la pelea activa de la misma función) puede entregar sus envíos pendientes de esa pelea antes de recibir `NEXT_FIGHT`; cada uno recibe su `ACK` y queda guardado en la pelea cerrada con el trato que ya tienen los envíos tardíos
  - Ningún envío de la pelea cerrada se guarda en la pelea nueva, ni al revés
  - Sin pendientes, el comportamiento es el de hoy (`NEXT_FIGHT` directo)
  - Prueba con judge-sim o socket de prueba: juez con un `SUBMIT_ROUND` sin `ACK` de la pelea cerrada se reconecta, la central lo guarda en esa pelea y después el juez pasa a la nueva; las pruebas actuales siguen en verde sin editarlas
- **Verify:** `BOXCENTRAL/gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.server.*'`
- **Next step:** prueba en `JudgeHubNextFightTest`: `HELLO` a la pelea cerrada seguido de un `SUBMIT_ROUND` de esa pelea

## AND-016 · Textos y esquinas al cambiar de pelea en la app del juez

- **Type:** fix
- **Priority:** P2
- **Estimate:** 1 h
- **Rubric / Spec:** revisión de AND-013 (deuda en el PR #65). El aviso "No participas en esta pelea" está escrito en Java y no en recursos; `CornerNames` no guarda las esquinas que trae `NEXT_FIGHT`, así que salen en blanco hasta el primer `STATE` de la pelea nueva
- **Depends on:** AND-013
- **Paths:** app/src/main/java/com/sigebox/juez/score/NotSeatedView.java, app/src/main/res/values/strings_score.xml, app/src/main/java/com/sigebox/juez/ui/CornerNames.java, app/src/test/java/com/sigebox/juez/score/NotSeatedViewTest.java, app/src/test/java/com/sigebox/juez/ui/CornerNamesTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - "No participas en esta pelea" sale de `strings_score.xml`
  - Al recibir `NEXT_FIGHT` con lugar, `CornerNames` recuerda `redCorner` y `blueCorner` de la pelea nueva y la puntuación los muestra antes del primer `STATE`
  - Pruebas Robolectric de ambos puntos; las pruebas actuales siguen en verde sin editarlas
- **Verify:** `./gradlew :app:testDebugUnitTest`
- **Next step:** prueba en `CornerNamesTest`: `remember(NextFight)` y luego leer las esquinas

## CORE-006 · Carpetas hermanas android/, central/ y core/

- **Type:** setup
- **Priority:** P0
- **Estimate:** 3 h
- **Rubric / Spec:** ADR 0003 · spec 003
- **Depends on:** None
- **Paths:** settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradlew.bat, gradle/, app/, android/, BOXCENTRAL/, central/, tools/smoke.sh, .github/CODEOWNERS, .github/CODEOWNERS.example, .uak/OWNERS.md, .uak/PROJECT.md, .gitignore, README.md, docs/runbook.md, docs/demo.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - Con `git mv` (se conserva el historial): el proyecto de Android de la raíz (settings, build, gradle.properties, gradlew*, `gradle/` y `app/`) queda en `android/`, y `BOXCENTRAL/` queda en `central/` con `judge-sim/`; `includeBuild("../core")` en ambos
  - `rootProject.name` de la central y las rutas que dependen de él (`install/…/lib` del `jpackage`) siguen funcionando; el instalador se sigue llamando "SIGEBOX Central"
  - `tools/smoke.sh`, `.github/CODEOWNERS` (y `.example`), `.uak/PROJECT.md` (carpetas y CODEOWNERS), `.gitignore`, README (rutas y comandos), `docs/runbook.md` y `docs/demo.md` usan las rutas nuevas; `.idea/` no se mueve
  - `.uak/OWNERS.md`: cada fila de `app/…`, raíz de Android o `BOXCENTRAL/…` pasa a su ruta nueva; las filas de la central `App.java`, `ui/Screens.java` y `db/` quedan para DSK-027; las demás filas movidas siguen siendo de CORE-006
  - Antes de empujar: `uak msg all --kind contract` con las rutas nuevas (zoe cambia sus comandos en Windows)
- **Verify:** `bash tools/smoke.sh`
- **Next step:** `git mv BOXCENTRAL central` y correr `central/gradlew -p central build`

## DSK-025 · Bitácora y estados en español en la central

- **Type:** feature
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** spec 003 §Español en las interfaces
- **Depends on:** CORE-006
- **Paths:** central/src/main/java/com/sigebox/central/ui/log/, central/src/main/java/com/sigebox/central/ui/Labels.java, central/src/main/java/com/sigebox/central/export/, central/src/test/java/com/sigebox/central/ui/log/, central/src/test/java/com/sigebox/central/ui/LabelsTest.java, central/src/test/java/com/sigebox/central/export/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `ui/Labels` traduce cada tipo de evento de la bitácora y cada valor de `EstadoPelea`, `EstadoRound`, `EstadoTarjeta`, `EstadoConexion`, `TipoDecision`, `MotivoTerminacion`, `Ganador` y `OrigenTarjeta`; una prueba recorre todos los valores de cada enum y todos los tipos que escribe la central (nada queda sin traducir)
  - La bitácora muestra el tipo y el mensaje en español (p. ej. `SUBMIT_ROUND` "lugar 2 round 3 TARDIA" → "Tarjeta de round · Juez 2 · round 3 · tardía"); el filtro muestra etiquetas en español y sigue filtrando por el código guardado
  - PDF y CSV muestran estados y decisiones en español; la base de datos no cambia
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.*' --tests 'com.sigebox.central.export.*'`
- **Next step:** `LabelsTest` que recorre `EstadoTarjeta.values()`

## DSK-026 · Rediseño v4 de la central: tema, logo y ayuda de conexión

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Imagen y diseño, §Ayuda de conexión · DSN-004
- **Depends on:** DSN-004
- **Paths:** central/src/main/resources/com/sigebox/central/ui/theme/, central/src/main/resources/com/sigebox/central/ui/help/, central/src/main/java/com/sigebox/central/ui/theme/, central/src/main/java/com/sigebox/central/ui/help/, central/src/main/resources/com/sigebox/central/light.css, central/src/main/resources/com/sigebox/central/dark.css, central/src/main/resources/com/sigebox/central/app.css, central/src/test/java/com/sigebox/central/ui/theme/, central/src/test/java/com/sigebox/central/ui/help/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Paleta v4 en claro y oscuro; prueba de tema mide contraste AA de cada par texto/fondo de DESIGN.md
  - Encabezado con el logo; paso y enlace activos con fondo, exactamente uno activo por pantalla (prueba)
  - Componentes en `ui/theme`: `Skeleton` (sin animación con movimiento reducido), campo con etiqueta arriba + ejemplo dentro, botones principal y secundario
  - "?" fijo en la barra: abre la guía de `docs/design/guia-conexion.md` (copiada en recursos, sin internet)
  - Evidencia: capturas en claro y oscuro (rama `evidence/dsk-026`)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.theme.*' --tests 'com.sigebox.central.ui.help.*'`
- **Next step:** prueba de contraste con la paleta v4

## AND-017 · App del juez en español y tarjeta final automática

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Cierre sin pedir tarjetas · spec 003 §Español en las interfaces · principio 1
- **Depends on:** CORE-006
- **Paths:** android/app/src/main/java/com/sigebox/juez/score/, android/app/src/main/java/com/sigebox/juez/late/, android/app/src/main/java/com/sigebox/juez/finalcard/, android/app/src/main/java/com/sigebox/juez/join/WaitingActivity.java, android/app/src/main/java/com/sigebox/juez/join/WaitingText.java, android/app/src/main/java/com/sigebox/juez/ui/ErrorText.java, android/app/src/main/res/values/strings_score.xml, android/app/src/main/res/values/strings_late.xml, android/app/src/test/java/com/sigebox/juez/score/, android/app/src/test/java/com/sigebox/juez/late/, android/app/src/test/java/com/sigebox/juez/finalcard/, android/app/src/test/java/com/sigebox/juez/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `ErrorText` da una frase en español con qué hacer para cada `Ack.Error`; ninguna pantalla usa `.name()` de un error; prueba que recorre todos los valores
  - Esperando aprobación, No participas, Round tardío y "Esperando round" dicen en una frase qué hacer
  - Al cerrarse el último round (o `STATE` terminada con `finalCardSent = false`) el teléfono arma, firma y mete a la `Outbox` la tarjeta final **sin pantalla ni acción del juez**; si está desconectado, la envía al reconectar (prueba con MockWebServer); no se manda dos veces
  - Los puntos se siguen capturando con + y − sin valor por defecto (principio 2)
  - Evidencia: capturas en emulador (Robolectric no construye `ScoreActivity` por AndroidKeyStore); si no hay emulador, el PR lo dice
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest`
- **Next step:** prueba de que el último `ROUND_CLOSED` deja la tarjeta final en la `Outbox`

## DSK-028 · Riesgos que dejó la revisión de DSK-022

- **Type:** fix
- **Priority:** P2
- **Estimate:** 2 h
- **Rubric / Spec:** revisión de DSK-022 (PR #72, cris-1) · principio 1
- **Depends on:** CORE-006
- **Paths:** central/src/main/java/com/sigebox/central/server/JudgeHub.java, central/src/test/java/com/sigebox/central/server/JudgeHubHandoffTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Prueba nueva (sin editar `JudgeHubNextFightTest`): la pelea se cierra con `ResultService` (no a mano) y el teléfono que vuelve con pendientes entrega su tarjeta de un round que no contó como `TARDIA`, sin cambiar el resultado guardado
  - Prueba de `SUBMIT_FINAL_CARD` por la conexión de traspaso tras una terminación
  - Cuando un juez manda la tarjeta de un round que el organizador ya capturó (`CAPTURADA`), la central responde como hoy y además lo deja en la bitácora con sus puntos, para que el organizador lo vea
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.server.*'`
- **Next step:** `JudgeHubHandoffTest` con `ResultService.close`

## DSK-023 · Evento: nombre, fecha y peleas sin nombres

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Arranque e inicio, §Peleas sin nombres · plan 004 §Data
- **Depends on:** CORE-006
- **Paths:** central/src/main/resources/db/V3__eventos.sql, central/src/main/java/com/sigebox/central/db/Database.java, central/src/main/java/com/sigebox/central/db/FuncionRepository.java, central/src/main/java/com/sigebox/central/service/FuncionService.java, central/src/main/java/com/sigebox/central/ui/funcion/, central/src/test/java/com/sigebox/central/db/, central/src/test/java/com/sigebox/central/service/FuncionServiceTest.java, central/src/test/java/com/sigebox/central/ui/funcion/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `V3__eventos.sql` (solo expansión: `funcion.nombre`, tabla `descuento` como en plan 004) agregada a `Database.MIGRATIONS`; prueba de migración desde una base V2 con datos
  - Crear evento: nombre opcional ("Evento <fecha>" si vacío), fecha elegible (hoy por defecto), rounds 1–12, jueces 1–5, peleas 1–20; se guarda en SQLite antes de mostrarse
  - Nombres de esquina vacíos → "Esquina roja N" / "Esquina azul N" (N fijo al crear); se pueden cambiar mientras la pelea no empieza; siguen las reglas de `Fields`
  - La pantalla dice "Evento" (no "Función"); cada campo tiene su etiqueta arriba y un ejemplo dentro
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.db.*' --tests 'com.sigebox.central.service.FuncionServiceTest' --tests 'com.sigebox.central.ui.funcion.*'`
- **Next step:** prueba de `FuncionService.crear` con nombres vacíos y fecha elegida

## DSK-024 · Cierre automático y Resultado con sumas por juez

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Cierre sin pedir tarjetas, §Resultado · principios 2 y 3
- **Depends on:** DSK-031, DSK-028, CORE-008
- **Paths:** central/src/main/java/com/sigebox/central/ui/result/, central/src/main/java/com/sigebox/central/service/ResultService.java, central/src/main/java/com/sigebox/central/service/RoundService.java, central/src/main/java/com/sigebox/central/export/, central/src/test/java/com/sigebox/central/ui/result/, central/src/test/java/com/sigebox/central/service/ResultServiceTest.java, central/src/test/java/com/sigebox/central/service/RoundServiceTest.java, central/src/test/java/com/sigebox/central/export/, central/src/main/java/com/sigebox/central/server/JudgeHub.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Cerrar el último round programado deja la pelea terminada sola; no existe "Terminar pelea"; la terminación anticipada sigue igual
  - Resultado muestra por juez cada round, los descuentos y la suma de cada esquina, más una fila de sumas por juez; todo de `core` con descuentos; **sin** ganador, tipo de decisión ni botón "Actualizar"; se actualiza sola ≤ 1 s después de cada `ACK` o descuento
  - "Cerrar pelea" se habilita al llegar todas las tarjetas finales (o con terminación); lo guardado = lo calculado con los descuentos vigentes al cerrar (prueba)
  - PDF y CSV siguen con ganador y decisión y agregan los descuentos (prueba)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.service.*' --tests 'com.sigebox.central.ui.result.*' --tests 'com.sigebox.central.export.*'`
- **Next step:** prueba de `RoundService`: cerrar el último round deja la pelea terminada

## DSK-027 · Central sin capacitación: etiquetas arriba, "Qué sigue" en cada pantalla

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Formularios · spec 003 §Sin capacitación
- **Depends on:** DSK-026, DSK-029, DSK-032, DSK-033, DSK-025
- **Paths:** central/src/main/java/com/sigebox/central/App.java, central/src/main/java/com/sigebox/central/ui/, central/src/main/resources/com/sigebox/central/, central/src/test/java/com/sigebox/central/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Todo campo de formulario de la central usa el campo de DSK-026 (etiqueta arriba, ejemplo dentro); prueba que recorre las pantallas y no encuentra un `TextField`/`Spinner`/`DatePicker` sin `Label` asociado
  - Cada pantalla muestra una línea "Qué sigue"; todo botón deshabilitado dice por qué
  - `HelpButton.connection(...)` (DSK-026) junto a cada juez desconectado en Jueces (`SetupScreen`) y En vivo (`RoundsScreen`); prueba
  - Jueces (`SetupScreen`) muestra el QR con `QrCode` e `Invitation.qrUri()` (forma `c=código`, ADR 0004) de DSK-033; prueba: el QR mostrado se decodifica al URI nuevo
  - Ningún código en mayúsculas ni enum visible (prueba con `ScreenshotHarness` o el árbol de nodos); "Función" no aparece en ninguna pantalla
  - Evidencia: capturas de un evento completo de 2 peleas con judge-sim (rama `evidence/dsk-027`), revisadas con `design-critic`
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba que recorre las pantallas buscando campos sin etiqueta

## DOC-001 · README, runbook y demo para eventos

- **Type:** docs
- **Priority:** P2
- **Estimate:** 3 h
- **Rubric / Spec:** spec 004 · spec 003 · pedido de cris ("mejora el MD")
- **Depends on:** DSK-027, AND-021, QA-004
- **Paths:** README.md, docs/runbook.md, docs/demo.md
- **Freeze-Allowed:** no
- **Acceptance:**
  - README: qué es, las tres carpetas, requisitos (JDK 25, Android SDK), cómo construir, probar y correr cada app, el simulador de jueces (`--codes`), empaquetar la central y una guía corta del evento (Inicio → Nuevo evento → Jueces → En vivo → Resultado) que remite al runbook
  - Runbook: unirse con código, guía "?", plan B sin "Capturar desde papel" (volver a papel), descuentos
  - Todo comando del README se corrió en esta máquina y funciona tal cual
- **Verify:** `test -s README.md && bash tools/smoke.sh`
- **Next step:** correr cada comando del README actual y anotar los que fallan

## CORE-007 · Protocolo: código de lugar y fin de evento

- **Type:** contract
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Unirse con código y QR, §Al terminar una pelea · ADR 0004
- **Depends on:** CORE-006, CORE-002, CORE-005
- **Paths:** core/schemas/, core/src/main/java/com/sigebox/core/protocol/, core/src/test/java/com/sigebox/core/protocol/, demo_data/mensajes/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `CODE_LOOKUP`, `CODE_INFO`, `CODE_UNKNOWN` y `EVENT_ENDED` existen en `MessageType`, `Messages`, record validado, esquema (`core/schemas/README.md` actualizado) y fixture; `ProtocolSchemaTest` los cubre; `code` exige exactamente 6 dígitos (prueba con 5, 7 y letras)
  - `JoinUri` genera `sigebox://join?h=&p=&c=[&f=]` y sigue leyendo la forma vieja (`s`, `seat`, `t`); pruebas de las dos
  - `JOIN`, `HELLO` y `DeviceProof` no cambian (las pruebas existentes siguen sin tocarse)
  - Compila con `-Xlint:all -Werror`, sin APIs fuera de Android 26; JaCoCo ≥ 80 %
  - Antes del push: `uak msg impact:CORE-007 --kind contract` con los mensajes nuevos y qué hacer
- **Verify:** `core/gradlew -p core build`
- **Next step:** fixture `CODE_LOOKUP.json` y caso en `ProtocolSchemaTest` que falla

## CORE-008 · Descuentos de punto en el resultado

- **Type:** contract
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §En vivo · ADR 0005 · principio 2
- **Depends on:** CORE-006, CORE-002
- **Paths:** core/src/main/java/com/sigebox/core/model/, core/src/main/java/com/sigebox/core/scoring/, core/src/test/java/com/sigebox/core/scoring/, core/src/test/resources/scoring/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `Descuento` (round, esquina, 1 punto, anulado) y `ResultCalculator.calcular(..., descuentos)`: cada descuento vigente resta 1 a esa esquina en ese round en la tarjeta de cada juez, sin bajar de 0; los anulados no cuentan
  - El resultado expone por juez: puntos por round ya descontados, descuentos aplicados y suma de cada esquina; ganador y decisión con las sumas descontadas
  - La firma vieja de `calcular` da lo mismo que hoy con `pelea_referencia.json` (prueba sin editar las existentes)
  - Caso de referencia calculado a mano en `core/src/test/resources/scoring/` (2 descuentos de esquinas distintas, 1 anulado, 2 en el mismo round) y su prueba
  - Compila con `-Xlint:all -Werror`, sin APIs fuera de Android 26; JaCoCo ≥ 80 %
  - Antes del push: `uak msg impact:CORE-008 --kind contract` con la firma nueva
- **Verify:** `core/gradlew -p core build`
- **Next step:** prueba con un descuento a la roja en el round 1 que falla

## DSN-004 · Guía visual v4: negro, blanco y dorado, logo de guantes

- **Type:** design
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Imagen y diseño, §Formularios, §Ayuda de conexión, §Arranque
- **Depends on:** CORE-006, DSN-001, DSN-002, DSN-003, DSK-013, DSK-017
- **Paths:** docs/DESIGN.md, docs/design/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `docs/DESIGN.md` v4 (reemplaza v3): paleta negro/blanco/dorado en claro y oscuro con tabla de contraste AA medida para cada par texto/fondo; rojo y azul solo para esquinas; tipografía (Barlow Condensed + Inter, ya empaquetadas)
  - Componentes descritos para las dos apps: pantalla de carga, botón principal y secundario, campo con etiqueta arriba y ejemplo dentro, tarjeta, skeleton (con movimiento reducido), barra de pasos con paso activo, botón "?" e Inicio con lista de eventos
  - Logo: 2–3 opciones de guantes rojos estilo ilustración con URL de origen y licencia verificada (CC0/dominio público, uso comercial sin atribución obligatoria) en `docs/design/CREDITOS.md`; se pregunta a cris (`uak msg human`) cuál usar **antes** de descargar; la elegida queda en `docs/design/logo/` (SVG o PNG ≥ 1024 px) con su licencia
  - `docs/design/guia-conexion.md`: pasos cortos en español para recuperar la conexión (teléfono y central), sin internet; es el texto que copian AND-020 y DSK-026
  - Revisión con `design-critic` sobre maquetas o capturas
- **Verify:** `test -s docs/DESIGN.md && test -s docs/design/CREDITOS.md && test -s docs/design/guia-conexion.md && grep -q 'v4' docs/DESIGN.md`
- **Next step:** buscar opciones de logo con licencia CC0 y anotarlas en `CREDITOS.md`

## DSK-029 · Inicio con historial de eventos y pantalla de carga

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Arranque e inicio
- **Depends on:** DSK-023, DSN-004
- **Paths:** central/src/main/java/com/sigebox/central/App.java, central/src/main/java/com/sigebox/central/ui/Screens.java, central/src/main/java/com/sigebox/central/ui/inicio/, central/src/main/java/com/sigebox/central/ui/splash/, central/src/main/resources/com/sigebox/central/img/, central/src/test/java/com/sigebox/central/ui/inicio/, central/src/test/java/com/sigebox/central/ui/splash/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al abrir: pantalla de carga con el logo de DSN-004 mientras `Database` migra y `RecoveryService` reconstruye (≥ 600 ms, ≤ 3 s sin error); si falla, el error en español
  - Después de los términos, Inicio: eventos del historial (nombre, fecha, peleas, cerradas), del más reciente al más antiguo, y "Nuevo evento" como acción principal; prueba con 3 eventos
  - Abrir un evento lleva a su pelea activa o a su cartelera; desde cualquier pantalla se puede volver a Inicio
  - Barra de pasos: Evento · Jueces · En vivo · Resultado
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.*'`
- **Next step:** prueba de `InicioScreen` que lista 3 eventos ordenados

## DSK-030 · Quitar Corrección y "Ver resumen parcial"

- **Type:** feature
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** spec 004 §En vivo, §Quitar Corrección · principio 3
- **Depends on:** DSK-029
- **Paths:** central/src/test/java/com/sigebox/central/ui/ScreenshotHarness.java, central/src/main/java/com/sigebox/central/ui/inicio/Nav.java, central/src/test/java/com/sigebox/central/ui/inicio/, central/src/main/java/com/sigebox/central/App.java, central/src/main/java/com/sigebox/central/ui/correction/, central/src/main/java/com/sigebox/central/service/CorrectionService.java, central/src/main/java/com/sigebox/central/ui/rounds/, central/src/test/java/com/sigebox/central/ui/correction/, central/src/test/java/com/sigebox/central/service/CorrectionServiceTest.java, central/src/test/java/com/sigebox/central/ui/rounds/
- **Freeze-Allowed:** no
- **Acceptance:**
  - No existe la sección ni el enlace Corrección (ni "Corregir tarjeta" ni "Capturar desde papel"); enlaces secundarios: Inicio · Terminación · Bitácora
  - En vivo no tiene "Ver resumen parcial"; prueba: ningún nodo de En vivo muestra un total ni un ganador
  - Las tarjetas `CORREGIDA`/`CAPTURADA` de peleas viejas siguen contando en el resultado y en el acta (prueba)
  - Las pruebas que solo cubrían lo quitado se borran junto con su código (no se edita ninguna otra prueba)
  - `CorrectionService` se conserva (crea las tarjetas `CORREGIDA`/`CAPTURADA` de peleas viejas y lo usa `JudgeHubHandoffTest`); se borran `CorrectionScreen` y su captura "05-correccion" del `ScreenshotHarness`
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de que `RoundsScreen` no tiene el botón de resumen parcial

## DSK-031 · Punto menos a una esquina en En vivo

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §En vivo · ADR 0005
- **Depends on:** CORE-008, DSK-030, DSK-025
- **Paths:** central/src/main/java/com/sigebox/central/service/DescuentoService.java, central/src/main/java/com/sigebox/central/db/DescuentoRepository.java, central/src/main/java/com/sigebox/central/ui/rounds/, central/src/main/java/com/sigebox/central/server/ServerEvents.java, central/src/main/java/com/sigebox/central/ui/Labels.java, central/src/test/java/com/sigebox/central/service/DescuentoServiceTest.java, central/src/test/java/com/sigebox/central/db/DescuentoRepositoryTest.java, central/src/test/java/com/sigebox/central/ui/rounds/
- **Freeze-Allowed:** no
- **Acceptance:**
  - "−1 roja" y "−1 azul" con round (por defecto el actual) y confirmación, mientras la pelea no termina; sin límite de veces (prueba con 3 en el mismo round)
  - Se guarda en `descuento` antes de mostrarse y queda en bitácora (`DESCUENTO`, en español con `Labels`)
  - Anular pide motivo, marca anulado (no borra) y queda en bitácora (`DESCUENTO_ANULADO`); prueba
  - Lista de descuentos de la pelea visible en En vivo; ningún total ni ganador (principio 3); `ServerEvents` avisa del cambio
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.service.DescuentoServiceTest' --tests 'com.sigebox.central.db.*' --tests 'com.sigebox.central.ui.rounds.*'`
- **Next step:** `DescuentoServiceTest`: poner y anular un descuento

## DSK-032 · Todas las secciones se actualizan solas, con skeletons

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Datos siempre al día
- **Depends on:** DSK-024, DSK-025, DSK-026, DSK-033
- **Paths:** central/src/main/java/com/sigebox/central/ui/termination/, central/src/main/java/com/sigebox/central/ui/log/, central/src/main/java/com/sigebox/central/ui/setup/, central/src/main/java/com/sigebox/central/ui/inicio/, central/src/main/java/com/sigebox/central/ui/LiveRefresh.java, central/src/test/java/com/sigebox/central/ui/termination/, central/src/test/java/com/sigebox/central/ui/log/, central/src/test/java/com/sigebox/central/ui/LiveRefreshTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Toda pantalla visible (Inicio, Evento, Jueces, En vivo, Resultado, Terminación, Bitácora) muestra cualquier cambio que la afecte ≤ 1 s, vía `ServerEvents` + `Timeline` de 1 s; prueba por pantalla con un cambio externo
  - No queda ningún botón "Actualizar" en la central (prueba que recorre el árbol de nodos)
  - Mientras carga, cada sección muestra el `Skeleton` de DSK-026 en lugar de vacío o "Cargando…"
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.*'`
- **Next step:** `LiveRefreshTest`: Bitácora muestra un evento nuevo sin pulsar nada

## DSK-033 · Código de 6 dígitos por lugar, QR verificado y fin de evento

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Unirse con código y QR · ADR 0004 · plan 004 §Threat model
- **Depends on:** CORE-007, DSK-028
- **Paths:** central/src/main/java/com/sigebox/central/server/SeatTokens.java, central/src/main/java/com/sigebox/central/server/JudgeHub.java, central/src/main/java/com/sigebox/central/service/FightService.java, central/src/main/java/com/sigebox/central/ui/setup/QrCode.java, central/judge-sim/src/, central/src/test/java/com/sigebox/central/server/, central/src/test/java/com/sigebox/central/ui/setup/QrCodeTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Cada lugar tiene un código de 6 dígitos (solo números), único entre los lugares de las peleas abiertas del evento (prueba con 20 peleas × 5 lugares)
  - `CODE_LOOKUP` → `CODE_INFO` (lugar libre) o `CODE_UNKNOWN` (no existe, usado o cerrada); 1 búsqueda/s por conexión; al 5.º `CODE_UNKNOWN` cierra la conexión y lo anota en bitácora (pruebas); `EVENT_ENDED` entrante se rechaza
  - El QR que se muestra contiene el `JoinUri` nuevo: prueba que genera la imagen con zxing, la decodifica y obtiene el mismo URI
  - Al cerrar la última pelea del evento se manda `EVENT_ENDED` a los teléfonos conectados (prueba)
  - `judge-sim --codes C1,C2,C3 --host IP` se une solo con códigos; `--fight/--tokens` sigue funcionando
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.server.*' --tests 'com.sigebox.central.ui.setup.*' && central/gradlew -p central :judge-sim:test`
- **Next step:** prueba de `SeatTokens`: códigos únicos entre peleas abiertas

## AND-018 · Pantalla de carga y unirse con nombre, código e IP

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Arranque, §Unirse con código y QR · ADR 0004
- **Depends on:** CORE-007, DSN-004
- **Paths:** android/app/src/main/java/com/sigebox/juez/MainActivity.java, android/app/src/main/java/com/sigebox/juez/join/JoinActivity.java, android/app/src/main/java/com/sigebox/juez/join/JoinRequest.java, android/app/src/main/java/com/sigebox/juez/join/QrParser.java, android/app/src/main/java/com/sigebox/juez/net/JudgeConnection.java, android/app/src/main/res/layout/activity_join.xml, android/app/src/main/res/values/strings_join.xml, android/app/src/main/res/drawable/splash.xml, android/app/src/main/res/values/themes.xml, android/app/src/main/AndroidManifest.xml, android/app/src/test/java/com/sigebox/juez/join/, android/app/src/test/java/com/sigebox/juez/net/JudgeConnectionCodeTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Pantalla de carga con el logo de DSN-004 al abrir (tema de arranque, sin dependencias nuevas), ≤ 3 s en Android 8
  - Unirse pide solo Nombre, Código (exactamente 6 dígitos, teclado numérico, rechaza otra cosa; prueba) e IP; "Escanear QR" es la acción principal
  - Con el código: `CODE_LOOKUP` → `CODE_INFO` → `JOIN` firmado de siempre (prueba con MockWebServer); `CODE_UNKNOWN` muestra "Ese código no es válido. Pídele a la mesa el código de tu lugar."
  - `QrParser` lee el QR nuevo (`c`) y el viejo (`s`/`seat`/`t`) y llena los campos (pruebas)
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest --tests 'com.sigebox.juez.join.*' --tests 'com.sigebox.juez.net.*'`
- **Next step:** prueba de que el campo Código rechaza 5 dígitos y letras

## AND-019 · Esperar la siguiente pelea o volver al inicio al terminar el evento

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** spec 004 §Al terminar una pelea, en el teléfono · ADR 0004
- **Depends on:** CORE-007, AND-017, AND-018
- **Paths:** android/app/src/main/java/com/sigebox/juez/net/JudgeRuntime.java, android/app/src/main/java/com/sigebox/juez/net/JudgeListener.java, android/app/src/main/java/com/sigebox/juez/wait/, android/app/src/main/res/layout/activity_next_fight.xml, android/app/src/main/res/values/strings_wait.xml, android/app/src/main/AndroidManifest.xml, android/app/src/test/java/com/sigebox/juez/wait/, android/app/src/test/java/com/sigebox/juez/net/JudgeRuntimeTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al recibir `FIGHT_CLOSED` de su pelea: pantalla "Espera la siguiente pelea" (con esquinas si ya llegó `NEXT_FIGHT`) hasta que la central la abra; con `NEXT_FIGHT` sigue como hoy (AND-013)
  - Al recibir `EVENT_ENDED`: "El evento terminó" y regreso a Unirse con el nombre ya escrito; se borra la sesión del lugar (prueba)
  - Nada se pierde: si hay envíos pendientes en la `Outbox`, se entregan antes de cambiar de pantalla (principio 1; prueba)
  - `AutoFinalCard` (AND-017) se registra una vez por proceso en `JudgeRuntime` y no en `ScoreActivity`: si la app reinicia en otra pantalla, la tarjeta final automática sigue funcionando (prueba). Decisión de cris, 2026-11-03
  - La nueva Activity se declara en el manifiesto
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest --tests 'com.sigebox.juez.wait.*' --tests 'com.sigebox.juez.net.*'`
- **Next step:** prueba de `JudgeRuntime` con `EVENT_ENDED`

## AND-020 · Botón "?" con la guía de conexión en el teléfono

- **Type:** feature
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** spec 004 §Ayuda de conexión
- **Depends on:** DSN-004
- **Paths:** android/app/src/main/java/com/sigebox/juez/help/, android/app/src/main/java/com/sigebox/juez/ui/TopBar.java, android/app/src/main/res/layout/dialog_help.xml, android/app/src/main/res/values/strings_help.xml, android/app/src/test/java/com/sigebox/juez/help/
- **Freeze-Allowed:** no
- **Acceptance:**
  - "?" siempre en la barra superior y destacado mientras dice "Sin conexión"; abre la guía de `docs/design/guia-conexion.md` en un diálogo (texto en `strings_help.xml`, sin internet; sin Activity nueva)
  - La guía muestra la IP a la que intenta conectarse y el estado actual
  - Prueba Robolectric: con estado desconectado el "?" es visible y abre la guía
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest --tests 'com.sigebox.juez.help.*'`
- **Next step:** prueba de que `TopBar` muestra "?" sin conexión

## AND-021 · Rediseño v4 de la app del juez

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 004 §Imagen y diseño, §Formularios, §Datos siempre al día · DSN-004
- **Depends on:** DSN-004, AND-017, AND-018, AND-019, AND-020
- **Paths:** android/app/src/main/java/com/sigebox/juez/finalcard/, android/app/src/main/res/values/, android/app/src/main/res/values-night/, android/app/src/main/res/drawable/, android/app/src/main/res/mipmap-anydpi/, android/app/src/main/res/mipmap-hdpi/, android/app/src/main/res/mipmap-mdpi/, android/app/src/main/res/mipmap-xhdpi/, android/app/src/main/res/mipmap-xxhdpi/, android/app/src/main/res/mipmap-xxxhdpi/, android/app/src/main/res/layout/, android/app/src/main/AndroidManifest.xml, android/app/src/main/java/com/sigebox/juez/ui/, android/app/src/test/java/com/sigebox/juez/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Paleta v4 en claro y oscuro con contraste AA (prueba de colores), ícono de la app con el logo, rojo y azul solo para esquinas
  - Tokens `logo_negro`, `logo_rojo` y `logo_claro` en `colors.xml` con los colores exactos del SVG; `drawable/splash.xml` (AND-018, hoy con colores del framework) los usa
  - Todo campo con etiqueta arriba siempre visible y ejemplo dentro (prueba que recorre los layouts)
  - Skeleton con la forma de la tarjeta mientras llega el primer `STATE` (prueba)
  - Se borra `FinalCardActivity` (sin uso desde AND-017) y su entrada del manifiesto
  - Evidencia: capturas en emulador de cada pantalla en claro y oscuro, revisadas con `design-critic`
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest`
- **Next step:** prueba de contraste de `colors.xml` v4

## QA-004 · Integración: evento de 2 peleas con código, descuentos y fin de evento

- **Type:** test
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** spec 004 · principios 1 y 2 · NFR latencia
- **Depends on:** DSK-024, DSK-033, AND-019
- **Paths:** central/src/test/java/com/sigebox/central/it/EventoTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - 3 jueces simulados se unen solo con código; 2 peleas de 3 rounds; 2 descuentos (1 anulado); tarjetas finales automáticas; resultado = cálculo a mano de `core`
  - Al cerrar la pelea 1 los jueces pasan a la 2 sin volver a emparejar; al cerrar la 2 reciben `EVENT_ENDED`
  - Corte de 60 s de un juez durante la pelea 2: 0 calificaciones perdidas; último `ACK` → Resultado listo ≤ 1 s (p95)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.it.EventoTest'`
- **Next step:** esqueleto de `EventoTest` a partir de `FuncionFlowTest`

## DSK-034 · Etiqueta en español para `CARD_OVER_CAPTURE` (followup de DSK-028)

- **Type:** fix
- **Priority:** P0
- **Estimate:** 30 min
- **Rubric / Spec:** spec 003 §Español en las interfaces · `main` en rojo tras DSK-028 + DSK-025
- **Depends on:** DSK-028, DSK-025
- **Paths:** central/src/main/java/com/sigebox/central/ui/Labels.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - `LabelsTest.cadaTipoDeEventoQueEscribeLaCentralTieneEtiqueta` pasa sin editarla: `CARD_OVER_CAPTURE` tiene etiqueta en español
  - La suite completa de la central pasa
- **Verify:** `central/gradlew -p central test`
- **Next step:** agregar la entrada a `Labels.EVENTOS`

## DSK-035 · Inicio solo con eventos; barra del evento con Bitácora y regreso a Inicio

- **Type:** feature
- **Priority:** P0
- **Estimate:** 4 h
- **Rubric / Spec:** spec 005 §Inicio y navegación
- **Depends on:** None
- **Paths:** central/src/main/java/com/sigebox/central/App.java, central/src/main/java/com/sigebox/central/ui/Screens.java, central/src/main/java/com/sigebox/central/ui/inicio/Nav.java, central/src/main/java/com/sigebox/central/ui/inicio/InicioScreen.java, central/src/main/java/com/sigebox/central/ui/theme/StepsBar.java, central/src/test/java/com/sigebox/central/ui/inicio/, central/src/test/java/com/sigebox/central/ui/theme/ComponentsTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Inicio muestra solo la lista de eventos, "Nuevo evento", "?" y el cambio de tema; sin barra de pasos ni enlaces del evento (prueba)
  - Dentro de un evento la barra es Evento · Jueces · En vivo · Resultado con los enlaces Inicio · Terminación · Bitácora, "?" y tema; Terminación sale en DSK-036, cuando ya vive en En vivo (prueba)
  - Volver a Inicio quita la barra; entrar a otro evento muestra ese evento en todas las secciones (prueba)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.inicio.*' --tests 'com.sigebox.central.ui.theme.*'`
- **Next step:** prueba de que Inicio no tiene StepsBar

## DSK-036 · En vivo: abrir round solo con los jueces conectados y terminar la pelea ahí mismo

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 005 §En vivo y terminación · principios 1 y 2
- **Depends on:** DSK-035, DSK-040
- **Paths:** central/src/main/java/com/sigebox/central/ui/inicio/Nav.java, central/src/test/java/com/sigebox/central/ui/inicio/NavTest.java, central/src/main/java/com/sigebox/central/ui/rounds/RoundsScreen.java, central/src/main/java/com/sigebox/central/ui/termination/, central/src/main/java/com/sigebox/central/service/TerminationService.java, central/src/main/java/com/sigebox/central/service/ResultService.java, central/src/main/java/com/sigebox/central/ui/result/ResultStepScreen.java, central/src/main/java/com/sigebox/central/App.java, central/src/test/java/com/sigebox/central/ui/rounds/RoundsScreenTest.java, central/src/test/java/com/sigebox/central/ui/rounds/RoundsGateTest.java, central/src/test/java/com/sigebox/central/ui/termination/, central/src/test/java/com/sigebox/central/service/TerminationServiceTest.java, central/src/test/java/com/sigebox/central/service/TerminationAutoCloseTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - "Abrir round" desactivado mientras falte conectar un juez; dice cuál falta y "…o baja los jueces en Evento si no va a llegar"; cerrar un round abierto sigue igual (prueba)
  - "Terminar pelea" en En vivo: el round es el abierto o el último cerrado (sin escribirlo), se pide solo el motivo y se confirma (prueba)
  - Al confirmar: la pelea queda cerrada con su resultado sin pulsar "Cerrar pelea", los teléfonos reciben `FIGHT_CLOSED` y `NEXT_FIGHT` o `EVENT_ENDED` como en un cierre normal, En vivo se actualiza en ≤ 1 s y se abre Resultado con la terminación (prueba que primero reproduce el fallo)
  - "Terminación" sale de los enlaces de la barra (quedan Inicio · Bitácora)
  - `RoundsScreenTest.thereIsOneActionCardWithTheRoundButtonOnly` y `NavTest.secondaryLinksAreInicioTerminacionAndBitacora` se reemplazan con `uak decision` (spec 005 §Decisions)
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba que termina una pelea y espera Resultado con la terminación sin cerrar a mano

## DSK-037 · Evento: "Agregar pelea" a la izquierda y bajar jueces o rounds del evento

- **Type:** feature
- **Priority:** P1
- **Estimate:** 5 h
- **Rubric / Spec:** spec 005 §Evento
- **Depends on:** None
- **Paths:** central/src/main/java/com/sigebox/central/ui/funcion/, central/src/main/java/com/sigebox/central/service/FuncionService.java, central/src/main/java/com/sigebox/central/db/FuncionRepository.java, central/src/main/java/com/sigebox/central/db/LugarJuezRepository.java, central/src/test/java/com/sigebox/central/ui/funcion/, central/src/test/java/com/sigebox/central/service/FuncionServiceTest.java, central/src/test/java/com/sigebox/central/service/FuncionCambioTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - "Agregar pelea" va a la izquierda de los campos de las esquinas, en la misma fila (prueba de orden de nodos)
  - Bajar jueces (1–5) o rounds (1–12) del evento cambia la base y todas las peleas programadas (sin empezar); no toca las empezadas o terminadas; anota el cambio en la Bitácora (prueba). La pelea que ya espera en Jueces la cubre DSK-040
  - Una pelea con su primer round abierto no cambia y la pantalla dice por qué (prueba)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.funcion.*' --tests 'com.sigebox.central.service.Funcion*'`
- **Next step:** prueba de que bajar a 2 jueces quita el lugar 3 de la pelea pendiente

## DSK-038 · Jueces: sin crear peleas y solo códigos de 6 números

- **Type:** fix
- **Priority:** P1
- **Estimate:** 3 h
- **Rubric / Spec:** spec 005 §Jueces
- **Depends on:** None
- **Paths:** central/src/main/java/com/sigebox/central/ui/setup/SetupScreen.java, central/src/test/java/com/sigebox/central/ui/setup/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Jueces no tiene formulario "Crear pelea"; empareja la pelea que sigue del evento; sin pelea pendiente dice "Agrega una pelea en Evento" con botón a Evento (prueba)
  - Solo se muestran los códigos de 6 números por lugar; el identificador de la pelea no aparece como "código" (prueba)
- **Verify:** `central/gradlew -p central test --tests 'com.sigebox.central.ui.setup.*'`
- **Next step:** prueba de que SetupScreen sin pelea no tiene campos de rounds ni jueces

## DSK-039 · Misma línea en todas las pantallas de la central

- **Type:** fix
- **Priority:** P1
- **Estimate:** 4 h
- **Rubric / Spec:** spec 005 §Una sola línea
- **Depends on:** DSK-035, DSK-036, DSK-037, DSK-038
- **Paths:** central/src/main/java/com/sigebox/central/ui/, central/src/main/java/com/sigebox/central/App.java, docs/runbook.md, central/src/test/java/com/sigebox/central/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Cada título coincide con su sección de la barra; mismo verbo para la misma acción ("Nuevo evento" en Inicio y en el botón que lo crea); preguntas con "¿…?"; botones con mayúscula solo al inicio; "Qué sigue" con el mismo patrón
  - `ConsistencyTest` recorre todas las pantallas y falla en cualquiera de esos casos
  - Evidencia: capturas de cada pantalla en claro y oscuro revisadas con `design-critic`
  - Jueces sin pelea: botón "Ir a Evento" que lleva a Evento (spec 005 §Jueces; followup de DSK-038, que no tenía `App.java`) (prueba)
- **Verify:** `central/gradlew -p central test`
- **Next step:** `ConsistencyTest` con los títulos de cada sección

## DSK-040 · Bajar jueces o rounds de la pelea preparada antes del primer round

- **Type:** feature
- **Priority:** P0
- **Estimate:** 5 h
- **Rubric / Spec:** spec 005 §Evento · principios 1 y 2 · ADR 0004 (código de lugar)
- **Depends on:** DSK-037
- **Paths:** central/src/main/java/com/sigebox/central/service/FightService.java, central/src/main/java/com/sigebox/central/db/PeleaRepository.java, central/src/main/java/com/sigebox/central/db/LugarJuezRepository.java, central/src/main/java/com/sigebox/central/server/SeatTokens.java, central/src/main/java/com/sigebox/central/server/JudgeHub.java, central/src/main/java/com/sigebox/central/ui/funcion/, central/src/main/java/com/sigebox/central/service/FuncionService.java, central/src/main/java/com/sigebox/central/App.java, central/src/test/java/com/sigebox/central/service/FightCambioTest.java, central/src/test/java/com/sigebox/central/service/FuncionCambioTest.java, central/src/test/java/com/sigebox/central/service/FuncionPreparadaTest.java, central/src/test/java/com/sigebox/central/server/SeatTokensTest.java, central/src/test/java/com/sigebox/central/server/JudgeHubCambioTest.java, central/src/test/java/com/sigebox/central/ui/funcion/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Mientras la pelea preparada (en Jueces) no haya abierto su primer round, Evento permite bajar sus jueces (1–5) y cambiar sus rounds (1–12), desde la base del evento o desde su fila; con un round abierto o cerrado no cambia y dice por qué (prueba). Sus jueces no suben (Jueces guarda el código de cada lugar): subir la base la deja como está
  - Al cambiar los rounds de la pelea preparada, los teléfonos ya aprobados reciben un `STATE` nuevo (el teléfono usa `rounds` para la tarjeta final) (prueba)
  - Nunca quita un lugar con un juez aprobado o esperando aprobación: si bajar dejaría fuera a uno, no cambia nada y dice qué juez es (prueba)
  - Los lugares que sobran pierden su código (por lugar, no toda la pelea): el código viejo responde `CODE_UNKNOWN` y una sesión abierta en ese lugar se cierra (prueba)
  - El cambio se anota en la Bitácora como `CARTELERA_CAMBIADA`; Jueces y En vivo muestran los lugares nuevos en ≤ 1 s
  - Revisión con `security-reviewer` (toca códigos y sesiones)
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de que bajar a 2 jueces con el lugar 3 sin juez quita su código

## DSN-005 · Guía visual v5 «Arena nocturna»

- **Type:** design
- **Priority:** P0
- **Estimate:** 0.5 d
- **Rubric / Spec:** spec 006
- **Depends on:** DSN-004
- **Paths:** docs/DESIGN.md, docs/design/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `docs/DESIGN.md` v5 (reemplaza v4, conserva sus principios vigentes): barra lateral, encabezado, aviso "Qué sigue", stepper, menú ⋯, chips, avatar, anillo de avance y segmentos de rounds; tokens de radio, espaciado y movimiento con los mismos nombres en las dos apps
  - Tabla AA medida para cada par nuevo (fondos tenues de conectado y pendiente, chips) en claro y oscuro
  - Íconos: set elegido con licencia libre y URL anotados en `docs/design/CREDITOS.md`; lista de los íconos que usa cada app
  - Movimiento: duraciones, curvas y qué hace "reducir movimiento" en cada app
- **Verify:** `grep -q 'v5' docs/DESIGN.md && grep -q 'Lucide' docs/design/CREDITOS.md`
- **Next step:** tokens nuevos y tabla AA en DESIGN.md

## DSK-041 · Central v5: íconos, componentes y movimiento

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Central, §Las dos
- **Depends on:** DSN-005
- **Paths:** central/src/main/java/com/sigebox/central/ui/theme/, central/src/main/resources/com/sigebox/central/app.css, central/src/main/resources/com/sigebox/central/light.css, central/src/main/resources/com/sigebox/central/dark.css, central/src/main/resources/com/sigebox/central/v5-shell.css, central/src/main/resources/com/sigebox/central/v5-evento.css, central/src/main/resources/com/sigebox/central/v5-envivo.css, central/src/main/resources/com/sigebox/central/v5-resultado.css, central/src/test/java/com/sigebox/central/ui/theme/
- **Freeze-Allowed:** no
- **Acceptance:**
  - `Icons`: íconos de línea como `SVGPath` (sin dependencias), con color del tema y tamaño 16/20/24 (prueba)
  - `Stepper` (− valor +) con rango, teclado (flechas, + y −) y texto accesible; `ActionMenu` (⋯), `Callout` ("Qué sigue" con ícono), `Chip`, `Avatar` (iniciales) (prueba)
  - `Motion`: entrada de pantalla (fundido + 8 px, ≤ 220 ms), hover y presión; con "reducir movimiento" solo fundidos ≤ 150 ms (prueba)
  - Tokens v5 en `app.css`, `light.css` y `dark.css`; las hojas `v5-*.css` existen y `Theme` las carga, para que cada área tenga la suya
  - Las pantallas actuales siguen funcionando (suite completa en verde)
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de `Stepper` con rango y teclado

## DSK-042 · Central v5: barra lateral, encabezado y transición entre pantallas

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Central
- **Depends on:** DSK-041
- **Paths:** central/src/main/java/com/sigebox/central/ui/theme/SideNav.java, central/src/main/java/com/sigebox/central/ui/theme/StepsBar.java, central/src/main/java/com/sigebox/central/ui/theme/PageHeader.java, central/src/main/java/com/sigebox/central/ui/inicio/Nav.java, central/src/main/java/com/sigebox/central/App.java, central/src/main/resources/com/sigebox/central/v5-shell.css, central/src/test/java/com/sigebox/central/ui/theme/SideNavTest.java, central/src/test/java/com/sigebox/central/ui/theme/PageHeaderTest.java, central/src/test/java/com/sigebox/central/ui/inicio/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Barra lateral: logo, evento en curso con su avance, pasos con ícono (✓ terminados, punto que late en En vivo con round abierto), Bitácora, Inicio, ayuda y tema; fuera del evento, como spec 005 (prueba)
  - `PageHeader`: contexto, título de la sección y acción principal a la derecha (prueba)
  - Cambio de pantalla con `Motion` (prueba con reducir movimiento)
  - La API de la barra (`select`, `showEvent`) se conserva: `NavModeTest` y `ConsistencyTest` siguen en verde
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de `SideNav` con el avance del evento

## DSK-043 · Central v5: Inicio y Evento

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Central
- **Depends on:** DSK-042
- **Paths:** central/src/main/java/com/sigebox/central/ui/inicio/InicioScreen.java, central/src/main/java/com/sigebox/central/ui/inicio/NuevoEventoScreen.java, central/src/main/java/com/sigebox/central/ui/inicio/CarteleraScreen.java, central/src/main/java/com/sigebox/central/ui/funcion/, central/src/main/resources/com/sigebox/central/v5-evento.css, central/src/test/java/com/sigebox/central/ui/funcion/, central/src/test/java/com/sigebox/central/ui/inicio/InicioV5Test.java, central/src/test/java/com/sigebox/central/ui/inicio/InicioTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Cartelera como lista: número, esquinas con punto de color, rounds, jueces, estado y menú ⋯ con todas las acciones de hoy (prueba)
  - Base del evento y nuevo evento con `Stepper`; ningún `Spinner` en estas pantallas (prueba)
  - Inicio con tarjetas de evento y estado vacío dibujado
  - Capturas claro y oscuro revisadas por un crítico de diseño
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de que la fila de la cartelera tiene menú ⋯ y no botones sueltos

## DSK-044 · Central v5: Jueces y En vivo

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Central · principio 3
- **Depends on:** DSK-042
- **Paths:** central/src/main/java/com/sigebox/central/ui/setup/, central/src/main/java/com/sigebox/central/ui/rounds/, central/src/main/java/com/sigebox/central/ui/termination/, central/src/main/resources/com/sigebox/central/v5-envivo.css, central/src/test/java/com/sigebox/central/ui/setup/, central/src/test/java/com/sigebox/central/ui/rounds/, central/src/test/java/com/sigebox/central/ui/termination/
- **Freeze-Allowed:** no
- **Acceptance:**
  - En vivo: anillo de tarjetas recibidas, segmentos de rounds y una tarjeta por juez (iniciales, conexión, estado del round); sin puntos ni totales (prueba)
  - Jueces: tarjetas de lugar con código grande, QR y estado con chip (prueba)
  - Capturas claro y oscuro revisadas por un crítico de diseño
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de que En vivo muestra el anillo con N de M tarjetas y ningún marcador

## DSK-045 · Central v5: Resultado, ventana de resultado, Bitácora y ayuda

- **Type:** feature
- **Priority:** P2
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Central
- **Depends on:** DSK-042
- **Paths:** central/src/main/java/com/sigebox/central/ui/result/, central/src/main/java/com/sigebox/central/ui/log/, central/src/main/java/com/sigebox/central/ui/help/, central/src/main/java/com/sigebox/central/ui/terms/, central/src/main/java/com/sigebox/central/ui/splash/, central/src/main/resources/com/sigebox/central/v5-resultado.css, central/src/test/java/com/sigebox/central/ui/result/, central/src/test/java/com/sigebox/central/ui/log/, central/src/test/java/com/sigebox/central/ui/help/, central/src/test/java/com/sigebox/central/ui/terms/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Resultado y su ventana con el encabezado v5; los totales entran contando (solo con la pelea cerrada)
  - Bitácora con chips de tipo y filtros v5; ayuda y términos con el mismo estilo
  - Capturas claro y oscuro revisadas por un crítico de diseño
- **Verify:** `central/gradlew -p central test`
- **Next step:** prueba de la ventana de resultado con el encabezado v5

## DSK-046 · Follow-up de DSK-040: STATE al aprobar y al reenviar un JOIN aprobado

- **Type:** fix
- **Priority:** P0
- **Estimate:** 1 h
- **Rubric / Spec:** spec 005 §En vivo
- **Depends on:** DSK-040
- **Paths:** central/src/main/java/com/sigebox/central/server/JudgeHub.java, central/src/test/java/com/sigebox/central/server/JudgeHubCodeTest.java
- **Freeze-Allowed:** no
- **Acceptance:**
  - Al aprobar un juez, el teléfono recibe `STATE` antes de `JOIN_APPROVED` sin reconectar (prueba)
  - Un `JOIN` de un lugar ya aprobado recibe `STATE` antes de `JOIN_APPROVED` (prueba)
  - Con eso el teléfono sale del esqueleto y manda la tarjeta final automática al terminar la pelea
- **Verify:** `central/gradlew -p central test`
- **Next step:** PR con las dos pruebas en verde

## AND-022 · Juez v5: tokens, íconos, chip de conexión y movimiento

- **Type:** feature
- **Priority:** P0
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Juez, §Las dos
- **Depends on:** DSN-005
- **Paths:** android/app/src/main/res/values/colors.xml, android/app/src/main/res/values/themes.xml, android/app/src/main/res/values/dimens.xml, android/app/src/main/res/values/strings.xml, android/app/src/main/res/values-night/, android/app/src/main/res/values-sw400dp/, android/app/src/main/res/drawable/, android/app/src/main/res/color/, android/app/src/main/res/anim/, android/app/src/main/res/layout/view_top_bar.xml, android/app/src/main/res/layout/view_top_bar_brand.xml, android/app/src/main/java/com/sigebox/juez/ui/, android/app/src/test/java/com/sigebox/juez/ui/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Tokens v5 en colores, dimensiones y estilos (mismos nombres que la central)
  - Íconos como vectores `ic_*` del set de DSN-005
  - Chip de conexión en la barra: Conectado (punto que late) y Sin conexión (anillo y texto) (prueba)
  - `Motion`: transición de eje compartido entre pantallas y lectura de la escala de animación del sistema (prueba con escala 0)
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest`
- **Next step:** prueba del chip de conexión con sus dos estados

## AND-023 · Juez v5: Puntuar, Enviada, Round tardío y Tarjeta final

- **Type:** feature
- **Priority:** P1
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Juez · principio 3
- **Depends on:** AND-022
- **Paths:** android/app/src/main/java/com/sigebox/juez/score/, android/app/src/main/java/com/sigebox/juez/late/, android/app/src/main/java/com/sigebox/juez/finalcard/, android/app/src/main/res/layout/activity_score.xml, android/app/src/main/res/layout/activity_late_score.xml, android/app/src/main/res/layout/view_corner_card.xml, android/app/src/main/res/layout/view_confirm_sheet.xml, android/app/src/main/res/values/strings_score.xml, android/app/src/main/res/values/styles_score.xml, android/app/src/test/java/com/sigebox/juez/score/, android/app/src/test/java/com/sigebox/juez/late/, android/app/src/test/java/com/sigebox/juez/finalcard/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Segmentos de rounds; el número rueda al cambiar y hay háptica en − y + (prueba)
  - Con el `ACK`, "Round N enviado" con palomita que se dibuja, la tarjeta enviada y "Corregir esta tarjeta" (prueba)
  - Sin animación del sistema, los cambios aparecen sin movimiento (prueba)
  - Capturas claro y oscuro revisadas por un crítico de diseño
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest`
- **Next step:** prueba de la pantalla Enviada tras el ACK

## AND-024 · Juez v5: Unirse, Espera de aprobación, Siguiente pelea y Términos

- **Type:** feature
- **Priority:** P2
- **Estimate:** 1 d
- **Rubric / Spec:** spec 006 §Juez
- **Depends on:** AND-022
- **Paths:** android/app/src/main/java/com/sigebox/juez/join/, android/app/src/main/java/com/sigebox/juez/wait/, android/app/src/main/java/com/sigebox/juez/terms/, android/app/src/main/java/com/sigebox/juez/help/, android/app/src/main/res/layout/activity_join.xml, android/app/src/main/res/layout/activity_scan.xml, android/app/src/main/res/layout/activity_terms.xml, android/app/src/main/res/layout/activity_waiting.xml, android/app/src/main/res/layout/activity_next_fight.xml, android/app/src/main/res/layout/dialog_help.xml, android/app/src/main/res/values/strings_join.xml, android/app/src/main/res/values/styles_join.xml, android/app/src/test/java/com/sigebox/juez/join/, android/app/src/test/java/com/sigebox/juez/wait/, android/app/src/test/java/com/sigebox/juez/help/
- **Freeze-Allowed:** no
- **Acceptance:**
  - Unirse con campos v5 e ícono; Espera y Siguiente pelea con estado ilustrado y chip de conexión (prueba)
  - Términos y ayuda con el mismo estilo
  - Capturas claro y oscuro revisadas por un crítico de diseño
- **Verify:** `android/gradlew -p android :app:testDebugUnitTest`
- **Next step:** prueba de la pantalla de espera con el chip de conexión
