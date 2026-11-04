# worklog/WL-1793766514-crismmolina-2-DSK-026-hack.4DbSut.md · DSK-026

## Live summary
- Done: aprobada por zoe-1 en 2bb67bc
- Decision: no fuerzo el merge
- Why: gate usa las Paths del claim, no TASKS.md
- Failing: uak_merge exit 3: New file outside Paths: central/src/main/resources/com/sigebox/central/ui/help/guia-conexion.md
- Commands: uak_merge DSK-026
- Next: plan --amend de la humana y reintentar uak_merge, o merge humano de #85 + uak integrate DSK-026

## History
- 1793766514 | CLAIMED | crismmolina-2 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793766612 | AVAILABLE | crismmolina-2 | release; claim released by crismmolina-2; reason: -; summary kept
- 1793770421 | CLAIMED | crismmolina-1 | claim; next: captura de la barra actual con `ScreenshotHarness`
- 1793770472 | CLAIMED | crismmolina-1 | heartbeat; lease until 1793784872
- 1793771169 | CLAIMED | crismmolina-1 | checkpoint; next: capturas claro/oscuro en evidence/dsk-026 y /uak-ship
- 1793771720 | CLAIMED | crismmolina-1 | heartbeat; lease until 1793786120
- 1793771747 | REVIEW | crismmolina-1 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/85; evidence: Verify exit 0 (32 pruebas theme+help), suite central exit 0, smoke exit 0; capturas en evidence/dsk-026; pendiente: ? junto a juez desconectado (#214) y Paths de 2 recursos (#232); reviewer: -
- 1793771757 | REVIEW | crismmolina-1 | checkpoint; next: Wait for review of SHA df0372a
- 1793772517 | REVIEW | zoe-1 | review; approve; SHA 2bb67bc74787fb65a6ae703477d1660748681d59; reviewer zoe-1
- 1793772641 | REVIEW | crismmolina-1 | merge rechazado; New file outside Paths: central/src/main/resources/com/sigebox/central/ui/help/guia-conexion.md. Move it into the task's Paths, or a human adds its dir to Additive-Paths in PROJECT.md (new files there need no reservation)
- 1793772678 | REVIEW | crismmolina-1 | checkpoint; next: plan --amend de la humana y reintentar uak_merge, o merge humano de #85 + uak integrate DSK-026
