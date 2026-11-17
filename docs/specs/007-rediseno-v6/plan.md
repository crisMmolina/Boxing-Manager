# Plan 007 · Rediseño v6 «Esquina» (app del juez)

## Design
- **Capturas primero (AND-026).** Un arnés Robolectric con gráficos nativos (`@GraphicsMode(NATIVE)`), como el `ScreenshotHarness` de la central, abre cada Activity con estado falso y guarda PNG en claro y oscuro en la carpeta de `SIGEBOX_SCREENSHOTS`. No corre en CI. Da las capturas "antes" y la evidencia de cada tarea.
- **Aditivo.** `ThemeV4Test`, `ThemeV5Test`, `ContrastV4Test` y `TopBarV4Test` fijan los tokens y la barra actuales. La v6 agrega tokens con nombres nuevos (`arena`, `arena_2`, `on_arena`, `on_arena_2`, `arena_line`, `ring_*`) en `values/` y `values-night/`, con su propia prueba de contraste, y no cambia ningún valor fijado. Los ids de vista que usan las pruebas se conservan.
- **Guía (DSN-006).** `docs/DESIGN.md` gana §7 «v6 Esquina»: tokens arena, fila de esquina, anillo de rounds, píldoras, marca inclinada, y la tabla de tendencias de la spec.
- **Componentes del juez:** `ui/RoundRing` (vista propia que dibuja los segmentos en arco; lee la escala de animación del sistema como `Motion`), estilo `Widget.Sigebox.Pill` / `Pill.Outline`, layout `view_corner_row` (número · etiqueta · − +) que reemplaza a la tarjeta de esquina en Puntuar conservando sus ids (`corner_value`, `corner_minus`, `corner_plus`, `corner_stripe`).
- **Por qué no Compose.** La app es de vistas XML; migrar es otra spec. Material Components ya trae las píldoras y la háptica.

## Data and migrations
Ninguna.

## Contracts
Ninguno: no cambia el protocolo, `ScoreViewModel` ni los servicios.

## Threat model
Solo presentación. Principio 3 intacto: ninguna pantalla nueva muestra totales o ganador durante la pelea; `nuncaHayTotales` y las pruebas de Round enviado siguen.

## Observability
Sin cambios.

## Rollout and rollback
Cada tarea es un PR independiente que deja la app usable. Rollback = revertir el PR.

## Cost
Ninguno. Sin dependencias nuevas (Robolectric ya está en las pruebas).
