# worklog/WL-1793773581-crismmolina-3-DSK-033-hack.wjGmEV.md · DSK-033

## Live summary
- Done: Claim created; implementation pending
- Decision: Own branch and worktree
- Why: Parallel work without touching the main checkout
- Failing: No validation run yet
- Commands: `central/gradlew -p central test --tests 'com.sigebox.central.server.*' --tests 'com.sigebox.central.ui.setup.*' && central/gradlew -p central :judge-sim:test`
- Next: prueba de `SeatTokens`: códigos únicos entre peleas abiertas

## History
- 1793773581 | CLAIMED | crismmolina-3 | claim; next: prueba de `SeatTokens`: códigos únicos entre peleas abiertas
- 1793773863 | CLAIMED | crismmolina-3 | pending decision DSK-033-1793773863; option: FightService.Invitation suma qrUri (forma c=código); uri() queda con la forma vieja para no editar FightServiceTest. El QR que se muestra cambia solo si SetupScreen usa QrCode con inv.qrUri(): pido sumar SetupScreen.java a Paths (2 líneas); Why: invite().uri() nuevo rompe FightServiceTest (fuera de Paths, no se edita); SetupScreen.qr está fuera de Paths
