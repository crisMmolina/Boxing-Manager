# worklog/WL-1793766514-crismmolina-2-DSK-026-hack.4DbSut.md · DSK-026

## Live summary
- Done: PR #85 en REVIEW, evidencia en evidence/dsk-026
- Decision: recursos en resources/.../ui/theme y ui/help
- Why: JavaFX no carga SVG; img/ es de DSK-029
- Failing: fuera de Paths: 2 recursos (#232); ? junto a juez sin cablear (#214)
- Commands: bash .uak/bin/q central/gradlew -p central test --tests 'com.sigebox.central.ui.theme.*' --tests 'com.sigebox.central.ui.help.*'
- Next: Wait for review of SHA df0372a

## History
- 1793766514 | CLAIMED | crismmolina-2 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793766612 | AVAILABLE | crismmolina-2 | release; claim released by crismmolina-2; reason: -; summary kept
- 1793770421 | CLAIMED | crismmolina-1 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793770472 | CLAIMED | crismmolina-1 | heartbeat; lease until 1793784872
- 1793771169 | CLAIMED | crismmolina-1 | checkpoint; next: capturas claro/oscuro en evidence/dsk-026 y /uak-ship
- 1793771720 | CLAIMED | crismmolina-1 | heartbeat; lease until 1793786120
- 1793771747 | REVIEW | crismmolina-1 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/85; evidence: Verify exit 0 (32 pruebas theme+help), suite central exit 0, smoke exit 0; capturas en evidence/dsk-026; pendiente: ? junto a juez desconectado (#214) y Paths de 2 recursos (#232); reviewer: -
- 1793771757 | REVIEW | crismmolina-1 | checkpoint; next: Wait for review of SHA df0372a
