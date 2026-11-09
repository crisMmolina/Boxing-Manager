# Spec 006 · Rediseño v5 «Arena nocturna»
- Status: approved (cris, 2026-11-09) · Owner: cris · PROJECT principles: 3, 4
- Dirección visual aprobada: maqueta «SIGEBOX v5 · Dirección visual» (central En vivo y Evento; juez Puntuar y Enviada).

## Problem
Las dos apps funcionan, pero se sienten antiguas (cris, 2026-11-09). En la central:
- los controles son los de JavaFX por defecto (`Spinner` con flechas, botones grises con borde grueso);
- todo va centrado, con mucho vacío y sin jerarquía;
- no hay íconos, las pantallas cambian de golpe y nada reacciona al pasar el mouse;
- la cartelera es un formulario de captura y no una lista que se lee de un vistazo.

En el teléfono la base es Material 3 Expressive, pero falta movimiento con intención (cambio de número, envío, cambio de pantalla), iconografía y una pantalla de puntuar con más presencia.

## Stories
- Como organizador, quiero ver de un vistazo en qué parte del evento voy y qué sigue, para no buscar en la pantalla.
- Como organizador, quiero que la cartelera se lea como una lista y que las acciones de cada pelea estén a un clic (menú ⋯), para preparar el evento rápido.
- Como organizador en En vivo, quiero ver cuántas tarjetas faltan y qué juez está calificando, sin ver ningún punto.
- Como juez, quiero que el número reaccione al tocar − y +, y que enviar me confirme con claridad que la central lo recibió.

## Acceptance criteria (EARS)
### Central
- The system shall navegar con una barra lateral: logo, el evento en curso con su avance ("2 de 5"), los pasos Evento · Jueces · En vivo · Resultado con ícono (✓ en los terminados y un punto que late en En vivo cuando hay un round abierto), y abajo Bitácora, Inicio, ayuda y tema. Fuera de un evento solo se ve lo que hoy (spec 005: Inicio, ayuda y tema).
- The system shall encabezar cada pantalla a la izquierda: contexto (miga de pan o pelea con sus esquinas), título igual a su sección y la acción principal arriba a la derecha.
- The system shall mostrar "Qué sigue" como un aviso con ícono, con el mismo patrón en todas las pantallas (spec 005 sigue vigente).
- The system shall reemplazar todo `Spinner` por un control − valor + propio, con el mismo rango y accesible por teclado.
- The system shall mostrar cada fila de la cartelera como una línea (número, esquinas con su punto de color, rounds, jueces, estado) con un menú ⋯ que reúne Editar esquinas, Cambiar rounds y jueces, Subir/Bajar, Saltar y Quitar.
- The system shall mostrar en En vivo, sin puntos ni totales: un anillo con las tarjetas recibidas del round, el avance de los rounds en segmentos y una tarjeta por juez con sus iniciales, su conexión y su estado del round.
- When se cambia de pantalla, the system shall entrar con un fundido y un desplazamiento de 8 px en ≤ 220 ms; botones y filas reaccionan al pasar el mouse y al presionar.
- While "reducir movimiento" esté activo, the system shall usar solo fundidos de ≤ 150 ms y ningún latido ni brillo.

### Juez
- The system shall mostrar en la barra un chip de conexión (Conectado con punto que late; Sin conexión con anillo y texto).
- The system shall mostrar el avance de los rounds en segmentos en Puntuar, Enviada y Round tardío.
- When el juez toca − o +, the system shall rodar el número al nuevo valor (≤ 200 ms) y dar háptica corta.
- When la central confirma el envío (`ACK`), the system shall mostrar "Round N enviado" con una palomita que se dibuja, la tarjeta enviada y la opción de corregir.
- When se pasa de una pantalla a otra, the system shall usar una transición de eje compartido (≤ 300 ms).
- While la animación del sistema esté apagada (`ANIMATOR_DURATION_SCALE` = 0), the system shall mostrar los cambios sin movimiento.

### Las dos
- The system shall dibujar los íconos con trazos vectoriales empaquetados en la app (nada se descarga en ejecución, principio 4).
- The system shall conservar los principios de DESIGN v4 que siguen: negro, blanco y dorado; rojo y azul solo en esquinas; Barlow Condensed + Inter empaquetadas; ningún total durante la pelea (principio 3); contraste AA medido en claro y oscuro.

## Non-functional
- Animaciones de la central a 60 fps en la laptop del evento; ninguna transición > 300 ms.
- Contraste AA (4.5:1 texto, 3:1 componentes) medido en la tabla de DESIGN v5 para cada par nuevo.
- Sin dependencias nuevas de UI en ninguna app (íconos propios).

## Out of scope
- Cambiar flujos, textos de spec 005 o el protocolo.
- Compose en Android o FXML en la central.
- Sonidos.

## Open questions
- [x] ¿Dirección visual? · aprobada por cris (2026-11-09)
