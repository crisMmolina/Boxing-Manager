# DESIGN · Guía visual de SIGEBOX (v5 «Arena nocturna»)

Guía visual y de experiencia de las dos apps: la del juez (Android, `android/`) y la central (JavaFX, `central/`). Es la fuente de verdad visual para DSK-041 a DSK-045 y AND-022 a AND-024 (spec 006), y **reemplaza a la v4** (DSN-004). Lo que la v5 no cambia sigue como en la v4: los principios, la paleta base, las esquinas, la tipografía, el logo, los textos y la accesibilidad.

- **Pedido v5:** cris (2026-11-09), spec `docs/specs/006-rediseno-v5/spec.md`: «no está mal, pero se siente antiguo»; tendencias actuales, animaciones sencillas y mejor UX/UI. La dirección se aprobó con la maqueta «SIGEBOX v5 · Dirección visual».

## 0. Qué cambia respecto a la v4

| v4 | v5 |
|---|---|
| Central: barra superior negra con pestañas Evento · Jueces · En vivo · Resultado | **Barra lateral** (§5.11) con el evento en curso y su avance, los pasos con ícono (✓ en los terminados, punto que late en En vivo con round abierto), y abajo Bitácora, Inicio, ayuda y tema. Fuera de un evento, como spec 005 |
| Título centrado y "Qué sigue" como línea de texto | **Encabezado a la izquierda** (§5.12): contexto, título de la sección y la acción principal a la derecha. "Qué sigue" es un **aviso con ícono** (§5.13) |
| `Spinner` de JavaFX con flechas | **Stepper** − valor + propio (§5.14) |
| Cartelera como formulario por pelea, con cuatro botones por fila | **Lista** de una línea por pelea con un **menú ⋯** (§5.15) |
| En vivo: "Enviaron 2 de 3" en texto | **Anillo** de tarjetas recibidas, **segmentos** de rounds y **tarjeta por juez** con iniciales (§5.16) |
| Sin íconos | **Íconos de línea** de 2 px, empaquetados (§3.9) |
| Movimiento solo en carga, skeleton y avisos | **Movimiento corto** en cambio de pantalla, hover, presión, número que rueda y palomita al enviar (§3.7) |
| Tarjeta de radio 24 | Radios **8 / 12 / 16 / 20** (§3.8); tarjeta 20 |
| Juez: punto de conexión suelto | **Chip de conexión** en la barra (§5.17) y pantalla **Round enviado** con palomita (§6.4) |

Se conserva de la v4: negro, blanco y dorado; rojo y azul solo para las esquinas; Barlow Condensed + Inter empaquetadas; ningún total ni ganador durante la pelea; sin cristal, neón, resplandores, degradados ni sombras de color; errores nunca en rojo.

## 0.1 Qué cambió de la v3 a la v4 (historia)

- **Pedido:** cris (2026-11-03), spec `docs/specs/004-eventos/spec.md` §Imagen y diseño, §Formularios, §Ayuda de conexión y §Arranque: «esa combinación de blanco y negro con dorado me gusta, pero mejóralo». Logo de guantes rojos «medio animados», etiquetas siempre visibles arriba de cada campo, *skeletons* al cargar y un botón "?" con la guía de reconexión.
- **Manda** `PROJECT.md` principio 3, más estricto desde la spec 004: nadie ve totales ni ganador durante la pelea, **sin excepción** (ya no existe "Ver resumen parcial"). Un juez solo ve lo suyo.
- **Idioma:** español (México). Se dice **evento**, nunca "función", en todo lo que se ve.
- **Sin internet:** fuentes, logo, íconos y la guía de conexión van dentro de las apps.
- **Medición:** todos los contrastes y simulaciones de daltonismo salen de `docs/design/contraste.py`, que escribe `docs/design/contraste.md`. Si cambias un color, cambia el script y vuelve a correrlo.

| v3 | v4 |
|---|---|
| Fondo crema «cantera» `#F7F4EF` y noche cálida `#14120F` | **Blanco** casi puro `#FAF9F6` con tarjetas `#FFFFFF`, y **negro** `#0E0E0C`. Menos beige, más contraste: se lee como blanco y negro |
| Dorado solo en detalles finos | El dorado sube de rango: **marca** (barra negra con SIGEBOX en dorado), paso activo, foco, selección y, en tema oscuro, el **botón principal**. Sigue sin aparecer en bloques de fondo grandes ni junto a una esquina |
| Barra superior del color del fondo | **Barra superior negra en los dos temas**, con un filete dorado de 1 px abajo. Es la firma visual de las dos apps |
| Marca: la palabra SIGEBOX | Logo de **guantes rojos** (ilustración de licencia libre, §3.6) + la palabra SIGEBOX |
| Sin pantalla de carga | Pantalla de carga con el logo animado (§5.1) |
| Etiqueta arriba solo en algunos formularios de la central | **Todo campo** de las dos apps: etiqueta arriba siempre visible + ejemplo dentro (§5.4) |
| "Cargando…" o pantallas vacías | *Skeletons* (§5.6) |
| Sin ayuda de conexión | Botón "?" fijo en la barra y visible junto a todo "Sin conexión" (§5.8, `docs/design/guia-conexion.md`) |
| Barra de pasos Pelea · Jueces · En vivo · Resultado | **Evento · Jueces · En vivo · Resultado**, con Terminación · Bitácora como enlaces secundarios; sin Corrección (§7) |
| "Ver resumen parcial" en En vivo | Se quita (principio 3) |
| Unirse: IP, pelea, lugar y código | Nombre, Código (6 dígitos) e IP, con "Escanear QR" como acción principal (§6.2) |
| Tarjeta final con botón "Enviar tarjeta final" | Se envía sola; el teléfono muestra "Espera la siguiente pelea" o "El evento terminó" (§6.5) |
| Cada round empezaba en 10–10 | **Sin valor por defecto** (decisión de cris, 2026-11-03; principio 2): al abrir un round el número muestra «—» y el juez toca + o − en cada esquina antes de enviar (§5.9) |
| Errores con texto | Errores neutros: ícono de alerta + texto + borde de 2 px en `ink`. **Nunca rojo**, porque el rojo es de la esquina roja (§5.4) |

Se conserva de la v3: Barlow Condensed recta e Inter; guinda y azul de la CDMX solo para las esquinas (mismos valores y misma medición de daltonismo); superficies sólidas tonales, sin cristal; el punto de conexión que cambia de forma; los botones − y + de 76 dp con háptica; Material 3 Expressive en Android; la rejilla bento de la central.

## 1. Principios

