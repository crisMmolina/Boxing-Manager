# CORE-002
ID: CORE-002
State: REVIEW
Owner: cris-1
Branch: feat/core-002
Worktree: /Users/cristophermolina/Desktop/box-wt/core-002
Worklog: worklog/WL-1792908267-cris-1-CORE-002-hack.GIlNmT.md
Priority: P0
Paths: core/src/main/java/com/sigebox/core/model/, core/src/main/java/com/sigebox/core/protocol/, core/schemas/, core/src/test/java/com/sigebox/core/protocol/, core/src/test/resources/, demo_data/
Depends: CORE-001
Verify: `core/gradlew -p core test`
Lease-Until: 1792996781
Updated: 1792910381
Task-Base: 782ded9d1d7fb58748b64f91f1c3de079c8cfd80
Next: Wait for review of SHA 0b669d8, then uak merge CORE-002
PR: https://github.com/crisMmolina/Boxing-Manager/pull/6
Evidence: core/gradlew -p core test exit 0; build --rerun-tasks exit 0; 151 tests; jacoco 96% lines; security-reviewer re-review: no HIGH
Events: 5
Checkpoints: 1
Sessions: 2
Last-Checkpoint: 1792910205
Task-Tip: 0b669d887b94f38c2828f330ab20aa4d375f08ea
Progress: 13 commit(s) since the claim · last: 0b669d8 Merge remote-tracking branch 'origin/main' into feat/core-002 (4 minutes ago) · 0 uncommitted tracked file(s) · at 1792910381
Progress-Head: 0b669d8/4294967295
Progress-Changed: 1792910381
