# CORE-008
ID: CORE-008
State: REVIEW
Owner: zoe-3
Branch: feat/core-008
Worktree: /c/Users/zm180/OneDrive/Desktop/BoxingJugde/Boxing-Manager-wt/core-008
Worklog: worklog/WL-1793767395-zoe-3-CORE-008-hack.Ob6HML.md
Priority: P0
Paths: core/src/main/java/com/sigebox/core/model/, core/src/main/java/com/sigebox/core/scoring/, core/src/test/java/com/sigebox/core/scoring/, core/src/test/resources/scoring/
Depends: CORE-006, CORE-002
Verify: `core/gradlew -p core build`
Lease-Until: 1793857234
Updated: 1793770834
Task-Base: 48c7ee02ac8c47aae289f8b4e3393a00bac5555b
Next: Humano: Base moved; revalidate the candidate
PR: https://github.com/crisMmolina/Boxing-Manager/pull/82
Evidence: core/gradlew -p core build --rerun-tasks exit 0 en b35f215; smoke exit 0 en 4648a50 (merge posterior solo TASKS.md); 6 criterios cumplidos; 227 pruebas, JaCoCo 97.6%
Events: 5
Checkpoints: 1
Sessions: 2
Last-Checkpoint: 1793770834
Task-Tip: b35f215d47ac65d1d3b44885f7eec78a58aaa5c4
