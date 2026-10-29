# STATE

Phase: execution
Updated: 1793257945
AVAILABLE: DSK-021 QA-003 
CLAIMED: -
BLOCKED: -
REVIEW: AND-013 
INTEGRATED: (44; last 12) ... DSK-014 DSK-015 DSK-016 DSK-017 DSK-018 DSK-019 DSK-020 DSN-001 DSN-002 DSN-003 QA-001 QA-002 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
AND-013 | REVIEW | crxtrhx-2 | until 1793344166

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1793021192 | deadline lease/AND-012/1793008781 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.
- 1793070910 | AND-014 | Repetición del último STATE/FIGHT_ENDED en JudgeConnection.addListener | Why: Paths de la tarea; ScoreActivity no cambia
- 1793072337 | AND-015 | Estado del envío desde la bandeja de salida (FinalCardStatus), no de onSaveInstanceState | Why: Sobrevive al cierre forzado; Paths solo finalcard/
- 1793257686 | DSK-020 | hub.nextFight + FuncionService.alSiguiente; SQL directo en JudgeHub (db/ fuera de Paths) | Why: Paths
- 1793257766 | AND-013 | relanzo ScoreActivity desde JudgeRuntime; sin lugar no cambia fila | Why: sin migración Room

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
