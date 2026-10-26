# Demo · SIGEBOX en 6 pasos (≤ 3 min)

Pelea de referencia: Roja vs Azul, 4 rounds, 3 jueces (`demo_data/pelea_referencia.json`). Resultado esperado: **Gana Roja por decisión dividida (2–1)**, con 39–37, 37–39 y 39–37.

| Juez | R1 | R2 | R3 | R4 |
|---|---|---|---|---|
| Juez 1 | 10–9 | 9–10 | 10–9 | 10–9 |
| Juez 2 | 9–10 | 9–10 | 10–9 (tardío) | 9–10 |
| Juez 3 | 10–9 | 10–9 | 9–10 | 10–9 |

## Antes de empezar

- Red e instalación listas según `docs/runbook.md` §1–§4: hotspot prendido, laptop solo en el hotspot, regla de firewall creada, app del juez en 3 teléfonos.
- Central abierta, con los términos ya aceptados.
- Reduce el riesgo: haz un ensayo completo el mismo día con los mismos teléfonos.

## Los 6 pasos

1. **Crear la pelea.** En **Pelea**: Roja, Azul, 4 rounds, 3 jueces. Elige la IP del hotspot. *Se ve:* un QR y un código de 6 dígitos por lugar.
2. **Unir a los jueces.** Cada juez escanea su QR y escribe su nombre; en **Jueces** compara la huella corta y pulsa **Aprobar**. *Se ve:* el teléfono pasa de "Esperando aprobación" a puntuar; la central marca al juez conectado.
3. **El wow: calificar un round.** En **En vivo**, `Abrir round 1` (o `Ctrl+Enter`). Cada juez pone su calificación con − / + y envía. *Se ve:* sin internet, la tarjeta de cada juez aparece en el tablero en ≤ 1 s. Los teléfonos nunca muestran totales.
4. **Cierre forzado y tardío.** Rounds 2 y 3. En el round 3 el Juez 2 **no** envía: `Cerrar round 3`, confirma y escribe **AVANZAR**. *Se ve:* el teléfono del Juez 2 vibra y le exige calificar; su tarjeta llega como **Tardío**.
5. **Nada se pierde.** Apaga el Wi-Fi de un juez, que califique el round 4 sin red y vuelve a prender el Wi-Fi (pulsa **Continuar** en el aviso de Android). Para el golpe fuerte, cierra la central a la fuerza y ábrela otra vez. *Se ve:* el juez reconecta solo, la central vuelve en el mismo round y no falta ninguna tarjeta.
6. **Resultado.** Cierra el round 4 y pulsa **Cerrar pelea**; cada juez envía su tarjeta final. *Se ve:* pantalla completa "Gana Roja por decisión dividida 2–1" con las tarjetas y nombres; **Exportar PDF/CSV**.

Medido para respaldar lo que se dice en la demo: con el hotspot de un Honor 200 y un Oppo Reno12 F (Android 16) la tarjeta llega por Wi-Fi aunque el teléfono tenga datos móviles, y la app reconecta en ≤ 6 s tras un corte (`docs/spikes/AND-002.md`). En la prueba de integración, el p95 envío → confirmación fue 13 ms en la misma máquina y no se perdió ninguna de las 60 tarjetas con un corte de 60 s y un reinicio del servidor (QA-001).

## Plan B

| Si falla… | Haz esto |
|---|---|
| Un teléfono de juez | Ese lugar lo cubre `judge-sim` desde la laptop (abajo), o captúralo en **Corrección** desde papel |
| Todos los teléfonos o la red | `judge-sim` con los 3 jueces contra la central en la misma laptop (`--host localhost`) |
| No da tiempo de capturar | Corre `judge-sim` con el guion de la pelea de referencia: califica los 4 rounds solo |
| La laptop o la demo en vivo | El video grabado de los 6 pasos |

`judge-sim` (ver `README.md` para compilarlo):

```bash
BOXCENTRAL/judge-sim/build/install/judge-sim/bin/judge-sim --judges 3 --script demo_data/pelea_referencia.json --tokens CODIGO1,CODIGO2,CODIGO3 --fight ID_PELEA --host localhost --drop 60s
```

- Los códigos son los de 6 dígitos de cada lugar en la central, en orden.
- `--fight` es el **Código de la pelea** de la tarjeta Pelea en la central (botón **Copiar**).
- Los jueces simulados esperan la aprobación: apruébalos en **Jueces** como a un teléfono.
- Con `--drop 60s` el Juez 1 se cae 60 s tras su primer envío y vuelve solo: cubre el paso 5.
- Al terminar imprime la latencia envío → ACK (p50/p95/máx).

**Pendiente de grabar:** el video del plan B no existe todavía. Grábalo en el ensayo general.
