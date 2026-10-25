# DESIGN · Guía visual de SIGEBOX

Guía visual de las dos apps: la del juez (Android, `app/`) y la central (JavaFX, `BOXCENTRAL/`). Es el contrato de UI para AND-004/005/006 y DSK-005 a DSK-009.
Fuentes: `IDEA.md` §2–§4 y §9, `docs/specs/001-sigebox/spec.md`. Idioma de la UI: español (México).

## 1. Principios
1. **Sin totales ni ganador durante la pelea.** Ni el juez ni la central muestran sumas, diferencias o "quién va ganando" hasta `FIGHT_CLOSED`. La única excepción es "Ver resumen parcial" en la central (RO-07): pide confirmación, se ve 15 s y queda en la bitácora.
2. **Un juez solo ve lo suyo.** Ninguna pantalla del teléfono muestra tarjetas, nombres ni estados de otros jueces.
3. **Una mano, junto al ring.** Toda acción del juez se hace con el pulgar, en la mitad inferior de la pantalla y con objetivos grandes.
4. **El color nunca va solo.** El rojo y el azul de esquina tienen casi la misma luminancia (contraste entre ellos 1.31:1), así que cada esquina lleva siempre su palabra (ROJA / AZUL) y una posición fija: **Roja a la izquierda, Azul a la derecha**, en las dos apps y en el PDF.
5. **Sin valor por defecto.** Un campo de calificación vacío muestra "—", nunca 10 ni 0.
6. **El estado siempre está visible.** La conexión y el estado de envío (Pendiente, Enviado, Tardío, Corregido) se ven sin tocar nada.
7. **Sin logotipo por ahora** (IDEA §0). La barra superior de cada app y el encabezado del PDF reservan un espacio de 40 × 40 dp (central: 48 × 48 px) para él.

## 2. Paleta

Dos temas:
- **Claro ("sol"):** para la central y para el juez en exteriores o con mucha luz.
- **Oscuro ("arena"):** para el juez en una arena con poca luz. El teléfono sigue el tema del sistema (`values-night/`), y la central usa el tema claro.

Contrastes calculados con la fórmula WCAG 2.x de luminancia relativa. AA pide 4.5:1 en texto normal y 3:1 en texto grande o componentes.

### 2.1 Esquinas
| Token | Claro (sol) | Oscuro (arena) | Uso |
|---|---|---|---|
| `corner_red` | `#B71C1C` | `#FF8A80` | Botones y bordes de Roja; texto "ROJA" |
| `on_corner_red` | `#FFFFFF` (6.57:1) | `#121212` (8.21:1) | Texto sobre un relleno rojo |
| `corner_red_container` | `#FDECEC` | `#3A1212` | Fondo del panel Roja |
| texto rojo sobre su panel | `#B71C1C` sobre `#FDECEC` (5.75:1) | `#FF8A80` sobre `#3A1212` (7.21:1) | Etiqueta y valor |
| `corner_blue` | `#0D47A1` | `#82B1FF` | Botones y bordes de Azul; texto "AZUL" |
| `on_corner_blue` | `#FFFFFF` (8.63:1) | `#121212` (8.64:1) | Texto sobre un relleno azul |
| `corner_blue_container` | `#E8EEF8` | `#10213F` | Fondo del panel Azul |
| texto azul sobre su panel | `#0D47A1` sobre `#E8EEF8` (7.4:1) | `#82B1FF` sobre `#10213F` (7.38:1) | Etiqueta y valor |

### 2.2 Neutros
| Token | Claro | Oscuro | Contraste |
|---|---|---|---|
| `background` | `#FFFFFF` | `#121212` | — |
| `surface` | `#F5F5F5` | `#1E1E1E` | — |
| `on_background` (texto) | `#1A1A1A` | `#EDEDED` | 17.4:1 · 16.0:1 |
| `on_surface` | `#1A1A1A` sobre `#F5F5F5` | `#EDEDED` sobre `#1E1E1E` | 15.96:1 · 14.24:1 |
| `text_secondary` | `#545454` | `#B0B0B0` | 7.57:1 · 8.64:1 |
| `disabled` (texto, botones deshabilitados) | `#6B6B6B` | `#B0B0B0` sobre `#2A2A2A` | 5.33:1 · 6.62:1 |

### 2.3 Estados
Cada estado lleva siempre **ícono + palabra + color**. En el tema claro se pone texto oscuro sobre un fondo tenue; en el oscuro, el texto claro va sobre `background`.

