# DESIGN · Guía visual de SIGEBOX (v2)

Guía visual y de experiencia de las dos apps: la del juez (Android, `app/`) y la central (JavaFX, `BOXCENTRAL/`). Es la fuente de verdad visual para AND-007, DSK-005, DSK-011 y DSK-012, y reemplaza a la v1 (DSN-001).

- **Origen:** la propuesta "Gong: guía de diseño de interfaz (v1)" de zoe (2026-10-25), adaptada a SIGEBOX, a su stack y a su protocolo. El nombre del producto sigue siendo **SIGEBOX**.
- **Spec:** `docs/specs/001-sigebox/spec.md`. Manda `PROJECT.md` principio 3: nadie ve totales ni ganador durante la pelea, salvo "Ver resumen parcial" (registrado en bitácora). El tablero en vivo con puntos y "Va ganando" de DSK-012 se revirtió en DSK-017 (decisión de zoe, 2026-10-26).
- **Idioma:** español (México).
- **Sin internet:** ninguna parte de la UI descarga nada en ejecución. Fuentes e íconos van empaquetados.

## 0. Qué cambió respecto a la v1

| v1 | v2 |
|---|---|
| Sin totales en la central hasta cerrar la pelea; "Ver resumen parcial" 15 s | Igual que v1 (DSK-017): tabla de estados por juez y round, sin puntos; "Ver resumen parcial" 15 s con bitácora |
| Tema claro y tema oscuro "arena" | Solo fondo blanco, con cristal translúcido sobre una textura tenue |
| Chips de estado de colores (ámbar, verde, morado) | Píldoras neutras; el único color de estado es el punto de conexión |
| Teclado 0–10 y campo vacío "—" | Botones − y + con el valor grande; cada round empieza en 10 y una hoja de confirmación muestra el resultado antes de enviar |
| Etiquetas ROJA / AZUL en mayúsculas | "Roja" / "Azul" en una píldora del color de la esquina |
| Roboto y Segoe UI | Inter (texto) y Barlow Condensed recta (números y títulos), empaquetadas |

Lo que **no** se toma de la propuesta, porque el producto o el protocolo no lo tienen: "Mi total" en el teléfono, "Escribiendo…", "El organizador reabrió el round", "Expulsar" y "puntos por round configurables". El puerto es **8765**, no 8080.

## 1. Principios

1. **Un protagonista por pantalla.** En el juez es el número de puntos; en la central, la tabla de tarjetas. Todo lo demás es discreto.
2. **Cristal con propósito.** Las superficies translúcidas agrupan controles o información. No decoran.
3. **El color significa algo.**
   - Rojo y azul identifican las esquinas.
   - El punto verde indica que un juez está conectado.
   - El azul marino es la acción principal.
   - No hay más colores.
4. **Silencio visual.** Sin resplandores, degradados luminosos, sombras de color, neón ni fondos oscuros.
5. **Se entiende sin leer.** Números grandes, íconos simples y textos cortos.
6. **Pensada para la arena.** Gente de pie, con prisa y luz irregular: botones grandes, contraste alto, cero ambigüedad.
7. **Un juez solo ve lo suyo.** Ninguna pantalla del teléfono muestra tarjetas, totales, nombres ni estados de otros jueces, ni su propio total acumulado.
8. **El tablero en vivo es solo del operador.** Nunca se proyecta al público ni viaja a los teléfonos (`STATE` sin cambios).

## 2. Lenguaje visual: cristal claro

Referencia: el Centro de control de iOS. Se adopta el lenguaje, no los recursos: ningún ícono ni logotipo de Apple.

- Controles circulares y cápsulas con radios muy grandes.
- Superficies translúcidas, con borde fino y brillante (más marcado arriba a la izquierda).
- Íconos simples sin etiqueta dentro del botón.
- Estado activo con relleno sólido, no con texto.
- Medidor de cápsula con relleno proporcional al valor.
- Cuadrícula regular con espacios uniformes.

