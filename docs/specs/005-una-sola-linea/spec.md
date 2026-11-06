# Spec 005 · Una sola línea: Inicio, evento y en vivo sin pasos que se contradicen
- Status: approved (cris, 2026-11-06, a partir de sus observaciones de la central y del teléfono) · Owner: cris (`central/` con visto bueno de zoe) · PROJECT principles: 1, 2, 3

## Problem
Probando la central de spec 004, las secciones no se sienten de la misma app:
- Inicio muestra la barra de pasos de un evento aunque todavía no se ha entrado a ninguno.
- En Evento, el botón "Agregar pelea" está a la derecha de los nombres y no se entiende que sirve para agregar peleas.
- Jueces deja crear una pelea suelta (eso le toca a Evento) y muestra un "Código de la pelea" con letras, aunque los códigos que usan los jueces son de 6 números.
- En vivo deja abrir un round sin que estén conectados los jueces, y una vez creado el evento ya no se puede bajar el número de jueces ni de rounds.
- La terminación extraordinaria vive en otra sección, pide el round a mano, no actualiza En vivo y no se refleja en Resultado hasta pulsar "Cerrar pelea".
- Los títulos, verbos y ayudas cambian de una pantalla a otra ("Jueces" / "Emparejar jueces", "Nuevo evento" / "Crear evento", "Terminación" / "Terminación extraordinaria", preguntas sin "¿").

En el teléfono, lo que se observó ya está en `main` (logo de guantes en la carga y en el ícono, unirse solo con nombre, código e IP, sesión por evento y paso a la siguiente pelea). El teléfono de prueba tenía una APK anterior; se valida con la APK de `main` (ver §Teléfono).

## Stories
- Como organizador, quiero que Inicio sea solo la lista de eventos, y que al entrar a un evento tenga todas sus secciones juntas, para saber siempre dónde estoy.
- Como organizador, quiero ver el botón "Agregar pelea" antes de los nombres, para entender que ahí se agregan peleas.
- Como organizador, quiero que Jueces solo empareje a los jueces de la pelea que sigue del evento, con códigos de números.
- Como organizador, quiero que no se pueda abrir un round si falta un juez, y poder bajar los jueces o los rounds si alguien no llega.
- Como organizador, quiero terminar una pelea antes de tiempo desde En vivo, sin escribir el round, y ver el resultado en cuanto la termino.
- Como cualquier usuario, quiero que cada pantalla use las mismas palabras y el mismo orden, para no aprender cada una por separado.

## Acceptance criteria (EARS)

### Inicio y navegación (DSK-035)
- When la central muestra **Inicio**, the system shall mostrar solo la lista de eventos, "Nuevo evento", el "?" y el cambio de tema claro/oscuro. Inicio no muestra la barra de pasos ni los enlaces del evento.
- When el organizador abre o crea un evento, the system shall mostrar la barra del evento: **Evento · Jueces · En vivo · Resultado · Bitácora**, más "Inicio" para volver, el "?" y el cambio de tema. "Terminación" deja de ser una sección cuando pasa a En vivo (DSK-036); hasta entonces sigue en la barra para no perderla.
- When el organizador vuelve a Inicio, the system shall quitar la barra del evento; al entrar a otro evento, todas sus secciones muestran ese evento.

### Evento (DSK-037)
- The system shall mostrar "Agregar pelea" a la izquierda de los campos de las esquinas, en la misma fila.
- When el organizador baja los jueces (1–5) o los rounds (1–12) del evento, the system shall aplicarlo a las peleas que no han empezado (también a la que espera en Jueces, quitando los lugares que sobran y sus códigos), y dejar igual las peleas que ya empezaron o terminaron. Cada cambio se anota en la Bitácora.
- If una pelea ya abrió su primer round, then the system shall no cambiar sus jueces ni sus rounds y decir por qué.

### Jueces (DSK-038)
- The system shall emparejar solo la pelea que sigue del evento; Jueces no tiene formulario para crear peleas. Sin pelea pendiente, dice "Agrega una pelea en Evento" con un botón que lleva a Evento.
- The system shall mostrar a los jueces solo códigos de 6 números (uno por lugar). El identificador interno de la pelea no se muestra como "código".

### En vivo y terminación (DSK-036)
- While falte conectar algún juez de la pelea, the system shall dejar "Abrir round" desactivado y decir qué juez falta y cómo seguir ("…o baja los jueces en Evento si no va a llegar"). Cerrar un round abierto sigue igual (si un juez se cae a mitad del round, el round se puede cerrar).
- When el organizador elige "Terminar pelea" en En vivo, the system shall usar como round el abierto o, si no hay, el último cerrado (no se escribe), pedir solo el motivo y confirmar.
- When se confirma la terminación, the system shall cerrar la pelea y guardar su resultado sin más pasos, avisar a los teléfonos como en un cierre normal (`FIGHT_CLOSED`, y `NEXT_FIGHT` o `EVENT_ENDED`), actualizar En vivo en ≤ 1 s y llevar al organizador a Resultado con la terminación ya reflejada.

### Una sola línea (DSK-039)
- The system shall titular cada pantalla con el nombre de su sección en la barra, usar los mismos verbos para la misma acción (p. ej. "Nuevo evento" en Inicio y en el botón que lo crea), escribir las preguntas con "¿…?" y los botones con mayúscula solo al inicio, y poner la ayuda "Qué sigue" con el mismo patrón en todas las pantallas.
- A test recorre todas las pantallas y falla si un título no coincide con su sección, si una pregunta no abre con "¿" o si un botón está en mayúsculas.

### Teléfono
- Antes de reportar algo del teléfono, se instala la APK de `main`: `android/gradlew -p android :app:assembleDebug` y `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
- When se agregan peleas a un evento que ya mandó `EVENT_ENDED`, the system shall pedir a los jueces que se unan de nuevo (con su código nuevo). Así ya funciona; no hay tarea.

## Out of scope
- Cambiar jueces o rounds de una pelea que ya empezó (cambia el cálculo de `core`; sería otra spec).
- Cambiar el formato del identificador de la pelea (`fightId`) en el protocolo.

## Decisions to record
- `NavTest.secondaryLinksAreInicioTerminacionAndBitacora` queda obsoleta con DSK-036 (Terminación sale de la barra al pasar a En vivo): se reemplaza por una prueba de los enlaces Inicio · Bitácora (`uak decision` en DSK-036).
- `RoundsScreenTest.thereIsOneActionCardWithTheRoundButtonOnly` queda obsoleta con DSK-036 (En vivo suma "Terminar pelea"): se reemplaza por una que pide "Cerrar round N" y "Terminar pelea" (`uak decision` en DSK-036).