1. **Blanco, negro y dorado; rojo y azul son de las esquinas.** La interfaz es blanco y negro. El dorado es el único acento. Guinda y azul solo pintan la esquina roja y la azul, siempre con su etiqueta "Roja" / "Azul".
2. **El rojo del logo no es un color de la interfaz.** Los guantes del logo son una ilustración: aparecen en la pantalla de carga, en la barra superior y en el ícono, y nunca dentro de una tarjeta de esquina, junto a un botón de esquina, ni como color de texto, borde o error.
3. **Un protagonista por pantalla.** En el juez es el número de puntos; en la central, el estado del round (o el evento, en Inicio).
4. **Nunca solo el color.** Esquinas con etiqueta y posición fija (Roja arriba o a la izquierda); conexión con forma (lleno o anillo); errores con ícono y texto.
5. **El nombre del campo siempre a la vista.** La etiqueta va arriba y no desaparece; el ejemplo de adentro sí.
6. **Nunca una pantalla vacía.** Mientras algo carga se ve su forma (*skeleton*); si algo falla, se dice qué hacer.
7. **La ayuda vive dentro de la app.** El "?" abre la guía sin internet, en pasos cortos.
8. **Pensada para la arena.** Gente de pie, con prisa y luz irregular: tema oscuro para poca luz, números enormes, botones grandes, cero ambigüedad.
9. **Un juez solo ve lo suyo, y nadie ve totales durante la pelea.** Ver `PROJECT.md` principio 3.
10. **Nada parece oficial.** Los colores de las esquinas y del dorado vienen del manual de identidad de la CDMX (ver «Fuente de los colores»), pero no se usa el escudo, logotipos, lema, tipografía ni la composición guinda con dorado del gobierno.

## 2. Lenguaje visual: «esquina de campeonato»

Inspirado en el cinturón de campeonato y la tarjeta impresa del juez: papel blanco, tinta negra y un filete dorado.

- **Tema claro.** Fondo blanco cálido `#FAF9F6`, tarjetas blancas `#FFFFFF` con sombra suave, texto casi negro `#111110`. Botón principal negro.
- **Tema oscuro.** Fondo negro `#0E0E0C`, tarjetas de carbón `#1A1917` y `#24221F`, texto marfil. Botón principal **dorado** con texto negro: en la arena oscura el dorado es lo que se ve primero.
- **En los dos temas:**
  - **Barra superior negra** (`bar`) con el logo y SIGEBOX en Barlow Condensed 700 dorado (`bar_accent`), y un filete dorado de 1 px en el borde inferior.
  - Superficies sólidas que se separan por tono; formas grandes y redondeadas.
  - El dorado nunca es fondo de una zona grande: solo marca, filete, paso activo, foco, selección (`accent_soft` como fondo tenue de la fila elegida, siempre con su franja de 3 px `accent`) y el botón principal en oscuro.
  - **En claro**, el dorado vivo (`bar_accent` `#C9A35E`) vive en la barra negra, donde pasa AA; sobre blanco se usa su versión profunda `accent` `#7A5A2A`, que es la que pasa AA como texto y foco. Son el mismo dorado a distinta luz, no un café aparte.
- **El tema** sigue al sistema (`DayNight` en Android; en la central, el botón de alternar en la barra). El juez puede fijarlo en ajustes.

## 3. Tokens

Ninguna pantalla usa valores sueltos.
- **Android:** `res/values/colors.xml` y `res/values-night/colors.xml`, `dimens.xml` y `themes.xml`, tema base `Theme.Material3Expressive.DayNight`.
- **Central:** variables al inicio de `app.css` (`-sg-…`) y dos hojas, `light.css` y `dark.css`, que solo redefinen los colores.
- Los nombres de los tokens son los mismos en las dos apps (`ink_2` → `-sg-ink-2`).

### 3.1 Color

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `bg` | `#FAF9F6` | `#0E0E0C` | Fondo de pantalla |
| `surface` | `#FFFFFF` | `#1A1917` | Tarjetas y **fondo de los campos** |
| `surface_2` | `#F2F0EB` | `#24221F` | Tarjeta elevada, hoja inferior, fila del round abierto, botón secundario |
| `line` | `#E3DFD7` | `#34312C` | Divisores (decorativo) |
| `ink` | `#111110` | `#F5F3EE` | Texto principal, etiquetas de campo, íconos, borde de error |
| `ink_2` | `#57534C` | `#B9B3A8` | Texto de apoyo, ejemplo dentro del campo (placeholder), estados |
| `field_border` | `#8C877D` | `#7D776D` | Borde de 1 px del campo en reposo |
| `primary` / `on_primary` | `#111110` / `#FFFFFF` | `#D4AF5A` / `#111110` | Botón principal |
| `accent` | `#7A5A2A` | `#D4AF5A` | Foco, paso activo, progreso, selección, enlaces |
| `accent_soft` | `#F3E7CF` | `#3A3020` | Fondo de la fila o tarjeta seleccionada (con texto `ink`) |
| `bar` / `on_bar` | `#111110` / `#F7F5F0` | `#0E0E0C` / `#F5F3EE` | Barra superior negra y su texto |
| `on_bar_2` | `#B9B3A8` | `#B9B3A8` | Pasos inactivos y texto secundario en la barra |
| `bar_accent` | `#C9A35E` | `#D4AF5A` | SIGEBOX, filete y línea del paso activo en la barra |
| `red` / `on_red` | `#9F2241` / `#FFFFFF` | `#E58A9D` / `#111110` | **Solo** esquina roja: píldora, botón + |
| `red_numeral` | `#9F2241` | `#E58A9D` | Números de la esquina roja |
| `blue` / `on_blue` | `#266CB4` / `#FFFFFF` | `#8CB8EE` / `#111110` | **Solo** esquina azul |
| `blue_numeral` | `#266CB4` | `#8CB8EE` | Números de la esquina azul |
| `connected` | `#027A35` | `#4CC97A` | Punto **lleno** de conectado |
| `disconnected` | `#57534C` | `#B9B3A8` | **Anillo** vacío de sin conexión |
| `skeleton` | `#E9E6DF` | `#36332E` | Bloques de *skeleton* (decorativo, sin texto) |

### 3.2 Contraste AA medido

Medido con `python3 docs/design/contraste.py` (luminancia relativa WCAG 2.x). Texto ≥ 4.5; componentes de interfaz ≥ 3. **Resultado: 0 pares sin AA, 28 pares por tema.** Detalle con los hex en `docs/design/contraste.md`.

