# STATE

Phase: execution
Updated: 1792916853
AVAILABLE: DSK-007 DSK-008 DSK-009 DSK-010 QA-001 QA-002 DSK-005 
CLAIMED: AND-006 
BLOCKED: AND-002 
REVIEW: DSK-006 
INTEGRATED: AND-001 AND-003 AND-004 AND-005 CORE-001 CORE-002 CORE-003 DSK-001 DSK-002 DSK-003 DSK-004 DSN-001 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
AND-002 | BLOCKED | cris-1 | until 1792925738
AND-006 | CLAIMED | cris-2 | until 1792931253
DSK-006 | REVIEW | zoe-1 | until 1793003165

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792912693 | DSK-003 | processed_message PK (fight,seat,id); public_key y signature como columnas fuera del modelo core | Why: contrato CORE-002 README
- 1792914901 | DSK-004 | hub transport-agnostic; idempotencia antes de estado; JOIN branches por llave | Why: contrato CORE-002 y AND-003
- 1792914981 | AND-005 | Tarjetas propias desde la bandeja (OwnCards) sin tocar net/; ROUND_CLOSED mustSubmit abre LateScoreActivity por nombre | Why: Paths de AND-005; AND-006 no 
- 1792916373 | DSK-005 | primero followup de DSK-004 para código manual de 6 dígitos en SeatTokens | Why: contrato AND-004 #37: código manual; SeatTokens fuera de Paths de DSK-0
- 1792916753 | DSK-006 | Broadcaster interface; UI refresca cada 500 ms | Why: testable sin JudgeHub; criterio <=1 s

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
