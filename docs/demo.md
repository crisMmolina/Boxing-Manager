# Demo · SIGEBOX en 6 pasos (≤ 3 min)

Pelea de referencia: Roja vs Azul, 4 rounds, 3 jueces (`demo_data/pelea_referencia.json`). Resultado esperado: **Gana Roja por decisión dividida (2–1)**, con 39–37, 37–39 y 39–37.

| Juez | R1 | R2 | R3 | R4 |
|---|---|---|---|---|
| Juez 1 | 10–9 | 9–10 | 10–9 | 10–9 |
| Juez 2 | 9–10 | 9–10 | 10–9 (tardío) | 9–10 |
| Juez 3 | 10–9 | 10–9 | 9–10 | 10–9 |

## Antes de empezar

- Red e instalación listas según `docs/runbook.md` §1–§3: hotspot prendido, laptop solo en el hotspot, regla de firewall creada, app del juez en 3 teléfonos.
- Central abierta, con los términos ya aceptados.
- Reduce el riesgo: haz el ensayo (abajo) el mismo día con los mismos teléfonos.

## Los 6 pasos

1. **Crear el evento.** **Inicio** → **Nuevo evento:** 4 rounds, 3 jueces, 2 peleas. En **Evento** escribe Roja y Azul en la cartelera y pulsa **Iniciar pelea**. En **Jueces** elige la IP del hotspot. *Se ve:* un QR y un código de 6 dígitos por lugar.
2. **Unir a los jueces.** Cada juez escanea su QR o escribe su código, pone su nombre y pulsa **Unirme**; en **Jueces** compara la huella y pulsa **Aprobar**. Con los 3 aprobados, **Iniciar pelea**. *Se ve:* el teléfono pasa de **Esperando aprobación** a puntuar; la central marca al juez conectado.
3. **El wow: calificar un round.** En **En vivo**, `Abrir round 1` (o `Ctrl+Enter`). Cada juez pone su calificación con − / + y envía. *Se ve:* sin internet, la casilla de cada juez pasa a **Enviado** en el tablero en ≤ 1 s. Ni la central ni los teléfonos muestran puntos, totales ni ganador durante la pelea.
4. **Cierre forzado y tardío.** Rounds 2 y 3. En el round 3 el Juez 2 **no** envía: `Cerrar round 3`, confirma y escribe **AVANZAR**. *Se ve:* el teléfono del Juez 2 vibra y le exige calificar; su tarjeta llega como **Tardío**.
5. **Nada se pierde.** Apaga el Wi-Fi de un juez, que califique el round 4 sin red y vuelve a prender el Wi-Fi (pulsa **Continuar** en el aviso de Android). Para el golpe fuerte, cierra la central a la fuerza y ábrela otra vez. *Se ve:* el juez reconecta solo, la central vuelve en el mismo round y no falta ninguna tarjeta.
6. **Resultado.** Cierra el round 4: la pelea termina sola y cada teléfono envía su tarjeta final. En **En vivo** pulsa **Cerrar pelea** para ir a **Resultado** y ahí **Cerrar pelea** otra vez. *Se ve:* los puntos de cada juez round por round y, en la ventana **Resultado final**, **Exportar PDF** y **Exportar CSV**, sin ganador en pantalla. El ganador y el tipo de decisión van en el PDF y el CSV.

Medido para respaldar lo que se dice en la demo: con el hotspot de un Honor 200 y un Oppo Reno12 F (Android 16) la tarjeta llega por Wi-Fi aunque el teléfono tenga datos móviles, y la app reconecta en ≤ 6 s tras un corte (`docs/spikes/AND-002.md`). En la prueba de integración, el p95 envío → confirmación fue 13 ms en la misma máquina y no se perdió ninguna de las 60 tarjetas con un corte de 60 s y un reinicio del servidor (QA-001).

## Ensayo antes del evento real

Haz un ensayo con **3 teléfonos reales y un evento de 2 peleas**: los 6 pasos en la primera pelea y, al cerrarla, la segunda completa. Comprueba que los teléfonos pasan a **Espera la siguiente pelea** entre peleas y a **El evento terminó** tras la última.

## Plan B

| Si falla… | Haz esto |
|---|---|
| Un teléfono de juez, antes de iniciar la pelea | Ese lugar lo cubre `judge-sim` desde la laptop, solo con su código (abajo). Califica con el guion, no como el juez |
| Un teléfono de juez, con la pelea iniciada | **Volver a papel** (`docs/runbook.md` §6) |
| Todos los teléfonos o la red | `judge-sim` con los 3 jueces contra la central en la misma laptop |
| No da tiempo de calificar | Corre `judge-sim` con el guion de la pelea de referencia: califica los 4 rounds solo |
| La laptop o la demo en vivo | El video grabado de los 6 pasos |

`judge-sim` (ver `README.md` para compilarlo):

```bash
central/judge-sim/build/install/judge-sim/bin/judge-sim --script demo_data/pelea_referencia.json --codes CODIGO1,CODIGO2,CODIGO3 --host localhost --port 8765 --drop 60s
```

- Los códigos son los de 6 dígitos de cada lugar en la pantalla **Jueces**.
- Los jueces simulados esperan la aprobación: apruébalos en **Jueces** como a un teléfono.
- Con `--drop 60s` el Juez 1 se cae 60 s tras su primer envío y vuelve solo: cubre el paso 5. Debe volver antes de que termine la pelea, o no manda su tarjeta final.
- Al terminar imprime la latencia envío → ACK (p50/p95/máx).

**Pendiente de grabar:** el video del plan B no existe todavía. Grábalo en el ensayo general.
