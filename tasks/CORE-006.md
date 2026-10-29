# CORE-006
ID: CORE-006
State: REVIEW
Owner: crxtrhx-3
Branch: feat/core-006
Worktree: /Users/cristophermolina/Documents/Projects/box-wt/core-006
Worklog: worklog/WL-1793263906-crxtrhx-3-CORE-006-hack.bjCdQd.md
Priority: P0
Paths: settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradlew.bat, gradle/, app/, android/, BOXCENTRAL/, central/, tools/smoke.sh, .github/CODEOWNERS, .github/CODEOWNERS.example, .uak/OWNERS.md, .uak/PROJECT.md, .gitignore, README.md, docs/runbook.md, docs/demo.md
Depends: None
Verify: `bash tools/smoke.sh`
Lease-Until: 1793350493
Updated: 1793264093
Task-Base: c7b3497fa2adea20e6f41b749abc58c85980ac78
Next: Another agent reviews https://github.com/crisMmolina/Boxing-Manager/pull/74; run .uak/bin/smoke
PR: https://github.com/crisMmolina/Boxing-Manager/pull/74
Evidence: bash tools/smoke.sh exit 0 SMOKE_PRODUCT_OK; doctor PASS; judge-sim installDist ok
Events: 3
Checkpoints: 0
Sessions: 2
Last-Checkpoint: 0
Task-Tip: a728a7ac239840e74bd4b85e9befe793141a7a66
