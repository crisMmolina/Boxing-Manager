# worklog/WL-1794014381-crxthrx-3-DSK-037-hack.WTOBe6.md · DSK-037

## Live summary
- Done: botón a la izquierda; base del evento editable
- Decision: pelea en Jueces pasa a DSK-040
- Why: SeatTokens/JudgeHub con security-reviewer
- Failing: none
- Commands: central/gradlew -p central test
- Next: Wait for review of SHA 1f7b33b

## History
- 1794014381 | CLAIMED | crxthrx-3 | claim; next: prueba de que bajar a 2 jueces quita el lugar 3 de la pelea pendiente
- 1794014890 | CLAIMED | crxthrx-3 | pending decision DSK-037-1794014890; option: DSK-037 cubre la base del evento y las peleas programadas; la pelea que ya espera en Jueces (quitar lugares y códigos) pasa a DSK-040 (plan PR #107). El cambio se anota como CARTELERA_CAMBIADA (sin tipo nuevo, sin tocar Labels.java). Al bajar la base, las peleas programadas dejan su valor propio y siguen la base; Why: SeatTokens y JudgeHub son superficie de autenticación: van con security-reviewer y fuera de Paths de DSK-037
- 1794014994 | CLAIMED | crxthrx-3 | heartbeat; lease until 1794029394
- 1794017432 | REVIEW | crxthrx-3 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/109; evidence: Verify exit 0 (FuncionCambioTest 5/5, FuncionAddRowTest 2/2); central test exit 0; smoke PRODUCT_PASS; reviewer: -
- 1794017486 | REVIEW | crxthrx-3 | checkpoint; next: Wait for review of SHA 1f7b33b