Sobre blanco, el cristal desaparece si no se define. Por eso lleva borde especular, contorno finísimo y sombra gris neutra, y va sobre una textura muy tenue (§4.2).

## 3. Tokens

Ninguna pantalla usa valores sueltos.
- **Android:** `res/values/colors.xml`, `dimens.xml` y `themes.xml`.
- **Central:** variables al inicio de `app.css` (`-sg-…`).

### 3.1 Color

| Token | Hex | Uso |
|---|---|---|
| `bg` | `#FFFFFF` | Fondo de todas las pantallas |
| `ink` | `#0F172A` | Texto principal e íconos |
| `ink_secondary` | `#5B6472` | Texto de apoyo (≈ 6:1 sobre blanco) |
| `navy` | `#14284B` | Acción principal, marca, progreso completado, round actual |
| `red_corner` | `#C62839` | Relleno del botón + y de la píldora de la esquina roja |
| `red_numeral` | `#B5222F` | Números y texto de la esquina roja |
| `blue_corner` | `#1F4E9C` | Relleno del botón + y de la píldora de la esquina azul |
| `blue_numeral` | `#1B438A` | Números y texto de la esquina azul |
| `connected` | `#1E8E5A` | **Solo** el punto de conexión |
| `disconnected` | `#9AA3B2` | Punto de juez sin conexión |
| `line` | `rgba(15,23,42,0.10)` | Divisores y bordes de campos |
| `track` | `rgba(15,23,42,0.08)` | Fondo de medidores |
| `pending` | `#E3E7EE` | Segmento de round pendiente |

Reglas:
- El verde solo existe como punto: no hay chips, fondos ni textos verdes.
- No hay ámbar, naranja ni amarillo.
- Rojo y azul nunca decoran: siempre significan esquina.
- La esquina se identifica siempre también con texto ("Roja", "Azul"), porque los dos colores tienen luminancia parecida.

### 3.2 Tipografía

- **Barlow Condensed**, recta (sin cursiva, pedido de zoe del 2026-10-25):
  - 800 (ExtraBold) para números de puntos y títulos.
  - 700 (Bold) para la marca.
- **Inter** 400, 500, 600 y 700 para todo lo demás.
- Las dos son OFL y van empaquetadas con su licencia:
  - Android: `app/src/main/res/font/`.
  - Central: `BOXCENTRAL/src/main/resources/com/sigebox/central/fonts/`, cargadas con `Font.loadFont` al arrancar.
- Minúsculas con mayúscula inicial; nada de mayúsculas sostenidas.
- Ningún texto en cursiva ni inclinado.
- Cifras tabulares (`tnum`) donde la fuente las tenga, para que los números no bailen.

| Rol | Fuente | Teléfono (sp) | Central (px) |
|---|---|---|---|
| Número de puntos | Barlow Condensed 800 | 120 (148 en pantallas ≥ 400 dp de ancho) | 26 en tabla, 112 en resultado |
| Título de pantalla | Barlow Condensed 800 | 40 | 32 |
| Título de tarjeta | Barlow Condensed 800 | 26 | 26 |
| Cuerpo | Inter 400–500 | 15 | 15 |
| Apoyo | Inter 400 | 13 | 13 |
| Botón | Inter 700 | 17 | 16 |

Con la escala de fuente del sistema al 130 %, el número de puntos no se recorta: la tarjeta crece en alto y el número baja de 148 a 120.

### 3.3 Espaciado y radios

Rejilla de 4: 4, 8, 12, 16, 20, 24, 32 y 40. Margen de pantalla de 20 dp en el teléfono y 40 px en la central.

| Elemento | Radio |
|---|---|
| Tarjeta de cristal grande | 28 dp (teléfono) · 24 px (central) |
| Botón circular − y + | circular |
| Botón principal y secundario | cápsula |
| Píldora de estado | cápsula |
| Campo de texto | 14 |
| Medidor y progreso | cápsula |

### 3.4 Elevación

