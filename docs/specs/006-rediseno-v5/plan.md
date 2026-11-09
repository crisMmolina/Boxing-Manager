# Plan 006 · Rediseño v5 «Arena nocturna»

## Design
- **Guía primero (DSN-005).** `docs/DESIGN.md` v5 fija tokens nuevos: radios (8/12/16/20), espaciado, movimiento, colores de estado (fondo tenue de conectado y de pendiente), ícono, y la tabla AA de cada par nuevo. Las dos apps usan los mismos nombres de token.
- **Central:**
  - `ui/theme` gana `Icons` (trazos SVG en `SVGPath`, estilo de línea de 2 px, de un set con licencia libre anotado en `docs/design/CREDITOS.md`), `Stepper`, `ActionMenu` (⋯), `Callout`, `Chip`, `Avatar` y `Motion` (entrada de pantalla, hover y presión, que respetan "reducir movimiento").
  - `SideNav` reemplaza a `StepsBar` con la misma API (`select`, `showEvent`), así `Nav` y sus pruebas siguen.
  - Cada pantalla se rearma con esos componentes, sin tocar servicios.
- **Juez:**
  - Tokens y estilos en `res/values*`; íconos como vectores en `res/drawable/ic_*`; `Motion` en `ui/` para transiciones de eje compartido y para leer la escala de animación del sistema.
  - Pantallas en XML con los componentes Material ya presentes.
- **Por qué no una librería.** AtlantaFX o Ikonli darían más rápido el look y los íconos, pero agregan dependencias a dos apps sin internet y cambian todo el CSS. Íconos propios y CSS propio mantienen el control y no necesitan revisión de dependencias.

## Data and migrations
Ninguna.

## Contracts
Ninguno: no cambia el protocolo ni los servicios.

## Threat model
No toca un área sensible: solo presentación. Se mantiene el principio 3: ninguna pantalla nueva muestra puntos o totales durante la pelea (las pruebas de En vivo lo siguen comprobando).

## Observability
Sin cambios.

## Rollout and rollback
Cada tarea es un PR independiente que deja la app usable. Rollback = revertir el PR.

## Cost
Ninguno.

## Tasks
DSN-005 → DSK-041 → DSK-042 → (DSK-043, DSK-044, DSK-045 en paralelo); DSN-005 → AND-022 → (AND-023, AND-024 en paralelo). Cada una empieza con su prueba, y la evidencia son capturas en claro y oscuro revisadas por un crítico de diseño.
