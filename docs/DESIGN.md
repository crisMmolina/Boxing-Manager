# DESIGN · Guía visual de SIGEBOX (v3)

Guía visual y de experiencia de las dos apps: la del juez (Android, `app/`) y la central (JavaFX, `BOXCENTRAL/`). Es la fuente de verdad visual para AND-012 y DSK-018, y reemplaza a la v2 (DSN-002).

- **Pedido:** cris (2026-10-26) pidió un rediseño total que siga las tendencias actuales, use los colores de la Ciudad de México y haga que SIGEBOX se sienta distinta.
- **Spec:** `docs/specs/001-sigebox/spec.md`. Manda `PROJECT.md` principio 3: nadie ve totales ni ganador durante la pelea, salvo "Ver resumen parcial" (registrado en bitácora). Un juez solo ve lo suyo.
- **Idioma:** español (México). **Sin internet:** fuentes e íconos van empaquetados; nada se descarga en ejecución.
- **Medición:** todos los contrastes y simulaciones de daltonismo salen de `docs/design/contraste.py`, que escribe `docs/design/contraste.md`. Si cambias un color, cambia el script y vuelve a correrlo.

## Fuente de la paleta

**"Manual de identidad institucional · Gobierno de la Ciudad de México 2024-2030"**, PDF oficial en https://cdmxassets.s3.us-east-1.amazonaws.com/media/Archivos+pdf/MANUAL+DE+IDENTIDAD+2024-2030+OFICIAL.pdf. Según sus metadatos se creó el 2024-11-08. Lo consulté el 2026-10-26 y leí los valores en el texto del PDF, págs. 16 y 25.

| Color del manual | Pantone | Hex | Uso en SIGEBOX |
|---|---|---|---|
| Guinda | 7420 C | `#9F2241` | **Solo** la esquina roja (§3.1) |
| Dorado | 465 C | `#BC955C` | Acento en el tema oscuro; base del acento del tema claro |
| Gris | 425 C | `#55585A` | Texto de apoyo en el tema claro |
| Azul (complementario) | 285 C | `#266CB4` | La esquina azul |
| Verde (complementario) | 356 C | `#027A35` | Punto de "conectado" en el tema claro |

- **Colores derivados.** Los tonos que el manual no trae se obtienen subiendo o bajando la luminosidad del color oficial, con el mismo tono: Dorado profundo `#7A5A2A`, guinda claro `#E58A9D`, azul claro `#8CB8EE` y verde claro `#4CC97A`. Se derivan para cumplir AA en su fondo.
- **Errores del PDF.** El manual da un hex para el Pantone 7548 C (`a27ab5`) que no corresponde a su RGB. SIGEBOX no usa ese color.

**Solo se toman los colores.** No se usa nada de lo siguiente: el escudo, los logotipos, el lema «Capital de la transformación», la tipografía Cabin del manual, la composición guinda con dorado de los materiales del gobierno, ni cualquier texto que sugiera que la app es oficial. SIGEBOX no pertenece al gobierno, y su marca es solo la palabra SIGEBOX.

## 0. Qué cambió respecto a la v2

| v2 | v3 |
|---|---|
| Solo tema claro, «sin fondos oscuros» | Tema claro **y** tema oscuro de arena (poca luz). La v3 revierte la regla de la v2 |
| Cristal translúcido con desenfoque | Superficies tonales sólidas al estilo Material 3 (§2). El desenfoque no existe antes de API 31 ni en JavaFX, y en la arena el cristal se lee mal |
| Navy `#14284B` como acción principal | Acción principal neutra: tinta en claro y crema en oscuro. Así se evita un bloque de color que compita con las esquinas o que recuerde la imagen del gobierno |
| «No hay ámbar ni amarillo» | El dorado de la CDMX es el único acento. La v3 revierte esa regla de la v2 |
| Rojo `#C62839` y azul `#1F4E9C` | Guinda CDMX `#9F2241` y azul CDMX `#266CB4`, con tonos claros en el tema oscuro |
| Punto verde como único estado | Punto lleno (conectado) frente a un anillo vacío (sin conexión): se distingue por la forma, no solo por el color |
| Tabla de estados en vivo en una sola tarjeta | Central en rejilla tipo bento (§7) |
| Botones − y + de 76 dp | Igual, con forma Material 3 Expressive: se aplanan al tocar, con movimiento de resorte (§4) |

