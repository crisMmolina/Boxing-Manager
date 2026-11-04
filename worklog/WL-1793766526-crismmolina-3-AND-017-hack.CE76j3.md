# worklog/WL-1793766526-crismmolina-3-AND-017-hack.CE76j3.md · AND-017

## Live summary
- Done: Sin valor por defecto (7415f9f), 113 pruebas en verde, PR #78 actualizado
- Decision: Decisiones de cris: sin 10–10; JudgeRuntime en AND-019; FinalCardActivity en AND-021
- Why: Respuestas de cris
- Failing: ninguna
- Commands: android/gradlew -p android :app:testDebugUnitTest
- Next: Wait for review of SHA 7415f9f

## History
- 1793766526 | CLAIMED | crismmolina-3 | claim; next: `ErrorTextTest` que recorre `Ack.Error.values()`
- 1793766870 | CLAIMED | crismmolina-3 | heartbeat; lease until 1793781270
- 1793767069 | CLAIMED | crismmolina-3 | pending decision AND-017-1793767069; option: Registrar AutoFinalCard en net/JudgeRuntime.java (fuera de Paths; sin claim activo en net/); Why: Las pantallas quitan sus listeners en onStop y ROUND_CLOSED no se repite: solo un listener del proceso envía sin pantalla
- 1793767073 | CLAIMED | crismmolina-3 | pending decision AND-017-1793767073; option: Mantener 10–10 inicial con − y + (applyDefault); Why: El criterio dice 'sin valor por defecto' pero ScreenDesignTest y DESIGN.md v2 §6.4 piden 10–10 inicial; no se edita una prueba existente; lo decide cris
- 1793767379 | CLAIMED | crismmolina-3 | checkpoint; next: ErrorText + textos de espera
- 1793768084 | CLAIMED | crismmolina-3 | heartbeat; lease until 1793782484
- 1793768152 | REVIEW | crismmolina-3 | done; PR https://github.com/crisMmolina/Boxing-Manager/pull/78; evidence: android/gradlew -p android :app:testDebugUnitTest exit 0 (113 pruebas) y smoke exit 0 en d063eaa; criterios: ErrorText, esperas y tarjeta final automática listos; 'sin valor por defecto' pendiente de decisión; sin capturas (lección #8); reviewer: -
- 1793768160 | REVIEW | crismmolina-3 | checkpoint; next: Wait for review of SHA d063eaa
- 1793768457 | REVIEW | crismmolina-3 | pending decision AND-017-1793768457; option: Sin valor por defecto: el round abre con las esquinas vacías y el juez toca + o − en cada una antes de enviar; se ajusta ScreenDesignTest (guía v2 reemplazada); Why: cris respondió textualmente: "Sin valor por defecto" (principio 2); registro en JudgeRuntime: "En AND-019"; FinalCardActivity la borra AND-021
- 1793768473 | REVIEW | crismmolina-3 | checkpoint; next: Wait for review of SHA 7415f9f
