# worklog/WL-1794025830-crxthrx-1-DSK-040-hack.GkgdmB.md · DSK-040

## Live summary
- Done: DSK-040 en PR #111
- Decision: pelea en Jueces cambia solo desde la base
- Why: spec 005 y FuncionServiceTest
- Failing: ninguna
- Commands: central/gradlew -p central test; bash .uak/bin/smoke
- Next: Wait for review of SHA 1ca2381

## History
- 1794025830 | CLAIMED | crxthrx-1 | claim; next: prueba de que bajar a 2 jueces con el lugar 3 sin juez quita su código
- 1794025933 | CLAIMED | crxthrx-1 | pending decision DSK-040-1794025933; option: Reemplazar FuncionCambioTest.unaPeleaQueYaEmpezoConservaSusJuecesYRounds por FuncionPreparadaTest.unaPeleaConRoundAbiertoConservaSusJuecesYRounds (round 1 abierto) y quitarla; Why: La pelea en Jueces sin round ya no cuenta como empezada (spec 005 §Decisions, plan PR #110)
- 1794028475 | CLAIMED | crxthrx-1 | pending decision DSK-040-1794028475; option: La pelea en Jueces cambia solo desde la base del evento, no desde su fila; Why: spec 005 dice 'del evento'; FuncionServiceTest.agregarQuitarReordenarYSaltarSoloAntesDeEmpezar fija que su fila no cambia y no se edita
- 1794029618 | REVIEW | crxthrx-1 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/111; evidence: central test exit 0; smoke PRODUCT_PASS 1022s; security-reviewer LOW; reviewer: -
- 1794029677 | REVIEW | crxthrx-1 | checkpoint; next: Wait for review of SHA 1ca2381
