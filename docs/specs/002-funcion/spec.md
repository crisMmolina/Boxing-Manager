# Spec 002 · Función: cartelera con configuración base
- Status: approved (zoe, 2026-10-26; contrato pendiente de cris) · Owner: zoe (BOXCENTRAL/), cris (app/) · PROJECT principles: 1, 2, 3, 4, 5

## Problem
Una función de box tiene varias peleas seguidas con los mismos jueces. Hoy cada pelea se crea desde cero: esquinas, rounds y número de jueces, y cada juez vuelve a escanear el QR, escribir su nombre y esperar la aprobación. Con 3 jueces son hasta 2 min por pelea (NFR Pairing), multiplicados por cada pelea de la noche, y con riesgo de equivocarse al capturar.

## Stories
- Como organizador, quiero capturar la cartelera completa de la función antes de empezar (esquinas de cada pelea, en orden), para no teclear entre pelea y pelea.
- Como organizador, quiero que la primera pelea sea la base (rounds, número de jueces y los jueces aprobados) y que las siguientes la hereden, para configurar una sola vez.
- Como organizador, quiero cambiar los rounds o el número de jueces de una pelea concreta (por ejemplo, el estelar a 12 rounds), para no forzar una sola configuración.
- Como juez, quiero que mi teléfono pase solo a la siguiente pelea sin volver a escanear ni esperar aprobación, para seguir calificando.

## Acceptance criteria (EARS)
- When el organizador crea una función, the system shall pedir la configuración base (rounds 1–12, jueces 1–5) y la lista ordenada de peleas (esquina roja y azul de cada una).
- When el organizador agrega, quita, salta o reordena peleas antes de que empiecen, the system shall guardar la cartelera en SQLite antes de mostrar el cambio.
- When el organizador salta una pelea (por ejemplo, se canceló), the system shall marcarla "Saltada" en la cartelera, registrarlo en la bitácora de la función y pasar a la siguiente sin borrarla.
- While una pelea de la cartelera no ha empezado, the system shall permitir cambiar sus rounds (1–12) o su número de jueces (1–5); si no se cambia, usa la base.
- When se cierra una pelea y hay otra en la cartelera, the system shall ofrecer "Siguiente pelea" con sus esquinas, rounds y jueces, sin volver a pedir datos.
- When el organizador inicia la siguiente pelea, the system shall llevar a sus lugares a los jueces aprobados de la pelea anterior (mismo lugar, nombre y llave del teléfono) y avisar a cada teléfono, que pasa directo a la pantalla de puntuación de la nueva pelea, sin escanear, sin aprobación y sin pantalla de confirmación.
- If la siguiente pelea tiene más jueces que la anterior, the system shall mostrar QR y código solo para los lugares nuevos.
- If la siguiente pelea tiene menos jueces, the system shall llevar solo los lugares 1..N y avisar a los demás teléfonos que no participan en esta pelea; esos teléfonos esperan y vuelven a entrar en la primera pelea siguiente que tenga su lugar.
- If un teléfono está desconectado al cambiar de pelea, the system shall llevarlo a la nueva pelea en cuanto reconecte (`HELLO` con su llave), sin perder nada de la anterior.
- While hay una pelea en curso, the system shall mantener una sola pelea activa (NFR Capacity); las demás de la cartelera quedan programadas.
- The system shall guardar cada pelea con sus propias tarjetas, resultado y bitácora, igual que hoy; el cambio de pelea queda en la bitácora de ambas.
- While la pelea está en curso, the system shall seguir sin mostrar totales ni ganador (principio 3) y sin enviar a un juez tarjetas ajenas.

## Non-functional
- Cambio de pelea con 5 jueces conectados: todos los teléfonos en la nueva pelea en ≤ 5 s, sin tocar el teléfono.
- Preparar una función de 8 peleas: ≤ 3 min de captura, una sola vez.
- Durabilidad: cerrar a la fuerza la central a mitad de la cartelera no pierde la cartelera, la pelea en curso ni las tarjetas (principio 1).
- Sin internet ni datos fuera de la red local (principio 4).

## Out of scope
- Más de una pelea activa a la vez.
- Cambiar a los jueces de lugar entre peleas (el juez 2 sigue siendo el juez 2).
- Importar la cartelera de un archivo o de otro sistema.
- Reportes de toda la función (cada pelea exporta su PDF/CSV como hoy).

## Open questions
- [x] Al cambiar de pelea, ¿el teléfono pide confirmar? · zoe: no, pasa directo a calificar
- [x] ¿Se pueden saltar peleas? · zoe: sí; quedan "Saltada" en la cartelera
- [ ] El cambio de pelea es un mensaje nuevo del protocolo (`NEXT_FIGHT` en `core`, compartido con `app/`): ADR 0002 propuesto; falta el visto bueno de cris · cris