Se conserva de la v2:
- Barlow Condensed recta, sin cursiva, pedido de zoe del 2026-10-25, e Inter.
- Los botones − y + con cada round empezando en 10 y la hoja de confirmación.
- Los textos, AVANZAR / TERMINAR, la huella corta del teléfono y el "Ver resumen parcial" de 15 s.

## 1. Principios

1. **Un protagonista por pantalla.** En el juez es el número de puntos; en la central, el estado del round.
2. **El color significa algo, y nunca solo el color.**
   - Guinda y azul son las esquinas, siempre con su etiqueta "Roja" o "Azul".
   - El dorado es el acento: foco, paso actual, progreso y selección.
   - El punto de conexión cambia de color **y** de forma.
   - No hay más colores.
3. **Guinda solo como esquina.** Nunca va en la marca, encabezados, fondos ni acentos. Así la esquina roja no se confunde con la «marca», y la app no reproduce la pareja guinda y dorado del gobierno.
4. **Nada parece oficial.** Ver «Fuente de la paleta».
5. **Pensada para la arena.** Gente de pie, con prisa y luz irregular: el tema oscuro para poca luz, números enormes, botones grandes y cero ambigüedad.
6. **Un juez solo ve lo suyo.** Ninguna pantalla del teléfono muestra tarjetas, totales, nombres ni estados de otros jueces, ni su propio total acumulado.
7. **Sin totales durante la pelea.** La central muestra estados, no puntos, hasta el resultado (salvo "Ver resumen parcial"). El tablero solo vive en la pantalla del operador.

## 2. Lenguaje visual: cantera y noche

- **Tema claro, «cantera».** Fondo crema cálido (`#F7F4EF`), como la piedra de los edificios del centro, con tarjetas blancas.
- **Tema oscuro, «noche de arena».** Fondo casi negro cálido (`#14120F`), con tarjetas en tonos de carbón.
- **En los dos temas:**
  - Las superficies son sólidas y se separan por tono, no por sombras de color ni transparencias.
  - Formas grandes y redondeadas (Material 3 Expressive), y números protagonistas en Barlow Condensed.
  - El dorado aparece en detalles finos: el anillo de foco, el segmento del round actual, la línea bajo el paso actual. Nunca en bloques grandes.
- **El tema** sigue al sistema (`DayNight` en Android; en la central, un botón de alternar en la barra superior). El juez puede fijarlo en ajustes.

## 3. Tokens

Ninguna pantalla usa valores sueltos.
- **Android:** `res/values/colors.xml` y `res/values-night/colors.xml`, `dimens.xml` y `themes.xml`, con tema base `Theme.Material3Expressive.DayNight`.
- **Central:** variables al inicio de `app.css` (`-sg-…`), con dos hojas, `light.css` y `dark.css`, que solo redefinen los colores.

### 3.1 Color

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `bg` | `#F7F4EF` | `#14120F` | Fondo de pantalla |
| `surface` | `#FFFFFF` | `#1F1C18` | Tarjetas |
| `surface_2` | `#EFEAE2` | `#2A2621` | Tarjeta elevada, campos, hoja inferior, fila del round abierto |
| `ink` | `#1C1A17` | `#F3EFE8` | Texto principal e íconos |
| `ink_2` | `#55585A` (Gris CDMX) | `#B8B1A6` | Texto de apoyo |
| `primary` / `on_primary` | `#1C1A17` / `#FFFFFF` | `#F3EFE8` / `#1C1A17` | Botón principal |
| `accent` | `#7A5A2A` (Dorado profundo) | `#BC955C` (Dorado CDMX) | Foco, paso actual, progreso, selección |
| `red` / `on_red` | `#9F2241` / `#FFFFFF` | `#E58A9D` / `#1C1A17` | Relleno de la esquina roja (píldora, botón +) |
| `red_numeral` | `#9F2241` | `#E58A9D` | Números de la esquina roja |
| `blue` / `on_blue` | `#266CB4` / `#FFFFFF` | `#8CB8EE` / `#1C1A17` | Relleno de la esquina azul |
| `blue_numeral` | `#266CB4` | `#8CB8EE` | Números de la esquina azul |
| `connected` | `#027A35` | `#4CC97A` | Punto **lleno** de conectado |
| `disconnected` | `#55585A` | `#B8B1A6` | **Anillo** vacío de sin conexión |
| `line` | `#D9D2C7` | `#3A352E` | Divisores, decorativos |

