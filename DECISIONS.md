# DECISIONS

Coordination decisions on the claims branch.
Why: a reversible option after 15 min keeps work moving.
- 1792907122 | CORE-001 | Gradle 9.8.2 wrapper, networknt 3.0.8, jacoco in check | Why: task criteria; latest stable versions verified on Maven Central
- 1792908043 | deadline decision/CORE-001-1792907035/1792907935 | BLOCKED_OTHER_TASK | Option: añadir core/build/ (y BOXCENTRAL/build/) a .gitignore y core/.gitattributes con 'gradlew text eol=lf'; Why: la regla /build de la raíz no cubre core/build; CRLF rompe gradlew en Windows (CI lo mitiga con autocrlf input). Ambos archivos están fuera de los Paths de CORE-001; no money, accounts, publishing or scope expansion.
- 1792908412 | DSK-002 | No se registra respaldo Java-WebSocket: Javalin funcionó. No se instaló WiX ni se creó la regla netsh (admin/seguridad) | Why: Evitar cambios de sistema sin autorización
- 1792909957 | DSK-001 | App no extiende Application (Window anidada) para jpackage con classpath | Why: Lanzador Java exige main no-Application con JavaFX en classpath
- 1792910132 | DSK-001 | App.Window anidada; .msi por defecto, app-image sin WiX | Why: jpackage con JavaFX en classpath
- 1792910205 | CORE-002 | 16 messages incl CHALLENGE; strict decode; records enforce schema rules | Why: security HIGH on HELLO replay; no-default-value principle
- 1792910920 | CORE-003 | termination round excluded; IDEA §5 majority rule literal | Why: DSK may close the stopping round; spec literal until human answers
- 1792911338 | AND-002 | BLOCKED: needs physical devices | Why: spike results cannot be produced without the A37 hotspot and phones; inventing them is forbidden
- 1792911986 | deadline decision/CORE-003-1792910864/1792911764 | BLOCKED_OTHER_TASK | Option: 1 juez para una esquina y el resto empate: hoy 'gana esa esquina por decisión mayoritaria' (IDEA §5 literal). En box profesional suele ser 'empate mayoritario'. ¿Qué dice Requerimientos v1.1?; Why: la comparación con papel del Hito 5 lo detectaría; cambiarlo es 1 línea en ResultCalculator; no money, accounts, publishing or scope expansion.
- 1792912693 | DSK-003 | processed_message PK (fight,seat,id); public_key y signature como columnas fuera del modelo core | Why: contrato CORE-002 README
- 1792914901 | DSK-004 | hub transport-agnostic; idempotencia antes de estado; JOIN branches por llave | Why: contrato CORE-002 y AND-003
- 1792914981 | AND-005 | Tarjetas propias desde la bandeja (OwnCards) sin tocar net/; ROUND_CLOSED mustSubmit abre LateScoreActivity por nombre | Why: Paths de AND-005; AND-006 no puede tocar score/
