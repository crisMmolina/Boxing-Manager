# STATE

Phase: execution
Updated: 1792911105
AVAILABLE: AND-002 AND-004 AND-005 AND-006 DSK-003 DSK-004 DSK-005 DSK-006 DSK-007 DSK-008 DSK-009 DSK-010 QA-001 QA-002 
CLAIMED: AND-003 
BLOCKED: -
REVIEW: CORE-003 DSK-001 
INTEGRATED: AND-001 CORE-001 CORE-002 DSK-002 DSN-001 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
AND-003 | CLAIMED | cris-2 | until 1792924869
CORE-003 | REVIEW | cris-1 | until 1792997401
DSK-001 | REVIEW | zoe-1 | until 1792997398

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792908412 | DSK-002 | No se registra respaldo Java-WebSocket: Javalin funcionó. No se instaló WiX ni se creó la regla netsh (admin/seguridad) | Why: Evitar cambios de sistema
- 1792909957 | DSK-001 | App no extiende Application (Window anidada) para jpackage con classpath | Why: Lanzador Java exige main no-Application con JavaFX en classpath
- 1792910132 | DSK-001 | App.Window anidada; .msi por defecto, app-image sin WiX | Why: jpackage con JavaFX en classpath
- 1792910205 | CORE-002 | 16 messages incl CHALLENGE; strict decode; records enforce schema rules | Why: security HIGH on HELLO replay; no-default-value principle
- 1792910920 | CORE-003 | termination round excluded; IDEA §5 majority rule literal | Why: DSK may close the stopping round; spec literal until human answers

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
