# worklog/WL-1794280399-crxthrx-8-DSK-043-hack.3AsW37.md · DSK-043

## Live summary
- Done: Claim created; implementation pending
- Decision: Own branch and worktree
- Why: Parallel work without touching the main checkout
- Failing: No validation run yet
- Commands: `central/gradlew -p central test`
- Next: prueba de que la fila de la cartelera tiene menú ⋯ y no botones sueltos

## History
- 1794280399 | CLAIMED | crxthrx-8 | claim; next: prueba de que la fila de la cartelera tiene menú ⋯ y no botones sueltos
- 1794280974 | CLAIMED | crxthrx-8 | pending decision DSK-043-1794280974; option: Reemplazar FuncionAddRowTest.laBaseDelEventoTieneRoundsYJuecesQueSeCambian y FuncionPreparadaRowTest.bajarLosJuecesDelEventoBajaLaPeleaEnJueces por FuncionV5Test (Stepper) e InicioTest.nuevoEventoFormCreatesTheEventWithLabeledFields por InicioV5Test (Stepper); Why: spec 006: ningún Spinner; las pruebas viejas buscaban Spinner y las nuevas prueban lo mismo con Stepper
