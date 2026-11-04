# STATE

Phase: execution
Updated: 1793773814
AVAILABLE: DSK-024 DSK-027 DOC-001 DSK-030 DSK-031 DSK-032 AND-019 AND-021 QA-004 AND-018 
CLAIMED: DSK-029 DSK-033 
BLOCKED: -
REVIEW: DSK-034 
INTEGRATED: (59; last 12) ... DSK-022 DSK-023 DSK-025 DSK-026 DSK-028 DSN-001 DSN-002 DSN-003 DSN-004 QA-001 QA-002 QA-003 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
DSK-029 | CLAIMED | crismmolina-1 | until 1793787717
DSK-033 | CLAIMED | crismmolina-3 | until 1793787981
DSK-034 | REVIEW | crismmolina-2 | until 1793860128

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1793771001 | AND-020 | ? agregado desde TopBar.java (view_top_bar.xml fuera de Paths) | Why: respetar Paths; AND-021 rediseña la barra
- 1793771169 | DSK-026 | logo y guía en resources/com/sigebox/central/ui/theme y ui/help (img/ es de DSK-029), comparados byte a byte con docs/design | Why: Paths no listan carpet
- 1793771544 | DSK-023 | App.java:67 queda para DSK-029 por estar fuera de Paths | Why: Paths de DSK-023 no incluye App.java
- 1793771757 | DSK-026 | recursos en resources/.../ui/theme y ui/help | Why: JavaFX no carga SVG; img/ es de DSK-029
- 1793772678 | DSK-026 | no fuerzo el merge | Why: gate usa las Paths del claim, no TASKS.md

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