**Contraste medido** (`docs/design/contraste.md`): los 21 pares texto/fondo y componente/fondo pasan AA en los dos temas.

| Par | Claro | Oscuro |
|---|---|---|
| `ink` / `bg` | 15.83 | 16.31 |
| `ink_2` / `bg` · `ink_2` / `surface` | 6.53 · 7.17 | 8.79 · 7.98 |
| `on_primary` / `primary` | 17.36 | 15.15 |
| `on_red` / `red` · `on_blue` / `blue` | 7.56 · 5.40 | 6.99 · 8.44 |
| `red_numeral` / `surface` · `blue_numeral` / `surface` | 7.56 · 5.40 | 6.83 · 8.25 |
| `accent` / `bg` · `accent` / `surface` | 5.76 · 6.32 | 6.76 · 6.13 |
| `connected` / `surface` (UI, mínimo 3) | 5.47 | 8.03 |

**Esquinas y daltonismo** (ΔE76 entre los colores simulados con Machado 2009, severidad 1.0; ≥ 20 se distingue y < 10 se confunde):

| Par | Normal | Deuteranopía | Protanopía | Tritanopía |
|---|---|---|---|---|
| Roja / Azul, claro | 75.5 | 67.1 | 47.9 | 95.8 |
| Roja / Azul, oscuro | 53.0 | 42.6 | 30.4 | 74.8 |
| Roja / dorado, claro | 48.3 | **14.0** | 31.8 | 43.5 |
| Roja / verde, claro | 98.6 | **8.0** | 38.6 | 97.4 |
| Azul / verde, claro | 88.8 | 74.2 | 78.3 | **15.3** |

Las esquinas se distinguen entre sí con cualquier tipo de daltonismo. Los tres pares débiles (en negritas) se resuelven con reglas, no con tonos:
- El dorado nunca aparece dentro de una tarjeta de esquina ni junto a un relleno de esquina.
- El punto de conexión se distingue por la forma: lleno o anillo.
- Las esquinas siempre llevan su etiqueta "Roja" / "Azul" y su posición fija: Roja arriba o a la izquierda, Azul abajo o a la derecha.

### 3.2 Tipografía

- **Barlow Condensed**, recta (sin cursiva). Se usa en 800 para números y títulos, y en 700 para la marca.
- **Inter** 400, 500, 600 y 700 para todo lo demás.
- Las dos son OFL, ya van empaquetadas con su licencia (`app/src/main/res/font/` y `BOXCENTRAL/.../fonts/`) y se conservan de la v2.
- Cifras tabulares (`tnum`) en todo número que cambia.
- Minúsculas con mayúscula inicial, salvo AVANZAR y TERMINAR. Nada en cursiva.

| Rol | Fuente | Teléfono (sp) | Central (px) |
|---|---|---|---|
| Número de puntos | Barlow Condensed 800 | 128 (160 en ≥ 400 dp) | 120 en resultado |
| Estado grande (bento) | Barlow Condensed 800 | — | 56 |
| Título de pantalla | Barlow Condensed 800 | 40 | 34 |
| Título de tarjeta | Barlow Condensed 800 | 26 | 24 |
| Cuerpo | Inter 400–500 | 16 | 15 |
| Apoyo | Inter 400 | 13 | 13 |
| Botón | Inter 700 | 17 | 16 |

Con la fuente del sistema al 130 %, el número no se recorta: la tarjeta crece y el número baja a 112.

### 3.3 Espaciado y radios

Rejilla de 4: 4, 8, 12, 16, 20, 24, 32, 40 y 56. Margen de 20 dp en el teléfono y de 32 px en la central; separación de 16 entre bloques bento.

| Elemento | Radio (token M3) |
|---|---|
| Tarjeta de esquina, bloque bento | 28 (`Corner.ExtraLarge`) |
| Hoja inferior | 32 arriba (`Corner.ExtraLargeIncreased`) |
| Botones − y + | círculo en reposo, se aplana al tocar (§4) |
| Botón principal y secundario | cápsula (`Corner.Full`) |
| Campo de texto | 16 (`Corner.Large`) |
| Píldora | cápsula |

