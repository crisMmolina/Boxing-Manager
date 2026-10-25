# Plan 001 · SIGEBOX v1

## Design
- **Red:** un Galaxy A37 hace de punto de acceso (sin internet). La laptop central y los teléfonos de los jueces se conectan a él. Respaldo: router de viaje sin internet.
- **Central (`BOXCENTRAL/`):** JavaFX 25 (UI) + Javalin 7 (WebSocket, puerto 8765) + SQLite WAL (sqlite-jdbc). La UI llama servicios internos (`FightService`, `RoundService`, `ResultService`, `ExportService`); los teléfonos solo hablan por WebSocket. Empaquetado con jpackage (requiere Windows).
- **Juez (`app/`):** Android Java, vistas XML + Material, OkHttp WebSocket, Room (bandeja de salida), CameraX + ML Kit (QR), `bindProcessToNetwork` al Wi‑Fi sin internet.
- **Core (`core/`):** Java 17 puro. Paquetes `com.sigebox.core.model`, `.protocol` (DTO + JSON con Gson), `.validation`, `.scoring`.
- Por qué: un solo cálculo compartido (principio 2); WebSocket da latencia < 1 s y empuje del servidor (vibración del faltante); SQLite WAL sobrevive cierres forzados. Alternativas descartadas: HTTP polling (latencia y batería), Bluetooth (alcance y emparejamiento de 5 equipos).

## Data and migrations
Tablas SQLite en la central (ver IDEA §6): `fight`, `judge_seat`, `round`, `round_card`, `card_version`, `final_card`, `termination`, `result`, `event_log`, más `processed_message(id)` para idempotencia. En el teléfono (Room): `outbox(message_id, payload, sent_at, acked)` y `local_state`. Migraciones numeradas `V1__init.sql`… aplicadas al iniciar; solo aditivas en v1.

## Contracts
Mensajes WS JSON, versión `v: 1`, cada uno con `id` (UUID) y `type`: `JOIN`, `JOIN_PENDING`, `JOIN_APPROVED`, `JOIN_REJECTED`, `HELLO`, `STATE`, `ROUND_OPENED`, `ROUND_CLOSED`, `SUBMIT_ROUND`, `ACK`, `SUBMIT_FINAL_CARD`, `FIGHT_ENDED`, `FIGHT_CLOSED`, `PING`, `PONG`. JSON Schema en `core/schemas/*.schema.json`; DTO en `core/src/main/java/com/sigebox/core/protocol/`. Una prueba verifica que DTO y Schema coinciden. URI de unión: `sigebox://join?h=<host>&p=<puerto>&s=<fightId>&seat=<n>&t=<token>`. Consumidores: `app/` y `BOXCENTRAL/` (y `judge-sim`).

## Threat model (STRIDE)
| Threat | Vector | Mitigation | Test |
|---|---|---|---|
| Spoofing | otro teléfono se hace pasar por un juez | token por lugar en el QR (un solo uso) + aprobación manual del organizador + huella de credencial del dispositivo | test de `JOIN` con token inválido o reusado |
| Tampering | mensaje con valores fuera de 0–10 o de otro juez | validación en `core` en el servidor; el juez se toma de la sesión, no del mensaje | prueba de contrato con mensajes manipulados |
| Repudiation | "yo no envié eso" | bitácora + versiones de tarjeta con hora; huella (hash) de tarjeta final | test de bitácora |
| Information disclosure | un juez ve tarjetas ajenas o totales | `STATE` filtrado por juez; sin totales hasta `FIGHT_CLOSED`; datos nunca salen de la LAN | test de `STATE` por juez |
| Denial of service | inundación de mensajes en la LAN | límite de tamaño de mensaje y de mensajes por conexión; sesiones no aprobadas se cierran | test de mensaje grande |
| Elevation of privilege | un juez abre/cierra rounds | solo la UI local de la central (no hay mensajes WS de control desde teléfonos) | test: mensaje de control desde juez → rechazo |

## Observability
Bitácora de eventos en SQLite (sin datos personales más allá del nombre del juez). Log local rotado. La central muestra estado de conexión por juez y `PING/PONG`.

## Rollout and rollback
Hito 5: función real con tarjetas de papel en paralelo; si no coincide, gana el papel. Rollback: volver a papel; los datos quedan en SQLite.

## Cost
0 en infraestructura (todo local). Equipo: laptop Windows + Galaxy A37 (IDEA §9).

## Tasks
Ver `.uak/TASKS.md`: CORE-*, AND-*, DSK-*, QA-*. Cada una empieza con una prueba que falla.
