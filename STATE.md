# STATE

Phase: execution
Updated: 1793774338
AVAILABLE: DSK-024 DSK-027 DOC-001 DSK-030 DSK-031 DSK-032 AND-019 AND-021 QA-004 AND-018 
CLAIMED: DSK-033 
BLOCKED: -
REVIEW: DSK-029 DSK-034 
INTEGRATED: (59; last 12) ... DSK-022 DSK-023 DSK-025 DSK-026 DSK-028 DSN-001 DSN-002 DSN-003 DSN-004 QA-001 QA-002 QA-003 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
DSK-029 | REVIEW | crismmolina-1 | until 1793860568
DSK-033 | CLAIMED | crismmolina-3 | until 1793788738
DSK-034 | REVIEW | crismmolina-2 | until 1793860128

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1793771544 | DSK-023 | App.java:67 queda para DSK-029 por estar fuera de Paths | Why: Paths de DSK-023 no incluye App.java
- 1793771757 | DSK-026 | recursos en resources/.../ui/theme y ui/help | Why: JavaFX no carga SVG; img/ es de DSK-029
- 1793772678 | DSK-026 | no fuerzo el merge | Why: gate usa las Paths del claim, no TASKS.md
- 1793774168 | DSK-029 | orden por creación como FuncionQuery.latest; eventos anteriores en cartelera de consulta | Why: Evento siempre muestra el último y ui/funcion no es mío
- 1793774338 | DSK-033 | Invitation suma qrUri, uri queda vieja (FightServiceTest) | Why: no editar pruebas existentes

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
