# CORE-003
ID: CORE-003
State: REVIEW
Owner: cris-1
Branch: feat/core-003
Worktree: /Users/cristophermolina/Desktop/box-wt/core-003
Worklog: worklog/WL-1792910472-cris-1-CORE-003-hack.9QCbSW.md
Priority: P0
Paths: core/src/main/java/com/sigebox/core/validation/, core/src/main/java/com/sigebox/core/scoring/, core/src/test/java/com/sigebox/core/validation/, core/src/test/java/com/sigebox/core/scoring/
Depends: CORE-002
Verify: `core/gradlew -p core test jacocoTestCoverageVerification`
Lease-Until: 1792997315
Updated: 1792910915
Task-Base: 0cd31291d92d5f77951972d0d2dd2c1e0115834c
Next: Another agent reviews https://github.com/crisMmolina/Boxing-Manager/pull/8; run .uak/bin/smoke
PR: https://github.com/crisMmolina/Boxing-Manager/pull/8
Evidence: core/gradlew -p core test jacocoTestCoverageVerification exit 0; build --rerun-tasks exit 0; 204 tests; validation+scoring 100% lines; reference fight Roja dividida 2-1; 12x5 hand sums
Events: 3
Checkpoints: 0
Sessions: 1
Last-Checkpoint: 0
Task-Tip: 9123dfdbf650a4298093065b8e207dd9bc89a7f3