| Estado | Claro: texto sobre fondo | Oscuro: texto | Ícono (Material Symbols) |
|---|---|---|---|
| Pendiente | `#8A4B00` sobre `#FFF4E5` (6.26:1) | `#FFB74D` (10.82:1) | `schedule` |
| Enviado | `#1B5E20` sobre `#E8F5E9` (7.0:1) | `#81C784` (9.31:1) | `check_circle` |
| Tardío | `#B33C00` sobre `#FBE9E7` (5.03:1) | `#FF9E80` (9.33:1) | `running_with_errors` |
| Corregido | `#4A148C` sobre `#F3E5F5` (9.8:1) | `#CE93D8` (7.84:1) | `edit` |
| Conectado | `#1B5E20` sobre `#E8F5E9` (7.0:1) | `#81C784` (9.31:1) | `wifi` |
| Sin conexión | `#B71C1C` sobre `#FDECEC` (5.75:1) | `#FF8A80` (8.21:1) | `wifi_off` |
| Esperando aprobación | `#8A4B00` sobre `#FFF4E5` (6.26:1) | `#FFB74D` (10.82:1) | `hourglass_top` |

## 3. Tipografía
- **Juez (Android):** Roboto, la fuente del sistema, mediante los estilos de Material 3. Números con `fontFeatureSettings="tnum"`, para que no salten de ancho.
- **Central (JavaFX, Windows):** Segoe UI (sistema), con respaldo `System`. Números en `Segoe UI Semibold`.

| Rol | Juez (sp) | Central (px) | Uso |
|---|---|---|---|
| Valor de calificación | 64, negrita | 40, semibold | "10" / "9" en los paneles Roja/Azul |
| Tecla del teclado | 32, medio | — | 0–10 |
| Título de pantalla | 24, medio | 28, semibold | "Round 3", "Crear pelea" |
| Etiqueta de esquina | 20, negrita, MAYÚSCULAS | 18, semibold, MAYÚSCULAS | ROJA / AZUL |
| Cuerpo | 18 | 16 | Textos e instrucciones |
| Estado (chip) | 16, medio | 14, semibold | Enviado, Pendiente… |
| Resultado final | — | 56, bold | "Gana Roja por decisión dividida 2–1" |

Texto mínimo: 16 sp en el teléfono y 14 px en la central. La UI del teléfono debe seguir funcionando con la escala de fuente del sistema al 130 %; para eso los paneles crecen en altura y no se recorta texto.

## 4. Tamaños y espaciado
- **Teclado del juez:** cada tecla mide **≥ 64 × 64 dp** (objetivo 72 dp de alto), con 8 dp entre teclas.
- **Botón primario del juez** ("Enviar round", "Unirme", "Enviar tarjeta final"): ancho completo, 72 dp de alto y a ≤ 16 dp del borde inferior.
- **Otros objetivos táctiles del juez:** ≥ 48 dp.
- **Central:** botones de ≥ 40 px de alto. Las acciones destructivas (cerrar round con faltantes, terminar pelea, cerrar pelea) van separadas al menos 24 px de las demás y nunca son el botón por defecto (Enter).
- Rejilla de 8 dp/px. Márgenes de pantalla: 16 dp (juez) y 24 px (central).
- El teléfono mantiene la pantalla encendida en `ScoreActivity`, `LateScoreActivity` y `FinalCardActivity` (`FLAG_KEEP_SCREEN_ON`).
- Retroalimentación háptica corta en cada tecla y una vibración larga en `ROUND_CLOSED` sin envío (AND-006).

## 5. Textos de estado (es-MX)
Cadenas fijas: las dos apps usan exactamente estas palabras.

| Clave | Texto | Dónde |
|---|---|---|
| `status_pending` | Pendiente | Round abierto sin envío del juez |
| `status_sent` | Enviado | Envío con `ACK` dentro del round abierto |
| `status_late` | Tardío | Envío recibido después de `ROUND_CLOSED` |
| `status_corrected` | Corregido | Tarjeta con más de una versión (juez u organizador) |
| `status_connected` | Conectado | Socket abierto y `PONG` reciente |
| `status_disconnected` | Sin conexión | Socket caído; el teléfono reintenta solo |
| `status_waiting_approval` | Esperando aprobación | Después de `JOIN`, antes de `JOIN_APPROVED` |
| `status_sending` | Enviando… | En la bandeja de salida, sin `ACK` todavía |
| `status_captured` | Capturado | Calificación capturada desde papel por el organizador |

