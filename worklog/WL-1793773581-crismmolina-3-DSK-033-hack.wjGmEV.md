# worklog/WL-1793773581-crismmolina-3-DSK-033-hack.wjGmEV.md · DSK-033

## Live summary
- Done: PR #91 en REVIEW
- Decision: límites globales y lugar rechazado reutilizable por seguridad; JOIN_REJECTED por decisión del líder
- Why: revisión de seguridad y Paths fijos
- Failing: ninguno
- Commands: bash .uak/bin/q central/gradlew -p central test --tests 'com.sigebox.central.server.*' --tests 'com.sigebox.central.ui.setup.*' && bash .uak/bin/q central/gradlew -p central :judge-sim:test
- Next: Wait for review of SHA 1537a94; revisar DSK-029 PR #89

## History
- 1793773581 | CLAIMED | crismmolina-3 | claim; next: prueba de `SeatTokens`: códigos únicos entre peleas abiertas
- 1793773863 | CLAIMED | crismmolina-3 | pending decision DSK-033-1793773863; option: FightService.Invitation suma qrUri (forma c=código); uri() queda con la forma vieja para no editar FightServiceTest. El QR que se muestra cambia solo si SetupScreen usa QrCode con inv.qrUri(): pido sumar SetupScreen.java a Paths (2 líneas); Why: invite().uri() nuevo rompe FightServiceTest (fuera de Paths, no se edita); SetupScreen.qr está fuera de Paths
- 1793774338 | CLAIMED | crismmolina-3 | checkpoint; next: Con autorización: Labels.java + SetupScreen usa QrCode/qrUri; luego smoke, security-reviewer, ship
- 1793774837 | CLAIMED | crismmolina-3 | pending decision DSK-033-1793774837; option: Sin ampliar Paths: el bloqueo al 5.º CODE_UNKNOWN se anota como JOIN_REJECTED ('búsqueda de código bloqueada tras 5 intentos · …NN'); SetupScreen no se toca y DSK-027 conectará inv.qrUri(); Why: decisión del líder (sesión que me lanzó): no se cambian Paths de una tarea reclamada
- 1793775399 | CLAIMED | crismmolina-3 | pending decision DSK-033-1793775399; option: Por la revisión de seguridad: (a) un CODE_UNKNOWN por falta de cupo global no cuenta para el cierre al 5.º; (b) un lugar RECHAZADO acepta otro teléfono con invitación nueva (antes SEAT_TAKEN); (c) al llegar a 16 conexiones sin autenticar se cierra la más vieja; Why: MEDIUM: un atacante que agota el cupo no debe echar a jueces reales; HIGH 2: el rechazo dejaba el lugar bloqueado para siempre
- 1793775437 | REVIEW | crismmolina-3 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/91; evidence: Verify exit 0 (server.*, ui.setup.*, judge-sim); suite central exit 0; smoke exit 0; security-reviewer sin HIGH; 5 criterios (QR en pantalla → DSK-027); reviewer: -
- 1793775444 | REVIEW | crismmolina-3 | checkpoint; next: Wait for review of SHA 1537a94; revisar DSK-029 PR #89
