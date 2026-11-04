# worklog/WL-1793766514-crismmolina-2-DSK-026-hack.4DbSut.md · DSK-026

## Live summary
- Done: paleta v4 + AA (28 pares de DESIGN §3.2 + bar/bar_accent), logo en la barra, activo con fondo (uno por pantalla), Skeleton, Ui.field con ejemplo, botones v4, ? en barra con panel de guía
- Decision: logo y guía en resources/com/sigebox/central/ui/theme y ui/help (img/ es de DSK-029), comparados byte a byte con docs/design
- Why: Paths no listan carpeta de recursos y JavaFX no carga SVG
- Failing: ninguna; falta cablear '? Cómo reconectar' en SetupScreen/RoundsScreen (fuera de Paths, pregunta #214 a human)
- Commands: bash .uak/bin/q central/gradlew -p central test --tests 'com.sigebox.central.ui.theme.*' --tests 'com.sigebox.central.ui.help.*'
- Next: capturas claro/oscuro en evidence/dsk-026 y /uak-ship

## History
- 1793766514 | CLAIMED | crismmolina-2 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793766612 | AVAILABLE | crismmolina-2 | release; claim released by crismmolina-2; reason: -; summary kept
- 1793770421 | CLAIMED | crismmolina-1 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793770472 | CLAIMED | crismmolina-1 | heartbeat; lease until 1793784872
- 1793771169 | CLAIMED | crismmolina-1 | checkpoint; next: capturas claro/oscuro en evidence/dsk-026 y /uak-ship
