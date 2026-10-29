# Spec 003 · Uso real: función primero, resultado automático y apps sin capacitación
- Status: approved (cris, 2026-10-29) · Owner: cris (`android/`, `central/`) · PROJECT principles: 1, 2, 3, 4, 5

## Problem
En un evento real el organizador y los jueces tienen muy poco tiempo para aprender las apps. Hoy la central abre en "En vivo", se arma pelea por pelea con nombres obligatorios, al final hay que pulsar "Terminar pelea" y luego "Cerrar pelea y ver resultado" sin ver qué jueces ya enviaron su tarjeta final, la bitácora muestra códigos en inglés y mayúsculas (`SUBMIT_ROUND`, `TARDIA`…), y en la barra de arriba no se distingue bien en qué pestaña está uno. En el teléfono, los errores muestran códigos (`ROUND_NOT_OPEN`…) y algunas pantallas no dicen qué hacer.

## Stories
- Como organizador, quiero crear la función completa (rounds, jueces y cuántas peleas) en una sola pantalla al abrir la central, sin escribir nombres si no los tengo, para empezar en segundos.
- Como organizador, quiero ver en Resultado qué jueces ya mandaron su tarjeta final y que el ganador aparezca solo cuando estén todas, para cerrar la pelea con un clic.
- Como organizador, quiero leer la bitácora y los estados en español, para entender qué pasó sin conocer el protocolo.
- Como organizador, quiero saber siempre en qué pestaña estoy y qué sigue, para no perderme durante el evento.
- Como juez sin capacitación, quiero que cada pantalla me diga qué hacer y que los errores estén en español, para no atorarme.

## Acceptance criteria (EARS)
### Función primero (item 1)
- When la central abre sin una función activa, the system shall mostrar primero "Nueva función" con rounds por pelea (1–12), jueces (1–5) y número de peleas (1–20), y un botón principal "Crear función".
- Where el organizador no escribe los nombres de las esquinas, the system shall usar "Peleador esquina roja N" y "Peleador esquina azul N", con N = el número de la pelea al crear la función. Los nombres no se renumeran al reordenar.
- While una pelea no ha empezado, the system shall permitir cambiar sus nombres de esquina (además de rounds y jueces, como en spec 002).
- The system shall dejar "Pelea suelta" (crear una sola pelea, flujo actual) como opción secundaria, no como primer paso.
- Los pasos de la barra pasan a ser **Función · Jueces · En vivo · Resultado** (antes Pelea · Jueces · En vivo · Resultado).

### Resultado automático (item 3)
- When se cierra el último round programado, the system shall pasar la pelea a terminada y pedir la tarjeta final a cada juez sin que el organizador pulse "Terminar pelea". La terminación anticipada (KO, descalificación…) sigue igual y no pide tarjetas finales.
- While faltan tarjetas finales, the system shall mostrar en Resultado la lista de jueces con "Tarjeta final enviada" o "Pendiente", y actualizarla sola al llegar cada una (sin recargar).
- When llegan todas las tarjetas finales (o hay terminación), the system shall mostrar el resultado (ganador, tipo de decisión y tarjetas por juez) calculado por `core` y un botón "Cerrar pelea".
- When el organizador cierra la pelea, the system shall guardar exactamente el resultado que mostró (mismo cálculo de `core`, principio 2). Si hubo una corrección entre que se mostró y el cierre, el resultado en pantalla se recalcula antes de cerrar.
- Mostrar el ganador con la pelea terminada y todas las tarjetas finales recibidas **no** es "durante la pelea" para el principio 3: ya no hay rounds por calificar. Antes de eso sigue sin mostrarse ningún total ni ganador.
- La central no "acepta" tarjetas finales a mano: se aceptan solas si la firma es válida (como hoy).

### Español en las interfaces (item 2)
- The system shall mostrar en español todo tipo de evento de la bitácora, el filtro de la bitácora y todo estado (pelea, round, tarjeta, conexión, terminación) que aparezca en la central, en el PDF y en el CSV. En la base de datos siguen los códigos actuales (solo se traduce al mostrar).
- The judge app shall mostrar cada código de error del protocolo como una frase en español que diga qué hacer, nunca el código.
- La huella del teléfono (hexadecimal) no se traduce.

### Pestaña activa (item 4)
- The system shall marcar el paso actual y el enlace actual (Función, Terminación, Corrección, Bitácora) con fondo de color, además del texto, en claro y en oscuro, con contraste AA.

### Sin capacitación (item 5)
- Central: cada pantalla muestra una línea "Qué sigue" con la siguiente acción; todo botón deshabilitado dice por qué; sin función activa, la acción principal es "Crear función".
- Juez: Esperando aprobación, No participas, Round tardío y Esperando round dicen qué hacer en una frase; la tarjeta final pide confirmación ("Ya no podrás corregir") como los rounds; los puntos se siguen capturando con + y − sin valor por defecto (principio 2).

## Non-functional
- Crear una función de 8 peleas sin nombres: ≤ 30 s.
- La lista de tarjetas finales en Resultado se actualiza ≤ 1 s después del `ACK`.
- Sin internet ni datos fuera de la red local (principio 4).

## Out of scope
- Botones de puntuación rápida (10 · 9 · 8 · 7): cris decidió seguir con + y −.
- Importar la cartelera de un archivo.
- Cambiar el protocolo: nada de esta spec agrega mensajes a `core`.

## Open questions
- [x] Carpetas: `android/`, `central/`, `core/` como hermanas · cris (ADR 0003)
- [x] Puntuación: se queda + y − · cris
- [x] "Mejorar el MD" = `README.md` · cris
