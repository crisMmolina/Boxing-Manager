# worklog/WL-1794025830-crxthrx-1-DSK-040-hack.GkgdmB.md · DSK-040

## Live summary
- Done: Claim created; implementation pending
- Decision: Own branch and worktree
- Why: Parallel work without touching the main checkout
- Failing: No validation run yet
- Commands: `central/gradlew -p central test`
- Next: prueba de que bajar a 2 jueces con el lugar 3 sin juez quita su código

## History
- 1794025830 | CLAIMED | crxthrx-1 | claim; next: prueba de que bajar a 2 jueces con el lugar 3 sin juez quita su código
- 1794025933 | CLAIMED | crxthrx-1 | pending decision DSK-040-1794025933; option: Reemplazar FuncionCambioTest.unaPeleaQueYaEmpezoConservaSusJuecesYRounds por FuncionPreparadaTest.unaPeleaConRoundAbiertoConservaSusJuecesYRounds (round 1 abierto) y quitarla; Why: La pelea en Jueces sin round ya no cuenta como empezada (spec 005 §Decisions, plan PR #110)
