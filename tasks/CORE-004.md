# CORE-004
ID: CORE-004
State: REVIEW
Owner: cris-1
Branch: feat/core-004
Worktree: /Users/cristophermolina/Desktop/box-wt/core-004
Worklog: worklog/WL-1792990498-cris-1-CORE-004-hack.XTfGvl.md
Priority: P1
Paths: core/build.gradle.kts, docs/specs/001-sigebox/plan.md
Depends: CORE-001, CORE-002
Verify: `core/gradlew -p core build`
Lease-Until: 1793076991
Updated: 1792990591
Task-Base: bcae7de41b537f8e0b251f08e80ba7b49c03bc1d
Next: Wait for review, then uak merge CORE-004
PR: https://github.com/crisMmolina/Boxing-Manager/pull/50
Evidence: core/gradlew -p core build exit 0; broken PING fixture now reruns and fails tests (was UP-TO-DATE); plan.md 16 messages + Spoofing row
Events: 4
Checkpoints: 1
Sessions: 2
Last-Checkpoint: 1792990591
Task-Tip: 2b13c34bdb6d5ca02af773021764169dd69b1e59
