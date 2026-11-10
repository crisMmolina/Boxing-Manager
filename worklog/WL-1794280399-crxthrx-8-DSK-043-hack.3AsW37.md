# worklog/WL-1794280399-crxthrx-8-DSK-043-hack.3AsW37.md · DSK-043

## Live summary
- Done: central test exit 0; smoke PRODUCT_PASS; capturas revisadas · existing tests changed with a human's approval (cris) · red at 13a1827:     org.opentest4j.AssertionFailedError at FuncionV5Test.java:114
- Decision: Reemplazar FuncionAddRowTest.laBaseDelEventoTieneRoundsYJuecesQueSeCambian y FuncionPreparadaRowTest.bajarLosJuecesDelEventoBajaLaPeleaEnJueces por FuncionV5Test (Stepper) e InicioTest.nuevoEventoFormCreatesTheEventWithLabeledFields por InicioV5Test (Stepper)
- Why: spec 006: ningún Spinner; las pruebas viejas buscaban Spinner y las nuevas prueban lo mismo con Stepper
- Failing: No validation run yet
- Commands: `central/gradlew -p central test`
- Next: Integrated in main: f99a3445fd4cc6048a5548a1f79312e17350c268 (recorded by zoe)

## History
- 1794280399 | CLAIMED | crxthrx-8 | claim; next: prueba de que la fila de la cartelera tiene menú ⋯ y no botones sueltos
- 1794280974 | CLAIMED | crxthrx-8 | pending decision DSK-043-1794280974; option: Reemplazar FuncionAddRowTest.laBaseDelEventoTieneRoundsYJuecesQueSeCambian y FuncionPreparadaRowTest.bajarLosJuecesDelEventoBajaLaPeleaEnJueces por FuncionV5Test (Stepper) e InicioTest.nuevoEventoFormCreatesTheEventWithLabeledFields por InicioV5Test (Stepper); Why: spec 006: ningún Spinner; las pruebas viejas buscaban Spinner y las nuevas prueban lo mismo con Stepper
- 1794281278 | AVAILABLE | crxthrx-8 | release; claim released by crxthrx-8; reason: -; summary kept
- 1794281295 | CLAIMED | crxthrx-8 | claim; next: PR de Evento e Inicio v5
- 1794283444 | REVIEW | crxthrx-8 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/126; evidence: central test exit 0; FuncionV5Test 5, InicioV5Test 1; smoke PRODUCT_PASS; 34 capturas · existing tests changed with a human's approval (cris) · red at 13a1827:     org.opentest4j.AssertionFailedError at FuncionV5Test.java:114; reviewer: -
- 1794283550 | REVIEW | scheduler | deadline decision DSK-043-1794280974; option: Reemplazar FuncionAddRowTest.laBaseDelEventoTieneRoundsYJuecesQueSeCambian y FuncionPreparadaRowTest.bajarLosJuecesDelEventoBajaLaPeleaEnJueces por FuncionV5Test (Stepper) e InicioTest.nuevoEventoFormCreatesTheEventWithLabeledFields por InicioV5Test (Stepper); Why: spec 006: ningún Spinner; las pruebas viejas buscaban Spinner y las nuevas prueban lo mismo con Stepper
- 1794283651 | REVIEW | cris-2 | review; approve; SHA 328f18d71055034fbd4eb355754b1a4538e7e2c3; reviewer cris-2
- 1794284053 | REVIEW | crxthrx-8 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/126; evidence: central test exit 0; smoke PRODUCT_PASS; capturas revisadas · existing tests changed with a human's approval (cris) · red at 13a1827:     org.opentest4j.AssertionFailedError at FuncionV5Test.java:114; reviewer: -
- 1794284175 | REVIEW | cris-2 | review; approve; SHA 9ad47bcb365a05274782c6cfb1c55ca53e7c3d99; reviewer cris-2
- 1794288602 | INTEGRATED | zoe | integrate; recorded by zoe; PR https://github.com/crisMmolina/Boxing-Manager/pull/126; merged f99a3445fd4cc6048a5548a1f79312e17350c268; proof ancestry