### 3.4 Elevación

- **Claro:** una sola sombra neutra para las tarjetas, `0 6 20 rgba(28,26,23,0.08)`. En Android, `cardElevation` de 2 dp.
- **Oscuro:** sin sombras, porque no se leen sobre negro. La elevación es tonal: `bg` → `surface` → `surface_2`.
- Nunca sombras de color, resplandores ni neón.

### 3.5 Movimiento

- **Android:** resortes Material 3, verificados en `material` 1.14.0. Se usan `Motion.Material3.Spring.Standard.Fast.Spatial` para posición y tamaño, y `...Fast.Effects` para color y opacidad. El estilo Expressive (rebote leve) solo en − y +.
- **Central:** transiciones de 150 a 200 ms con salida suave (`Interpolator.EASE_OUT`).
- **Número que cambia:** desplazamiento vertical de 120 ms.
- **Movimiento reducido** (`ANIMATOR_DURATION_SCALE` = 0, o el ajuste de la central): solo cambian opacidades.
- **Háptica:** ver §4.5.

## 4. Tendencias: qué se adopta y por qué

| # | Tendencia | Se adopta | No se adopta | Por qué sirve en la arena |
|---|---|---|---|---|
| 4.1 | **Tipografía expresiva y numerales enormes** | Número de 128–160 sp en Barlow Condensed 800 con cifras tabulares; estados grandes en la central | Fuentes variables descargadas o display decorativas | El juez lee su número de un vistazo, de pie y con luz mala. La fuente condensada deja cifras altas en poco ancho |
| 4.2 | **Material 3 Expressive** (Android) | Tema `Theme.Material3Expressive.DayNight`; formas grandes (`Corner.ExtraLarge`); `MaterialButtonGroup` para − y +, que se aplanan al presionar; `LoadingIndicator` para "Enviando…"; resortes `Motion.Material3.Spring.*`. Todo existe en `material` 1.14.0 (verificado en el AAR) | `FloatingToolbarLayout` y `MaterialSplitButton`, porque no hay acciones que agrupar; Compose, porque la app es de vistas XML | Formas grandes = blancos grandes para el pulgar. El aplanado del botón confirma el toque sin mirar |
| 4.3 | **Layout bento** (central) | Rejilla de bloques de distinto tamaño: round actual (grande), estado por juez, acciones y resumen parcial (§7.2) | Bloques decorativos o métricas de vanidad | El operador ve de un golpe qué falta y qué sigue, sin leer una tabla larga |
| 4.4 | **Modo oscuro** | Tema oscuro completo, con su propia medición AA | Oscuro forzado | En las arenas de box la luz es baja o de escenario. El oscuro no deslumbra al juez junto al ring |
| 4.5 | **Microinteracciones con háptica** | Vibración corta al tocar − o +, al enviar y al abrirse un round; larga en `ROUND_CLOSED` con `mustSubmit`; `HapticFeedbackConstants` en vez de patrones propios | Sonidos (no se oyen y molestan) | El juez confirma sin quitar los ojos del ring |
| 4.6 | **Superficies tonales en vez de cristal** | Capas `bg`/`surface`/`surface_2` | Cristal con desenfoque (glassmorphism) | El desenfoque no existe antes de API 31 ni en JavaFX, y baja el contraste |
| 4.7 | **Color de acento único** | El dorado CDMX solo en detalles | Degradados y varios acentos | Identidad propia sin ruido ni bloques guinda y dorado oficiales |

## 5. Componentes

| Componente | Juez (Android) | Central (JavaFX) |
|---|---|---|
| Tarjeta | `MaterialCardView`, `surface`, radio 28 | `.card` en `app.css` |
| Botón principal / secundario | `MaterialButton` con `Widget.Sigebox.Button.Primary` / `.Secondary` | `.button.primary` / `.button.secondary` |
| Botones − y + | `MaterialButtonGroup` con dos `MaterialButton` circulares de 76 dp | — |
| Píldora de esquina | `TextView` con fondo `red` / `blue` y texto `on_red` / `on_blue` | `.pill.red` / `.pill.blue` |
| Píldora de estado | Neutra: `surface_2` con texto `ink_2` | `.pill` |
| Punto de conexión | `View` de 10 dp: óvalo lleno `connected` o anillo de 2 dp `disconnected` | `Circle` de 10 px: lleno o con `stroke` |
| Progreso de rounds | Segmentos de 6 dp: completados en `ink_2`, actual en `accent`, pendientes en `line` | Igual en la barra de pasos |
| Enviando | `LoadingIndicator` pequeño junto a la píldora | `ProgressIndicator` indeterminado de 16 px |
| Hoja de confirmación | `BottomSheetDialog`, `surface_2`, radio 32 arriba | Diálogo modal `.sheet` |
| Foco | Anillo de 2 dp en `accent` | Anillo de 2 px en `accent` |

