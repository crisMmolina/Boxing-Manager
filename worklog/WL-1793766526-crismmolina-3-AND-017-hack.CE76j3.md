# worklog/WL-1793766526-crismmolina-3-AND-017-hack.CE76j3.md · AND-017

## Live summary
- Done: Claim created; implementation pending
- Decision: Own branch and worktree
- Why: Parallel work without touching the main checkout
- Failing: No validation run yet
- Commands: `android/gradlew -p android :app:testDebugUnitTest`
- Next: `ErrorTextTest` que recorre `Ack.Error.values()`

## History
- 1793766526 | CLAIMED | crismmolina-3 | claim; next: `ErrorTextTest` que recorre `Ack.Error.values()`
- 1793766870 | CLAIMED | crismmolina-3 | heartbeat; lease until 1793781270
- 1793767069 | CLAIMED | crismmolina-3 | pending decision AND-017-1793767069; option: Registrar AutoFinalCard en net/JudgeRuntime.java (fuera de Paths; sin claim activo en net/); Why: Las pantallas quitan sus listeners en onStop y ROUND_CLOSED no se repite: solo un listener del proceso envía sin pantalla
- 1793767073 | CLAIMED | crismmolina-3 | pending decision AND-017-1793767073; option: Mantener 10–10 inicial con − y + (applyDefault); Why: El criterio dice 'sin valor por defecto' pero ScreenDesignTest y DESIGN.md v2 §6.4 piden 10–10 inicial; no se edita una prueba existente; lo decide cris
