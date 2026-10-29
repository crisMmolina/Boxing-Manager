# worklog/WL-1793257286-crxtrhx-1-DSK-020-hack.mnwwKH.md · DSK-020

## Live summary
- Done: criterios completos
- Decision: hub.nextFight + FuncionService.alSiguiente; SQL directo en JudgeHub (db/ fuera de Paths)
- Why: Paths
- Failing: -
- Commands: gradlew -p BOXCENTRAL test --tests 'com.sigebox.central.server.*'
- Next: Wait for review of SHA 9a507ba

## History
- 1793257286 | CLAIMED | crxtrhx-1 | claim; next: prueba de `JudgeHub`: dos jueces aprobados en la pelea 1; al iniciar la 2, ambos reciben `NEXT_FIGHT` con su lugar y su `HELLO` a la pelea 2 se acepta sin aprobación
- 1793257679 | REVIEW | crxtrhx-1 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/64; evidence: gradlew test --tests 'com.sigebox.central.server.*' exit 0; smoke exit 0; criterios cumplidos; reviewer: -
- 1793257686 | REVIEW | crxtrhx-1 | checkpoint; next: Wait for review of SHA 9a507ba
