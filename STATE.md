# STATE

Phase: execution
Updated: 1792910200
AVAILABLE: (16; last 12) ... AND-005 AND-006 DSK-003 DSK-004 DSK-005 DSK-006 DSK-007 DSK-008 DSK-009 DSK-010 QA-001 QA-002 
CLAIMED: -
BLOCKED: -
REVIEW: CORE-002 DSK-001 
INTEGRATED: AND-001 CORE-001 DSK-002 DSN-001 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
CORE-002 | REVIEW | cris-1 | until 1792996600
DSK-001 | REVIEW | zoe-1 | until 1792996532

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792907122 | CORE-001 | Gradle 9.8.2 wrapper, networknt 3.0.8, jacoco in check | Why: task criteria; latest stable versions verified on Maven Central
- 1792908043 | deadline decision/CORE-001-1792907035/1792907935 | BLOCKED_OTHER_TASK | Option: añadir core/build/ (y BOXCENTRAL/build/) a .gitignore y core/.gitattributes con 'gra
- 1792908412 | DSK-002 | No se registra respaldo Java-WebSocket: Javalin funcionó. No se instaló WiX ni se creó la regla netsh (admin/seguridad) | Why: Evitar cambios de sistema
- 1792909957 | DSK-001 | App no extiende Application (Window anidada) para jpackage con classpath | Why: Lanzador Java exige main no-Application con JavaFX en classpath
- 1792910132 | DSK-001 | App.Window anidada; .msi por defecto, app-image sin WiX | Why: jpackage con JavaFX en classpath

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