| Par (primer plano / fondo) | Tipo | Claro | Oscuro |
|---|---|---|---|
| `ink` / `bg` | texto | 17.95 | 17.42 |
| `ink` / `surface` | texto | 18.89 | 15.84 |
| `ink` / `surface_2` | texto | 16.59 | 14.31 |
| `ink_2` / `bg` | texto | 7.26 | 9.27 |
| `ink_2` / `surface` | texto | 7.64 | 8.43 |
| `ink_2` / `surface_2` | texto | 6.71 | 7.61 |
| `on_primary` / `primary` | texto | 18.89 | 9.07 |
| `ink` / `accent_soft` | texto | 15.42 | 11.66 |
| `on_bar` / `bar` | texto | 17.34 | 17.42 |
| `on_bar_2` / `bar` | texto | 9.07 | 9.27 |
| `bar_accent` / `bar` | texto | 7.99 | 9.27 |
| `on_red` / `red` | texto | 7.56 | 7.61 |
| `on_blue` / `blue` | texto | 5.40 | 9.18 |
| `red_numeral` / `surface` | texto | 7.56 | 7.07 |
| `blue_numeral` / `surface` | texto | 5.40 | 8.54 |
| `red_numeral` / `bg` | texto | 7.18 | 7.78 |
| `blue_numeral` / `bg` | texto | 5.13 | 9.39 |
| `accent` / `bg` | texto | 6.00 | 9.27 |
| `accent` / `surface` | texto | 6.32 | 8.43 |
| `accent` / `surface_2` | texto | 5.55 | 7.61 |
| `primary` / `bg` | ui | 17.95 | 9.27 |
| `primary` / `surface` | ui | 18.89 | 8.43 |
| `field_border` / `surface` | ui | 3.57 | 3.96 |
| `red` / `surface` | ui | 7.56 | 7.07 |
| `blue` / `surface` | ui | 5.40 | 8.54 |
| `accent` / `surface_2` (foco) | ui | 5.55 | 7.61 |
| `connected` / `surface` | ui | 5.47 | 8.32 |
| `disconnected` / `surface` | ui | 7.64 | 8.43 |

Reglas que salen de la medición:
- El campo va sobre `surface` (no sobre `surface_2`): así su borde pasa 3:1. Si un formulario va dentro de una hoja `surface_2`, el campo conserva su fondo `surface`.
- `skeleton` y `line` son decorativos y no llevan texto encima.
- El logo (ilustración roja) no es un par de contraste: siempre va sobre `bar` negra o sobre `bg`, con su propio contorno oscuro.

### 3.3 Esquinas y daltonismo

ΔE76 entre los colores simulados (Machado 2009, severidad 1.0); ≥ 20 se distingue y < 10 se confunde. Valores de `contraste.md`:

| Par | Normal | Deuteranopía | Protanopía | Tritanopía |
|---|---|---|---|---|
| Roja / Azul, claro | 75.5 | 67.1 | 47.9 | 95.8 |
| Roja / Azul, oscuro | 53.0 | 42.6 | 30.4 | 74.8 |
| Roja / dorado, claro | 48.3 | **14.0** | 31.8 | 43.5 |
| Roja / verde, claro | 98.6 | **8.0** | 38.6 | 97.4 |
| Azul / verde, claro | 88.8 | 74.2 | 78.3 | **15.3** |
| Azul / verde, oscuro | 79.6 | 59.5 | 66.4 | **19.9** |

Las esquinas se distinguen entre sí con cualquier tipo de daltonismo. Los pares débiles se resuelven con reglas: el dorado nunca va dentro ni junto a una tarjeta de esquina; la conexión se distingue por la forma; las esquinas siempre llevan etiqueta y posición fija.

### 3.4 Tipografía

- **Barlow Condensed**, recta (sin cursiva): 800 para números y títulos, 700 para la marca SIGEBOX.
- **Inter** 400, 500, 600 y 700 para todo lo demás.
- Las dos son OFL y ya van empaquetadas con su licencia (`android/app/src/main/res/font/` y `central/src/main/resources/com/sigebox/central/fonts/`).
- Cifras tabulares (`tnum`) en todo número que cambia.
- Minúsculas con mayúscula inicial, salvo AVANZAR y TERMINAR. Nada en cursiva.

| Rol | Fuente | Teléfono (sp) | Central (px) |
|---|---|---|---|
| Número de puntos | Barlow Condensed 800 | 128 (160 en ≥ 400 dp) | 120 en resultado |
| Estado grande (bento) | Barlow Condensed 800 | — | 56 |
| Título de pantalla | Barlow Condensed 800 | 40 | 34 |
| Título de tarjeta | Barlow Condensed 800 | 26 | 24 |
| Marca SIGEBOX en la barra | Barlow Condensed 700, espaciado +2 % | 22 | 22 |
| Etiqueta de campo | Inter 600 | 14 | 13 |
| Cuerpo | Inter 400–500 | 16 | 15 |
| Apoyo, ejemplo dentro del campo | Inter 400 | 13–16 | 13–15 |
| Botón | Inter 700 | 17 | 16 |

Con la fuente del sistema al 130 %, nada se recorta: las tarjetas crecen y el número baja a 112.

### 3.5 Espaciado, radios y elevación

Rejilla de 4: 4, 8, 12, 16, 20, 24, 32, 40 y 56. Margen de 20 dp en el teléfono y de 32 px en la central; separación de 16 entre bloques.

| Elemento | Radio |
|---|---|
| Tarjeta, bloque bento | 24 |
| Hoja inferior | 28 arriba |
| Botones − y + | círculo en reposo, se aplana a 20 al tocar |
| Botón principal y secundario | 14 (rectángulo redondeado, más sobrio que la cápsula de la v3) |
| Campo de texto | 12 |
| Píldora | cápsula |

- **Claro:** una sombra neutra para tarjetas, `0 4 16 rgba(17,17,16,0.08)`; en Android `cardElevation` 2 dp.
- **Oscuro:** sin sombras; la elevación es tonal (`bg` → `surface` → `surface_2`).
- Nunca sombras de color, resplandores, neón ni degradados.

### 3.6 Logo

- **Qué es:** una ilustración de guantes de box rojos, estilo caricatura, con licencia CC0 (dominio público). Fuente, autor, URL y licencia verificada en `docs/design/CREDITOS.md`. El archivo elegido vive en `docs/design/logo/` (SVG, o PNG ≥ 1024 px) y se copia a cada app; nada se descarga en ejecución.
- **Elegido (cris, 2026-11-03):** opción B, par de guantes rojos colgando de sus cordones, `docs/design/logo/guantes-colgando.svg`. En la pantalla de carga el «medio animado» es un balanceo leve desde los cordones (§3.7).
- **Composición (lockup):** guantes a la izquierda + SIGEBOX en Barlow Condensed 700 `bar_accent`, separados 8 px. Altura del logo = 1.4 × la altura de la «S».
- **Versión chica (≤ 32 px):** para la barra y los íconos pequeños se prepara una versión simplificada (un solo guante o la silueta del par, sin brillos ni trazos finos). Se prueba sobre la barra negra a 28 px antes de darla por buena.
- **Limpieza:** si la ilustración trae texto, se quita en el SVG antes de usarla. Se permite simplificar trazos para tamaños chicos; no se cambia el rojo por otro color de la interfaz.
- **Usos:**

| Lugar | Tamaño | Fondo |
|---|---|---|
| Pantalla de carga (las dos apps) | 160 dp / 200 px de alto | `bg` |
| Barra superior de la central | 28 px de alto, con SIGEBOX | `bar` |
| Barra superior del teléfono | 24 dp de alto, con SIGEBOX | `bar` |
| Ícono de Android | ícono adaptativo: guantes en la zona segura de 66 dp sobre fondo `#111110` con filete dorado | — |
| Ícono de la central (`jpackage`) | `.ico` / `.png` de 16 a 256 px, guantes sobre `#111110` | — |

- **Prohibido:** usarlo dentro de una tarjeta de esquina, recolorearlo, deformarlo, ponerle sombras o brillos, o usar los guantes como ícono de acción.