Una sola sombra, siempre gris neutra:
- **Tarjeta:** `0 10 28 rgba(15,23,42,0.08)`. En Android, `cardElevation` de 6 dp con sombra ambiental neutra.
- **Botón:** `0 4 10 rgba(15,23,42,0.10)`.

Nunca sombras de color.

## 4. El cristal

### 4.1 Receta

Capas, de abajo hacia arriba:

1. Desenfoque del fondo, si la plataforma lo permite (§4.3).
2. Relleno blanco: 0.58 de opacidad con desenfoque, 0.88 sin él.
3. Borde especular de 1 px, de `rgba(255,255,255,0.95)` arriba a la izquierda a `rgba(255,255,255,0.35)` abajo a la derecha.
4. Contorno de definición de 1 px: `rgba(15,23,42,0.05)`.
5. Brillo interior: línea superior de 1 px `rgba(255,255,255,0.9)`.
6. Sombra de tarjeta (§3.4).

| Variante | Relleno (con / sin desenfoque) | Uso |
|---|---|---|
| Cristal | 0.58 / 0.88 | Tarjetas |
| Cristal fuerte | 0.80 / 0.94 | Píldoras, botones secundarios, campos |
| Presionado | 0.72 / 0.90 y escala 0.97 | Al tocar |

### 4.2 Fondo

- Base blanca `#FFFFFF`.
- Franjas diagonales a 115°: 14 de ancho, 16 de separación, color `rgba(20,40,75,0.06)`. Se desvanecen hacia el blanco en la parte inferior.
- Detrás de cada tarjeta de esquina, las mismas franjas en su color al 10 % (`rgba(198,40,57,0.10)` y `rgba(31,78,156,0.10)`), con bordes suaves.
- Sin degradados radiales ni manchas de luz: las franjas son textura, no iluminación.
- **Android:** un `Drawable` propio que se dibuja una vez en un `Bitmap` y se reutiliza.
- **Central:** un `ImagePattern` o un `Canvas` cacheado detrás del contenido.

### 4.3 Desenfoque y modo sencillo

- **El modo sencillo es la base, no un modo degradado.** Relleno alto (0.88), sin desenfoque. Toda pantalla debe verse bien así.
- **Android:** el desenfoque real (`RenderEffect.createBlurEffect`, radio 12) solo existe en API 31+, y `minSdk` es 26.
  - En API 31+ se usa sobre la capa de fondo, y como mucho en 3 superficies a la vez: las dos tarjetas y la píldora de juez, más la hoja de confirmación cuando aparece.
  - "Modo sencillo" en ajustes lo apaga.
- **Central:** JavaFX no difumina lo que hay detrás de un nodo. Se usa siempre el modo sencillo sobre el fondo de franjas. No se simula el cristal con `GaussianBlur` sobre el propio contenido, porque lo ensucia.

## 5. Componentes

| Componente | Juez (Android) | Central (JavaFX) |
|---|---|---|
| Superficie de cristal | `MaterialCardView` con fondo `glass_card.xml` (relleno + borde + contorno) | `.glass` en `app.css` sobre `Region`/`VBox` |
| Píldora | `TextView` con `pill.xml` | `.pill` |
| Botón principal / secundario | `MaterialButton` con estilo `Widget.Sigebox.Button.Primary` / `.Secondary` | `.button.primary` / `.button.secondary` |
| Punto de conexión | `View` de 8 dp, `ShapeDrawable` óvalo | `Circle` de 10 px, clase `.dot` / `.dot.connected` |
| Botón − / + | `MaterialButton` circular de 76 dp, ícono de trazo | — |
| Medidor de cápsula | `LinearProgressIndicator` de 8 dp con esquinas redondas | `.meter` |
| Progreso de rounds | Fila de segmentos de 6 dp | Igual, en la barra de pasos |
| Aviso temporal | Píldora arriba, 4 s | Píldora arriba, 4 s |

### 5.1 Punto de conexión

