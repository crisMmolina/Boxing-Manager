# STATE

Phase: execution
Updated: 1792954403
AVAILABLE: CORE-004 QA-001 QA-002 DSK-005 
CLAIMED: -
BLOCKED: AND-002 
REVIEW: AND-006 DSK-006 DSK-007 DSK-008 DSK-009 DSK-010 
INTEGRATED: AND-001 AND-003 AND-004 AND-005 CORE-001 CORE-002 CORE-003 DSK-001 DSK-002 DSK-003 DSK-004 DSN-001 
CANCELLED: -

Claims (first 24; status --task ID shows detail):
AND-002 | BLOCKED | cris-1 | until 1792944253
AND-006 | REVIEW | cris-2 | until 1793017139
DSK-006 | REVIEW | zoe-1 | until 1793040757
DSK-007 | REVIEW | zoe-1 | until 1793040773
DSK-008 | REVIEW | zoe-1 | until 1793040788
DSK-009 | REVIEW | zoe-1 | until 1793040803
DSK-010 | REVIEW | zoe-1 | until 1793037905

Blockers: BLOCKED tasks; use status --task ID --summary.
Current decisions (last 5):
- 1792922340 | DSK-009 | aceptacion como evento global por version | Why: sin tocar db/ (DSK-003)
- 1792929059 | deadline lease/AND-002/1792925738 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.
- 1792929849 | AND-002 | BLOCKED: needs physical devices | Why: spike results require the A37 hotspot and phones; inventing them is forbidden
- 1792930736 | AND-006 | Reutiliza ScoreViewModel.openLate; se puede salir al enviar (queda en la bandeja); tardío = ACK status TARDIA | Why: SubmitRound no tiene campo late; nada
- 1792948662 | deadline lease/AND-002/1792944253 | REQUIRES_RECOVERY | Claim kept; read the summary and use release --expired --read-summary. No work is deleted.

Global next step: available P0; else review PRs, tests, demo, rehearsal.
Why: bounded snapshot to resume without rereading history.
