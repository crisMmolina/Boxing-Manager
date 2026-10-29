# worklog/WL-1793258939-crxtrhx-2-DSK-021-hack.NRDHQS.md · DSK-021

## Live summary
- Done: pantalla Función, Siguiente pelea en Resultado, wiring alSiguiente
- Decision: FuncionQuery SQL para la función más reciente
- Why: FuncionService no la expone
- Failing: ninguno
- Commands: BOXCENTRAL/gradlew -p BOXCENTRAL build
- Next: Wait for review of SHA 6403345

## History
- 1793258939 | CLAIMED | crxtrhx-2 | claim; next: registrar la pantalla Función en `App` y mostrar la cartelera de `FuncionService`
- 1793259506 | REVIEW | crxtrhx-2 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/70; evidence: BOXCENTRAL/gradlew -p BOXCENTRAL build exit 0; smoke exit 0; función 3 peleas + judge-sim (FuncionUiFlowTest, capturas evidence/dsk-021); reviewer: -
- 1793259514 | REVIEW | crxtrhx-2 | checkpoint; next: Wait for review of SHA 6403345
- 1793260209 | REVIEW | cris-1 | review; approve; SHA 64033458970a2e85e21a1e7b5a5b5d259331acb2; reviewer cris-1