- 8 dp en la píldora del juez, 10 px en las tarjetas de la central.
- Conectado: `connected`. Sin conexión: `disconnected`. Transición de 200 ms, sin parpadeo.
- No lleva texto al lado, salvo en la pantalla de unión, donde el juez aún no tiene lugar: ahí dice "Sin conexión".
- La descripción accesible lee "Juez 2, conectado" o "Juez 2, sin conexión".

### 5.2 Botones − y +

- 76 dp de diámetro (mínimo 64), ícono de trazo redondeado de 2.6 y 30 dp.
- **−:** cristal fuerte con ícono en `ink`.
- **+:** relleno del color de la esquina con ícono blanco.
- Presionado: escala 0.97. Deshabilitado: opacidad 0.35. El − se deshabilita en 0 y el + en 10.
- Mantener presionado **no** repite.
- Descripción accesible: "Restar un punto a la esquina roja, valor actual 10".

### 5.3 Tarjeta de esquina (juez)

- Fila superior: píldora de la esquina (color sólido, texto blanco, "Roja" o "Azul") y, a la derecha, el nombre del boxeador del `JOIN_APPROVED` (`redCorner` / `blueCorner`).
- Fila central: −, número, +.
- Medidor de cápsula: relleno del color de la esquina al 85 %, proporcional a `valor / 10`. Es decorativo y no se anuncia.
- **Sin "Mi total".**

### 5.4 Botones

- 64 dp de alto en el teléfono y 56 px en la central, en forma de cápsula.
- **Principal:** `navy` con texto blanco.
- **Secundario:** cristal fuerte con borde `rgba(15,23,42,0.14)` y texto en `ink`.
- Deshabilitado: opacidad 0.4.
- Foco visible: anillo de 2 en `navy`.
- Las acciones destructivas (cerrar round con faltantes, terminar, cerrar pelea) nunca son el botón por defecto y van separadas al menos 24 de las demás.

### 5.5 Píldora de estado

Siempre neutra: cristal fuerte con texto en `ink_secondary`. Textos: "Round abierto", "Round cerrado", "Round enviado", "Esperando round", "Esperando aprobación".

### 5.6 Hoja de confirmación (juez)

Hoja inferior de cristal (`BottomSheetDialog`) que aparece al tocar "Enviar round N":
- Título: "¿Enviar este round?".
- El resultado en grande: "10 – 9", con cada número en el color de su esquina.
- Botones: **Enviar** (principal) y **Revisar** (secundario).

Como cada round empieza en 10–10, esta hoja es la protección contra envíos por descuido.

## 6. Pantallas del juez (Android)

- **Referencia:** 390 × 844 dp en vertical. En pantallas de 720 dp o más de ancho, las dos tarjetas de esquina van lado a lado (Roja a la izquierda).
- **Bordes del sistema:** targetSdk 37 dibuja de borde a borde. Cada pantalla aplica los insets de las barras del sistema a su raíz (`ViewCompat.setOnApplyWindowInsetsListener`), y nada queda bajo la barra de estado ni la de navegación.
- **Pantalla encendida:** en puntuación, tardío y tarjeta final (`FLAG_KEEP_SCREEN_ON`).
- **Vibración corta** al tocar − o +, al enviar, al quedar aprobado y al abrirse un round. Vibración larga en `ROUND_CLOSED` con `mustSubmit` (AND-006).

### 6.1 Términos (`TermsActivity`)

Marca arriba, título "Antes de empezar" y el texto en una tarjeta de cristal con desplazamiento. Debajo, la casilla "Leí y acepto…" y el botón principal "Aceptar y continuar".

### 6.2 Unirse (`JoinActivity` + `ScanActivity`)

- Barra superior: marca y píldora con punto gris + "Sin conexión".
- Título "Unirme a la pelea"; apoyo "Escanea el código de tu lugar en la pantalla del organizador."
- Visor de cámara en una tarjeta de cristal con cuatro esquinas en `navy`.
- Separador "o escribe el código".
- Campos: IP de la central (con `:puerto` opcional), pelea (`fightId` corto), lugar y código de 6 dígitos. El código va en 24 sp, centrado, con 6 de espaciado entre letras (contrato AND-004).
- Botón principal "Unirme".
- Nota: "Primero conéctate al Wi‑Fi de la función."
- Error: "Código incorrecto. Revisa que sea el de tu lugar."