Otros textos recurrentes:
- Botones: "Enviar round", "Corregir", "Enviar tarjeta final", "Unirme", "Escanear QR", "Escribir código", "Aceptar y continuar", "Aprobar", "Rechazar", "Abrir round", "Cerrar round", "Cerrar pelea", "Exportar PDF", "Exportar CSV", "Ver resumen parcial".
- Confirmación fuerte: "Escribe AVANZAR para cerrar el round sin todas las calificaciones." y "Escribe TERMINAR para registrar la terminación."
- Esquinas: "ROJA" y "AZUL" (en mayúsculas en las etiquetas; "Roja" y "Azul" en frases).

## 6. Bocetos · app del juez
Medidas de referencia: 360 × 800 dp. Roja siempre a la izquierda.

### 6.1 Términos (`TermsActivity`, primer uso)
```
┌──────────────────────────────┐
│ [logo]  SIGEBOX Juez         │
├──────────────────────────────┤
│ Términos y aviso de          │
│ privacidad                   │
│                              │
│ · Tus datos (nombre y        │
│   calificaciones) se quedan  │
│   en los equipos del evento. │
│ · No salen de la red local.  │
│ · Prohibido usarlo para      │
│   apuestas.                  │
│   (texto con scroll)         │
│                              │
│ [ ] Leí y acepto             │
│ ┌──────────────────────────┐ │
│ │   Aceptar y continuar    │ │  ← deshabilitado sin la casilla
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

### 6.2 Unión (`JoinActivity` + `ScanActivity`)
```
┌──────────────────────────────┐
│ [logo]  Unirme a la pelea    │
├──────────────────────────────┤
│ Tu nombre                    │
│ ┌──────────────────────────┐ │
│ │ María López              │ │
│ └──────────────────────────┘ │
│                              │
│ Lugar: Juez 2 · Pelea 4F2A   │  ← se llena al leer el QR
│                              │
│ ┌──────────────────────────┐ │
│ │  ▣  Escanear QR          │ │  → ScanActivity: visor de
│ └──────────────────────────┘ │    cámara a pantalla completa
│      Escribir código         │  → código de 6 dígitos + host
│ ┌──────────────────────────┐ │
│ │         Unirme           │ │  ← deshabilitado sin nombre o lugar
│ └──────────────────────────┘ │
└──────────────────────────────┘
```
- `ScanActivity` muestra el visor con un marco cuadrado y el texto "Apunta al QR de tu lugar". Al leerlo vuelve sola a esta pantalla.
- Con un código inválido o un QR que no es de SIGEBOX: "Este código no es de SIGEBOX".

### 6.3 Espera (`WaitingActivity`)
```
┌──────────────────────────────┐
│ [logo]          ● Conectado  │
├──────────────────────────────┤
│                              │
│         ⏳                    │
│   Esperando aprobación       │
│                              │
│   Juez 2 · María López       │
│   El organizador debe        │
│   aprobarte en la central.   │
│                              │
│        Cancelar              │
└──────────────────────────────┘
```
- Con `JOIN_REJECTED`: "No fuiste aprobado: <motivo>" y el botón "Volver".

### 6.4 Puntuación (`ScoreActivity`)
```
┌──────────────────────────────┐
│ Juez 2 · María   ● Conectado │
│ Round 3 de 4     ◷ Pendiente │
├──────────────┬───────────────┤
│    ROJA      │     AZUL      │  ← tocar un panel lo selecciona
│              │               │    (borde de 4 dp en su color)
│     10       │      —        │  ← 64 sp; "—" = sin valor
│              │               │
├──────────────┴───────────────┤
│ ┌────┐┌────┐┌────┐┌────┐     │
│ │ 0  ││ 1  ││ 2  ││ 3  │     │  teclas ≥ 64 dp
│ └────┘└────┘└────┘└────┘     │
│ ┌────┐┌────┐┌────┐┌────┐     │
│ │ 4  ││ 5  ││ 6  ││ ⌫  │     │
│ └────┘└────┘└────┘└────┘     │
│ ┌────┐┌────┐┌────┐┌────┐     │
│ │ 7  ││ 8  ││ 9  ││ 10 │     │  ← 10 y 9, junto al pulgar
│ └────┘└────┘└────┘└────┘     │
│ ┌──────────────────────────┐ │
│ │      Enviar round        │ │  72 dp; deshabilitado hasta
│ └──────────────────────────┘ │  tener Roja y Azul válidos
└──────────────────────────────┘
```
- Flujo: empieza seleccionada ROJA. Al tocar un número se llena el panel y la selección pasa a AZUL. "⌫" borra el panel seleccionado.
- Cada tecla escribe el valor completo (no se concatenan dígitos), así que "1" seguido de "0" deja 0, no 10.
- Al enviar, el chip pasa a "Enviando…" y, con el `ACK`, a "Enviado". Los valores enviados quedan visibles en gris y aparece el botón "Corregir" mientras el round siga abierto. Después de corregir, el chip dice "Corregido".
- Entre rounds (round cerrado y el siguiente sin abrir): "Esperando el round 4". Nunca muestra los rounds anteriores sumados.
- Con "Sin conexión" se puede calificar igual: el envío queda en "Enviando…" hasta recibir el `ACK`.

### 6.5 Tardío (`LateScoreActivity`)
```
┌──────────────────────────────┐
│ ⚠  Round 3 cerrado           │  fondo `status_late`
│ Falta tu calificación.       │
│ Califica ahora.              │
├──────────────┬───────────────┤
│    ROJA      │     AZUL      │
│      —       │      —        │
├──────────────┴───────────────┤
│        (mismo teclado)       │
│ ┌──────────────────────────┐ │
│ │   Enviar round tardío    │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```
- Pantalla completa, con vibración larga al abrirse. "Atrás" no la cierra; solo se sale enviando. El envío lleva `late: true` y el chip final dice "Tardío".

### 6.6 Tarjeta final (`FinalCardActivity`)
```
┌──────────────────────────────┐
│ Tarjeta final · Juez 2       │
├──────────────────────────────┤
│ Round   ROJA   AZUL   Estado │
│  1       9     10     Enviado│
│  2       9     10     Enviado│
│  3      10      9     Tardío │
│  4       9     10     Enviado│
│                              │
│ Revisa tu tarjeta. Al        │
│ enviarla ya no podrás        │
│ corregir.                    │
│ ┌──────────────────────────┐ │
│ │  Enviar tarjeta final    │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```
- Solo la tarjeta propia, **sin fila de totales ni ganador** (principio 1). Tras el `ACK`: "Tarjeta final enviada. Gracias."

## 7. Bocetos · central
Ventana de referencia: 1366 × 768 px. Barra superior fija con [logo], el nombre de la pelea, el estado del servidor ("Servidor activo · 192.168.x.x:8765") y el botón de bitácora.

### 7.1 Crear pelea
```
┌──────────────────────────────────────────────────────────────┐
│ [logo] SIGEBOX · Crear pelea                   Bitácora      │
├──────────────────────────────────────────────────────────────┤
│ Esquina ROJA  [ Nombre del peleador ...... ]                 │
│ Esquina AZUL  [ Nombre del peleador ...... ]                 │
│ Rounds        [ 4 ▾ ]  (1–12)                                │
│ Jueces        [ 3 ▾ ]  (1–5)                                 │
│                                                [ Crear pelea ]│
└──────────────────────────────────────────────────────────────┘
```
Al crearla se abre la pantalla de aprobación.

### 7.2 Aprobación de jueces
```
┌──────────────────────────────────────────────────────────────┐
│ Pelea: Roja vs Azul · 4 rounds · 3 jueces                    │
├────────────────────┬────────────────────┬────────────────────┤
│ Juez 1             │ Juez 2             │ Juez 3             │
│ ┌──────────────┐   │ ┌──────────────┐   │ ┌──────────────┐   │
│ │   [QR 240px] │   │ │   [QR 240px] │   │ │   [QR 240px] │   │
│ └──────────────┘   │ └──────────────┘   │ └──────────────┘   │
│ Código 482 913     │ Código 107 556     │ Código 993 201     │
│ ● Conectado        │ ⏳ Esperando aprob. │ — Sin unir         │
│ Juan Pérez         │ María López        │                    │
│ Aprobado 20:14     │ [Aprobar][Rechazar]│                    │
├────────────────────┴────────────────────┴────────────────────┤
│                                  [ Ir a rounds ] (todos ok)  │
└──────────────────────────────────────────────────────────────┘
```
- El código va en dos grupos de 3 dígitos y 32 px. Con 4 o 5 jueces, las tarjetas pasan a 2 filas.

### 7.3 Rounds
```
┌──────────────────────────────────────────────────────────────┐
│ Round 3 de 4 · ABIERTO 02:41                                 │
├──────────────────────────────────────────────────────────────┤
│ Juez            Conexión      R1        R2        R3         │
│ 1 Juan Pérez    ● Conectado   Enviado   Enviado   Enviado    │
│ 2 María López   ● Conectado   Enviado   Corregido ◷ Pendiente│
│ 3 Ana Ruiz      ✕ Sin conex.  Enviado   Enviado   Enviado    │
├──────────────────────────────────────────────────────────────┤
│ [ Abrir round ]  [ Cerrar round ]          Ver resumen parcial│
│                                  [ Terminación ]  [ Cerrar pelea ]│
└──────────────────────────────────────────────────────────────┘
```
- Las celdas muestran **solo el estado**, nunca valores ni totales.
- Cerrar con faltantes abre un diálogo: "Faltan: Juez 2 · María López. Escribe AVANZAR para cerrar el round sin todas las calificaciones." El botón se habilita solo con el texto exacto. Después, la celda del faltante dice "Pendiente" hasta que llega su envío, y entonces "Tardío".
- "Ver resumen parcial" es un enlace discreto en `text_secondary`. Pide confirmación, se muestra 15 s con cuenta regresiva y queda en la bitácora (RO-07).
- Clic derecho sobre una celda → "Corregir / capturar desde papel…" (RO-09), con motivo obligatorio. La celda queda en "Corregido" o "Capturado".

### 7.4 Terminación extraordinaria (diálogo)
```
┌──────────────────────────────────────────────┐
│ Terminación extraordinaria                   │
├──────────────────────────────────────────────┤
│ Motivo   ( ) KO  ( ) KO técnico              │
│          ( ) Detenida por el médico          │
│          ( ) Abandono  ( ) Descalificación   │
│          ( ) Decisión técnica                │
│          ( ) Sin decisión  ( ) Otro: [_____] │
│ Gana     ( ) ROJA  ( ) AZUL  ( ) No aplica   │
│ Round    [ 3 ▾ ]   Tiempo [ 01:47 ]          │
│ Observaciones [____________________________] │
│                                              │
│ Escribe TERMINAR para confirmar: [________]  │
│                       [Cancelar] [Registrar] │
└──────────────────────────────────────────────┘
```
- "Otro" exige razón escrita. "Registrar" se habilita solo con todos los campos obligatorios y TERMINAR escrito.

### 7.5 Resultado (pantalla completa, solo operador)
```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│        Gana ROJA por decisión dividida 2–1                   │  56 px
│                                                              │
│  Juez          R1     R2     R3       R4     Total   Gana    │
│  Juan Pérez    10–9   9–10   10–9     10–9   39–37   Roja    │
│  María López   9–10   9–10   10–9 T   9–10   37–39   Azul    │
│  Ana Ruiz      10–9   10–9   9–10     10–9   39–37   Roja    │
│                                                              │
│  T = Tardío · C = Corregido · P = Capturado desde papel      │
│                    [ Exportar PDF ] [ Exportar CSV ] [ Salir ]│
└──────────────────────────────────────────────────────────────┘
```
- Valores en formato "Roja–Azul" y totales con nombre de juez. Esta es la única pantalla con totales.
- Con una terminación extraordinaria, el título dice el motivo: "Gana AZUL por KO técnico · Round 5, 01:47".
- El PDF repite esta tabla, con el espacio reservado para el logo en el encabezado.

### 7.6 Bitácora
```
┌──────────────────────────────────────────────────────────────┐
│ Bitácora · Pelea Roja vs Azul        Filtro [ Todos ▾ ]      │
├──────────────────────────────────────────────────────────────┤
│ 20:14:03  JOIN_APPROVED     Juez 1 · Juan Pérez              │
│ 20:31:47  ROUND_CLOSED      R3 · cierre forzado · falta J2   │
│ 20:31:59  SUBMIT_ROUND      J2 · R3 · tardío                 │
│ 20:33:10  PARTIAL_SUMMARY   visto 15 s por el operador       │
└──────────────────────────────────────────────────────────────┘
```
- Solo lectura, en orden cronológico y en monoespaciada (Consolas, 14 px). Nunca muestra valores de calificación antes de `FIGHT_CLOSED`.

## 8. Implementación
- **Android:** colores en `values/colors.xml` y `values-night/colors.xml`, con los nombres de token de §2 (`corner_red`, `status_sent`…). Tema Material 3 en `themes.xml`, y los textos de §5 en los `strings_*.xml` de cada tarea.
- **JavaFX:** los mismos tokens como variables CSS en `BOXCENTRAL/.../sigebox.css` (`-corner-red: #B71C1C;` …).
- Un cambio de color o de texto de estado se hace primero aquí, por PR, y después en las dos apps.