### 5.1 Botones − y +

- 76 dp de diámetro (mínimo 64) e ícono de trazo de 30 dp.
- **−:** relleno `surface_2` con ícono en `ink`. **+:** relleno de la esquina con ícono `on_red` / `on_blue`.
- Al presionar: se aplanan a radio 20 con resorte Expressive y vibración corta. Deshabilitados: opacidad 0.38. El − se deshabilita en 0 y el + en 10.
- Mantener presionado **no** repite.
- Descripción accesible: "Restar un punto a la esquina roja, valor actual 10".

### 5.2 Tarjeta de esquina (juez)

- Arriba: la píldora de la esquina ("Roja" o "Azul") y, a la derecha, el nombre del boxeador (`redCorner` / `blueCorner` de `JOIN_APPROVED`).
- En medio: −, el número en `red_numeral` / `blue_numeral`, +.
- **Sin dorado dentro de la tarjeta. Sin "Mi total".**

## 6. Pantallas del juez (Android)

- **Referencia:** 390 × 844 dp en vertical. Desde 720 dp de ancho, las tarjetas van lado a lado (Roja a la izquierda).
- **Bordes:** de borde a borde, con insets del sistema aplicados a la raíz. Nada queda bajo las barras.
- **Pantalla encendida** en puntuación, tardío y tarjeta final (`FLAG_KEEP_SCREEN_ON`).

### 6.1 Términos (`TermsActivity`)

Marca SIGEBOX en Barlow 700, título "Antes de empezar" y el texto en una tarjeta con desplazamiento. Debajo, la casilla "Leí y acepto…" y el botón principal "Aceptar y continuar".

### 6.2 Unirse (`JoinActivity` + `ScanActivity`)

- Barra superior: marca y píldora con el anillo de sin conexión + "Sin conexión".
- Título "Unirme a la pelea" y apoyo "Escanea el código de tu lugar en la pantalla del organizador."
- Visor de cámara en una tarjeta radio 28, con cuatro esquinas de guía en `accent`.
- "o escribe el código", campos IP, pelea, lugar y el código de 6 dígitos en 24 sp con espaciado amplio, y el botón principal "Unirme".
- Nota: "Primero conéctate al Wi‑Fi de la función." Error: "Código incorrecto. Revisa que sea el de tu lugar."

### 6.3 Esperando aprobación (`WaitingActivity`)

Tarjeta con `LoadingIndicator`, "Esperando aprobación" y la huella corta del teléfono en Barlow 800 de 40 sp ("AB12 CD34"), para que el organizador la compare con la de la central.

### 6.4 Puntuar (`ScoreActivity`)

```
┌────────────────────────────────┐
│ SIGEBOX               ( ● Juez 2 )
│ Round 4 de 10      Round abierto│
│ ▬▬▬▬━─────────  (actual en dorado)
│ ┌────────────────────────────┐ │
│ │ (Roja)        M. Hernández │ │
│ │ (−)         10         (+) │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ (Azul)           J. Ortega │ │
│ │ (−)          9         (+) │ │
│ └────────────────────────────┘ │
│ [        Enviar round 4      ] │  ← primary: tinta / crema
└────────────────────────────────┘
```

- **Esperando round:** las tarjetas al 50 %, los botones deshabilitados y la píldora "Esperando round". `ROUND_OPENED` las activa con resorte y vibración corta.
- **Enviar:** la hoja "¿Enviar este round?" con "10 – 9" en los colores numerales, y los botones **Enviar** y **Revisar**.
- **Round enviado:** las tarjetas se bloquean, el botón dice "Round 4 enviado" y abajo se lee "Esperando el siguiente round." Con el round abierto, "Corregir" (secundario) las desbloquea (RJ-05).
- **Sin conexión:** el punto pasa a anillo y aparece el aviso "Sin conexión. Tus puntos se guardan y se envían al reconectar." Mientras no llegue el `ACK`, se ve el `LoadingIndicator` y "Enviando…".