### 6.3 Esperando aprobación (`WaitingActivity`)

Tarjeta de cristal con "Esperando aprobación" y la huella corta del teléfono en grande ("AB12 CD34"), para que el organizador la compare con la de la central.

### 6.4 Puntuar (`ScoreActivity`)

```
┌────────────────────────────────┐
│ [marca] SIGEBOX    ( • Juez 2 )│
│ Round 4 de 10     Round abierto│
│ ▰▰▰▰▱▱▱▱▱▱                     │
│ ┌────────────────────────────┐ │
│ │ (Roja)        M. Hernández │ │
│ │ (−)         10         (+) │ │
│ │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ (Azul)           J. Ortega │ │
│ │ (−)          9         (+) │ │
│ │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░ │ │
│ └────────────────────────────┘ │
│ [        Enviar round 4      ] │
└────────────────────────────────┘
```

- **Esperando round:** las tarjetas al 50 % de opacidad, los botones deshabilitados y la píldora "Esperando round". Al llegar `ROUND_OPENED` se activan en 200 ms con vibración corta.
- **Round enviado:** las tarjetas quedan bloqueadas con los valores enviados. El botón pasa a "Round 4 enviado" (deshabilitado) y abajo se lee "Esperando el siguiente round."
- **Corregir** mientras el round sigue abierto (RJ-05): el botón secundario "Corregir" desbloquea las tarjetas y el envío vuelve a pasar por la hoja de confirmación.
- **Sin conexión:**
  - El punto se vuelve gris y aparece el aviso "Sin conexión. Tus puntos se guardan y se envían al reconectar."
  - El juez puede seguir puntuando. Mientras no llegue el `ACK`, la píldora dice "Enviando…".

### 6.5 Round tardío obligatorio (`LateScoreActivity`)

La misma tarjeta doble, con la píldora "Round 3 cerrado" y el título "Califica el round 3". No tiene botón de salir: es obligatoria.

### 6.6 Tarjeta final y fin (`FinalCardActivity`)

- Título "Pelea terminada".
- La lista de sus propios rounds ("R1 10 – 9"…), **sin sumas**.
- Botón principal "Enviar tarjeta final" y, al confirmarse, "Tarjeta final enviada".

## 7. Pantallas de la central (JavaFX)

- **Ventana:** 1280 × 800 de referencia, mínimo 1100 × 700.
- **Barra superior de 44 px:** marca a la izquierda y los pasos **Pelea · Jueces · En vivo · Resultado** al centro. El paso actual va en `navy` con texto blanco; los demás en cristal fuerte.
- **Navegación:** con teclado, foco visible y un orden de tabulación lógico.
- **Términos:** en el primer uso, la pantalla de términos va antes que todo (`TermsGate`).
- **Columna centrada:** el contenido de cada pantalla va en una columna de hasta 1100 px centrada en la ventana, con títulos y tarjetas centrados y desplazamiento vertical si no cabe.

### 7.1 Pelea y jueces (DSK-005)

- **Columna izquierda (340 px), tarjeta de cristal:**
  - Nombre de la esquina roja y de la azul, cada uno con un círculo de 10 px de su color.
  - Rounds (1–12) y jueces (1–5).
  - El `fightId` corto (≤ 8 caracteres) a la vista.
- **Zona derecha:** una tarjeta por lugar, todas del mismo ancho.
  - **Sin conectar:** punto gris, QR de 168 px sobre blanco con borde fino, código de 6 dígitos en 22 px y "Nuevo código" (secundario).
  - **Esperando aprobación:**
    - Punto gris, nombre del juez y la huella corta del teléfono ("AB12 CD34"), en Inter 600.
    - Botones "Aprobar" (principal) y "Rechazar" (secundario).
    - El QR se oculta.
  - **Conectado:** punto verde, "Juez 2 · María López".
  - **Sin conexión en la pelea:** punto gris y "Reconectando…".
