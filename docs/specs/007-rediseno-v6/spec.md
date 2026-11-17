# Spec 007 · Rediseño v6 «Esquina» (app del juez)
- Status: approved (cris, 2026-11-17: «necesito un rediseño de la app, mira las referencias… crea las tareas y empieza a trabajar») · Owner: cris · PROJECT principles: 2, 3, 4
- Referencias: capturas de Heavy Bag Pro (Inicio, Round en curso con temporizador, Elegir entrenamiento, Calendario, Train Now, Entrenamiento personalizado), entregadas por cris el 2026-11-17.
- Alcance: **solo la app del juez** (`android/`). Las referencias son todas de teléfono; la central queda fuera (seguimiento aparte si se pide).

## Problem
La v5 ordenó la app, pero la pantalla de puntuar sigue viéndose como un formulario claro con dos tarjetas. Las apps de entrenamiento de box que usa el público (Heavy Bag Pro) se sienten de deporte: superficies negras de borde a borde, un número enorme rodeado de un anillo, un contador "Round 1/10" en la esquina, filas de "valor grande + − / +" y botones píldora. El juez debería sentir lo mismo en ringside: ver de un golpe el round, su número y el botón de enviar bajo el pulgar.

## Stories
- Como juez en ringside, quiero una pantalla de puntuar oscura y de alto contraste con el round y su avance en un anillo, para leerla con las luces del evento.
- Como juez, quiero que cada esquina sea una fila "número grande · etiqueta · − +" como en las apps de entrenamiento, para calificar con el pulgar sin buscar.
- Como juez, quiero que unirme, esperar y terminar la pelea se vean del mismo producto que puntuar.

## Tendencias y referencias: qué se adopta y por qué

Fuentes de tendencia consultadas (2026-11-17): listas de tendencias móviles 2026 (modo oscuro por defecto, acción principal en el tercio inferior para el pulgar, micro-interacciones funcionales) y Material 3 Expressive (forma, tamaño y movimiento de resorte para guiar la atención). Son blogs de proveedores y la guía de Google de 2025: se toman como orientación, no como dato.

| De la referencia / tendencia | Se adopta | No se adopta | Por qué |
|---|---|---|---|
| Pantalla de round negra de borde a borde (temporizador) | Puntuar, Round enviado y Round tardío sobre superficie negra en los dos temas | Foto o ilustración 3D de fondo | Alto contraste en ringside; la foto no aporta al juez |
| Número enorme dentro de un anillo de avance | Anillo de rounds (cerrados / actual / pendientes) alrededor del round actual | Cuenta regresiva | La central manda el reloj; el juez no cronometra |
| "Round 1/10" y "Combo 1/3" en las esquinas superiores | "Round 3 de 12" a la izquierda y "Juez 2" a la derecha | Totales o marcador | Principio 3: nada de totales durante la pelea |
| Filas "03:00 · Round Length · − +" | Una fila por esquina: número 0–10 grande, etiqueta "Roja"/"Azul" con su píldora, − y + circulares de 56 dp | Stepper pequeño en una tarjeta | El pulgar llega; el número es el protagonista |
| Botones píldora (START outline, filled negro) | Píldora llena para la acción principal (Enviar, Unirme), contorno para la secundaria | Rojo como color de botón | En SIGEBOX el rojo es la esquina Roja (DESIGN §1) |
| Barra negra con marca en mayúsculas, en itálica | Marca "SIGEBOX" en Barlow Condensed 800 con inclinación leve en la barra | Itálica en títulos o texto | Un solo gesto deportivo, en la marca |
| Acento rojo de interfaz | **No**: el dorado sigue siendo el único acento | — | Decisión registrada: el rojo y el azul solo pintan esquinas, siempre con etiqueta |
| Barra de pestañas inferior | **No** | — | El juez sigue un flujo lineal; no hay secciones entre las que moverse |
| Mosaicos con ícono ("Train Now!") | **No** como cuadrícula | — | Es un tic del look generado (product-ui §6); Unirse usa una opción principal y una secundaria |

## Acceptance criteria (EARS)
- The system shall pintar Puntuar, Round enviado y Round tardío sobre la superficie `arena` (negra) en tema claro y oscuro, con tokens v6 nuevos cuyos pares cumplen AA (prueba nueva, sin tocar los tokens v4/v5).
- The system shall mostrar en Puntuar un anillo con un segmento por round (cerrado, actual, pendiente) y, en su centro, "Round N" y "de M".
- The system shall mostrar una fila por esquina con el número (Barlow Condensed 800, ≥ 96 sp), la etiqueta con su píldora de color, y los botones − y + circulares de al menos 56 dp; Roja arriba, Azul abajo.
- The system shall dejar "Enviar round" como píldora llena en el tercio inferior de la pantalla.
- While una pelea está en curso, the system shall no mostrar totales ni ganador (las pruebas actuales lo siguen comprobando).
- The system shall conservar los ids de vista, los textos y el comportamiento que prueban las pruebas actuales; el rediseño es aditivo.
- The system shall dar a Unirse, Esperando aprobación, Siguiente pelea y Términos la misma familia visual: título display, una acción principal en píldora y el resto secundario.
- When el sistema pide reducir movimiento, the system shall cambiar número, anillo y palomita sin animación.
- The system shall generar capturas de cada pantalla del juez, en claro y oscuro, con un arnés de pruebas que solo corre con `SIGEBOX_SCREENSHOTS`.

## Out of scope
- La central (JavaFX).
- Fotos, ilustraciones 3D, cuenta regresiva o temporizador en el teléfono.
- Cambiar el protocolo, los servicios o el flujo de pantallas.
