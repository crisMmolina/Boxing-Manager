# STATE

Phase: execution
Updated: 1793258874
AVAILABLE: DSK-021 
CLAIMED: -
BLOCKED: -
REVIEW: -
INTEGRATED: (46; last 12) ... DSK-015 DSK-016 DSK-017 DSK-018 DSK-019 DSK-020 DSN-001 DSN-002 DSN-003 QA-001 QA-002 QA-003 
CANCELLED: -

Claims (first 24; status --task ID shows detail):

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1793072337 | AND-015 | Estado del envío desde la bandeja de salida (FinalCardStatus), no de onSaveInstanceState | Why: Sobrevive al cierre forzado; Paths solo finalcard/
- 1793257686 | DSK-020 | hub.nextFight + FuncionService.alSiguiente; SQL directo en JudgeHub (db/ fuera de Paths) | Why: Paths
- 1793257766 | AND-013 | relanzo ScoreActivity desde JudgeRuntime; sin lugar no cambia fila | Why: sin migración Room
- 1793258205 | deadline decision/CORE-005-1793256966/1793257866 | REVERSIBLE_AND_LOG | Option: cris (humano) da el OK al contrato NEXT_FIGHT (ADR 0002); se puede mergear; Why: Crit
- 1793258568 | QA-003 | wire alSiguiente en el test | Why: DSK-021 aun no

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
