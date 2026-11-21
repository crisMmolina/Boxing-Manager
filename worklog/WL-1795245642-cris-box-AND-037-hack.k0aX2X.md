# worklog/WL-1795245642-cris-box-AND-037-hack.k0aX2X.md · AND-037

## Live summary
- Done: CodeGroupSpan «123 456», filtro de dígitos antes del de largo (pegar 123-456), autoJoin y pedir nombre; 4 pruebas nuevas en JoinCodeFieldTest pasan
- Decision: pausado por el humano
- Why: el humano pidió parar
- Failing: JoinCodeTest:110 assertTrue(submit.isEnabled()) falla: la consulta automática deshabilita Unirme
- Commands: android/gradlew -p android :app:testDebugUnitTest --tests 'com.sigebox.juez.join.*'
- Next: esperar respuesta: uak allow para cambiar JoinCodeTest:110 o quitar autoJoin; nada commiteado

## History
- 1795245642 | CLAIMED | cris-box | claim; next: prueba de JoinActivity: escribir 123456 → texto visible «123 456»
- 1795277585 | CLAIMED | cris-box | checkpoint; next: esperar respuesta: uak allow para cambiar JoinCodeTest:110 o quitar autoJoin; nada commiteado
