# ADR 0002 · Mensaje `NEXT_FIGHT` para pasar a los jueces a la siguiente pelea
- Status: proposed · Date: 2026-10-26 · Decider: zoe y cris

## Context
Spec 002: en una función, los jueces aprobados de una pelea pasan solos a la siguiente, sin escanear ni aprobación. Hoy el teléfono está atado a una pelea: `JOIN` y `HELLO` firman el `fightId`, y el protocolo no tiene forma de decirle que cambió. El protocolo vive en `core` y lo comparten la central (zoe) y la app (cris), así que cambiarlo es caro de revertir: hay que publicar ambas apps juntas.

## Decision
Agregar a `core` el mensaje central → juez `NEXT_FIGHT {v, id, type, fightId, nextFightId, seat|null, redCorner, blueCorner, rounds}`, con su schema y fixture. El teléfono guarda la nueva pelea, reconecta y se presenta con `HELLO` firmado con su misma llave. La central copia antes los lugares aprobados (con su huella) a la nueva pelea y responde `NEXT_FIGHT` también a un `HELLO` que llega de la pelea ya cerrada. `seat: null` = no participa en esta pelea.

## Alternatives
- Reusar la misma pelea para toda la función: mezcla tarjetas, resultados y bitácoras.
- Nuevo QR por pelea: es el problema que se quiere quitar.
- Un `JOIN` automático con token nuevo enviado por el socket: más piezas (tokens, aprobación) para el mismo resultado; la llave del teléfono ya identifica al juez.

## Consequences
- Fácil: cambiar de pelea en segundos y sin tocar los teléfonos; un teléfono desconectado se pone al día al reconectar.
- Difícil: la central y la APK deben publicarse juntas; una APK vieja ignoraría `NEXT_FIGHT` y se quedaría en la pelea cerrada (el juez tendría que escanear el QR como hoy).
- Revertir: quitar el uso de `NEXT_FIGHT` en ambas apps; el mensaje puede quedarse en `core` sin efecto.
