# STATE

Phase: execution
Updated: 1792955845
AVAILABLE: CORE-004 DSK-012 AND-007 QA-001 QA-002 DSK-005 
CLAIMED: DSK-011 
BLOCKED: AND-002 
REVIEW: AND-006 
INTEGRATED: (18; last 12) ... CORE-003 DSK-001 DSK-002 DSK-003 DSK-004 DSK-006 DSK-007 DSK-008 DSK-009 DSK-010 DSN-001 DSN-002 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
AND-002 | BLOCKED | cris-1 | until 1792944253
AND-006 | REVIEW | cris-2 | until 1793017139
DSK-011 | CLAIMED | zoe-1 | until 1792970245

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792929059 | deadline lease/AND-002/1792925738 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.
- 1792929849 | AND-002 | BLOCKED: needs physical devices | Why: spike results require the A37 hotspot and phones; inventing them is forbidden
- 1792930736 | AND-006 | Reutiliza ScoreViewModel.openLate; se puede salir al enviar (queda en la bandeja); tardío = ACK status TARDIA | Why: SubmitRound no tiene campo late; nada
- 1792948662 | deadline lease/AND-002/1792944253 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.
- 1792955149 | DSN-002 | modo sencillo como base; sin Mi total; 10-10 + hoja de confirmacion | Why: JavaFX sin backdrop blur, minSdk 26; decisiones de zoe

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