### 6.5 Round tardío obligatorio (`LateScoreActivity`)

La misma tarjeta doble, con la píldora "Round 3 cerrado" y el título "Califica el round 3". No tiene salida. Llega con vibración larga.

### 6.6 Tarjeta final (`FinalCardActivity`)

"Pelea terminada", la lista de sus propios rounds ("R1 10 – 9"…) **sin sumas**, y el botón principal "Enviar tarjeta final", que pasa a "Tarjeta final enviada". Si la central la rechaza, el botón vuelve a habilitarse con el aviso "No se pudo enviar. Intenta de nuevo."

## 7. Pantallas de la central (JavaFX)

- **Ventana:** 1280 × 800 de referencia, mínimo 1100 × 700.
- **Barra superior de 48 px:** a la izquierda la marca SIGEBOX; al centro los pasos **Pelea · Jueces · En vivo · Resultado**, con el paso actual en `ink` y una línea de 2 px en `accent` debajo; a la derecha el botón de tema claro u oscuro.
- **Términos** en el primer uso (`TermsGate`). Todo se opera con teclado y con el foco visible.

### 7.1 Crear pelea y aprobación (DSK-005)

Rejilla bento de dos columnas:
- **Bloque «Pelea»** (1 columna): las esquinas roja y azul, cada una con su píldora "Roja" / "Azul"; rounds (1–12), jueces (1–5) y el `fightId` corto.
- **Bloques «Lugar N»** (rejilla de 2 o 3 por fila):
  - **Sin conectar:** anillo, QR de 168 px sobre blanco (también en oscuro, para que se pueda escanear), código de 6 dígitos y "Nuevo código".
  - **Esperando aprobación:** nombre del juez y huella corta en Barlow 800 de 28 px, con los botones "Aprobar" (principal) y "Rechazar" (secundario).
  - **Conectado:** punto lleno y "Juez 2 · María López".
- **Pie:** "Dirección para los jueces: 192.168.43.1:8765" y "Iniciar pelea", que se habilita con todos aprobados; si no, dice "Falta que se conecten 2 jueces."

### 7.2 En vivo, sin totales (DSK-006, DSK-017)

```
┌───────────────────────────────┬──────────────────┐
│  ROUND 3 de 4                 │  Acciones        │
│  Round abierto                │  [Cerrar round 3]│
│  Enviaron 2 de 3              │  [Corregir]      │
│  (bloque grande, Barlow 56)   │                  │
├──────────┬──────────┬─────────┼──────────────────┤
│ ● Juez 1 │ ○ Juez 2 │ ● Juez 3│  Resumen parcial │
│ R3 Envió │ R3 Falta │ R3 Envió│  [Ver resumen]   │
│ R1–R2 ✓  │ R2 Tardío│ R1–R2 ✓ │  (discreto)      │
└──────────┴──────────┴─────────┴──────────────────┘
```

- **Sin puntos, totales ni ganador** (principio 3). Cada bloque de juez muestra su punto o anillo y los **estados** de sus rounds ("Enviado", "Pendiente", "Tardío", "Corregido"), en `ink_2`.
- **Bloque grande:** el round actual, su estado y "Enviaron 2 de 3". El segmento del round actual va en `accent`.
- **Acciones:** "Cerrar round 3" / "Abrir round 4" (principal) y "Cerrar pelea" después del último. Cerrar con faltantes pide escribir **AVANZAR**. `Ctrl+Enter` abre o cierra el round.
- **Ver resumen parcial:** un botón secundario discreto. Pide confirmación, muestra el total por juez de los rounds cerrados durante 15 s y queda en la bitácora.
- Nada de esto viaja a los teléfonos ni se proyecta.

### 7.3 Terminación, corrección y bitácora (DSK-007, DSK-009, DSK-011)

- **Formularios:** en bloques con campos de 44 px, radio 16 y la etiqueta arriba en `ink_2`.
- **Terminación:** pide motivo, esquina ganadora, round y tiempo, y se confirma escribiendo **TERMINAR**.
- **Corrección y captura desde papel:** exigen motivo.
- **Bitácora:** tabla filtrable, con la cabecera en Inter 600 y filas de 36 px.

