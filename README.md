# Evidencia DSK-026 · Rediseño v4 de la central

Capturas generadas en macOS con JavaFX 25 (`SIGEBOX_SCREENSHOTS=<carpeta>`) desde `feat/dsk-026`.

- `componentes-claro.png` / `componentes-oscuro.png`: barra negra con logo y SIGEBOX dorado, paso activo con fondo dorado, "?" en la barra, campos con etiqueta arriba y ejemplo dentro, botón principal y secundario, juez desconectado con "? Cómo reconectar" y skeleton (movimiento reducido). Prueba: `ThemeScreenshots`.
- `ayuda-claro.png` / `ayuda-oscuro.png`: panel lateral "Recuperar la conexión" de 420 px, la central primero, botón "Listo".
- `01…07-*-claro.png`: pantallas actuales con el tema v4 (`ScreenshotHarness`). El harness se cuelga en la captura 08 (ventana de Resultado con `Stage` propio) antes de pasar al oscuro; no es de este cambio.

El primer commit de esta rama subió por error el árbol del repo; este commit lo quita.
