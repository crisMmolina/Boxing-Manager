# Spec 004 · Eventos: inicio con historial, cierre sin pasos extra y rediseño negro, blanco y dorado
- Status: approved (cris, 2026-11-03) · Owner: cris (`android/`, `central/`; `central/` con visto bueno de zoe) · PROJECT principles: 1, 2, 3, 4, 5

## Problem
Una noche de box es un **evento** de varias peleas, pero la central abre en "En vivo" de una pelea suelta, llama "función" a lo que el organizador llama evento, y no guarda un historial que se pueda abrir. Al terminar cada pelea el organizador pulsa "Terminar pelea", espera a que cada juez confirme su tarjeta final a mano y luego "Cerrar pelea"; en Resultado hay que pulsar "Actualizar". Varias secciones (Corrección, Terminación, Bitácora, Resultado) no se actualizan solas. No hay forma de registrar un punto menos que marque el réferi. En el teléfono, unirse pide código de pelea con letras, lugar y código de lugar, aunque el campo del código solo acepta números; al cerrarse una pelea el teléfono se queda en la última pantalla, y si se pierde la conexión no hay guía. Las dos apps se ven distintas entre sí y los formularios solo muestran el texto de ayuda dentro del campo, que desaparece al escribir.

## Stories
- Como organizador, quiero abrir la central y ver mis eventos anteriores con su fecha y un botón "Nuevo evento", para empezar o retomar en segundos.
- Como organizador, quiero agregar peleas al evento sin escribir los nombres de los peleadores, para armar la cartelera aunque no la tenga completa.
- Como organizador, quiero marcar "−1 punto" a una esquina cada vez que el réferi lo indique, para que el resultado lo refleje sin papel.
- Como organizador, quiero que al cerrarse el último round la central ya tenga todo y me muestre cuánto sumó cada esquina con cada juez, sin pedir nada a los jueces.
- Como organizador, quiero que todas las secciones se actualicen solas, para no dudar si lo que veo es lo último.
- Como juez, quiero escribir solo mi nombre, un código de números y la IP (o escanear el QR), para unirme sin ayuda.
- Como juez, quiero que al terminar una pelea el teléfono me diga que espere la siguiente, o me regrese al inicio si el evento terminó.
- Como juez u organizador, quiero un botón "?" con los pasos para recuperar la conexión, para resolverlo sin llamar a nadie.
- Como cualquier usuario, quiero que las dos apps se vean igual de cuidadas, en negro, blanco y dorado, con el nombre de cada campo siempre visible.

## Acceptance criteria (EARS)

### Arranque e inicio (items 1, 2, 17)
- When se abre cualquiera de las dos apps, the system shall mostrar una pantalla de carga con el logo hasta que la app esté lista (base abierta en la central; estado restaurado en el teléfono), al menos 600 ms y como máximo 3 s si no hay error.
- When la central termina de cargar (y los términos ya se aceptaron), the system shall mostrar **Inicio**: la lista de eventos del historial (nombre, fecha, número de peleas, cuántas cerradas), del más reciente al más antiguo, y un botón principal "Nuevo evento".
- When el organizador abre un evento del historial, the system shall llevarlo a ese evento: a su pelea activa si hay una, o a su cartelera si no.
- When el organizador crea un evento, the system shall pedir nombre (opcional; si se deja vacío: "Evento <fecha>"), fecha (por defecto hoy, se puede cambiar), rounds por pelea (1–12), jueces (1–5) y número de peleas (1–20), y guardar la fecha elegida.
- The system shall decir "evento" en toda la interfaz, el PDF y el CSV donde hoy dice "función". La base y el código pueden seguir usando `funcion` (solo cambia lo que se muestra).
- Mientras haya un evento abierto, la barra de pasos es **Evento · Jueces · En vivo · Resultado**; los enlaces secundarios son **Terminación · Bitácora** (sin Corrección). Desde cualquier pantalla se puede volver a Inicio.

### Peleas sin nombres (item 4)
- Where el organizador no escribe los nombres de las esquinas, the system shall usar "Esquina roja N" y "Esquina azul N" (N = número de la pelea) y aceptar la pelea. Los nombres se pueden cambiar mientras la pelea no haya empezado.

### En vivo (items 9, 14)
- The system shall quitar "Ver resumen parcial" de En vivo. Ningún total ni ganador se muestra durante la pelea, sin excepción (principio 3 se vuelve más estricto).
- While un round está abierto o cerrado y la pelea no ha terminado, the system shall ofrecer "−1 roja" y "−1 azul" con el round al que aplica (por defecto el round actual) y pedir confirmación.
- When el organizador confirma un descuento, the system shall guardarlo (pelea, round, esquina, 1 punto, hora), anotarlo en bitácora y mostrarlo en la lista de descuentos de la pelea. No hay límite de descuentos por esquina ni por round.
- When el organizador quita un descuento puesto por error, the system shall pedir un motivo, marcarlo como anulado (no se borra) y anotarlo en bitácora.
- The system shall restar cada descuento vigente del puntaje de esa esquina en ese round **en la tarjeta de cada juez**, solo en `core` (principio 2). Los jueces no ven los descuentos.

### Quitar Corrección (item 10)
- The system shall quitar la sección Corrección completa ("Corregir tarjeta" y "Capturar desde papel") de la central. Las tarjetas guardadas antes siguen contando y su historial sigue en bitácora. El juez sigue pudiendo corregir su round desde el teléfono mientras el round esté abierto (sin cambio).

