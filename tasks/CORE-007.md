# CORE-007
ID: CORE-007
State: REVIEW
Owner: zoe-2
Branch: feat/core-007
Worktree: /c/Users/zm180/OneDrive/Desktop/BoxingJugde/Boxing-Manager-wt/core-007
Worklog: worklog/WL-1793767388-zoe-2-CORE-007-hack.1QRubb.md
Priority: P0
Paths: core/schemas/, core/src/main/java/com/sigebox/core/protocol/, core/src/test/java/com/sigebox/core/protocol/, demo_data/mensajes/
Depends: CORE-006, CORE-002, CORE-005
Verify: `core/gradlew -p core build`
Lease-Until: 1793856061
Updated: 1793769661
Task-Base: 48c7ee02ac8c47aae289f8b4e3393a00bac5555b
Next: Another agent reviews https://github.com/crisMmolina/Boxing-Manager/pull/80; run .uak/bin/smoke
PR: https://github.com/crisMmolina/Boxing-Manager/pull/80
Evidence: core/gradlew -p core build exit 0 en d54b0f5; smoke PRODUCT_PASS exit 0 en e99ddb6 (luego solo TASKS.md); JoinUri 39 pruebas y ProtocolSchemaTest en verde; JaCoCo lineas 97.7; JOIN, HELLO y DeviceProof sin cambios
Events: 3
Checkpoints: 1
Sessions: 1
Last-Checkpoint: 1793767705
Task-Tip: d54b0f54f4346eee52cbec4b5bacf00933e4eba7
