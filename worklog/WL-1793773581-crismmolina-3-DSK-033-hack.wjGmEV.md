# worklog/WL-1793773581-crismmolina-3-DSK-033-hack.wjGmEV.md · DSK-033

## Live summary
- Done: SeatTokens códigos únicos + lookup/forget; JudgeHub CODE_LOOKUP/límite/EVENT_ENDED; FightService.qrUri; QrCode; judge-sim --codes; Verify verde
- Decision: Invitation suma qrUri, uri queda vieja (FightServiceTest)
- Why: no editar pruebas existentes
- Failing: LabelsTest falta etiqueta CODE_LOOKUP_LIMIT en Labels.java (fuera de Paths, pedido a humano #250); SetupScreen fuera de Paths (#247)
- Commands: bash .uak/bin/q central/gradlew -p central test --tests 'com.sigebox.central.server.*' --tests 'com.sigebox.central.ui.setup.*' && bash .uak/bin/q central/gradlew -p central :judge-sim:test
- Next: Con autorización: Labels.java + SetupScreen usa QrCode/qrUri; luego smoke, security-reviewer, ship

## History
- 1793773581 | CLAIMED | crismmolina-3 | claim; next: prueba de `SeatTokens`: códigos únicos entre peleas abiertas
- 1793773863 | CLAIMED | crismmolina-3 | pending decision DSK-033-1793773863; option: FightService.Invitation suma qrUri (forma c=código); uri() queda con la forma vieja para no editar FightServiceTest. El QR que se muestra cambia solo si SetupScreen usa QrCode con inv.qrUri(): pido sumar SetupScreen.java a Paths (2 líneas); Why: invite().uri() nuevo rompe FightServiceTest (fuera de Paths, no se edita); SetupScreen.qr está fuera de Paths
- 1793774338 | CLAIMED | crismmolina-3 | checkpoint; next: Con autorización: Labels.java + SetupScreen usa QrCode/qrUri; luego smoke, security-reviewer, ship