### 3.7 Movimiento

- **Android:** resortes Material 3 (`Motion.Material3.Spring.Standard.Fast.Spatial` para posición y tamaño, `...Fast.Effects` para color y opacidad). Rebote Expressive solo en − y +.
- **Central:** transiciones de 150 a 200 ms con salida suave (`Interpolator.EASE_OUT`).
- **Logo «medio animado»** (solo en la pantalla de carga): los guantes entran con un balanceo desde los cordones: giro de −8° a 0° en 420 ms con resorte (eje en el punto donde cuelgan), y después un vaivén de ±2° cada 1.6 s mientras carga. En la barra superior el logo **no** se mueve.
- **Skeleton:** un brillo que recorre el bloque de izquierda a derecha cada 1.4 s (opacidad 0.6 ↔ 1.0).
- **Movimiento reducido** (`ANIMATOR_DURATION_SCALE` = 0 en Android; ajuste "Reducir movimiento" en la central): el logo aparece quieto con un fundido de 150 ms; el skeleton queda fijo, sin brillo; solo cambian opacidades.
- **v5, tokens de movimiento** (mismos nombres en las dos apps):

| Token | Valor | Uso |
|---|---|---|
| `motion_fast` | 120 ms | Hover, presión (escala 0.98), color de un chip |
| `motion_base` | 180 ms | Menús ⋯, avisos, aparición de una fila |
| `motion_enter` | 220 ms | Entrada de pantalla: fundido de 0 a 1 + desplazamiento de 8 px hacia arriba |
| `motion_number` | 200 ms | Número del juez que rueda al nuevo valor (sube o baja según el signo), con resorte leve |
| `motion_check` | 380 ms | Palomita de "Round enviado": el círculo crece (0.6 → 1.08 → 1) y el trazo se dibuja |
| `motion_pulse` | 1.8 s | Latido del punto de conectado / En vivo: un anillo que se expande y se desvanece |
| Curva | `cubic-bezier(0.2, 0, 0, 1)` (central: `Interpolator.SPLINE(0.2, 0, 0, 1)`) | Todo lo que entra o se mueve |

- **v5, cambio de pantalla.** Central: la pantalla nueva entra con `motion_enter`; la vieja no se anima. Juez: transición de eje compartido (Material `MaterialSharedAxis` X) de ≤ 300 ms entre actividades.
- **v5, reducir movimiento:** sin desplazamientos, latidos, rodar ni dibujar; la palomita aparece completa; solo fundidos de ≤ 150 ms. En la central lo activa el ajuste "Reducir movimiento" o la preferencia del sistema; en el juez, la escala de animación en 0.
- **Háptica:** vibración corta al tocar − o +, al enviar y al abrirse un round (`HapticFeedbackConstants`); nada de sonidos.

### 3.8 Tokens v5

Se suman a los de §3.1, con los mismos nombres en las dos apps (`nav_ink_dim` → `-sg-nav-ink-dim`).

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `nav` | `#111110` | `#0A0A09` | Fondo de la barra lateral (central) |
| `nav_ink` | `#B9B3A8` | `#B9B3A8` | Pasos y enlaces de la barra lateral |
| `nav_ink_dim` | `#8F897E` | `#8F897E` | Pasos que aún no se pueden usar |
| `nav_active` | `#2A2418` | `#2A2418` | Fondo del paso actual, con franja interior de 3 px `bar_accent` |
| `success_soft` / `success_ink` | `#E3F3E8` / `#027A35` | `#16301F` / `#7FE0A2` | Chip "Enviada", "Conectado", ✓ |
| `pending_ink` | `#5C431E` | `#E9C978` | Chip "Calificando…", "Pendiente", sobre `accent_soft` |
| `ink_3` | `#8F897E` | `#8F897E` | Texto terciario: etiquetas en mayúsculas pequeñas, hora |
| `ring_track` | `#E9E6DF` | `#24221F` | Pista del anillo y segmentos aún sin usar (decorativo) |

**Radios:** `r_sm` 8 (chips cuadrados, celdas), `r_md` 12 (botones, campos, aviso), `r_lg` 16 (filas, tarjetas de juez, stepper), `r_xl` 20 (tarjetas y bloques bento). Píldoras y avatares en cápsula. **Espaciado:** la rejilla de 4 de §3.5 sigue; el contenido de la central lleva 40 px a los lados y 28 arriba, con un ancho máximo de 1180 px.

**Contraste AA medido** con `docs/design/contraste.py` (resultado completo en `docs/design/contraste.md`; texto ≥ 4.5:1, grande y componentes ≥ 3:1):

| Par | Claro | Oscuro |
|---|---|---|
| `success_ink` sobre `success_soft` | 4.76 | 8.86 |
| `pending_ink` sobre `accent_soft` | 7.53 | 8.06 |
| `nav_ink` sobre `nav` | 9.07 | 9.50 |
| `nav_ink_dim` sobre `nav` | 5.44 | 5.70 |
| `on_bar` sobre `nav_active` | 14.13 | 13.88 |
| `bar_accent` sobre `nav_active` (texto e ícono del paso actual) | 6.51 | 7.39 |
| `ink_3` sobre `surface` (solo texto grande o componente) | 3.47 | 5.06 |

En la barra lateral el paso actual usa `bar_accent`, no `accent`: en claro, `accent` sobre `nav_active` mide 2.44 y no pasa. En claro, `ink_3` no pasa como texto chico: ahí las etiquetas pequeñas usan `ink_2`.

### 3.9 Íconos

