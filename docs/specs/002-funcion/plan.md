# Plan 002 · Función: cartelera con configuración base

## Design
- **Central (`BOXCENTRAL/`):**
  - `FuncionService`: crea la función con su base (rounds, jueces), administra la cartelera (agregar, quitar, reordenar, saltar, cambiar rounds o jueces de una pelea que no ha empezado) y crea la `Pelea` de la siguiente fila con `FightService` cuando el organizador la inicia. Sigue habiendo una sola pelea activa.
  - Al iniciar la siguiente pelea, copia los lugares aprobados de la pelea anterior (`LugarJuez`: lugar, nombre, dispositivo, huella de la llave, aprobado) a la nueva, en una sola transacción, y solo después avisa a los teléfonos (principio 1: persistir antes de difundir).
  - `JudgeHub` manda `NEXT_FIGHT` a cada teléfono conectado. Si un teléfono vuelve con `HELLO` de una pelea ya cerrada y su llave está en la pelea activa de la misma función, le responde `NEXT_FIGHT` en vez de rechazarlo.
  - UI: pantalla Función (cartelera) y botón "Siguiente pelea" en Resultado. Va después del rediseño v3 (DSK-018), que es dueño de `ui/`.
- **App del juez (`app/`, cris):** al recibir `NEXT_FIGHT` guarda la nueva pelea y el lugar en `local_state` (sigue aprobado), cierra el socket y reconecta con `HELLO` a la nueva pelea; abre directo la pantalla de puntuación. Sin lugar (`seat` nulo): pantalla "No participas en esta pelea" y espera el siguiente `NEXT_FIGHT`.
- **Por qué así:**
  - *Alternativa 1, reusar la misma `Pelea` para toda la función:* mezcla tarjetas, resultado y bitácora de peleas distintas; rompe RecoveryService, ResultService y la exportación.
  - *Alternativa 2, que el juez vuelva a escanear un QR por pelea:* es justo lo que hay que evitar (NFR Pairing × peleas de la noche).
  - *Alternativa 3, que el teléfono descubra la pelea nueva solo (sondeo):* no hay canal sin una pelea autenticada; `NEXT_FIGHT` por el socket ya autenticado es lo más simple y seguro.

## Data and migrations
Solo **expand**, sin tocar tablas existentes (reversible borrando las tablas nuevas):
- `funcion(id TEXT PK, fecha TEXT, base_rounds INT, base_jueces INT, creada_ms INT)`.
- `cartelera(funcion_id TEXT, orden INT, esquina_roja TEXT, esquina_azul TEXT, rounds INT NULL, jueces INT NULL, estado TEXT CHECK(estado IN ('PROGRAMADA','EN_CURSO','CERRADA','SALTADA')), pelea_id TEXT NULL, PK(funcion_id, orden))`. `rounds`/`jueces` nulos = usa la base.
- Bitácora: `FUNCION_CREADA`, `CARTELERA_CAMBIADA`, `PELEA_SALTADA`, `SIGUIENTE_PELEA` (en la función y en las dos peleas).
- Las peleas sueltas de hoy (sin función) siguen funcionando igual.

## Contracts
Mensaje nuevo en `core` (ADR 0002), versión `v: 1`, mismo formato que los demás:
- `NEXT_FIGHT` (central → juez): `{v, id, type, fightId, nextFightId, seat|null, redCorner, blueCorner, rounds}`.
  - `fightId`: la pelea que termina (el teléfono la reconoce). `seat`: su lugar en la nueva pelea, o `null` si no participa.
  - Sin puntos, totales ni nombres de otros jueces (principio 3).
- Schema `core/schemas/next_fight.schema.json`, fixture `demo_data/mensajes/NEXT_FIGHT.json`, `MessageType.NEXT_FIGHT`.
- `HELLO` no cambia: el teléfono firma `helloText(nextFightId, seat, deviceId, nonce)` con su misma llave.
- Consumidores: `JudgeHub` (central), `JudgeSession` (app), `SimJudge` (judge-sim). Un consumidor viejo ignora el tipo desconocido; por eso la app y la central se publican juntas.

## Threat model (STRIDE)
| Threat | Vector | Mitigation | Test |
|---|---|---|---|
| Spoofing | un teléfono ajeno se cuela en la pelea nueva | solo se copian lugares **aprobados** con su huella de llave; el `HELLO` a la pelea nueva exige firma con esa llave (igual que hoy al reconectar) | `HELLO` con otra llave a la pelea nueva → rechazo |
| Tampering | un cliente manda un `NEXT_FIGHT` falso | `NEXT_FIGHT` solo va de central a juez; el servidor rechaza ese tipo si lo recibe | prueba de `JudgeHub` |
| Repudiation | discusión sobre quién juzgó qué pelea | bitácora `SIGUIENTE_PELEA` con lugares copiados y huellas, en ambas peleas | prueba de `FuncionService` |
| Information disclosure | el teléfono ve datos de otros | `NEXT_FIGHT` solo lleva esquinas, rounds y su lugar | schema sin campos extra (`additionalProperties: false`) |
| Denial of service | cambio de pelea con teléfonos desconectados | el teléfono recibe `NEXT_FIGHT` al reconectar con `HELLO` de la pelea cerrada | prueba de reconexión |
| Elevation of privilege | un juez de la pelea 1 juzga una pelea con menos lugares | solo se copian los lugares 1..N; los demás reciben `seat: null` | prueba con menos jueces |

## Observability
Bitácora de la central (sin datos personales nuevos). judge-sim mide el tiempo de cambio de pelea (meta ≤ 5 s con 5 jueces).

## Rollout and rollback
- Se publican juntas la central y la APK que entienden `NEXT_FIGHT`.
- Sin función, todo funciona como hoy (peleas sueltas).
- Rollback: volver a la versión anterior de ambas apps; las tablas nuevas se ignoran. En el evento, si algo falla, se crea la pelea a mano como hoy y los jueces escanean el QR.

## Cost
Cero: local, sin servicios externos.

## Tasks
CORE-005 → (DSK-019 ∥ AND-013) → DSK-020 → DSK-021 (tras DSK-018) → QA-003. Cada una empieza con una prueba.