### Cierre sin pedir tarjetas (item 12)
- When se cierra el último round programado, the system shall terminar la pelea sola (sin "Terminar pelea"), y cada teléfono shall enviar su tarjeta final firmada **sin que el juez pulse nada** (sin pantalla de confirmación).
- If un teléfono está desconectado al terminar la pelea, the system shall enviar su tarjeta final en cuanto se reconecte (mismo reenvío que los rounds, principio 1).
- When llegan todas las tarjetas finales (≤ 1 s después del último `ACK` en red local), the system shall mostrar el resultado y habilitar "Cerrar pelea". La terminación anticipada (KO, descalificación…) sigue sin pedir tarjetas finales.

### Resultado (item 13)
- The system shall mostrar en Resultado, por juez, la puntuación de cada esquina round por round, los descuentos y la suma de cada esquina; y una fila con la suma de cada esquina por juez. Todos los números salen de `core`.
- The system shall **no** mostrar ganador ni tipo de decisión en pantalla, ni botón "Actualizar": la pantalla se actualiza sola.

### Datos siempre al día (item 6)
- While una pantalla de la central está visible, the system shall mostrar cualquier cambio que la afecte (envío, corrección del juez, conexión, descuento, tarjeta final, bitácora) en ≤ 1 s, sin botón "Actualizar".
- While una sección carga sus datos (o el teléfono espera el primer `STATE`), the system shall mostrar *skeletons* (bloques grises con la forma del contenido) en lugar de pantallas vacías o "Cargando…".

### Unirse con código y QR (items 7, 8)
- The judge app shall pedir solo **Nombre**, **Código** (exactamente 6 dígitos, teclado numérico, no acepta otra cosa) e **IP de la central**, además de "Escanear QR" como opción principal.
- The central shall mostrar por cada lugar de juez un código de **6 dígitos solo números**, único entre los lugares de las peleas abiertas del evento, y un QR con el mismo dato.
- When un teléfono escanea el QR que muestra la central, the system shall llenar los tres campos y unirse con el mismo resultado que tecleando el código (prueba: el QR generado se decodifica y se une con `judge-sim`).
- If el código no existe o ya se usó, the system shall decirlo en español ("Ese código no es válido. Pídele a la mesa el código de tu lugar.").

### Al terminar una pelea, en el teléfono (item 18)
- When la pelea en la que está el juez se cierra y hay una siguiente pelea en el evento, the judge app shall mostrar "Espera la siguiente pelea" con las esquinas de la siguiente si ya se conocen, hasta que la central la abra.
- When se cierra la última pelea del evento, the judge app shall mostrar "El evento terminó" y regresar al inicio (pantalla para unirse) con el nombre del juez ya escrito.

### Ayuda de conexión (item 11)
- While el teléfono o la central muestran que un juez está desconectado, the system shall mostrar un botón "?" visible.
- When se pulsa "?", the system shall mostrar una guía de pasos cortos en español (hotspot encendido, misma red, IP correcta, cortafuegos de Windows, volver a abrir la app) que no requiere internet. La misma guía se puede abrir siempre desde un "?" fijo en la barra superior.

### Formularios (item 15)
- The system shall mostrar en todo campo de formulario, en las dos apps, el nombre del campo **arriba** del campo, siempre visible, y un ejemplo dentro del campo (placeholder) que desaparece al escribir.

### Imagen y diseño (items 3, 16)
- The system shall usar un logo de guantes de box rojos, estilo ilustración/caricatura, con licencia libre verificable (CC0, dominio público o equivalente que permita uso comercial sin atribución obligatoria), en la pantalla de carga, el encabezado de la central y el ícono de las dos apps. La fuente y la licencia quedan en `docs/design/CREDITOS.md`.
- The system shall aplicar en las dos apps una guía visual v4 en negro, blanco y dorado, en claro y oscuro, con contraste AA en todo texto, la misma tipografía, el mismo logo y los mismos componentes (botón principal, campo con etiqueta arriba, tarjeta, skeleton, barra de pasos con paso activo). Rojo y azul solo para las esquinas.

## Non-functional
- Cambios visibles en la central ≤ 1 s (p95) después del `ACK` o de la acción del organizador.
- Pantalla de carga ≤ 3 s en la laptop del evento y en un teléfono Android 8.
- Unirse tecleando el código: ≤ 30 s para un juez sin capacitación.
- Crear un evento de 8 peleas sin nombres: ≤ 30 s.
- Sin internet ni datos fuera de la red local (principio 4): el logo y la guía van dentro de las apps.

## Out of scope
- Importar la cartelera desde un archivo.
- Borrar eventos del historial.
- Mostrar descuentos a los jueces.
- Botones de puntuación rápida (se queda + y −, spec 003).

## Open questions
- [x] Tarjeta final: se envía sola, firmada, al terminar la pelea · cris
- [x] Descuentos: por round, a la tarjeta de todos los jueces, se pueden anular con motivo · cris
- [x] Corrección: se quita completa, incluida la captura desde papel · cris
- [x] Resultado en pantalla: solo sumas por juez y por esquina, sin ganador · cris
- [x] El PDF/CSV del acta siguen mostrando ganador y tipo de decisión (calculados por `core`) y agregan los descuentos · cris
- [x] Código de 6 dígitos por lugar en lugar de código de pelea + lugar + código: se busca con un mensaje nuevo y `JOIN` no cambia (ADR 0004) · cris
- [ ] Principio 3 de `.uak/PROJECT.md` aún menciona "Ver resumen parcial": quitar la excepción en un PR humano · cris
- [x] Logo: DSN-004 propone 2–3 opciones con su licencia verificada y cris elige antes de descargar nada · cris