### 7.4 Resultado (DSK-008)

- **Pantalla completa, solo en la laptop del operador.**
- **Bloque grande:** "Resultado final", el ganador en Barlow 800 de 120 px en el color numeral de su esquina con su píldora, y la frase de `ResultService.headline` ("Gana Roja por decisión dividida 2–1").
- **Un bloque por juez** con su total ("39 – 37") y su fallo.
- "Exportar PDF" (principal), "Exportar CSV" y "Cerrar" (secundarios).
- Sin logotipos de terceros ni elementos oficiales.

## 8. Textos

Sin cambios respecto a la v2: frase en minúscula con mayúscula inicial, verbos claros, sin signos de exclamación ni disculpas. Una acción conserva su nombre en todo el flujo.

| Clave | Texto |
|---|---|
| Estados | Pendiente · Enviando… · Enviado · Tardío · Corregido · Capturado |
| Conexión | Conectado (solo lectura accesible) · Sin conexión · Reconectando… · Esperando aprobación |
| Juez | Unirme a la pelea · Unirme · Round 4 de 10 · Round abierto · Esperando round · Enviar round 4 · ¿Enviar este round? · Enviar · Revisar · Corregir · Round 4 enviado · Califica el round 3 · Pelea terminada · Enviar tarjeta final |
| Central | Pelea · Jueces · En vivo · Resultado · Aprobar · Rechazar · Nuevo código · Iniciar pelea · Ver resumen parcial · Abrir round 5 · Cerrar round 4 · Cerrar pelea · Resultado final · Exportar PDF · Exportar CSV |
| Confirmaciones | Escribe AVANZAR para cerrar el round sin todas las calificaciones. · Escribe TERMINAR para registrar la terminación. |

## 9. Accesibilidad

- AA medido en los dos temas: ≥ 4.5 en texto y ≥ 3 en componentes (`docs/design/contraste.md`).
- La esquina siempre lleva texto y posición fija; la conexión, forma y descripción accesible ("Juez 2, conectado").
- Zonas táctiles ≥ 48 dp; − y + de 76.
- Fuente del sistema hasta 130 % sin recortar números. Movimiento reducido respetado.
- En la central, todo funciona con teclado y tiene el foco visible.

## 10. Lo que no se hace

- Escudo, logotipos, lema, tipografía Cabin o composición guinda con dorado del gobierno, o cualquier texto que haga parecer la app oficial.
- Guinda fuera de la esquina roja. Dorado en bloques grandes o dentro de una tarjeta de esquina.
- Cristal con desenfoque, neón, resplandores, degradados luminosos o sombras de color.
- Color como única señal (esquina, conexión, estado).
- Mostrar a un juez tarjetas, totales o estados de otro, o su propio total. Mostrar totales o ganador en la central durante la pelea, salvo "Ver resumen parcial".
- Proyectar o enviar el tablero en vivo fuera de la pantalla del operador.
- Mayúsculas sostenidas (salvo AVANZAR y TERMINAR), emojis o ilustraciones decorativas.

## 11. Lista de revisión visual

- [ ] Las dos apps se ven bien en tema claro **y** oscuro, sin valores sueltos fuera de los tokens.
- [ ] `python3 docs/design/contraste.py` termina con "0 pares sin AA" con los colores del código.
- [ ] Guinda solo aparece en la esquina roja; el dorado solo en foco, paso actual, progreso y selección.
- [ ] Nada recuerda la imagen oficial del gobierno: ni escudo, ni lema, ni bloques guinda con dorado.
- [ ] Cada esquina lleva "Roja" / "Azul" y su posición fija; se distinguen con un simulador de daltonismo.
- [ ] El punto de conexión cambia de forma (lleno o anillo), no solo de color.
- [ ] En el teléfono, lo primero que se ve es el número; − y + miden 76 dp y responden con háptica.
- [ ] La hoja de confirmación aparece antes de enviar un round.
- [ ] Un juez nunca ve tarjetas de otro ni su total.
- [ ] La central en vivo muestra estados, no puntos; "Ver resumen parcial" dura 15 s y queda en la bitácora.
- [ ] La central se opera con teclado y el foco es visible en los dos temas.