- **Set:** Lucide (licencia ISC, https://lucide.dev), trazo de 2 px, esquinas redondeadas, cuadrícula de 24. Se copian solo los trazos que se usan; nada se descarga en ejecución. Crédito en `docs/design/CREDITOS.md`.
- **Central:** `Icons` dibuja cada uno como `SVGPath` con trazo del color del texto (`-sg-ink`, `-sg-ink-2` o `-sg-accent`), en 16, 20 o 24 px.
- **Juez:** `res/drawable/ic_*.xml` (vectores con `strokeWidth` 2 y `strokeLineCap` round), tintados por tema.
- **Lista v5:** calendario (Evento), usuarios (Jueces), actividad (En vivo), trofeo (Resultado), archivo (Bitácora), casa (Inicio), sol/luna (tema), ayuda, información (aviso), palomita, equis, más/menos, mas-horizontal (⋯), play (iniciar pelea), reloj (calificando), deshacer (corregida), alerta, wifi / wifi apagado, flecha derecha (enviar), agarradera (arrastrar), código QR.
- **Nunca:** emoji como ícono, íconos rellenos de color, los guantes del logo como ícono de acción.

## 4. Tendencias: qué se adopta y por qué

| Tendencia v5 | Se adopta | No se adopta | Por qué |
|---|---|---|---|
| Barra lateral de app de escritorio | Navegación vertical con ícono, avance del evento y estado vivo | Barra lateral plegable, multinivel | El operador ve dónde está y qué falta sin leer pestañas |
| Encabezado de página con acción principal | Título a la izquierda, acción arriba a la derecha con su atajo | Barras de herramientas con muchos botones | Una sola acción principal por pantalla, siempre en el mismo lugar |
| Micro-interacciones | Hover que eleva, presión que comprime, número que rueda, palomita | Animaciones decorativas largas, partículas | Confirman lo que pasó sin estorbar; se apagan con reducir movimiento |
| Listas densas con menú contextual | Una línea por pelea con ⋯ | Tablas con botones en cada celda | Se lee de un vistazo; las acciones no compiten |
| Datos visuales sin números sensibles | Anillo y segmentos de avance | Gráficas de puntos durante la pelea | Se ve cuánto falta sin mostrar marcadores (principio 3) |

Tabla de la v4 (sigue vigente):


| Tendencia | Se adopta | No se adopta | Por qué |
|---|---|---|---|
| Monocromo con un acento metálico | Blanco y negro + dorado único | Varios acentos, degradados dorados «metálicos» | Se ve premium y deja que rojo y azul (las esquinas) sean lo único con color fuerte |
| Tipografía expresiva | Números de 128–160 sp en Barlow Condensed 800 | Fuentes descargadas | El juez lee su número de un vistazo |
| Material 3 Expressive (Android) | Formas grandes, botones que se aplanan al tocar, `LoadingIndicator` | Compose (la app es de vistas XML) | Blancos grandes para el pulgar |
| Bento (central) | Bloques de distinto tamaño en En vivo e Inicio | Métricas de vanidad | El operador ve de un golpe qué falta |
| Skeleton loading | En toda lista o tarjeta que espera datos | Spinners a pantalla completa | La pantalla nunca se ve rota ni vacía |
| Etiqueta fija arriba | En todo campo | Etiquetas flotantes que se encogen dentro del campo | El nombre no desaparece al escribir; se lee con prisa |
| Modo oscuro | Completo, con su propia medición AA | Oscuro forzado | Arenas con poca luz |

## 5. Componentes (las dos apps)

| Componente | Juez (Android) | Central (JavaFX) |
|---|---|---|
| Pantalla de carga | `SplashActivity` (o `core-splashscreen`) con logo animado | `SplashPane` sobre la ventana principal |
| Barra superior | `MaterialToolbar` con `Widget.Sigebox.Toolbar` (fondo `bar`) | `.topbar` |
| Botón principal | `MaterialButton` `Widget.Sigebox.Button.Primary` | `.button.primary` |
| Botón secundario | `Widget.Sigebox.Button.Secondary` | `.button.secondary` |
| Campo con etiqueta | `TextView` (etiqueta) + `TextInputLayout` `Widget.Sigebox.Field` (outlined, sin hint flotante) | `.field` = `Label.field-label` + `TextField` |
| Tarjeta | `MaterialCardView`, `surface`, radio 24 | `.card` |
| Skeleton | `View` con `@drawable/skeleton` + `SkeletonAnimator` | `.skeleton` (`Region`) + `Timeline` |
| Barra de pasos | Segmentos de round (§5.7) | `.steps` en la barra superior |
| Botón "?" | `MaterialButton` ícono 48 dp en la barra | `.help-button` en la barra |
| Inicio con eventos | — (el juez no tiene historial) | `HomeView` (§7.1) |
| Píldora de esquina | Fondo `red` / `blue`, texto `on_red` / `on_blue` | `.pill.red` / `.pill.blue` |
| Punto de conexión | 10 dp: lleno `connected` o anillo 2 dp `disconnected` | `Circle` 10 px lleno o con `stroke` |
| Foco | Anillo de 2 dp en `accent`, separado 2 dp | Anillo de 2 px en `accent` (`-fx-focus-color`) |

### 5.1 Pantalla de carga

```
┌──────────────────────────────┐
│                              │
│           (guantes)          │  logo 160 dp, balanceo (§3.7)        
│           SIGEBOX            │  Barlow 700, 32, ink
│    ────────────────────      │  barra de progreso indeterminada, 2 dp, accent
│    Abriendo la base…         │  Inter 13, ink_2 (central) · "Preparando…" (juez)
└──────────────────────────────┘
```
- Fondo `bg`. Mínimo 600 ms y máximo 3 s si no hay error (spec 004 §Arranque).
- Si hay error al cargar: el logo queda quieto y abajo aparece el error (§5.4 estilo error) con un botón principal "Reintentar".
- Sin términos aceptados: la carga pasa a Términos; si no, a Inicio (central) o a Unirse / la pantalla restaurada (juez).

### 5.2 Botón principal y secundario

| | Principal | Secundario | Texto (terciario) |
|---|---|---|---|
| Relleno | `primary` | `surface_2` con borde 1 px `field_border` | ninguno |
| Texto | `on_primary`, Inter 700 | `ink`, Inter 700 | `accent`, Inter 600 |
| Alto | 52 dp / 44 px | 52 dp / 44 px | 48 dp / 36 px |
| Radio | 14 | 14 | — |

- **Presionado:** el relleno se oscurece 8 % (claro) o se aclara 8 % (oscuro); en Android, aplanado leve con resorte.
- **Deshabilitado:** opacidad 0.38 y, si hay motivo, una línea de apoyo debajo ("Falta que se conecten 2 jueces.").
- **Ocupado:** el texto cambia ("Guardando…") y aparece un indicador de 16 dp a la izquierda; el ancho no cambia.
- Un solo botón principal por pantalla o por bloque.

### 5.3 Tarjeta

- Fondo `surface`, radio 24, padding 20 (teléfono) / 24 (central), sombra solo en claro.
- Título en Barlow Condensed 800 26 sp / 24 px; apoyo en `ink_2`.
- **Seleccionada:** fondo `accent_soft` y borde izquierdo de 3 px `accent`.
- **Tarjeta de esquina (juez):** píldora "Roja" / "Azul" y nombre arriba; −, número en `red_numeral` / `blue_numeral`, +. Sin dorado, sin logo, sin "Mi total".

### 5.4 Campo con etiqueta arriba

```
Código                         ← etiqueta: Inter 600, ink, siempre visible
┌──────────────────────────┐
│ 123456                   │   ← ejemplo dentro (placeholder) en ink_2; desaparece al escribir
└──────────────────────────┘
6 dígitos, te lo da la mesa.   ← apoyo opcional: Inter 400 13, ink_2
```

- **Etiqueta:** encima del campo, a 6 dp / 6 px, Inter 600, color `ink`. Nunca se oculta ni se mueve. Si el campo es opcional, la etiqueta dice "(opcional)". Se asocia al campo para lectores de pantalla (`labelFor` en Android; `Label.setLabelFor` en JavaFX).
- **Ejemplo (placeholder):** un valor de ejemplo real ("María López", "123456", "192.168.43.1"), en `ink_2`. No repite la etiqueta.
- **Campo:** fondo `surface`, borde 1 px `field_border`, radio 12, alto 52 dp / 44 px, texto Inter 400 16 sp / 15 px `ink`.
- **Foco:** borde 2 px `accent` **y** anillo exterior de 2 px `accent` separado 2 px.
- **Error:** borde 2 px `ink`, un ícono de alerta (triángulo de trazo con «!») **dentro del campo a la derecha** y, justo debajo, el mismo ícono + el mensaje en Inter 500 `ink` ("Ese código no es válido. Pídele a la mesa el código de tu lugar."). **Sin rojo.** El mensaje se anuncia al lector de pantalla.
- **Foco frente a error:** el foco se reconoce por el anillo doble dorado; el error, por el ícono dentro del campo y el mensaje debajo. Nunca dependen solo del tono del borde.
- **Deshabilitado:** opacidad 0.38.
- **Teclado:** Código → numérico, solo 6 dígitos (`inputType="number"`, `maxLength=6`; en la central, un `TextFormatter` que rechaza otra cosa). IP → `inputType="text"` con dígitos y punto. En el código, cifras en Barlow Condensed 800 24 sp con espaciado amplio.
- En Android **no** se usa la etiqueta flotante de `TextInputLayout` (`app:hintEnabled="false"`); la etiqueta es un `TextView` aparte y el placeholder es `placeholderText`.

### 5.5 Selección y listas

- Fila de 56 dp / 44 px, divisor `line`. Fila seleccionada: `accent_soft` + barra izquierda 3 px `accent`.
- Casillas y radios en `ink`, marcados con check `on_primary` sobre `primary`.

### 5.6 Skeleton

- Bloques `skeleton` con la forma del contenido que viene: líneas de texto de 12 dp / px de alto y radio 6, al 100 %, 80 % y 60 % de ancho; tarjetas con su mismo radio; círculos para los puntos de conexión.
- Animación: brillo de izquierda a derecha cada 1.4 s. **Movimiento reducido: fijo, sin brillo.**
- Se muestra mientras una sección carga sus datos o el teléfono espera el primer `STATE`. Reemplaza a "Cargando…".
- El skeleton reemplaza **toda** la sección que carga (por ejemplo, las 4 filas de Inicio); nunca convive con filas ya cargadas de la misma lista.
- Si tarda más de 3 s, debajo aparece en `ink_2` "Esto está tardando más de lo normal." y, si aplica, el botón "?".
- Accesible: el contenedor se anuncia como "Cargando" una sola vez (`contentDescription` / `accessibleText`).

### 5.7 Barra de pasos con paso activo

- **Central (barra superior, 52 px, fondo `bar`):** logo + SIGEBOX a la izquierda y, junto a la marca, **Inicio** (ícono de casa + texto, `on_bar`; clic en la marca también lleva a Inicio); al centro **Evento · Jueces · En vivo · Resultado** en Inter 600 15 px; a la derecha "Terminación" y "Bitácora" como dos enlaces secundarios separados 16 px, con zona de clic de 36 px (`on_bar_2`), el "?" y el botón de tema.
  - Paso activo: texto `on_bar` + línea de 3 px `bar_accent` debajo + `aria`/`accessibleRole` de pestaña seleccionada.
  - Pasos completados: `on_bar` con un check pequeño; pendientes: `on_bar_2`.
  - Navegable con teclado (`Ctrl+1…4`) y foco visible en `bar_accent`.
- **Teléfono (progreso de rounds):** segmentos de 6 dp: completados en `ink_2`, actual en `accent`, pendientes en `line`, con el texto "Round 4 de 10" encima. Nunca dentro de una tarjeta de esquina.

### 5.8 Botón "?" y ayuda de conexión

- **Siempre** en la barra superior de las dos apps: ícono de signo de interrogación en un círculo, 48 dp / 36 px, color `on_bar`, descripción accesible "Ayuda de conexión".
- **Además**, junto a cada "Sin conexión" (aviso del teléfono, bloque de juez desconectado en la central) aparece un botón secundario pequeño "? Cómo reconectar": 36 dp / px de alto, relleno `surface_2`, borde 1 px `field_border`, radio 14, Inter 600 14. Si no cabe en la misma línea, va en una segunda línea debajo del estado, nunca partido.
- Abre una hoja inferior (teléfono) o un panel lateral de 420 px (central) con el título "Recuperar la conexión" y los pasos de `docs/design/guia-conexion.md` (la parte que toca a esa app primero), numerados en Barlow Condensed 800 `accent`, texto Inter 16 / 15 `ink`.
- El texto viaja dentro de la app (recurso de cadenas / archivo de recursos). Sin enlaces a internet.
- Botón principal al final: "Listo" (cierra).

### 5.9 Botones − y +

76 dp (mínimo 64), − en `surface_2` con ícono `ink`, + con relleno de la esquina; se aplanan con resorte y vibración; deshabilitados al 0.38; mantener presionado no repite.

- **Sin valor por defecto** (decisión de cris, 2026-11-03; `PROJECT.md` principio 2). Al abrir un round **no** se pone 10–10: cada esquina muestra «—» en `ink_2` (mismo tamaño que el número) y el apoyo "Toca + o − en cada esquina." El juez tiene que tocar + o − en **las dos** esquinas antes de enviar. El valor que da el primer toque lo fija la app según la spec 003; la guía no lo define.
- "Enviar round N" queda deshabilitado mientras alguna esquina siga en «—», con la línea de apoyo "Falta puntuar la esquina azul." (o roja).
- Rango 0–10: − se deshabilita en 0 y + en 10. Con «—», los dos están habilitados.
- Descripción accesible: "Sumar un punto a la esquina roja, sin valor" o "…, valor actual 9".

### 5.10 Avisos

- **Sin conexión (teléfono):** franja `surface_2` arriba del contenido, anillo `disconnected`, "Sin conexión. Tus puntos se guardan y se envían al reconectar." y "? Cómo reconectar".
- **Confirmaciones y hojas:** `surface_2`, radio 28 arriba; el botón que confirma es el principal.
- Sin *toasts* para errores: los errores quedan a la vista hasta resolverse.

### 5.11 Barra lateral (central, v5)

```
┌──────────────────────┐
│ (guantes) SIGEBOX    │  Barlow 700 24, bar_accent
│ ┌──────────────────┐ │
│ │ Evento en curso  │ │  ink_3 12 · nombre Inter 600 15
│ │ Copa Delegación  │ │
│ │ ▬▬▬▬▬▬──────  2/5│ │  barra de avance 6 px: accent sobre ring_track
│ └──────────────────┘ │
│ PELEA 3              │  Inter 700 11, nav_ink_dim
│ ▣ Evento          ✓  │  ícono 20 + texto Inter 500 15, nav_ink
│ ◫ Jueces          ✓  │
│▌∿ En vivo         ●  │  actual: fondo nav_active, franja de 3 px bar_accent, punto que late
│ ⌂ Resultado          │  aún no: nav_ink_dim
│                      │
│ ▤ Bitácora           │
│ ⌂ Inicio             │
│ [? Ayuda]  [◐]       │
└──────────────────────┘
```
- Ancho 248 px, fondo `nav` en los dos temas (la firma negra de la v4 pasa de arriba a la izquierda).
- Fuera de un evento (spec 005): solo logo, Inicio (si no se está en Inicio), ayuda y tema.
- Teclado: Tab recorre los pasos; Enter abre; el paso actual lleva `aria`/`accessibleRole` de selección.

### 5.12 Encabezado de página (central, v5)
Contexto en `ink_2` 13 (miga de pan, o "Pelea 3 de 5 · Roja vs Azul" con los nombres en su color numeral), título Barlow 800 40 igual a su sección, chip de estado opcional y, a la derecha, la acción principal (y a lo más una secundaria).

### 5.13 Aviso "Qué sigue" (las dos, v5)
Ícono de información 18 + **"Qué sigue:"** en `ink` 600 + la instrucción. Claro: fondo `accent_soft`, texto `#3D2E14`. Oscuro: fondo `surface`, borde 1 px `line`. Radio `r_md`. Una por pantalla, siempre debajo del encabezado.

### 5.14 Stepper (central, v5)
`[ − ]  4  [ + ]` en una cápsula `surface_2` de radio `r_md`, botones de 36 px, valor en Barlow 800 24 con cifras tabulares. Flechas ↑/↓ y las teclas + y − cambian el valor; en el límite el botón se apaga. Reemplaza a todo `Spinner`.

### 5.15 Lista con menú ⋯ (central, v5)
Fila de 56 px con radio `r_lg`: agarradera (arrastrar para reordenar), número Barlow 800 24, esquinas con punto de color de 10 px y nombre Inter 600 16, rounds, jueces, chip de estado y botón ⋯ de 36 px. Hover: fondo `surface` y sombra de §3.5. El menú (radio 14, sombra 0 12 32 al 14 %) reúne Editar esquinas, Cambiar rounds y jueces, Subir/Bajar, Saltar y Quitar (este último separado por una línea).

### 5.16 Anillo, segmentos y tarjeta de juez (las dos, v5)
- **Anillo** (central, En vivo): 176 px, pista de 12 px `ring_track`, arco `accent` con extremo redondeado, "2/3" en Barlow 800 56 y "tarjetas" en `ink_3`. Nunca muestra puntos.
- **Segmentos de rounds:** uno por round, 6–8 px de alto, cápsula: cerrado `connected`, actual `accent`, futuro `ring_track`.
- **Tarjeta de juez:** avatar de 36 px con iniciales (fondo `nav_active`, texto `accent`), nombre Inter 600 15, "Juez N" en `ink_3`, y a la derecha su estado del round con ícono: ✓ Enviada (`success_ink`), reloj Calificando… (`pending_ink`), anillo Sin conexión + "? Cómo reconectar".

### 5.17 Chip de conexión (juez, v5)
Cápsula de 28 dp en la barra: **Conectado** = punto de 7 dp `connected` que late + texto sobre `success_soft`; **Sin conexión** = anillo + texto sobre `surface_2`, y toca para abrir la ayuda (§5.8).

## 6. Pantallas del juez (Android)

- **v5:** chip de conexión en la barra (§5.17) y segmentos de rounds (§5.16) en Puntuar, Round enviado y Round tardío. Cada esquina lleva una franja izquierda de 6 dp de su color además de su píldora. Al tocar − o +, el número rueda (`motion_number`) con háptica. Tras el `ACK`, **Round enviado**: palomita que se dibuja (`motion_check`), "Round N enviado", "La central lo recibió. Espera a que abra el round N+1.", la tarjeta enviada (solo la propia, principio 3) y "Corregir esta tarjeta" (secundario).

- **Referencia:** 390 × 844 dp en vertical. Desde 720 dp de ancho, las tarjetas van lado a lado (Roja a la izquierda).
- **Barra superior negra** con logo + SIGEBOX, el punto o anillo de conexión con "Juez 2", y el "?".
- Bordes a bordes con insets aplicados a la raíz; pantalla encendida en puntuación y espera.

### 6.1 Carga y términos
Pantalla de carga (§5.1) → Términos en el primer uso (texto en tarjeta con desplazamiento, casilla "Leí y acepto…", botón principal "Aceptar y continuar").

### 6.2 Unirse (`JoinActivity` + `ScanActivity`)

```
┌────────────────────────────────┐
│▓(guantes) SIGEBOX    ○ Sin conexión  (?)▓│  barra negra
│ Unirme a la pelea              │  título Barlow 800 40
│ [  Escanear QR  ]              │  botón principal
│ ── o escribe los datos ──      │
│ Nombre                         │
│ [ María López            ]     │
│ Código                         │
│ [ 1 2 3 4 5 6            ]     │  numérico, 6 dígitos
│ IP de la central               │
│ [ 192.168.43.1           ]     │
│ [        Unirme          ]     │  secundario si "Escanear QR" es el principal
└────────────────────────────────┘
```
- Solo Nombre, Código e IP (spec 004). El QR llena los tres y se une.
- Nota de apoyo: "Primero conéctate al Wi‑Fi del evento."
- Error del código: "Ese código no es válido. Pídele a la mesa el código de tu lugar." (estilo §5.4).
- Si regresa porque el evento terminó, Nombre ya viene escrito.

### 6.3 Esperando aprobación
Tarjeta con `LoadingIndicator`, "Esperando aprobación" y la huella corta en Barlow 800 40 sp.

### 6.4 Puntuar y round tardío
Como en la v3 (tarjeta doble, hoja "¿Enviar este round?", "Round 4 enviado", "Califica el round 3"), con la barra negra y el progreso de rounds de §5.7, **pero cada round abre sin valor** («—» en las dos esquinas, §5.9); también el round tardío. Mientras espera el primer `STATE`: skeleton de dos tarjetas de esquina.

### 6.5 Al terminar una pelea
- **Hay siguiente pelea:** pantalla "Espera la siguiente pelea" con un `LoadingIndicator`, y, si se conocen, las píldoras "Roja" / "Azul" con los nombres de la siguiente. Sin botones.
- **Fue la última:** "El evento terminó" durante 3 s y regresa a Unirse con el nombre escrito.
- La tarjeta final se envía sola: no hay pantalla de confirmación. Si no hay conexión, el aviso de §5.10 dice que se enviará al reconectar.

## 7. Pantallas de la central (JavaFX)

- **Ventana:** 1280 × 800 de referencia, mínimo 1100 × 700.
- **v5:** barra lateral (§5.11) + contenido con encabezado (§5.12), aviso "Qué sigue" (§5.13) y bloques. Los esquemas de abajo describen el contenido; donde dicen "barra superior", en la v5 es la barra lateral. Todo se opera con teclado y con el foco visible.
- **v5 por pantalla:** Evento = base con steppers + cartelera en lista (§5.15). En vivo = anillo + "Round N de M" con segmentos + tarjetas de juez + tabla de estados por round con chips (§5.16), sin puntos. Jueces = tarjetas de lugar con código grande, QR sobre blanco y chip de estado. Resultado = encabezado v5; los totales entran contando de 0 a su valor en 400 ms (solo con la pelea cerrada).

### 7.1 Inicio (lista de eventos)

```
┌▓(guantes) SIGEBOX                         (?) ◐ ▓┐
│  Eventos                       [ Nuevo evento ] │  título Barlow 800 34 · botón principal
│ ┌───────────────────────────────────────────┐   │
│ │ Noche de box Iztapalapa         3 nov 2026│   │  tarjeta-fila: nombre Inter 600 16
│ │ 8 peleas · 5 cerradas                    ›│   │  apoyo ink_2
│ ├───────────────────────────────────────────┤   │
│ │ Evento 27 oct 2026             27 oct 2026│   │
│ │ 6 peleas · 6 cerradas                    ›│   │
│ └───────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```
- Del más reciente al más antiguo; fila completa clicable y navegable con flechas + Enter.
- Mientras carga: skeleton de 4 filas. Sin eventos: "Aún no hay eventos." + el mismo botón "Nuevo evento".
- **Nuevo evento:** hoja o panel con campos con etiqueta arriba: Nombre (opcional), Fecha, Rounds por pelea (1–12), Jueces (1–5), Número de peleas (1–20). Botón principal "Crear evento".

### 7.2 Evento (cartelera) y Jueces
- Cartelera en tarjetas por pelea: "Pelea 3", píldoras Roja / Azul con nombre o "Esquina roja 3" / "Esquina azul 3", estado en `ink_2`.
- Jueces: rejilla de bloques «Lugar N». **Sin conectar:** anillo, QR de 168 px **siempre sobre blanco** (también en oscuro), código de 6 dígitos en Barlow 800 28 px y el "? Cómo reconectar". **Esperando aprobación:** nombre y huella, "Aprobar" (principal) y "Rechazar" (secundario). **Conectado:** punto lleno y "Juez 2 · María López".

### 7.3 En vivo (bento, sin totales)
- Bloque grande: round actual, estado y "Enviaron 2 de 3". Bloques por juez con punto/anillo y estados de rounds en `ink_2`; los desconectados llevan "? Cómo reconectar".
- Acciones: "Cerrar round 3" / "Abrir round 4" (principal). Descuentos: "−1 roja" y "−1 azul" como **botones secundarios** con una franja izquierda de 4 px del color de su esquina y su etiqueta; piden confirmación. Lista de descuentos debajo.
- **Sin "Ver resumen parcial", sin puntos, totales ni ganador.**

### 7.4 Resultado
Tabla por juez: rounds en filas, Roja y Azul en columnas con sus colores numerales, descuentos y suma; fila final con la suma por juez. Sin ganador ni tipo de decisión en pantalla y sin botón "Actualizar". "Exportar PDF" (principal), "Exportar CSV" (secundario).

### 7.5 Terminación y bitácora
Formularios con campos de §5.4. Terminación se confirma escribiendo **TERMINAR**. Bitácora: tabla con cabecera Inter 600 y filas de 36 px; se actualiza sola.

## 8. Textos

Frase en minúscula con mayúscula inicial, verbos claros, sin signos de exclamación ni disculpas. Una acción conserva su nombre en todo el flujo. Se dice **evento**.

| Clave | Texto |
|---|---|
| Carga | Abriendo la base… · Preparando… · Reintentar |
| Conexión | Conectado · Sin conexión · Reconectando… · Esperando aprobación · Cómo reconectar · Recuperar la conexión |
| Juez | Unirme a la pelea · Escanear QR · Unirme · Nombre · Código · IP de la central · Espera la siguiente pelea · El evento terminó |
| Central | Eventos · Nuevo evento · Crear evento · Evento · Jueces · En vivo · Resultado · Terminación · Bitácora · Inicio · −1 roja · −1 azul |
| Errores | Ese código no es válido. Pídele a la mesa el código de tu lugar. · Esto está tardando más de lo normal. |

## 9. Accesibilidad

- AA medido en los dos temas (§3.2): ≥ 4.5 en texto y ≥ 3 en componentes.
- Esquinas con texto y posición fija; conexión con forma y descripción ("Juez 2, conectado"); errores con ícono y texto.
- Etiquetas de campo asociadas al campo; el placeholder nunca es la única pista.
- Zonas táctiles ≥ 48 dp; − y + de 76. Fuente del sistema hasta 130 % sin recortes.
- Movimiento reducido respetado en logo, skeleton y transiciones.
- En la central todo funciona con teclado y el foco es visible en la barra negra y en el contenido.

## 10. Lo que no se hace

- Rojo o azul fuera de las esquinas (salvo la ilustración del logo, en sus lugares de §3.6). Errores en rojo.
- Dorado como fondo de zonas grandes, dentro de una tarjeta de esquina o junto a un relleno de esquina.
- Etiquetas que solo viven dentro del campo, etiquetas flotantes que se encogen.
- "Cargando…" sobre una pantalla vacía; spinners a pantalla completa.
- Totales o ganador en pantalla durante la pelea, en ninguna forma.
- Escudo, logotipos, lema, tipografía o composición guinda con dorado del gobierno; cualquier cosa que haga parecer la app oficial.
- Cristal con desenfoque, neón, resplandores, degradados o sombras de color.
- Imágenes o fuentes descargadas en ejecución; enlaces a ayuda en internet.

## 11. Fuente de los colores

Guinda `#9F2241` (Pantone 7420 C), azul `#266CB4` (285 C), verde `#027A35` (356 C) y dorado `#BC955C` (465 C, base del dorado de la v4) vienen del «Manual de identidad institucional · Gobierno de la Ciudad de México 2024-2030» (consultado el 2026-10-26, págs. 16 y 25; ver DSN-003). Los demás tonos se derivan subiendo o bajando la luminosidad para cumplir AA. **Solo se toman los colores.**

## 12. Lista de revisión visual

- [ ] Las dos apps se ven en blanco/negro/dorado, en claro **y** oscuro, sin valores fuera de los tokens.
- [ ] `python3 docs/design/contraste.py` termina con "0 pares sin AA".
- [ ] Rojo y azul solo en las esquinas (y en el logo); los errores no son rojos.
- [ ] La barra superior es negra con el filete dorado en las dos apps y en los dos temas.
- [ ] La pantalla de carga muestra el logo animado (quieto con movimiento reducido), entre 600 ms y 3 s.
- [ ] Todo campo tiene su etiqueta arriba siempre visible y un ejemplo dentro.
- [ ] Nada se ve vacío al cargar: hay skeletons.
- [ ] El "?" está en la barra y junto a cada "Sin conexión", y abre la guía sin internet.
- [ ] Inicio lista los eventos con fecha, peleas y cerradas, y tiene "Nuevo evento".
- [ ] La barra de pasos dice Evento · Jueces · En vivo · Resultado, con el paso activo marcado en dorado.
- [ ] Al abrir un round el teléfono muestra «—», no 10–10, y no deja enviar hasta tocar + o − en las dos esquinas.
- [ ] Un juez nunca ve tarjetas de otro ni su total; la central no muestra totales ni ganador durante la pelea.
- [ ] La central se opera con teclado y el foco es visible en los dos temas.
