# Agent messages

Append-only; use bash .uak/bin/uak msg / inbox. Format: - N | epoch | from | to | kind | task | text

- 1 | 1792907035 | cris-1 | human | human | CORE-001 | Pending decision (scope): añadir core/build/ (y BOXCENTRAL/build/) a .gitignore y core/.gitattributes con 'gradlew text eol=lf'. Why: la regla /build de la raíz no cubre core/build; CRLF rompe gradlew en Windows (CI lo mitiga con autocrlf input). Ambos archivos están fuera de los Paths de CORE-001. Applied after 15 min without answer if routine and authorized
