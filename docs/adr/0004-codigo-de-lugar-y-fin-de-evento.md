# ADR 0004 · Unirse con el código de lugar y aviso de fin de evento
- Status: accepted · Date: 2026-11-03 · Decider: cris · Spec: 004 §Unirse con código y QR, §Al terminar una pelea

## Context
Hoy el teléfono necesita tres datos además de la IP: el id de la pelea (6 letras y números, `FightService.ID_ALPHABET`), el número de lugar y el token del lugar (6 dígitos, `SeatTokens`). El campo de código del teléfono solo acepta números y permite 7. Spec 004 pide que el juez escriba solo nombre, un código de números y la IP.

`JOIN` firma `DeviceProof.joinText(fightId, seat, token, deviceId, nonce)`, así que un teléfono que solo conoce el token no puede armar el `JOIN` firmado de hoy. Además, la central nunca confía en `seat` ni en `fightId` de un mensaje: los toma de la sesión aprobada.

Tampoco existe un aviso de que el evento no tiene más peleas: `JudgeHub` solo manda `NEXT_FIGHT` si hay una siguiente, y el teléfono ignora `FIGHT_CLOSED`.

## Decision
Son cambios aditivos en el protocolo v1: `v` sigue en 1 y `JOIN`, `HELLO` y `DeviceProof` no cambian.
- **Código de lugar = token del lugar.** Son 6 dígitos y la central los genera únicos entre los lugares de las peleas abiertas del evento.
- **`CODE_LOOKUP {v, id, type, code}`** (teléfono → central), con `code` de exactamente 6 dígitos. Se manda después de `CHALLENGE` y antes de `JOIN`.
- **`CODE_INFO {v, id, type, code, fightId, seat, redCorner, blueCorner}`** (central → teléfono) cuando el código es de un lugar libre. Con esos datos el teléfono arma el `JOIN` de siempre.
- **`CODE_UNKNOWN {v, id, type, code}`** cuando el código no existe, ya se usó o es de una pelea cerrada. No dice cuál de las tres.
- **Límite:** a lo más 1 `CODE_LOOKUP` por segundo por conexión. Tras 5 `CODE_UNKNOWN` en una conexión, la central la cierra y lo anota en la bitácora. La aprobación del organizador sigue siendo obligatoria para unirse.
- **`EVENT_ENDED {v, id, type, fightId}`** (central → teléfono). Va después de `FIGHT_CLOSED` cuando la pelea cerrada era la última del evento (no queda ninguna sin cerrar ni saltar). El teléfono vuelve al inicio.
- **QR:** pasa a `sigebox://join?h=<host>&p=<puerto>&c=<código>[&f=<huella>]`. `JoinUri.parse` sigue aceptando la forma vieja (`s`, `seat`, `t`) para no romper QR impresos ni el `judge-sim` actual.

## Alternatives
- **Quitar `fightId` y `seat` de `JOIN` y firmar sobre el código:** sería un cambio que rompe y obligaría a `v` 2. También habría que cambiar `DeviceProof`, `HELLO` y la reconexión. No.
- **Hacer numérico el id de la pelea y seguir pidiendo lugar y token:** el juez seguiría tecleando tres datos. No cumple la spec.
- **Una URL HTTP de búsqueda en la central:** sería un segundo canal fuera del WebSocket, sin `CHALLENGE` y sin bitácora. No.
- **Avisar el fin de evento con un campo nuevo en `FIGHT_CLOSED`:** cambiaría un mensaje que ya existe. Un tipo nuevo se valida aparte. No.

## Consequences
- Consumidores: `android/` (`join/JoinActivity`, `QrParser`, `net/JudgeConnection`, `net/JudgeRuntime`), `central/` (`server/JudgeHub`, `server/SeatTokens`, `service/FightService`, `ui/setup`) y `central/judge-sim` (nueva opción `--codes`; `--fight` y `--tokens` siguen funcionando).
- Con 6 dígitos hay 1 000 000 de códigos. El límite de intentos y la aprobación manual hacen inútil adivinar en una red local.
- Revertir: los mensajes nuevos se dejan de mandar y el teléfono vuelve al formulario de tres datos. `JOIN` nunca cambió.
