# PROJECT · SIGEBOX (marathon mode)

> The project constitution. Humans change it by reviewed PR. uak reads the last block from the remote `main`.
> Fuente de producto: `IDEA.md` (y el documento "Requerimientos v1.1", que gana si chocan). Spec: `docs/specs/001-sigebox/`.

## Purpose
Para el organizador de una función de box profesional: recibir la calificación de cada juez (teléfono Android) en la laptop central (Windows, JavaFX) en 1 s o menos y obtener el resultado calculado al cerrar la pelea, todo en red local sin internet ni router, sin perder ninguna calificación. Resultado medible: 100 % de coincidencia con el cálculo a mano, 0 calificaciones perdidas.

Dos apps + un núcleo compartido (ADR 0001):
- `app/` → app del juez (Android, Java, minSdk 26, paquete `com.sigebox.juez`) · dueño humano: **cris** (crisMmolina).
- `BOXCENTRAL/` → app central de escritorio (JDK 25, JavaFX 25, Javalin 7, SQLite) · dueño humano: **zoe** (zoe718).
- `core/` → Java 17 puro: modelo, protocolo, validación y cálculo del resultado. Lo usan las dos apps.

## Non-negotiable principles
1. Ninguna calificación enviada se pierde: se persiste en SQLite (WAL) antes del `ACK`; el teléfono reenvía hasta recibir `ACK`; el servidor es idempotente por id de mensaje.
2. Ningún número del resultado sale de otro lugar que `core` (determinista, sin IA). Se valida en el teléfono y otra vez en el servidor: enteros 0–10, sin valor por defecto.
3. Un juez nunca ve tarjetas ajenas; nadie ve totales ni ganador durante la pelea (salvo "Ver resumen parcial", registrado en bitácora).
4. Los datos nunca salen de la red local del evento. Sin internet, sin nube, sin telemetría.
5. Nothing reaches `main` without green CI and approval of the other human (CODEOWNERS). Every decision costly to reverse has an ADR.
6. Autoría: los commits van solo a nombre del humano; nunca se agregan agentes ni IA como autores o co-autores.

## Non-functional requirements (measurable)
| Attribute | Target | Measured by |
|---|---|---|
| Latency | envío/corrección visible en la central en ≤ 1 s (p95, red local) | `judge-sim` midiendo `SUBMIT_ROUND` → `ACK` |
| Durability | 0 calificaciones perdidas con corte de Wi‑Fi de 60 s y cierre forzado de cualquiera de las apps | prueba de integración `QA-001` |
| Correctness | resultado = cálculo a mano (12 rounds × 5 jueces, pelea de referencia) | JUnit 5 en `core` · función con papel en paralelo |
| Capacity | 1–5 jueces, 1–12 rounds, una pelea activa | pruebas de `core` |
| Pairing | 3 jueces emparejados (QR + nombre + aprobación) en < 2 min | prueba manual paso 2 |
| Coverage | 80 % de líneas en `core` | JaCoCo en CI |
| Platforms | central Windows 10/11 sin Java preinstalado · juez Android 8+ | instalador jpackage + APK firmado |

## Environments and ownership
| Env | URL | Deploys | Secrets in |
|---|---|---|---|
| dev | local (laptop + teléfonos, hotspot) | cada humano en su máquina | `.env` local (keystore de firma) |
| prod | sin web: instalador .msi/.exe + APK firmado | un humano, por release semver | keystore fuera del repo |

CODEOWNERS: `app/` → @crisMmolina · `BOXCENTRAL/` → @zoe718 · `core/` → ambos · On-call: el organizador del evento · Runbook: `docs/runbook.md`.

## Executable config
```text
Mode: marathon
Backlog-Proposed-Epoch: 1792906554
Backlog-Approved: yes
Autonomy: yes
Auto-Merge: no
Review-Mode: claims
Plan-Channel: main
Freeze-Epoch: 0
Lease-Seconds: 14400
Review-Lease-Seconds: 86400
Events-Per-Session: 150
Checkpoint-Percent: 60
Merge-Lease-Seconds: 1800
Merge-Wait-Seconds: 180
Max-Parallel-Subagents: 3
Opus-Subagents-Per-Session: 4
Pace: auto
Subagent-Hard-Cap: 4
Strong-Hard-Cap: 4
Subagents-Per-Session: 30
Subagents-Per-Machine: 6
Subagent-Burst-Per-Minute: 6
Max-Worktree-Agents: 4
Blackbox: off
Additive-Paths: CHANGELOG.md
```