- **Pie:**
  - "Dirección para los jueces: 192.168.43.1:8765" (IP detectada).
  - Botón principal "Iniciar pelea". Se habilita cuando todos los lugares están aprobados; si no, dice "Falta que se conecten 2 jueces."
- **Error al arrancar el servidor:** "No se pudo iniciar la conexión local. Revisa que la computadora esté en la red de la función."

### 7.2 En vivo (DSK-006, DSK-012, DSK-015, DSK-017)

```
┌──────────────────────────────────────────────────────────────┐
│ [marca] SIGEBOX     Pelea · Jueces · En vivo · Resultado     │
│ ┌──────────────────────────────────────┐ ┌────────────────┐ │
│ │ Tarjetas                             │ │ Round 3 de 4   │ │
│ │ Round  Juez 1 •  Juez 2 •  Juez 3 •  │ │ Round abierto  │ │
│ │ 1      Enviado   Enviado   Enviado   │ │ Enviaron 2 de 3│ │
│ │ 2      Enviado   Tardío    Enviado   │ │ [Cerrar round] │ │
│ │ 3      Enviado   Pendiente Enviado   │ │ [Corregir]     │ │
│ │ 4      —         —         —         │ ├────────────────┤ │
│ └──────────────────────────────────────┘ │ Resumen parcial│ │
│                                          │ [Ver resumen]  │ │
│                                          └────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

- **Sin puntos, totales ni ganador** durante la pelea (`PROJECT.md` principio 3).
- **Tabla de estados:**
  - Por juez y round: "Pendiente", "Enviado", "Tardío" o "Corregido" en gris; "—" si no hay tarjeta o el round no se ha abierto.
  - Encabezado por juez con su punto de conexión.
- **Round abierto:** fila con fondo `rgba(20,40,75,0.05)`.
- **Rounds futuros:** si hay muchos, se agrupan en una fila "Pendientes".
- **Control del round:**
  - "Round 3 de 4", la píldora y "Enviaron 2 de 3 jueces".
  - Botón principal "Cerrar round 3" o "Abrir round 4", y "Cerrar pelea" después del último.
  - Botón secundario "Corregir un round".
  - Cerrar con faltantes pide confirmación y escribir **AVANZAR**.
- **Ver resumen parcial:** botón secundario discreto. Pide confirmación, muestra el total de cada juez en los rounds cerrados durante 15 s y cada vista queda en la bitácora.
- **Atajo:** `Ctrl+Enter` abre o cierra el round según el estado.
- **Solo en la pantalla del operador:** nada de esto se envía a los teléfonos.

### 7.3 Terminación, corrección y bitácora (DSK-007, DSK-009, DSK-011)

- **Formularios:** en tarjetas de cristal, con campos de 44 px de alto y radio 14, y etiqueta encima en 13 px `ink_secondary`.
- **Terminación:** se confirma escribiendo **TERMINAR**.
- **Corrección y captura desde papel:** exigen motivo.
- **Bitácora:** tabla filtrable por pelea y tipo, con la cabecera en Inter 600 y filas de 36 px.

### 7.4 Resultado (DSK-008)

- **Pantalla completa, solo en la laptop del operador.**
- **Tarjeta principal:**
  - "Resultado final".
  - El ganador en Barlow Condensed de 112 px, en el color numeral de su esquina.
  - La frase de `ResultService.headline` ("Gana Roja por decisión dividida 2–1").
- **Una tarjeta por juez:** su total en grande ("39 – 37") y una píldora neutra "Roja", "Azul" o "Empate".
- **Acciones:** "Exportar PDF" (principal), "Exportar CSV" y "Cerrar" (secundarios).
- **Espacio de logotipo:** 160 × 80 arriba a la izquierda.

## 8. Textos

Frase en minúscula con mayúscula inicial, verbos claros, sin signos de exclamación ni disculpas en los errores. Una acción conserva el mismo nombre en todo el flujo.

| Clave | Texto |
|---|---|
| Estados de tarjeta | Pendiente · Enviando… · Enviado · Tardío · Corregido · Capturado |
| Conexión | Conectado (solo lectura accesible) · Sin conexión · Reconectando… · Esperando aprobación |
| Juez | Unirme a la pelea · o escribe el código · Unirme · Round 4 de 10 · Round abierto · Esperando round · Enviar round 4 · ¿Enviar este round? · Enviar · Revisar · Corregir · Round 4 enviado · Esperando el siguiente round. · Califica el round 3 · Pelea terminada · Enviar tarjeta final |
| Central | Pelea · Jueces · En vivo · Resultado · Aprobar · Rechazar · Nuevo código · Iniciar pelea · Tarjetas · Ver resumen parcial · Abrir round 5 · Cerrar round 4 · Cerrar pelea · Corregir un round · Resultado final · Exportar PDF · Exportar CSV |
| Confirmaciones fuertes | Escribe AVANZAR para cerrar el round sin todas las calificaciones. · Escribe TERMINAR para registrar la terminación. |
| Errores | Código incorrecto. Revisa que sea el de tu lugar. · Sin conexión. Tus puntos se guardan y se envían al reconectar. · No se pudo iniciar la conexión local. Revisa que la computadora esté en la red de la función. · Falta que se conecten 2 jueces. |

Las palabras de control AVANZAR y TERMINAR son la única excepción a las minúsculas: se escriben tal cual.

## 9. Movimiento

- **Duración:** el movimiento responde a una acción y dura de 100 a 200 ms, con salida suave y sin rebote. No hay animaciones de entrada decorativas.
- **Número que cambia:** 120 ms de desplazamiento vertical sutil.
- **Movimiento reducido:** se respeta el ajuste del sistema (`ANIMATOR_DURATION_SCALE` = 0 en Android); solo cambian opacidades.

## 10. Accesibilidad

- Contraste de texto ≥ 4.5:1 y de números grandes ≥ 3:1.
- La esquina siempre con texto; la conexión, con descripción accesible además del color.
- Zonas táctiles ≥ 48 dp; los botones − y + miden 76.
- Escala de fuente del sistema hasta 130 % sin recortar números.
- En la central, todo se opera con teclado y tiene foco visible.

## 11. Lo que no se hace

- Fondos oscuros, neón, resplandores, degradados luminosos o sombras de color.
- Más colores de estado que el punto verde y el gris.
- Mayúsculas sostenidas (salvo AVANZAR y TERMINAR), emojis o ilustraciones decorativas.
- Íconos o recursos de Apple.
- Una barra de color a un lado de las tarjetas.
- El mismo radio y la misma sombra en todo: hay que respetar la jerarquía de §3.3.
- Mostrar a un juez tarjetas, totales o estados de otro juez, o su propio total acumulado.
- Proyectar o enviar el tablero en vivo fuera de la pantalla del operador.

## 12. Lista de revisión visual

- [ ] Fondo blanco en todas las pantallas, con la textura de franjas.
- [ ] El verde solo aparece como punto de conexión.
- [ ] Sin sombras de color ni resplandores.
- [ ] Rojo y azul solo en esquinas y en sus botones +.
- [ ] Las tarjetas de cristal se distinguen del fondo en modo sencillo.
- [ ] En el teléfono, lo primero que se ve es el número de puntos, y nada queda bajo las barras del sistema.
- [ ] Los botones − y + miden 76 dp y se alcanzan con el pulgar.
- [ ] Todo el texto está en español, en minúscula con mayúscula inicial.
- [ ] Un juez nunca ve tarjetas de otro ni su total.
- [ ] La hoja de confirmación aparece antes de enviar un round.
- [ ] El estado sin conexión se entiende con el punto y con el aviso.
- [ ] La central se opera con teclado y el tablero en vivo solo está en su pantalla.
