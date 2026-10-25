# DECISIONS

Coordination decisions on the claims branch.
Why: a reversible option after 15 min keeps work moving.
- 1792907122 | CORE-001 | Gradle 9.8.2 wrapper, networknt 3.0.8, jacoco in check | Why: task criteria; latest stable versions verified on Maven Central
- 1792908043 | deadline decision/CORE-001-1792907035/1792907935 | BLOCKED_OTHER_TASK | Option: añadir core/build/ (y BOXCENTRAL/build/) a .gitignore y core/.gitattributes con 'gradlew text eol=lf'; Why: la regla /build de la raíz no cubre core/build; CRLF rompe gradlew en Windows (CI lo mitiga con autocrlf input). Ambos archivos están fuera de los Paths de CORE-001; no money, accounts, publishing or scope expansion.
- 1792908412 | DSK-002 | No se registra respaldo Java-WebSocket: Javalin funcionó. No se instaló WiX ni se creó la regla netsh (admin/seguridad) | Why: Evitar cambios de sistema sin autorización
