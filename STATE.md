# STATE

Phase: execution
Updated: 1792991181
AVAILABLE: -
CLAIMED: -
BLOCKED: -
REVIEW: -
INTEGRATED: (36; last 12) ... DSK-010 DSK-011 DSK-012 DSK-013 DSK-014 DSK-015 DSK-016 DSK-017 DSN-001 DSN-002 QA-001 QA-002 
CANCELLED: -

Claims (first 24; status --task ID shows detail):

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792982194 | DSK-016 | no duplicar el id; solo boton Copiar | Why: el id ya se mostraba
- 1792985735 | AND-008 | no cambiar JudgeConnectionTest sin evidencia | Why: la hipotesis de carrera no se reprodujo
- 1792986992 | deadline decision/AND-008-1792985990/1792986890 | BLOCKED_OTHER_TASK | Option: auto-merge de GitHub en los PR del kit: lo activa zoe (humana) en cada PR, no el agent
- 1792988375 | AND-009 | test-only fix: latch holds central down; await CONNECTED/DISCONNECTED | Why: app behavior (at-least-once resend) is correct; test submitted before client n
- 1792990591 | CORE-004 | inputs.dir with RELATIVE path sensitivity, Gradle 8 compatible | Why: composite builds run core with host Gradle

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
