# STATE

Phase: execution
Updated: 1793257286
AVAILABLE: AND-013 DSK-021 QA-003 
CLAIMED: DSK-020 
BLOCKED: -
REVIEW: -
INTEGRATED: (43; last 12) ... DSK-013 DSK-014 DSK-015 DSK-016 DSK-017 DSK-018 DSK-019 DSN-001 DSN-002 DSN-003 QA-001 QA-002 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
DSK-020 | CLAIMED | crxtrhx-1 | until 1793271686

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792994381 | AND-012 | uak decision: quitar ScreenDesignTest.greenIsOnly... (hex v2), lo reemplaza ThemeV3Test; capturas fuera de Paths: propuesta rama evidence/and-012 (sin resp
- 1792999626 | CORE-005 | seat opcional (encode omite null) | Why: consistente con README de schemas
- 1793021192 | deadline lease/AND-012/1793008781 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.
- 1793070910 | AND-014 | Repetición del último STATE/FIGHT_ENDED en JudgeConnection.addListener | Why: Paths de la tarea; ScoreActivity no cambia
- 1793072337 | AND-015 | Estado del envío desde la bandeja de salida (FinalCardStatus), no de onSaveInstanceState | Why: Sobrevive al cierre forzado; Paths solo finalcard/

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
