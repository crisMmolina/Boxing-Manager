# Revisión visual de la v4 (DSN-004)

- **Fecha:** 2026-11-03. **Sobre:** `docs/design/maqueta-v4.html` (central, claro y oscuro) y `docs/DESIGN.md` v4.
- **Revisor:** crítico visual (subagente con el rol de `design-critic`, que no está instalado en este modo). Solo lectura; los contrastes salen de `contraste.md`, no de las capturas.
- **Veredicto:** aprobado con cambios (1 bloqueante, 5 menores). Todos aplicados en DESIGN.md y en la maqueta:

| # | Hallazgo | Cambio aplicado |
|---|---|---|
| 1 (bloqueante) | Foco y error solo se distinguían por el tono del borde | Foco = borde + anillo exterior dorado; error = borde `ink` + ícono dentro del campo + mensaje debajo (§5.4) |
| 2 | En claro el dorado se leía como café; `accent_soft` muy grande | §2 explica el dorado vivo en la barra y su versión profunda sobre blanco; la selección siempre lleva franja de 3 px |
| 3 | "Bitácora · Inicio" como un solo texto; Inicio parecía secundario | Inicio junto a la marca en `on_bar`; Terminación y Bitácora separados 16 px con zona de 36 px (§5.7) |
| 4 | "? Cómo reconectar" partido y sin forma de botón | Botón secundario de 36 px, en segunda línea si no cabe (§5.8) |
| 5 | Skeleton mezclado con filas cargadas y poco visible en oscuro | Reemplaza toda la sección (§5.6); `skeleton` oscuro `#2C2A26` → `#36332E` |
| 6 | El logo a 28 px puede no leerse | Versión chica simplificada para ≤ 32 px, probada sobre la barra negra (§3.6) |

Pendiente: la pantalla de carga con el logo real se revisa cuando cris elija la ilustración (`CREDITOS.md`).
