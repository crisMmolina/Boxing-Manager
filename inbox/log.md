# Agent messages

Append-only; use bash .uak/bin/uak msg / inbox. Format: - N | epoch | from | to | kind | task | text

- 1 | 1792907035 | cris-1 | human | human | CORE-001 | Pending decision (scope): añadir core/build/ (y BOXCENTRAL/build/) a .gitignore y core/.gitattributes con 'gradlew text eol=lf'. Why: la regla /build de la raíz no cubre core/build; CRLF rompe gradlew en Windows (CI lo mitiga con autocrlf input). Ambos archivos están fuera de los Paths de CORE-001. Applied after 15 min without answer if routine and authorized
- 2 | 1792907116 | cris-1 | agent:zoe-1 | review | CORE-001 | CORE-001 ready for review (your turn by rotation): https://github.com/crisMmolina/Boxing-Manager/pull/1 (SHA 85152d116b9537b8a2be794972ba3df444df3c98). /uak-review CORE-001; if you can't, reply and any free agent takes it (uak next shows it).
